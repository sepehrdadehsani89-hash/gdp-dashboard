package com.jarvis.assistant.data

import com.jarvis.assistant.brain.ProviderFailureKind
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantSettings
import com.jarvis.assistant.voice.BackendVoiceErrors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

/** MockWebServer contract tests. No real API key, provider inference or physical audio is used. */
class OpenAiResponsesHttpTest {
    private val settings=AssistantSettings(provider="openai",model="gpt-4.1-mini",name="PRIVATE_NAME")
    private fun client(server:MockWebServer,fail:java.io.IOException?=null):LlmClient {
        val root=server.url("/")
        val http=OkHttpClient.Builder().retryOnConnectionFailure(false).addInterceptor{chain->
            fail?.let{throw it}
            val original=chain.request();val local=root.newBuilder().encodedPath(original.url.encodedPath).encodedQuery(original.url.encodedQuery).build()
            chain.proceed(original.newBuilder().url(local).build())
        }.build()
        return LlmClient({settings},{"private-test-key"},http)
    }
    private fun completed(text:String="Actual answer",model:String=settings.model)=JSONObject("""{"id":"resp_fixture","object":"response","status":"completed","output":[{"type":"message","role":"assistant","content":[{"type":"output_text","text":"placeholder"}]}]}""").put("model",model).apply{getJSONArray("output").getJSONObject(0).getJSONArray("content").getJSONObject(0).put("text",text)}
    private fun json(body:String)=MockResponse().setHeader("Content-Type","application/json").setBody(body)
    private fun sse(vararg events:String)=MockResponse().setHeader("Content-Type","text/event-stream").setBody(events.joinToString(""){"data: $it\n\n"})
    private fun done(text:String="Actual answer")=JSONObject().put("type","response.completed").put("response",completed(text)).toString()
    private fun delta(text:String) = JSONObject().put("type","response.output_text.delta").put("delta",text).toString()
    private suspend fun answer(c:LlmClient,onChunk:(String)->Unit={})=c.answer("Hello",AppLanguage.EN,"",emptyList(),onChunk)
    private suspend fun failure(block:suspend()->Unit):ProviderException {val e=runCatching{block()}.exceptionOrNull();assertTrue("Expected ProviderException, got $e",e is ProviderException);return e as ProviderException}
    private suspend fun httpError(status:Int,code:String,kind:ProviderFailureKind){MockWebServer().use{s->s.enqueue(json(JSONObject().put("error",JSONObject().put("code",code).put("message","PRIVATE_BODY")).toString()).setResponseCode(status));val e=failure{answer(client(s))};assertEquals(kind,e.kind);assertEquals(status,e.httpStatus);assertFalse(e.message!!.contains("PRIVATE_BODY"));assertEquals(1,s.requestCount);assertNotNull(BackendVoiceErrors.message(e,AppLanguage.EN))}}
    @Test fun modernAnswerUsesResponsesAndBearerOnly()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(sse(delta("Actual answer"),done()));assertEquals("Actual answer",answer(client(s)));val r=s.takeRequest();assertEquals("/v1/responses",r.path);assertEquals("POST",r.method);assertEquals("Bearer private-test-key",r.getHeader("Authorization"));assertFalse(r.body.readUtf8().contains("private-test-key"));assertNull(r.getHeader("x-api-key"))}}
    @Test fun connectionProbeUsesNonstreamResponsesAndNoPhoneContext()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(json(completed("READY").toString()));val result=client(s).testConnection(settings,"private-test-key");assertEquals("READY",result.sample);assertEquals(settings.model,result.resolvedModel);val r=s.takeRequest();val raw=r.body.readUtf8();val body=JSONObject(raw);assertFalse(raw.contains("PRIVATE_NAME"));assertFalse(body.getBoolean("store"));assertFalse(body.getBoolean("stream"));assertEquals(1024,body.getInt("max_output_tokens"));assertFalse(body.has("max_tokens"))}}
    @Test fun responseAliasIsRecordedSeparatelyFromRequestedModel()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(json(completed("READY","gpt-4.1-mini-2025-04-14").toString()));val r=client(s).testConnection(settings,"key");assertEquals(settings.model,r.model);assertEquals("gpt-4.1-mini-2025-04-14",r.resolvedModel)}}
    @Test fun missingKeyMakesNoNetworkRequest()=runBlocking<Unit>{MockWebServer().use{s->val c=LlmClient({settings},{""});assertEquals(ProviderFailureKind.AUTHENTICATION,failure{c.testConnection(settings,"")}.kind);assertEquals(0,s.requestCount)}}
    @Test fun http400ExplainsRequestCompatibility()=runBlocking<Unit>{httpError(400,"unsupported_parameter",ProviderFailureKind.BAD_REQUEST)}
    @Test fun http401RejectsCredential()=runBlocking<Unit>{httpError(401,"invalid_api_key",ProviderFailureKind.AUTHENTICATION)}
    @Test fun http403ExplainsAuthorization()=runBlocking<Unit>{httpError(403,"permission_error",ProviderFailureKind.AUTHORIZATION)}
    @Test fun http404ExplainsUnavailableModel()=runBlocking<Unit>{httpError(404,"model_not_found",ProviderFailureKind.MODEL_UNAVAILABLE)}
    @Test fun http429RateLimitIsNotRetriedInference()=runBlocking<Unit>{httpError(429,"rate_limit_error",ProviderFailureKind.RATE_LIMIT)}
    @Test fun http429QuotaIsNotRetriedInference()=runBlocking<Unit>{httpError(429,"insufficient_quota",ProviderFailureKind.CREDITS)}
    @Test fun http500IsNotRetriedInference()=runBlocking<Unit>{httpError(500,"server_error",ProviderFailureKind.USAGE_UNAVAILABLE)}
    @Test fun malformedProbeJsonCannotVerify()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(json("PRIVATE invalid"));val e=failure{client(s).testConnection(settings,"key")};assertEquals(ProviderFailureKind.INCOMPLETE,e.kind);assertFalse(e.message!!.contains("PRIVATE"))}}
    @Test fun htmlSuccessPageCannotVerify()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(MockResponse().setHeader("Content-Type","text/html").setBody("<html>login</html>"));assertEquals(ProviderFailureKind.INCOMPLETE,failure{client(s).testConnection(settings,"key")}.kind)}}
    @Test fun emptyCompletedProbeCannotVerify()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(json(completed("").toString()));assertEquals(ProviderFailureKind.INCOMPLETE,failure{client(s).testConnection(settings,"key")}.kind)}}
    @Test fun streamEndingAfterDeltasCannotVerify()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(sse(delta("Provisional")));assertEquals(ProviderFailureKind.INCOMPLETE,failure{answer(client(s))}.kind)}}
    @Test fun streamFailureAfterDeltaDoesNotReturnPartialAnswer()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(sse(delta("Provisional"),"""{"type":"response.failed","response":{"error":{"code":"insufficient_quota"}}}"""));assertEquals(ProviderFailureKind.CREDITS,failure{answer(client(s))}.kind);assertEquals(1,s.requestCount)}}
    @Test fun jsonBodyCannotStandInForEventStream()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(json(completed().toString()));assertEquals(ProviderFailureKind.INCOMPLETE,failure{answer(client(s))}.kind)}}
    @Test fun dnsFailureIsNetworkNotFakeTimeout()=runBlocking<Unit>{MockWebServer().use{s->assertEquals(ProviderFailureKind.NETWORK,failure{answer(client(s,java.net.UnknownHostException("PRIVATE_DNS")))}.kind)}}
    @Test fun tlsFailureIsTypedAndRedacted()=runBlocking<Unit>{MockWebServer().use{s->val e=failure{answer(client(s,javax.net.ssl.SSLHandshakeException("PRIVATE_CERT")))};assertEquals(ProviderFailureKind.NETWORK,e.kind);assertTrue(e.message!!.contains("TLS"));assertFalse(e.message!!.contains("PRIVATE_CERT"))}}
    @Test fun socketTimeoutHasDistinctCategory()=runBlocking<Unit>{MockWebServer().use{s->assertEquals(ProviderFailureKind.TIMEOUT,failure{answer(client(s,java.net.SocketTimeoutException("timeout")))}.kind)}}
    @Test fun retryAfterAndSafeRequestIdAreRecorded()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(json("""{"error":{"code":"rate_limit_error"}}""").setResponseCode(429).setHeader("Retry-After","3").setHeader("x-request-id","req_0123456789abcdef"));val e=failure{answer(client(s))};assertEquals(3000L,e.retryAfterMs);assertEquals("req_0123456789abcdef",e.requestId)}}
    @Test fun unsafeRequestIdIsNotReflected()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(json("{}").setResponseCode(500).setHeader("x-request-id","private-test-key"));assertNull(failure{answer(client(s))}.requestId)}}
    @Test fun catalogUsesOnlyReturnedChatCandidates()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(json("""{"data":[{"id":"gpt-4.1-mini"},{"id":"gpt-image-1"},{"id":"gpt-realtime"}]}"""));assertEquals(listOf("gpt-4.1-mini"),client(s).availableApiModels("openai","",AppLanguage.EN));val r=s.takeRequest();assertEquals("GET",r.method);assertEquals("/v1/models",r.path)}}
    @Test fun catalogAuthenticationFailureIsNeverRetried()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(json("{}").setResponseCode(401));assertEquals(ProviderFailureKind.AUTHENTICATION,failure{client(s).availableApiModels("openai","",AppLanguage.EN)}.kind);assertEquals(1,s.requestCount)}}
    @Test fun catalogRetriesTransientSafeReadOnly()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(json("{}").setResponseCode(503));s.enqueue(json("""{"data":[{"id":"gpt-4.1-mini"}]}"""));assertEquals(listOf(settings.model),client(s).availableApiModels("openai","",AppLanguage.EN));assertEquals(2,s.requestCount)}}
    @Test fun catalogNeverHammerRetriesBeyondLimit()=runBlocking<Unit>{MockWebServer().use{s->repeat(3){s.enqueue(json("{}").setResponseCode(503))};failure{client(s).availableApiModels("openai","",AppLanguage.EN)};assertEquals(3,s.requestCount)}}
    @Test fun catalogQuotaFailureNeverRetries()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(json("""{"error":{"code":"insufficient_quota"}}""").setResponseCode(429));assertEquals(ProviderFailureKind.CREDITS,failure{client(s).availableApiModels("openai","",AppLanguage.EN)}.kind);assertEquals(1,s.requestCount)}}
    @Test fun cancellationAfterFirstDeltaCannotDeliverCompletedReply()=runBlocking<Unit>{MockWebServer().use{s->val first=CompletableDeferred<Unit>();val payload="data: ${delta("provisional")}\n\n: "+"x".repeat(32000)+"\n\ndata: ${done()}\n\n";s.enqueue(MockResponse().setHeader("Content-Type","text/event-stream").setBody(payload).throttleBody(512,30,TimeUnit.MILLISECONDS));val job=async{answer(client(s)){first.complete(Unit)}};withTimeout(3000){first.await()};job.cancelAndJoin();assertTrue(job.isCancelled);assertEquals(1,s.requestCount)}}
}
