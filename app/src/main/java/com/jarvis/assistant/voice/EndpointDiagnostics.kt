package com.jarvis.assistant.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EndpointStage { IDLE, WAITING_FOR_MIC, LISTENING, SPEECH_ACTIVE, ENDPOINT_CANDIDATE, FINALIZING, COMPLETED, ERROR, CANCELLED }
data class EndpointReading(
    val engine: String = "Not started",
    val state: String = EndpointStage.IDLE.name,
    val elapsedMs: Long = 0,
    val finalizationMs: Long? = null,
    val error: String? = null,
    val vadCapability: String = HEURISTIC_VAD,
) {
    companion object { const val HEURISTIC_VAD = "ASR boundaries + RMS/text heuristics; no neural VAD or speaker separation" }
}

/** Current capture metadata only. Session tokens reject old callbacks; no transcript/PCM is accepted. */
object EndpointDiagnostics {
    private val mutable = MutableStateFlow(EndpointReading())
    val state = mutable.asStateFlow()
    private var serial = 0L
    private var started = 0L
    private var finalizingAt: Long? = null
    private var publishedAt = 0L
    private var terminal = false

    @Synchronized fun begin(engine: String, at: Long, vadCapability: String = EndpointReading.HEURISTIC_VAD): Long {
        serial++; started = at; finalizingAt = null; publishedAt = at; terminal = false
        mutable.value = EndpointReading(engine.take(100), EndpointStage.WAITING_FOR_MIC.name, vadCapability = vadCapability.take(180))
        return serial
    }
    @Synchronized fun captureReady(token: Long, at: Long) {
        if (token == 0L || token != serial || terminal) return
        started = at; finalizingAt = null
        publish(token, EndpointStage.LISTENING, at)
    }
    @Synchronized fun activity(token: Long, at: Long, recognizedSpeech: Boolean) {
        publish(token, if (recognizedSpeech) EndpointStage.SPEECH_ACTIVE else EndpointStage.LISTENING, at, throttle = true)
    }
    @Synchronized fun candidate(token: Long, at: Long) = publish(token, EndpointStage.ENDPOINT_CANDIDATE, at)
    @Synchronized fun finalizing(token: Long, at: Long) {
        if (token == 0L || token != serial || terminal) return
        if (finalizingAt == null) finalizingAt = at
        publish(token, EndpointStage.FINALIZING, at)
    }
    @Synchronized fun complete(token: Long, at: Long) = publish(token, EndpointStage.COMPLETED, at)
    /** The decoder returned an empty boundary and explicitly resumed the same capture. */
    @Synchronized fun resumeAfterEmptyBoundary(token:Long,at:Long){
        if(token==0L || token!=serial || terminal)return
        finalizingAt=null
        publish(token,EndpointStage.LISTENING,at)
    }
    @Synchronized fun cancel(token: Long, at: Long) = publish(token, EndpointStage.CANCELLED, at)
    @Synchronized fun fail(token: Long, at: Long, code: String) = publish(token, EndpointStage.ERROR, at,
        error = code.takeIf { Regex("[A-Z0-9_]{1,80}").matches(it) } ?: "ENDPOINT_ERROR")

    private fun publish(token: Long, stage: EndpointStage, at: Long, error: String? = null, throttle: Boolean = false) {
        if (token == 0L || token != serial || terminal) return
        if (finalizingAt != null && stage in setOf(EndpointStage.LISTENING, EndpointStage.SPEECH_ACTIVE, EndpointStage.ENDPOINT_CANDIDATE)) return
        if (throttle && mutable.value.state == stage.name && at - publishedAt in 0..249) return
        publishedAt = at
        mutable.value = mutable.value.copy(state = stage.name, elapsedMs = (at - started).coerceAtLeast(0),
            finalizationMs = finalizingAt?.let { (at - it).coerceAtLeast(0) }, error = error)
        terminal = stage in setOf(EndpointStage.COMPLETED, EndpointStage.ERROR, EndpointStage.CANCELLED)
    }
}
