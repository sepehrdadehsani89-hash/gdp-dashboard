package com.jarvis.assistant.google

import com.jarvis.assistant.omnisystem.FeatureVault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class GoogleAccountConnection(val id:String,val email:String,val services:Set<GoogleProduct>,val lastVerified:Map<GoogleProduct,Long> = emptyMap(),val teacherEmail:String?=null) {override fun toString()="GoogleAccountConnection(id=$id, identity=redacted, services=$services)"}
data class GoogleConnections(val accounts:List<GoogleAccountConnection> = emptyList(),val selected:String?=null,val error:String?=null)
/** Google Play services owns renewable grants. No bearer/refresh token or mailbox is persisted here. */
class GoogleConnectionsStore internal constructor(private val read:()->String?,private val contains:()->Boolean,private val write:(String)->Unit,private val now:()->Long=System::currentTimeMillis){
    constructor(vault:FeatureVault):this({vault.read("google_connections")},{vault.contains("google_connections")},{vault.write("google_connections",it)})
    private val mutable=MutableStateFlow(GoogleConnections())
    val state=mutable.asStateFlow()
    private val verified=mutableMapOf<String,Long>()
    var epoch:Long=0;private set
    init{runCatching{reload()}.onFailure{mutable.value=GoogleConnections(error="Saved Google connection metadata is unavailable. Existing storage was preserved.")}}
    @Synchronized private fun reload():GoogleConnections {
        val raw=read();check(raw!=null || !contains()){"Saved Google metadata cannot be decrypted. It was not overwritten."}
        if(raw==null)return GoogleConnections().also{mutable.value=it}
        val root=JSONObject(raw);require(root.getInt("version")==1)
        val accounts=root.getJSONArray("accounts");require(accounts.length()<=10)
        val rows=(0 until accounts.length()).map{i->val row=accounts.getJSONObject(i);val email=row.getString("email");val id=GoogleServicesPolicy.accountId(email);require(row.getString("id")==id)
            val services=row.getJSONArray("services");val enabled=(0 until services.length()).map{GoogleProduct.valueOf(services.getString(it))}.toSet();require(enabled.all{GoogleServicesPolicy.definition(it).scope!=null})
            val timestamps=row.optJSONObject("lastVerified");val times=enabled.mapNotNull{p->timestamps?.optLong(p.name)?.takeIf{it>0}?.let{p to it}}.toMap()
            GoogleAccountConnection(id,email,enabled,times,row.optString("teacherEmail").takeIf{it.isNotBlank()}?.also{require(GoogleServicesPolicy.validAccount(it) && !it.any{c->c in "\"(){}"})})}
        require(rows.map{it.id}.distinct().size==rows.size)
        return GoogleConnections(rows,root.optString("selected").takeIf{id->rows.any{it.id==id}}).also{mutable.value=it}
    }
    private fun persist(value:GoogleConnections,accessChanged:Boolean){
        val root=JSONObject().put("version",1).put("selected",value.selected.orEmpty()).put("accounts",JSONArray().apply{value.accounts.forEach{row->put(JSONObject().put("id",row.id).put("email",row.email).put("teacherEmail",row.teacherEmail.orEmpty()).put("services",JSONArray(row.services.map{it.name})).put("lastVerified",JSONObject().apply{row.lastVerified.forEach{(p,t)->put(p.name,t)}}))}})
        write(root.toString());if(accessChanged){epoch++;verified.clear()};mutable.value=value
    }
    @Synchronized fun addAccount(email:String):GoogleAccountConnection {
        require(GoogleServicesPolicy.validAccount(email));val value=reload();val id=GoogleServicesPolicy.accountId(email)
        val row=value.accounts.firstOrNull{it.id==id} ?: GoogleAccountConnection(id,email,emptySet()).also{check(value.accounts.size<10){"At most10 Google connections are supported."}}
        persist(value.copy(accounts=value.accounts.filterNot{it.id==id}+row,selected=id),value.selected!=id || value.accounts.none{it.id==id});return row
    }
    @Synchronized fun setTeacher(id:String,email:String?){val value=reload();val sender=email?.trim()?.takeIf{it.isNotBlank()};require(sender==null || (GoogleServicesPolicy.validAccount(sender) && !sender.any{it in "\"(){}"}));require(value.accounts.any{it.id==id});persist(value.copy(accounts=value.accounts.map{if(it.id==id)it.copy(teacherEmail=sender)else it}),true)}
    @Synchronized fun select(id:String){val value=reload();require(value.accounts.any{it.id==id});if(value.selected!=id)persist(value.copy(selected=id),true)}
    @Synchronized fun selected():GoogleAccountConnection? {val value=reload();return value.accounts.firstOrNull{it.id==value.selected}}
    @Synchronized fun grant(id:String,product:GoogleProduct,scopes:Collection<String>){
        val definition=GoogleServicesPolicy.definition(product);require(definition.scope!=null && definition.scope in scopes){"Google did not return the requested service scope."}
        val value=reload();check(value.selected==id);val row=value.accounts.single{it.id==id}
        if(product !in row.services)persist(value.copy(accounts=value.accounts.map{if(it.id==id)row.copy(services=row.services+product)else it}),true)
    }
    @Synchronized fun markVerified(id:String,product:GoogleProduct,expectedEpoch:Long){
        check(epoch==expectedEpoch);val value=reload();check(value.selected==id);val row=value.accounts.single{it.id==id};check(product in row.services)
        persist(value.copy(accounts=value.accounts.map{if(it.id==id)row.copy(lastVerified=row.lastVerified+(product to now()))else it}),false);verified["$id:$product"]=now()
    }
    @Synchronized fun markFailure(id:String,product:GoogleProduct){verified.remove("$id:$product")}
    @Synchronized fun currentlyVerified(id:String,product:GoogleProduct)=verified["$id:$product"]?.let{now()-it in 0..3_600_000}==true
    @Synchronized fun removeService(id:String,product:GoogleProduct){val value=reload();persist(value.copy(accounts=value.accounts.map{if(it.id==id)it.copy(services=it.services-product,lastVerified=it.lastVerified-product)else it}),true)}
    @Synchronized fun removeAccount(id:String){val value=reload();val rows=value.accounts.filterNot{it.id==id};persist(value.copy(accounts=rows,selected=if(value.selected==id)rows.firstOrNull()?.id else value.selected),true)}
}
