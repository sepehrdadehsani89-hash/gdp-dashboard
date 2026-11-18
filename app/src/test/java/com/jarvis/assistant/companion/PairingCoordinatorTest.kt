package com.jarvis.assistant.companion

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class PairingCoordinatorTest {
    private class Gateway:PairingGateway {
        var requestFailure:String?=null;var finishFailure:String?=null;var verifyFailure:String?=null;var approved=false;var wait=false
        var inspections=0;var requests=mutableListOf<Pair<String,String>>();var finishes=0;var verifications=0
        val ticket=UUID.randomUUID().toString();val pair=DesktopPair("https://192.168.1.8:8765","a".repeat(64),UUID.randomUUID().toString(),UUID.randomUUID().toString(),UUID.randomUUID().toString(),"T".repeat(43))
        override suspend fun inspectInvitation(i:DesktopInvitation){inspections++}
        override suspend fun requestPair(i:DesktopInvitation,clientId:String,nonce:String):JSONObject {requests+=clientId to nonce;if(wait)awaitCancellation();requestFailure?.let{throw CompanionFailure(it)};return JSONObject().put("ticket",ticket).put("compare_code","ABC123").put("expires_at",i.expires)}
        override suspend fun pairingStatus(i:DesktopInvitation,c:PairingChallenge,clientId:String,nonce:String)=approved
        override suspend fun finishPair(i:DesktopInvitation,ticket:String,clientId:String,nonce:String):DesktopPair {finishes++;finishFailure?.let{throw CompanionFailure(it)};return pair}
        override suspend fun verifyPair(pair:DesktopPair){verifications++;verifyFailure?.let{throw CompanionFailure(it)}}
    }
    private class Fixture(scope:CoroutineScope){
        val gateway=Gateway();var now=1700000000000L;val tracked=mutableListOf<Job>();val c=PairingCoordinator(gateway,scope,{now},{tracked+=it})
        fun invitation():String {val p=gateway.pair;val j=JSONObject().put("schema_version",1).put("url",p.url).put("certificate_sha256",p.pin).put("device_id",p.deviceId).put("pairing_id",UUID.randomUUID().toString()).put("code","C".repeat(43)).put("expires_at",now+120000);return PairingInvitation.PREFIX+Base64.getUrlEncoder().withoutPadding().encodeToString(j.toString().toByteArray())}
        fun ready(){c.setInvitation(invitation())}
    }
    @Test fun tlsDiagnosticsAreNotPairingOrAuthentication()=runTest{val f=Fixture(this);f.ready();f.c.inspect();advanceUntilIdle();assertEquals(1,f.gateway.inspections);assertTrue(f.gateway.requests.isEmpty());assertEquals(PairingStage.INVITATION_READY,f.c.state.value.stage);assertEquals(f.now,f.c.state.value.tlsCheckedAt);assertNull(f.c.state.value.lastSuccess)}
    @Test fun scannerDiagnosticsContainOnlyTypedEngineAndStatus()=runTest{val f=Fixture(this);f.ready();f.c.scannerInfo(QrScannerEngine.GOOGLE,QrScannerOutcome.FAILED,17);f.c.scannerOpening();assertTrue(f.c.diagnostics().contains("SDK code 17"));assertFalse(f.c.diagnostics().contains("C".repeat(43)))}
    @Test fun startsUnpairedWithoutNetwork()=runTest{val f=Fixture(this);assertEquals(PairingStage.UNPAIRED,f.c.state.value.stage);assertTrue(f.gateway.requests.isEmpty())}
    @Test fun newScanClearsPriorInvitationAndPreview()=runTest{val f=Fixture(this);f.ready();f.c.scannerOpening();assertEquals("",f.c.input);assertEquals("",f.c.state.value.address)}
    @Test fun newScanDoesNotReuseAnOldPendingTicket()=runTest{val f=Fixture(this);f.ready();f.c.request();advanceUntilIdle();f.c.scannerOpening();assertEquals("",f.c.state.value.ticket);assertEquals("",f.c.state.value.comparison);assertEquals(1,f.gateway.requests.size)}
    @Test fun scannerOpeningIsExplicit()=runTest{val f=Fixture(this);assertTrue(f.c.scannerOpening());assertEquals(PairingStage.SCANNER_OPENING,f.c.state.value.stage)}
    @Test fun scannerCannotLaunchTwice()=runTest{val f=Fixture(this);f.c.scannerOpening();assertFalse(f.c.scannerOpening())}
    @Test fun scanningIsNotPairing()=runTest{val f=Fixture(this);f.c.scanning();assertEquals(PairingStage.SCANNING,f.c.state.value.stage);assertTrue(f.gateway.requests.isEmpty())}
    @Test fun scanCancellationIsVisible()=runTest{val f=Fixture(this);f.c.scanCancelled();assertEquals(PairingStage.CANCELLED,f.c.state.value.stage)}
    @Test fun captureConflictIsExplained()=runTest{val f=Fixture(this);f.c.scanFailure("CAPTURE_BUSY");assertTrue(f.c.state.value.message.contains("CAPTURE_BUSY"))}
    @Test fun permissionFailureGivesAlternative()=runTest{val f=Fixture(this);f.c.scanFailure("CAMERA_PERMISSION_DENIED");assertTrue(f.c.state.value.message.contains("gallery"))}
    @Test fun invalidScanNeverSendsARequest()=runTest{val f=Fixture(this);f.c.setInvitation("wrong",true);assertEquals(PairingStage.FAILED,f.c.state.value.stage);assertTrue(f.gateway.requests.isEmpty())}
    @Test fun validScanRequiresExplicitRequest()=runTest{val f=Fixture(this);f.ready();assertEquals(PairingStage.INVITATION_READY,f.c.state.value.stage);assertTrue(f.gateway.requests.isEmpty())}
    @Test fun requestHasImmediateProgress()=runTest{val f=Fixture(this);f.ready();f.c.request();assertEquals(PairingStage.REQUESTING_PAIRING,f.c.state.value.stage);assertTrue(f.c.state.value.busy);advanceUntilIdle()}
    @Test fun validRequestShowsActualChallenge()=runTest{val f=Fixture(this);f.ready();f.c.request();advanceUntilIdle();assertEquals(f.gateway.ticket,f.c.state.value.ticket);assertEquals("ABC123",f.c.state.value.comparison);assertEquals(PairingStage.AWAITING_DESKTOP_APPROVAL,f.c.state.value.stage)}
    @Test fun duplicateTapDoesNotSendTwice()=runTest{val f=Fixture(this);f.ready();f.c.request();f.c.request();advanceUntilIdle();assertEquals(1,f.gateway.requests.size)}
    @Test fun existingTicketIsNotReplaced()=runTest{val f=Fixture(this);f.ready();f.c.request();advanceUntilIdle();f.c.request();advanceUntilIdle();assertEquals(1,f.gateway.requests.size)}
    @Test fun lostResponseRetryRetainsProofIdentity()=runTest{val f=Fixture(this);f.ready();f.gateway.requestFailure="DESKTOP_TIMEOUT_STATUS_UNKNOWN";f.c.request();advanceUntilIdle();f.gateway.requestFailure=null;f.c.request();advanceUntilIdle();assertEquals(2,f.gateway.requests.size);assertEquals(f.gateway.requests[0],f.gateway.requests[1])}
    @Test fun wrongCertificateIsAnExplicitFailure()=runTest{val f=Fixture(this);f.ready();f.gateway.requestFailure="TLS_PIN_OR_CERTIFICATE_ERROR";f.c.request();advanceUntilIdle();assertTrue(f.c.state.value.message.contains("TLS_CERTIFICATE_MISMATCH"));assertEquals(PairingStage.FAILED,f.c.state.value.stage)}
    @Test fun offlineErrorDoesNotClaimPairing()=runTest{val f=Fixture(this);f.ready();f.gateway.requestFailure="DESKTOP_CONNECTION_UNAVAILABLE";f.c.request();advanceUntilIdle();assertTrue(f.c.state.value.message.contains("NETWORK_UNREACHABLE"));assertNull(f.c.state.value.lastSuccess)}
    @Test fun invitationExpiryUpdatesWhileVisible()=runTest{val f=Fixture(this);f.ready();f.now+=120000;f.c.tick();assertEquals(PairingStage.EXPIRED,f.c.state.value.stage)}
    @Test fun expiredInvitationCannotBeSubmitted()=runTest{val f=Fixture(this);f.ready();f.now+=120000;f.c.request();advanceUntilIdle();assertTrue(f.gateway.requests.isEmpty());assertEquals(PairingStage.EXPIRED,f.c.state.value.stage)}
    @Test fun approvalPendingIsNotConnected()=runTest{val f=Fixture(this);f.ready();f.c.request();advanceUntilIdle();f.c.approval();advanceUntilIdle();assertEquals(PairingStage.AWAITING_DESKTOP_APPROVAL,f.c.state.value.stage);assertNull(f.c.state.value.lastSuccess)}
    @Test fun desktopApprovalStillNeedsPhoneFinish()=runTest{val f=Fixture(this);f.ready();f.c.request();advanceUntilIdle();f.gateway.approved=true;f.c.approval();advanceUntilIdle();assertEquals(PairingStage.DESKTOP_APPROVED,f.c.state.value.stage);assertEquals(0,f.gateway.finishes)}
    @Test fun rejectedApprovalDoesNotSaveSession()=runTest{val f=Fixture(this);f.ready();f.c.request();advanceUntilIdle();f.gateway.finishFailure="DESKTOP_APPROVAL_REQUIRED";f.c.finish();advanceUntilIdle();assertTrue(f.c.state.value.message.contains("APPROVAL_PENDING"));assertEquals(0,f.gateway.verifications)}
    @Test fun successfulFinishPerformsAuthenticatedVerification()=runTest{val f=Fixture(this);f.ready();f.c.request();advanceUntilIdle();f.c.finish();advanceUntilIdle();assertEquals(PairingStage.PAIRED_VERIFIED,f.c.state.value.stage);assertEquals(1,f.gateway.verifications);assertEquals(f.now,f.c.state.value.lastSuccess)}
    @Test fun savedPairingAndFailedConnectionAreDifferent()=runTest{val f=Fixture(this);f.ready();f.c.request();advanceUntilIdle();f.gateway.verifyFailure="UNAUTHORIZED";f.c.finish();advanceUntilIdle();assertEquals(PairingStage.PAIRED_UNVERIFIED,f.c.state.value.stage);assertTrue(f.c.state.value.message.contains("REAUTHENTICATION_REQUIRED"))}
    @Test fun finishClearsPrivateInvitationAndChallenge()=runTest{val f=Fixture(this);f.ready();f.c.request();advanceUntilIdle();f.c.finish();advanceUntilIdle();assertEquals("",f.c.input);assertEquals("",f.c.state.value.ticket);assertEquals("",f.c.state.value.comparison)}
    @Test fun cancelClearsSecretsAndTracksOperation()=runTest{val f=Fixture(this);f.ready();f.gateway.wait=true;f.c.request();runCurrent();f.c.cancel();advanceUntilIdle();assertEquals(1,f.tracked.size);assertFalse(f.tracked[0].isActive);assertEquals("",f.c.input);assertEquals(PairingStage.CANCELLED,f.c.state.value.stage)}
    @Test fun targetChangeInvalidatesOldLiveState()=runTest{val f=Fixture(this);f.c.check(f.gateway.pair);advanceUntilIdle();f.c.selected();assertEquals(PairingStage.PAIRED_UNVERIFIED,f.c.state.value.stage);assertNull(f.c.state.value.lastSuccess)}
    @Test fun historicalProofExpiresInsteadOfPermanentOnline()=runTest{val f=Fixture(this);f.c.check(f.gateway.pair);advanceUntilIdle();f.now+=60001;f.c.tick();assertEquals(PairingStage.PAIRED_UNVERIFIED,f.c.state.value.stage)}
    @Test fun diagnosticsExcludeAllPairingSecrets()=runTest{val f=Fixture(this);f.ready();f.c.request();advanceUntilIdle();val text=f.c.diagnostics();assertFalse(text.contains("C".repeat(43)));assertFalse(text.contains(f.gateway.ticket));assertFalse(text.contains("ABC123"));assertFalse(text.contains(f.gateway.pair.token))}
    @Test fun privateExceptionTextCannotBeExported()=runTest{val f=Fixture(this);f.c.scanFailure("secret invitation payload");assertFalse(f.c.diagnostics().contains("secret invitation payload"))}
    @Test fun revocationIsSeparateFromOffline()=runTest{val f=Fixture(this);f.c.revoked();assertEquals(PairingStage.REVOKED,f.c.state.value.stage)}
}
