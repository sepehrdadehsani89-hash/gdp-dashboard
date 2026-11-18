package com.jarvis.assistant.companion

import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/** Actual production JVM client -> actual loopback Python HTTPS, never a handset/Windows test. */
class CompanionTlsIntegrationTest {
    private class Fixture(mode:String="normal"):AutoCloseable {
        private val python=System.getenv("JARVIS_COMPANION_PYTHON").orEmpty()
        private val root=generateSequence(File(System.getProperty("user.dir")).absoluteFile){it.parentFile}.first{File(it,"companion/tests/https_client_fixture.py").isFile}
        private val temporary=Files.createTempDirectory("jarvis-interop-").toFile()
        private val process:Process
        private val reader:java.io.BufferedReader
        private val writer:java.io.BufferedWriter
        private var saved:String?=null
        val registry=DesktopRegistry({saved},{saved!=null},{saved=it},{null},{false})
        val client=CompanionClient(registry)
        val invitation:DesktopInvitation
        val id=UUID.randomUUID().toString();val nonce=UUID.randomUUID().toString()
        init{
            assumeTrue("Set JARVIS_COMPANION_PYTHON to an existing compatible test environment; real Python HTTPS fixture unavailable",python.isNotBlank() && File(python).canExecute())
            process=ProcessBuilder(python,File(root,"companion/tests/https_client_fixture.py").path,"--state",temporary.path,"--mode",mode).redirectError(File(temporary,"fixture-stderr.log")).start()
            reader=process.inputStream.bufferedReader();writer=process.outputStream.bufferedWriter()
            invitation=PairingInvitation.parse(read().getString("invitation"),System.currentTimeMillis())
        }
        private fun read():JSONObject{val line=CompletableFuture.supplyAsync{reader.readLine()}.get(10,TimeUnit.SECONDS);check(line!=null){"HTTPS fixture did not respond"};return JSONObject(line)}
        fun operator(op:String,ticket:String?=null):JSONObject {writer.write(JSONObject().put("op",op).put("ticket",ticket).toString());writer.newLine();writer.flush();return read()}
        suspend fun challenge(i:DesktopInvitation=invitation)=PairingChallenge.parse(client.requestPair(i,id,nonce),i,System.currentTimeMillis())
        suspend fun pair():DesktopPair{val c=challenge();check(operator("approve",c.ticket).getBoolean("approved"));return client.finishPair(invitation,c.ticket,id,nonce)}
        override fun close(){runCatching{operator("stop")};if(!process.waitFor(5,TimeUnit.SECONDS))process.destroyForcibly();reader.close();writer.close();temporary.deleteRecursively()}
    }
    private suspend fun failure(code:String,block:suspend ()->Unit){try{block();fail("Expected a rejected operation")}catch(e:CompanionFailure){assertEquals(code,e.code)}}
    @Test fun actualPinnedPublicHealthDoesNotCreatePairingOrAuthProof()=runBlocking{Fixture().use{f->f.client.inspectInvitation(f.invitation);assertNull(f.client.paired());assertNull(f.client.connectionProof.value);assertEquals(0,f.operator("stats").getInt("pending"))}}
    @Test fun actualTlsPairingCreatesSavedAndAuthenticatedSession()=runBlocking{Fixture().use{f->val p=f.pair();assertEquals(1,f.registry.snapshot().devices.size);f.client.verifyPair(p);assertTrue(DesktopConnectionProofPolicy.current(f.client.connectionProof.value,p,System.currentTimeMillis()))}}
    @Test fun actualTlsFinishRequiresDesktopApproval()=runBlocking{Fixture().use{f->val c=f.challenge();failure("DESKTOP_APPROVAL_REQUIRED"){f.client.finishPair(f.invitation,c.ticket,f.id,f.nonce)};assertNull(f.client.paired())}}
    @Test fun exactCertificatePinIsActuallyEnforced()=runBlocking{Fixture().use{f->failure("TLS_PIN_OR_CERTIFICATE_ERROR"){f.challenge(f.invitation.copy(pin="0".repeat(64)))}}}
    @Test fun pinnedCertificateStillRequiresMatchingHostname()=runBlocking{Fixture("wrong-host").use{f->failure("TLS_PIN_OR_CERTIFICATE_ERROR"){f.challenge()}}}
    @Test fun expiredPinnedCertificateIsActuallyRejected()=runBlocking{Fixture("expired").use{f->failure("TLS_PIN_OR_CERTIFICATE_ERROR"){f.challenge()}}}
    @Test fun notYetValidPinnedCertificateIsActuallyRejected()=runBlocking{Fixture("future").use{f->failure("TLS_PIN_OR_CERTIFICATE_ERROR"){f.challenge()}}}
    @Test fun wrongInvitationSecretCannotRequestATicket()=runBlocking{Fixture().use{f->failure("PAIRING_PROOF_INVALID"){f.challenge(f.invitation.copy(code="A".repeat(43)))}}}
    @Test fun wrongPairingIdentityCannotRequestATicket()=runBlocking{Fixture().use{f->failure("PAIRING_PROOF_INVALID"){f.challenge(f.invitation.copy(pairingId=UUID.randomUUID().toString()))}}}
    @Test fun expiredInvitationIsRejectedBeforeTransmission()=runBlocking{Fixture().use{f->failure("INVITATION_EXPIRED"){f.challenge(f.invitation.copy(expires=System.currentTimeMillis()-1))};assertEquals(0,f.operator("stats").getInt("pending"))}}
    @Test fun identicalRealHttpsRetryReturnsSameChallenge()=runBlocking{Fixture().use{f->assertEquals(f.challenge(),f.challenge());assertEquals(1,f.operator("stats").getInt("pending"))}}
    @Test fun completionRetryRetainsSameSessionAndRegistryEntry()=runBlocking{Fixture().use{f->val c=f.challenge();f.operator("approve",c.ticket);val a=f.client.finishPair(f.invitation,c.ticket,f.id,f.nonce);val b=f.client.finishPair(f.invitation,c.ticket,f.id,f.nonce);assertEquals(a,b);assertEquals(1,f.registry.snapshot().devices.size)}}
    @Test fun realDesktopDenialIsNotAConnection()=runBlocking{Fixture().use{f->val c=f.challenge();f.operator("deny",c.ticket);failure("PAIRING_DENIED"){f.client.pairingStatus(f.invitation,c,f.id,f.nonce)};assertNull(f.client.paired())}}
    @Test fun realPendingStatusIsNotApproval()=runBlocking{Fixture().use{f->val c=f.challenge();assertFalse(f.client.pairingStatus(f.invitation,c,f.id,f.nonce));assertNull(f.client.paired())}}
    @Test fun realApprovedStatusDoesNotSaveBeforePhoneFinishes()=runBlocking{Fixture().use{f->val c=f.challenge();f.operator("approve",c.ticket);assertTrue(f.client.pairingStatus(f.invitation,c,f.id,f.nonce));assertNull(f.client.paired())}}
    @Test fun wrongCompletionNonceIsRejectedByActualServer()=runBlocking{Fixture().use{f->val c=f.challenge();f.operator("approve",c.ticket);failure("PAIRING_PROOF_INVALID"){f.client.finishPair(f.invitation,c.ticket,f.id,UUID.randomUUID().toString())};assertNull(f.client.paired())}}
    @Test fun realRevokeInvalidatesTheSavedSession()=runBlocking{Fixture().use{f->val p=f.pair();f.client.revoke();assertNull(f.client.paired());f.registry.add(p);failure("UNAUTHORIZED"){f.client.verifyPair(p)}}}
    @Test fun restartClientReusesSavedTrustWithoutRescanning()=runBlocking{Fixture().use{f->val p=f.pair();val next=CompanionClient(f.registry);next.verifyPair(p);assertNotNull(next.connectionProof.value)}}
    @Test fun badCapabilitySignatureCannotVerifySession()=runBlocking{Fixture("bad-signature").use{f->val p=f.pair();failure("CAPABILITY_VERIFICATION_FAILED"){f.client.verifyPair(p)};assertNull(f.client.connectionProof.value)}}
    @Test fun signedWrongNonceCannotVerifySession()=runBlocking{Fixture("wrong-nonce").use{f->val p=f.pair();failure("CAPABILITY_VERIFICATION_FAILED"){f.client.verifyPair(p)}}}
    @Test fun signedWrongDeviceCannotVerifySession()=runBlocking{Fixture("wrong-device").use{f->val p=f.pair();failure("CAPABILITY_VERIFICATION_FAILED"){f.client.verifyPair(p)}}}
    @Test fun signedWrongSessionCannotVerifySession()=runBlocking{Fixture("wrong-session").use{f->val p=f.pair();failure("CAPABILITY_VERIFICATION_FAILED"){f.client.verifyPair(p)}}}
    @Test fun signedStaleManifestCannotVerifySession()=runBlocking{Fixture("stale-manifest").use{f->val p=f.pair();failure("CAPABILITY_VERIFICATION_FAILED"){f.client.verifyPair(p)}}}
    @Test fun realRedirectIsNotFollowed()=runBlocking{Fixture("redirect").use{f->failure("REQUEST_REJECTED"){f.challenge()};val requests=f.operator("stats").getJSONObject("requests");assertFalse(requests.has("/redirect-target"))}}
    @Test fun oversizedRealResponseIsRejected()=runBlocking{Fixture("oversize").use{f->failure("RECEIPT_TOO_LARGE"){f.challenge()}}}
    @Test fun malformedRealJsonIsRejected()=runBlocking{Fixture("invalid-json").use{f->failure("INVALID_DESKTOP_RESPONSE"){f.challenge()}}}
    @Test fun malformedRealChallengeDoesNotSavePairing()=runBlocking{Fixture("bad-challenge").use{f->failure("INVALID_PAIRING_CHALLENGE"){f.challenge()};assertNull(f.client.paired())}}
    @Test fun cancellingActualHttpsDoesNotClaimPaired()=runBlocking{Fixture("delay").use{f->val request=launch{f.challenge()};delay(50);request.cancelAndJoin();assertNull(f.client.paired());assertNull(f.client.connectionProof.value)}}
}
