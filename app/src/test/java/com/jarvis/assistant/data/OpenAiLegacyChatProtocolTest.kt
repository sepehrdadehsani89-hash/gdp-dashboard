package com.jarvis.assistant.data

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantSettings
import com.jarvis.assistant.brain.ProviderFailureKind
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

/** Legacy Chat Completions fixtures; not live provider responses. */
class OpenAiLegacyChatProtocolTest {
    private val model="gpt-4"
    private fun completed(text:String="READY",actual:String=model,reason:String="stop")=JSONObject().put("id","chatcmpl-fixture").put("object","chat.completion").put("model",actual)
        .put("choices",JSONArray().put(JSONObject().put("index",0).put("finish_reason",reason).put("message",JSONObject().put("role","assistant").put("content",text))))
    private fun chunk(text:String="Answer",reason:String?=null,actual:String=model)=JSONObject().put("id","chatcmpl-fixture").put("object","chat.completion.chunk").put("model",actual)
        .put("choices",JSONArray().put(JSONObject().put("index",0).put("finish_reason",reason ?: JSONObject.NULL).put("delta",JSONObject().put("content",text)))).toString()
    private fun rejects(root:JSONObject){val e=runCatching{OpenAiLegacyChatProtocol.completed(root.toString(),model)}.exceptionOrNull();assertTrue(e is ProviderException);assertEquals(ProviderFailureKind.INCOMPLETE,(e as ProviderException).kind)}
    private fun client(s:MockWebServer):LlmClient {val url=s.url("/");return LlmClient({AssistantSettings(provider="openai",model=model)},{"test-key"},OkHttpClient.Builder().retryOnConnectionFailure(false).addInterceptor{chain->val r=chain.request();chain.proceed(r.newBuilder().url(url.newBuilder().encodedPath(r.url.encodedPath).build()).build())}.build())}
    @Test fun completedAssistantTextIsAccepted(){assertEquals("READY",OpenAiLegacyChatProtocol.completed(completed().toString(),model).text)}
    @Test fun documentedLegacySnapshotSuffixIsAccepted(){assertEquals("gpt-4-0613",OpenAiLegacyChatProtocol.completed(completed(actual="gpt-4-0613").toString(),model).model)}
    @Test fun legacyInvalidMonthDayCannotResolveAlias(){rejects(completed(actual="gpt-4-1332"))}
    @Test fun modernDateSuffixCanResolveLegacyTurbo(){val r=completed(actual="gpt-4-turbo-2024-04-09");assertEquals("READY",OpenAiLegacyChatProtocol.completed(r.toString(),"gpt-4-turbo").text)}
    @Test fun documentedTurboPreviewAliasIsRetained(){assertEquals("READY",OpenAiLegacyChatProtocol.completed(completed(actual="gpt-4-0125-preview").toString(),"gpt-4-turbo-preview").text)}
    @Test fun differentModelCannotVerify(){rejects(completed(actual="gpt-3.5-turbo"))}
    @Test fun missingResponseIdCannotVerify(){rejects(completed().apply{remove("id")})}
    @Test fun wrongResponseObjectCannotVerify(){rejects(completed().put("object","response"))}
    @Test fun lengthTruncatedAnswerCannotVerify(){rejects(completed(reason="length"))}
    @Test fun toolCallAloneCannotVerifyText(){rejects(completed(reason="tool_calls"))}
    @Test fun contentFilterTerminationCannotVerifyCompletedText(){rejects(completed(reason="content_filter"))}
    @Test fun emptyAnswerCannotVerify(){rejects(completed(" "))}
    @Test fun userEchoCannotVerifyAssistantResponse(){val r=completed();r.getJSONArray("choices").getJSONObject(0).getJSONObject("message").put("role","user");rejects(r)}
    @Test fun malformedJsonIsTyped(){assertEquals(ProviderFailureKind.INCOMPLETE,(runCatching{OpenAiLegacyChatProtocol.completed("invalid private",model)}.exceptionOrNull() as ProviderException).kind)}
    @Test fun oversizedAnswerCannotVerify(){rejects(completed("x".repeat(64001)))}
    @Test fun explicitCompletedRefusalIsActualInference(){val r=completed();r.getJSONArray("choices").getJSONObject(0).getJSONObject("message").put("content",JSONObject.NULL).put("refusal","I cannot.");assertEquals("I cannot.",OpenAiLegacyChatProtocol.completed(r.toString(),model).text)}
    @Test fun streamedTextWithoutFinishReasonCannotVerify(){val s=OpenAiLegacyChatStream(model){};s.event(chunk());assertTrue(runCatching{s.finish()}.isFailure)}
    @Test fun doneMarkerAloneCannotVerify(){val s=OpenAiLegacyChatStream(model){};s.event("[DONE]");assertTrue(runCatching{s.finish()}.isFailure)}
    @Test fun explicitStopCompletesAccumulatedText(){val s=OpenAiLegacyChatStream(model){};s.event(chunk("Actual "));s.event(chunk("answer","stop"));assertEquals("Actual answer",s.finish())}
    @Test fun mixedResponseIdsCannotBeCombined(){val s=OpenAiLegacyChatStream(model){};s.event(chunk());val r=JSONObject(chunk("other","stop")).put("id","chatcmpl-other");assertTrue(runCatching{s.event(r.toString())}.isFailure)}
    @Test fun truncatedTerminalEventFails(){val s=OpenAiLegacyChatStream(model){};s.event(chunk());assertTrue(runCatching{s.event(chunk("","length"))}.isFailure)}
    @Test fun emptyStoppedStreamCannotVerify(){val s=OpenAiLegacyChatStream(model){};s.event(chunk("","stop"));assertTrue(runCatching{s.finish()}.isFailure)}
    @Test fun oversizedDeltaCannotReachUi(){var calls=0;val s=OpenAiLegacyChatStream(model){calls++};assertTrue(runCatching{s.event(chunk("x".repeat(64001)))}.isFailure);assertEquals(0,calls)}
    @Test fun legacyProbePreservesChatEndpointAndSupportedParameters()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(MockResponse().setHeader("Content-Type","application/json").setBody(completed().toString()));assertEquals("READY",client(s).testConnection(AssistantSettings(provider="openai",model=model),"key").sample);val r=s.takeRequest();assertEquals("/v1/chat/completions",r.path);assertEquals(64,JSONObject(r.body.readUtf8()).getInt("max_tokens"))}}
    @Test fun legacyTransportRejectsStreamWithoutStop()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(MockResponse().setHeader("Content-Type","text/event-stream").setBody("data: ${chunk()}\n\n"));val e=runCatching{client(s).answer("Hello",AppLanguage.EN,"",emptyList(),{})}.exceptionOrNull() as ProviderException;assertEquals(ProviderFailureKind.INCOMPLETE,e.kind)}}
    @Test fun legacyTransportPreservesWorkingCompletedStream()=runBlocking<Unit>{MockWebServer().use{s->s.enqueue(MockResponse().setHeader("Content-Type","text/event-stream").setBody("data: ${chunk()}\n\ndata: ${chunk("","stop")}\n\n"));assertEquals("Answer",client(s).answer("Hello",AppLanguage.EN,"",emptyList(),{}));assertEquals("/v1/chat/completions",s.takeRequest().path)}}
}
