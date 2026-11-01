package com.jarvis.assistant.data

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantSettings
import com.jarvis.assistant.core.ChatMessage
import com.jarvis.assistant.core.choose
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

/** Network calls stay cancellable while streaming; no request or response body is logged. */
class LlmClient internal constructor(
    private val currentSettings: () -> AssistantSettings,
    private val savedKey: (String) -> String,
    private val http: OkHttpClient = defaultHttp(),
) {
    constructor(store: JarvisStore) : this({ store.settings.value }, store::getApiKey)

    suspend fun answer(
        userText: String,
        language: AppLanguage,
        context: String,
        history: List<ChatMessage>,
        onChunk: (String) -> Unit,
    ): String {
        val settings = currentSettings()
        val provider = settings.provider
        val key = savedKey(provider)
        if (key.isBlank()) throw MissingApiKeyException(language)
        val model = BrainProtocol.model(provider, settings.model)
        val body = BrainProtocol.requestBody(provider, model, BrainProtocol.systemPrompt(language, settings.name), userText, context, history)
        return stream(http.newCall(request(provider, model, key, body)), provider, language, onChunk)
    }

    private fun request(provider: String, model: String, key: String, body: String): Request {
        val builder = Request.Builder().post(body.toRequestBody(JSON))
            .header("Accept", "text/event-stream")
        when (provider) {
            "openai" -> builder.url("https://api.openai.com/v1/chat/completions").header("Authorization", "Bearer $key")
            "anthropic" -> builder.url("https://api.anthropic.com/v1/messages").header("x-api-key", key).header("anthropic-version", "2023-06-01")
            "gemini" -> builder.url("https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse").header("x-goog-api-key", key)
            else -> throw ProviderException("Select a supported AI provider in Settings.")
        }
        return builder.build()
    }

    /** Explicit Settings test: a canned prompt, with no history, files, memories or phone data. */
    suspend fun testConnection(settings: AssistantSettings, enteredKey: String): String {
        val key = connectionKey(settings.provider, enteredKey, settings.language)
        val model = BrainProtocol.model(settings.provider, settings.model)
        val body = BrainProtocol.requestBody(settings.provider, model, "Reply briefly to this connection test.", "Say hello.", "", emptyList())
        stream(http.newCall(request(settings.provider, model, key, body)), settings.provider, settings.language) {}
        return model
    }

    suspend fun availableGeminiModels(enteredKey: String, language: AppLanguage): List<String> {
        val key = connectionKey("gemini", enteredKey, language)
        val request = Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000")
            .header("x-goog-api-key", key).build()
        val body = json(http.newCall(request), language)
        val models = BrainProtocol.geminiModels(body)
        if (models.isEmpty()) throw ProviderException(language.choose("Google listed no compatible Gemini text models for this key. Check the project and API access in Google AI Studio.", "Google no mostró modelos de texto compatibles para esta clave. Revisa el proyecto y el acceso en Google AI Studio.", "گوگل برای این کلید مدل متنی سازگار فهرست نکرد. پروژه و دسترسی API را در Google AI Studio بررسی کنید."))
        return models
    }

    private fun connectionKey(provider: String, enteredKey: String, language: AppLanguage): String {
        val key = enteredKey.ifBlank { savedKey(provider) }
        if (key.isBlank()) throw MissingApiKeyException(language)
        return BrainProtocol.validateApiKey(key)
    }

    private suspend fun json(call: Call, language: AppLanguage): String = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(ProviderException(language.choose("Couldn't reach Google. Check your internet connection.", "No se pudo conectar con Google. Revisa tu conexión.", "اتصال به گوگل ممکن نشد. اینترنت را بررسی کنید.")))
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use {
                        if (!it.isSuccessful) throw ProviderException(BrainProtocol.providerHttpError(it.code, "gemini", it.peekBody(32_768).string(), language))
                        val content = it.body ?: throw ProviderException("Google returned no model list.")
                        val source = content.source()
                        if (source.request(1_000_001)) throw ProviderException("The model list exceeded the length limit.")
                        if (continuation.isActive) continuation.resume(source.readUtf8())
                    }
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(if (error is ProviderException) error else ProviderException(language.choose("Could not read Google's model list. Please try again.", "No se pudo leer la lista de modelos. Inténtalo otra vez.", "خواندن فهرست مدل‌های گوگل ممکن نشد. دوباره امتحان کنید.")))
                }
            }
        })
    }

    /** Optional explicit cloud voice. The text is sent only after opt-in and with an OpenAI key. */
    suspend fun synthesizeSpeech(text: String, language: AppLanguage): ByteArray {
        if (!currentSettings().useCloudVoice) throw ProviderException(language.choose("Cloud voice is disabled.", "La voz en la nube está desactivada.", "صدای ابری خاموش است."))
        val key = savedKey("openai")
        if (key.isBlank()) throw ProviderException(language.choose("Add an OpenAI key in Settings to use the optional cloud voice.", "Añade una clave de OpenAI en Ajustes para usar la voz en la nube.", "برای استفاده از صدای ابری، کلید OpenAI را در تنظیمات وارد کن."))
        val instructions = when (language) {
            AppLanguage.EN -> "Speak naturally in English with a calm and clear voice."
            AppLanguage.ES -> "Habla con claridad en español latinoamericano natural."
            AppLanguage.FA -> "به فارسی روان و طبیعی با لحنی آرام و واضح صحبت کن."
        }
        val body = JSONObject()
            .put("model", "gpt-4o-mini-tts")
            .put("voice", "alloy")
            .put("input", text.take(4000))
            .put("instructions", instructions)
            .put("response_format", "mp3")
            .toString()
        val request = Request.Builder().url("https://api.openai.com/v1/audio/speech")
            .header("Authorization", "Bearer $key")
            .post(body.toRequestBody(JSON)).build()
        return bytes(http.newCall(request), language)
    }

    private suspend fun stream(call: Call, provider: String, language: AppLanguage, onChunk: (String) -> Unit): String =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(ProviderException(language.choose("I couldn't reach the AI provider. Check your connection and try again.", "No pude conectar con el proveedor. Revisa tu conexión e inténtalo otra vez.", "نتوانستم به هوش مصنوعی وصل شوم. اینترنت را بررسی کن و دوباره امتحان کن.")))
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use {
                            if (!it.isSuccessful) throw ProviderException(BrainProtocol.providerHttpError(it.code, provider, it.peekBody(32_768).string(), language))
                            val content = it.body ?: throw ProviderException("The AI provider returned an empty response.")
                            val text = StringBuilder()
                            val decoder = SseDecoder { event ->
                                val delta = BrainProtocol.textDelta(provider, event)
                                if (delta.isNotEmpty()) {
                                    if (text.length + delta.length > 64_000) throw ProviderException("The AI response exceeded the length limit.")
                                    text.append(delta)
                                    if (continuation.isActive) onChunk(delta)
                                }
                            }
                            content.charStream().buffered().use { reader ->
                                decoder.consume(reader) { continuation.isActive }
                            }
                            val answer = text.toString().trim()
                            if (answer.isBlank()) throw ProviderException("The AI provider did not return a spoken answer. Try a different question or model.")
                            if (continuation.isActive) continuation.resume(answer)
                        }
                    } catch (error: Exception) {
                        val safe = if (error is ProviderException) error else ProviderException(language.choose("The AI provider response could not be read. Please try again.", "No pude leer la respuesta del proveedor. Inténtalo otra vez.", "پاسخ هوش مصنوعی قابل خواندن نبود. دوباره امتحان کن."))
                        if (continuation.isActive) continuation.resumeWithException(safe)
                    }
                }
            })
        }

    private suspend fun bytes(call: Call, language: AppLanguage): ByteArray = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(ProviderException(language.choose("The cloud voice could not be reached.", "No pude conectar con la voz en la nube.", "اتصال به صدای ابری ممکن نشد.")))
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use {
                        if (!it.isSuccessful) throw ProviderException(BrainProtocol.safeHttpError(it.code, language))
                        val body = it.body ?: throw ProviderException("The voice provider returned no audio.")
                        val length = body.contentLength()
                        if (length > 8_000_000) throw ProviderException("The voice response exceeded the safety limit.")
                        val stream = body.byteStream()
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (continuation.isActive) {
                            val count = stream.read(buffer)
                            if (count < 0) break
                            if (output.size() + count > 8_000_000) throw ProviderException("The voice response exceeded the safety limit.")
                            output.write(buffer, 0, count)
                        }
                        val bytes = output.toByteArray()
                        if (bytes.isEmpty() || bytes.size > 8_000_000) throw ProviderException("The voice response was empty or too large.")
                        if (continuation.isActive) continuation.resume(bytes)
                    }
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(if (error is ProviderException) error else ProviderException("The voice audio could not be read."))
                }
            }
        })
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private fun defaultHttp() = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }
}
