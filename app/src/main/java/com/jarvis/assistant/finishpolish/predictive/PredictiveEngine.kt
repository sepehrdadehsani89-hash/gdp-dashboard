package com.jarvis.assistant.finishpolish.predictive

import android.Manifest
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.CancellationSignal
import android.os.PowerManager
import android.os.SystemClock
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.jarvis.assistant.omnisystem.FeatureVault
import com.jarvis.assistant.power.PowerReading
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZoneId

/** Process-scoped, bounded read-only suggestions. Never executes its proposed actions. */
class PredictiveEngine(private val context:Context,private val contextVault:FeatureVault){
    val store=PredictiveStore(contextVault)
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val mutable=MutableStateFlow<FutureSuggestion?>(null);val state=mutable.asStateFlow()
    private val statusMutable=MutableStateFlow("Predictive suggestions are off until explicitly enabled")
    val status=statusMutable.asStateFlow()
    private var refresh:Job?=null
    private var lastRefresh=0L
    init{scope.launch{store.state.collect{p->
        if(!p.config.enabled){refresh?.cancel();mutable.value=null;statusMutable.value="Predictive suggestions are off"}
        else mutable.value?.let{if(!sourceEnabled(it))mutable.value=null}
    }}}
    fun refresh(power:PowerReading,idle:Boolean){
        val config=store.state.value.config
        if(!store.available){mutable.value=null;statusMutable.value="Predictive private storage is unavailable; existing data preserved";return}
        if(!config.enabled){mutable.value=null;return}
        val e=environment(idle)
        if(!e.unlocked || !e.idle || e.dnd || e.silent || e.music || e.powerSave || PredictivePolicy.quiet(config,e)){mutable.value=null;statusMutable.value=PredictivePolicy.suppression(config,e,store.state.value.history) ?: "Suggestion hidden";return}
        mutable.value?.let{s->
            val batteryInvalid=s.source==SuggestionSource.BATTERY && (power.battery.percent !in 0..10 || power.charging || SystemClock.elapsedRealtime()-power.observedAt !in 0..PredictivePolicy.FRESH_BATTERY_MS)
            val calendarStale=s.source==SuggestionSource.CALENDAR && e.now-s.observedAt>60_000
            if(s.expiresAt<=e.now || !sourceEnabled(s) || batteryInvalid || calendarStale)mutable.value=null else return
        }
        if(refresh?.isActive==true || SystemClock.elapsedRealtime()-lastRefresh in 0 until 15_000)return
        lastRefresh=SystemClock.elapsedRealtime()
        refresh=scope.launch{
            val profile=store.state.value
            if(!profile.config.enabled)return@launch
            val candidates=mutableListOf<FutureSuggestion>()
            val level=power.battery.percent
            val elapsedAge=SystemClock.elapsedRealtime()-power.observedAt
            if(power.charging || level!=null && level>=25){
                val history=store.state.value.history
                if("battery-low" in history.seen)store.history(history.copy(seen=history.seen-"battery-low"))
            }
            if(profile.config.battery){
                if(elapsedAge in 0..PredictivePolicy.FRESH_BATTERY_MS){PredictivePolicy.battery(level,power.charging,e.now-elapsedAge,e.now)?.let{candidates+=it}}
            }
            profile.routines.mapNotNullTo(candidates){PredictivePolicy.routine(it,e.now)}
            if(profile.config.calendar){
                if(ContextCompat.checkSelfPermission(context,Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED){
                    try{candidates+=calendar(e.now)}catch(_:SecurityException){statusMutable.value="Calendar permission was revoked; no calendar suggestion fabricated"}catch(e:CancellationException){throw e}catch(_:Exception){statusMutable.value="Calendar source unavailable; local routines and battery remain independent"}
                }else statusMutable.value="Calendar consent is enabled but Android READ_CALENDAR is not granted"
            }
            val latest=environment(idle)
            if(store.state.value.config!=profile.config)return@launch
            val selected=PredictivePolicy.choose(candidates,profile.config,latest,store.state.value.history)
            if(selected!=null){store.history(PredictivePolicy.shown(selected,profile.config,latest,store.state.value.history));mutable.value=selected;statusMutable.value="One suggestion available; no action executed"}
            else if(statusMutable.value.startsWith("One suggestion"))statusMutable.value="No fresh eligible suggestion"
        }
    }
    private fun environment(idle:Boolean):SuggestionEnvironment{
        val keyguard=context.getSystemService(KeyguardManager::class.java);val audio=context.getSystemService(AudioManager::class.java)
        return SuggestionEnvironment(System.currentTimeMillis(),ZoneId.systemDefault().id,!keyguard.isDeviceLocked && !keyguard.isKeyguardLocked,idle && com.jarvis.assistant.rebirth.AppVisibility.visible,
            context.getSystemService(NotificationManager::class.java).currentInterruptionFilter!=NotificationManager.INTERRUPTION_FILTER_ALL,
            audio.ringerMode!=AudioManager.RINGER_MODE_NORMAL,audio.isMusicActive,context.getSystemService(PowerManager::class.java).isPowerSaveMode)
    }
    private fun sourceEnabled(s:FutureSuggestion):Boolean=store.state.value.config.let{it.enabled && when(s.source){SuggestionSource.BATTERY->it.battery;SuggestionSource.CALENDAR->it.calendar && ContextCompat.checkSelfPermission(context,Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED;SuggestionSource.ROUTINE->store.state.value.routines.any{r->r.enabled && s.sourceKey=="routine:${r.id}" && PredictivePolicy.occurrence(r,System.currentTimeMillis())==s.dueAt && (r.recipeTrigger.ifBlank{null}==s.action?.target)}}}
    fun dismiss(id:String){val s=mutable.value?.takeIf{it.id==id} ?: return;store.history(store.state.value.history.let{it.copy(dismissed=it.dismissed+1)});mutable.value=null;statusMutable.value="Suggestion dismissed; no action executed"}
    fun snooze(id:String){val s=mutable.value?.takeIf{it.id==id} ?: return;val h=store.state.value.history;store.history(h.copy(seen=h.seen-s.id,snoozed=h.snoozed+(s.sourceKey to (System.currentTimeMillis()+30*60_000))));mutable.value=null;statusMutable.value="Snoozed for at least 30 minutes; expiry and frequency rules still apply"}
    fun never(id:String){val s=mutable.value?.takeIf{it.id==id} ?: return;val h=store.state.value.history;require(h.never.size<100 || s.sourceKey in h.never);store.history(h.copy(never=h.never+s.sourceKey));mutable.value=null;statusMutable.value="This source is suppressed until you restore suppressed sources"}
    /** Returns a reviewed proposal only. Root registry must still preview/authorize/verify it. */
    fun accept(id:String):SuggestionAction? {
        val s=mutable.value?.takeIf{it.id==id} ?: return null
        val e=environment(true)
        if(!sourceEnabled(s) || s.expiresAt<=e.now || !e.unlocked || e.dnd || e.silent || e.music || e.powerSave || PredictivePolicy.quiet(store.state.value.config,e)){mutable.value=null;statusMutable.value="Suggestion is no longer eligible; no action executed";return null}
        if(s.action?.kind==SuggestionActionKind.REVIEW_RECIPE){
            val recipe=runCatching{com.jarvis.assistant.finishpolish.learning.LearningStore(contextVault).match(s.action.target)}.getOrNull()
            if(recipe==null || recipe.id!=s.action.referenceId){mutable.value=null;statusMutable.value="The approved recipe was changed or removed. Review the routine before trying again.";return null}
        }
        store.history(store.state.value.history.let{it.copy(accepted=it.accepted+1)});mutable.value=null
        statusMutable.value=if(s.action==null)"Reminder acknowledged; no action was configured" else "Suggestion accepted for review; execution is not yet verified"
        return s.action
    }
    fun hide(){refresh?.cancel();mutable.value=null}
    fun disable(){store.configure(store.state.value.config.copy(enabled=false));refresh?.cancel();mutable.value=null}
    fun forgetAfterUserErase(){hide();store.resetAfterUserErase();statusMutable.value="Predictive data erased; suggestions off"}
    fun reset(){refresh?.cancel();store.clear();mutable.value=null;statusMutable.value="Predictive settings and history deleted; suggestions off"}
    private suspend fun calendar(now:Long):List<FutureSuggestion> = withContext(Dispatchers.IO){
        val cancellation=CancellationSignal();val job=currentCoroutineContext()[Job];val handle=job?.invokeOnCompletion{if(it is CancellationException)cancellation.cancel()};val timeout=scope.launch{delay(3000);cancellation.cancel()}
        try{
            val uri=CalendarContract.Instances.CONTENT_URI.buildUpon().appendPath(now.toString()).appendPath((now+20*60_000).toString()).build()
            val columns=arrayOf(CalendarContract.Instances.EVENT_ID,CalendarContract.Instances.BEGIN,CalendarContract.Instances.END,CalendarContract.Instances.TITLE)
            val result=mutableListOf<FutureSuggestion>()
            context.contentResolver.query(uri,columns,null,null,CalendarContract.Instances.BEGIN+" ASC",cancellation)?.use{cursor->
                while(cursor.moveToNext() && result.size<5){
                    val id=cursor.getLong(0);val begin=cursor.getLong(1);val end=cursor.getLong(2);val title=cursor.getString(3).orEmpty().take(100).filterNot(Char::isISOControl).ifBlank{"Calendar event"}
                    if(begin !in now..(now+20*60_000) || end<begin)continue
                    result+=FutureSuggestion("calendar:$id:$begin",SuggestionSource.CALENDAR,"calendar:$id",title,"Source: opted-in Android calendar event; starts ${java.time.Instant.ofEpochMilli(begin).atZone(ZoneId.systemDefault())}. Calendar title stays local.",now,begin,minOf(end,begin+300_000),SuggestionAction(SuggestionActionKind.REVIEW_CALENDAR,id.toString()),announcement="Sir, an authorized calendar event starts soon. Would you like to review it?")
                }
            };result
        }finally{timeout.cancel();handle?.dispose();cancellation.cancel()}
    }
}
