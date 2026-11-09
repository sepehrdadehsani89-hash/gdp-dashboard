package com.jarvis.assistant.titan

import android.content.*
import android.app.*
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.R
import com.jarvis.assistant.hud.HudRepository
import kotlinx.coroutines.*
import java.time.ZonedDateTime

class BatteryAlertPreferences(context:Context){
    private val prefs=context.getSharedPreferences("jarvis_titan_battery_v1",Context.MODE_PRIVATE)
    var enabled:Boolean get()=prefs.getBoolean("enabled",true);set(v){check(prefs.edit().putBoolean("enabled",v).commit())}
    var critical:Boolean get()=prefs.getBoolean("critical",true);set(v){check(prefs.edit().putBoolean("critical",v).commit())}
    var notifications:Boolean get()=prefs.getBoolean("notifications",true);set(v){check(prefs.edit().putBoolean("notifications",v).commit())}
    var quiet:Boolean get()=prefs.getBoolean("quiet",true);set(v){check(prefs.edit().putBoolean("quiet",v).commit())}
    var start:Int get()=prefs.getInt("start",22).coerceIn(0,23);set(v){require(v in 0..23);check(prefs.edit().putInt("start",v).commit())}
    var end:Int get()=prefs.getInt("end",7).coerceIn(0,23);set(v){require(v in 0..23);check(prefs.edit().putInt("end",v).commit())}
    var volume:Float get()=prefs.getFloat("volume",.8f).coerceIn(0f,1f);set(v){check(prefs.edit().putFloat("volume",v.coerceIn(0f,1f)).commit())}
    fun state()=BatteryAlertState(prefs.getInt("previous",-1).takeIf{it in 0..100},prefs.getBoolean("powered",false),prefs.getInt("mask",0),prefs.getLong("episode",0))
    fun persist(s:BatteryAlertState){check(prefs.edit().putInt("previous",s.previous ?: -1).putBoolean("powered",s.powered).putInt("mask",s.announcedMask).putLong("episode",s.episode).commit())}
    var lastStatus:String get()=prefs.getString("last_status","No spoken alert completed").orEmpty();set(v){prefs.edit().putString("last_status",v.take(140)).apply()}
    var chargingStart:Boolean get()=prefs.getBoolean("charging_start",true);set(v){prefs.edit().putBoolean("charging_start",v).apply()}
    var fullEnabled:Boolean get()=prefs.getBoolean("full_enabled",true);set(v){prefs.edit().putBoolean("full_enabled",v).apply()}
    var repeatFull:Boolean get()=prefs.getBoolean("repeat_full",true);set(v){prefs.edit().putBoolean("repeat_full",v).apply()}
    var interval:Int get()=prefs.getInt("full_interval",15).takeIf{it in listOf(5,10,15,30)} ?: 15;set(v){require(v in listOf(5,10,15,30));prefs.edit().putInt("full_interval",v).apply()}
    var disconnectVoice:Boolean get()=prefs.getBoolean("disconnect_voice",false);set(v){prefs.edit().putBoolean("disconnect_voice",v).apply()}
    var cinematicFull:Boolean get()=prefs.getBoolean("cinematic_full",false);set(v){prefs.edit().putBoolean("cinematic_full",v).apply()}
    var progressMinutes:Int get()=prefs.getInt("progress_minutes",0).takeIf{it in listOf(0,15,30,60)} ?: 0;set(v){require(v in listOf(0,15,30,60));prefs.edit().putInt("progress_minutes",v).apply()}
    fun config()=com.jarvis.assistant.power.PowerAlertConfig(enabled,critical,chargingStart,fullEnabled,if(repeatFull)interval else 0,disconnectVoice,progressMinutes)
    fun powerState():com.jarvis.assistant.power.PowerAlertState {
        val saved=prefs.getString("overcharge_state",null)
        if(saved!=null)com.jarvis.assistant.power.PowerAlertCodec.decode(saved)?.let{return it}
        val old=state()
        // Legacy mask reserved playback, so it cannot establish completed speech.
        return com.jarvis.assistant.power.PowerAlertState(percent=old.previous,connected=old.powered,episode=old.episode,
            criticalPending=old.previous?.takeIf{!old.powered && it in 1..10})
    }
    fun savePower(s:com.jarvis.assistant.power.PowerAlertState){check(prefs.edit().putString("overcharge_state",com.jarvis.assistant.power.PowerAlertCodec.encode(s)).commit())}
    fun clear(){check(prefs.edit().clear().commit())}
}
/** One bounded system-alert coordinator using the shared battery source and Fish output pipeline. */
class BatteryAlerts(private val context:Context,private val hud:HudRepository,private val scope:CoroutineScope,
    private val ready:()->Boolean,private val play:(String,Float,()->Unit,()->Unit,(String)->Unit)->Unit,
    private val language:()->com.jarvis.assistant.core.AppLanguage,private val stopPlayback:()->Unit){
    val preferences=BatteryAlertPreferences(context)
    private var started=false
    private var state=preferences.powerState()
    private var lastConfig=preferences.config()
    private var reading=com.jarvis.assistant.power.PowerReading()
    private var active:com.jarvis.assistant.power.PowerAlert?=null
    val isPlaying get()=active!=null
    private var serial=0L
    private var notified:String?=null
    private var scheduledAt:Long?=null
    private var watchdog:Job?=null
    private var observedDrain:Job?=null
    private val traces=ArrayDeque<String>()
    var lastLatencyMs:Long?=null;private set
    fun diagnostics()=traces.toList()
    private fun trace(code:String){if(traces.size>=20)traces.removeFirst();traces.addLast("${android.os.SystemClock.elapsedRealtime()} · $code")}
    fun start(){
        if(started)return
        started=true;hud.powerSource.start()
        scope.launch{hud.powerSource.state.collect{
            val current=hud.powerSource.state.value
            if(current.observedAt==0L)return@collect
            reading=current
            save(com.jarvis.assistant.power.PowerAlertPolicy.observe(state,current))
            if(current.charging && !current.verifiedFull && preferences.progressMinutes>0 && state.progressAt==0L)save(state.copy(progressAt=System.currentTimeMillis()+preferences.progressMinutes*60_000L))
            if(active?.let{!valid(it)}==true){cancel();stopPlayback()}
            trace("BATTERY_OBSERVED");schedule()
            observedDrain?.cancel();observedDrain=scope.launch{delay(400);drain()}
        }}
        scope.launch{while(isActive){delay(15_000);drain();schedule()}}
    }
    private fun save(s:com.jarvis.assistant.power.PowerAlertState){if(s!=state){preferences.savePower(s);state=s}}
    fun cancel(){
        active?.let{save(com.jarvis.assistant.power.PowerAlertPolicy.failed(state,it,System.currentTimeMillis()))}
        active=null;serial++;watchdog?.cancel();watchdog=null
    }
    private fun valid(a:com.jarvis.assistant.power.PowerAlert):Boolean {
        val c=preferences.config()
        if(!c.enabled)return false
        return when(a.kind){
            com.jarvis.assistant.power.PowerAlertKind.CRITICAL->a.key.startsWith("critical:${state.episode}:") && reading.battery.percent==a.level && !reading.charging && reading.condition in setOf(com.jarvis.assistant.power.PowerCondition.DISCHARGING,com.jarvis.assistant.power.PowerCondition.PLUGGED_NOT_CHARGING) && (requireNotNull(a.level)>5 || c.critical)
            com.jarvis.assistant.power.PowerAlertKind.FULL,com.jarvis.assistant.power.PowerAlertKind.REMINDER->a.key.startsWith("charge:${state.session}:") && reading.verifiedFull && c.full && !state.dismissed && System.currentTimeMillis()>=state.snoozeUntil
            com.jarvis.assistant.power.PowerAlertKind.START,com.jarvis.assistant.power.PowerAlertKind.ESTIMATE->a.key.startsWith("charge:${state.session}:") && reading.charging && !reading.verifiedFull && c.chargingStart
            com.jarvis.assistant.power.PowerAlertKind.PROGRESS->a.key.startsWith("charge:${state.session}:") && reading.charging && !reading.verifiedFull && c.progressMinutes>0 && !state.dismissed && System.currentTimeMillis()>=state.snoozeUntil
            com.jarvis.assistant.power.PowerAlertKind.DISCONNECTED->reading.connected==false && c.disconnect
        }
    }
    private fun blocked():String? {
        val p=preferences
        val audio=context.getSystemService(AudioManager::class.java)
        val permission=ContextCompat.checkSelfPermission(context,Manifest.permission.READ_PHONE_STATE)==PackageManager.PERMISSION_GRANTED
        val idle=permission && runCatching{context.getSystemService(android.telephony.TelephonyManager::class.java).callState==android.telephony.TelephonyManager.CALL_STATE_IDLE}.getOrDefault(false)
        return com.jarvis.assistant.power.PowerAudioPolicy.blocked(p.quiet,ZonedDateTime.now().hour,p.start,p.end,permission,idle,audio.mode==AudioManager.MODE_NORMAL,audio.ringerMode==AudioManager.RINGER_MODE_SILENT,
            context.getSystemService(NotificationManager::class.java).currentInterruptionFilter!=NotificationManager.INTERRUPTION_FILTER_ALL,ready())
    }
    fun drain(){
        val latest=hud.powerSource.state.value
        if(latest.observedAt!=0L && latest!=reading){reading=latest;save(com.jarvis.assistant.power.PowerAlertPolicy.observe(state,latest))}
        active?.let{if(!valid(it)){cancel();stopPlayback()}else return}
        val now=System.currentTimeMillis()
        if(android.os.SystemClock.elapsedRealtime()-reading.observedAt !in 0..150_000L)return
        val item=com.jarvis.assistant.power.PowerAlertPolicy.next(state,reading,preferences.config(),now) ?: return
        blocked()?.let{preferences.lastStatus=it;if(preferences.notifications)notifyAlert(item);return}
        save(com.jarvis.assistant.power.PowerAlertPolicy.requested(state,item,now));active=item
        preferences.lastStatus="${item.kind} playback requested; not yet completed";trace("${item.kind}_QUEUED")
        val playbackSerial=++serial
        val requested=android.os.SystemClock.elapsedRealtime()
        play(phrase(item),preferences.volume,{
            if(playbackSerial==serial){lastLatencyMs=android.os.SystemClock.elapsedRealtime()-requested;trace("${item.kind}_AUDIO_STARTED")}
        },{
            if(playbackSerial!=serial)return@play
            active=null;watchdog?.cancel();watchdog=null
            if(valid(item))save(com.jarvis.assistant.power.PowerAlertPolicy.completed(state,item,preferences.config(),System.currentTimeMillis()))
            preferences.lastStatus="${item.kind} playback completed";trace("${item.kind}_AUDIO_COMPLETED");schedule();scope.launch{delay(500);drain()}
        },{_->if(playbackSerial==serial)failed(item)})
        watchdog=scope.launch{delay(45_000);if(playbackSerial==serial && active!=null){failed(item);stopPlayback()}}
    }
    private fun failed(item:com.jarvis.assistant.power.PowerAlert){
        active=null;serial++;watchdog?.cancel();watchdog=null
        save(com.jarvis.assistant.power.PowerAlertPolicy.failed(state,item,System.currentTimeMillis()))
        preferences.lastStatus=if((state.attempts[item.key]?.count ?: 0)>=3)"${item.kind} speech unavailable; retry limit reached, text retained" else "${item.kind} speech unavailable; text retained, bounded retry pending";trace("${item.kind}_AUDIO_FAILED")
        if(preferences.notifications)notifyAlert(item)
        schedule()
    }
    private fun phrase(a:com.jarvis.assistant.power.PowerAlert):String {
        val es=language()==com.jarvis.assistant.core.AppLanguage.ES
        return when(a.kind){
            com.jarvis.assistant.power.PowerAlertKind.CRITICAL->BatteryPolicy.phrase(requireNotNull(a.level),language())
            com.jarvis.assistant.power.PowerAlertKind.REMINDER->if(es)"Señor, tu teléfono sigue completamente cargado y conectado a la corriente." else "Sir, your phone is still fully charged and connected to power."
            com.jarvis.assistant.power.PowerAlertKind.FULL->if(preferences.cinematicFull){if(es)"Energía restaurada, señor. Batería al cien por ciento. Todos los sistemas listos." else "Power restored, sir. Battery at one hundred percent. All systems ready."}
                else if(es)"Señor, tu batería está completamente cargada. Está al cien por ciento y lista para usar." else "Sir, your battery is fully charged. You're at one hundred percent and ready to go."
            com.jarvis.assistant.power.PowerAlertKind.DISCONNECTED->if(es)"Carga desconectada, señor." else "Charging disconnected, sir."
            else->com.jarvis.assistant.power.ChargingText.answer(reading,android.os.SystemClock.elapsedRealtime(),System.currentTimeMillis(),java.time.ZoneId.systemDefault().id,language())
        }
    }
    private fun notifyAlert(a:com.jarvis.assistant.power.PowerAlert){
        if(notified==a.key)return
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        runCatching{
            val manager=context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("jarvis_titan_battery","Jarvis battery",NotificationManager.IMPORTANCE_LOW))
            val b=NotificationCompat.Builder(context,"jarvis_titan_battery").setSmallIcon(R.drawable.ic_jarvis).setContentTitle("Jarvis · Power Core")
                .setContentText(phrase(a)).setStyle(NotificationCompat.BigTextStyle().bigText(phrase(a))).setOnlyAlertOnce(true).setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(context,6112,Intent(context,TitanCenterActivity::class.java).putExtra("page","Battery"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            if(a.kind in setOf(com.jarvis.assistant.power.PowerAlertKind.FULL,com.jarvis.assistant.power.PowerAlertKind.REMINDER)){
                for(action in listOf("snooze","dismiss"))b.addAction(0,if(action=="snooze")"Snooze 15 min" else "Dismiss until unplugged",PendingIntent.getBroadcast(context,if(action=="snooze")6110 else 6111,Intent(context,com.jarvis.assistant.power.PowerReminderAction::class.java).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
            manager.notify(6107,b.build());notified=a.key
        }
    }
    fun dismissReminders(){save(com.jarvis.assistant.power.PowerAlertPolicy.dismiss(state));if(active?.kind in setOf(com.jarvis.assistant.power.PowerAlertKind.FULL,com.jarvis.assistant.power.PowerAlertKind.REMINDER)){cancel();stopPlayback()};preferences.lastStatus="Full-charge reminders dismissed until unplugged";schedule()}
    fun snoozeReminders(){save(com.jarvis.assistant.power.PowerAlertPolicy.snooze(state,System.currentTimeMillis()));if(active?.kind in setOf(com.jarvis.assistant.power.PowerAlertKind.FULL,com.jarvis.assistant.power.PowerAlertKind.REMINDER)){cancel();stopPlayback()};preferences.lastStatus="Full-charge reminders snoozed for 15 minutes";schedule()}
    fun settingsChanged(){
        val config=preferences.config()
        if(config.repeatMinutes!=lastConfig.repeatMinutes && state.fullDone)save(state.copy(reminderAt=if(config.repeatMinutes>0)System.currentTimeMillis()+config.repeatMinutes*60_000L else 0))
        if(config.progressMinutes!=lastConfig.progressMinutes)save(state.copy(progressAt=if(config.progressMinutes>0 && reading.charging)System.currentTimeMillis()+config.progressMinutes*60_000L else 0))
        lastConfig=config;scheduledAt=null;drain();schedule()
    }
    fun scheduledCheck(){hud.powerSource.refresh();drain();schedule()}
    fun retryPending(){save(state.copy(attempts=emptyMap()));preferences.lastStatus="Retry requested for the current observed alert";drain();schedule()}
    fun reset(){cancel();state=com.jarvis.assistant.power.PowerAlertState();scheduledAt=null;androidx.work.WorkManager.getInstance(context).cancelUniqueWork(com.jarvis.assistant.power.PowerReminderWorker.WORK);preferences.clear()}
    private fun schedule(){
        val now=System.currentTimeMillis()
        var at=com.jarvis.assistant.power.PowerAlertPolicy.fullCheckAt(state,reading,preferences.config(),now)
        if(at!=null){
            if(at<=now+1000)at=(now/60_000+1)*60_000
            if(blocked()!=null)at=maxOf(at,(now/(15*60_000)+1)*(15*60_000))
        }
        if(at==scheduledAt)return
        scheduledAt=at
        val work=androidx.work.WorkManager.getInstance(context)
        if(at==null){work.cancelUniqueWork(com.jarvis.assistant.power.PowerReminderWorker.WORK);return}
        work.enqueueUniqueWork(com.jarvis.assistant.power.PowerReminderWorker.WORK,androidx.work.ExistingWorkPolicy.REPLACE,
            androidx.work.OneTimeWorkRequestBuilder<com.jarvis.assistant.power.PowerReminderWorker>().setInitialDelay((at-now).coerceAtLeast(1000),java.util.concurrent.TimeUnit.MILLISECONDS).build())
    }
}
