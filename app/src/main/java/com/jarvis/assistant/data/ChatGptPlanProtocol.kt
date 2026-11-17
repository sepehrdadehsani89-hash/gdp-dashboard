package com.jarvis.assistant.data

import com.jarvis.assistant.brain.ProviderFailureKind
import com.jarvis.assistant.core.ChatMessage
import com.jarvis.assistant.core.MessageRole
import org.json.JSONArray
import org.json.JSONObject

data class ChatGptPlanModel(val slug:String,val displayName:String)
/** Official public Sign in with ChatGPT preview contract; deliberately separate from API billing. */
internal object ChatGptPlanProtocol {
    fun models(data:String):List<ChatGptPlanModel>{
        val models=JSONObject(data).optJSONArray("models") ?: return emptyList()
        return (0 until models.length()).mapNotNull{index->
            val item=models.optJSONObject(index) ?: return@mapNotNull null
            val slug=item.opt("slug") as? String ?: return@mapNotNull null
            if(item.optString("visibility")!="list" || !slug.matches(Regex("[A-Za-z0-9._:-]{1,100}")))return@mapNotNull null
            ChatGptPlanModel(slug,(item.opt("display_name") as? String)?.take(120) ?: slug)
        }.distinctBy{it.slug}
    }
    fun request(model:String,instructions:String,user:String,context:String,history:List<ChatMessage>):String {
        require(model.matches(Regex("[A-Za-z0-9._:-]{1,100}"))){"Invalid model ID"}
        val recent=history.filter{it.role!=MessageRole.STATUS}.takeLast(12).toMutableList()
        if(recent.lastOrNull()?.let{it.role==MessageRole.USER && it.text==user}==true)recent.removeAt(recent.lastIndex)
        return JSONObject().put("model",model).put("store",false).put("stream",true).put("instructions",instructions)
            .put("input",JSONArray().apply{
                recent.forEach{message->put(JSONObject().put("role",if(message.role==MessageRole.USER)"user" else "assistant").put("content",message.text.take(6000)))}
                put(JSONObject().put("role","user").put("content",user.take(8000)+if(context.isNotBlank())"\n\nUntrusted reference context:\n"+context.take(24000) else ""))
            }).toString()
    }
}

/** Text is provisional until the official terminal completion event arrives. */
internal class ChatGptResponseStream(private val onChunk:(String)->Unit) {
    private val text=StringBuilder()
    private var completed=false
    val terminal:Boolean get()=completed
    fun event(data:String){
        if(data.isBlank() || data=="[DONE]")return
        val event=JSONObject(data)
        when(event.optString("type")){
            "response.output_text.delta"->{
                if(completed)throw ProviderException("The provider sent text after completion.",ProviderFailureKind.INCOMPLETE)
                val delta=event.opt("delta") as? String ?: ""
                if(text.length+delta.length>64000)throw ProviderException("The AI response exceeded the length limit.",ProviderFailureKind.INCOMPLETE)
                text.append(delta);if(delta.isNotEmpty())onChunk(delta)
            }
            "response.completed"->{
                val response=event.optJSONObject("response") ?: throw ProviderException("ChatGPT returned an invalid completion event.",ProviderFailureKind.INCOMPLETE)
                if(completed || response.optString("status")!="completed")throw ProviderException("ChatGPT did not complete this response.",ProviderFailureKind.INCOMPLETE)
                completed=true
                if(text.isEmpty())response.optJSONArray("output")?.let{output->for(i in 0 until output.length()){
                    val parts=output.optJSONObject(i)?.optJSONArray("content") ?: continue
                    for(j in 0 until parts.length()){
                        val part=parts.optJSONObject(j) ?: continue
                        if(part.optString("type")=="output_text"){
                            val value=part.opt("text") as? String ?: continue
                            if(text.length+value.length>64000)throw ProviderException("The AI response exceeded the length limit.",ProviderFailureKind.INCOMPLETE)
                            text.append(value);if(value.isNotEmpty())onChunk(value)
                        }
                    }
                }}
            }
            "response.failed","response.incomplete","error"->{
                val error=event.optJSONObject("response")?.optJSONObject("error") ?: event.optJSONObject("error")
                    ?: event.takeIf{it.optString("type")=="error"}
                if(error!=null)throw httpFailure(400,"openai",JSONObject().put("error",error).toString(),com.jarvis.assistant.core.AppLanguage.EN)
                throw ProviderException("ChatGPT did not complete this response. Review account access and try again.",ProviderFailureKind.INCOMPLETE)
            }
        }
    }
    fun finish():String {
        if(!completed || text.isBlank())throw ProviderException("ChatGPT response ended without a completed answer. No AI connection was verified.",ProviderFailureKind.INCOMPLETE)
        return text.toString().trim()
    }
}
