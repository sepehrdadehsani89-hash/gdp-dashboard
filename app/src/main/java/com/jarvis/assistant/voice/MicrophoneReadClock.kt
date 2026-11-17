package com.jarvis.assistant.voice

/** Nonblocking reads must either deliver PCM or reach a finite error, rather than busy-spin. */
internal class MicrophoneReadClock(private val now: () -> Long, private val maximumGapMs: Long = 3000) {
    init { require(maximumGapMs > 0) }
    private var last = now()
    fun progress() { last = now() }
    fun stalled(): Boolean = now().let { it < last || it - last >= maximumGapMs }
}
