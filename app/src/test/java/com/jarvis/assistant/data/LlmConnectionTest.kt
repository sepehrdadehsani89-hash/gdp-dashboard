package com.jarvis.assistant.data

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantSettings
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class LlmConnectionTest {
    private fun client(server: MockWebServer, key: String = "saved-test-key"): LlmClient {
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val original = chain.request()
            val local = server.url(original.url.encodedPath).newBuilder().encodedQuery(original.url.encodedQuery).build()
            chain.proceed(original.newBuilder().url(local).build())
        }.build()
        return LlmClient({ AssistantSettings(name = "PRIVATE NAME") }, { key }, http)
    }

    @Test fun modelLookupUsesHeaderAndKeepsOnlyCompatibleModels() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"models":[{"name":"models/gemini-text-test","supportedGenerationMethods":["generateContent"]},{"name":"models/gemini-live-test","supportedGenerationMethods":["bidiGenerateContent"]}]}"""))
            assertEquals(listOf("gemini-text-test"), client(server).availableGeminiModels("entered-test-key", AppLanguage.EN))
            val request = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("/v1beta/models?pageSize=1000", request.path)
            assertEquals("entered-test-key", request.getHeader("x-goog-api-key"))
            assertFalse(request.path!!.contains("test-key"))
        }
    }

    @Test fun connectionTestUsesSameStreamingEndpointAndOnlyCannedPrompt() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setHeader("Content-Type", "text/event-stream").setBody("data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello\"}]}}]}\n\n"))
            val settings = AssistantSettings(provider = "gemini", model = "models/gemini-text-test", name = "PRIVATE NAME")
            assertEquals("gemini-text-test", client(server).testConnection(settings, ""))
            val request = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("/v1beta/models/gemini-text-test:streamGenerateContent?alt=sse", request.path)
            assertEquals("saved-test-key", request.getHeader("x-goog-api-key"))
            val body = request.body.readUtf8()
            assertFalse(body.contains("PRIVATE NAME"))
            val contents = JSONObject(body).getJSONArray("contents")
            assertEquals(1, contents.length())
            assertEquals("Say hello.", contents.getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text"))
        }
    }

    @Test fun missingModelProducesActionableErrorAndDoesNotReportSuccessfulTest() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":{"status":"NOT_FOUND","message":"secret-test-key"}}"""))
            val failure = runCatching { client(server).testConnection(AssistantSettings(provider = "gemini", model = "gemini-unknown"), "") }.exceptionOrNull()
            assertTrue(failure is ProviderException)
            assertTrue(failure!!.message!!.contains("Find Gemini models"))
            assertFalse(failure.message!!.contains("secret-test-key"))
        }
    }

    @Test fun modelLookupReportsKeyRestrictionsAndDoesNotTreatEmptyCatalogAsConnected() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(403).setBody("""{"error":{"status":"PERMISSION_DENIED","details":[{"reason":"API_KEY_ANDROID_APP_BLOCKED"}]}}"""))
            val denied = runCatching { client(server).availableGeminiModels("", AppLanguage.EN) }.exceptionOrNull()
            assertTrue(denied is ProviderException)
            assertTrue(denied!!.message!!.contains("restrictions"))
            server.enqueue(MockResponse().setBody("""{"models":[]}"""))
            assertTrue(runCatching { client(server).availableGeminiModels("", AppLanguage.EN) }.exceptionOrNull() is ProviderException)
        }
    }
}
