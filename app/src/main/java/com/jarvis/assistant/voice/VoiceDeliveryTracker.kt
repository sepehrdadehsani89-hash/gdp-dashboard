package com.jarvis.assistant.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class InferenceOutcome { NOT_REQUESTED, REQUESTING, RESPONSE_RECEIVED, FAILED }
enum class VoiceOutputOutcome { NOT_REQUESTED, PREPARING, STARTED, COMPLETED, FAILED, STOPPED }
data class VoiceDeliveryStatus(val generation:Long=0,val provider:String="",val inference:InferenceOutcome=InferenceOutcome.NOT_REQUESTED,val output:VoiceOutputOutcome=VoiceOutputOutcome.NOT_REQUESTED,val responseCharacters:Int=0,val engineReady:Boolean=false)

/** Separate inference and output evidence; never stores text, account identity or credentials. */
class VoiceDeliveryTracker {
    private val mutable=MutableStateFlow(VoiceDeliveryStatus())
    val state=mutable.asStateFlow()
    private fun current(token:Long):Boolean {
        if(token<mutable.value.generation)return false
        if(token>mutable.value.generation)mutable.value=VoiceDeliveryStatus(generation=token)
        return true
    }
    @Synchronized fun request(token:Long,provider:String){if(current(token))mutable.value=mutable.value.copy(provider=provider.takeIf{it in setOf("openai","anthropic","gemini","perplexity","local")} ?: "",inference=InferenceOutcome.REQUESTING,responseCharacters=0,output=VoiceOutputOutcome.NOT_REQUESTED,engineReady=false)}
    @Synchronized fun response(token:Long,characters:Int){if(current(token) && characters>0)mutable.value=mutable.value.copy(inference=InferenceOutcome.RESPONSE_RECEIVED,responseCharacters=characters.coerceAtMost(64000))}
    @Synchronized fun inferenceFailed(token:Long){if(current(token))mutable.value=mutable.value.copy(inference=InferenceOutcome.FAILED)}
    @Synchronized fun prepare(token:Long){if(current(token))mutable.value=mutable.value.copy(output=VoiceOutputOutcome.PREPARING,engineReady=false)}
    @Synchronized fun prepared(token:Long){if(current(token) && mutable.value.output==VoiceOutputOutcome.PREPARING)mutable.value=mutable.value.copy(engineReady=true)}
    @Synchronized fun started(token:Long){if(current(token) && mutable.value.output==VoiceOutputOutcome.PREPARING)mutable.value=mutable.value.copy(output=VoiceOutputOutcome.STARTED)}
    @Synchronized fun completed(token:Long){if(current(token) && mutable.value.output==VoiceOutputOutcome.STARTED)mutable.value=mutable.value.copy(output=VoiceOutputOutcome.COMPLETED)}
    @Synchronized fun failed(token:Long){if(current(token))mutable.value=mutable.value.copy(output=VoiceOutputOutcome.FAILED)}
    @Synchronized fun stop(token:Long){if(current(token))mutable.value=VoiceDeliveryStatus(generation=token,output=VoiceOutputOutcome.STOPPED)}
}
object SpeakableReply {
    fun normalize(text:String):String=text.take(32000)
        .replace(Regex("\\[([^]\\n]+)]\\(https?://[^)]+\\)"),"$1")
        .replace(Regex("https?://\\S+"),"")
        .replace(Regex("[*#`]+"),"")
        .filter{!it.isISOControl() || it=='\n' || it=='\t'}
        .trim().take(16000)
}
