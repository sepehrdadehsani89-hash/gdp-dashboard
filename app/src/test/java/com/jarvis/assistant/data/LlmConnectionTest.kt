package com.jarvis.assistant.data

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantSettings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import okhttp3.mockwebserver.SocketPolicy
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class LlmConnectionTest {
    private fun client(server: MockWebServer, key: String = "saved-test-key", settings: AssistantSettings = AssistantSettings(name = "PRIVATE NAME")): LlmClient {
        val localRoot = server.url("/")
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val original = chain.request()
            val local = localRoot.newBuilder().encodedPath(original.url.encodedPath).encodedQuery(original.url.encodedQuery).build()
            chain.proceed(original.newBuilder().url(local).build())
        }.build()
        return LlmClient({ settings }, { key }, http)
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

    @Test fun connectionTestUsesShortNonStreamingRequestAndReportsActualSample() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""{"candidates":[{"content":{"parts":[{"text":"Hello"}]}}]}"""))
            val settings = AssistantSettings(provider = "gemini", model = "models/gemini-text-test", name = "PRIVATE NAME")
            val result = client(server).testConnection(settings, "")
            assertEquals("gemini-text-test", result.model)
            assertEquals("Hello", result.sample)
            assertTrue(result.elapsedMs >= 0)
            val request = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("/v1beta/models/gemini-text-test:generateContent", request.path)
            assertEquals("saved-test-key", request.getHeader("x-goog-api-key"))
            val body = request.body.readUtf8()
            assertFalse(body.contains("PRIVATE NAME"))
            val contents = JSONObject(body).getJSONArray("contents")
            assertEquals(128, JSONObject(body).getJSONObject("generationConfig").getInt("maxOutputTokens"))
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun stalledProviderHasAFiniteDeadline() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val pending = async { runCatching { client(server).testConnection(AssistantSettings(provider = "gemini", model = "gemini-test"), "") } }
            runCurrent()
            assertNotNull(server.takeRequest(2, TimeUnit.SECONDS))
            advanceTimeBy(15_001); runCurrent()
            val failure = pending.await().exceptionOrNull()
            assertTrue(failure is ProviderException)
            assertTrue(failure!!.message!!.contains("15 seconds"))
        }
    }

    @Test fun emptyAnswerCannotVerifyAConnection() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"candidates":[]}"""))
            val empty = runCatching { client(server).testConnection(AssistantSettings(provider = "gemini", model = "gemini-test"), "") }.exceptionOrNull()
            assertTrue(empty is ProviderException)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun ordinaryReplyCannotWaitForeverForItsFirstText() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val pending = async { runCatching { client(server, settings = AssistantSettings(provider = "gemini", model = "gemini-test")).answer("Hello", AppLanguage.EN, "", emptyList(), {}) } }
            runCurrent()
            assertNotNull(server.takeRequest(2, TimeUnit.SECONDS))
            advanceTimeBy(12_001); runCurrent()
            val failure = pending.await().exceptionOrNull()
            assertTrue(failure is ProviderException)
            assertTrue(failure!!.message!!.contains("12 seconds"))
        }
    }

    @Test fun firstTextArrivesBeforeStreamEndsAndLockedRequestCanOmitSavedIdentity() = runBlocking {
        MockWebServer().use { server ->
            val event = "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"A useful first sentence is here. \"}]}}]}\n\n"
            // MockWebServer throttles request bodies too. Keep the request below one period,
            // while making the response long enough to prove delivery before its EOF.
            server.enqueue(MockResponse().setHeader("Content-Type", "text/event-stream").setBody(event + ": " + "x".repeat(32_000) + "\n\n").throttleBody(8192, 1, TimeUnit.SECONDS))
            val first = CompletableDeferred<String>()
            val provider = client(server, settings = AssistantSettings(provider = "gemini", model = "gemini-test", name = "PRIVATE NAME"))
            val pending = async { provider.answer("Hello", AppLanguage.EN, "", emptyList(), { first.complete(it) }, includeIdentity = false) }
            assertEquals("A useful first sentence is here. ", withTimeout(3000) { first.await() })
            assertFalse(pending.isCompleted)
            pending.cancelAndJoin()
            val request = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertFalse(request.body.readUtf8().contains("PRIVATE NAME"))
        }
    }
}
