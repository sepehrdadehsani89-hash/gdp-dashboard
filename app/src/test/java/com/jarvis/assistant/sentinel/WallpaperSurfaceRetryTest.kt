package com.jarvis.assistant.sentinel

import org.junit.Assert.*
import org.junit.Test

/** JVM scheduling-policy cases, not actual Android SurfaceHolder/device evidence. */
class WallpaperSurfaceRetryTest {
    private fun WallpaperSurfaceRetry.ready() = nextDelay(true, true, true, false)

    @Test fun aTransientInvalidVisibleSurfaceRetainsAnotherRenderOpportunity() {
        assertEquals(50L, WallpaperSurfaceRetry().ready())
    }
    @Test fun repeatedInvalidityBacksOffRatherThanSpinningAtZeroDelay() {
        val retry = WallpaperSurfaceRetry()
        assertEquals(listOf(50L, 100L, 200L, 400L, 800L, 1000L, 1000L, 1000L),
            List(8) { retry.ready() })
    }
    @Test fun aPersistentlyInvalidSurfaceExhaustsItsRetryBudget() {
        val retry = WallpaperSurfaceRetry(); repeat(8) { assertNotNull(retry.ready()) }
        assertNull(retry.ready()); assertNull(retry.ready())
    }
    @Test fun aNewExplicitFrameRequestRestartsExhaustedRecovery() {
        val retry = WallpaperSurfaceRetry(); repeat(8) { retry.ready() }; assertNull(retry.ready())
        retry.reset(); assertEquals(50L, retry.ready())
    }
    @Test fun hiddenWallpapersNeverQueueRecoveryOrSpendTheBudget() {
        val retry = WallpaperSurfaceRetry()
        repeat(20) { assertNull(retry.nextDelay(false, true, true, false)) }
        assertEquals(50L, retry.ready())
    }
    @Test fun aDestroyedSurfaceWaitsForTheCreationCallback() {
        val retry = WallpaperSurfaceRetry()
        assertNull(retry.nextDelay(true, false, true, false))
        assertEquals(50L, retry.ready())
    }
    @Test fun aNoninteractiveScreenCannotKeepARecoveryLoopAlive() {
        val retry = WallpaperSurfaceRetry()
        assertNull(retry.nextDelay(true, true, false, false))
        assertEquals(50L, retry.ready())
    }
    @Test fun aDestroyedEngineCannotRecoverEvenWithOtherFlagsStillTrue() {
        assertNull(WallpaperSurfaceRetry().nextDelay(true, true, true, true))
    }
    @Test fun successfulRenderingResetsAnOldPartialFailureStreak() {
        val retry = WallpaperSurfaceRetry(); repeat(4) { retry.ready() }
        retry.reset(); assertEquals(50L, retry.ready())
    }
    @Test fun aShorterExplicitBudgetStillStopsAtItsExactLimit() {
        val retry = WallpaperSurfaceRetry(2)
        assertEquals(50L, retry.ready()); assertEquals(100L, retry.ready()); assertNull(retry.ready())
    }
    @Test fun resetIsIdempotentAndCannotStartHiddenRendering() {
        val retry = WallpaperSurfaceRetry(); retry.reset(); retry.reset()
        assertNull(retry.nextDelay(false, false, false, false))
        assertEquals(50L, retry.ready())
    }
    @Test fun visibilityLossDoesNotReplenishAnAlreadyExhaustedWindow() {
        val retry = WallpaperSurfaceRetry(1); retry.ready()
        assertNull(retry.nextDelay(false, true, true, false)); assertNull(retry.ready())
        retry.reset(); assertNotNull(retry.ready())
    }
    @Test fun invalidBudgetsFailRatherThanEnablingAnUnboundedRetryLoop() {
        assertThrows(IllegalArgumentException::class.java) { WallpaperSurfaceRetry(0) }
        assertThrows(IllegalArgumentException::class.java) { WallpaperSurfaceRetry(-1) }
    }
    @Test fun largerBudgetsCannotOverflowTheBackoffShift() {
        val retry = WallpaperSurfaceRetry(100)
        repeat(100) { assertTrue(requireNotNull(retry.ready()) in 1L..1000L) }
        assertNull(retry.ready())
    }
    @Test fun failedPostsConsumeRecoveryEvenWhenTheSurfaceIsReportedReady() {
        val retry = WallpaperSurfaceRetry(2)
        assertEquals(50L, retry.afterPost(false, true, true, true, false))
        assertEquals(100L, retry.afterPost(false, true, true, true, false))
        assertNull(retry.afterPost(false, true, true, true, false))
    }
    @Test fun aSuccessfulPostReplenishesTheNextFailureWindow() {
        val retry = WallpaperSurfaceRetry(1); retry.ready(); assertNull(retry.ready())
        assertNull(retry.afterPost(true, true, true, true, false))
        assertEquals(50L, retry.afterPost(false, true, true, true, false))
    }
    @Test fun unsuccessfulDrawingAfterVisibilityLossDoesNotQueueWork() {
        val retry = WallpaperSurfaceRetry()
        assertNull(retry.afterPost(false, false, true, true, false))
        assertEquals(50L, retry.ready())
    }
    @Test fun aSuccessfulStaticFrameDoesNotCreateAnAnimationLoop() {
        assertNull(WallpaperSurfaceRetry().afterPost(true, true, true, true, false))
    }
    @Test fun aFailedPostDoesNotResetAnExhaustedBudget() {
        val retry = WallpaperSurfaceRetry(1)
        assertNotNull(retry.afterPost(false, true, true, true, false))
        repeat(3) { assertNull(retry.afterPost(false, true, true, true, false)) }
    }
    @Test fun explicitSurfaceRecreationCanRecoverAfterAnExhaustedPostFailure() {
        val retry = WallpaperSurfaceRetry(1); retry.afterPost(false, true, true, true, false)
        assertNull(retry.afterPost(false, true, false, true, false))
        retry.reset()
        assertEquals(50L, retry.afterPost(false, true, true, true, false))
    }
}
