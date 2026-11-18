package com.jarvis.assistant.data

import com.jarvis.assistant.brain.ProviderFailureKind
import com.jarvis.assistant.core.AppLanguage
import org.json.JSONObject

/** Preserve older GPT-3.5/GPT-4 endpoints without accepting unfinished output as an answer. */
internal object OpenAiLegacyChatProtocol {
    private fun modelMatches(requested:String,actual:String):Boolean {
        if(OpenAiResponsesProtocol.modelMatches(requested,actual))return true
        if(requested=="gpt-4-turbo-preview" && actual in setOf("gpt-4-0125-preview","gpt-4-1106-preview"))return true
        if(requested !in setOf("gpt-4","gpt-4-32k","gpt-3.5-turbo","gpt-3.5-turbo-16k") || !actual.startsWith("$requested-"))return false
        val suffix=actual.removePrefix("$requested-")
        return suffix.matches(Regex("[0-9]{4}")) && runCatching{java.time.MonthDay.parse("--"+suffix.take(2)+"-"+suffix.takeLast(2))}.isSuccess
    }
    fun validate(root:JSONObject,model:String,stream:Boolean){
        val expected=if(stream)"chat.completion.chunk" else "chat.completion"
        val actual=root.opt("model") as? String ?: throw incomplete()
        val id=root.opt("id") as? String ?: throw incomplete()
        if(root.optString("object")!=expected || !id.matches(Regex("chatcmpl[-_][A-Za-z0-9_-]{1,160}")) || !modelMatches(model,actual))throw incomplete()
    }
    fun completed(raw:String,model:String):OpenAiCompletedResponse=try{
        val root=JSONObject(raw);validate(root,model,false)
        val choice=root.optJSONArray("choices")?.optJSONObject(0) ?: throw incomplete()
        if(choice.optString("finish_reason")!="stop")throw incomplete()
        val message=choice.optJSONObject("message") ?: throw incomplete()
        if(message.optString("role")!="assistant")throw incomplete()
        val answer=((message.opt("content") as? String) ?: (message.opt("refusal") as? String)).orEmpty().trim()
        if(answer.isEmpty() || answer.length>64000)throw incomplete()
        OpenAiCompletedResponse(answer,root.getString("model"),root.getString("id"))
    }catch(_:org.json.JSONException){throw incomplete()}
    fun incomplete()=ProviderException("OpenAI did not complete the legacy text response. No AI connection was verified; retry or choose a listed modern text model.",ProviderFailureKind.INCOMPLETE)
}
internal class OpenAiLegacyChatStream(private val model:String,private val onChunk:(String)->Unit){
    private val text=StringBuilder()
    private var responseId:String?=null
    private var resolvedModel:String?=null
    var terminal=false;private set
    fun event(raw:String){
        if(raw.isBlank() || raw=="[DONE]")return
        if(terminal)throw OpenAiLegacyChatProtocol.incomplete()
        try{
            val root=JSONObject(raw)
            if(root.has("error"))throw httpFailure(200,"openai",raw,AppLanguage.EN)
            OpenAiLegacyChatProtocol.validate(root,model,true)
            val id=root.getString("id");val actual=root.getString("model")
            if(responseId!=null && (id!=responseId || actual!=resolvedModel))throw OpenAiLegacyChatProtocol.incomplete()
            responseId=id;resolvedModel=actual
            val choices=root.optJSONArray("choices") ?: throw OpenAiLegacyChatProtocol.incomplete()
            if(choices.length()==0)return // Optional usage-only chunk; no completion proof.
            val choice=choices.optJSONObject(0) ?: throw OpenAiLegacyChatProtocol.incomplete()
            val delta=choice.optJSONObject("delta")
            val value=(delta?.opt("content") as? String) ?: (delta?.opt("refusal") as? String) ?: ""
            if(text.length+value.length>64000)throw OpenAiLegacyChatProtocol.incomplete()
            if(value.isNotEmpty()){text.append(value);onChunk(value)}
            if(!choice.isNull("finish_reason")){
                if(choice.optString("finish_reason")!="stop")throw OpenAiLegacyChatProtocol.incomplete()
                terminal=true
            }
        }catch(_:org.json.JSONException){throw OpenAiLegacyChatProtocol.incomplete()}
    }
    fun finish():String {if(!terminal || text.isBlank())throw OpenAiLegacyChatProtocol.incomplete();return text.toString().trim()}
}
