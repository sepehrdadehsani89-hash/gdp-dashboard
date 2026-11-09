package com.jarvis.assistant.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jarvis.assistant.JarvisApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Android-scheduled, inexact reminders; always re-read actual battery state before an alert. */
class PowerReminderWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters) {
    override suspend fun doWork():Result=withContext(Dispatchers.Main.immediate){
        val runtime=(applicationContext as JarvisApplication).runtime
        runtime.hud.powerSource.start();runtime.hud.powerSource.refresh()
        delay(600)
        runtime.batteryAlerts.scheduledCheck()
        var waiting=0
        while(runtime.batteryAlerts.isPlaying && waiting<100){delay(250);waiting++}
        Result.success()
    }
    companion object {const val WORK="jarvis_overcharge_full_reminder"}
}

/** Only explicit immutable PendingIntents from Jarvis; no exported command/permission endpoint. */
class PowerReminderAction:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent?){
        val alerts=(context.applicationContext as JarvisApplication).runtime.batteryAlerts
        when(intent?.action){"dismiss"->alerts.dismissReminders();"snooze"->alerts.snoozeReminders()}
    }
}
