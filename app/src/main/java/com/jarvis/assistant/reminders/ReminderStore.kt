package com.jarvis.assistant.reminders

import com.jarvis.assistant.omnisystem.FeatureVault
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Additive store; does not change the calendar, Call Me store, or Room database. */
class ReminderStore internal constructor(private val read:()->String?,private val exists:()->Boolean,private val write:(String)->Unit,private val now:()->Long=System::currentTimeMillis){
    constructor(vault:FeatureVault):this({vault.read("owned_reminders")},{vault.contains("owned_reminders")},{vault.write("owned_reminders",it)})
    @Synchronized fun list():List<OwnedReminder>{val raw=read();check(raw!=null || !exists()){"Saved reminders cannot be read. Existing data was preserved."};if(raw==null)return emptyList()
        val rows=JSONArray(raw);require(rows.length()<=100)
        return (0 until rows.length()).map{val r=rows.getJSONObject(it);OwnedReminder(r.getString("id"),r.getString("revision"),r.getString("title"),r.getLong("at"),r.getString("zone"),ReminderStatus.valueOf(r.getString("status"))).also(ReminderPolicy::validate)}.also{require(it.map{r->r.id}.distinct().size==it.size)}}
    private fun save(rows:List<OwnedReminder>){require(rows.size<=100);rows.forEach(ReminderPolicy::validate);write(JSONArray().apply{rows.forEach{put(JSONObject().put("id",it.id).put("revision",it.revision).put("title",it.title).put("at",it.at).put("zone",it.zone).put("status",it.status.name))}}.toString())}
    @Synchronized fun create(title:String,at:Long,zone:String,id:String?=null):OwnedReminder {
        ReminderPolicy.validateFuture(at,now());val all=list();val old=all.firstOrNull{it.id==id};require(id==null || old!=null);check(all.size<100 || old!=null){"Remove an old reminder before adding another."}
        val row=OwnedReminder(old?.id ?: UUID.randomUUID().toString(),UUID.randomUUID().toString(),title.trim(),at,zone);ReminderPolicy.validate(row);save(all.filterNot{it.id==row.id}+row);return row
    }
    @Synchronized fun ifCurrent(id:String,revision:String,status:ReminderStatus,action:()->Unit):Boolean {if(list().none{it.id==id && it.revision==revision && it.status==status})return false;action();return true}
    @Synchronized fun mark(id:String,revision:String,status:ReminderStatus,expected:ReminderStatus?=null):Boolean {val rows=list();val row=rows.firstOrNull{it.id==id && it.revision==revision && (expected==null || it.status==expected)} ?: return false;save(rows.map{if(it.id==id)row.copy(status=status)else it});return true}
    fun upcoming(end:Long):List<OwnedReminder> = list().filter{it.status==ReminderStatus.SCHEDULED && it.at in now()..end}.sortedBy{it.at}
    @Synchronized fun remove(id:String):Boolean{val all=list();if(all.none{it.id==id})return false;save(all.filterNot{it.id==id});return true}
}
