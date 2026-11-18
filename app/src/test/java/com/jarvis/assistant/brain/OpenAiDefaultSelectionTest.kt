package com.jarvis.assistant.brain

import com.jarvis.assistant.core.ConnectionCheck
import com.jarvis.assistant.data.ProviderException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Simulated orchestration; listing, configuration and real-inference proof remain separate steps. */
class OpenAiDefaultSelectionTest {
    private class Flow {
        var ids=listOf("gpt-4.1-mini")
        val events=mutableListOf<String>()
        var failureAt:String?=null
        var error:Exception=ProviderException("failure",ProviderFailureKind.AUTHENTICATION)
        var sample="READY"
        var returned:String?=null
        var primary="previous-working-brain"
        var selected:ProviderConfig?=null
        suspend fun step(name:String){events+=name;if(failureAt==name)throw error}
        suspend fun connect()=OpenAiDefaultSelection.connect(
            catalog={step("catalog");ids},configure={step("configure")},
            verify={step("verify");ConnectionCheck(returned ?: it.model,sample,7)},
            select={step("select");selected=it;primary=it.model})
    }
    private suspend fun fails(f:Flow){assertTrue(runCatching{f.connect()}.isFailure);assertEquals("previous-working-brain",f.primary);assertNull(f.selected)}
    @Test fun defaultIsSelectedOnlyAfterCatalogConfigureAndInference()=runBlocking<Unit>{val f=Flow();f.connect();assertEquals(listOf("catalog","configure","verify","select"),f.events)}
    @Test fun defaultRetainsExactApiCredentialMode()=runBlocking<Unit>{val f=Flow();f.connect();assertEquals(ProviderCredentialMode.API_KEY,f.selected?.mode)}
    @Test fun defaultNeverUsesPlanCredentialMode()=runBlocking<Unit>{val f=Flow();f.connect();assertEquals("openai",f.selected?.provider)}
    @Test fun listedPreferredModelIsUsed()=runBlocking<Unit>{val f=Flow();f.ids=listOf("gpt-4o-mini","gpt-4.1-mini");f.connect();assertEquals("gpt-4.1-mini",f.primary)}
    @Test fun absentPreferredModelIsNeverInvented()=runBlocking<Unit>{val f=Flow();f.ids=listOf("o3-mini");f.connect();assertEquals("o3-mini",f.primary)}
    @Test fun returnedConnectionEvidenceIsPreserved()=runBlocking<Unit>{val result=Flow().connect();assertEquals("READY",result.second.sample);assertEquals(7L,result.second.elapsedMs)}
    @Test fun emptyCatalogStopsBeforeKeyConfiguration()=runBlocking<Unit>{val f=Flow();f.ids=emptyList();fails(f);assertEquals(listOf("catalog"),f.events)}
    @Test fun audioOnlyCatalogCannotSelectChatDefault()=runBlocking<Unit>{val f=Flow();f.ids=listOf("gpt-realtime","gpt-4o-mini-tts");fails(f)}
    @Test fun imageOnlyCatalogCannotSelectChatDefault()=runBlocking<Unit>{val f=Flow();f.ids=listOf("gpt-image-1");fails(f)}
    @Test fun malformedModelCannotBecomeDefault()=runBlocking<Unit>{val f=Flow();f.ids=listOf("gpt-../../secret");fails(f)}
    @Test fun catalogFailurePreservesActiveBrain()=runBlocking<Unit>{val f=Flow();f.failureAt="catalog";fails(f);assertFalse(f.events.contains("configure"))}
    @Test fun keyStorageFailureStopsBeforeInference()=runBlocking<Unit>{val f=Flow();f.failureAt="configure";fails(f);assertFalse(f.events.contains("verify"))}
    @Test fun authenticationFailurePreservesActiveBrain()=runBlocking<Unit>{val f=Flow();f.failureAt="verify";fails(f)}
    @Test fun insufficientCreditsNeverTryAnotherModel()=runBlocking<Unit>{val f=Flow();f.ids=listOf("gpt-4.1-mini","gpt-4o-mini");f.failureAt="verify";f.error=ProviderException("credits",ProviderFailureKind.CREDITS);fails(f);assertEquals(1,f.events.count{it=="verify"})}
    @Test fun modelUnavailableNeverTryAnotherModel()=runBlocking<Unit>{val f=Flow();f.failureAt="verify";f.error=ProviderException("model",ProviderFailureKind.MODEL_UNAVAILABLE);fails(f)}
    @Test fun timeoutDoesNotChangePrimary()=runBlocking<Unit>{val f=Flow();f.failureAt="verify";f.error=ProviderException("timeout",ProviderFailureKind.TIMEOUT);fails(f)}
    @Test fun emptyInferenceCannotSelectDefault()=runBlocking<Unit>{val f=Flow();f.sample="  ";fails(f)}
    @Test fun wrongModelEvidenceCannotSelectDefault()=runBlocking<Unit>{val f=Flow();f.returned="gpt-other";fails(f)}
    @Test fun cancellationCannotProceedToSelection()=runBlocking<Unit>{val f=Flow();f.failureAt="verify";f.error=CancellationException("canceled");fails(f);assertFalse(f.events.contains("select"))}
    @Test fun selectionRefusalIsSurfacedWithoutRetry()=runBlocking<Unit>{val f=Flow();f.failureAt="select";fails(f);assertEquals(1,f.events.count{it=="select"})}
}
