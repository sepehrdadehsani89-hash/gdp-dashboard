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
    fun clear(){check(prefs.edit().clear().commit())}
}
/** Android battery events only. No repeating announcement timer, new recorder or hidden service. */
class BatteryAlerts(private val context:Context,private val hud:HudRepository,private val scope:CoroutineScope,
    private val ready:()->Boolean,private val play:(String,Float,()->Unit,(String)->Unit)->Unit,private val language:()->com.jarvis.assistant.core.AppLanguage){
    val preferences=BatteryAlertPreferences(context)
    private var started=false
    private var pending:Pair<Int,Long>?=null
    private var active=false
    private var serial=0L
    private var notified:Pair<Int,Long>?=null
    private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){if(i?.action==Intent.ACTION_BATTERY_CHANGED)observed(i)}}
    fun start(){if(started)return;started=true;ContextCompat.registerReceiver(context,receiver,IntentFilter(Intent.ACTION_BATTERY_CHANGED),ContextCompat.RECEIVER_EXPORTED)?.let(::observed)}
    private fun observed(intent:Intent){
        val battery=HudRepository.battery(intent);hud.acceptBattery(intent)
        val powered=battery.charging || battery.full || battery.source!="Battery"
        val result=BatteryAlertPolicy.observe(preferences.state(),battery.percent,powered)
        preferences.persist(result.state)
        if(powered || battery.percent!=pending?.first)pending=null
        result.candidate?.let{pending=it to android.os.SystemClock.elapsedRealtime()}
        drain()
    }
    fun cancel(){pending=null;active=false;serial++}
    fun drain(){
        if(active)return
        val item=pending ?: return
        val p=preferences
        if(android.os.SystemClock.elapsedRealtime()-item.second !in 0..120_000 || !p.enabled || item.first<=5 && !p.critical){pending=null;return}
        val now=ZonedDateTime.now()
        if(p.quiet && BatteryPolicy.quiet(now.hour,p.start,p.end)){pending=null;p.lastStatus="Suppressed by quiet hours";return}
        val audio=context.getSystemService(AudioManager::class.java)
        val notification=context.getSystemService(NotificationManager::class.java)
        // Fail closed without reliable call-state permission; never speak over an emergency/other call.
        val idle=ContextCompat.checkSelfPermission(context,Manifest.permission.READ_PHONE_STATE)==PackageManager.PERMISSION_GRANTED && runCatching{context.getSystemService(android.telephony.TelephonyManager::class.java).callState==android.telephony.TelephonyManager.CALL_STATE_IDLE}.getOrDefault(false)
        if(!idle || audio.mode!=AudioManager.MODE_NORMAL || audio.ringerMode==AudioManager.RINGER_MODE_SILENT || notification.currentInterruptionFilter!=NotificationManager.INTERRUPTION_FILTER_ALL || !ready()){
            if(p.notifications)notifyLevel(item.first)
            return
        }
        val current=p.state();if(current.previous!=item.first || current.powered){pending=null;return}
        p.persist(BatteryAlertPolicy.reserve(current,item.first));pending=null;active=true
        p.lastStatus="Playback requested at ${item.first}% (not yet completed)"
        val playbackSerial=++serial
        play(BatteryPolicy.phrase(item.first,language()),p.volume,{
            if(playbackSerial!=serial)return@play
            active=false;p.lastStatus="Playback completed at ${item.first}%";drain()
        },{_->if(playbackSerial==serial){active=false;p.lastStatus="Voice unavailable at ${item.first}%; see text notification";if(p.notifications)notifyLevel(item.first);drain()}})
        scope.launch{delay(60_000);if(playbackSerial==serial){active=false;drain()}} // bounded recovery, no repeat announcement
    }
    private fun notifyLevel(level:Int){
        val key=level to preferences.state().episode
        if(notified==key)return
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        runCatching{
            val manager=context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("jarvis_titan_battery","Jarvis battery",NotificationManager.IMPORTANCE_LOW))
            manager.notify(6107,NotificationCompat.Builder(context,"jarvis_titan_battery").setSmallIcon(R.drawable.ic_jarvis).setContentTitle("Jarvis · $level%")
                .setContentText(BatteryPolicy.phrase(level,language())).setOnlyAlertOnce(true).setAutoCancel(true).build())
            notified=key
        }
    }
}
