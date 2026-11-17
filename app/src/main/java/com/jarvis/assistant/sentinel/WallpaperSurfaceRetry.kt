package com.jarvis.assistant.sentinel

/** A transient invalid surface must not consume the render loop's only callback.
 * Retries run on the wallpaper render worker, stop with visibility/surface loss, and
 * are bounded until Android or a new state update explicitly requests another frame. */
internal class WallpaperSurfaceRetry(private val maxAttempts: Int = 8) {
    private var attempts = 0

    init { require(maxAttempts > 0) }

    fun reset() { attempts = 0 }

    fun nextDelay(visible: Boolean, surfaceReady: Boolean, interactive: Boolean, destroyed: Boolean): Long? {
        if (!visible || !surfaceReady || !interactive || destroyed || attempts >= maxAttempts) return null
        val delay = minOf(1000L, 50L shl attempts.coerceAtMost(5))
        attempts++
        return delay
    }
}
