package com.jarvis.assistant.screen

import org.junit.Assert.*
import org.junit.After
import org.junit.Test

/** Projection lifecycle and finalization policy only; no real video is recorded by JVM tests. */
class RecordingPolicyTest {
    @After fun cleanup(){ProjectionLease.release("fixture-a");ProjectionLease.release("fixture-b")}
    @Test fun firstProjectionGetsExclusiveLease(){assertTrue(ProjectionLease.claim("fixture-a"))}
    @Test fun differentProjectionCannotShareLease(){ProjectionLease.claim("fixture-a");assertFalse(ProjectionLease.claim("fixture-b"))}
    @Test fun ownerCanObserveItsLease(){ProjectionLease.claim("fixture-a");assertTrue(ProjectionLease.owned("fixture-a"))}
    @Test fun otherOwnerCannotObserveOwnership(){ProjectionLease.claim("fixture-a");assertFalse(ProjectionLease.owned("fixture-b"))}
    @Test fun wrongReleaseCannotStopOtherProjection(){ProjectionLease.claim("fixture-a");ProjectionLease.release("fixture-b");assertTrue(ProjectionLease.owned("fixture-a"))}
    @Test fun releasedProjectionAllowsNewCapture(){ProjectionLease.claim("fixture-a");ProjectionLease.release("fixture-a");assertTrue(ProjectionLease.claim("fixture-b"))}
    @Test fun completedStoppedVideoRequiresActualBytesAndDuration(){assertTrue(RecordingPolicy.saved(true,5000,1000))}
    @Test fun failedRecorderCannotClaimSavedVideo(){assertFalse(RecordingPolicy.saved(false,5000,1000))}
    @Test fun zeroByteOutputCannotClaimSavedVideo(){assertFalse(RecordingPolicy.saved(true,0,1000))}
    @Test fun headerOnlyFileCannotClaimSavedVideo(){assertFalse(RecordingPolicy.saved(true,1024,1000))}
    @Test fun zeroDurationCannotClaimSavedVideo(){assertFalse(RecordingPolicy.saved(true,5000,0))}
    @Test fun negativeDurationCannotClaimSavedVideo(){assertFalse(RecordingPolicy.saved(true,5000,-1))}
    @Test fun landscapeResolutionIsBounded(){assertEquals(1280 to 720,RecordingPolicy.dimensions(3840,2160))}
    @Test fun portraitResolutionIsBoundedAndEven(){val size=RecordingPolicy.dimensions(1080,2340);assertEquals(1280,size.second);assertEquals(0,size.first%2)}
    @Test fun smallValidScreenIsNotUpscaledUnnecessarily(){assertEquals(640 to 480,RecordingPolicy.dimensions(640,480))}
    @Test fun invalidScreenCannotStartRecorder(){assertTrue(runCatching{RecordingPolicy.dimensions(0,1080)}.isFailure)}
    @Test fun freshConsentIsUsable(){assertTrue(RecordingPolicy.validConsent(1000,1001))}
    @Test fun consentExpiryHasFiniteBoundary(){assertTrue(RecordingPolicy.validConsent(0,120000));assertFalse(RecordingPolicy.validConsent(0,120001))}
    @Test fun futureConsentCannotBeReplayedAfterClockChange(){assertFalse(RecordingPolicy.validConsent(1000,999))}
    @Test fun savedMetadataDoesNotMeanRecorderStillActive(){assertFalse(RecordingStatus(RecordingStage.SAVED).active)}
    @Test fun activeRecordingIsVisibleInStatus(){assertTrue(RecordingStatus(RecordingStage.RECORDING).active)}
    @Test fun consentAloneDoesNotClaimRecording(){assertFalse(RecordingStatus(RecordingStage.CONSENT_REQUIRED).active)}
}
