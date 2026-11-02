package com.jarvis.assistant.hud

import java.util.UUID

enum class AuthorizationStatus { AUTHORIZATION_REQUIRED, AWAITING_CODEX, IDENTITY_CONFIRMATION, AUTHORIZED, DENIED, EXPIRED }
data class AuthorizationRequest(val id:String,val action:String,val expiresAt:Long,val status:AuthorizationStatus)
/** A spoken control word is a prompt trigger, never proof of identity. One action, one use. */
class AuthorizationCoordinator(private val now:()->Long,private val timeout:Long=90_000) {
    private var request:AuthorizationRequest?=null
    @Synchronized fun begin(action:String):AuthorizationRequest = AuthorizationRequest(UUID.randomUUID().toString(),action,now()+timeout,AuthorizationStatus.AUTHORIZATION_REQUIRED).also{request=it}
    @Synchronized fun awaiting():AuthorizationRequest? = transition(AuthorizationStatus.AWAITING_CODEX)
    @Synchronized fun codex():AuthorizationRequest? = current()?.let { if(it.status in listOf(AuthorizationStatus.AUTHORIZATION_REQUIRED,AuthorizationStatus.AWAITING_CODEX)) transition(AuthorizationStatus.IDENTITY_CONFIRMATION) else it }
    @Synchronized fun current():AuthorizationRequest? { val r=request; if(r!=null && now()>=r.expiresAt && r.status in listOf(AuthorizationStatus.AUTHORIZATION_REQUIRED,AuthorizationStatus.AWAITING_CODEX,AuthorizationStatus.IDENTITY_CONFIRMATION))request=r.copy(status=AuthorizationStatus.EXPIRED); return request }
    @Synchronized fun complete(id:String,success:Boolean):Boolean {
        val r=current() ?: return false
        if(r.id!=id || r.status!=AuthorizationStatus.IDENTITY_CONFIRMATION)return false
        request=r.copy(status=if(success)AuthorizationStatus.AUTHORIZED else AuthorizationStatus.DENIED)
        return success
    }
    @Synchronized fun cancel(){request=request?.copy(status=AuthorizationStatus.DENIED)}
    private fun transition(s:AuthorizationStatus):AuthorizationRequest? { val r=current() ?: return null; if(r.status in listOf(AuthorizationStatus.DENIED,AuthorizationStatus.EXPIRED,AuthorizationStatus.AUTHORIZED))return r;return r.copy(status=s).also{request=it} }
    companion object { fun isCodex(text:String)=text.trim().lowercase().trimEnd('.','!','؟','?') in setOf("codex","códex","کدکس","کدکس را فعال کن") }
}
