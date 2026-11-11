package com.jarvis.assistant.companion

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import java.util.UUID
import java.security.MessageDigest

class CompanionPolicyTest {
    private val now=1_700_000_000_000L
    private val pair=DesktopPair("https://192.168.1.8:8765","a".repeat(64),UUID.randomUUID().toString(),UUID.randomUUID().toString(),UUID.randomUUID().toString(),"A".repeat(43))
    private fun command()=CompanionPolicy.command(pair,DesktopRequest("ChatGPT"),UUID.randomUUID().toString(),now)
    private fun receipt(request:JSONObject)=JSONObject().put("schema_version",1).put("command_id",request.getString("command_id")).put("target_device_id",pair.deviceId).put("requester_user_id",pair.clientId).put("session_id",pair.sessionId).put("action_type",request.getString("action_type")).put("request_sha256",CompanionPolicy.sha(request.toString().toByteArray())).put("accepted_at",now).put("state","COMPLETED").put("verified_postcondition",JSONObject().put("kind","CHROME_DOCUMENT_LOADED").put("document_ready","complete").put("http_status",200).put("url_sha256",CompanionPolicy.sha(CompanionPolicy.searchUrl("ChatGPT").toByteArray())))
    private fun signed(value:JSONObject):JSONObject {
        val bytes=value.toString().toByteArray();val key=MessageDigest.getInstance("SHA-256").digest(pair.token.toByteArray())
        return JSONObject().put("receipt",Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)).put("signature",CompanionPolicy.mac(key,bytes))
    }
    @Test fun desktopTargetIsRequiredSoPhoneAppLaunchingIsPreserved(){
        assertNull(CompanionPolicy.parse("open Chrome"));assertNull(CompanionPolicy.parse("open my bank"));assertEquals(DesktopRequest(null),CompanionPolicy.parse("Jarvis, open Chrome on my computer."));assertEquals(DesktopRequest("ChatGPT"),CompanionPolicy.parse("open Chrome on my paired Windows computer and search for ChatGPT"))
        assertEquals(DesktopRequest("el clima"),CompanionPolicy.parse("abre Chrome en mi computadora y busca el clima"))
    }
    @Test fun queriesAreDataAndCannotAddOtherDesktopActions(){
        assertNull(CompanionPolicy.parse("run powershell on my computer"));assertNull(CompanionPolicy.parse("open Chrome on my computer and search for " + "x".repeat(301)))
        assertEquals("https://www.google.com/search?q=%24%28danger%29%3B%20%22%2A",CompanionPolicy.searchUrl("$(danger); \"*"))
        assertTrue(CompanionPolicy.searchUrl("hola señor").endsWith("hola%20se%C3%B1or"))
    }
    @Test fun endpointIsHttpsPrivateIpv4WithNoCredentialQueryRedirectOrPublicHost(){
        assertEquals(pair.url,CompanionPolicy.localUrl(pair.url+"/"))
        for(url in listOf("http://192.168.1.8:8765","https://8.8.8.8:8765","https://example.com:8765","https://127.0.0.1:8765/?token=x","https://user@192.168.1.8:8765","https://0177.0.0.1:8765","https://192.168.1.8:8765/path"))assertThrows(url,IllegalArgumentException::class.java){CompanionPolicy.localUrl(url)}
    }
    @Test fun invitationExpiresAndRequiresARealCertificatePin(){
        val j=JSONObject().put("schema_version",1).put("url",pair.url).put("certificate_sha256",pair.pin).put("device_id",pair.deviceId).put("pairing_id",UUID.randomUUID().toString()).put("code","C".repeat(43)).put("expires_at",now+120000)
        fun encode()="JARVIS-PAIR-1:"+Base64.getUrlEncoder().withoutPadding().encodeToString(j.toString().toByteArray())
        assertEquals(pair.pin,CompanionPolicy.invitation(encode(),now).pin)
        assertThrows(IllegalArgumentException::class.java){CompanionPolicy.invitation(encode(),now+120001)}
        j.put("certificate_sha256","trust-all");assertThrows(IllegalArgumentException::class.java){CompanionPolicy.invitation(encode(),now)}
    }
    @Test fun verifiedReceiptBindsExactRequestAndLoadedPage(){val r=command();val e=signed(receipt(r));assertEquals("COMPLETED",CompanionPolicy.verify(e,pair,r,CompanionPolicy.sha(r.toString().toByteArray()),now).getString("state"))}
    @Test fun modifiedMacCannotReportCompletion(){val r=command();val e=signed(receipt(r)).put("signature","0".repeat(64));assertThrows(IllegalArgumentException::class.java){CompanionPolicy.verify(e,pair,r,CompanionPolicy.sha(r.toString().toByteArray()),now)}}
    @Test fun evenSignedReceiptsCannotSubstituteAnotherSessionDeviceOrCommand(){
        for(k in listOf("command_id","target_device_id","requester_user_id","session_id","request_sha256","action_type")){
            val r=command();val v=receipt(r).put(k,"different");assertThrows(k,IllegalArgumentException::class.java){CompanionPolicy.verify(signed(v),pair,r,CompanionPolicy.sha(r.toString().toByteArray()),now)}
        }
    }
    @Test fun acknowledgementAndFailuresAreNeverUpgradedToCompleted(){
        val r=command();val v=receipt(r).put("state","PROCESSING").put("verified_postcondition",JSONObject.NULL)
        assertEquals("PROCESSING",CompanionPolicy.verify(signed(v),pair,r,CompanionPolicy.sha(r.toString().toByteArray()),now).getString("state"))
    }
    @Test fun wrongPageOrHttpErrorCannotBeClaimedAsACompletedSearch(){
        for(error in listOf("url_sha256","http_status","document_ready")){
            val r=command();val v=receipt(r);v.getJSONObject("verified_postcondition").put(error,if(error=="http_status")404 else "wrong")
            assertThrows(IllegalArgumentException::class.java){CompanionPolicy.verify(signed(v),pair,r,CompanionPolicy.sha(r.toString().toByteArray()),now)}
        }
    }
    @Test fun oldSignedReceiptIsRejected(){val r=command();assertThrows(IllegalArgumentException::class.java){CompanionPolicy.verify(signed(receipt(r)),pair,r,CompanionPolicy.sha(r.toString().toByteArray()),now+180001)}}
    @Test fun pairingCredentialsRoundTripWithoutChangingStoredIdentity(){assertEquals(pair,DesktopPair.parse(pair.json()))}
}
