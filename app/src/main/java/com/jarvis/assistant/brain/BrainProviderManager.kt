package com.jarvis.assistant.brain

import com.jarvis.assistant.core.*
import com.jarvis.assistant.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.MessageDigest
import java.util.UUID

/** Shares the existing network adapter and runtime pipeline. Identity alone never issues a proof. */
class BrainProviderManager internal constructor(
    private val repository:ProviderConnectionRepository,
    private val settings:()->AssistantSettings,
    private val apiKey:(String)->String,
    private val apiRevision:(String)->Long,
    private val planAccess:suspend()->ChatGptPlanAccess?,
    private val probe:suspend(ProviderConfig,String,Boolean)->ConnectionCheck,
    private val answerRequest:suspend(ProviderConfig,String,Boolean,String,AppLanguage,String,List<ChatMessage>,(String)->Unit,ByteArray?,Boolean,Boolean)->String,
    private val saveSelection:(ProviderConfig)->Unit,
    private val removeKey:(String)->Unit,
    private val disconnectPlan:suspend()->Unit,
    private val localRevision:(String)->String?,
    private val beforeChange:()->Unit,
    private val now:()->Long=System::currentTimeMillis,
) {
    constructor(store:JarvisStore,client:LlmClient,planAccess:suspend()->ChatGptPlanAccess?={null},disconnectPlan:suspend()->Unit={},localRevision:(String)->String?={null},beforeChange:()->Unit={}) : this(
        ProviderConnectionStore(store),{store.settings.value},store::getApiKey,store::getApiKeyRevision,planAccess,
        {config,credential,plan->client.testConnection(store.settings.value.copy(provider=config.provider,model=config.model),if(plan)"" else credential,if(plan)credential else null,explicitApi=!plan)},
        {config,credential,plan,text,language,context,history,onChunk,screenshot,identity,concise->client.answer(text,language,context,history,onChunk,screenshot,identity,concise,
            store.settings.value.copy(provider=config.provider,model=config.model),if(plan)credential else null,if(plan)null else credential)},
        {config->store.updateSettings(store.settings.value.copy(provider=config.provider,model=config.model))},{provider->store.setApiKey(provider,"")},disconnectPlan,localRevision,beforeChange)

    private val mutable=MutableStateFlow<Map<String,ProviderConnection>>(emptyMap())
    val connections=mutable.asStateFlow()
    private val epochs=mutableMapOf<String,Long>()
    private val sessionProofs=java.util.concurrent.ConcurrentHashMap<String,String>()
    private val failedBindings=java.util.concurrent.ConcurrentHashMap<String,String>()
    @Synchronized private fun epoch(id:String)=epochs.getOrPut(id){0L}
    @Synchronized private fun invalidate(config:ProviderConfig){epochs[config.id]=epoch(config.id)+1;repository.remove(config.id);failedBindings.remove(config.id);sessionProofs.remove(config.id)}
    private fun publish(config:ProviderConfig,status:ProviderConnectionStatus,message:String="",failure:ProviderFailureKind?=null,at:Long?=null,elapsed:Long?=null,httpStatus:Int?=null,retryAt:Long?=null,resolvedModel:String?=null){mutable.update{it+(config.id to ProviderConnection(config,status,at,message,failure,elapsed,httpStatus,retryAt,resolvedModel))}}
    fun currentConfig():ProviderConfig {val s=settings();return ProviderConfig(s.provider,s.model,repository.mode(s.provider))}
    fun selectedMode(provider:String)=repository.mode(provider)
    private class Credential(val secret:String,val binding:String){override fun toString()="ProviderCredential(redacted)"}
    fun configuredModel(provider:String,mode:ProviderCredentialMode):String?=repository.configuredModel("$provider:${mode.name}")
        ?: repository.proof("$provider:${mode.name}")?.config?.model
        ?: settings().takeIf{it.provider==provider && repository.mode(provider)==mode}?.model
    fun saveConfiguredModel(config:ProviderConfig){config.validated();if(configuredModel(config.provider,config.mode)!=config.model)configurationChanged(config);repository.saveConfigured(config)}
    private suspend fun credential(config:ProviderConfig):Credential {
        config.validated()
        return when(config.mode){
            ProviderCredentialMode.API_KEY->{val key=apiKey(config.provider);if(key.isBlank())throw ProviderException("Configure this provider's API access first.",ProviderFailureKind.AUTHENTICATION)
                Credential(key,hash("${config.id}:${apiRevision(config.provider)}:$key"))}
            ProviderCredentialMode.CHATGPT_PLAN->{val plan=planAccess() ?: throw ProviderException("Sign in with ChatGPT and authorize AI access first.",ProviderFailureKind.AUTHENTICATION)
                if(!plan.scopes.containsAll(setOf(ChatGptPlanAccess.INFERENCE_SCOPE,"resource.invoke")))throw ProviderException("ChatGPT identity is signed in; AI access is not authorized.",ProviderFailureKind.AUTHORIZATION)
                if(plan.bearer.isBlank() || plan.accountId.isBlank())throw ProviderException("ChatGPT session is unavailable. Sign in again.",ProviderFailureKind.AUTHENTICATION)
                Credential(plan.bearer,hash("${config.id}:${plan.accountId}:${plan.revision}:${plan.scopes.sorted().joinToString(" ")}"))}
            ProviderCredentialMode.LOCAL->{val revision=localRevision(config.model) ?: throw ProviderException("The selected Local Brain is not installed and verified.",ProviderFailureKind.NOT_VERIFIED);Credential("",hash("${config.id}:$revision"))}
        }
    }
    private fun matches(proof:ProviderVerificationProof?,config:ProviderConfig,binding:String)=proof!=null && proof.config==config && proof.credentialBinding==binding && proof.nonce.length>=24 && now()-proof.verifiedAt in 0..PROOF_MAX_AGE_MS
    @Synchronized private fun record(config:ProviderConfig,binding:String,expectedEpoch:Long=epoch(config.id),check:ConnectionCheck?=null):ProviderVerificationProof {
        if(epoch(config.id)!=expectedEpoch)throw ProviderException("Connection changed during this request. Test again.",ProviderFailureKind.NOT_VERIFIED)
        val proof=ProviderVerificationProof(config,binding,now(),UUID.randomUUID().toString());repository.save(proof);sessionProofs[config.id]=proof.nonce;failedBindings.remove(config.id)
        publish(config,ProviderConnectionStatus.VERIFIED,"Actual model response verified in this app session.",at=proof.verifiedAt,elapsed=check?.elapsedMs,resolvedModel=check?.resolvedModel);return proof
    }
    suspend fun refresh(config:ProviderConfig):ProviderConnection {
        try{
            val c=credential(config);val proof=repository.proof(config.id)
            val previous=connections.value[config.id]?.takeIf{it.config==config}
            if(previous?.status==ProviderConnectionStatus.VERIFYING)return previous
            if(matches(proof,config,c.binding)){
                if(proof!!.nonce==sessionProofs[config.id]){
                    if(previous?.status!=ProviderConnectionStatus.VERIFIED)publish(config,ProviderConnectionStatus.VERIFIED,"Actual response verified in this app session.",at=proof.verifiedAt)
                }else publish(config,ProviderConnectionStatus.PREVIOUSLY_VERIFIED,"Previously verified. Run Test Connection or a real question to check current access.",at=proof.verifiedAt)
            }else if(previous?.status==ProviderConnectionStatus.FAILED && failedBindings[config.id]==c.binding){
                return previous // A UI refresh must not erase the actual provider failure.
            }else if(proof!=null && proof.config!=config){
                publish(config,ProviderConnectionStatus.CONFIGURED,"This draft model is not verified. The saved active-model proof was preserved.")
            }else{
                if(proof!=null)repository.remove(config.id)
                publish(config,if(proof!=null)ProviderConnectionStatus.EXPIRED else ProviderConnectionStatus.CONFIGURED,if(proof!=null)"The previous verification expired. Test again." else "Test this connection before selecting it.",at=proof?.verifiedAt)
            }
        }catch(e:CancellationException){throw e}catch(e:Exception){repository.remove(config.id);publish(config,ProviderConnectionStatus.NOT_CONFIGURED,safeMessage(e),kind(e))}
        return connections.value.getValue(config.id)
    }
    /** Pure metadata maintenance, including while settings are closed. Never touches the network. */
    @Synchronized fun expireStaleVerifications(){
        val timestamp=now()
        connections.value.values.forEach{row->
            val at=row.verifiedAt
            if(row.status==ProviderConnectionStatus.VERIFIED && at!=null && timestamp-at !in 0..PROOF_MAX_AGE_MS){
                repository.remove(row.config.id);sessionProofs.remove(row.config.id)
                publish(row.config,ProviderConnectionStatus.EXPIRED,"The previous verification expired. Test again.",at=at,elapsed=row.elapsedMs,resolvedModel=row.resolvedModel)
            }
        }
    }
    suspend fun verify(config:ProviderConfig):ConnectionCheck {
        config.validated();require(config.mode!=ProviderCredentialMode.LOCAL){"Local inference must be tested on the local model."}
        val previous=connections.value[config.id]?.takeIf{it.config==config}
        if(previous!=null && !previous.retryAllowed(now()))throw ProviderException("The provider requested a retry delay. Wait before testing again.",ProviderFailureKind.RATE_LIMIT,previous.httpStatus,(previous.retryAt!!-now()).coerceAtLeast(0))
        val started=now();val requestEpoch=epoch(config.id);var binding:String?=null
        publish(config,ProviderConnectionStatus.VERIFYING,"Waiting for an actual model response.")
        try{
            val c=credential(config);binding=c.binding;val result=probe(config,c.secret,config.mode==ProviderCredentialMode.CHATGPT_PLAN)
            if(result.sample.isBlank())throw ProviderException("The provider returned no answer.",ProviderFailureKind.INCOMPLETE)
            if(result.model!=config.model.removePrefix(if(config.provider=="gemini")"models/" else ""))throw ProviderException("The provider verified a different model. Test the exact selected model.",ProviderFailureKind.MODEL_UNAVAILABLE)
            val after=credential(config)
            if(epoch(config.id)!=requestEpoch || c.binding!=after.binding)throw ProviderException("Connection changed while verification was running. Test again.",ProviderFailureKind.NOT_VERIFIED)
            record(config,c.binding,requestEpoch,result);return result
        }catch(e:CancellationException){if(epoch(config.id)==requestEpoch){repository.remove(config.id);publish(config,ProviderConnectionStatus.CONFIGURED,"Connection test canceled.",ProviderFailureKind.CANCELLED)};throw e}
        catch(e:Exception){if(epoch(config.id)==requestEpoch){repository.remove(config.id);binding?.let{failedBindings[config.id]=it};val failure=e as? ProviderException;publish(config,ProviderConnectionStatus.FAILED,safeMessage(e),kind(e),at=previous?.verifiedAt,elapsed=(now()-started).coerceAtLeast(0),httpStatus=failure?.httpStatus,retryAt=failure?.retryAfterMs?.let{now()+it.coerceIn(0,86400000L)})};throw e}
    }
    /** Called only after the runtime has completed a real local inference probe. */
    suspend fun recordLocalInferenceSuccess(config:ProviderConfig,revision:String){
        require(config.mode==ProviderCredentialMode.LOCAL);val c=credential(config)
        require(c.binding==hash("${config.id}:$revision")){"Local model changed during verification"};record(config,c.binding)
    }
    suspend fun ensureSelectable(config:ProviderConfig){
        val c=credential(config)
        if(!matches(repository.proof(config.id),config,c.binding))throw ProviderException("Test this exact provider, model and account before selecting it.",ProviderFailureKind.NOT_VERIFIED)
    }
    suspend fun select(config:ProviderConfig){
        ensureSelectable(config)
        beforeChange();repository.saveConfigured(config);repository.setMode(config.provider,config.mode);saveSelection(config)
    }
    fun configurationChanged(config:ProviderConfig){invalidate(config);publish(config,ProviderConnectionStatus.CONFIGURED,"Configuration changed. Test the connection again.")}
    suspend fun disconnect(config:ProviderConfig){
        beforeChange();invalidate(config)
        try{
            when(config.mode){ProviderCredentialMode.API_KEY->removeKey(config.provider);ProviderCredentialMode.CHATGPT_PLAN->disconnectPlan();ProviderCredentialMode.LOCAL->Unit}
            publish(config,ProviderConnectionStatus.NOT_CONFIGURED,"Disconnected on this device.")
        }catch(e:CancellationException){publish(config,ProviderConnectionStatus.CONFIGURED,"Disconnect interrupted; review credential status.");throw e}
        catch(e:Exception){publish(config,ProviderConnectionStatus.FAILED,"Credential removal could not be confirmed. Retry disconnect.");throw e}
    }
    @Synchronized fun resetAfterWipe(){(epochs.keys+mutable.value.keys).toSet().forEach{epochs[it]=epoch(it)+1};mutable.value=emptyMap();sessionProofs.clear();failedBindings.clear()}
    /** Existing configured providers may answer; only the resulting real response can create proof. */
    suspend fun answer(userText:String,language:AppLanguage,context:String,history:List<ChatMessage>,onChunk:(String)->Unit,screenshot:ByteArray?=null,includeIdentity:Boolean=true,concise:Boolean=true):String {
        val config=currentConfig().validated();require(config.mode!=ProviderCredentialMode.LOCAL){"Local inference uses the existing LocalBrain."}
        val requestEpoch=epoch(config.id);val c=credential(config)
        if(config.mode==ProviderCredentialMode.CHATGPT_PLAN && !matches(repository.proof(config.id),config,c.binding))throw ProviderException("Test this ChatGPT account and model before using it as the AI Brain.",ProviderFailureKind.NOT_VERIFIED)
        try{val result=answerRequest(config,c.secret,config.mode==ProviderCredentialMode.CHATGPT_PLAN,userText,language,context,history,{chunk->if(requestEpoch==epoch(config.id))onChunk(chunk)},screenshot,includeIdentity,concise)
            if(result.isBlank())throw ProviderException("The provider returned no answer.",ProviderFailureKind.INCOMPLETE)
            if(requestEpoch!=epoch(config.id) || c.binding!=credential(config).binding)throw ProviderException("The connection changed before this answer completed. No completed response was accepted.",ProviderFailureKind.NOT_VERIFIED)
            record(config,c.binding,requestEpoch)
            return result
        }catch(e:CancellationException){throw e}catch(e:Exception){if(requestEpoch==epoch(config.id)){repository.remove(config.id);failedBindings[config.id]=c.binding;val failure=e as? ProviderException;publish(config,ProviderConnectionStatus.FAILED,safeMessage(e),kind(e),httpStatus=failure?.httpStatus,retryAt=failure?.retryAfterMs?.let{now()+it.coerceIn(0,86400000L)})};throw e}
    }
    private fun kind(error:Exception)=(error as? ProviderException)?.kind ?: ProviderFailureKind.UNKNOWN
    private fun safeMessage(error:Exception)=(error as? ProviderException)?.message ?: "The provider operation failed. Review connection settings."
    companion object {const val PROOF_MAX_AGE_MS=3_600_000L;private fun hash(value:String)=MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString(""){"%02x".format(it)}}
}
