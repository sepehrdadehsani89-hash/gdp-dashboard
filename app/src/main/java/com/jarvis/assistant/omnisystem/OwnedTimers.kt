package com.jarvis.assistant.omnisystem

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.R
import com.jarvis.assistant.core.choose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class OwnedTimer(val id:Int,val nonce:String,val seconds:Int,val created:Long,val deadline:Long,val boot:Int,val status:String)
object OwnedTimerPolicy {
    fun remaining(timer:OwnedTimer,elapsed:Long,boot:Int):Long? {
        if(timer.boot!=boot || elapsed<timer.created || timer.status !in setOf("SCHEDULED","PREPARING"))return null
        return (timer.deadline-elapsed).coerceAtLeast(0)
    }
    fun fraction(timer:OwnedTimer,elapsed:Long,boot:Int):Float?=remaining(timer,elapsed,boot)?.let{(1.0-it.toDouble()/(timer.seconds*1000L)).toFloat().coerceIn(0f,1f)}
    fun validate(timer:OwnedTimer){
        require(timer.id>0 && timer.seconds in 1..86400 && timer.created>=0 && timer.deadline-timer.created==timer.seconds*1000L && timer.boot>=0 && timer.nonce.length in 20..100)
        require(timer.status in setOf("PREPARING","SCHEDULED","ALERT_REQUESTED","ALERT_BLOCKED","CANCELLED","INTERRUPTED"))
    }
}
/** Own timers only. Never invents state for an external Clock app. Reboots interrupt timers. */
class OwnedTimers(private val context:Context){
    companion object {const val CHANNEL="jarvis_owned_timers_v1"}
    private val prefs=context.getSharedPreferences("jarvis_owned_timers_v1",Context.MODE_PRIVATE)
    private val alarm=context.getSystemService(AlarmManager::class.java)
    private val notifications=context.getSystemService(NotificationManager::class.java)
    private val health=MutableStateFlow(true)
    val readable=health.asStateFlow()
    private val mutable=MutableStateFlow(read())
    val state=mutable.asStateFlow()
    init {notifications.createNotificationChannel(NotificationChannel(CHANNEL,"Jarvis timers",NotificationManager.IMPORTANCE_HIGH).apply{description="Alerts for explicitly created Jarvis timers; Android sound and DND settings apply."})}
    fun boot()=Settings.Global.getInt(context.contentResolver,Settings.Global.BOOT_COUNT,-1)
    fun exactAllowed()=Build.VERSION.SDK_INT<31 || alarm.canScheduleExactAlarms()
    fun alertsAllowed()=NotificationManagerCompat.from(context).areNotificationsEnabled() && notifications.getNotificationChannel(CHANNEL)?.importance!=NotificationManager.IMPORTANCE_NONE
    @Synchronized fun reconcile(){
        val values=read();if(!health.value)return
        if(!exactAllowed()){
            val changed=values.map{t->if(t.status in setOf("SCHEDULED","PREPARING")){val p=broadcast(t);alarm.cancel(p);p.cancel();t.copy(status="INTERRUPTED")}else t}
            if(changed!=values)write(changed)
        }
    }
    private fun read():List<OwnedTimer> = runCatching{
        val raw=JSONArray(prefs.getString("timers","[]"));require(raw.length()<=20)
        (0 until raw.length()).map{i->val t=raw.getJSONObject(i)
            OwnedTimer(t.getInt("id"),t.getString("nonce"),t.getInt("seconds"),t.getLong("created"),t.getLong("deadline"),t.getInt("boot"),t.getString("status")).also(OwnedTimerPolicy::validate)
        }.also{values->require(values.map{it.id}.distinct().size==values.size && values.map{it.nonce}.distinct().size==values.size)}
    }.onSuccess{health.value=true}.getOrElse{health.value=false;emptyList()}
    private fun write(list:List<OwnedTimer>){
        check(health.value){"Stored timers are unavailable; existing data was not overwritten."}
        require(list.size<=20);list.forEach(OwnedTimerPolicy::validate)
        val raw=JSONArray().apply{list.forEach{t->put(JSONObject().put("id",t.id).put("nonce",t.nonce).put("seconds",t.seconds).put("created",t.created).put("deadline",t.deadline).put("boot",t.boot).put("status",t.status))}}.toString()
        check(prefs.edit().putString("timers",raw).commit()){"Timer state could not be saved."};mutable.value=list
    }
    private fun broadcast(timer:OwnedTimer,action:String="elapsed")=PendingIntent.getBroadcast(context,timer.id*2+(if(action=="elapsed")0 else 1),
        Intent(context,OwnedTimerReceiver::class.java).setAction("com.jarvis.next.TIMER_$action").putExtra("id",timer.id).putExtra("nonce",timer.nonce),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    @Synchronized @SuppressLint("ScheduleExactAlarm")
    fun start(seconds:Int):OwnedTimer {
        require(seconds in 1..86400);check(exactAllowed()){"Allow exact alarms in Android settings."};check(alertsAllowed()){"Allow timer notifications first."}
        val boot=boot();check(boot>=0){"Android boot identity unavailable."}
        val existing=read();check(health.value);check(existing.count{OwnedTimerPolicy.remaining(it,SystemClock.elapsedRealtime(),boot)!=null}<10){"Ten timers are already active."}
        val previous=prefs.getInt("next_id",0);var id=if(previous>=1_000_000)1 else previous+1
        while(existing.any{it.id==id})id++
        check(prefs.edit().putInt("next_id",id).commit())
        val now=SystemClock.elapsedRealtime();val timer=OwnedTimer(id,UUID.randomUUID().toString(),seconds,now,now+seconds*1000L,boot,"PREPARING")
        val keep=existing.filter{OwnedTimerPolicy.remaining(it,now,boot)!=null}.takeLast(19)
        existing.filter{old->keep.none{it.id==old.id}}.forEach{notifications.cancel(it.id)}
        write(keep+timer)
        val pending=broadcast(timer)
        try {
            alarm.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,timer.deadline,pending)
            val scheduled=timer.copy(status="SCHEDULED");write(keep+scheduled);return scheduled
        } catch(e:Exception){alarm.cancel(pending);pending.cancel();write(keep);throw e}
    }
    @Synchronized fun cancel(id:Int){
        val current=read();val timer=current.firstOrNull{it.id==id} ?: return
        val alarmIntent=broadcast(timer);alarm.cancel(alarmIntent);alarmIntent.cancel();notifications.cancel(timer.id)
        write(current.filterNot{it.id==id})
    }
    @Synchronized @SuppressLint("MissingPermission")
    fun receive(id:Int,nonce:String,action:String){
        val current=read();val timer=current.firstOrNull{it.id==id && it.nonce==nonce} ?: return
        if(action=="dismiss"){cancel(id);return}
        if(action!="elapsed" || timer.status !in setOf("PREPARING","SCHEDULED") || timer.boot!=boot() || SystemClock.elapsedRealtime()<timer.deadline)return
        if(!alertsAllowed()){write(current.map{if(it.id==id)it.copy(status="ALERT_BLOCKED")else it});return}
        val language=(context.applicationContext as JarvisApplication).runtime.store.settings.value.language
        val open=PendingIntent.getActivity(context,id,Intent(context,ClockHudActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            val notification=NotificationCompat.Builder(context,CHANNEL).setSmallIcon(R.drawable.ic_jarvis)
                .setContentTitle(language.choose("Jarvis timer elapsed","Temporizador de Jarvis finalizado","زمان تایمر جارویس پایان یافت"))
                .setContentText(language.choose("Open Jarvis to review or dismiss.","Abre Jarvis para revisar o descartar.","برای بررسی یا بستن، جارویس را باز کنید."))
                .setContentIntent(open).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setCategory(NotificationCompat.CATEGORY_ALARM)
                .addAction(0,language.choose("Dismiss","Descartar","بستن"),broadcast(timer,"dismiss")).build()
            notifications.notify(id,notification)
            write(current.map{if(it.id==id)it.copy(status="ALERT_REQUESTED")else it})
        } catch(_:SecurityException){write(current.map{if(it.id==id)it.copy(status="ALERT_BLOCKED")else it})}
    }
    @Synchronized fun clear(){read().forEach{val p=broadcast(it);alarm.cancel(p);p.cancel();notifications.cancel(it.id)};check(prefs.edit().clear().commit());health.value=true;mutable.value=emptyList()}
}
class OwnedTimerReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){
    runCatching{(context.applicationContext as JarvisApplication).runtime.timers.receive(intent.getIntExtra("id",-1),intent.getStringExtra("nonce").orEmpty(),intent.action.orEmpty().substringAfter("TIMER_"))}
}}
