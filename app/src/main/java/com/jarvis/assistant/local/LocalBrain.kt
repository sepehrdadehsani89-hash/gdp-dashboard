package com.jarvis.assistant.local

import android.content.Context
import android.os.StatFs
import androidx.annotation.Keep
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Explicit-download, phone-resident inference. No user text is sent over the network. */
@Keep
class LocalBrain(context: Context) {
    private val directory = File(context.applicationContext.filesDir, "local-brain").apply { mkdirs() }
    private val modelFile = File(directory, MODEL_FILENAME)
    private val manifestFile = File(directory, "verified-model.json")
    private val inferenceMutex = Mutex()
    private val downloadMutex = Mutex()
    @Volatile private var handle: Long = 0L
    @Volatile private var downloadCancelled = false
    @Volatile private var activeDownload: okhttp3.Call? = null
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.MINUTES)
        .followSslRedirects(false)
        .build()

    @Keep
    interface TokenReceiver { @Keep fun onToken(bytes: ByteArray) }

    fun modelReady(): Boolean = runCatching {
        if (!modelFile.isFile || !manifestFile.isFile) return@runCatching false
        val metadata = JSONObject(manifestFile.readText())
        metadata.getString("repository") == MODEL_REPOSITORY &&
            metadata.getString("sha256").matches(Regex("[a-f0-9]{64}")) &&
            metadata.getString("revision").matches(Regex("[a-f0-9]{40}")) &&
            metadata.getLong("size") == modelFile.length() && modelFile.length() > 1_000_000
    }.getOrDefault(false)

    fun modelStatus(): String = if (modelReady()) {
        "Qwen2.5 0.5B Q4_K_M • on-device • ${modelFile.length() / (1024 * 1024)} MB"
    } else {
        "Download the optional on-device model (about 400 MB). No API key required."
    }

    suspend fun download(onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        downloadMutex.withLock {
            if (modelReady()) { onProgress(1f); return@withLock }
            downloadCancelled = false
            val temporary = File(directory, "$MODEL_FILENAME.part")
            val temporaryManifest = File(directory, "verified-model.json.part")
            val cancellationWatcher = CoroutineScope(currentCoroutineContext()).launch(
                Dispatchers.Default, start = CoroutineStart.UNDISPATCHED,
            ) {
                try { awaitCancellation() } finally { activeDownload?.cancel() }
            }
            try {
                if (temporary.exists()) check(temporary.delete()) { "Could not remove an interrupted model download." }
                temporaryManifest.delete()
                currentCoroutineContext().ensureActive()
                onProgress(0f)
                val metadataUrl = "https://huggingface.co/api/models/$MODEL_REPOSITORY/revision/main?blobs=true"
                val metadataText = fetchMetadata(metadataUrl)
                currentCoroutineContext().ensureActive()
                if (downloadCancelled) throw CancellationException("Model download cancelled")
                val descriptor = ModelDownloadPolicy.parseOfficialMetadata(metadataText)
                val revision = descriptor.revision
                val expectedSize = descriptor.size
                val free = StatFs(directory.absolutePath).availableBytes
                require(free > expectedSize + 150L * 1024 * 1024) { "Free at least 600 MB of phone storage and try again." }
                val url = "https://huggingface.co/$MODEL_REPOSITORY/resolve/$revision/$MODEL_FILENAME?download=true"
                val call = http.newCall(Request.Builder().url(url).build())
                activeDownload = call
                call.execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Model download failed (${response.code}). Try again on Wi-Fi.")
                    require(response.request.url.isHttps) { "Insecure model redirect refused." }
                    val body = response.body ?: throw IOException("Empty model response.")
                    val contentLength = body.contentLength()
                    require(contentLength < 0 || contentLength == expectedSize) { "Unexpected model response size." }
                    val digest = MessageDigest.getInstance("SHA-256")
                    var total = 0L
                    var previousPercent = -1
                    body.byteStream().use { input ->
                        temporary.outputStream().use { output ->
                            val buffer = ByteArray(128 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                if (downloadCancelled) throw CancellationException("Model download cancelled")
                                val count = input.read(buffer)
                                if (count < 0) break
                                total += count
                                require(total <= expectedSize) { "Model response exceeded verified size." }
                                output.write(buffer, 0, count)
                                digest.update(buffer, 0, count)
                                val percent = (100L * total / expectedSize).toInt()
                                if (percent != previousPercent) {
                                    previousPercent = percent
                                    onProgress((total.toFloat() / expectedSize).coerceIn(0f, 0.99f))
                                }
                            }
                            output.flush()
                            output.fd.sync()
                        }
                    }
                    val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
                    ModelDownloadPolicy.verifyDownloaded(descriptor, total, actualHash)
                    currentCoroutineContext().ensureActive()
                    if (downloadCancelled) throw CancellationException("Model download cancelled")
                    temporaryManifest.writeText(JSONObject().apply {
                        put("repository", MODEL_REPOSITORY)
                        put("revision", revision)
                        put("file", MODEL_FILENAME)
                        put("sha256", actualHash)
                        put("size", total)
                        put("license", "Apache-2.0 (official Qwen2.5-0.5B model)")
                    }.toString())
                    check(temporary.renameTo(modelFile)) { "Could not install verified model." }
                    check(temporaryManifest.renameTo(manifestFile)) { "Could not save model integrity record." }
                    onProgress(1f)
                }
            } finally {
                activeDownload = null
                temporary.delete()
                temporaryManifest.delete()
                cancellationWatcher.cancelAndJoin()
            }
        }
    }

    private fun fetchMetadata(url: String): String {
        val call = http.newCall(Request.Builder().url(url).build())
        activeDownload = call
        return call.execute().use { response ->
            if (!response.isSuccessful) throw IOException("Cannot verify the official model (${response.code}).")
            require(response.request.url.isHttps) { "Insecure model metadata redirect refused." }
            val body = response.body ?: throw IOException("Empty model metadata.")
            body.byteStream().use { input ->
                val data = input.readBytesLimited(2 * 1024 * 1024)
                data.toString(Charsets.UTF_8)
            }
        }
    }

    suspend fun answer(
        text: String,
        language: AppLanguage,
        history: List<ChatMessage>,
        onChunk: (String) -> Unit,
        concise:Boolean=true,
    ): String = withContext(Dispatchers.IO) {
        inferenceMutex.withLock {
            check(modelReady()) { "Download the on-device model in Settings first. No API key is needed." }
            check(nativeAvailable) { "The local AI runtime is unavailable on this phone architecture." }
            val prompt = LocalPromptBuilder.build(text, language, history, concise).toByteArray(Charsets.UTF_8)
            if (handle == 0L) handle = nativeCreate(modelFile.absolutePath)
            check(handle != 0L) { "Could not initialize the local AI runtime." }
            currentCoroutineContext().ensureActive()
            nativePrepare(handle)
            coroutineScope {
                val activeHandle = handle
                val cancellationWatcher = launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                    try { awaitCancellation() } finally { nativeCancel(activeHandle) }
                }
                try {
                    nativeGenerate(activeHandle, prompt, if(concise)256 else 512, object : TokenReceiver {
                        override fun onToken(bytes: ByteArray) {
                            onChunk(bytes.toString(Charsets.UTF_8))
                        }
                    }).toString(Charsets.UTF_8).trim().also {
                        check(it.isNotBlank()) { "The small local model returned no answer. Please try a shorter question." }
                    }
                } finally {
                    cancellationWatcher.cancelAndJoin()
                }
            }
        }
    }

    fun cancel() {
        downloadCancelled = true
        activeDownload?.cancel()
        val current = handle
        if (current != 0L && nativeAvailable) nativeCancel(current)
    }

    suspend fun release() = withContext(Dispatchers.IO) {
        cancel()
        inferenceMutex.withLock {
            val current = handle
            handle = 0L
            if (current != 0L && nativeAvailable) nativeDestroy(current)
        }
    }

    private external fun nativeCreate(path: String): Long
    private external fun nativePrepare(handle: Long)
    private external fun nativeGenerate(handle: Long, prompt: ByteArray, maxTokens: Int, receiver: TokenReceiver): ByteArray
    private external fun nativeCancel(handle: Long)
    private external fun nativeDestroy(handle: Long)

    companion object {
        const val MODEL_REPOSITORY = "Qwen/Qwen2.5-0.5B-Instruct-GGUF"
        const val MODEL_FILENAME = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
        private val nativeAvailable = runCatching { System.loadLibrary("jarvis-local"); true }.getOrDefault(false)
    }
}

private fun java.io.InputStream.readBytesLimited(maximum: Int): ByteArray {
    val result = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (true) {
        val count = read(buffer)
        if (count < 0) return result.toByteArray()
        require(result.size() + count <= maximum) { "Model metadata exceeded the size limit." }
        result.write(buffer, 0, count)
    }
}
