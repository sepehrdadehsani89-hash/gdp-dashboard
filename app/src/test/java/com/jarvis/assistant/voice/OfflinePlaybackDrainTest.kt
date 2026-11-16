package com.jarvis.assistant.voice

import com.jarvis.assistant.voice.OfflinePlaybackDrain.Result.*
import org.junit.Assert.*
import org.junit.Test

/** Cursor/timing tests execute on the JVM; they do not claim real AudioTrack output. */
class OfflinePlaybackDrainTest {
    private class Output {
        var time = 1000L
        var played = 0L
        var cancelled = false
        var waits = 0
        var reads = 0
        var step: () -> Unit = { time += 20 }
        fun drain(written: Long = 100, timeout: Long = 100) = OfflinePlaybackDrain.awaitCompletion(
            written, { reads++; played }, { cancelled }, { time }, { waits++; step() }, timeout,
        )
    }
    @Test fun emptyOutputCompletesWithoutPolling() {
        val o = Output(); assertEquals(COMPLETE, o.drain(0)); assertEquals(0, o.waits)
    }
    @Test fun aFullyPlayedBufferCompletesImmediately() {
        val o = Output(); o.played = 100; assertEquals(COMPLETE, o.drain()); assertEquals(0, o.waits)
    }
    @Test fun aCursorBeyondTheWrittenBufferAlsoConfirmsCompletion() {
        val o = Output(); o.played = 101; assertEquals(COMPLETE, o.drain())
    }
    @Test fun normalProgressWaitsForTheLastFrame() {
        val o = Output(); o.step = { o.time += 20; o.played += 25 }
        assertEquals(COMPLETE, o.drain()); assertEquals(4, o.waits)
    }
    @Test fun aStalledTrackTimesOutInsteadOfReportingCompletion() {
        val o = Output(); assertEquals(TIMED_OUT, o.drain()); assertEquals(5, o.waits)
    }
    @Test fun aPartlyPlayedTrackStillCannotReportCompletionAtTimeout() {
        val o = Output(); o.played = 99; assertEquals(TIMED_OUT, o.drain())
    }
    @Test fun observedCompletionAtTheDeadlineIsStillRealCompletion() {
        val o = Output(); o.step = { o.time += 100; o.played = 100 }
        assertEquals(COMPLETE, o.drain()); assertEquals(1, o.waits)
    }
    @Test fun progressDoesNotResetTheAbsoluteDrainDeadline() {
        val o = Output(); o.step = { o.time += 20; o.played++ }
        assertEquals(TIMED_OUT, o.drain()); assertEquals(5L, o.played)
    }
    @Test fun cancellationBeforePollingDoesNotTouchTheTrack() {
        val o = Output(); o.cancelled = true
        assertEquals(CANCELLED, o.drain()); assertEquals(0, o.reads); assertEquals(0, o.waits)
    }
    @Test fun cancellationDuringTheWaitStopsTheDrain() {
        val o = Output(); o.step = { o.cancelled = true }
        assertEquals(CANCELLED, o.drain()); assertEquals(1, o.waits)
    }
    @Test fun cancellationTakesPriorityOverAnAlreadyCompleteCursor() {
        val o = Output(); o.cancelled = true; o.played = 100
        assertEquals(CANCELLED, o.drain())
    }
    @Test fun cancellationWhileReadingTheCursorCannotBecomeSuccess() {
        var cancelled = false
        assertEquals(CANCELLED, OfflinePlaybackDrain.awaitCompletion(100,
            { cancelled = true; 100 }, { cancelled }, { 0 }))
    }
    @Test fun aClockMovingBackwardsFailsClosed() {
        val o = Output(); o.step = { o.time-- }; assertEquals(TIMED_OUT, o.drain())
    }
    @Test fun aLongSchedulingPauseCannotExtendTheTimeout() {
        val o = Output(); o.step = { o.time += 5000 }
        assertEquals(TIMED_OUT, o.drain()); assertEquals(1, o.waits)
    }
    @Test fun aCustomTimeoutControlsTheActualWaitBudget() {
        val o = Output(); assertEquals(TIMED_OUT, o.drain(timeout = 40)); assertEquals(2, o.waits)
    }
    @Test fun negativeWrittenFrameCountsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { Output().drain(-1) }
    }
    @Test fun nonpositiveTimeoutsAreRejected() {
        for (timeout in listOf(0L, -1L)) {
            assertThrows(IllegalArgumentException::class.java) { Output().drain(timeout = timeout) }
        }
    }
    @Test fun invalidPlaybackCursorsAreNotSuccess() {
        val o = Output(); o.played = -1
        assertThrows(IllegalStateException::class.java) { o.drain() }
    }
    @Test fun interruptionPropagatesToTheExistingOutputErrorHandler() {
        val o = Output(); o.step = { throw InterruptedException("cancelled output worker") }
        assertThrows(InterruptedException::class.java) { o.drain() }
    }
    @Test fun aTrackReadFailurePropagatesWithoutACompletionResult() {
        assertThrows(IllegalStateException::class.java) {
            OfflinePlaybackDrain.awaitCompletion(100, { error("released track") }, { false }, { 0 })
        }
    }
}
