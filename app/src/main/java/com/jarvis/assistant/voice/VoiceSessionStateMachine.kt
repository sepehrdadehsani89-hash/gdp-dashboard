package com.jarvis.assistant.voice

enum class VoiceStage { IDLE, WAKE_DETECTED, GREETING, WAITING_FOR_MIC, LISTENING, PROCESSING, SPEAKING, FOLLOW_UP_LISTENING, ERROR }
enum class VoiceReason { WAKE, GREETING_REQUESTED, GREETING_DUPLICATE, CAPTURE_ARMED, CAPTURE_READY, FINAL_RECEIVED, ASR_FAILED, OWNER_CHECK, OWNER_MATCHED, OWNER_REJECTED, BACKEND_FAILED, AUDIO_STARTED, AUDIO_COMPLETED, AUDIO_FAILED, CANCELLED, INACTIVE }
data class VoiceTransition(val at:Long,val generation:Long,val stage:VoiceStage,val reason:VoiceReason,val characters:Int=0)

/** One logical voice turn coordinator. Physical recorders still enforce their exclusive leases.
 * Diagnostics are bounded metadata; raw text/audio, paths, credentials and provider bodies never enter them. */
class VoiceSessionStateMachine(private val now:()->Long,private val report:(VoiceTransition)->Unit={}){
    companion object {
        private val tickets=java.util.concurrent.atomic.AtomicLong()
        fun nextWakeTicket()=tickets.incrementAndGet()
    }
    private var generation=0L
    private var stage=VoiceStage.IDLE
    private var lastGreeting=Long.MIN_VALUE
    private var recognitionFailures=0
    private val events=ArrayDeque<VoiceTransition>()
    @Synchronized fun snapshot()=VoiceTransition(now(),generation,stage,events.lastOrNull()?.reason ?: VoiceReason.INACTIVE)
    @Synchronized fun recent()=events.toList()
    private fun emit(next:VoiceStage,reason:VoiceReason,characters:Int=0){
        stage=next
        val event=VoiceTransition(now(),generation,next,reason,characters.coerceIn(0,8000))
        if(events.size>=64)events.removeFirst();events.addLast(event);report(event)
    }
    private fun current(token:Long):Boolean {
        if(token<generation)return false
        generation=token;return true
    }
    @Synchronized fun wake(token:Long){if(current(token)){recognitionFailures=0;emit(VoiceStage.WAKE_DETECTED,VoiceReason.WAKE)}}
    @Synchronized fun claimGreeting(ticket:Long):Boolean {
        if(ticket<=lastGreeting){emit(stage,VoiceReason.GREETING_DUPLICATE);return false}
        lastGreeting=ticket;return true
    }
    @Synchronized fun greeting(token:Long){if(current(token))emit(VoiceStage.GREETING,VoiceReason.GREETING_REQUESTED)}
    @Synchronized fun armCapture(token:Long){if(current(token))emit(VoiceStage.WAITING_FOR_MIC,VoiceReason.CAPTURE_ARMED)}
    @Synchronized fun captureReady(token:Long,followUp:Boolean):Boolean {
        if(!current(token) || stage in setOf(VoiceStage.GREETING,VoiceStage.SPEAKING,VoiceStage.PROCESSING))return false
        emit(if(followUp)VoiceStage.FOLLOW_UP_LISTENING else VoiceStage.LISTENING,VoiceReason.CAPTURE_READY);return true
    }
    @Synchronized fun recognized(token:Long,length:Int){if(current(token) && length>0){recognitionFailures=0;emit(VoiceStage.PROCESSING,VoiceReason.FINAL_RECEIVED,length)}}
    @Synchronized fun ownerCheck(token:Long){if(current(token))emit(VoiceStage.PROCESSING,VoiceReason.OWNER_CHECK)}
    @Synchronized fun ownerResult(token:Long,matched:Boolean){if(current(token))emit(if(matched)VoiceStage.PROCESSING else VoiceStage.ERROR,if(matched)VoiceReason.OWNER_MATCHED else VoiceReason.OWNER_REJECTED)}
    @Synchronized fun asrFailed(token:Long):Boolean {
        if(!current(token))return false
        recognitionFailures++;emit(VoiceStage.ERROR,VoiceReason.ASR_FAILED)
        return recognitionFailures<=1
    }
    @Synchronized fun backendFailed(token:Long){if(current(token))emit(VoiceStage.ERROR,VoiceReason.BACKEND_FAILED)}
    @Synchronized fun audioStarted(token:Long){if(current(token))emit(if(stage==VoiceStage.GREETING)VoiceStage.GREETING else VoiceStage.SPEAKING,VoiceReason.AUDIO_STARTED)}
    @Synchronized fun audioCompleted(token:Long){if(current(token))emit(VoiceStage.WAITING_FOR_MIC,VoiceReason.AUDIO_COMPLETED)}
    @Synchronized fun audioFailed(token:Long){if(current(token))emit(VoiceStage.ERROR,VoiceReason.AUDIO_FAILED)}
    @Synchronized fun idle(token:Long){if(current(token))emit(VoiceStage.IDLE,VoiceReason.INACTIVE)}
    @Synchronized fun cancel(token:Long){if(current(token))emit(VoiceStage.IDLE,VoiceReason.CANCELLED)}
}
