package com.jarvis.assistant.companion

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class DesktopConnectionProofTest {
    private val now=1700000000000L
    private val pair=DesktopPair("https://192.168.1.8:8765","a".repeat(64),UUID.randomUUID().toString(),UUID.randomUUID().toString(),UUID.randomUUID().toString(),"T".repeat(43))
    private val proof=DesktopConnectionProof(pair.deviceId,pair.sessionId,now)
    @Test fun currentSignedSessionCanBeMarkedVerified(){assertTrue(DesktopConnectionProofPolicy.current(proof,pair,now))}
    @Test fun savedPairAloneIsNotVerified(){assertFalse(DesktopConnectionProofPolicy.current(null,pair,now))}
    @Test fun proofWithoutSavedGrantIsNotVerified(){assertFalse(DesktopConnectionProofPolicy.current(proof,null,now))}
    @Test fun noProofOrPairIsUnpaired(){assertEquals("Computer · unpaired",DesktopConnectionProofPolicy.text(null,null,now))}
    @Test fun differentDeviceCannotUseProof(){assertFalse(DesktopConnectionProofPolicy.current(proof,pair.copy(deviceId=UUID.randomUUID().toString()),now))}
    @Test fun rotatedSessionCannotUseOldProof(){assertFalse(DesktopConnectionProofPolicy.current(proof,pair.copy(sessionId=UUID.randomUUID().toString()),now))}
    @Test fun proofForAnotherDeviceIsRejected(){assertFalse(DesktopConnectionProofPolicy.current(proof.copy(device=UUID.randomUUID().toString()),pair,now))}
    @Test fun proofForAnotherSessionIsRejected(){assertFalse(DesktopConnectionProofPolicy.current(proof.copy(session=UUID.randomUUID().toString()),pair,now))}
    @Test fun staleConnectionIsNotPermanentlyOnline(){assertFalse(DesktopConnectionProofPolicy.current(proof,pair,now+60001))}
    @Test fun exactExpiryBoundaryIsDefined(){assertTrue(DesktopConnectionProofPolicy.current(proof,pair,now+60000))}
    @Test fun recentConnectionRemainsCurrent(){assertTrue(DesktopConnectionProofPolicy.current(proof,pair,now+30000))}
    @Test fun futureProofIsNotAcceptedAfterClockRollback(){assertFalse(DesktopConnectionProofPolicy.current(proof,pair,now-1))}
    @Test fun zeroTimestampCannotCreateProof(){assertFalse(DesktopConnectionProofPolicy.current(proof.copy(at=0),pair,0))}
    @Test fun negativeTimestampCannotCreateProof(){assertFalse(DesktopConnectionProofPolicy.current(proof.copy(at=-1),pair,-1))}
    @Test fun currentTextDistinguishesCommandCompletion(){assertTrue(DesktopConnectionProofPolicy.text(proof,pair,now).contains("Command completion is separate"))}
    @Test fun localPairTextDoesNotClaimOnline(){val s=DesktopConnectionProofPolicy.text(null,pair,now);assertTrue(s.contains("unverified"));assertFalse(s.contains("response verified"))}
    @Test fun expiredTextDoesNotClaimAuthenticated(){assertFalse(DesktopConnectionProofPolicy.text(proof,pair,now+60001).contains("paired / authenticated"))}
    @Test fun proofDiagnosticsNeverContainSessionId(){assertFalse(proof.toString().contains(pair.sessionId))}
    @Test fun proofDiagnosticsNeverContainDeviceIdentity(){assertFalse(proof.toString().contains(pair.deviceId))}
    @Test fun currentTextNeverContainsSessionToken(){assertFalse(DesktopConnectionProofPolicy.text(proof,pair,now).contains(pair.token))}
}
