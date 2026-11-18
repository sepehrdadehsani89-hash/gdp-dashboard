package com.jarvis.assistant.modes

import android.content.Context
import com.jarvis.assistant.neural.AppMatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

data class SystemModeState(val silent:Boolean=false,val deep:Boolean=false,val wakeInSilent:Boolean=false,val keepReminders:Boolean=true)
enum class SystemModeCommand { SILENT,ACTIVE,DEEP,SETTINGS }
object SystemModePolicy {
    fun parse(raw:String):SystemModeCommand?=when(AppMatching.normalize(raw).removePrefix("hey jarvis ").removePrefix("jarvis ")){
        "activate silent mode","enter quiet mode","activate focus protocol","enter silent mode","activa el modo silencio","activa modo silencio","حالت ساکت را فعال کن"->SystemModeCommand.SILENT
        "deactivate silent mode","exit silent mode","return to active mode","desactiva el modo silencio","حالت ساکت را غیرفعال کن"->SystemModeCommand.ACTIVE
        "shut down your systems","deactivate jarvis completely","enter deep shutdown","apaga tus sistemas","خاموشی عمیق را فعال کن"->SystemModeCommand.DEEP
        "open silent mode settings","open deep shutdown settings","show system modes"->SystemModeCommand.SETTINGS
        else->null
    }
    fun wakeAllowed(s:SystemModeState)=!s.deep && (!s.silent || s.wakeInSilent)
    fun optionalActivityAllowed(s:SystemModeState)=!s.deep && !s.silent
    fun remindersAllowed(s:SystemModeState)=optionalActivityAllowed(s)||s.keepReminders
    fun privatePreviewsAllowed(s:SystemModeState)=!s.deep && !s.silent
    fun activationMillis(s:SystemModeState)=if(s.silent && s.wakeInSilent && !s.deep)4000L else 2100L
    fun animationElapsed(elapsed:Long,duration:Long)=(elapsed.coerceAtLeast(0).coerceAtMost(duration)*2100/duration.coerceAtLeast(1))
}
class SystemModes internal constructor(private val read:()->String?,private val write:(String)->Unit,private val recover:(String?,String)->Unit){
    constructor(context:Context):this({context.applicationContext.getSharedPreferences("jarvis_system_modes_v1",Context.MODE_PRIVATE).getString("state",null)},
        {check(context.applicationContext.getSharedPreferences("jarvis_system_modes_v1",Context.MODE_PRIVATE).edit().putString("state",it).commit()){"Mode preferences could not be saved."}},
        {old,value->check(context.applicationContext.getSharedPreferences("jarvis_system_modes_v1",Context.MODE_PRIVATE).edit().putString("last_corrupt_state",old).putString("state",value).commit())})
    private val initial=runCatching{load()};var storageError=initial.exceptionOrNull()?.let{"Mode preferences are unavailable; capture is blocked until you explicitly recover these preferences."};private set
    private val mutable=MutableStateFlow(initial.getOrDefault(SystemModeState(deep=true)));val state:StateFlow<SystemModeState> = mutable
    fun load():SystemModeState{val raw=read() ?: return SystemModeState();val j=JSONObject(raw);require(j.getInt("version")==1);return SystemModeState(j.getBoolean("silent"),j.getBoolean("deep"),j.getBoolean("wake"),j.getBoolean("reminders"))}
    private fun encoded(s:SystemModeState)=JSONObject().put("version",1).put("silent",s.silent).put("deep",s.deep).put("wake",s.wakeInSilent).put("reminders",s.keepReminders).toString()
    @Synchronized fun save(s:SystemModeState){load();write(encoded(s));mutable.value=s}
    @Synchronized fun silent(enabled:Boolean,wake:Boolean=state.value.wakeInSilent,reminders:Boolean=state.value.keepReminders){save(state.value.copy(silent=enabled,wakeInSilent=wake,keepReminders=reminders))}
    @Synchronized fun shutdown(){save(state.value.copy(deep=true))}
    @Synchronized fun reopen(){save(state.value.copy(deep=false))}
    @Synchronized fun recoverExplicitly(){val s=SystemModeState();recover(read(),encoded(s));storageError=null;mutable.value=s}
    companion object{
        fun current(context:Context)=runCatching{val raw=context.applicationContext.getSharedPreferences("jarvis_system_modes_v1",Context.MODE_PRIVATE).getString("state",null);if(raw==null)SystemModeState()else{val j=JSONObject(raw);require(j.getInt("version")==1);SystemModeState(j.getBoolean("silent"),j.getBoolean("deep"),j.getBoolean("wake"),j.getBoolean("reminders"))}}.getOrDefault(SystemModeState(deep=true))
    }
}

object ModePresentation {
    fun hud(h:com.jarvis.assistant.hud.HudState,mode:SystemModeState,quality:String?=null):com.jarvis.assistant.hud.HudState {
        val s=h.settings
        val profile=when{mode.silent || mode.deep->"saver";quality=="saver"->"saver";quality=="high"->"cinematic";quality=="balanced"->"balanced";else->s.animationProfile}
        if(!mode.silent && !mode.deep && quality==null)return h
        val v=s.sentinel
        return h.copy(settings=s.copy(animationProfile=profile,reducedMotion=s.reducedMotion||mode.deep,
            animationIntensity=if(mode.silent)s.animationIntensity*.3f else s.animationIntensity,
            sentinel=v.copy(brightness=if(mode.silent)maxOf(.15f,v.brightness*.4f)else v.brightness,ambientGlow=if(mode.silent)v.ambientGlow*.2f else v.ambientGlow,particleDensity=if(mode.silent)v.particleDensity*.15f else v.particleDensity,animationSpeed=if(mode.deep)0f else if(mode.silent)minOf(v.animationSpeed,.35f)else v.animationSpeed)))
    }
}
