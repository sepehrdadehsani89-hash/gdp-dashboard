package com.jarvis.assistant.companion

import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class DesktopRequest(val query:String?){val action get()=if(query==null)"desktop.open_app" else "desktop.search_web";fun parameters()=if(query==null)JSONObject().put("app","chrome")else JSONObject().put("browser","chrome").put("query",query)}
data class DesktopInvitation(val url:String,val pin:String,val deviceId:String,val pairingId:String,val code:String,val expires:Long)
data class DesktopPair(val url:String,val pin:String,val deviceId:String,val clientId:String,val sessionId:String,val token:String){
    fun json()=JSONObject().put("url",url).put("pin",pin).put("device",deviceId).put("client",clientId).put("session",sessionId).put("token",token).toString()
    companion object {fun parse(raw:String)=JSONObject(raw).let{j->DesktopPair(CompanionPolicy.localUrl(j.getString("url")),j.getString("pin"),j.getString("device"),j.getString("client"),j.getString("session"),j.getString("token")).also{p->require(p.pin.matches(Regex("[a-f0-9]{64}")) && listOf(p.deviceId,p.clientId,p.sessionId).all(CompanionPolicy::uuid) && p.token.matches(Regex("[A-Za-z0-9_-]{43}")))}}}
}
class CompanionFailure(val code:String):java.io.IOException(code)
object CompanionPolicy {
    fun uuid(s:String)=runCatching{UUID.fromString(s).toString()==s}.getOrDefault(false)
    fun sha(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
    fun mac(key:ByteArray,bytes:ByteArray)=Mac.getInstance("HmacSHA256").run{init(SecretKeySpec(key,"HmacSHA256"));doFinal(bytes).joinToString(""){"%02x".format(it)}}
    fun localUrl(text:String):String {
        val u=URI(text);val host=u.host.orEmpty();val pieces=host.split('.')
        require(u.scheme=="https" && pieces.size==4 && pieces.all{it.matches(Regex("0|[1-9][0-9]{0,2}")) && it.toInt() in 0..255})
        val p=pieces.map(String::toInt)
        require(p[0]==10 || p[0]==127 || p[0]==192 && p[1]==168 || p[0]==172 && p[1] in 16..31)
        require(u.port in 1024..65535 && u.rawUserInfo==null && u.rawQuery==null && u.rawFragment==null && u.path in listOf("","/"))
        return text.trimEnd('/')
    }
    fun invitation(text:String,now:Long):DesktopInvitation {
        require(text.length in 100..8192 && text.startsWith("JARVIS-PAIR-1:"))
        val j=JSONObject(Base64.getUrlDecoder().decode(text.removePrefix("JARVIS-PAIR-1:")).toString(Charsets.UTF_8))
        require(j.getInt("schema_version")==1)
        return DesktopInvitation(localUrl(j.getString("url")),j.getString("certificate_sha256"),j.getString("device_id"),j.getString("pairing_id"),j.getString("code"),j.getLong("expires_at")).also{
            require(it.pin.matches(Regex("[a-f0-9]{64}")) && uuid(it.deviceId) && uuid(it.pairingId) && it.code.matches(Regex("[A-Za-z0-9_-]{43}")) && it.expires in (now+1)..(now+150_000))
        }
    }
    fun parse(text:String):DesktopRequest? {
        val clean=text.trim().removeSuffix(".")
        val en=Regex("^(?:(?:hey )?jarvis[, ]+)?(?:please |can you )?(?:open|launch) chrome on (?:my |the )?(?:paired )?(?:(?:windows|mac|linux) )?(?:computer|pc|desktop)(?: (?:and )?(?:search(?: google)? for|look up) (.+))?[?!]?$",RegexOption.IGNORE_CASE).matchEntire(clean)
        val es=Regex("^(?:jarvis[, ]+)?(?:por favor )?abre chrome en (?:mi |la )?(?:computadora|ordenador|pc)(?: y busca (.+))?[?!]?$",RegexOption.IGNORE_CASE).matchEntire(clean)
        val m=en ?: es ?: return null
        val query=m.groups[1]?.value?.trim()?.trimEnd('?','!')
        if(query!=null && (query.isBlank() || query.length>300 || query.toByteArray().size>1000 || query.any(Char::isISOControl)))return null
        return DesktopRequest(query)
    }
    fun searchUrl(query:String):String="https://www.google.com/search?q="+query.toByteArray(Charsets.UTF_8).joinToString(""){b->val n=b.toInt() and 255;if(n in 65..90 || n in 97..122 || n in 48..57 || n in listOf(45,46,95,126))n.toChar().toString() else "%%%02X".format(n)}
    fun command(p:DesktopPair,r:DesktopRequest,id:String,now:Long)=JSONObject().put("schema_version",1).put("command_id",id).put("requester_user_id",p.clientId).put("target_device_id",p.deviceId).put("session_id",p.sessionId).put("issued_at",now).put("expires_at",now+30_000).put("action_type",r.action).put("normalized_parameters",r.parameters()).put("required_capabilities",org.json.JSONArray().put(r.action)).put("risk_level",1).put("idempotency_key",id).put("correlation_id",UUID.randomUUID().toString()).put("nonce",UUID.randomUUID().toString()).put("confirmed",true)
    fun verify(envelope:JSONObject,p:DesktopPair,request:JSONObject,bodyHash:String,now:Long):JSONObject {
        val encoded=envelope.getString("receipt");require(encoded.length<=32768)
        val bytes=Base64.getUrlDecoder().decode(encoded)
        val key=MessageDigest.getInstance("SHA-256").digest(p.token.toByteArray(Charsets.UTF_8))
        val expected=try{mac(key,bytes)}finally{key.fill(0)}
        require(MessageDigest.isEqual(expected.toByteArray(),envelope.getString("signature").toByteArray())){"RECEIPT_SIGNATURE_INVALID"}
        val j=JSONObject(bytes.toString(Charsets.UTF_8));bytes.fill(0)
        require(j.getInt("schema_version")==1 && j.getString("command_id")==request.getString("command_id") && j.getString("target_device_id")==p.deviceId && j.getString("requester_user_id")==p.clientId && j.getString("session_id")==p.sessionId && j.getString("action_type")==request.getString("action_type") && j.getString("request_sha256")==bodyHash)
        require(kotlin.math.abs(now-j.getLong("accepted_at"))<=180_000)
        require(j.getString("state") in setOf("PROCESSING","COMPLETED","FAILED","CANCELLED"))
        if(j.getString("state")=="COMPLETED"){
            val e=j.getJSONObject("verified_postcondition");val query=request.getJSONObject("normalized_parameters").optString("query").takeIf{it.isNotEmpty()}
            val url=if(query==null)"about:blank" else searchUrl(query)
            require(e.getString("kind")=="CHROME_DOCUMENT_LOADED" && e.getString("document_ready")=="complete" && e.getString("url_sha256")==sha(url.toByteArray()))
            if(query!=null)require(e.getInt("http_status") in 200..299)
        }
        return j
    }
}
