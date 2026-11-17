package com.jarvis.assistant.setup
import org.junit.Assert.*
import org.junit.Test
class SetupPolicyTest {
    @Test fun freshInstallNeedsSetup(){assertTrue(SetupPolicy.needed(false,0))}
    @Test fun priorReleaseGetsMajorUpdateWizard(){assertTrue(SetupPolicy.needed(true,0))}
    @Test fun completedCurrentRevisionNotRepeated(){assertFalse(SetupPolicy.needed(true,SetupPolicy.REVISION))}
    @Test fun futureRevisionNotDowngraded(){assertFalse(SetupPolicy.needed(true,99))}
    @Test fun explicitIncompleteStillRuns(){assertTrue(SetupPolicy.needed(false,SetupPolicy.REVISION))}
    @Test fun malformedNegativeRevisionNeedsSetup(){assertTrue(SetupPolicy.needed(true,-1))}
    @Test fun allThirteenStepsAreNavigable(){var s=0;val seen=mutableSetOf(s);repeat(12){s=SetupPolicy.next(s);seen+=s};assertEquals((0..12).toSet(),seen)}
    @Test fun backCanReachEveryPriorScreen(){var s=12;repeat(12){s=SetupPolicy.back(s)};assertEquals(0,s)}
    @Test fun nextAtLastNeverOverflows(){assertEquals(12,SetupPolicy.next(12))}
    @Test fun backAtFirstNeverUnderflows(){assertEquals(0,SetupPolicy.back(0))}
    @Test fun negativeNextRestoredWithinBounds(){assertEquals(0,SetupPolicy.next(-9))}
    @Test fun invalidBackRestoredWithinBounds(){assertEquals(12,SetupPolicy.back(99))}
    @Test fun completeSetupRequiresAllEvidence(){assertTrue(SetupPolicy.ready(true,true,true,true))}
    @Test fun identityAloneNeverReady(){assertFalse(SetupPolicy.ready(true,false,false,false))}
    @Test fun inferenceWithoutVoiceNotReady(){assertFalse(SetupPolicy.ready(true,true,false,false))}
    @Test fun engineCallbackWithoutAudibleConfirmationNotReady(){assertFalse(SetupPolicy.ready(true,true,true,false))}
    @Test fun userCheckboxWithoutPlaybackNotReady(){assertFalse(SetupPolicy.ready(true,true,false,true))}
    @Test fun missingMicCannotClaimVoiceAssistantReady(){assertFalse(SetupPolicy.ready(false,true,true,true))}
    @Test fun incompleteSetupCanBeSavedTruthfully(){assertTrue(SetupPolicy.status(false,false,false,false).contains("pending"))}
    @Test fun allIncompleteCombinationsAvoidReadyLabel(){for(bits in 0..14){val s=SetupPolicy.status(bits and 1!=0,bits and 2!=0,bits and 4!=0,bits and 8!=0);assertFalse(s.startsWith("Jarvis Ready"))}}
}
