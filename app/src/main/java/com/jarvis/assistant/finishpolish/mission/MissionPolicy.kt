package com.jarvis.assistant.finishpolish.mission

import com.jarvis.assistant.companion.*
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64

/** Metadata-only availability; browser execution must still produce its own verified receipt. */
data class DesktopCapability(val actionId:String,val status:String,val reason:String?)
data class DesktopReadiness(val deviceId:String,val platform:String,val sampledAt:Long,val agentVersion:String,val capabilities:List<DesktopCapability>,val authenticated:Boolean,val sessionId:String?=null){
    fun allows(action:String)=capabilities.any{it.actionId==action && it.status in setOf("SUPPORTED","REQUIRES_CONFIRMATION")}
}
object MissionPolicy {
    const val DEVICE_NAME_KEY="mission_device_name_v_one"
    val phases=setOf("IDLE","CHECKING","CONNECTED","OFFLINE","AWAITING_PAIRING","REQUESTED","AUTHORIZED","SENT","RUNNING","VERIFIED","FAILED","NOT_SENT","CANCEL_REQUESTED","CANCELLED")
    fun progress(current:MissionState,id:String,event:CompanionProgress):MissionState {
        if(current.jobId!=id || !current.active || current.phase=="CANCEL_REQUESTED" || current.phase=="RUNNING" && event==CompanionProgress.SENT)return current
        return current.copy(phase=event.name,detail=if(event==CompanionProgress.SENT)"Request transmitted; waiting for the computer. No completed result yet." else "Computer accepted the command and reports processing. Waiting for verified postcondition.",verified=false)
    }
    fun reviewedIdentityMatches(actualDevice:String?,actualSession:String?,reviewedDevice:String,reviewedSession:String)=
        actualDevice==reviewedDevice && actualSession==reviewedSession && CompanionPolicy.uuid(reviewedDevice) && CompanionPolicy.uuid(reviewedSession)
    fun friendly(value:String):String=value.trim().also{require(it.length in 1..60 && it.none(Char::isISOControl))}
    fun parseManifest(envelope:JSONObject,p:DesktopPair,nonce:String,now:Long):DesktopReadiness {
        val raw=envelope.getString("manifest");require(raw.length<=32768)
        val bytes=Base64.getUrlDecoder().decode(raw)
        val key=MessageDigest.getInstance("SHA-256").digest(p.token.toByteArray())
        val expected=try{CompanionPolicy.mac(key,bytes)}finally{key.fill(0)}
        require(MessageDigest.isEqual(expected.toByteArray(),envelope.getString("signature").toByteArray()))
        val j=try{JSONObject(bytes.toString(Charsets.UTF_8))}finally{bytes.fill(0)}
        require(j.getInt("schema_version")==1 && j.getString("service")=="jarvis-companion" && j.getString("device_id")==p.deviceId && j.getString("client_id")==p.clientId && j.getString("session_id")==p.sessionId && j.getString("request_nonce")==nonce)
        val at=j.getLong("sampled_at");require(at>0 && now>0 && kotlin.math.abs(now-at)<=90_000)
        val platform=j.getString("platform");require(platform in setOf("windows","macos","linux","unsupported"))
        val array=j.getJSONArray("capabilities");require(array.length() in 1..30)
        val capabilities=(0 until array.length()).map{i->array.getJSONObject(i).let{c->
            val action=c.getString("action_id");require(action.matches(Regex("desktop\\.[a-z_]{2,40}")))
            val status=c.getString("status");require(status in setOf("SUPPORTED","REQUIRES_CONFIRMATION","UNAVAILABLE","PERMISSION_DENIED"))
            DesktopCapability(action,status,c.optString("reason").takeIf{it.matches(Regex("[A-Z_]{3,60}"))})
        }}
        require(capabilities.map{it.actionId}.distinct().size==capabilities.size)
        return DesktopReadiness(p.deviceId,platform,at,j.getString("agent_version").take(30),capabilities,true,p.sessionId)
    }
}

data class MissionState(val jobId:String="",val phase:String="IDLE",val deviceId:String?=null,val label:String="My computer",val detail:String="No mission requested",val sampledAt:Long=0,val readiness:DesktopReadiness?=null,val active:Boolean=false,val verified:Boolean=false)
data class MissionOutcome(val verified:Boolean,val detail:String,val errorCode:String?=null){val message get()=detail}
