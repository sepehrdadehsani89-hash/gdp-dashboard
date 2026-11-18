package com.jarvis.assistant.companion

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import java.util.UUID

class PairingInvitationTest {
    private val now=1_700_000_000_000L
    private fun payload()=JSONObject().put("schema_version",1).put("url","https://192.168.100.74:8765").put("certificate_sha256","a".repeat(64)).put("device_id",UUID.randomUUID().toString()).put("pairing_id",UUID.randomUUID().toString()).put("code","C".repeat(43)).put("expires_at",now+120_000).put("device_name","My computer")
    private fun encode(j:JSONObject)=PairingInvitation.PREFIX+Base64.getUrlEncoder().withoutPadding().encodeToString(j.toString().toByteArray())
    private fun rejected(code:String,raw:String,at:Long=now){val e=assertThrows(InvitationFailure::class.java){PairingInvitation.parse(raw,at)};assertEquals(code,e.code);assertFalse(e.message.orEmpty().contains("C".repeat(43)))}
    @Test fun companionWireFormatRetainsEveryIdentity(){val p=payload();val i=PairingInvitation.parse(encode(p),now);assertEquals(p.getString("device_id"),i.deviceId);assertEquals(p.getString("pairing_id"),i.pairingId);assertEquals(p.getString("code"),i.code)}
    @Test fun outerWhitespaceIsAccepted(){assertEquals("My computer",PairingInvitation.parse(" \n\t"+encode(payload())+"\r\n",now).deviceName)}
    @Test fun wrappedPayloadIsAcceptedWithoutTruncation(){val raw=encode(payload());val wrapped=PairingInvitation.PREFIX+raw.removePrefix(PairingInvitation.PREFIX).chunked(55).joinToString("\r\n ");assertEquals(PairingInvitation.normalize(raw),PairingInvitation.normalize(wrapped))}
    @Test fun normalizedPayloadIsIdempotent(){val s=PairingInvitation.normalize(encode(payload()));assertEquals(s,PairingInvitation.normalize(s))}
    @Test fun paddingCanBeTransported(){val p=payload();val e=PairingInvitation.PREFIX+Base64.getUrlEncoder().encodeToString(p.toString().toByteArray());assertEquals(p.getString("pairing_id"),PairingInvitation.parse(e,now).pairingId)}
    @Test fun qrFilenameIsNotAnInvitation(){rejected("INVALID_INVITATION","pairing-qr.svg")}
    @Test fun urlIsNotAnInvitation(){rejected("INVALID_INVITATION","https://192.168.100.74:8765")}
    @Test fun pinIsNotAnInvitation(){rejected("INVALID_INVITATION","a".repeat(64))}
    @Test fun ticketIsNotAnInvitation(){rejected("INVALID_INVITATION",UUID.randomUUID().toString())}
    @Test fun emptyInputIsRejected(){rejected("INVALID_INVITATION","")}
    @Test fun unsupportedPrefixIsExplicit(){rejected("UNSUPPORTED_PROTOCOL",encode(payload()).replace("PAIR-1:","PAIR-2:"))}
    @Test fun unsupportedSchemaIsExplicit(){rejected("UNSUPPORTED_PROTOCOL",encode(payload().put("schema_version",2)))}
    @Test fun numericStringSchemaIsNotAccepted(){rejected("UNSUPPORTED_PROTOCOL",encode(payload().put("schema_version","1")))}
    @Test fun expiryBoundaryIsExpired(){rejected("INVITATION_EXPIRED",encode(payload().put("expires_at",now)))}
    @Test fun expiredInvitationIsExplicit(){rejected("INVITATION_EXPIRED",encode(payload()),now+120001)}
    @Test fun implausibleFutureClockIsExplained(){rejected("INVITATION_CLOCK_OR_EXPIRATION",encode(payload().put("expires_at",now+150001)))}
    @Test fun expirationMustBeAnInteger(){rejected("INVALID_INVITATION",encode(payload().put("expires_at",now.toString())))}
    @Test fun missingCodeIsRejected(){val p=payload();p.remove("code");rejected("INVALID_INVITATION",encode(p))}
    @Test fun publicEndpointIsRejected(){rejected("INVALID_INVITATION",encode(payload().put("url","https://8.8.8.8:8765")))}
    @Test fun endpointCredentialsAreRejected(){rejected("INVALID_INVITATION",encode(payload().put("url","https://user@192.168.100.74:8765")))}
    @Test fun httpEndpointIsRejected(){rejected("INVALID_INVITATION",encode(payload().put("url","http://192.168.100.74:8765")))}
    @Test fun certificateMustBeAFullHexPin(){rejected("INVALID_INVITATION",encode(payload().put("certificate_sha256","a".repeat(63))))}
    @Test fun malformedDeviceIdentityIsRejected(){rejected("INVALID_INVITATION",encode(payload().put("device_id","another device")))}
    @Test fun malformedPairingIdentityIsRejected(){rejected("INVALID_INVITATION",encode(payload().put("pairing_id","wrong")))}
    @Test fun invalidUtf8IsRejected(){val raw=Base64.getUrlEncoder().encodeToString(ByteArray(100){0xff.toByte()});rejected("INVALID_INVITATION",PairingInvitation.PREFIX+raw)}
    @Test fun nonBase64InputIsRejected(){rejected("INVALID_INVITATION",PairingInvitation.PREFIX+"+".repeat(100))}
    @Test fun oversizeInputHasExplicitError(){rejected("INVITATION_TOO_LARGE",PairingInvitation.PREFIX+"A".repeat(17000))}
    @Test fun displayNameControlsAreRejected(){rejected("INVALID_INVITATION",encode(payload().put("device_name","bad\nname")))}
    @Test fun invitationToStringRedactsTheSecret(){assertFalse(PairingInvitation.parse(encode(payload()),now).toString().contains("C".repeat(43)))}
    @Test fun legacyInvitationWithoutNameStillWorks(){val p=payload();p.remove("device_name");assertEquals("Jarvis computer",PairingInvitation.parse(encode(p),now).deviceName)}
    @Test fun challengeMustBeBoundedByInvitation(){val i=PairingInvitation.parse(encode(payload()),now);val c=JSONObject().put("ticket",UUID.randomUUID().toString()).put("compare_code","ABC123").put("expires_at",i.expires+1);assertThrows(CompanionFailure::class.java){PairingChallenge.parse(c,i,now)}}
    @Test fun challengeMustHaveARealTicket(){val i=PairingInvitation.parse(encode(payload()),now);val c=JSONObject().put("ticket",i.url).put("compare_code","ABC123").put("expires_at",i.expires);assertThrows(CompanionFailure::class.java){PairingChallenge.parse(c,i,now)}}
}
