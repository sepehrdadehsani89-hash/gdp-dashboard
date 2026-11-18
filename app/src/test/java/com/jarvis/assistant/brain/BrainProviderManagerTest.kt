package com.jarvis.assistant.brain

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantSettings
import com.jarvis.assistant.core.ChatMessage
import com.jarvis.assistant.core.ConnectionCheck
import com.jarvis.assistant.core.MessageRole
import com.jarvis.assistant.data.ProviderException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Policy and in-flight lifecycle tests using an injected adapter, never live account credentials. */
class BrainProviderManagerTest {
    @Test fun legacyGeminiModelsPrefixRemainsValid(){
        val config=ProviderConfig("gemini","models/gemini-2.5-flash")
        assertEquals(config,config.validated())
    }
    @Test fun legacyPrefixCannotBeUsedToInjectExtraGeminiPath(){
        assertTrue(runCatching{ProviderConfig("gemini","models/gemini-test/extra").validated()}.exceptionOrNull() is IllegalArgumentException)
    }
    private val api = ProviderConfig("openai", "gpt-test")
    private val plan = ProviderConfig("openai", "plan-test", ProviderCredentialMode.CHATGPT_PLAN)
    private val local = ProviderConfig("local", "qwen-test", ProviderCredentialMode.LOCAL)
    private val scopes = setOf("openid", "resource.invoke", ChatGptPlanAccess.INFERENCE_SCOPE)

    private data class Invocation(val config:ProviderConfig,val credential:String,val plan:Boolean)
    private class Fixture {
        val repository=MemoryProviderConnectionRepository()
        var settings=AssistantSettings(provider="openai",model="gpt-test")
        val keys=mutableMapOf("openai" to "private-api-key", "anthropic" to "private-claude-key", "gemini" to "private-google-key")
        val revisions=mutableMapOf("openai" to 1L)
        var access:ChatGptPlanAccess?=null
        var localRevision:String?="sha256-model-revision"
        var clock=1_000_000L
        var beforeChanges=0
        var disconnects=0
        var removalFailure:Exception?=null
        var disconnectFailure:Exception?=null
        var probeAction:suspend(ProviderConfig)->ConnectionCheck={ConnectionCheck(it.model,"READY",7)}
        var answerAction:suspend()->String={"Actual response"}
        val probes=mutableListOf<Invocation>()
        val answers=mutableListOf<Invocation>()
        var receivedText=""
        var receivedContext=""
        var receivedHistory:List<ChatMessage> = emptyList()
        var receivedScreenshot:ByteArray?=null
        var receivedIdentity=true
        var receivedConcise=true
        var receivedLanguage=AppLanguage.EN
        fun createManager()=BrainProviderManager(
            repository=repository,settings={settings},apiKey={keys[it].orEmpty()},apiRevision={revisions[it] ?: 0L},planAccess={access},
            probe={config,credential,isPlan->probes+=Invocation(config,credential,isPlan);probeAction(config)},
            answerRequest={config,credential,isPlan,text,language,context,history,onChunk,screenshot,identity,concise->
                answers+=Invocation(config,credential,isPlan);receivedText=text;receivedLanguage=language;receivedContext=context
                receivedHistory=history;receivedScreenshot=screenshot;receivedIdentity=identity;receivedConcise=concise
                answerAction().also{onChunk(it)}
            },
            saveSelection={settings=settings.copy(provider=it.provider,model=it.model)},
            removeKey={provider->removalFailure?.let{throw it};keys.remove(provider);revisions[provider]=(revisions[provider] ?: 0L)+1},
            disconnectPlan={disconnects++;disconnectFailure?.let{throw it};access=null},
            localRevision={localRevision},beforeChange={beforeChanges++},now={clock},
        )
        val manager=createManager()
        suspend fun answer()=manager.answer("Hello",AppLanguage.EN,"",emptyList(),{})
    }
    private fun Fixture.authorize(account:String="account-one",revision:Long=1L,bearer:String="private-plan-token",grants:Set<String> = scopes){
        access=ChatGptPlanAccess(bearer,account,grants,revision)
    }
    private suspend fun expect(kind:ProviderFailureKind,block:suspend()->Unit):ProviderException {
        try{block();fail("Expected $kind")}catch(e:ProviderException){assertEquals(kind,e.kind);return e}
        throw AssertionError("No failure")
    }
    private suspend fun rejectArgument(block:suspend()->Unit){
        try{block();fail("Expected invalid configuration rejection")}catch(_:IllegalArgumentException){ }
    }

    @Test fun identityWithoutInferenceScopeCannotReachAdapter()=runBlocking<Unit>{
        val f=Fixture();f.authorize(grants=setOf("openid","profile","resource.invoke"))
        expect(ProviderFailureKind.AUTHORIZATION){f.manager.verify(plan)}
        assertTrue(f.probes.isEmpty());assertNull(f.repository.proof(plan.id))
    }
    @Test fun inferenceScopeWithoutResourceInvokeCannotReachAdapter()=runBlocking<Unit>{
        val f=Fixture();f.authorize(grants=setOf("openid",ChatGptPlanAccess.INFERENCE_SCOPE))
        expect(ProviderFailureKind.AUTHORIZATION){f.manager.verify(plan)}
        assertTrue(f.probes.isEmpty())
    }
    @Test fun signedOutPlanNeverFallsBackToSavedApiKey()=runBlocking<Unit>{
        val f=Fixture();expect(ProviderFailureKind.AUTHENTICATION){f.manager.verify(plan)}
        assertEquals("private-api-key",f.keys["openai"]);assertTrue(f.probes.isEmpty())
    }
    @Test fun emptyBearerDoesNotAuthorizePlanInference()=runBlocking<Unit>{
        val f=Fixture();f.authorize(bearer="")
        expect(ProviderFailureKind.AUTHENTICATION){f.manager.verify(plan)};assertTrue(f.probes.isEmpty())
    }
    @Test fun emptyAccountDoesNotAuthorizePlanInference()=runBlocking<Unit>{
        val f=Fixture();f.authorize(account="")
        expect(ProviderFailureKind.AUTHENTICATION){f.manager.verify(plan)};assertTrue(f.probes.isEmpty())
    }
    @Test fun missingApiKeyCannotReachAdapter()=runBlocking<Unit>{
        val f=Fixture();f.keys.remove("openai")
        expect(ProviderFailureKind.AUTHENTICATION){f.manager.verify(api)};assertTrue(f.probes.isEmpty())
    }
    @Test fun configuredCredentialIsNotInferenceProof()=runBlocking<Unit>{
        val f=Fixture();assertEquals(ProviderConnectionStatus.CONFIGURED,f.manager.refresh(api).status)
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.select(api)}
        assertEquals(0,f.beforeChanges);assertNull(f.repository.proof(api.id))
    }
    @Test fun refreshDoesNotMakeNetworkProbe()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.refresh(plan)
        assertTrue(f.probes.isEmpty());assertTrue(f.answers.isEmpty());assertNull(f.repository.proof(plan.id))
    }
    @Test fun successfulApiProbePassesOnlyMatchingKeyAndRecordsProof()=runBlocking<Unit>{
        val f=Fixture();val check=f.manager.verify(api)
        assertEquals("READY",check.sample);assertEquals(listOf(Invocation(api,"private-api-key",false)),f.probes)
        assertEquals(ProviderConnectionStatus.VERIFIED,f.manager.connections.value[api.id]?.status)
        f.manager.ensureSelectable(api)
    }
    @Test fun successfulPlanProbePassesOAuthBearerAndPlanFlag()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.verify(plan)
        assertEquals(listOf(Invocation(plan,"private-plan-token",true)),f.probes);f.manager.ensureSelectable(plan)
    }
    @Test fun blankProbeAnswerCannotCreateProof()=runBlocking<Unit>{
        val f=Fixture();f.probeAction={ConnectionCheck(it.model,"  ",2)}
        expect(ProviderFailureKind.INCOMPLETE){f.manager.verify(api)}
        assertNull(f.repository.proof(api.id));assertEquals(ProviderConnectionStatus.FAILED,f.manager.connections.value[api.id]?.status)
    }
    @Test fun modelFailureCannotSelectOrReplaceWorkingPrimary()=runBlocking<Unit>{
        val f=Fixture();val before=f.settings;val claude=ProviderConfig("anthropic","claude-test")
        f.probeAction={throw ProviderException("Model unavailable",ProviderFailureKind.MODEL_UNAVAILABLE)}
        expect(ProviderFailureKind.MODEL_UNAVAILABLE){f.manager.verify(claude)}
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.select(claude)}
        assertEquals(before,f.settings);assertEquals(0,f.beforeChanges)
    }
    @Test fun proofForOneModelCannotSelectAnother()=runBlocking<Unit>{
        val f=Fixture();f.manager.verify(api)
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.select(api.copy(model="other-model"))}
        assertEquals("gpt-test",f.settings.model)
    }
    @Test fun apiProofCannotSelectPlanAccess()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.verify(api)
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.select(plan)}
        assertEquals(ProviderCredentialMode.API_KEY,f.manager.selectedMode("openai"))
    }
    @Test fun planProofCannotSelectApiAccess()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.verify(plan)
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.select(api)}
    }
    @Test fun primarySelectionUpdatesProviderModelAndModeTogetherAfterProof()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.verify(plan);f.manager.select(plan)
        assertEquals(plan,f.manager.currentConfig());assertEquals(1,f.beforeChanges)
        assertEquals(plan.model,f.repository.configuredModel(plan.id))
    }
    @Test fun changedApiKeyInvalidatesMatchingProof()=runBlocking<Unit>{
        val f=Fixture();f.manager.verify(api);f.keys["openai"]="replacement-key"
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.ensureSelectable(api)}
        assertEquals(ProviderConnectionStatus.EXPIRED,f.manager.refresh(api).status)
    }
    @Test fun sameApiKeyAfterRemovalAndReadditionRequiresFreshProbe()=runBlocking<Unit>{
        val f=Fixture();f.manager.verify(api);f.revisions["openai"]=2L
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.ensureSelectable(api)}
    }
    @Test fun switchedChatGptAccountCannotReuseOtherAccountProof()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.verify(plan);f.authorize(account="account-two")
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.ensureSelectable(plan)}
    }
    @Test fun changedGrantRevisionRequiresFreshProbe()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.verify(plan);f.authorize(revision=2L)
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.ensureSelectable(plan)}
    }
    @Test fun healthyTokenRotationWithStableGrantRetainsProof()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.verify(plan);f.authorize(bearer="rotated-token")
        f.manager.ensureSelectable(plan);assertEquals(ProviderConnectionStatus.VERIFIED,f.manager.refresh(plan).status)
    }
    @Test fun changedGrantedScopeSetInvalidatesProof()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.verify(plan);f.authorize(grants=scopes+"extra-grant")
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.ensureSelectable(plan)}
    }
    @Test fun proofExpiresAtFiniteBoundary()=runBlocking<Unit>{
        val f=Fixture();f.manager.verify(api);f.clock+=BrainProviderManager.PROOF_MAX_AGE_MS
        f.manager.ensureSelectable(api);f.clock++
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.ensureSelectable(api)}
    }
    @Test fun clockRollbackCannotMakeFutureProofValid()=runBlocking<Unit>{
        val f=Fixture();f.manager.verify(api);f.clock--
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.ensureSelectable(api)}
    }
    @Test fun persistedProofCannotContainCredentialMaterial()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.verify(api);f.manager.verify(plan)
        val metadata=f.repository.proof(api.id).toString()+f.repository.proof(plan.id).toString()
        assertFalse(metadata.contains("private-api-key"));assertFalse(metadata.contains("private-plan-token"))
        assertEquals(64,f.repository.proof(api.id)!!.credentialBinding.length)
        assertTrue(f.repository.proof(api.id)!!.nonce.length>=24)
    }
    @Test fun planAccessDebugStringIsRedacted() {
        assertEquals("ChatGptPlanAccess(redacted)",ChatGptPlanAccess("secret","sensitive-id",scopes,1).toString())
    }
    @Test fun shortenedNonceCannotForgeUsableProof()=runBlocking<Unit>{
        val f=Fixture();f.manager.verify(api);val proof=f.repository.proof(api.id)!!
        f.repository.save(proof.copy(nonce="short"))
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.ensureSelectable(api)}
    }
    @Test fun canceledProbeRemovesOldProofAndReportsCanceled()=runBlocking<Unit>{
        val f=Fixture();f.manager.verify(api);f.probeAction={throw CancellationException("user canceled")}
        try{f.manager.verify(api);fail("Expected cancellation")}catch(_:CancellationException){ }
        assertNull(f.repository.proof(api.id));assertEquals(ProviderFailureKind.CANCELLED,f.manager.connections.value[api.id]?.failure)
    }
    @Test fun disconnectDuringProbeCannotRestoreProof()=runBlocking<Unit>{
        val f=Fixture();val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        f.probeAction={entered.complete(Unit);release.await();ConnectionCheck(it.model,"READY",4)}
        val pending=async{runCatching{f.manager.verify(api)}};entered.await();f.manager.disconnect(api);release.complete(Unit)
        assertTrue(pending.await().isFailure);assertNull(f.repository.proof(api.id));assertFalse(f.keys.containsKey("openai"))
        assertEquals(ProviderConnectionStatus.NOT_CONFIGURED,f.manager.connections.value[api.id]?.status)
    }
    @Test fun configurationChangeDuringProbeCannotRestoreOldProof()=runBlocking<Unit>{
        val f=Fixture();val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        f.probeAction={entered.complete(Unit);release.await();ConnectionCheck(it.model,"READY",4)}
        val pending=async{runCatching{f.manager.verify(api)}};entered.await();f.manager.configurationChanged(api.copy(model="new-model"));release.complete(Unit)
        assertTrue(pending.await().isFailure);assertNull(f.repository.proof(api.id))
        assertEquals("new-model",f.manager.connections.value[api.id]?.config?.model)
    }
    @Test fun accountSwitchDuringProbeCannotCreateProofForPreviousAccount()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.probeAction={f.authorize(account="different-account");ConnectionCheck(it.model,"READY",1)}
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.verify(plan)};assertNull(f.repository.proof(plan.id))
    }
    @Test fun wipeDuringProbeCannotRepopulateVerificationState()=runBlocking<Unit>{
        val f=Fixture();val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        f.probeAction={entered.complete(Unit);release.await();ConnectionCheck(it.model,"READY",4)}
        val pending=async{runCatching{f.manager.verify(api)}};entered.await();f.repository.remove(api.id);f.manager.resetAfterWipe();release.complete(Unit)
        assertTrue(pending.await().isFailure);assertNull(f.repository.proof(api.id));assertTrue(f.manager.connections.value.isEmpty())
    }
    @Test fun apiDisconnectPreservesOtherProviderKeyAndPlanSession()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.verify(api);f.manager.disconnect(api)
        assertFalse(f.keys.containsKey("openai"));assertEquals("private-claude-key",f.keys["anthropic"])
        assertNotNull(f.access);assertEquals(0,f.disconnects);assertNull(f.repository.proof(api.id))
    }
    @Test fun planDisconnectPreservesExplicitApiCredential()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.verify(plan);f.manager.disconnect(plan)
        assertNull(f.access);assertEquals(1,f.disconnects);assertEquals("private-api-key",f.keys["openai"])
    }
    @Test fun failedCredentialRemovalCannotReportSuccessfulDisconnect()=runBlocking<Unit>{
        val f=Fixture();f.manager.verify(api);f.removalFailure=IllegalStateException("storage failed")
        try{f.manager.disconnect(api);fail("Expected storage failure")}catch(_:IllegalStateException){ }
        assertEquals("private-api-key",f.keys["openai"]);assertNull(f.repository.proof(api.id))
        assertEquals(ProviderConnectionStatus.FAILED,f.manager.connections.value[api.id]?.status)
    }
    @Test fun failedRemotePlanDisconnectNeverRestoresVerifiedProof()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.verify(plan);f.disconnectFailure=IllegalStateException("network failed")
        try{f.manager.disconnect(plan);fail("Expected disconnect failure")}catch(_:IllegalStateException){ }
        assertNull(f.repository.proof(plan.id));assertEquals(ProviderConnectionStatus.FAILED,f.manager.connections.value[plan.id]?.status)
    }
    @Test fun unverifiedSelectedPlanCannotRunOrdinaryAnswerOrFallback()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.repository.setMode("openai",ProviderCredentialMode.CHATGPT_PLAN);f.settings=f.settings.copy(model=plan.model)
        expect(ProviderFailureKind.NOT_VERIFIED){f.answer()};assertTrue(f.answers.isEmpty());assertTrue(f.probes.isEmpty())
    }
    @Test fun existingApiProviderStillAnswersAndRealAnswerCreatesProof()=runBlocking<Unit>{
        val f=Fixture();assertEquals("Actual response",f.answer())
        assertEquals(listOf(Invocation(api,"private-api-key",false)),f.answers);f.manager.ensureSelectable(api)
    }
    @Test fun verifiedPlanUsesPlanBearerForOrdinaryAnswer()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.verify(plan);f.manager.select(plan);assertEquals("Actual response",f.answer())
        assertEquals(listOf(Invocation(plan,"private-plan-token",true)),f.answers)
    }
    @Test fun usageLimitDoesNotTryApiFallbackOrPreserveGreenConnection()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.verify(plan);f.manager.select(plan)
        f.answerAction={throw ProviderException("Plan usage unavailable",ProviderFailureKind.USAGE_UNAVAILABLE)}
        expect(ProviderFailureKind.USAGE_UNAVAILABLE){f.answer()}
        assertEquals(1,f.answers.size);assertTrue(f.answers.single().plan);assertNull(f.repository.proof(plan.id))
        assertEquals(ProviderFailureKind.USAGE_UNAVAILABLE,f.manager.connections.value[plan.id]?.failure)
    }
    @Test fun emptyOrdinaryAnswerCannotCreateProof()=runBlocking<Unit>{
        val f=Fixture();f.answerAction={""};expect(ProviderFailureKind.INCOMPLETE){f.answer()}
        assertNull(f.repository.proof(api.id))
    }
    @Test fun changedApiKeyDuringAnswerRejectsCompletedResponse()=runBlocking<Unit>{
        val f=Fixture();f.answerAction={f.keys["openai"]="different-key";"Stale answer"}
        expect(ProviderFailureKind.NOT_VERIFIED){f.answer()}
        assertNull(f.repository.proof(api.id))
    }
    @Test fun changedPlanAccountDuringAnswerRejectsCompletedResponse()=runBlocking<Unit>{
        val f=Fixture();f.authorize();f.manager.verify(plan);f.manager.select(plan)
        f.answerAction={f.authorize(account="other-account");"Wrong-account answer"}
        expect(ProviderFailureKind.NOT_VERIFIED){f.answer()}
        assertNull(f.repository.proof(plan.id))
    }
    @Test fun configurationChangeDuringAnswerDropsLateChunksAndCompletion()=runBlocking<Unit>{
        val f=Fixture();val chunks=mutableListOf<String>()
        f.answerAction={f.manager.configurationChanged(api.copy(model="new-model"));"Old-model late text"}
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.answer("Hello",AppLanguage.EN,"",emptyList(),{chunks+=it})}
        assertTrue(chunks.isEmpty());assertNull(f.repository.proof(api.id))
        assertEquals("new-model",f.manager.connections.value[api.id]?.config?.model)
    }
    @Test fun disconnectDuringAnswerDropsLateChunksAndCompletion()=runBlocking<Unit>{
        val f=Fixture();val chunks=mutableListOf<String>()
        f.answerAction={f.manager.disconnect(api);"Disconnected late text"}
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.answer("Hello",AppLanguage.EN,"",emptyList(),{chunks+=it})}
        assertTrue(chunks.isEmpty());assertNull(f.repository.proof(api.id))
        assertEquals(ProviderConnectionStatus.NOT_CONFIGURED,f.manager.connections.value[api.id]?.status)
    }
    @Test fun wipeDuringAnswerDropsLateChunksWithoutRestoringConnectionState()=runBlocking<Unit>{
        val f=Fixture();val chunks=mutableListOf<String>()
        f.answerAction={f.repository.remove(api.id);f.manager.resetAfterWipe();"Wiped late text"}
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.answer("Hello",AppLanguage.EN,"",emptyList(),{chunks+=it})}
        assertTrue(chunks.isEmpty());assertNull(f.repository.proof(api.id));assertTrue(f.manager.connections.value.isEmpty())
    }
    @Test fun probeResultForDifferentModelCannotAuthorizeSelectedModel()=runBlocking<Unit>{
        val f=Fixture();f.probeAction={ConnectionCheck("unexpected-model","READY",5)}
        expect(ProviderFailureKind.MODEL_UNAVAILABLE){f.manager.verify(api)}
        assertNull(f.repository.proof(api.id))
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.select(api)}
    }
    @Test fun voiceRequestRetainsLanguageContextHistoryImageAndPrivacyFlags()=runBlocking<Unit>{
        val f=Fixture();val history=listOf(ChatMessage(role=MessageRole.USER,text="Earlier question"));val image=byteArrayOf(1,2,3);val chunks=mutableListOf<String>()
        f.manager.answer("Current question",AppLanguage.ES,"Consented context",history,{chunks+=it},image,includeIdentity=false,concise=false)
        assertEquals("Current question",f.receivedText);assertEquals(AppLanguage.ES,f.receivedLanguage)
        assertEquals("Consented context",f.receivedContext);assertEquals(history,f.receivedHistory);assertArrayEquals(image,f.receivedScreenshot)
        assertFalse(f.receivedIdentity);assertFalse(f.receivedConcise);assertEquals(listOf("Actual response"),chunks)
    }
    @Test fun localSelectionRequiresActualInferenceProofNotInstalledRevisionAlone()=runBlocking<Unit>{
        val f=Fixture();expect(ProviderFailureKind.NOT_VERIFIED){f.manager.select(local)}
        f.manager.recordLocalInferenceSuccess(local,"sha256-model-revision");f.manager.select(local)
        assertEquals(local,f.manager.currentConfig())
    }
    @Test fun localModelReplacementInvalidatesPriorNativeInferenceProof()=runBlocking<Unit>{
        val f=Fixture();f.manager.recordLocalInferenceSuccess(local,"sha256-model-revision");f.localRevision="replacement-model"
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.ensureSelectable(local)}
    }
    @Test fun removedLocalModelCannotBeSelected()=runBlocking<Unit>{
        val f=Fixture();f.manager.recordLocalInferenceSuccess(local,"sha256-model-revision");f.localRevision=null
        expect(ProviderFailureKind.NOT_VERIFIED){f.manager.select(local)}
    }
    @Test fun localProbeCannotRecordMismatchedModelRevision()=runBlocking<Unit>{
        val f=Fixture();rejectArgument{f.manager.recordLocalInferenceSuccess(local,"wrong-hash")};assertNull(f.repository.proof(local.id))
    }
    @Test fun localModelNeverUsesCloudConnectionProbe()=runBlocking<Unit>{
        val f=Fixture();rejectArgument{f.manager.verify(local)};assertTrue(f.probes.isEmpty())
    }
    @Test fun modelPreferencesRemainSeparateForPlanAndApiModes()=runBlocking<Unit>{
        val f=Fixture();f.manager.saveConfiguredModel(api);f.manager.saveConfiguredModel(plan)
        assertEquals(api.model,f.manager.configuredModel("openai",ProviderCredentialMode.API_KEY))
        assertEquals(plan.model,f.manager.configuredModel("openai",ProviderCredentialMode.CHATGPT_PLAN))
        assertEquals(api,f.manager.currentConfig())
    }
    @Test fun unsupportedProviderOrCredentialPairCannotReachAdapter()=runBlocking<Unit>{
        val f=Fixture();rejectArgument{f.manager.verify(ProviderConfig("unknown","model"))}
        rejectArgument{f.manager.verify(ProviderConfig("anthropic","model",ProviderCredentialMode.CHATGPT_PLAN))}
        rejectArgument{f.manager.verify(ProviderConfig("local","model",ProviderCredentialMode.API_KEY))}
        assertTrue(f.probes.isEmpty())
    }
    @Test fun invalidModelCannotInjectUrlPathOrQuery()=runBlocking<Unit>{
        val f=Fixture();listOf("../secret","model?api_key=secret","model/extra","","a".repeat(101),"model\nAuthorization").forEach{model->
            rejectArgument{f.manager.verify(api.copy(model=model))}
        };assertTrue(f.probes.isEmpty())
    }
    @Test fun actualFailureSurvivesUiRefresh()=runBlocking<Unit>{val f=Fixture();f.probeAction={throw ProviderException("Rejected",ProviderFailureKind.AUTHENTICATION,401)};expect(ProviderFailureKind.AUTHENTICATION){f.manager.verify(api)};val row=f.manager.refresh(api);assertEquals(ProviderConnectionStatus.FAILED,row.status);assertEquals(401,row.httpStatus)}
    @Test fun creditsFailureSurvivesUiRefresh()=runBlocking<Unit>{val f=Fixture();f.probeAction={throw ProviderException("Credits",ProviderFailureKind.CREDITS,429)};expect(ProviderFailureKind.CREDITS){f.manager.verify(api)};assertEquals(ProviderFailureKind.CREDITS,f.manager.refresh(api).failure)}
    @Test fun newAppSessionDoesNotRestoreGreenFromCachedProof()=runBlocking<Unit>{val f=Fixture();f.manager.verify(api);assertEquals(ProviderConnectionStatus.PREVIOUSLY_VERIFIED,f.createManager().refresh(api).status)}
    @Test fun sameSessionRetainsActualResponseEvidence()=runBlocking<Unit>{val f=Fixture();f.manager.verify(api);assertEquals(ProviderConnectionStatus.VERIFIED,f.manager.refresh(api).status)}
    @Test fun restartRestorationDoesNotSpendApiCredits()=runBlocking<Unit>{val f=Fixture();f.manager.verify(api);val count=f.probes.size;f.createManager().refresh(api);assertEquals(count,f.probes.size)}
    @Test fun historicalConnectionNeedsFreshResponseForGreen()=runBlocking<Unit>{val f=Fixture();f.manager.verify(api);val next=f.createManager();next.refresh(api);next.verify(api);assertEquals(ProviderConnectionStatus.VERIFIED,next.connections.value[api.id]?.status)}
    @Test fun historicalConnectionActualQuestionCanRevalidate()=runBlocking<Unit>{val f=Fixture();f.manager.verify(api);val next=f.createManager();next.refresh(api);next.answer("Hi",AppLanguage.EN,"",emptyList(),{});assertEquals(ProviderConnectionStatus.VERIFIED,next.connections.value[api.id]?.status)}
    @Test fun probeDurationAndResolvedModelAreRetained()=runBlocking<Unit>{val f=Fixture();f.probeAction={ConnectionCheck(it.model,"READY",251,"gpt-test-2025-01-01")};f.manager.verify(api);val row=f.manager.refresh(api);assertEquals(251L,row.elapsedMs);assertEquals("gpt-test-2025-01-01",row.resolvedModel)}
    @Test fun failureRecordsHttpStatusAndElapsedTime()=runBlocking<Unit>{val f=Fixture();f.probeAction={f.clock+=400;throw ProviderException("limit",ProviderFailureKind.RATE_LIMIT,429,2000)};expect(ProviderFailureKind.RATE_LIMIT){f.manager.verify(api)};val row=f.manager.refresh(api);assertEquals(400L,row.elapsedMs);assertEquals(429,row.httpStatus);assertEquals(f.clock+2000,row.retryAt)}
    @Test fun earlyExplicitRetryCannotHammerProvider()=runBlocking<Unit>{val f=Fixture();f.probeAction={throw ProviderException("limit",ProviderFailureKind.RATE_LIMIT,429,2000)};expect(ProviderFailureKind.RATE_LIMIT){f.manager.verify(api)};expect(ProviderFailureKind.RATE_LIMIT){f.manager.verify(api)};assertEquals(1,f.probes.size)}
    @Test fun retryAllowedAfterProviderDelay()=runBlocking<Unit>{val f=Fixture();f.probeAction={throw ProviderException("limit",ProviderFailureKind.RATE_LIMIT,429,2000)};expect(ProviderFailureKind.RATE_LIMIT){f.manager.verify(api)};f.clock+=2000;f.probeAction={ConnectionCheck(it.model,"READY",2)};f.manager.verify(api);assertEquals(2,f.probes.size)}
    @Test fun replacingCredentialClearsStaleFailure()=runBlocking<Unit>{val f=Fixture();f.probeAction={throw ProviderException("bad key",ProviderFailureKind.AUTHENTICATION)};expect(ProviderFailureKind.AUTHENTICATION){f.manager.verify(api)};f.keys["openai"]="new-key";assertEquals(ProviderConnectionStatus.CONFIGURED,f.manager.refresh(api).status)}
    @Test fun draftModelRefreshDoesNotDeleteWorkingProof()=runBlocking<Unit>{val f=Fixture();f.manager.verify(api);f.manager.refresh(api.copy(model="gpt-draft"));f.manager.ensureSelectable(api);assertNotNull(f.repository.proof(api.id))}
    @Test fun metadataRefreshDoesNotEraseInFlightValidation()=runBlocking<Unit>{val f=Fixture();val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>();f.probeAction={entered.complete(Unit);release.await();ConnectionCheck(it.model,"READY",1)};val job=async{f.manager.verify(api)};entered.await();assertEquals(ProviderConnectionStatus.VERIFYING,f.manager.refresh(api).status);release.complete(Unit);job.await()}
    @Test fun expiredProofCannotRemainGreen()=runBlocking<Unit>{val f=Fixture();f.manager.verify(api);f.clock+=BrainProviderManager.PROOF_MAX_AGE_MS+1;assertEquals(ProviderConnectionStatus.EXPIRED,f.manager.refresh(api).status)}
    @Test fun expiredHistoricalProofCannotRestoreGreen()=runBlocking<Unit>{val f=Fixture();f.manager.verify(api);f.clock+=BrainProviderManager.PROOF_MAX_AGE_MS+1;assertEquals(ProviderConnectionStatus.EXPIRED,f.createManager().refresh(api).status)}
    @Test fun failingQuestionRevokesOldProofAndRetainsFailure()=runBlocking<Unit>{val f=Fixture();f.manager.verify(api);f.answerAction={throw ProviderException("network",ProviderFailureKind.NETWORK)};expect(ProviderFailureKind.NETWORK){f.answer()};assertNull(f.repository.proof(api.id));assertEquals(ProviderFailureKind.NETWORK,f.manager.refresh(api).failure)}
    @Test fun cancelledTestCannotRestoreOldGreenOnRefresh()=runBlocking<Unit>{val f=Fixture();f.manager.verify(api);f.probeAction={throw CancellationException("cancel")};try{f.manager.verify(api)}catch(_:CancellationException){};assertNotEquals(ProviderConnectionStatus.VERIFIED,f.manager.refresh(api).status)}
    @Test fun successfulRetryClearsActualFailure()=runBlocking<Unit>{val f=Fixture();f.probeAction={throw ProviderException("network",ProviderFailureKind.NETWORK)};expect(ProviderFailureKind.NETWORK){f.manager.verify(api)};f.probeAction={ConnectionCheck(it.model,"READY",5)};f.manager.verify(api);assertNull(f.manager.refresh(api).failure)}
    @Test fun failedConnectionNeverChangesUserSelection()=runBlocking<Unit>{val f=Fixture();val before=f.settings;f.probeAction={throw ProviderException("bad",ProviderFailureKind.BAD_REQUEST,400)};expect(ProviderFailureKind.BAD_REQUEST){f.manager.verify(api)};assertEquals(before,f.settings);assertEquals(0,f.beforeChanges)}

    @Test fun closedSettingsCannotKeepExpiredCoreBadgeGreen()=runBlocking<Unit>{val f=Fixture();f.manager.verify(api);f.clock+=BrainProviderManager.PROOF_MAX_AGE_MS+1;f.manager.expireStaleVerifications();assertEquals(ProviderConnectionStatus.EXPIRED,f.manager.connections.value[api.id]?.status);assertNull(f.repository.proof(api.id))}
    @Test fun expiryMaintenanceDoesNotMakeNetworkProbe()=runBlocking<Unit>{val f=Fixture();f.manager.verify(api);val count=f.probes.size;f.clock+=BrainProviderManager.PROOF_MAX_AGE_MS+1;f.manager.expireStaleVerifications();assertEquals(count,f.probes.size);assertTrue(f.answers.isEmpty())}
    @Test fun expiryMaintenancePreservesRecentVerifiedResponse()=runBlocking<Unit>{val f=Fixture();f.manager.verify(api);f.clock+=100;f.manager.expireStaleVerifications();assertEquals(ProviderConnectionStatus.VERIFIED,f.manager.connections.value[api.id]?.status)}
    @Test fun expiryMaintenancePreservesFailureEvidence()=runBlocking<Unit>{val f=Fixture();f.probeAction={throw ProviderException("bad",ProviderFailureKind.CREDITS,429)};expect(ProviderFailureKind.CREDITS){f.manager.verify(api)};f.clock+=BrainProviderManager.PROOF_MAX_AGE_MS+1;f.manager.expireStaleVerifications();assertEquals(ProviderFailureKind.CREDITS,f.manager.connections.value[api.id]?.failure)}
    @Test fun clockRollbackExpiresCurrentGreenBadge()=runBlocking<Unit>{val f=Fixture();f.manager.verify(api);f.clock--;f.manager.expireStaleVerifications();assertEquals(ProviderConnectionStatus.EXPIRED,f.manager.connections.value[api.id]?.status)}
    @Test fun expiryMaintenancePreservesPreviousVerificationTime()=runBlocking<Unit>{val f=Fixture();f.manager.verify(api);val time=f.clock;f.clock+=BrainProviderManager.PROOF_MAX_AGE_MS+1;f.manager.expireStaleVerifications();assertEquals(time,f.manager.connections.value[api.id]?.verifiedAt)}

}
