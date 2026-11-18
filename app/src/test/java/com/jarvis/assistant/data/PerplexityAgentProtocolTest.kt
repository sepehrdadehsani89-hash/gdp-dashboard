package com.jarvis.assistant.data

import com.jarvis.assistant.brain.ProviderConfig
import com.jarvis.assistant.brain.ProviderCredentialMode
import com.jarvis.assistant.core.ChatMessage
import com.jarvis.assistant.core.MessageRole
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PerplexityAgentProtocolTest {
    private val model="vendor/current-model"
    private fun request(user:String="Hello",context:String="",history:List<ChatMessage> = emptyList())=JSONObject(PerplexityAgentProtocol.request(model,"Instructions",user,context,history))
    @Test fun explicitVendorModelCanBeSaved(){assertEquals(model,ProviderConfig("perplexity",model).validated().model)}
    @Test fun nonPerplexityProvidersStillRejectSlashIds(){assertThrows(IllegalArgumentException::class.java){ProviderConfig("openai",model).validated()}}
    @Test fun noChatGPTGrantCanAuthorizePerplexity(){assertThrows(IllegalArgumentException::class.java){ProviderConfig("perplexity",model,ProviderCredentialMode.CHATGPT_PLAN).validated()}}
    @Test fun blankModelCannotBeInvented(){assertThrows(IllegalArgumentException::class.java){PerplexityAgentProtocol.validateModel("")}}
    @Test fun legacySonarIdIsNotAssumedCurrent(){assertThrows(IllegalArgumentException::class.java){PerplexityAgentProtocol.validateModel("sonar")}}
    @Test fun modelCannotInjectPathOrQueries(){assertThrows(IllegalArgumentException::class.java){PerplexityAgentProtocol.validateModel("vendor/model?key=x")}}
    @Test fun modelCannotIncludeAHost(){assertThrows(IllegalArgumentException::class.java){PerplexityAgentProtocol.validateModel("https://evil.example/model")}}
    @Test fun controlsCannotEnterModel(){assertThrows(IllegalArgumentException::class.java){PerplexityAgentProtocol.validateModel("vendor/model\n")}}
    @Test fun responseStorageIsExplicitlyDisabled(){assertFalse(request().getBoolean("store"))}
    @Test fun noCloudToolsArePassed(){assertEquals(0,request().getJSONArray("tools").length())}
    @Test fun instructionsStaySeparateFromUserInput(){val r=request();assertEquals("Instructions",r.getString("instructions"));assertEquals("user",r.getJSONArray("input").getJSONObject(0).getString("role"))}
    @Test fun privateContextIsDeliberatelyMarkedUntrusted(){assertTrue(request(context="document").getJSONArray("input").toString().contains("untrusted_context_for_this_request"))}
    @Test fun duplicatedCurrentUserTurnIsNotSentTwice(){val r=request(history=listOf(ChatMessage(role=MessageRole.USER,text="Hello")));assertEquals(1,r.getJSONArray("input").length())}
    @Test fun historyRemainsBounded(){val r=request(history=(0..100).map{ChatMessage(role=MessageRole.USER,text="prior$it")});assertTrue(r.getJSONArray("input").length()<=13)}
    @Test fun catalogNeedsActualListShape(){assertThrows(ProviderException::class.java){PerplexityAgentProtocol.models("{}")}}
    @Test fun emptyCatalogDoesNotInventDefault(){assertThrows(ProviderException::class.java){PerplexityAgentProtocol.models("{\"object\":\"list\",\"data\":[]}")}}
    @Test fun malformedCatalogIsUnavailable(){assertThrows(ProviderException::class.java){PerplexityAgentProtocol.models("not json")}}
    @Test fun catalogFiltersUnsafeIds(){val r=PerplexityAgentProtocol.models("""{"object":"list","data":[{"object":"model","id":"vendor/good"},{"object":"model","id":"https://evil.example"}]}""");assertEquals(listOf("vendor/good"),r)}
    @Test fun catalogDoesNotAcceptChatMessagesAsModelRows(){assertThrows(ProviderException::class.java){PerplexityAgentProtocol.models("""{"object":"list","data":[{"object":"message","id":"vendor/model"}]}""")}}
    @Test fun catalogDeduplicatesWithoutSynthesizingModels(){assertEquals(listOf("vendor/a"),PerplexityAgentProtocol.models("""{"object":"list","data":[{"object":"model","id":"vendor/a"},{"object":"model","id":"vendor/a"}]}"""))}
    @Test fun perplexityCredentialStorageIsIndependentOfOpenAI(){assertEquals("key_perplexity",JarvisStore.credentialName("perplexity"));assertNotEquals(JarvisStore.credentialName("openai"),JarvisStore.credentialName("perplexity"))}
    @Test fun defaultOpenAIRequestValidationIsPreserved(){assertThrows(IllegalArgumentException::class.java){OpenAiResponsesProtocol.request(model,"Instructions","Hello","",emptyList())}}
}
