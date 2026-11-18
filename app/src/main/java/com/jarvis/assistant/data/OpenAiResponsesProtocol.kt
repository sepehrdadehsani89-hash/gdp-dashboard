package com.jarvis.assistant.data

import com.jarvis.assistant.brain.ProviderFailureKind
import com.jarvis.assistant.core.ChatMessage
import com.jarvis.assistant.core.MessageRole
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.Base64

/** API-key Responses contract. OAuth plan catalog/credentials remain a separate integration. */
internal object OpenAiResponsesProtocol {
    fun request(model:String,instructions:String,user:String,context:String,history:List<ChatMessage>,stream:Boolean=true,maxOutput:Int=2048,pictures:List<ByteArray> = emptyList()):String {
        OpenAiModelCatalog.validateId(model)
        require(maxOutput in 64..4096 && pictures.size<=2 && pictures.all{it.isNotEmpty() && it.size<=1_500_000})
        val recent=history.filter{it.role!=MessageRole.STATUS}.takeLast(12).toMutableList()
        if(recent.lastOrNull()?.let{it.role==MessageRole.USER && it.text==user}==true)recent.removeAt(recent.lastIndex)
        var budget=24000
        val bounded=recent.asReversed().mapNotNull{message->
            val text=message.text.take(minOf(6000,budget));budget-=text.length
            text.takeIf{it.isNotBlank()}?.let{message.copy(text=it)}
        }.asReversed()
        val prompt=user.take(12000)+if(context.isNotBlank())"\n\n<untrusted_context_for_this_request>\n"+context.take(24000)+"\n</untrusted_context_for_this_request>" else ""
        return JSONObject().put("model",model).put("store",false).put("stream",stream).put("instructions",instructions.take(12000)).put("max_output_tokens",maxOutput)
            .put("input",JSONArray().apply{
                bounded.forEach{m->put(JSONObject().put("role",if(m.role==MessageRole.USER)"user" else "assistant").put("content",m.text))}
                put(JSONObject().put("role","user").put("content",if(pictures.isEmpty())prompt else JSONArray().apply{
                    put(JSONObject().put("type","input_text").put("text",prompt))
                    pictures.forEach{put(JSONObject().put("type","input_image").put("image_url","data:image/jpeg;base64,"+Base64.getEncoder().encodeToString(it)))}
                }))
            }).toString()
    }
    fun modelMatches(requested:String,actual:String):Boolean {
        if(actual==requested)return true
        if(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}$").containsMatchIn(requested))return false
        if(!actual.startsWith("$requested-"))return false
        val date=actual.removePrefix("$requested-")
        return date.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) && runCatching{LocalDate.parse(date)}.isSuccess
    }
    fun completed(raw:String,requested:String):OpenAiCompletedResponse=try{completed(JSONObject(raw),requested)}catch(e:org.json.JSONException){throw incomplete()}
    fun completed(root:JSONObject,requested:String):OpenAiCompletedResponse {
        if(root.optString("object")!="response" || root.optString("status")!="completed" || (root.has("error") && !root.isNull("error")))throw incomplete()
        val id=root.opt("id") as? String ?: throw incomplete()
        val model=root.opt("model") as? String ?: throw incomplete()
        if(!id.matches(Regex("resp_[A-Za-z0-9_-]{1,160}")) || !modelMatches(requested,model))throw ProviderException("OpenAI returned an unexpected response or model. Nothing was verified.",ProviderFailureKind.INCOMPLETE)
        val output=root.optJSONArray("output") ?: throw incomplete();val text=StringBuilder()
        for(i in 0 until output.length()){
            val item=output.optJSONObject(i) ?: continue
            if(item.optString("type")!="message" || item.optString("role")!="assistant")continue
            val parts=item.optJSONArray("content") ?: continue
            for(j in 0 until parts.length()){
                val part=parts.optJSONObject(j) ?: continue
                val value=when(part.optString("type")){"output_text"->part.opt("text") as? String;"refusal"->part.opt("refusal") as? String;else->null} ?: continue
                if(text.length+value.length>64000)throw incomplete()
                text.append(value)
            }
        }
        val answer=text.toString().trim();if(answer.isBlank())throw incomplete()
        return OpenAiCompletedResponse(answer,model,id)
    }
    fun incomplete()=ProviderException("OpenAI did not return a completed, nonempty text response. No AI connection was verified. Check the selected model or retry the request.",ProviderFailureKind.INCOMPLETE)
}
internal class OpenAiCompletedResponse(val text:String,val model:String,val responseId:String){override fun toString()="OpenAiCompletedResponse(model=$model, text=redacted)"}

/** Provisional deltas never establish inference success; the final response is authoritative. */
internal class OpenAiResponseStream(private val model:String,private val onChunk:(String)->Unit){
    private var result:OpenAiCompletedResponse?=null
    private var characters=0
    val terminal get()=result!=null
    fun event(data:String){
        if(data.isBlank() || data=="[DONE]")return
        if(terminal)throw OpenAiResponsesProtocol.incomplete()
        val event=JSONObject(data)
        when(event.optString("type")){
            "response.output_text.delta"->{val delta=event.opt("delta") as? String ?: throw OpenAiResponsesProtocol.incomplete();characters+=delta.length;if(characters>64000)throw OpenAiResponsesProtocol.incomplete();if(delta.isNotEmpty())onChunk(delta)}
            "response.completed"->result=OpenAiResponsesProtocol.completed(event.optJSONObject("response") ?: throw OpenAiResponsesProtocol.incomplete(),model)
            "response.failed","error"->{val error=event.optJSONObject("response")?.optJSONObject("error") ?: event.optJSONObject("error") ?: event;throw httpFailure(200,"openai",JSONObject().put("error",error).toString(),com.jarvis.assistant.core.AppLanguage.EN)}
            "response.incomplete"->throw OpenAiResponsesProtocol.incomplete()
        }
    }
    fun finish()=result ?: throw OpenAiResponsesProtocol.incomplete()
}
