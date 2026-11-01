package com.jarvis.assistant.tools

import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Read only on explicit request. Sensitive packages, codes and lockscreen content excluded. */
class JarvisNotificationService : NotificationListenerService() {
    override fun onListenerConnected() { connected = true }
    override fun onListenerDisconnected() { connected = false }
    override fun onDestroy() { connected = false; if (instance === this) instance = null; super.onDestroy() }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = Unit
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = Unit

    companion object {
        @Volatile var connected: Boolean = false
            private set

        fun snapshot(context: Context): List<String>? {
            if (context.getSystemService(KeyguardManager::class.java).isDeviceLocked) return null
            val component = android.content.ComponentName(context, JarvisNotificationService::class.java)
            val manager = context.getSystemService(android.app.NotificationManager::class.java)
            if (!manager.isNotificationListenerAccessGranted(component)) return null
            val notifications = runCatching { instance?.activeNotifications }.getOrNull() ?: return emptyList()
            return notifications.asSequence()
                .filter { !ToolPolicy.protectedPackage(it.packageName) && it.packageName != context.packageName }
                .filter { it.notification.visibility != Notification.VISIBILITY_SECRET }
                .sortedByDescending { it.postTime }
                .take(8)
                .mapNotNull { sbn ->
                    val extras = sbn.notification.extras
                    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
                    if (title.isBlank() && text.isBlank()) null else {
                        val label = runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(sbn.packageName, 0)).toString() }.getOrDefault(sbn.packageName)
                        ToolPolicy.sensitiveText("$label: $title. $text").take(300)
                    }
                }.toList()
        }

        @Volatile private var instance: JarvisNotificationService? = null
    }

    override fun onCreate() { super.onCreate(); instance = this }
}
