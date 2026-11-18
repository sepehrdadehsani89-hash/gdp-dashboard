package com.jarvis.assistant.data

import com.jarvis.assistant.brain.ProviderFailureKind
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantSettings
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class PerplexityAgentHttpTest {
    private val settings=AssistantSettings(provider="perplexity",model="vendor/actual-model",name="PRIVATE_NAME")
    private fun client(server:MockWebServer,key:String="private-test-key",failure:java.io.IOException?=null):LlmClient {
        val origin=server.url("/");val http=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).callTimeout(3,TimeUnit.SECONDS).addInterceptor{chain->
            failure?.let{throw it};val request=chain.request();chain.proceed(request.newBuilder().url(origin.newBuilder().encodedPath(request.url.encodedPath).encodedQuery(request.url.encodedQuery).build()).build())}.build()
        return LlmClient({settings},{key},http)
    }
    private fun completed(text:String="Actual answer",model:String=settings.model)=JSONObject("""{"id":"resp_fixture","object":"response","status":"completed","output":[{"type":"message","role":"assistant","content":[{"type":"output_text","text":"placeholder"}]}]}""").put("model",model).apply{getJSONArray("output").getJSONObject(0).getJSONArray("content").getJSONObject(0).put("text",text)}
    private fun json(body:String)=MockResponse().setHeader("Content-Type","application/json").setBody(body)
    private fun sse(vararg events:String)=MockResponse().setHeader("Content-Type","text/event-stream").setBody(events.joinToString(""){"data: $it\n\n"})
    private fun done(text:String="Actual answer",model:String=settings.model)=JSONObject().put("type","response.completed").put("response",completed(text,model)).toString()
    private fun delta(text:String)=JSONObject().put("type","response.output_text.delta").put("delta",text).toString()
    private suspend fun answer(c:LlmClient,onChunk:(String)->Unit={})=c.answer("Hello",AppLanguage.EN,"",emptyList(),onChunk)
    private suspend fun failure(block:suspend()->Unit):ProviderException {val error=runCatching{block()}.exceptionOrNull();assertTrue("Expected provider error, got $error",error is ProviderException);return error as ProviderException}
    private suspend fun httpError(status:Int,code:String,kind:ProviderFailureKind){MockWebServer().use{s->s.enqueue(json(JSONObject().put("error",JSONObject().put("code",code).put("message","PRIVATE_BODY")).toString()).setResponseCode(status));val e=failure{answer(client(s))};assertEquals(kind,e.kind);assertEquals(status,e.httpStatus);assertFalse(e.message!!.contains("PRIVATE_BODY"));assertEquals(1,s.requestCount)}}
    @Test fun explicitTextUsesCanonicalAgentEndpoint()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(sse(done()));assertEquals("Actual answer",answer(client(s)));assertEquals("/v1/agent",s.takeRequest().path)}}
    @Test fun keyStaysInAuthorizationHeader()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(sse(done()));answer(client(s));val r=s.takeRequest();assertEquals("Bearer private-test-key",r.getHeader("Authorization"));assertFalse(r.body.readUtf8().contains("private-test-key"));assertFalse(r.path!!.contains("private-test-key"))}}
    @Test fun requestedModelIsNotReplaced()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(sse(done()));answer(client(s));assertEquals(settings.model,JSONObject(s.takeRequest().body.readUtf8()).getString("model"))}}
    @Test fun cloudDeviceToolsAreNotProvided()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(sse(done()));answer(client(s));val j=JSONObject(s.takeRequest().body.readUtf8());assertEquals(0,j.getJSONArray("tools").length());assertFalse(j.has("previous_response_id"));assertFalse(j.getBoolean("store"))}}
    @Test fun deltasAreProvisionalAndFinalIsAuthoritative()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(sse(delta("Draft"),done("Final")));val chunks=mutableListOf<String>();assertEquals("Final",answer(client(s),chunks::add));assertEquals(listOf("Draft"),chunks)}}
    @Test fun deltaAloneCannotVerifyAnAnswer()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(sse(delta("Draft"),"[DONE]"));assertEquals(ProviderFailureKind.INCOMPLETE,failure{answer(client(s))}.kind)}}
    @Test fun blankCompletedAnswerCannotVerifyConnection()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(sse(done(" ")));assertEquals(ProviderFailureKind.INCOMPLETE,failure{answer(client(s))}.kind)}}
    @Test fun wrongModelCannotVerifyConnection()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(sse(done(model="other/model")));assertEquals(ProviderFailureKind.INCOMPLETE,failure{answer(client(s))}.kind)}}
    @Test fun failedEventNeverReturnsThePartialAnswer()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(sse(delta("Draft"),"""{"type":"response.failed","response":{"error":{"code":"insufficient_quota"}}}"""));assertEquals(ProviderFailureKind.CREDITS,failure{answer(client(s))}.kind)}}
    @Test fun incompleteEventIsNotSuccess()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(sse("""{"type":"response.incomplete"}"""));assertEquals(ProviderFailureKind.INCOMPLETE,failure{answer(client(s))}.kind)}}
    @Test fun jsonCannotMasqueradeAsStream()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(json(completed().toString()));assertEquals(ProviderFailureKind.INCOMPLETE,failure{answer(client(s))}.kind)}}
    @Test fun inferenceTestUsesNonstreamAndNoPrivateIdentity()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(json(completed("READY").toString()));val result=client(s).testConnection(settings,"private-test-key",explicitApi=true);assertEquals("READY",result.sample);val body=JSONObject(s.takeRequest().body.readUtf8());assertFalse(body.getBoolean("stream"));assertFalse(body.toString().contains("PRIVATE_NAME"))}}
    @Test fun publicCatalogDoesNotSendAnAccountKey()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(json("""{"object":"list","data":[{"object":"model","id":"vendor/current"}]}"""));assertEquals(listOf("vendor/current"),client(s).availableApiModels("perplexity","",AppLanguage.EN));val r=s.takeRequest();assertEquals("GET",r.method);assertEquals("/v1/models",r.path);assertNull(r.getHeader("Authorization"))}}
    @Test fun publicCatalogCanBeRetrievedWithoutASavedKey()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(json("""{"object":"list","data":[{"object":"model","id":"vendor/current"}]}"""));assertEquals(1,client(s,key="").availableApiModels("perplexity","",AppLanguage.EN).size)}}
    @Test fun missingInferenceCredentialSendsNothing()=runBlocking<Unit>{MockWebServer().use{s->failure{answer(client(s,key=""))};assertEquals(0,s.requestCount)}}
    @Test fun badModelDoesNotFallBackToAnotherProvider()=runBlocking<Unit>{MockWebServer().use{s->try{client(s).answer("Hi",AppLanguage.EN,"",emptyList(),{},settingsOverride=settings.copy(model="invented"));fail("Invalid model accepted")}catch(_:IllegalArgumentException){};assertEquals(0,s.requestCount)}}
    @Test fun refusedImageDoesNotSendAPaidAlternateRequest()=runBlocking<Unit>{MockWebServer().use{s->assertEquals(ProviderFailureKind.AUTHORIZATION,failure{client(s).answer("Hi",AppLanguage.EN,"",emptyList(),{},screenshot=byteArrayOf(1))}.kind);assertEquals(0,s.requestCount)}}
    @Test fun deniedCredentialIsTyped()=runBlocking{httpError(401,"invalid_api_key",ProviderFailureKind.AUTHENTICATION)}
    @Test fun deniedPermissionIsTyped()=runBlocking{httpError(403,"permission_error",ProviderFailureKind.AUTHORIZATION)}
    @Test fun exhaustedCreditsAreTyped()=runBlocking{httpError(429,"insufficient_quota",ProviderFailureKind.CREDITS)}
    @Test fun rateLimitDoesNotAutomaticallyRetry()=runBlocking{httpError(429,"rate_limit_error",ProviderFailureKind.RATE_LIMIT)}
    @Test fun serverFailureDoesNotAutomaticallyRetry()=runBlocking{httpError(503,"server_error",ProviderFailureKind.USAGE_UNAVAILABLE)}
    @Test fun badParametersAreNotCalledBadPasswords()=runBlocking{httpError(400,"unsupported_parameter",ProviderFailureKind.BAD_REQUEST)}
    @Test fun unavailableModelIsTyped()=runBlocking{httpError(404,"model_not_found",ProviderFailureKind.MODEL_UNAVAILABLE)}
    @Test fun dnsFailureDoesNotExposeNetworkDetails()=runBlocking<Unit>{MockWebServer().use{s->val e=failure{answer(client(s,failure=java.net.UnknownHostException("PRIVATE_DNS")))};assertEquals(ProviderFailureKind.NETWORK,e.kind);assertFalse(e.message!!.contains("PRIVATE_DNS"))}}
    @Test fun tlsFailureDoesNotExposeCertificateDetails()=runBlocking<Unit>{MockWebServer().use{s->val e=failure{answer(client(s,failure=javax.net.ssl.SSLHandshakeException("PRIVATE_CERT")))};assertEquals(ProviderFailureKind.NETWORK,e.kind);assertFalse(e.message!!.contains("PRIVATE_CERT"))}}
    @Test fun cancelledInferenceDoesNotRetry()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(sse(done()).setBodyDelay(2,TimeUnit.SECONDS));val c=client(s);val job=launch{answer(c)};withContext(Dispatchers.IO){assertNotNull(s.takeRequest(2,TimeUnit.SECONDS))};job.cancelAndJoin();assertTrue(job.isCancelled);assertEquals(1,s.requestCount)}}
    @Test fun malformedStreamIsNotReportedAsOpenAI()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(sse("{bad"));val e=failure{answer(client(s))};assertEquals(ProviderFailureKind.INCOMPLETE,e.kind);assertTrue(e.message!!.contains("Perplexity"));assertFalse(e.message!!.contains("OpenAI"))}}
}
