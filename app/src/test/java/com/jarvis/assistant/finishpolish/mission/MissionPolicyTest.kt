package com.jarvis.assistant.finishpolish.mission

import com.jarvis.assistant.companion.*
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import java.security.MessageDigest
import java.util.UUID

class MissionPolicyTest {
    private val pair=DesktopPair("https://192.168.1.8:8765","a".repeat(64),UUID.randomUUID().toString(),UUID.randomUUID().toString(),UUID.randomUUID().toString(),"b".repeat(43))
    private val nonce=UUID.randomUUID().toString()
    private fun manifest()=JSONObject().put("schema_version",1).put("service","jarvis-companion").put("device_id",pair.deviceId).put("client_id",pair.clientId).put("session_id",pair.sessionId).put("request_nonce",nonce).put("sampled_at",100_000L).put("agent_version","1.1").put("platform","linux").put("capabilities",JSONArray().put(JSONObject().put("action_id","desktop.search_web").put("status","REQUIRES_CONFIRMATION")))
    private fun sign(j:JSONObject):JSONObject {
        val bytes=j.toString().toByteArray();val key=MessageDigest.getInstance("SHA-256").digest(pair.token.toByteArray())
        return JSONObject().put("manifest",Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)).put("signature",CompanionPolicy.mac(key,bytes))
    }
    private fun rejects(block:()->Unit){try{block();fail("Expected verification rejection")}catch(_:IllegalArgumentException){}}
    @Test fun authenticatedManifestBindsTargetAndSessionButIsNotExecutionProof(){val ready=MissionPolicy.parseManifest(sign(manifest()),pair,nonce,100_000);assertTrue(ready.authenticated);assertTrue(ready.allows("desktop.search_web"));assertEquals(pair.sessionId,ready.sessionId);assertFalse(ready.allows("desktop.remote_shell"))}
    @Test fun wrongManifestSignatureCannotGrantCapabilities(){val envelope=sign(manifest()).put("signature","0".repeat(64));rejects{MissionPolicy.parseManifest(envelope,pair,nonce,100_000)}}
    @Test fun nonceReplayCannotMasqueradeAsAFreshCheck(){rejects{MissionPolicy.parseManifest(sign(manifest()),pair,UUID.randomUUID().toString(),100_000)}}
    @Test fun signedManifestForAnotherClientDeviceOrSessionIsRejected(){for(field in listOf("device_id","client_id","session_id"))rejects{MissionPolicy.parseManifest(sign(manifest().put(field,UUID.randomUUID().toString())),pair,nonce,100_000)}}
    @Test fun staleAndFutureCapabilitySamplesAreNotCurrentReadiness(){for(at in listOf(1L,200_001L))rejects{MissionPolicy.parseManifest(sign(manifest().put("sampled_at",at)),pair,nonce,100_000)}}
    @Test fun unavailableOrDeniedCapabilitiesCannotExecute(){for(status in listOf("UNAVAILABLE","PERMISSION_DENIED")){val j=manifest();j.getJSONArray("capabilities").getJSONObject(0).put("status",status);assertFalse(MissionPolicy.parseManifest(sign(j),pair,nonce,100_000).allows("desktop.search_web"))}}
    @Test fun unsupportedStatusesAndDuplicateCapabilitiesAreRejected(){val j=manifest();j.getJSONArray("capabilities").getJSONObject(0).put("status","DONE");rejects{MissionPolicy.parseManifest(sign(j),pair,nonce,100_000)};val duplicate=manifest();duplicate.getJSONArray("capabilities").put(duplicate.getJSONArray("capabilities").getJSONObject(0));rejects{MissionPolicy.parseManifest(sign(duplicate),pair,nonce,100_000)}}
    @Test fun progressNeverImpliesVerificationOrRegressesARealRunningTask(){val requested=MissionState(jobId="job",phase="AUTHORIZED",active=true);val sent=MissionPolicy.progress(requested,"job",CompanionProgress.SENT);val running=MissionPolicy.progress(sent,"job",CompanionProgress.RUNNING);assertEquals("RUNNING",running.phase);assertFalse(running.verified);assertEquals(running,MissionPolicy.progress(running,"job",CompanionProgress.SENT))}
    @Test fun staleCancelledAndTerminalProgressEventsCannotResurrectAMission(){for(state in listOf(MissionState(jobId="current",phase="VERIFIED",verified=true),MissionState(jobId="current",phase="CANCEL_REQUESTED",active=true),MissionState(jobId="current",phase="CANCELLED"))){assertEquals(state,MissionPolicy.progress(state,"old",CompanionProgress.SENT));assertEquals(state,MissionPolicy.progress(state,"current",CompanionProgress.SENT))}}
    @Test fun reviewedIdentityCannotMoveToANewComputerOrRepairedSession(){
        assertTrue(MissionPolicy.reviewedIdentityMatches(pair.deviceId,pair.sessionId,pair.deviceId,pair.sessionId))
        assertFalse(MissionPolicy.reviewedIdentityMatches(UUID.randomUUID().toString(),pair.sessionId,pair.deviceId,pair.sessionId))
        assertFalse(MissionPolicy.reviewedIdentityMatches(pair.deviceId,UUID.randomUUID().toString(),pair.deviceId,pair.sessionId))
        assertFalse(MissionPolicy.reviewedIdentityMatches(null,null,pair.deviceId,pair.sessionId))
    }
    @Test fun malformedTimestampCannotExploitSignedLongOverflow(){rejects{MissionPolicy.parseManifest(sign(manifest().put("sampled_at",Long.MIN_VALUE)),pair,nonce,100_000)}}
    @Test fun friendlyNamesCannotContainControlCharactersOrBeEmpty(){assertEquals("My laptop",MissionPolicy.friendly(" My laptop "));for(s in listOf("","a\nb","x".repeat(61)))rejects{MissionPolicy.friendly(s)}}
}
