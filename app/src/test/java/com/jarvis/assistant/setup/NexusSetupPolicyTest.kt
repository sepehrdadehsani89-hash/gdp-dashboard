package com.jarvis.assistant.setup

import org.junit.Assert.*
import org.junit.Test

class NexusSetupPolicyTest {
    @Test fun wizardContainsAllFifteenStages(){assertEquals(15,NexusSetupPolicy.steps.size);assertEquals(15,NexusSetupPolicy.steps.distinct().size)}
    @Test fun firstStageIsWelcome(){assertEquals("Welcome",NexusSetupPolicy.steps.first())}
    @Test fun lastStageDoesNotInventConnectivity(){assertEquals("Completion",NexusSetupPolicy.steps.last());assertFalse(NexusSetupPolicy.completion().contains("all connected",true))}
    @Test fun missingAccountIsNotLiveEvenWithStaleFlags(){assertFalse(NexusSetupPolicy.integration(false,true,true).startsWith("LIVE"))}
    @Test fun accountOnlyIsNotLive(){assertFalse(NexusSetupPolicy.integration(true,false,false).startsWith("LIVE"))}
    @Test fun scopeOnlyIsNotLive(){assertFalse(NexusSetupPolicy.integration(false,true,false).startsWith("LIVE"))}
    @Test fun oldReadWithoutScopeIsNotLive(){assertFalse(NexusSetupPolicy.integration(true,false,true).startsWith("LIVE"))}
    @Test fun authorizationAloneIsNotARead(){assertTrue(NexusSetupPolicy.integration(true,true,false).contains("real read not verified"))}
    @Test fun actualCurrentAccountScopeAndReadAreRequired(){assertTrue(NexusSetupPolicy.integration(true,true,true).startsWith("LIVE API VERIFIED"))}
    @Test fun skippedAccountIsNotMarkedConnected(){assertTrue(NexusSetupPolicy.integration(true,true,true,true).startsWith("SKIPPED"))}
    @Test fun skippedMissingAccountDoesNotNeedFakeAuthorization(){assertTrue(NexusSetupPolicy.integration(false,false,false,true).startsWith("SKIPPED"))}
    @Test fun missingAccountTellsUserWhatToDo(){assertTrue(NexusSetupPolicy.integration(false,false,false).contains("select an account"))}
    @Test fun missingScopeTellsUserWhatToDo(){assertTrue(NexusSetupPolicy.integration(true,false,false).contains("authorize this service"))}
    @Test fun pendingProgressIsNotLiveProof(){assertEquals("PENDING",NexusSetupPolicy.progress("LIVE_API_VERIFIED"))}
    @Test fun manuallyClaimedConnectedProgressIsRejected(){assertEquals("PENDING",NexusSetupPolicy.progress("CONNECTED"))}
    @Test fun userReviewIsExplicitlyUserReview(){assertEquals("REVIEWED_BY_USER",NexusSetupPolicy.progress("REVIEWED_BY_USER"))}
    @Test fun skipIsPreserved(){assertEquals("SKIPPED",NexusSetupPolicy.progress("SKIPPED"))}
    @Test fun corruptProgressFailsToPending(){assertEquals("PENDING",NexusSetupPolicy.progress("{bad}"))}
    @Test fun backFromFirstStageDoesNotCrash(){assertEquals(0,NexusSetupPolicy.back(0))}
    @Test fun nextFromFinalStageDoesNotRunUnknownControl(){assertEquals(14,NexusSetupPolicy.next(14))}
    @Test fun completionDoesNotInferPhysicalTests(){assertTrue(NexusSetupPolicy.completion().contains("No physical device test is inferred"))}
    @Test fun completionPreservesUnverifiedServices(){assertTrue(NexusSetupPolicy.completion().contains("unverified integrations remain unconnected"))}
}
