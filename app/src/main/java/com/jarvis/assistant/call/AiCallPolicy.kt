package com.jarvis.assistant.call

import com.jarvis.assistant.neural.AppMatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class AiCallPhase { IDLE, RINGING, CONNECTING, LISTENING, THINKING, SPEAKING, ENDED, ERROR }
enum class AiCallSource { USER, CHECK_IN, BATTERY_20 }
enum class AiCallCommand { START, ANSWER, END }
data class AiCallState(val id:String="",val phase:AiCallPhase=AiCallPhase.IDLE,val source:AiCallSource=AiCallSource.USER,val referenceId:String?=null,val referenceRevision:String?=null,val ringAt:Long=0,val answeredAt:Long?=null,val changedAt:Long=0,val activityAt:Long=0,val wakeWasRunning:Boolean=false,val reason:String="",val audioConnected:Boolean=false){
    val active get()=phase !in setOf(AiCallPhase.IDLE,AiCallPhase.ENDED,AiCallPhase.ERROR)
    val connected get()=active && answeredAt!=null
}
object AiCallPolicy {
    fun command(text:String):AiCallCommand? {
        val n=AppMatching.normalize(text).removePrefix("hey jarvis ").removePrefix("oye jarvis ").removePrefix("jarvis ").removePrefix("please ")
        return when(n){
            "call me","call me now","let s do a call","lets do a call","let us do a call","start a call","start jarvis call","open jarvis call","llamame","llamame ahora","hagamos una llamada"->AiCallCommand.START
            "answer jarvis call","answer the call","answer call","pick up","contesta la llamada"->AiCallCommand.ANSWER
            "end jarvis call","end the call","end call","hang up","decline call","reject call","cancel call","cuelga","termina la llamada"->AiCallCommand.END
            else->null
        }
    }
    fun greeting(spanish:Boolean)=if(spanish)"Hola, señor. Estamos conectados. ¿En qué puedo ayudarte?" else "Hello sir, we're connected. What can I help you with?"
    fun timeout(s:AiCallState,now:Long):String?=when {
        !s.active->null
        now<s.ringAt->"CLOCK_CHANGED"
        s.phase==AiCallPhase.RINGING && now-s.ringAt>=45_000->"MISSED"
        s.answeredAt!=null && now-s.answeredAt>=600_000->"CALL_LIMIT_REACHED"
        s.answeredAt!=null && !s.audioConnected && now-s.answeredAt>=30_000->"AUDIO_CONNECTION_TIMEOUT"
        s.phase==AiCallPhase.CONNECTING && now-s.changedAt>=30_000->"MICROPHONE_TIMEOUT"
        s.phase==AiCallPhase.LISTENING && now-s.activityAt>=45_000->"QUIET_TIMEOUT"
        else->null
    }
}
/** In-process call lifecycle. Old notifications cannot answer/end a newer call; no boot restart. */
class AiCallController(private val now:()->Long){
    private val mutable=MutableStateFlow(AiCallState());val state=mutable.asStateFlow()
    @Synchronized fun start(source:AiCallSource,wakeWasRunning:Boolean,referenceId:String?=null,referenceRevision:String?=null):AiCallState? {
        if(mutable.value.active)return null
        val at=now();return AiCallState(UUID.randomUUID().toString(),AiCallPhase.RINGING,source,referenceId,referenceRevision,at,null,at,at,wakeWasRunning).also{mutable.value=it}
    }
    @Synchronized fun answer(id:String):Boolean {
        val s=mutable.value;if(s.id!=id || s.phase!=AiCallPhase.RINGING || AiCallPolicy.timeout(s,now())!=null)return false
        val at=now();mutable.value=s.copy(phase=AiCallPhase.CONNECTING,answeredAt=at,changedAt=at,activityAt=at);return true
    }
    @Synchronized fun stage(id:String,phase:AiCallPhase){
        val s=mutable.value;if(s.id!=id || !s.connected || phase !in setOf(AiCallPhase.CONNECTING,AiCallPhase.LISTENING,AiCallPhase.THINKING,AiCallPhase.SPEAKING) || s.phase==phase)return
        val at=now();mutable.value=s.copy(phase=phase,changedAt=at,audioConnected=s.audioConnected || phase in setOf(AiCallPhase.SPEAKING,AiCallPhase.LISTENING),activityAt=if(phase==AiCallPhase.LISTENING || phase==AiCallPhase.SPEAKING)at else s.activityAt)
    }
    @Synchronized fun activity(){val s=mutable.value;if(s.connected)mutable.value=s.copy(activityAt=now())}
    @Synchronized fun finish(id:String,reason:String,error:Boolean=false):AiCallState? {
        val s=mutable.value;if(s.id!=id || !s.active)return null
        mutable.value=s.copy(phase=if(error)AiCallPhase.ERROR else AiCallPhase.ENDED,changedAt=now(),reason=reason);return s
    }
}
