package com.jarvis.assistant.checkin

import android.app.*
import android.content.*
import android.os.Build
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.R
import com.jarvis.assistant.omnisystem.FeatureVault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class CallMeStore(private val context:Context,private val vault:FeatureVault){
    private val alarm=context.getSystemService(AlarmManager::class.java)
    private val manager=context.getSystemService(NotificationManager::class.java)
    private val mutable=MutableStateFlow<List<CallMeEntry>>(emptyList());val state=mutable.asStateFlow()
    companion object {const val CHANNEL="jarvis_call_me_v1";const val ACTION="com.jarvis.next.CALL_ME"}
    init{manager.createNotificationChannel(NotificationChannel(CHANNEL,"Jarvis scheduled check-ins",NotificationManager.IMPORTANCE_HIGH));runCatching{val all=reload();val recovered=all.map{if(it.status=="DELIVERING")it.copy(status="TEXT_ONLY")else it};if(recovered!=all)write(recovered)}}
    fun exactAllowed()=Build.VERSION.SDK_INT<31 || alarm.canScheduleExactAlarms()
    fun alertsAllowed()=NotificationManagerCompat.from(context).areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL)?.importance!=NotificationManager.IMPORTANCE_NONE
    @Synchronized fun reload():List<CallMeEntry>{
        val raw=vault.read("call_me_entries")
        check(raw!=null || !vault.contains("call_me_entries")){"Existing scheduled check-ins are unavailable; nothing was overwritten."}
        val a=JSONArray(raw ?: "[]");require(a.length()<=50)
        return (0 until a.length()).map{i->a.getJSONObject(i).let{j->CallMeEntry(j.getString("id"),j.getString("revision"),j.getLong("at"),j.getString("zone"),j.getString("title"),j.getBoolean("exact"),j.getBoolean("spoken"),j.getString("status")).also(CallMePolicy::validate)}}.also{v->require(v.map{it.id}.distinct().size==v.size);mutable.value=v}
    }
    private fun write(values:List<CallMeEntry>){require(values.size<=50);values.forEach(CallMePolicy::validate);vault.write("call_me_entries",JSONArray().apply{values.forEach{c->put(JSONObject().put("id",c.id).put("revision",c.revision).put("at",c.at).put("zone",c.zone).put("title",c.title).put("exact",c.exact).put("spoken",c.spoken).put("status",c.status))}}.toString());mutable.value=values}
    private fun pending(c:CallMeEntry)=PendingIntent.getBroadcast(context,0,Intent(context,CallMeReceiver::class.java).setAction(ACTION).setData(Uri.parse("jarvis-checkin://${c.id}/${c.revision}")).putExtra("id",c.id).putExtra("revision",c.revision),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    @android.annotation.SuppressLint("ScheduleExactAlarm") private fun arm(c:CallMeEntry){if(c.exact){check(exactAllowed());alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.at,pending(c))}else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,c.at,pending(c))}
    @Synchronized fun save(at:Long,zone:String,title:String,exact:Boolean,spoken:Boolean,id:String?=null):CallMeEntry {
        require(at>System.currentTimeMillis()+1000 && at-System.currentTimeMillis()<=366L*86400_000);check(alertsAllowed()){"Allow check-in notifications first."};if(exact)check(exactAllowed()){"Allow exact alarms or explicitly choose approximate delivery."}
        val before=reload();val old=before.firstOrNull{it.id==id};require(id==null || old!=null);require(before.size<50 || old!=null)
        val c=CallMeEntry(old?.id ?: UUID.randomUUID().toString(),UUID.randomUUID().toString(),at,zone,title.trim(),exact,spoken);CallMePolicy.validate(c)
        arm(c)
        try{write(before.filterNot{it.id==c.id}+c)}catch(e:Exception){alarm.cancel(pending(c));throw e}
        old?.let{alarm.cancel(pending(it));pending(it).cancel();manager.cancel(it.id.hashCode())};return c
    }
    @Synchronized fun cancel(id:String){val all=reload();val c=all.firstOrNull{it.id==id} ?: return;write(all.filterNot{it.id==id});alarm.cancel(pending(c));pending(c).cancel();manager.cancel(c.id.hashCode())}
    @Synchronized fun mark(id:String,revision:String,status:String):Boolean {
        val all=reload();val c=all.firstOrNull{it.id==id && it.revision==revision} ?: return false
        if(c.status in setOf("SPOKEN","MISSED","PERMISSION_REQUIRED") && status!="DELIVERING")return false
        if(status=="DELIVERING" && c.status=="DELIVERING")return false
        write(all.map{if(it.id==id)c.copy(status=status)else it});return true
    }
    @Synchronized fun receive(id:String,revision:String){
        val c=reload().firstOrNull{it.id==id} ?: return
        if(!CallMePolicy.due(c,revision,System.currentTimeMillis()))return
        if(!alertsAllowed()){mark(id,revision,"TEXT_ONLY");return}
        if(System.currentTimeMillis()-c.at>24*3600_000L){mark(id,revision,"MISSED");return}
        mark(id,revision,"NOTIFIED");notify(c)
        if(c.spoken)(context.applicationContext as JarvisApplication).runtime.requestAiCall(com.jarvis.assistant.call.AiCallSource.CHECK_IN,id,revision,showUi=false)
    }
    @android.annotation.SuppressLint("MissingPermission") private fun notify(c:CallMeEntry){
        if(!alertsAllowed())return
        val open=PendingIntent.getActivity(context,c.id.hashCode(),Intent(context,CallMeActivity::class.java).putExtra("answer_id",c.id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(c.id.hashCode(),NotificationCompat.Builder(context,CHANNEL).setSmallIcon(R.drawable.ic_jarvis).setContentTitle("Jarvis check-in ready").setContentText("You asked Jarvis to call you. Unlock and tap to answer.").setContentIntent(open).addAction(0,"Answer",open).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setCategory(NotificationCompat.CATEGORY_REMINDER).build())
    }
    @Synchronized fun reschedule(){
        val all=reload();val now=System.currentTimeMillis()
        val updated=all.map{c->when{c.status=="DELIVERING"->c.copy(status="TEXT_ONLY");c.status!="SCHEDULED"->c;c.at<=now->c.copy(status="MISSED");c.exact && !exactAllowed()->c.copy(status="PERMISSION_REQUIRED");else->{if(runCatching{arm(c)}.isSuccess)c else c.copy(status="SCHEDULE_UNAVAILABLE")}}}
        if(updated!=all)write(updated)
    }
    fun clear(){reload().forEach{cancel(it.id)}}
}
class CallMeReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){
    val store=(context.applicationContext as JarvisApplication).runtime.callMe
    runCatching{if(intent.action==CallMeStore.ACTION)store.receive(intent.getStringExtra("id").orEmpty(),intent.getStringExtra("revision").orEmpty())else if(intent.action in setOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED))store.reschedule()}
}}
class CallMeService:Service(){
    private val handler=android.os.Handler(android.os.Looper.getMainLooper());private var id="";private var revision=""
    private val watchdog=Runnable{finish(false)}
    private val runtime get()=(application as JarvisApplication).runtime
    override fun onBind(intent:Intent?)=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(id.isNotEmpty())return START_NOT_STICKY
        id=intent?.getStringExtra("id").orEmpty();revision=intent?.getStringExtra("revision").orEmpty()
        val n=NotificationCompat.Builder(this,CallMeStore.CHANNEL).setSmallIcon(R.drawable.ic_jarvis).setContentTitle("Jarvis scheduled voice check-in").setContentText("Playing your requested invitation. Microphone only follows an existing enabled listening session.").build()
        try{if(Build.VERSION.SDK_INT>=29)startForeground(6921,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)else startForeground(6921,n)}catch(_:Exception){stopSelf();return START_NOT_STICKY}
        if(!runtime.beginCallMe(id,revision){finish(true)}){finish(false);return START_NOT_STICKY}
        handler.postDelayed(watchdog,45_000);return START_NOT_STICKY
    }
    private fun finish(completed:Boolean){handler.removeCallbacks(watchdog);if(!completed){runtime.cancelCallMe(id,revision);runCatching{runtime.callMe.mark(id,revision,"TEXT_ONLY")}};stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
    override fun onDestroy(){handler.removeCallbacks(watchdog);runtime.cancelCallMe(id,revision);runCatching{runtime.callMe.mark(id,revision,"TEXT_ONLY")};super.onDestroy()}
}
