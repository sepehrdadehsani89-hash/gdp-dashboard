package com.jarvis.assistant.ui

/** Preserve the target cadence rather than accumulating callback-rounding delays.
 * At most one update is accepted per callback; missed frames are never replayed. */
internal class CoreFramePacer(fps: Int) {
    init { require(fps in 1..60) }
    private val interval = 1_000_000_000L / fps
    private var deadline: Long? = null
    private var previousCallback: Long? = null

    fun accept(timeNanos: Long): Boolean {
        val previous = previousCallback
        if (previous == timeNanos) return false
        previousCallback = timeNanos
        val due = deadline
        if (due == null || previous != null && timeNanos - previous < 0) {
            deadline = timeNanos + interval
            return true
        }
        val late = timeNanos - due
        if (late < 0) return false
        // Keep a single boundary of timing debt so an early/late callback pair
        // does not permanently drop to half rate. Larger stalls resynchronize
        // rather than replaying an accumulated backlog on subsequent callbacks.
        deadline = due + interval
        if (timeNanos - deadline!! >= interval) deadline = timeNanos + interval
        return true
    }
}

/** Main-thread developer counters. Draw calls are not physical display FPS. */
internal class CoreRenderDiagnostics {
    data class Snapshot(
        val callbacks: Long = 0,
        val updates: Long = 0,
        val draws: Long = 0,
        val callbacksPerSecond: Float? = null,
        val drawsPerSecond: Float? = null,
    )
    private var callbacks = 0L
    private var updates = 0L
    private var draws = 0L
    private var previousAt: Long? = null
    private var previousCallbacks = 0L
    private var previousDraws = 0L
    fun clockCallback() { callbacks++ }
    fun update() { updates++ }
    fun draw() { draws++ }
    fun sample(now: Long): Snapshot {
        val elapsed = previousAt?.let { now - it }
        val callbackRate = elapsed?.takeIf { it > 0 }?.let { (callbacks - previousCallbacks) * 1000f / it }
        val drawRate = elapsed?.takeIf { it > 0 }?.let { (draws - previousDraws) * 1000f / it }
        previousAt = now
        previousCallbacks = callbacks
        previousDraws = draws
        return Snapshot(callbacks, updates, draws, callbackRate, drawRate)
    }
}
