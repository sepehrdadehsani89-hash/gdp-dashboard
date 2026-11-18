package com.jarvis.assistant.companion

import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.Base64

class InvitationFailure(val code:String):IllegalArgumentException(code)

/** The established wire format; only transport whitespace is normalized, never truncated. */
object PairingInvitation {
    const val PREFIX="JARVIS-PAIR-1:"
    fun normalize(raw:String):String {
        if(raw.length>16_384)throw InvitationFailure("INVITATION_TOO_LARGE")
        val text=raw.trim()
        if(!text.startsWith(PREFIX))throw InvitationFailure(if(text.startsWith("JARVIS-PAIR-"))"UNSUPPORTED_PROTOCOL" else "INVALID_INVITATION")
        val payload=text.removePrefix(PREFIX).filterNot{it in " \t\r\n"}
        if(payload.length !in 80..8192 || !(Regex("[A-Za-z0-9_-]+={0,2}").matches(payload)))throw InvitationFailure("INVALID_INVITATION")
        return PREFIX+payload
    }
    fun parse(raw:String,now:Long):DesktopInvitation {
        try {
            val encoded=normalize(raw).removePrefix(PREFIX)
            val bytes=Base64.getUrlDecoder().decode(encoded)
            if(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)!=encoded.trimEnd('='))throw InvitationFailure("INVALID_INVITATION")
            val decoder=Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            val j=JSONObject(decoder.decode(ByteBuffer.wrap(bytes)).toString())
            val version=j.get("schema_version")
            if(version !is Int || version!=1)throw InvitationFailure("UNSUPPORTED_PROTOCOL")
            val expires=j.get("expires_at")
            if(expires !is Long && expires !is Int)throw InvitationFailure("INVALID_INVITATION")
            val expiry=(expires as Number).toLong()
            if(expiry<=now)throw InvitationFailure("INVITATION_EXPIRED")
            if(expiry-now !in 1..150_000)throw InvitationFailure("INVITATION_CLOCK_OR_EXPIRATION")
            fun string(name:String)=j.get(name).also{require(it is String)} as String
            val name=if(j.has("device_name"))string("device_name") else "Jarvis computer"
            require(name.length in 1..60 && name.none(Char::isISOControl))
            return DesktopInvitation(CompanionPolicy.localUrl(string("url")),string("certificate_sha256"),string("device_id"),string("pairing_id"),string("code"),expiry,name).also{
                require(it.pin.matches(Regex("[a-f0-9]{64}")) && CompanionPolicy.uuid(it.deviceId) && CompanionPolicy.uuid(it.pairingId) && it.code.matches(Regex("[A-Za-z0-9_-]{43}")))
            }
        }catch(e:InvitationFailure){throw e}catch(_:Exception){throw InvitationFailure("INVALID_INVITATION")}
    }
}

data class PairingChallenge(val ticket:String,val comparison:String,val expires:Long){
    override fun toString()="PairingChallenge(comparison=redacted, ticket=redacted)"
    companion object {
        fun parse(j:JSONObject,i:DesktopInvitation,now:Long):PairingChallenge {
            try {
                val result=PairingChallenge(j.getString("ticket"),j.getString("compare_code"),j.getLong("expires_at"))
                if(result.expires<=now)throw CompanionFailure("PAIRING_EXPIRED")
                require(CompanionPolicy.uuid(result.ticket) && result.comparison.matches(Regex("[A-F0-9]{6}")) && result.expires<=i.expires)
                return result
            }catch(e:CompanionFailure){throw e}catch(_:Exception){throw CompanionFailure("INVALID_PAIRING_CHALLENGE")}
        }
    }
}
