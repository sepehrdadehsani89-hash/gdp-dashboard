package com.jarvis.assistant.google

import com.jarvis.assistant.omnisystem.FeatureVault
import org.json.JSONArray
import org.json.JSONObject

data class GoogleIndexedPage(val accountId:String,val product:GoogleProduct,val at:Long,val summary:String)
/** Explicit, bounded, encrypted snapshots. Separate from general AI retrieval and mailbox history. */
class GoogleProjectIndex internal constructor(private val read:()->String?,private val exists:()->Boolean,private val write:(String)->Unit,private val now:()->Long=System::currentTimeMillis){
    constructor(vault:FeatureVault):this({vault.read("google_project_index")},{vault.contains("google_project_index")},{vault.write("google_project_index",it)})
    @Synchronized fun list(accountId:String):List<GoogleIndexedPage> = all().filter{it.accountId==accountId}
    private fun all():List<GoogleIndexedPage>{val raw=read();check(raw!=null || !exists()){"The saved Google index is unavailable and was preserved."};if(raw==null)return emptyList()
        val rows=JSONArray(raw);require(rows.length()<=100)
        return (0 until rows.length()).map{val r=rows.getJSONObject(it);GoogleIndexedPage(r.getString("account"),GoogleProduct.valueOf(r.getString("product")),r.getLong("at"),r.getString("summary"))}}
    private fun save(rows:List<GoogleIndexedPage>){write(JSONArray().apply{rows.forEach{put(JSONObject().put("account",it.accountId).put("product",it.product.name).put("at",it.at).put("summary",it.summary))}}.toString())}
    @Synchronized fun index(accountId:String,result:GoogleReadResult){
        require(accountId.matches(Regex("[a-f0-9]{64}")))
        require(result.product in setOf(GoogleProduct.DRIVE,GoogleProduct.DOCS,GoogleProduct.SHEETS,GoogleProduct.SLIDES,GoogleProduct.CALENDAR,GoogleProduct.TASKS))
        val rows=all().filterNot{it.accountId==accountId && it.product==result.product}
        save((rows+GoogleIndexedPage(accountId,result.product,now(),result.summary.take(6000))).takeLast(100))
    }
    @Synchronized fun delete(accountId:String,product:GoogleProduct?=null){save(all().filterNot{it.accountId==accountId && (product==null || product==it.product)})}
    fun summarize(accountId:String):String {val rows=list(accountId);return if(rows.isEmpty())"No Google project snapshots have been indexed. Read an authorized page in Connected Accounts and explicitly choose Index this result. No historical activity is assumed."
        else "Your explicitly indexed Google snapshots (these may be stale; no live refresh was performed):\n"+rows.joinToString("\n"){"${it.product} · ${java.util.Date(it.at)}\n${it.summary.take(1800)}"}.take(10_000)}
}
