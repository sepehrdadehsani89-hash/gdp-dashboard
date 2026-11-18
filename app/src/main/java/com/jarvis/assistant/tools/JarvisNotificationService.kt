package com.jarvis.assistant.tools

import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.jarvis.assistant.notifications.*

/** Explicit local previews from per-source consent; no history collection or cloud upload. */
class JarvisNotificationService:NotificationListenerService(){
    override fun onCreate(){super.onCreate();instance=this}
    override fun onListenerConnected(){instance=this;connected=true;changed()}
    override fun onListenerDisconnected(){connected=false;changed()}
    override fun onDestroy(){if(instance===this){connected=false;instance=null;changed()};super.onDestroy()}
    override fun onNotificationPosted(sbn:StatusBarNotification?){changed()}
    override fun onNotificationRemoved(sbn:StatusBarNotification?){changed()}
    companion object {
        private val signal=kotlinx.coroutines.flow.MutableStateFlow(0L)
        val changes:kotlinx.coroutines.flow.StateFlow<Long> = signal
        @Synchronized fun changed(){signal.value=signal.value+1}
        @Volatile var connected=false;private set
        @Volatile private var instance:JarvisNotificationService?=null
        fun read(context:Context,source:String?=null):NotificationSnapshot {
            fun state(status:NotificationReadStatus)=NotificationAwarenessPolicy.result(status)
            fun unlocked()=context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked==false
            if(!unlocked())return state(NotificationReadStatus.LOCKED)
            val manager=context.getSystemService(android.app.NotificationManager::class.java)
            if(!manager.isNotificationListenerAccessGranted(android.content.ComponentName(context,JarvisNotificationService::class.java)))return state(NotificationReadStatus.PERMISSION_REQUIRED)
            val allowed=NotificationSourcePreferences(context).allowed()
            val targets=source?.let{NotificationAwarenessPolicy.sources[it] ?: emptySet()}
            if(allowed.isEmpty() || targets!=null && targets.none{it in allowed})return state(NotificationReadStatus.SOURCE_CONSENT_REQUIRED)
            val service=instance
            if(!connected || service==null)return state(NotificationReadStatus.DISCONNECTED)
            val rows=try{service.activeNotifications?.toList() ?: return state(NotificationReadStatus.READ_FAILED)}catch(_:Exception){return state(NotificationReadStatus.READ_FAILED)}
            val chosen=NotificationAwarenessPolicy.previews(rows.map{sbn->val n=sbn.notification;NotificationPreview(sbn.packageName,n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),sbn.postTime,n.visibility==Notification.VISIBILITY_SECRET,n.flags and Notification.FLAG_GROUP_SUMMARY!=0)},context.packageName,allowed,targets)
            // Recheck consent and Android access after the query; never publish after lock/revocation.
            if(!unlocked())return state(NotificationReadStatus.LOCKED)
            if(!manager.isNotificationListenerAccessGranted(android.content.ComponentName(context,JarvisNotificationService::class.java)))return state(NotificationReadStatus.PERMISSION_REQUIRED)
            val current=NotificationSourcePreferences(context).allowed()
            if(current.isEmpty() || targets!=null && targets.none{it in current})return state(NotificationReadStatus.SOURCE_CONSENT_REQUIRED)
            val previews=chosen.filter{it.packageId in current}.map{row->val label=runCatching{context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(row.packageId,0)).toString()}.getOrDefault(row.packageId);"${ToolPolicy.sensitiveText(label).take(80)}: ${row.title}. ${row.text}"}
            return NotificationAwarenessPolicy.result(NotificationReadStatus.AVAILABLE,previews)
        }
        /** Compatibility for existing callers. Null means unavailable, never an invented empty inbox. */
        fun snapshot(context:Context):List<String>?=read(context).let{if(it.status==NotificationReadStatus.AVAILABLE)it.previews else null}
    }
}
