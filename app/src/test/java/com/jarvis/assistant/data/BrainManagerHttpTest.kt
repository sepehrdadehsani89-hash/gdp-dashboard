package com.jarvis.assistant.data

import com.jarvis.assistant.brain.ProviderCredentialMode
import com.jarvis.assistant.brain.ProviderFailureKind
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantSettings
import com.jarvis.assistant.core.ChatMessage
import com.jarvis.assistant.core.MessageRole
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Mock HTTP contracts, not live account authorization or billed provider tests. */
class BrainManagerHttpTest {
    private val planSettings=AssistantSettings(provider="openai",model="gpt-test-plan",name="PRIVATE NAME")
    private fun client(server:MockWebServer,settings:AssistantSettings=planSettings,mode:ProviderCredentialMode=ProviderCredentialMode.API_KEY,key:String="saved-api-secret"):LlmClient {
        val root=server.url("/")
        val http=OkHttpClient.Builder().addInterceptor{chain->
            val request=chain.request()
            val local=root.newBuilder().encodedPath(request.url.encodedPath).encodedQuery(request.url.encodedQuery).build()
            chain.proceed(request.newBuilder().url(local).build())
        }.build()
        return LlmClient({settings},{key},http,{mode})
    }
    private fun request(server:MockWebServer):RecordedRequest=server.takeRequest(2,TimeUnit.SECONDS) ?: error("Expected HTTP request")
    private fun catalog(slug:String="gpt-test-plan")="""{"models":[{"slug":"$slug","display_name":"Plan model","visibility":"list"}]}"""
    private fun sse(vararg events:String)=MockResponse().setHeader("Content-Type","text/event-stream").setBody(events.joinToString(""){"data: $it\n\n"})
    private fun delta(text:String) = JSONObject().put("type","response.output_text.delta").put("delta",text).toString()
    private fun apiCompleted(text:String)="""{"id":"resp_test","object":"response","model":"gpt-test-plan","status":"completed","output":[{"type":"message","role":"assistant","content":[{"type":"output_text","text":"$text"}]}]}"""
    private fun completed()="""{"type":"response.completed","response":{"status":"completed"}}"""
    private suspend fun planAnswer(client:LlmClient,onChunk:(String)->Unit={}):String=client.answer("Hello",AppLanguage.EN,"",emptyList(),onChunk,planBearer="oauth-plan-secret")
    private suspend fun providerFailure(block:suspend()->Unit):ProviderException {
        val failure=runCatching{block()}.exceptionOrNull()
        assertTrue("Expected provider failure, got $failure",failure is ProviderException)
        return failure as ProviderException
    }

    @Test fun planCatalogUsesBearerHeaderAndPublicModelsRoute()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(MockResponse().setBody(catalog()))
            assertEquals(listOf(ChatGptPlanModel("gpt-test-plan","Plan model")),client(server).availableChatGptModels("oauth-plan-secret",AppLanguage.EN))
            val req=request(server)
            assertEquals("GET",req.method);assertEquals("/v1/models",req.path)
            assertEquals("Bearer oauth-plan-secret",req.getHeader("Authorization"))
            assertNull(req.getHeader("x-api-key"));assertFalse(req.path!!.contains("secret"))
        }
    }

    @Test fun planCatalogRetainsVisibleServerOrderAndHidesNonListEntries()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(MockResponse().setBody("""{"models":[{"slug":"gpt-z","visibility":"list"},{"slug":"gpt-hidden","visibility":"hidden"},{"slug":"gpt-a","visibility":"list"}]}"""))
            assertEquals(listOf("gpt-z","gpt-a"),client(server).availableChatGptModels("token",AppLanguage.EN).map{it.slug})
        }
    }

    @Test fun emptyPlanCatalogCannotProveAiAuthorization()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(MockResponse().setBody("""{"models":[]}"""))
            assertEquals(ProviderFailureKind.AUTHORIZATION,providerFailure{client(server).availableChatGptModels("token",AppLanguage.EN)}.kind)
        }
    }

    @Test fun apiShapedCatalogIsNotAcceptedAsPlanCatalog()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(MockResponse().setBody("""{"data":[{"id":"gpt-test-plan"}]}"""))
            providerFailure{client(server).availableChatGptModels("token",AppLanguage.EN)}
            assertEquals(1,server.requestCount)
        }
    }

    @Test fun catalogAuthenticationFailureIsTypedAndDoesNotLeakProviderBody()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"code":"invalid_token","message":"oauth-plan-secret"}}"""))
            val error=providerFailure{client(server).availableChatGptModels("token",AppLanguage.EN)}
            assertEquals(ProviderFailureKind.AUTHENTICATION,error.kind);assertEquals(401,error.httpStatus)
            assertFalse(error.message!!.contains("oauth-plan-secret"))
        }
    }

    @Test fun planInferenceUsesResponsesBearerAndNeverSavedApiCredential()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(sse(delta("Hello back"),completed()))
            assertEquals("Hello back",planAnswer(client(server,mode=ProviderCredentialMode.CHATGPT_PLAN)))
            val req=request(server)
            assertEquals("POST",req.method);assertEquals("/v1/responses",req.path)
            assertEquals("Bearer oauth-plan-secret",req.getHeader("Authorization"))
            assertEquals("text/event-stream",req.getHeader("Accept"))
            assertFalse(req.body.readUtf8().contains("saved-api-secret"))
            assertEquals(1,server.requestCount)
        }
    }

    @Test fun planRequestUsesNonstoredArrayInputAndOmitsUnsupportedPreviewParameters()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(sse(delta("Answer"),completed()))
            planAnswer(client(server))
            val body=JSONObject(request(server).body.readUtf8())
            assertFalse(body.getBoolean("store"));assertTrue(body.getBoolean("stream"))
            assertEquals("gpt-test-plan",body.getString("model"));assertEquals("user",body.getJSONArray("input").getJSONObject(0).getString("role"))
            for(name in listOf("messages","max_tokens","max_output_tokens","temperature","top_p","previous_response_id"))assertFalse(name,body.has(name))
        }
    }

    @Test fun lockedPlanRequestCanOmitSavedIdentityAndFilterStatusHistory()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(sse(delta("Answer"),completed()))
            val history=listOf(ChatMessage(role=MessageRole.STATUS,text="PRIVATE STATUS"),ChatMessage(role=MessageRole.USER,text="Hello"))
            client(server).answer("Hello",AppLanguage.EN,"reference",history,{},includeIdentity=false,planBearer="plan")
            val raw=request(server).body.readUtf8();val body=JSONObject(raw)
            assertFalse(raw.contains("PRIVATE NAME"));assertFalse(raw.contains("PRIVATE STATUS"))
            assertEquals(1,body.getJSONArray("input").length());assertTrue(raw.contains("reference"))
        }
    }

    @Test fun planConnectionTestRequiresCatalogAndActualTerminalInference()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(MockResponse().setBody(catalog()));server.enqueue(sse(delta("READY"),completed()))
            val result=client(server).testConnection(planSettings,"ignored-api-secret",planBearer="oauth")
            assertEquals("READY",result.sample);assertEquals("gpt-test-plan",result.model);assertTrue(result.elapsedMs>=0)
            assertEquals("/v1/models",request(server).path)
            val inference=request(server);assertEquals("/v1/responses",inference.path)
            val body=inference.body.readUtf8();assertTrue(body.contains("READY"));assertFalse(body.contains("PRIVATE NAME"));assertFalse(body.contains("ignored-api-secret"))
        }
    }

    @Test fun unavailablePlanModelStopsBeforeInferenceWithoutFallback()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(MockResponse().setBody(catalog("gpt-other")))
            assertEquals(ProviderFailureKind.MODEL_UNAVAILABLE,providerFailure{client(server).testConnection(planSettings,"",planBearer="oauth")}.kind)
            assertEquals(1,server.requestCount);assertEquals("/v1/models",request(server).path)
        }
    }

    @Test fun planDeltaWithoutCompletedEventCannotVerifyAnswer()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(sse(delta("Provisional text")))
            assertEquals(ProviderFailureKind.INCOMPLETE,providerFailure{planAnswer(client(server))}.kind)
        }
    }

    @Test fun planErrorAfterVisibleDeltaStillFailsAndDoesNotRetryWithApiKey()=runBlocking<Unit> {
        MockWebServer().use{server->
            val chunks=mutableListOf<String>()
            server.enqueue(sse(delta("Provisional"),"""{"type":"response.failed","response":{"error":{"code":"subscription_sharing_usage_limit_exceeded","message":"private diagnostic"}}}"""))
            val failure=providerFailure{planAnswer(client(server)){chunks+=it}}
            assertEquals(listOf("Provisional"),chunks);assertEquals(ProviderFailureKind.RATE_LIMIT,failure.kind)
            assertFalse(failure.message!!.contains("private diagnostic"));assertEquals(1,server.requestCount)
        }
    }

    @Test fun completedEventCanSupplyActualTextWhenNoDeltasWereSent()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(sse("""{"type":"response.completed","response":{"status":"completed","output":[{"content":[{"type":"output_text","text":"Actual result"}]}]}}"""))
            assertEquals("Actual result",planAnswer(client(server)))
        }
    }

    @Test fun completedEventWithoutTextCannotProveConnection()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(sse(completed()))
            assertEquals(ProviderFailureKind.INCOMPLETE,providerFailure{planAnswer(client(server))}.kind)
        }
    }

    @Test fun malformedPlanSseIsReportedAsInvalidResponse()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(sse("not json"))
            assertEquals(ProviderFailureKind.INCOMPLETE,providerFailure{planAnswer(client(server))}.kind)
        }
    }

    @Test fun selectedPlanDoesNotSilentlyUseSavedApiKeyForOrdinaryReply()=runBlocking<Unit> {
        MockWebServer().use{server->
            val failure=providerFailure{client(server,mode=ProviderCredentialMode.CHATGPT_PLAN).answer("hello",AppLanguage.EN,"",emptyList(),{})}
            assertEquals(ProviderFailureKind.AUTHORIZATION,failure.kind);assertEquals(0,server.requestCount)
        }
    }

    @Test fun selectedPlanDoesNotSilentlyUseSavedApiKeyForConnectionTest()=runBlocking<Unit> {
        MockWebServer().use{server->
            providerFailure{client(server,mode=ProviderCredentialMode.CHATGPT_PLAN).testConnection(planSettings,"saved-api-secret")}
            assertEquals(0,server.requestCount)
        }
    }

    @Test fun selectedPlanRefusesUnsupportedVisionWithoutSendingPaidRequest()=runBlocking<Unit> {
        MockWebServer().use{server->
            val failure=providerFailure{client(server,mode=ProviderCredentialMode.CHATGPT_PLAN).vision(false,AppLanguage.EN,listOf(byteArrayOf(1)),{})}
            assertEquals(ProviderFailureKind.AUTHORIZATION,failure.kind);assertEquals(0,server.requestCount)
        }
    }

    @Test fun planVoiceRequestRefusesScreenshotInsteadOfDroppingImageSilently()=runBlocking<Unit> {
        MockWebServer().use{server->
            providerFailure{client(server).answer("What is here?",AppLanguage.EN,"",emptyList(),{},screenshot=byteArrayOf(1),planBearer="plan")}
            assertEquals(0,server.requestCount)
        }
    }

    @Test fun explicitApiConnectionTestCanBeRequestedWithoutChangingPlanSelection()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(MockResponse().setHeader("Content-Type","application/json").setBody(apiCompleted("READY")))
            assertEquals("READY",client(server,mode=ProviderCredentialMode.CHATGPT_PLAN).testConnection(planSettings,"explicit-api-key",explicitApi=true).sample)
            val req=request(server);assertEquals("/v1/responses",req.path);assertEquals("Bearer explicit-api-key",req.getHeader("Authorization"))
            val body=JSONObject(req.body.readUtf8());assertFalse(body.getBoolean("stream"));assertEquals(1024,body.getInt("max_output_tokens"));assertFalse(body.has("max_tokens"))
        }
    }

    @Test fun explicitApiAnswerOverrideUsesRequestedApiCredential()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(sse(delta("API answer"),JSONObject().put("type","response.completed").put("response",JSONObject(apiCompleted("API answer"))).toString()))
            assertEquals("API answer",client(server,mode=ProviderCredentialMode.CHATGPT_PLAN).answer("Hi",AppLanguage.EN,"",emptyList(),{},apiKeyOverride="explicit-api-key"))
            val req=request(server);assertEquals("/v1/responses",req.path);assertEquals("Bearer explicit-api-key",req.getHeader("Authorization"))
        }
    }

    @Test fun unsupportedApiModelDoesNotQuietlyVerifyDifferentFallbackModel()=runBlocking<Unit> {
        MockWebServer().use{server->
            val settings=planSettings.copy(model="claude-wrong-family")
            providerFailure{client(server).testConnection(settings,"key",explicitApi=true)}
            assertEquals(0,server.requestCount)
        }
    }

    @Test fun anthropicStreamingUsesSupportedBearerVersionAndReturnsActualText()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(sse("""{"type":"content_block_delta","delta":{"type":"text_delta","text":"Claude answer"}}""","""{"type":"message_stop"}"""))
            val settings=AssistantSettings(provider="anthropic",model="claude-test")
            assertEquals("Claude answer",client(server,settings).answer("Hi",AppLanguage.EN,"",emptyList(),{}))
            val req=request(server);assertEquals("/v1/messages",req.path)
            assertEquals("Bearer saved-api-secret",req.getHeader("Authorization"));assertEquals("2026-10-21",req.getHeader("anthropic-version"));assertNull(req.getHeader("x-api-key"))
            val body=JSONObject(req.body.readUtf8());assertEquals("claude-test",body.getString("model"));assertTrue(body.getBoolean("stream"));assertTrue(body.has("system"))
        }
    }

    @Test fun anthropicConnectionTestParsesRealMessagesResponseAndNoUserData()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(MockResponse().setBody("""{"content":[{"type":"text","text":"READY"}]}"""))
            val settings=AssistantSettings(provider="anthropic",model="claude-test",name="PRIVATE NAME")
            assertEquals("READY",client(server,settings).testConnection(settings,"personal-key",explicitApi=true).sample)
            val req=request(server);assertEquals("Bearer personal-key",req.getHeader("Authorization"));assertEquals("2026-10-21",req.getHeader("anthropic-version"))
            val raw=req.body.readUtf8();assertFalse(raw.contains("PRIVATE NAME"));assertTrue(raw.contains("READY"));assertFalse(JSONObject(raw).getBoolean("stream"))
        }
    }

    @Test fun emptyAnthropicMessagesResponseCannotVerifyConnection()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(MockResponse().setBody("""{"content":[]}"""))
            val settings=AssistantSettings(provider="anthropic",model="claude-test")
            providerFailure{client(server,settings).testConnection(settings,"key",explicitApi=true)}
        }
    }

    @Test fun anthropicCatalogUsesApiCredentialAndVersionWithoutTreatingListingAsInference()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(MockResponse().setBody("""{"data":[{"id":"claude-a"},{"id":"claude-b"}]}"""))
            assertEquals(listOf("claude-a","claude-b"),client(server).availableApiModels("anthropic","personal-key",AppLanguage.EN))
            val req=request(server);assertEquals("/v1/models?limit=1000",req.path);assertEquals("Bearer personal-key",req.getHeader("Authorization"));assertEquals("2026-10-21",req.getHeader("anthropic-version"));assertEquals(1,server.requestCount)
        }
    }

    @Test fun explicitApiCatalogRequiresDataIdsAndFiltersUnsafeModelNames()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(MockResponse().setHeader("Content-Type","application/json").setBody("""{"data":[{"id":"gpt-good"},{"id":"../../unsafe"},{"id":"gpt-good"},{"id":"gpt-other"}]}"""))
            assertEquals(listOf("gpt-good","gpt-other"),client(server).availableApiModels("openai","personal",AppLanguage.EN))
            assertEquals("Bearer personal",request(server).getHeader("Authorization"))
        }
    }

    @Test fun planUsageFailureHasNoApiRetryAndNoSensitiveErrorReflection()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(MockResponse().setResponseCode(429).setBody("""{"error":{"code":"subscription_sharing_usage_unavailable","message":"oauth-plan-secret private email"}}"""))
            val failure=providerFailure{planAnswer(client(server))}
            assertEquals(ProviderFailureKind.USAGE_UNAVAILABLE,failure.kind);assertFalse(failure.message!!.contains("private email"));assertFalse(failure.message!!.contains("oauth-plan-secret"));assertEquals(1,server.requestCount)
        }
    }

    @Test fun apiBillingFailureIsReportedAsCreditsInsteadOfSuccessfulConnection()=runBlocking<Unit> {
        MockWebServer().use{server->
            server.enqueue(MockResponse().setResponseCode(429).setBody("""{"error":{"code":"insufficient_quota"}}"""))
            assertEquals(ProviderFailureKind.CREDITS,providerFailure{client(server).testConnection(planSettings,"key",explicitApi=true)}.kind)
        }
    }

    @Test fun cancellationAfterFirstPlanTextDoesNotCompleteOrLaunchFallback()=runBlocking<Unit> {
        MockWebServer().use{server->
            val first=CompletableDeferred<String>()
            val stream="data: ${delta("Provisional")}\n\n: "+"x".repeat(32000)+"\n\ndata: ${completed()}\n\n"
            server.enqueue(MockResponse().setHeader("Content-Type","text/event-stream").setBody(stream).throttleBody(8192,1,TimeUnit.SECONDS))
            val pending=async{planAnswer(client(server)){first.complete(it)}}
            assertEquals("Provisional",withTimeout(3000){first.await()});assertFalse(pending.isCompleted)
            pending.cancelAndJoin();assertTrue(pending.isCancelled);assertEquals(1,server.requestCount)
        }
    }

    @Test fun blankPlanBearerNeverDispatchesHttpRequest()=runBlocking<Unit> {
        MockWebServer().use{server->
            providerFailure{client(server).availableChatGptModels("",AppLanguage.EN)}
            providerFailure{client(server).answer("Hi",AppLanguage.EN,"",emptyList(),{},planBearer="")}
            assertEquals(0,server.requestCount)
        }
    }

    @Test fun planCredentialCannotBeSentToAnthropicProvider()=runBlocking<Unit> {
        MockWebServer().use{server->
            val failure=runCatching{client(server,AssistantSettings(provider="anthropic",model="claude-test")).answer("Hi",AppLanguage.EN,"",emptyList(),{},planBearer="plan")}.exceptionOrNull()
            assertTrue(failure is IllegalArgumentException);assertEquals(0,server.requestCount)
        }
    }
}
