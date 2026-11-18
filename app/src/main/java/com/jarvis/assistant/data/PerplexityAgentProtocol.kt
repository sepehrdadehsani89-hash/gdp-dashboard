package com.jarvis.assistant.data

import com.jarvis.assistant.brain.ProviderFailureKind
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.ChatMessage
import org.json.JSONArray
import org.json.JSONObject

/** Official Agent / Responses contract. A public model listing never verifies paid account access. */
internal object PerplexityAgentProtocol {
    fun validateModel(model:String):String {require(model.length in 3..180 && model.matches(Regex("[A-Za-z0-9._:-]+/[A-Za-z0-9._:-]+"))){"Choose an exact vendor/model identifier from Perplexity's current public model catalog."};return model}
    fun models(raw:String):List<String> {
        val root=try{JSONObject(raw)}catch(_:Exception){throw ProviderException("Perplexity returned an invalid public model catalog.",ProviderFailureKind.MODEL_UNAVAILABLE)}
        if(root.optString("object")!="list")throw ProviderException("Perplexity returned no public model list.",ProviderFailureKind.MODEL_UNAVAILABLE)
        val rows=root.optJSONArray("data") ?: throw ProviderException("Perplexity returned no public model list.",ProviderFailureKind.MODEL_UNAVAILABLE)
        return (0 until minOf(rows.length(),1000)).mapNotNull{rows.optJSONObject(it)?.takeIf{it.optString("object")=="model"}?.optString("id")?.let{id->runCatching{validateModel(id)}.getOrNull()}}.distinct()
            .also{if(it.isEmpty())throw ProviderException("Perplexity listed no compatible Agent models. No model was invented or selected.",ProviderFailureKind.MODEL_UNAVAILABLE)}
    }
    fun request(model:String,instructions:String,user:String,context:String,history:List<ChatMessage>,stream:Boolean=true,maxOutput:Int=2048):String {
        val raw=OpenAiResponsesProtocol.request(model,instructions,user,context,history,stream,maxOutput,modelValidator={validateModel(it)})
        // No MCP servers, remote device tools, or implicit web tools are enabled by this adapter.
        return JSONObject(raw).put("tools",JSONArray()).toString()
    }
    fun completed(raw:String,model:String)=try{OpenAiResponsesProtocol.completed(raw,model)}catch(e:ProviderException){throw relabel(e)}
    fun relabel(error:ProviderException)=ProviderException(error.message?.replace("OpenAI","Perplexity") ?: "Perplexity did not verify an answer.",error.kind,error.httpStatus)
}
internal class PerplexityAgentStream(private val model:String,private val onChunk:(String)->Unit){
    private var result:OpenAiCompletedResponse?=null
    private var characters=0
    val terminal get()=result!=null
    fun event(data:String){if(data.isBlank() || data=="[DONE]")return;if(terminal)throw PerplexityAgentProtocol.relabel(OpenAiResponsesProtocol.incomplete())
        val event=JSONObject(data)
        when(event.optString("type")){
            "response.output_text.delta"->{val value=event.opt("delta") as? String ?: throw PerplexityAgentProtocol.relabel(OpenAiResponsesProtocol.incomplete());characters+=value.length;if(characters>64000)throw PerplexityAgentProtocol.relabel(OpenAiResponsesProtocol.incomplete());if(value.isNotEmpty())onChunk(value)}
            "response.completed"->result=PerplexityAgentProtocol.completed((event.optJSONObject("response") ?: throw PerplexityAgentProtocol.relabel(OpenAiResponsesProtocol.incomplete())).toString(),model)
            "response.failed","error"->{val failure=event.optJSONObject("response")?.optJSONObject("error") ?: event.optJSONObject("error") ?: event;throw httpFailure(200,"perplexity",JSONObject().put("error",failure).toString(),AppLanguage.EN)}
            "response.incomplete"->throw PerplexityAgentProtocol.relabel(OpenAiResponsesProtocol.incomplete())
        }
    }
    fun finish()=result ?: throw PerplexityAgentProtocol.relabel(OpenAiResponsesProtocol.incomplete())
}
