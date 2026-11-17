package com.jarvis.assistant.voice

enum class VoiceStage { IDLE, WAKE_DETECTED, GREETING, WAITING_FOR_MIC, LISTENING, FINALIZING, THINKING, PROCESSING, SPEAKING, COOLDOWN, READY, FOLLOW_UP_LISTENING, ERROR }
enum class VoiceReason { WAKE, GREETING_REQUESTED, GREETING_DUPLICATE, CAPTURE_ARMED, CAPTURE_READY, FINALIZING, FINAL_RECEIVED, ASR_FAILED, OWNER_CHECK, OWNER_MATCHED, OWNER_REJECTED, BACKEND_FAILED, AUDIO_PREPARING, AUDIO_STARTED, AUDIO_COMPLETED, AUDIO_FAILED, COOLDOWN_COMPLETE, CANCELLED, INACTIVE }
data class VoiceTransition(val at:Long,val generation:Long,val stage:VoiceStage,val reason:VoiceReason,val characters:Int=0)

/** Authoritative half-duplex turn ownership; UI phases are presentation only.
 * No transcripts, audio, credentials or provider bodies are retained in diagnostics. */
class VoiceSessionStateMachine(private val now:()->Long,private val report:(VoiceTransition)->Unit={}){
    companion object {
        private val tickets=java.util.concurrent.atomic.AtomicLong()
        fun nextWakeTicket()=tickets.incrementAndGet()
    }
    private var generation=0L
    private var stage=VoiceStage.IDLE
    private var speechReserved=false
    private var cooldownUntil=0L
    private var lastGreeting=Long.MIN_VALUE
    private var recognitionFailures=0
    private val events=ArrayDeque<VoiceTransition>()
    @Synchronized fun snapshot()=VoiceTransition(now(),generation,stage,events.lastOrNull()?.reason ?: VoiceReason.INACTIVE)
    @Synchronized fun recent()=events.toList()
    @Synchronized fun blocksRecognition()=speechReserved || now()<cooldownUntil || stage in setOf(VoiceStage.FINALIZING,VoiceStage.THINKING,VoiceStage.PROCESSING)
    @Synchronized fun recognitionAllowed(token:Long)=token==generation && !blocksRecognition()
    @Synchronized fun cooldownRemaining(token:Long)=if(token==generation)(cooldownUntil-now()).coerceAtLeast(0L) else 0L
    private fun emit(next:VoiceStage,reason:VoiceReason,characters:Int=0){
        stage=next
        val event=VoiceTransition(now(),generation,next,reason,characters.coerceIn(0,8000))
        if(events.size>=64)events.removeFirst();events.addLast(event);report(event)
    }
    private fun current(token:Long):Boolean {
        if(token<generation)return false
        if(token>generation){speechReserved=false;cooldownUntil=0L}
        generation=token;return true
    }
    @Synchronized fun wake(token:Long){if(current(token) && !blocksRecognition()){recognitionFailures=0;emit(VoiceStage.WAKE_DETECTED,VoiceReason.WAKE)}}
    @Synchronized fun claimGreeting(ticket:Long):Boolean {
        if(blocksRecognition())return false
        if(ticket<=lastGreeting){emit(stage,VoiceReason.GREETING_DUPLICATE);return false}
        lastGreeting=ticket;return true
    }
    @Synchronized fun greeting(token:Long){if(current(token) && !blocksRecognition())emit(VoiceStage.GREETING,VoiceReason.GREETING_REQUESTED)}
    @Synchronized fun armCapture(token:Long){if(current(token) && !blocksRecognition())emit(VoiceStage.WAITING_FOR_MIC,VoiceReason.CAPTURE_ARMED)}
    @Synchronized fun captureReady(token:Long,followUp:Boolean):Boolean {
        if(!current(token) || blocksRecognition() || stage in setOf(VoiceStage.GREETING,VoiceStage.SPEAKING,VoiceStage.THINKING,VoiceStage.PROCESSING))return false
        emit(if(followUp)VoiceStage.FOLLOW_UP_LISTENING else VoiceStage.LISTENING,VoiceReason.CAPTURE_READY);return true
    }
    @Synchronized fun finalizing(token:Long){if(current(token) && !blocksRecognition())emit(VoiceStage.FINALIZING,VoiceReason.FINALIZING)}
    @Synchronized fun recognized(token:Long,length:Int){if(current(token) && !speechReserved && now()>=cooldownUntil && length>0){recognitionFailures=0;emit(VoiceStage.FINALIZING,VoiceReason.FINALIZING,length);emit(VoiceStage.THINKING,VoiceReason.FINAL_RECEIVED,length)}}
    @Synchronized fun ownerCheck(token:Long){if(current(token) && !speechReserved && now()>=cooldownUntil)emit(VoiceStage.THINKING,VoiceReason.OWNER_CHECK)}
    @Synchronized fun ownerResult(token:Long,matched:Boolean){if(current(token) && !speechReserved && now()>=cooldownUntil)emit(if(matched)VoiceStage.THINKING else VoiceStage.ERROR,if(matched)VoiceReason.OWNER_MATCHED else VoiceReason.OWNER_REJECTED)}
    @Synchronized fun asrFailed(token:Long):Boolean {
        if(!current(token) || blocksRecognition())return false
        recognitionFailures++;emit(VoiceStage.ERROR,VoiceReason.ASR_FAILED)
        return recognitionFailures<=1
    }
    @Synchronized fun backendFailed(token:Long){if(current(token) && !speechReserved)emit(VoiceStage.ERROR,VoiceReason.BACKEND_FAILED)}
    @Synchronized fun prepareSpeech(token:Long):Boolean {
        if(!current(token))return false
        speechReserved=true;cooldownUntil=0L
        emit(VoiceStage.THINKING,VoiceReason.AUDIO_PREPARING);return true
    }
    @Synchronized fun audioStarted(token:Long){if(current(token)){speechReserved=true;emit(VoiceStage.SPEAKING,VoiceReason.AUDIO_STARTED)}}
    @Synchronized fun audioCompleted(token:Long,cooldownMs:Long=500L){if(current(token)){
        speechReserved=false;cooldownUntil=now()+cooldownMs.coerceIn(250L,2000L)
        emit(VoiceStage.COOLDOWN,VoiceReason.AUDIO_COMPLETED)
    }}
    @Synchronized fun audioFailed(token:Long,cooldownMs:Long=500L){if(current(token)){
        speechReserved=false;cooldownUntil=now()+cooldownMs.coerceIn(250L,2000L)
        emit(VoiceStage.COOLDOWN,VoiceReason.AUDIO_FAILED)
    }}
    @Synchronized fun cooldownAfterStop(token:Long,cooldownMs:Long=500L){if(current(token)){
        speechReserved=false;cooldownUntil=now()+cooldownMs.coerceIn(250L,2000L)
        emit(VoiceStage.COOLDOWN,VoiceReason.CANCELLED)
    }}
    @Synchronized fun ready(token:Long):Boolean {
        if(!current(token) || speechReserved || now()<cooldownUntil)return false
        emit(VoiceStage.READY,VoiceReason.COOLDOWN_COMPLETE);return true
    }
    @Synchronized fun idle(token:Long){if(current(token) && !speechReserved && now()>=cooldownUntil)emit(VoiceStage.READY,VoiceReason.INACTIVE)}
    @Synchronized fun cancel(token:Long){if(current(token)){speechReserved=false;cooldownUntil=0L;emit(VoiceStage.IDLE,VoiceReason.CANCELLED)}}
}
