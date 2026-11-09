package com.jarvis.assistant.sentinel

import com.jarvis.assistant.core.AssistantPhase
import com.jarvis.assistant.hud.AuthorizationStatus
import com.jarvis.assistant.hud.BatteryReading
import com.jarvis.assistant.hud.HudSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class SentinelSettings(
    val reactorScale:Float=1f, val animationSpeed:Float=1f, val particleDensity:Float=.45f,
    val detail:String="balanced", val brightness:Float=.65f, val ambientGlow:Float=.45f,
    val cinematicWallpaper:Boolean=false, val wallpaperClock:Boolean=true, val wallpaperBattery:Boolean=true,
    val wallpaperWeather:Boolean=false, val chargingEffects:Boolean=true, val effectsVolume:Float=.35f,
    val activationSound:String="chime", val shutdownSound:String="sweep", val conciseReplies:Boolean=true,
    val returnOnTimeout:Boolean=true,
    val activationVolume:Float=.35f,
) {
    fun sanitized()=copy(reactorScale=reactorScale.safe(.65f,1.1f,1f),animationSpeed=animationSpeed.safe(0f,2f,1f),
        particleDensity=particleDensity.safe(0f,1f,.45f),brightness=brightness.safe(.15f,1f,.65f),ambientGlow=ambientGlow.safe(0f,1f,.45f),effectsVolume=effectsVolume.safe(0f,1f,.35f),activationVolume=activationVolume.safe(0f,1f,.35f),
        detail=detail.takeIf { it in listOf("minimal","balanced","cinematic") } ?: "balanced",
        activationSound=activationSound.takeIf { it in listOf("chime","soft","deep") } ?: "chime",
        shutdownSound=shutdownSound.takeIf { it in listOf("sweep","soft","silent") } ?: "sweep")
    private fun Float.safe(min:Float,max:Float,fallback:Float)=if(isFinite())coerceIn(min,max) else fallback
}

enum class SentinelMode { DORMANT, ACTIVATING, LISTENING, PROCESSING, SPEAKING, AUTHORIZATION_REQUIRED, ERROR, RETURNING_TO_WALLPAPER }
data class SentinelVisualState(val mode:SentinelMode=SentinelMode.DORMANT,val activationId:Long=0,val activatedAt:Long=0,val returningAt:Long=0,val changedAt:Long=0)
data class Awakening(val core:Float,val rings:Float,val panels:Float,val pulse:Float,val complete:Boolean)

/** Shared timing and geometry, rendered by both native Canvas wallpaper and Compose. */
object ReactorGeometry {
    data class Vector(val x:Float,val y:Float)
    val ticks:List<Vector> = List(120){val rad=it*PI*2/120;Vector(cos(rad).toFloat(),sin(rad).toFloat())}
    val ringScales=listOf(.44f,.56f,.68f,.80f,.92f)
    val arcScales=listOf(.95f,.80f,.65f)
    fun speed(settings:HudSettings,mode:SentinelMode,executing:Boolean=false)=settings.sentinel.animationSpeed*settings.animationIntensity*when{executing->8f;mode==SentinelMode.DORMANT->1.6f;mode==SentinelMode.AUTHORIZATION_REQUIRED->.3f;mode==SentinelMode.PROCESSING->6.5f;mode==SentinelMode.LISTENING->6f;mode==SentinelMode.SPEAKING->4.8f;else->1.3f}
    fun angle(elapsed:Long,speed:Float,index:Int):Float = ((elapsed.coerceAtLeast(0)*.001 * 5 * speed * if(index%2==0)1 else -1)%360).toFloat()
    fun awakening(elapsed:Long):Awakening {
        fun ease(start:Long,length:Long):Float { val x=((elapsed-start).toFloat()/length).coerceIn(0f,1f);return x*x*(3-2*x) }
        return Awakening(ease(0,450),ease(350,850),ease(1100,650),if(elapsed in 1650..2100)sin((elapsed-1650)*PI/450).toFloat().coerceAtLeast(0f) else 0f,elapsed>=2100)
    }
}

/** Accumulate only visible frame deltas. Resume and phase changes never jump or reset rotation. */
class ReactorFrameClock {
    private var previous:Long?=null
    var angle:Float=0f;private set
    fun pause(){previous=null}
    fun frame(time:Long,speed:Float):Float {
        val delta=previous?.let{(time-it).coerceIn(0L,250L)} ?: 0L
        previous=time
        if(speed.isFinite())angle=(angle+delta*.005f*speed.coerceAtLeast(0f))%360f
        return angle
    }
}

/** Activation never holds the microphone; rendering resolves its own choreography. */
class ReactorAnimationController(private val now:()->Long) {
    private val mutable=MutableStateFlow(SentinelVisualState())
    val state=mutable.asStateFlow()
    private var target=SentinelMode.DORMANT
    @Synchronized fun activate(){val t=now();val old=mutable.value;if(old.mode==SentinelMode.ACTIVATING && t-old.activatedAt<2100)return;mutable.value=old.copy(mode=SentinelMode.ACTIVATING,activationId=old.activationId+1,activatedAt=t,returningAt=0,changedAt=t)}
    @Synchronized fun update(phase:AssistantPhase,awaiting:Boolean,authorization:AuthorizationStatus?){
        target=when {
            authorization in listOf(AuthorizationStatus.AUTHORIZATION_REQUIRED,AuthorizationStatus.AWAITING_CODEX,AuthorizationStatus.IDENTITY_CONFIRMATION)->SentinelMode.AUTHORIZATION_REQUIRED
            phase==AssistantPhase.ERROR->SentinelMode.ERROR
            phase==AssistantPhase.SPEAKING->SentinelMode.SPEAKING
            phase==AssistantPhase.THINKING->SentinelMode.PROCESSING
            awaiting || phase==AssistantPhase.LISTENING->SentinelMode.LISTENING
            else->SentinelMode.DORMANT
        }
        settle()
    }
    @Synchronized fun settle(){
        val old=mutable.value;val t=now()
        if(old.mode==SentinelMode.ACTIVATING && t-old.activatedAt<2100 && target in listOf(SentinelMode.DORMANT,SentinelMode.LISTENING))return
        if(old.mode==SentinelMode.RETURNING_TO_WALLPAPER && t-old.returningAt<600 && target==SentinelMode.DORMANT)return
        if(target==SentinelMode.DORMANT && old.mode !in listOf(SentinelMode.DORMANT,SentinelMode.RETURNING_TO_WALLPAPER)) { dismiss();return }
        if(old.mode!=target)mutable.value=old.copy(mode=target,changedAt=t)
    }
    @Synchronized fun dismiss(){target=SentinelMode.DORMANT;val old=mutable.value;if(old.mode==SentinelMode.DORMANT)return;mutable.value=old.copy(mode=SentinelMode.RETURNING_TO_WALLPAPER,returningAt=now(),changedAt=now())}
}

object WallpaperRenderPolicy {
    fun fps(visible:Boolean,surface:Boolean,interactive:Boolean,settings:HudSettings,battery:BatteryReading,powerSaver:Boolean,systemMotion:Boolean,mode:SentinelMode,restricted:Boolean=false,thermal:Int?=null,refreshRate:Float=60f):Int {
        if(!visible || !surface || !interactive)return 0
        val target=CoreMotionPolicy.fps(settings,battery,powerSaver,systemMotion,mode,restricted,thermal,refreshRate)
        if(target==0)return 0
        if((battery.percent ?:100)<20)return minOf(8,target)
        if(mode in setOf(SentinelMode.ERROR,SentinelMode.AUTHORIZATION_REQUIRED))return minOf(12,target)
        if(mode==SentinelMode.DORMANT && !battery.charging && !battery.full)return minOf(12,target)
        return target
    }
}
