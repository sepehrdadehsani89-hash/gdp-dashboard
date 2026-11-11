package com.jarvis.assistant.finishpolish.doctor

import org.junit.Assert.*
import org.junit.Test

class DoctorPolicyTest {
    @Test fun readOnlyHealthyChecksCannotClaimAcousticRecognitionSucceeded(){
        val p=HealthProbe("permission","doctor",sampledAt=1,status=ProbeStatus.HEALTHY,certainty=HealthCertainty.MEASURED,category=FailureCategory.PERMISSION,evidence="Permission granted",nextStep="none",evidenceReference="permission")
        assertTrue(DoctorReport(1,listOf(p)).explanation().contains("does not prove"))
    }
    @Test fun genuineBackendFailureGetsABackendExplanationInsteadOfASpeechNoMatch(){
        val p=HealthProbe("backend","doctor",sampledAt=1,status=ProbeStatus.DEGRADED,certainty=HealthCertainty.MEASURED,category=FailureCategory.BACKEND,evidence="The response backend failed.",nextStep="Check service access.",evidenceReference="event")
        val explanation=DoctorReport(1,listOf(p)).explanation();assertTrue(explanation.contains("backend"));assertFalse(explanation.contains("didn't catch"))
    }
    @Test fun repairRequiresApprovalUnlockedPhoneAndIdlePrivateCapture(){
        assertNotNull(RepairAdmission.denial(false,true,true,emptyList(),10))
        assertNotNull(RepairAdmission.denial(true,false,true,emptyList(),10))
        assertNotNull(RepairAdmission.denial(true,true,false,emptyList(),10))
        assertNull(RepairAdmission.denial(true,true,true,emptyList(),10))
    }
    @Test fun rapidRetriesAreDeniedButNoAutomaticRetryLoopExists(){
        assertNotNull(RepairAdmission.denial(true,true,true,listOf(1_000),60_999))
        assertNull(RepairAdmission.denial(true,true,true,listOf(1_000),61_000))
    }
    @Test fun repeatedFailedAttemptsOpenCircuitUntilWindowExpires(){
        val failures=listOf(1_000L,70_000L,140_000L)
        assertNotNull(RepairAdmission.denial(true,true,true,failures,250_000))
        assertNull(RepairAdmission.denial(true,true,true,failures,740_000))
    }
    @Test fun timeRollbackCannotClearTheRepairBudget(){assertNotNull(RepairAdmission.denial(true,true,true,listOf(5000),4000))}
    @Test fun missingMeasurementsAreUnknownAndDoNotBecomeFailures(){
        val p=HealthProbe("accuracy","doctor",sampledAt=1,status=ProbeStatus.UNKNOWN,certainty=HealthCertainty.UNKNOWN,category=FailureCategory.RECOGNITION,evidence="Not measured",nextStep="Phone test",evidenceReference="none")
        assertTrue(DoctorReport(1,listOf(p)).issues.isEmpty());assertEquals(HealthSeverity.INFO,p.severity)
    }
    @Test fun verificationRejectsDifferentObservedSettingsAndNeverSetsAcousticPass(){
        val desired=VoiceRepairSettings("BALANCED",true,true)
        assertFalse(RepairAdmission.verified(desired,desired.copy(echoCancellation=false)))
        assertTrue(RepairAdmission.verified(desired,desired))
        assertFalse(RepairOutcome(RepairState.VERIFIED_SETTINGS_ONLY,"Saved").acousticVerified)
    }
    @Test fun repairProposalHasReversalAndExplicitVerificationBoundary(){val p=RepairProposal();assertEquals("T2",p.tier);assertEquals(3,p.maxAttempts);assertTrue(p.timeoutMs>0);assertTrue(p.rollback.contains("encrypted"));assertTrue(p.verification.contains("Acoustic"))}
}
