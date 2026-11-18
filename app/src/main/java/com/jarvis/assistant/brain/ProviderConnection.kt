package com.jarvis.assistant.brain

enum class ProviderCredentialMode { API_KEY, CHATGPT_PLAN, LOCAL }
data class ProviderConfig(val provider:String,val model:String,val mode:ProviderCredentialMode=ProviderCredentialMode.API_KEY) {
    val id:String get()="$provider:${mode.name}"
    fun validated():ProviderConfig {
        require(provider in setOf("openai","anthropic","gemini","local")){"Unsupported provider"}
        require((mode==ProviderCredentialMode.CHATGPT_PLAN && provider=="openai") || (mode==ProviderCredentialMode.LOCAL && provider=="local") || (mode==ProviderCredentialMode.API_KEY && provider!="local")){"Unsupported provider credential mode"}
        val apiModel=model.removePrefix(if(provider=="gemini")"models/" else "")
        require(apiModel.isNotBlank() && apiModel.length<=100 && apiModel.matches(Regex("[A-Za-z0-9._:-]+"))){"Invalid model ID"}
        return this
    }
}
enum class ProviderConnectionStatus { NOT_CONFIGURED, CONFIGURED, VERIFYING, VERIFIED, AUTHORIZATION_REQUIRED, IDENTITY_ONLY, PREVIOUSLY_VERIFIED, EXPIRED, FAILED }
enum class ProviderFailureKind { AUTHENTICATION, AUTHORIZATION, RATE_LIMIT, CREDITS, USAGE_UNAVAILABLE, MODEL_UNAVAILABLE, TIMEOUT, NETWORK, INCOMPLETE, NOT_VERIFIED, CANCELLED, BAD_REQUEST, BACKEND_UNAVAILABLE, UNKNOWN }
data class ProviderConnection(val config:ProviderConfig,val status:ProviderConnectionStatus,val verifiedAt:Long?=null,val message:String="",val failure:ProviderFailureKind?=null,val elapsedMs:Long?=null,val httpStatus:Int?=null,val retryAt:Long?=null,val resolvedModel:String?=null) {
    val diagnosticState:AiConnectionState get()=when(status){
        ProviderConnectionStatus.NOT_CONFIGURED->AiConnectionState.NOT_CONFIGURED
        ProviderConnectionStatus.VERIFYING->AiConnectionState.VALIDATING
        ProviderConnectionStatus.VERIFIED->AiConnectionState.CONNECTED_VERIFIED
        ProviderConnectionStatus.PREVIOUSLY_VERIFIED->AiConnectionState.PREVIOUSLY_VERIFIED
        ProviderConnectionStatus.FAILED->when(failure){
            ProviderFailureKind.AUTHENTICATION->AiConnectionState.AUTHENTICATION_FAILED
            ProviderFailureKind.CREDITS->AiConnectionState.INSUFFICIENT_QUOTA
            ProviderFailureKind.RATE_LIMIT->AiConnectionState.RATE_LIMITED
            ProviderFailureKind.NETWORK->AiConnectionState.NETWORK_UNAVAILABLE
            ProviderFailureKind.TIMEOUT->AiConnectionState.TIMEOUT
            ProviderFailureKind.MODEL_UNAVAILABLE->AiConnectionState.MODEL_UNAVAILABLE
            ProviderFailureKind.BACKEND_UNAVAILABLE->AiConnectionState.BACKEND_UNAVAILABLE
            else->AiConnectionState.PROVIDER_ERROR
        }
        else->AiConnectionState.NOT_VERIFIED
    }
    fun retryAllowed(now:Long)=retryAt==null || now>=retryAt
}
enum class AiConnectionState { NOT_CONFIGURED, VALIDATING, CONNECTED_VERIFIED, PREVIOUSLY_VERIFIED, NOT_VERIFIED, AUTHENTICATION_FAILED, INSUFFICIENT_QUOTA, RATE_LIMITED, NETWORK_UNAVAILABLE, TIMEOUT, MODEL_UNAVAILABLE, BACKEND_UNAVAILABLE, PROVIDER_ERROR }

/** Bearer material is transient and deliberately absent from toString and persistent proofs. */
class ChatGptPlanAccess(val bearer:String,val accountId:String,val scopes:Set<String>,val revision:Long) {
    override fun toString()="ChatGptPlanAccess(redacted)"
    companion object {const val INFERENCE_SCOPE="chatgpt.tokens.use.direct"}
}
data class ProviderVerificationProof internal constructor(val config:ProviderConfig,val credentialBinding:String,val verifiedAt:Long,val nonce:String)

interface ProviderConnectionRepository {
    fun proof(id:String):ProviderVerificationProof?
    fun save(proof:ProviderVerificationProof)
    fun remove(id:String)
    fun mode(provider:String):ProviderCredentialMode
    fun setMode(provider:String,mode:ProviderCredentialMode)
    fun configuredModel(id:String):String?
    fun saveConfigured(config:ProviderConfig)
}

class MemoryProviderConnectionRepository:ProviderConnectionRepository {
    private val proofs=mutableMapOf<String,ProviderVerificationProof>()
    private val modes=mutableMapOf<String,ProviderCredentialMode>()
    private val models=mutableMapOf<String,String>()
    override fun proof(id:String)=proofs[id]
    override fun save(proof:ProviderVerificationProof){proofs[proof.config.id]=proof}
    override fun remove(id:String){proofs.remove(id)}
    override fun mode(provider:String)=modes[provider] ?: if(provider=="local")ProviderCredentialMode.LOCAL else ProviderCredentialMode.API_KEY
    override fun setMode(provider:String,mode:ProviderCredentialMode){modes[provider]=mode}
    override fun configuredModel(id:String)=models[id]
    override fun saveConfigured(config:ProviderConfig){models[config.id]=config.model}
}
