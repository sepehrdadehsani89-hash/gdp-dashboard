package com.jarvis.assistant.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.R
import com.jarvis.assistant.core.AssistantReply
import com.jarvis.assistant.omnisystem.FeatureVault
import com.jarvis.assistant.rebirth.AppLaunchCoordinator
import kotlinx.coroutines.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class ReminderManager(private val context:Context,vault:FeatureVault,private val launches:AppLaunchCoordinator){
    val store=ReminderStore(vault)
    private val notifications=context.getSystemService(NotificationManager::class.java)
    init{notifications.createNotificationChannel(NotificationChannel(CHANNEL,"Jarvis reminders",NotificationManager.IMPORTANCE_DEFAULT))}
    fun allowed()=notifications.areNotificationsEnabled() && notifications.getNotificationChannel(CHANNEL)?.importance!=NotificationManager.IMPORTANCE_NONE && (Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
    private fun workName(id:String)="jarvis-owned-reminder-$id"
    private fun arm(row:OwnedReminder){val request=OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay(maxOf(0,row.at-System.currentTimeMillis()),TimeUnit.MILLISECONDS)
        .setInputData(workDataOf("id" to row.id,"revision" to row.revision)).build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(row.id),ExistingWorkPolicy.REPLACE,request).result.get(5,TimeUnit.SECONDS)}
    suspend fun create(title:String,at:Long,zone:String,id:String?=null):OwnedReminder=withContext(Dispatchers.IO+NonCancellable){
        check(allowed()){"Enable Jarvis reminder notifications first."}
        val row=store.create(title,at,zone,id)
        try{notifications.cancel(row.id.hashCode());arm(row);row}catch(e:Exception){store.mark(row.id,row.revision,ReminderStatus.SCHEDULE_FAILED);throw IllegalStateException("Reminder data was saved, but Android scheduling was not confirmed. Review it in Reminders; no delivery is promised.",e)}
    }
    suspend fun remove(id:String)=withContext(Dispatchers.IO+NonCancellable){val removed=store.remove(id);if(removed){WorkManager.getInstance(context).cancelUniqueWork(workName(id));notifications.cancel(id.hashCode())};removed}
    suspend fun restore()=withContext(Dispatchers.IO){
        val now=System.currentTimeMillis()
        store.list().forEach{row->currentCoroutineContext().ensureActive()
            if(row.status==ReminderStatus.DELIVERING)store.mark(row.id,row.revision,ReminderStatus.DELIVERY_UNCONFIRMED,ReminderStatus.DELIVERING)
            else if(row.status==ReminderStatus.SCHEDULED)store.ifCurrent(row.id,row.revision,ReminderStatus.SCHEDULED){
                if(now-row.at>86_400_000)store.mark(row.id,row.revision,ReminderStatus.MISSED,ReminderStatus.SCHEDULED)
                else try{arm(row)}catch(_:Exception){store.mark(row.id,row.revision,ReminderStatus.SCHEDULE_FAILED,ReminderStatus.SCHEDULED)}
            }
        }
    }
    @android.annotation.SuppressLint("MissingPermission")
    suspend fun deliver(id:String,revision:String):Boolean=withContext(Dispatchers.IO){
        if(!com.jarvis.assistant.modes.SystemModePolicy.remindersAllowed(com.jarvis.assistant.modes.SystemModes.current(context)))return@withContext false
        val row=store.list().firstOrNull{it.id==id} ?: return@withContext true
        if(row.revision==revision && row.status==ReminderStatus.SCHEDULED && row.at>System.currentTimeMillis())return@withContext false
        if(!ReminderPolicy.due(row,revision,System.currentTimeMillis()))return@withContext true
        if(System.currentTimeMillis()-row.at>86_400_000){store.mark(id,revision,ReminderStatus.MISSED,ReminderStatus.SCHEDULED);return@withContext true}
        if(!allowed()){store.mark(id,revision,ReminderStatus.PERMISSION_REQUIRED,ReminderStatus.SCHEDULED);return@withContext true}
        // Claim, post and completion share the store lock with removal, edits and clock recovery.
        store.ifCurrent(id,revision,ReminderStatus.SCHEDULED){
            store.mark(id,revision,ReminderStatus.DELIVERING,ReminderStatus.SCHEDULED)
            try{
                val intent=PendingIntent.getActivity(context,id.hashCode(),Intent(context,RemindersActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val notification=NotificationCompat.Builder(context,CHANNEL).setSmallIcon(R.drawable.ic_jarvis).setContentTitle("Jarvis reminder ready")
                    .setContentText("Unlock and open Jarvis to read your saved reminder.").setContentIntent(intent).setAutoCancel(true).setOnlyAlertOnce(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setCategory(NotificationCompat.CATEGORY_REMINDER).build()
                if(!allowed())store.mark(id,revision,ReminderStatus.PERMISSION_REQUIRED,ReminderStatus.DELIVERING)
                else {notifications.notify(id.hashCode(),notification);store.mark(id,revision,ReminderStatus.NOTIFIED,ReminderStatus.DELIVERING)}
            }catch(_:Exception){store.mark(id,revision,ReminderStatus.SCHEDULE_FAILED,ReminderStatus.DELIVERING)}
        }
        true
    }
    private fun setup()=launches.open(Intent(context,RemindersActivity::class.java),"Jarvis reminders").detail
    suspend fun route(text:String):AssistantReply? {
        val request=ReminderPolicy.parse(text) ?: return null
        if(context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked!=false)return AssistantReply("Unlock with Android before reading or changing reminders.")
        return try{when(request){
            ReminderIntent.NeedsDetails->AssistantReply("Choose an exact date/time and reminder text. Say ‘remind me in 20 minutes to take a break’ or ‘remind me tomorrow at 09:00 to review my notes’. Vague or repeated daylight-saving times need your choice. ${setup()}")
            ReminderIntent.ListReminders->{val rows=store.list().sortedBy{it.at};AssistantReply(if(rows.isEmpty())"You have no saved Jarvis reminders." else rows.take(20).joinToString("\n"){"${it.title} · ${format(it)} · ${it.status} · ID ${it.id}"}+"\nThese are Jarvis-owned reminders; delivery is approximate and may be delayed by Android/Xiaomi.")}
            is ReminderIntent.Create->{if(!allowed())AssistantReply("Allow Jarvis reminder notifications first. No reminder was scheduled. ${setup()}")else{val row=create(request.title,request.at,request.zone);AssistantReply("Saved reminder: ${row.title} · ${format(row)}. Android WorkManager accepted scheduling. Delivery is approximate and may be delayed by battery restrictions; this is not an exact alarm.")}}
            is ReminderIntent.Cancel->{val rows=store.list();val matches=rows.filter{it.id==request.query || it.title.equals(request.query,true)};if(matches.size!=1)AssistantReply("Use the exact reminder ID or a unique title; no reminder was removed. ${setup()}")else{remove(matches.single().id);AssistantReply("That Jarvis reminder was removed locally and cancellation was requested. No calendar event or separate alarm was deleted.")}}
        }}catch(e:CancellationException){throw e}catch(e:Exception){AssistantReply(e.message?.take(350) ?: "Reminders are unavailable. Existing reminder data was preserved.")}
    }
    companion object {const val CHANNEL="jarvis_owned_reminders"
        fun format(row:OwnedReminder)=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z").format(Instant.ofEpochMilli(row.at).atZone(ZoneId.of(row.zone)))
    }
}
class ReminderWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters){
    override suspend fun doWork():Result {
        val id=inputData.getString("id") ?: return Result.failure();val revision=inputData.getString("revision") ?: return Result.failure()
        return try{if((applicationContext as JarvisApplication).runtime.reminders.deliver(id,revision))Result.success()else Result.retry()}catch(e:CancellationException){throw e}catch(_:Exception){if(runAttemptCount<3)Result.retry()else Result.failure()}
    }
}
/** WorkManager handles reboot persistence; time changes explicitly recalculate queued delays. */
class ReminderTimeReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){if(intent.action !in setOf(Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED,Intent.ACTION_MY_PACKAGE_REPLACED))return
    val pending=goAsync();CoroutineScope(SupervisorJob()+Dispatchers.IO).launch{try{withTimeout(8000){(context.applicationContext as JarvisApplication).runtime.reminders.restore()}}catch(_:Exception){}finally{pending.finish()}}}}
