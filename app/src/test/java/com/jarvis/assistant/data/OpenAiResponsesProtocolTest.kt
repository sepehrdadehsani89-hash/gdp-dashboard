package com.jarvis.assistant.data

import com.jarvis.assistant.core.ChatMessage
import com.jarvis.assistant.core.MessageRole
import com.jarvis.assistant.brain.ProviderFailureKind
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

/** Documented Responses wire contracts, using local JSON fixtures, never live inference. */
class OpenAiResponsesProtocolTest {
    private val model="gpt-4.1-mini"
    private fun request(history:List<ChatMessage> = emptyList(),user:String="hello",context:String="")=JSONObject(OpenAiResponsesProtocol.request(model,"instructions",user,context,history))
    private fun response(text:String="READY",actual:String=model)=JSONObject().put("id","resp_fixture").put("object","response").put("status","completed").put("model",actual)
        .put("output",JSONArray().put(JSONObject().put("type","message").put("role","assistant").put("content",JSONArray().put(JSONObject().put("type","output_text").put("text",text)))))
    private fun reject(root:JSONObject){val e=runCatching{OpenAiResponsesProtocol.completed(root,model)}.exceptionOrNull();assertTrue(e is ProviderException);assertEquals(ProviderFailureKind.INCOMPLETE,(e as ProviderException).kind)}
    private fun streamEvent(type:String,root:JSONObject=response())=JSONObject().put("type",type).put("response",root).toString()
    @Test fun modernRequestUsesInputAndNoLegacyMessages(){val r=request();assertTrue(r.has("input"));assertFalse(r.has("messages"))}
    @Test fun modernRequestNeverSendsLegacyMaxTokens(){val r=request();assertFalse(r.has("max_tokens"));assertEquals(2048,r.getInt("max_output_tokens"))}
    @Test fun responsesAreExplicitlyNotStored(){assertFalse(request().getBoolean("store"))}
    @Test fun modernRequestOmitsUnsupportedSamplingDefaults(){val r=request();assertFalse(r.has("temperature"));assertFalse(r.has("top_p"))}
    @Test fun exactRequestedModelIsNeverReplaced(){assertEquals(model,request().getString("model"))}
    @Test fun statusHistoryNeverLeavesDevice(){val r=request(listOf(ChatMessage(role=MessageRole.STATUS,text="PRIVATE_STATUS")));assertFalse(r.toString().contains("PRIVATE_STATUS"))}
    @Test fun currentUserTurnIsSentOnce(){assertEquals(1,request(listOf(ChatMessage(role=MessageRole.USER,text="hello"))).getJSONArray("input").length())}
    @Test fun historyRetainsChronologyAndRoles(){val input=request(listOf(ChatMessage(role=MessageRole.USER,text="first"),ChatMessage(role=MessageRole.ASSISTANT,text="second"))).getJSONArray("input");assertEquals("first",input.getJSONObject(0).getString("content"));assertEquals("assistant",input.getJSONObject(1).getString("role"));assertEquals("hello",input.getJSONObject(2).getString("content"))}
    @Test fun historyIsBoundedByTurnsAndCharacters(){val input=request((1..30).map{ChatMessage(role=MessageRole.USER,text="x".repeat(9000))}).getJSONArray("input");assertTrue(input.length()<=13);assertTrue((0 until input.length()-1).sumOf{input.getJSONObject(it).getString("content").length}<=24000)}
    @Test fun externalContextHasExplicitUntrustedBoundary(){val text=request(context="reference").getJSONArray("input").getJSONObject(0).getString("content");assertTrue(text.contains("<untrusted_context_for_this_request>\nreference"))}
    @Test fun oversizedPromptAndContextAreBounded(){assertTrue(request(user="x".repeat(50000),context="y".repeat(50000)).toString().length<37000)}
    @Test fun invalidModelCannotBecomeAnEndpointPath(){assertTrue(runCatching{OpenAiResponsesProtocol.request("../../secret","","","",emptyList())}.isFailure)}
    @Test fun excessiveGenerationBudgetIsRejected(){assertTrue(runCatching{OpenAiResponsesProtocol.request(model,"","","",emptyList(),maxOutput=90000)}.isFailure)}
    @Test fun imagesRequireExplicitSelectedBytes(){val input=JSONObject(OpenAiResponsesProtocol.request(model,"rules","describe","",emptyList(),pictures=listOf(byteArrayOf(1,2)))).getJSONArray("input").getJSONObject(0).getJSONArray("content");assertEquals("input_image",input.getJSONObject(1).getString("type"));assertTrue(input.getJSONObject(1).getString("image_url").startsWith("data:image/jpeg;base64,"))}
    @Test fun tooManyImagesAreRejected(){assertTrue(runCatching{OpenAiResponsesProtocol.request(model,"","","",emptyList(),pictures=List(3){byteArrayOf(1)})}.isFailure)}
    @Test fun oversizedImageIsRejected(){assertTrue(runCatching{OpenAiResponsesProtocol.request(model,"","","",emptyList(),pictures=listOf(ByteArray(1500001)))}.isFailure)}
    @Test fun completedResponseReturnsActualTextAndResolvedModel(){val r=OpenAiResponsesProtocol.completed(response(),model);assertEquals("READY",r.text);assertEquals(model,r.model);assertEquals("resp_fixture",r.responseId)}
    @Test fun documentedDatedAliasCanResolveSameModel(){assertEquals("READY",OpenAiResponsesProtocol.completed(response(actual="$model-2025-04-14"),model).text)}
    @Test fun unrelatedModelCannotVerifySelectedModel(){reject(response(actual="gpt-unrelated"))}
    @Test fun pinnedSnapshotCannotResolveToDifferentSnapshot(){assertFalse(OpenAiResponsesProtocol.modelMatches("gpt-4.1-mini-2025-04-14","gpt-4.1-mini-2025-04-14-2026-01-01"))}
    @Test fun invalidCalendarDateIsNotAcceptedAsAlias(){reject(response(actual="$model-2025-02-30"))}
    @Test fun prefixCollisionIsNotAnAlias(){reject(response(actual="$model-other"))}
    @Test fun missingResponseIdentityCannotVerify(){reject(response().apply{remove("id")})}
    @Test fun wrongObjectCannotVerify(){reject(response().put("object","chat.completion"))}
    @Test fun queuedResponseCannotVerify(){reject(response().put("status","queued"))}
    @Test fun incompleteResponseCannotVerify(){reject(response().put("status","incomplete"))}
    @Test fun errorAttachedToCompletedResponseCannotVerify(){reject(response().put("error",JSONObject().put("code","server_error")))}
    @Test fun whitespaceAnswerCannotVerify(){reject(response(" \n "))}
    @Test fun toolCallAloneCannotCountAsAnswer(){reject(response().put("output",JSONArray().put(JSONObject().put("type","function_call").put("arguments","READY"))))}
    @Test fun userEchoCannotCountAsAssistantAnswer(){val r=response();r.getJSONArray("output").getJSONObject(0).put("role","user");reject(r)}
    @Test fun refusalIsAnHonestCompletedModelResponse(){val r=response();r.getJSONArray("output").getJSONObject(0).put("content",JSONArray().put(JSONObject().put("type","refusal").put("refusal","I cannot do that.")));assertEquals("I cannot do that.",OpenAiResponsesProtocol.completed(r,model).text)}
    @Test fun malformedJsonIsTypedAndRedacted(){val e=runCatching{OpenAiResponsesProtocol.completed("PRIVATE malformed",model)}.exceptionOrNull() as ProviderException;assertEquals(ProviderFailureKind.INCOMPLETE,e.kind);assertFalse(e.message!!.contains("PRIVATE"))}
    @Test fun oversizedCompletedTextFails(){reject(response("x".repeat(64001)))}
    @Test fun debugStringNeverContainsReplyText(){assertFalse(OpenAiResponsesProtocol.completed(response("PRIVATE"),model).toString().contains("PRIVATE"))}
    @Test fun deltasAreProvisionalUntilCompleted(){val chunks=mutableListOf<String>();val s=OpenAiResponseStream(model){chunks+=it};s.event("""{"type":"response.output_text.delta","delta":"partial"}""");assertEquals(listOf("partial"),chunks);assertTrue(runCatching{s.finish()}.isFailure)}
    @Test fun finalCompletedTextIsAuthoritative(){val s=OpenAiResponseStream(model){};s.event("""{"type":"response.output_text.delta","delta":"partial"}""");s.event(streamEvent("response.completed",response("final answer")));assertEquals("final answer",s.finish().text)}
    @Test fun doneMarkerWithoutCompletedResponseCannotVerify(){val s=OpenAiResponseStream(model){};s.event("[DONE]");assertTrue(runCatching{s.finish()}.isFailure)}
    @Test fun failedStreamCannotSucceedAfterPartialText(){val s=OpenAiResponseStream(model){};s.event("""{"type":"response.output_text.delta","delta":"partial"}""");val e=runCatching{s.event(streamEvent("response.failed",JSONObject().put("error",JSONObject().put("code","insufficient_quota"))))}.exceptionOrNull() as ProviderException;assertEquals(ProviderFailureKind.CREDITS,e.kind)}
    @Test fun incompleteTerminalEventFails(){val s=OpenAiResponseStream(model){};assertTrue(runCatching{s.event(streamEvent("response.incomplete"))}.isFailure)}
    @Test fun terminalResponseCanCompleteWithoutDeltas(){val s=OpenAiResponseStream(model){};s.event(streamEvent("response.completed"));assertEquals("READY",s.finish().text)}
    @Test fun oversizedDeltaCannotReachUi(){var calls=0;val s=OpenAiResponseStream(model){calls++};assertTrue(runCatching{s.event(JSONObject().put("type","response.output_text.delta").put("delta","x".repeat(64001)).toString())}.isFailure);assertEquals(0,calls)}
}
