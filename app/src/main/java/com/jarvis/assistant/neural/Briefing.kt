package com.jarvis.assistant.neural

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.jarvis.assistant.core.*
import com.jarvis.assistant.data.JarvisStore
import com.jarvis.assistant.hud.HudPreferences
import com.jarvis.assistant.tools.DeviceTools
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object BriefingPolicy {
    fun quiet(hour:Int,start:Int,end:Int)=if(start==end)false else if(start<end)hour in start until end else hour>=start || hour<end
    fun next(now:ZonedDateTime,hour:Int,minute:Int):ZonedDateTime {val candidate=now.toLocalDate().atTime(hour,minute).atZone(now.zone);return if(candidate.isAfter(now))candidate else now.toLocalDate().plusDays(1).atTime(hour,minute).atZone(now.zone)}
}
class BriefingService(private val context:Context,private val store:JarvisStore,private val index:KnowledgeIndex,private val tools:DeviceTools) {
    private val prefs=NeuralPreferences(context)
    private val file=File(context.filesDir,"neural-briefing.json")
    suspend fun compose():AssistantReply=withTimeout(65_000){
        val s=prefs.state.value;val lang=store.settings.value.language
        val blocks=mutableListOf(ZonedDateTime.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy · HH:mm z",java.util.Locale.forLanguageTag(lang.tag))));val sources=mutableListOf<SourceLink>()
        if(s.briefingWeather){
            val city=HudPreferences(context).read().weatherCity
            if(city.isBlank())blocks+=lang.choose("Weather: configure a city first.","Clima: configura una ciudad.","هوا: ابتدا شهر را تنظیم کنید.")
            else try{val r=ForecastService().reply(city,0,lang);blocks+=r.text;sources+=r.sources}catch(e:CancellationException){throw e}catch(_:Exception){blocks+=lang.choose("Weather unavailable; no forecast was invented.","Clima no disponible.","پیش‌بینی هوا در دسترس نیست.")}
        }
        if(s.briefingCalendar){
            val reply=PhoneContext(context).route("my calendar today",lang)
            blocks+=reply?.text ?: lang.choose("Calendar unavailable.","Calendario no disponible.","تقویم در دسترس نیست.")
        }
        if(s.briefingTasks){val tasks=withContext(Dispatchers.IO){index.tasks()};blocks+=lang.choose("Saved tasks: ","Tareas guardadas: ","کارهای ذخیره‌شده: ")+tasks.size+if(tasks.isNotEmpty())"\n"+tasks.take(5).joinToString("\n"){it.second} else ""}
        if(s.briefingNews){val r=tools.news("news about ${s.newsTopic}",lang);blocks+=r.text;sources+=r.sources}
        val battery=context.getSystemService(android.os.BatteryManager::class.java).getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if(battery in 0..100)blocks+=lang.choose("Battery: $battery%.","Batería: $battery%.","باتری: $battery٪.")
        val result=AssistantReply(blocks.joinToString("\n\n"),sources,allowCloudSpeech=false)
        withContext(Dispatchers.IO){file.writeText(JSONObject().put("at",System.currentTimeMillis()).put("text",result.text).toString())}
        NeuralEvents.emit(NeuralEvent.BRIEFING_READY,"Briefing ready")
        result
    }
    fun last():String=runCatching{val j=JSONObject(file.readText());"${Instant.ofEpochMilli(j.getLong("at"))}\n${j.getString("text")}"}.getOrDefault("No briefing prepared yet.")
    fun clear(){file.delete()}
}
object BriefingScheduler {
    private const val NAME="jarvis_opt_in_briefing_v4"
    fun sync(context:Context,fromWorker:Boolean=false,keepExisting:Boolean=false){
        val settings=NeuralPreferences(context).state.value
        val work=WorkManager.getInstance(context)
        if(!settings.briefing){work.cancelUniqueWork(NAME);return}
        val now=ZonedDateTime.now();val next=BriefingPolicy.next(now,settings.briefingHour,settings.briefingMinute)
        val request=OneTimeWorkRequestBuilder<BriefingWorker>().setInitialDelay(Duration.between(now,next).toMillis(),TimeUnit.MILLISECONDS).addTag(NAME).build()
        work.enqueueUniqueWork(NAME,if(fromWorker)ExistingWorkPolicy.APPEND_OR_REPLACE else if(keepExisting)ExistingWorkPolicy.KEEP else ExistingWorkPolicy.REPLACE,request)
    }
}
class BriefingWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){
    override suspend fun doWork():Result{
        val s=NeuralPreferences(applicationContext).state.value
        if(!s.briefing)return Result.success()
        try{
            val now=ZonedDateTime.now()
            if(!BriefingPolicy.quiet(now.hour,s.quietStart,s.quietEnd)){
                val runtime=(applicationContext as com.jarvis.assistant.JarvisApplication).runtime
                runtime.briefing.compose()
                val granted=Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(applicationContext,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
                if(NeuralPreferences(applicationContext).state.value.briefing && granted && NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()){
                    val manager=applicationContext.getSystemService(NotificationManager::class.java)
                    manager.createNotificationChannel(NotificationChannel("jarvis_briefing_v4","Jarvis daily briefing",NotificationManager.IMPORTANCE_DEFAULT))
                    val intent=Intent(applicationContext,NeuralSettingsActivity::class.java)
                    val open=PendingIntent.getActivity(applicationContext,4001,intent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    val lang=runtime.store.settings.value.language
                    val notification=NotificationCompat.Builder(applicationContext,"jarvis_briefing_v4").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Jarvis").setContentText(lang.choose("Your daily briefing is ready. Open Jarvis to read it.","Tu resumen diario está listo. Abre Jarvis para leerlo.","گزارش روزانه آماده است؛ برای خواندن جارویس را باز کنید.")).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setContentIntent(open).setAutoCancel(true).build()
                    NotificationManagerCompat.from(applicationContext).notify(4001,notification)
                }
            }
            return Result.success()
        }catch(e:CancellationException){throw e}catch(_:Exception){return Result.success()}
        finally{if(!isStopped)BriefingScheduler.sync(applicationContext,true)}
    }
}

class BriefingTimeReceiver:android.content.BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){if(intent.action in setOf(Intent.ACTION_TIMEZONE_CHANGED,Intent.ACTION_TIME_CHANGED))BriefingScheduler.sync(context)}
}
