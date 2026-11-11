package com.jarvis.assistant.call

import android.app.*
import android.content.*
import android.media.*
import android.os.*
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.R
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

/** Visible ringing/status only. The existing exclusive wake service owns the command mic. */
class AiCallService:Service(){
    private val runtime get()=(application as JarvisApplication).runtime
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val handler=Handler(Looper.getMainLooper())
    private var fallbackOnly=false
    private var callId="";private var observe:Job?=null;private var tone:ToneGenerator?=null
    private val ring=object:Runnable{override fun run(){
        val s=runtime.aiCall.state.value
        if(s.id!=callId || s.phase!=AiCallPhase.RINGING)return
        val audio=getSystemService(AudioManager::class.java);val notifications=getSystemService(NotificationManager::class.java)
        if(audio.mode==AudioManager.MODE_NORMAL && audio.ringerMode==AudioManager.RINGER_MODE_NORMAL && notifications.currentInterruptionFilter==NotificationManager.INTERRUPTION_FILTER_ALL && notifications.areNotificationsEnabled() && notifications.getNotificationChannel(CHANNEL)?.importance!=NotificationManager.IMPORTANCE_NONE){
            runCatching{if(tone==null)tone=ToneGenerator(AudioManager.STREAM_NOTIFICATION,if(s.source==AiCallSource.BATTERY_20)(runtime.batteryAlerts.preferences.volume*100).toInt().coerceIn(0,100)else 55);tone?.startTone(ToneGenerator.TONE_PROP_BEEP2,450)}
        }
        handler.postDelayed(this,2500)
    }}
    override fun onCreate(){super.onCreate();current=WeakReference(this);getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Jarvis AI calls",NotificationManager.IMPORTANCE_HIGH).apply{description="User-requested AI voice calls: Answer or End. Not telephone calls.";setSound(null,null)})}
    override fun onBind(intent:Intent?)=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        val id=intent?.getStringExtra("call_id").orEmpty();val s=runtime.aiCall.state.value
        if(id!=s.id || !s.active){stopSelf(startId);return START_NOT_STICKY}
        callId=id;fallbackOnly=false
        try{val n=notification(s);if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)else startForeground(NOTIFICATION,n)}catch(_:Exception){fallbackOnly=showFallback(this,s);stopSelf(startId);return START_NOT_STICKY}
        observe?.cancel();observe=scope.launch{runtime.aiCall.state.collect{next->
            if(next.id!=callId || !next.active){silence();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(startId)}
            else{if(Build.VERSION.SDK_INT<33 || androidx.core.content.ContextCompat.checkSelfPermission(this@AiCallService,android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED)runCatching{getSystemService(NotificationManager::class.java).notify(NOTIFICATION,notification(next))};if(next.phase==AiCallPhase.RINGING){handler.removeCallbacks(ring);handler.post(ring)}else silence()}
        }}
        return START_NOT_STICKY
    }
    private fun notification(s:AiCallState)=buildNotification(this,s)

    fun silence(){handler.removeCallbacks(ring);runCatching{tone?.stopTone()};runCatching{tone?.release()};tone=null}
    override fun onDestroy(){silence();observe?.cancel();scope.cancel();if(!fallbackOnly)runtime.endAiCall(callId,"CALL_SERVICE_STOPPED");if(current.get()===this)current=WeakReference(null);super.onDestroy()}
    companion object {
    private fun buildNotification(context:Context,s:AiCallState):android.app.Notification {
        fun open(answer:Boolean)=PendingIntent.getActivity(context,if(answer)1 else 0,Intent(context,AiCallActivity::class.java).setData(android.net.Uri.parse("jarvis-ai-call://${s.id}/${if(answer)"answer" else "open"}")).putExtra("call_id",s.id).putExtra("answer",answer),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val end=PendingIntent.getBroadcast(context,0,Intent(context,AiCallActionReceiver::class.java).setData(android.net.Uri.parse("jarvis-ai-call://${s.id}/end")).putExtra("call_id",s.id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val b=NotificationCompat.Builder(context,CHANNEL).setSmallIcon(R.drawable.ic_jarvis).setContentTitle(if(s.phase==AiCallPhase.RINGING)"Incoming Jarvis AI call" else "Jarvis AI call · ${s.phase.name.lowercase()}").setContentText(if(s.phase==AiCallPhase.RINGING)"Your AI assistant is calling. Answer to speak, or decline." else "An AI voice conversation is active. Tap to return or End to stop.").setContentIntent(open(false)).setOngoing(true).setOnlyAlertOnce(true).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setCategory(NotificationCompat.CATEGORY_CALL)
        if(s.phase==AiCallPhase.RINGING)b.addAction(android.R.drawable.sym_action_call,"Answer",open(true))
        b.addAction(android.R.drawable.ic_menu_close_clear_cancel,if(s.phase==AiCallPhase.RINGING)"Decline" else "End",end)
        return b.build()
    }
        fun showFallback(context:Context,s:AiCallState):Boolean=runCatching{
            val manager=context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL,"Jarvis AI calls",NotificationManager.IMPORTANCE_HIGH).apply{setSound(null,null)})
            if(!manager.areNotificationsEnabled() || Build.VERSION.SDK_INT>=33 && androidx.core.content.ContextCompat.checkSelfPermission(context,android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return@runCatching false
            manager.notify(NOTIFICATION,buildNotification(context,s));true
        }.getOrDefault(false)
        const val CHANNEL="jarvis_ai_calls_v1";const val NOTIFICATION=6930
        private var current=WeakReference<AiCallService>(null)
        fun silenceRinging(){current.get()?.silence()}
    }
}
class AiCallActionReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){(context.applicationContext as JarvisApplication).runtime.endAiCall(intent.getStringExtra("call_id").orEmpty(),"ENDED_BY_USER")}}
