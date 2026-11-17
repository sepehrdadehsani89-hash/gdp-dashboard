package com.jarvis.assistant.ui

import org.junit.Assert.*
import org.junit.Test

/** Virtual clock coverage: not measured Android GPU/display performance. */
class CoreFramePacerTest {
    private fun accepted(fps:Int,step:Long,seconds:Int=10):Int {
        val pacer=CoreFramePacer(fps)
        var count=0;var at=0L
        while(at<seconds*1_000_000_000L){if(pacer.accept(at))count++;at+=step}
        return count
    }
    @Test fun firstCallbackIsAcceptedAtZero(){assertTrue(CoreFramePacer(60).accept(0))}
    @Test fun anIdenticalCallbackCannotBeCountedTwice(){val p=CoreFramePacer(60);assertTrue(p.accept(0));assertFalse(p.accept(0))}
    @Test fun callbackBeforeDeadlineIsSkipped(){val p=CoreFramePacer(60);p.accept(0);assertFalse(p.accept(16_000_000))}
    @Test fun callbackAtDeadlineIsAccepted(){val p=CoreFramePacer(60);p.accept(0);assertTrue(p.accept(1_000_000_000L/60))}
    @Test fun slightlyFastSixtyHertzCallbacksDoNotHalveSixtyCap(){assertTrue(accepted(60,16_666_000) in 599..602)}
    @Test fun slightlyFastSixtyHertzCallbacksDoNotReduceThirtyCapToTwenty(){assertTrue(accepted(30,16_666_000) in 299..302)}
    @Test fun oneTwentyHertzCallbacksMaintainSixtyCap(){assertTrue(accepted(60,8_333_000) in 599..602)}
    @Test fun oneTwentyHertzCallbacksMaintainThirtyCap(){assertTrue(accepted(30,8_333_000) in 299..302)}
    @Test fun ninetyHertzCallbacksMaintainSixtyCap(){assertTrue(accepted(60,11_111_000) in 599..602)}
    @Test fun slowerDisplayCannotInventAdditionalCallbacks(){assertEquals(300,accepted(60,33_333_334))}
    @Test fun saverCapRemainsLowWithoutAccumulatingTruncation(){assertTrue(accepted(8,16_666_000) in 79..82)}
    @Test fun oneFramePerSecondCapIsSupported(){assertEquals(10,accepted(1,16_666_000))}
    @Test fun zeroOrNegativeCapsAreRejected(){for(rate in listOf(0,-1))assertThrows(IllegalArgumentException::class.java){CoreFramePacer(rate)}}
    @Test fun unsupportedRatesAboveTheSharedPolicyMaximumAreRejected(){assertThrows(IllegalArgumentException::class.java){CoreFramePacer(61)}}
    @Test fun aLongGapAcceptsOnlyOneUpdateAndNeverReplaysMissedFrames(){val p=CoreFramePacer(60);p.accept(0);assertTrue(p.accept(30_000_000_000));assertFalse(p.accept(30_000_000_000));assertFalse(p.accept(30_000_000_001))}
    @Test fun aRegressingClockRestartsInsteadOfWaitingForTheOldDeadline(){val p=CoreFramePacer(60);p.accept(1_000_000_000);assertTrue(p.accept(1));assertFalse(p.accept(2))}
    @Test fun aFreshVisibleSessionDoesNotRetainAHiddenDeadline(){CoreFramePacer(60).accept(1000);assertTrue(CoreFramePacer(60).accept(1001))}
    @Test fun signedNanosecondRolloverDoesNotTrapThePacer(){val p=CoreFramePacer(60);val first=Long.MAX_VALUE-5_000_000;p.accept(first);assertTrue(p.accept(first+20_000_000))}
    @Test fun callbackJitterDoesNotAccumulateHalfRateDrops(){val p=CoreFramePacer(60);var count=0;var at=0L;repeat(600){if(p.accept(at))count++;at+=if(it%2==0)16_400_000 else 16_933_333};assertTrue(count in 598..600)}
    @Test fun callbacksFasterThanTheBudgetRemainCapped(){assertTrue(accepted(30,1_000_000) in 299..302)}
    @Test fun diagnosticsDoNotInventRatesBeforeAnObservationWindow(){val d=CoreRenderDiagnostics();d.clockCallback();d.update();d.draw();val s=d.sample(1000);assertEquals(1L,s.callbacks);assertEquals(1L,s.updates);assertEquals(1L,s.draws);assertNull(s.drawsPerSecond);assertNull(s.callbacksPerSecond)}
    @Test fun diagnosticRatesUseActualCounterDeltasAndElapsedTime(){val d=CoreRenderDiagnostics();d.sample(0);repeat(30){d.clockCallback()};repeat(15){d.update();d.draw()};val s=d.sample(500);assertEquals(60f,s.callbacksPerSecond!!,.001f);assertEquals(30f,s.drawsPerSecond!!,.001f)}
    @Test fun diagnosticIdleWindowsReportZeroRatherThanAStaleRate(){val d=CoreRenderDiagnostics();d.sample(0);d.draw();d.sample(500);val s=d.sample(1000);assertEquals(0f,s.drawsPerSecond!!,0f);assertEquals(1L,s.draws)}
    @Test fun diagnosticZeroOrBackwardsDurationDoesNotInventFps(){val d=CoreRenderDiagnostics();d.sample(1000);d.draw();assertNull(d.sample(1000).drawsPerSecond);assertNull(d.sample(900).drawsPerSecond)}
}
