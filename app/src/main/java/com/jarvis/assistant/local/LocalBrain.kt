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
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Explicit-download, phone-resident inference. No user text is sent over the network. */
@Keep
class LocalBrain(private val context: Context,val preferences: com.jarvis.assistant.neural.NeuralPreferences = com.jarvis.assistant.neural.NeuralPreferences(context)) {
    val profile get()=com.jarvis.assistant.neural.BrainProfiles.get(preferences.state.value.profile)
    private val root=File(context.applicationContext.filesDir,"local-brain").apply{mkdirs()}
    private fun directoryFor(p:com.jarvis.assistant.neural.BrainProfile)=if(p.id=="lite")root else File(root,p.id).apply{mkdirs()}
    private val progressState=kotlinx.coroutines.flow.MutableStateFlow("Idle")
    val progress=progressState.asStateFlow()
    @Volatile private var loadedProfile=""
    @Volatile private var stopReason:String?=null
    @Volatile var lastMetrics:com.jarvis.assistant.neural.InferenceMetrics?=null;private set
    suspend fun inspectDownload(p:com.jarvis.assistant.neural.BrainProfile):ModelDownloadPolicy.Descriptor=withContext(Dispatchers.IO){
        require(p.available)
        ModelDownloadPolicy.parseOfficialMetadata(fetchMetadata("https://huggingface.co/api/models/${p.repository}/revision/main?blobs=true"),p)
    }
    suspend fun select(id:String){val next=com.jarvis.assistant.neural.BrainProfiles.get(id);require(next.available);release();preferences.save(preferences.state.value.copy(profile=next.id))}
    fun hardwareError(p:com.jarvis.assistant.neural.BrainProfile=profile):String? {
        val info=android.app.ActivityManager.MemoryInfo();context.getSystemService(android.app.ActivityManager::class.java).getMemoryInfo(info)
        return com.jarvis.assistant.neural.BrainProfiles.admission(p,info.totalMem/1048576,info.availMem/1048576,info.lowMemory,context.getSystemService(android.os.PowerManager::class.java).currentThermalStatus,android.os.Build.SUPPORTED_ABIS.contains("arm64-v8a"))
    }

    private val directory get()=directoryFor(profile)
    private val modelFile get()=File(directory,profile.file)
    private val manifestFile get()=File(directory,"verified-model.json")
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

    fun modelReady():Boolean=ready(profile)
    private fun ready(selected:com.jarvis.assistant.neural.BrainProfile): Boolean = runCatching {
        val modelFile=File(directoryFor(selected),selected.file);val manifestFile=File(directoryFor(selected),"verified-model.json")
        if (!modelFile.isFile || !manifestFile.isFile) return@runCatching false
        val metadata = JSONObject(manifestFile.readText())
        metadata.getString("repository") == selected.repository && metadata.optString("file",selected.file)==selected.file &&
            metadata.getString("sha256").matches(Regex("[a-f0-9]{64}")) &&
            metadata.getString("revision").matches(Regex("[a-f0-9]{40}")) &&
            metadata.getLong("size") == modelFile.length() && modelFile.length() > 1_000_000
    }.getOrDefault(false)

    fun modelStatus(): String = if (modelReady()) {
        "${profile.title} • Qwen2.5 Q4_K_M • ${modelFile.length()} bytes • ${profile.context} token runtime context"
    } else {
        "${profile.title} weights are not installed. Inspect exact size and license in Neural Core. No API key required."
    }

    suspend fun download(approved:ModelDownloadPolicy.Descriptor?=null,target:com.jarvis.assistant.neural.BrainProfile=profile,onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        downloadMutex.withLock {
            val selected=target
            val directory=directoryFor(selected);val modelFile=File(directory,selected.file);val manifestFile=File(directory,"verified-model.json")
            require(selected.available)
            if (ready(selected)) { onProgress(1f); return@withLock }
            downloadCancelled = false
            val temporary = File(directory, "${selected.file}.part")
            val temporaryManifest = File(directory, "verified-model.json.part")
            val cancellationWatcher = CoroutineScope(currentCoroutineContext()).launch(
                Dispatchers.Default, start = CoroutineStart.UNDISPATCHED,
            ) {
                try { awaitCancellation() } finally { activeDownload?.cancel() }
            }
            try {
                // Keep an interrupted, revision-bound partial download for a verified Range retry.
                temporaryManifest.delete()
                currentCoroutineContext().ensureActive()
                onProgress(0f)
                val metadataUrl = "https://huggingface.co/api/models/${selected.repository}/revision/main?blobs=true"
                val metadataText = fetchMetadata(metadataUrl)
                currentCoroutineContext().ensureActive()
                if (downloadCancelled) throw CancellationException("Model download cancelled")
                val descriptor = ModelDownloadPolicy.parseOfficialMetadata(metadataText,selected)
                if(approved!=null)require(descriptor==approved){"Model metadata changed. Review the new download details."}
                val revision = descriptor.revision
                val resumeRecord=File(directory,"${selected.file}.resume")
                val identity="$revision:${descriptor.sha256}:${descriptor.size}"
                if(!resumeRecord.isFile || resumeRecord.readText()!=identity || temporary.length()>descriptor.size){temporary.delete();resumeRecord.writeText(identity)}
                if(temporary.length()==descriptor.size)temporary.delete()
                val offset=temporary.length()
                val expectedSize = descriptor.size
                val free = StatFs(directory.absolutePath).availableBytes
                require(free > expectedSize - offset + 150L * 1024 * 1024) { "Free sufficient storage for the remaining verified download plus 150 MiB, then retry." }
                val url = "https://huggingface.co/${selected.repository}/resolve/$revision/${selected.file}?download=true"
                val call = http.newCall(Request.Builder().url(url).apply{if(offset>0)header("Range","bytes=$offset-")}.build())
                activeDownload = call
                call.execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Model download failed (${response.code}). Try again on Wi-Fi.")
                    require(response.request.url.isHttps) { "Insecure model redirect refused." }
                    val body = response.body ?: throw IOException("Empty model response.")
                    val initial=com.jarvis.assistant.neural.DownloadResumePolicy.initialBytes(offset,expectedSize,response.code,response.header("Content-Range"),body.contentLength())
                    val resumed=initial>0
                    val digest = MessageDigest.getInstance("SHA-256")
                    if(resumed)temporary.inputStream().use{input->val bytes=ByteArray(131072);while(true){currentCoroutineContext().ensureActive();val n=input.read(bytes);if(n<0)break;digest.update(bytes,0,n)}}
                    var total = initial
                    var previousPercent = -1
                    body.byteStream().use { input ->
                        java.io.FileOutputStream(temporary,resumed).use { output ->
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
                    try{ModelDownloadPolicy.verifyDownloaded(descriptor, total, actualHash)}catch(e:IllegalArgumentException){temporary.delete();resumeRecord.delete();throw e}
                    currentCoroutineContext().ensureActive()
                    if (downloadCancelled) throw CancellationException("Model download cancelled")
                    temporaryManifest.writeText(JSONObject().apply {
                        put("repository", selected.repository)
                        put("revision", revision)
                        put("file", selected.file)
                        put("sha256", actualHash)
                        put("size", total)
                        put("license", "Apache-2.0 (official Qwen2.5-0.5B model)")
                    }.toString())
                    check(temporary.renameTo(modelFile)) { "Could not install verified model." }
                    check(temporaryManifest.renameTo(manifestFile)) { "Could not save model integrity record." }
                    resumeRecord.delete()
                    onProgress(1f)
                }
            } finally {
                activeDownload = null
                temporaryManifest.delete()
                cancellationWatcher.cancelAndJoin()
            }
        }
    }

    private fun fetchMetadata(url: String): String {
        val call = http.newBuilder().callTimeout(30,TimeUnit.SECONDS).readTimeout(25,TimeUnit.SECONDS).build().newCall(Request.Builder().url(url).build())
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
        benchmark:Boolean=false,
        baselinePrompt:Boolean=false,
    ): String = withContext(Dispatchers.IO) {
        inferenceMutex.withLock {
            if(!modelReady())throw LocalModelUnavailable(profile.title)
            check(nativeAvailable) { "The local AI runtime is unavailable on this phone architecture." }
            hardwareError()?.let{error(it)}
            val selected=profile
            val started=android.os.SystemClock.elapsedRealtime()
            var firstToken:Long?=null
            var peakPss=android.os.Debug.getPss()
            stopReason=null
            progressState.value="Loading ${selected.title}"
            try {
            val prompt = (if(baselinePrompt)com.jarvis.assistant.neural.BaselinePromptBuilder.build(text,language,emptyList(),concise) else LocalPromptBuilder.build(text, language, history, concise)).toByteArray(Charsets.UTF_8)
            if(handle==0L){
                val expected=JSONObject(manifestFile.readText()).getString("sha256")
                val digest=MessageDigest.getInstance("SHA-256")
                modelFile.inputStream().use{input->val b=ByteArray(131072);while(true){currentCoroutineContext().ensureActive();val n=input.read(b);if(n<0)break;digest.update(b,0,n)}}
                require(digest.digest().joinToString(""){"%02x".format(it)}==expected){"Model integrity check failed. Keep the other profile and redownload this one."}
            }
            if(handle!=0L && loadedProfile!=selected.id){nativeDestroy(handle);handle=0L}
            if (handle == 0L){ handle = nativeCreate(modelFile.absolutePath,selected.context,if(preferences.state.value.eco)1 else 2);loadedProfile=selected.id}
            check(handle != 0L) { "Could not initialize the local AI runtime." }
            currentCoroutineContext().ensureActive()
            nativePrepare(handle)
            coroutineScope {
                val activeHandle = handle
                val cancellationWatcher = launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                    try { awaitCancellation() } finally { nativeCancel(activeHandle) }
                }
                val monitor=launch(Dispatchers.Default){
                    while(true){kotlinx.coroutines.delay(if(benchmark)500 else 2000)
                        if(benchmark)peakPss=maxOf(peakPss,android.os.Debug.getPss())
                        val info=android.app.ActivityManager.MemoryInfo();context.getSystemService(android.app.ActivityManager::class.java).getMemoryInfo(info)
                        if(info.lowMemory || context.getSystemService(android.os.PowerManager::class.java).currentThermalStatus>=3){stopReason="Local inference stopped for memory pressure or thermal protection.";nativeCancel(activeHandle);break}
                    }
                }
                try {
                    nativeGenerate(activeHandle, prompt, if(concise)256 else 512, object : TokenReceiver {
                        override fun onToken(bytes: ByteArray) {
                            if(firstToken==null)firstToken=android.os.SystemClock.elapsedRealtime()-started
                            progressState.value="Generating locally"
                            onChunk(bytes.toString(Charsets.UTF_8))
                        }
                    }).toString(Charsets.UTF_8).trim().also {
                        val stats=nativeStatistics(activeHandle)
                        lastMetrics=com.jarvis.assistant.neural.InferenceMetrics(selected.id,modelFile.length(),stats.getOrElse(0){0},stats.getOrElse(1){0},stats.getOrElse(2){0}/1000,firstToken,android.os.SystemClock.elapsedRealtime()-started,peakPss,context.getSystemService(android.os.PowerManager::class.java).currentThermalStatus,stats.getOrElse(4){0}/1000,mapOf("training_context_tokens" to stats.getOrElse(5){0},"vocabulary_size" to stats.getOrElse(6){0},"parameter_count" to stats.getOrElse(7){0},"tensor_bytes" to stats.getOrElse(8){0},"llama_vocab_type" to stats.getOrElse(9){0}))
                        if(benchmark)File(root,"benchmark-last.json").writeText(lastMetrics!!.json())
                        check(it.isNotBlank()) { "The small local model returned no answer. Please try a shorter question." }
                    }
                } catch(e:java.util.concurrent.CancellationException){
                    if(stopReason!=null)throw IllegalStateException(stopReason)
                    throw e
                } finally {
                    monitor.cancelAndJoin()
                    progressState.value="Idle"
                    cancellationWatcher.cancelAndJoin()
                }
            }
            } finally {progressState.value="Idle"}
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

    private external fun nativeCreate(path: String, contextTokens:Int,threads:Int): Long
    private external fun nativeStatistics(handle:Long):LongArray
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

class LocalModelUnavailable(profile:String):IllegalStateException("Install $profile through Neural Core first; no API key is required.")
