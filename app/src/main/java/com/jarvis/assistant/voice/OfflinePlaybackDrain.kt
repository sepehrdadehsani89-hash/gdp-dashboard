package com.jarvis.assistant.voice

/** Wait on the audio playback cursor, never treating a timeout as audible completion.
 * Call from the existing serial output worker with a monotonic clock. */
internal object OfflinePlaybackDrain {
    enum class Result { COMPLETE, CANCELLED, TIMED_OUT }

    fun awaitCompletion(
        writtenFrames: Long,
        playedFrames: () -> Long,
        cancelled: () -> Boolean,
        now: () -> Long,
        wait: () -> Unit = { Thread.sleep(20) },
        timeoutMs: Long = 10_000,
    ): Result {
        require(writtenFrames >= 0 && timeoutMs > 0)
        val started = now()
        while (true) {
            if (cancelled()) return Result.CANCELLED
            val played = playedFrames()
            check(played >= 0) { "Invalid audio playback cursor" }
            if (cancelled()) return Result.CANCELLED
            if (played >= writtenFrames) return Result.COMPLETE
            val time = now()
            if (time < started || time - started >= timeoutMs) return Result.TIMED_OUT
            wait()
        }
    }
}
