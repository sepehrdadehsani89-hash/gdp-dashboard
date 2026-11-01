package com.jarvis.assistant.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.jarvis.assistant.core.AppLanguage
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Real neural speech on the phone, using the pinned sherpa-onnx native runtime. */
class OfflineVoiceManager(context: Context) {
    private val root = File(context.filesDir, "voice-models").apply { mkdirs() }
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).callTimeout(15, TimeUnit.MINUTES).build()
    private val downloadMutex = Mutex()
    private val main = Handler(Looper.getMainLooper())
    private val serial = Executors.newSingleThreadExecutor { Thread(it, "JarvisNeuralVoice").apply { isDaemon = true } }
    private val generation = AtomicLong()
    @Volatile private var track: AudioTrack? = null
    private var nativeVoice: OfflineTts? = null
    private var loadedLanguage: AppLanguage? = null

    data class VoiceSpec(val archiveName: String, val modelName: String, val sha256: String, val approximateMegabytes: Int, val datasetLicense: String) {
        val url get() = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/$archiveName.tar.bz2"
    }

    fun spec(language: AppLanguage) = when (language) {
        AppLanguage.EN -> VoiceSpec("vits-piper-en_US-ljspeech-medium", "en_US-ljspeech-medium.onnx", "3dfb4b759d8be032a4903a9538d128b0fda2a06ab1de6cbc2d93a97e2dd83dba", 65, "Public domain (LJ Speech)")
        AppLanguage.ES -> VoiceSpec("vits-piper-es_ES-davefx-medium", "es_ES-davefx-medium.onnx", "a3f6beb54a9cb893279f72978a22f807a4d9fc9c7848157b524d5cc7b7f58b22", 65, "CC0 (DaveFX)")
        AppLanguage.FA -> VoiceSpec("vits-piper-fa_IR-amir-medium", "fa_IR-amir-medium.onnx", "46af65c1cdd86ec300db3998f2bf31a68d9e38da930c0655c1ff6f610e84f864", 65, "CC0 (Amir)")
    }

    fun modelPath(language: AppLanguage): File? {
        val spec = spec(language)
        val directory = File(root, spec.archiveName)
        return directory.takeIf {
            File(it, ".complete").isFile && File(it, spec.modelName).isFile && File(it, "tokens.txt").isFile && File(it, "espeak-ng-data").isDirectory
        }
    }

    suspend fun download(language: AppLanguage, onProgress: (Float) -> Unit): File = downloadMutex.withLock {
        withContext(Dispatchers.IO) {
            modelPath(language)?.let { onProgress(1f); return@withContext it }
            require(root.usableSpace > 700L * 1024 * 1024) { "At least 700 MB of free storage is needed for a neural voice." }
            val spec = spec(language)
            val archive = File(root, ".${spec.archiveName}.download")
            val stage = File(root, ".${spec.archiveName}.extract")
            archive.delete(); stage.deleteRecursively(); stage.mkdirs()
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                client.newCall(Request.Builder().url(spec.url).build()).execute().use { response ->
                    check(response.isSuccessful) { "Voice download failed (HTTP ${response.code})." }
                    val body = checkNotNull(response.body) { "Voice download is empty." }
                    val length = body.contentLength()
                    require(length <= 180L * 1024 * 1024) { "Voice archive is unexpectedly large." }
                    var received = 0L
                    body.byteStream().use { input ->
                        archive.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                received += count
                                require(received <= 180L * 1024 * 1024) { "Voice archive exceeded its limit." }
                                digest.update(buffer, 0, count); output.write(buffer, 0, count)
                                if (length > 0) onProgress(received.toFloat() / length * 0.85f)
                            }
                        }
                    }
                    if (length >= 0) check(received == length) { "Voice download was incomplete." }
                }
                val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
                check(actualHash == spec.sha256) { "Voice integrity check failed. No model was installed." }
                var expanded = 0L
                var count = 0
                val canonicalRoot = stage.canonicalPath + File.separator
                BZip2CompressorInputStream(archive.inputStream().buffered()).use { bzip ->
                    TarArchiveInputStream(bzip).use { tar ->
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            @Suppress("DEPRECATION") val entry = tar.nextTarEntry ?: break
                            require(++count <= 6000) { "Voice archive has too many entries." }
                            require(!entry.isSymbolicLink && !entry.isLink) { "Voice archive contains an unsupported link." }
                            val target = File(stage, entry.name)
                            require(target.canonicalPath.startsWith(canonicalRoot)) { "Unsafe voice archive path." }
                            if (entry.isDirectory) target.mkdirs() else {
                                require(entry.isFile) { "Voice archive contains an unsupported file." }
                                target.parentFile?.mkdirs()
                                target.outputStream().use { output ->
                                    val buffer = ByteArray(64 * 1024)
                                    while (true) {
                                        currentCoroutineContext().ensureActive()
                                        val read = tar.read(buffer)
                                        if (read < 0) break
                                        expanded += read
                                        require(expanded <= 900L * 1024 * 1024) { "Voice expansion exceeded its limit." }
                                        output.write(buffer, 0, read)
                                    }
                                }
                            }
                        }
                    }
                }
                val extracted = File(stage, spec.archiveName)
                check(File(extracted, spec.modelName).isFile && File(extracted, "tokens.txt").isFile && File(extracted, "espeak-ng-data").isDirectory) { "Voice archive does not match its manifest." }
                val destination = File(root, spec.archiveName)
                destination.deleteRecursively()
                check(extracted.renameTo(destination)) { "Unable to install neural voice." }
                File(destination, ".complete").writeText("source=${spec.url}\nsha256=${spec.sha256}\ndataset=${spec.datasetLicense}\n")
                onProgress(1f)
                destination
            } finally { archive.delete(); stage.deleteRecursively() }
        }
    }

    fun speak(text: String, language: AppLanguage, onDone: () -> Unit, onError: (String) -> Unit) {
        stop()
        val token = generation.get()
        val directory = modelPath(language)
        if (directory == null) { onError("Download the ${language.label} offline voice first."); return }
        serial.execute {
            try {
                if (loadedLanguage != language || nativeVoice == null) {
                    nativeVoice?.release(); nativeVoice = null
                    val spec = spec(language)
                    val config = OfflineTtsConfig(
                        model = OfflineTtsModelConfig(
                            vits = OfflineTtsVitsModelConfig(
                                model = File(directory, spec.modelName).absolutePath,
                                tokens = File(directory, "tokens.txt").absolutePath,
                                dataDir = File(directory, "espeak-ng-data").absolutePath,
                            ),
                            numThreads = 2,
                            debug = false,
                            provider = "cpu",
                        ),
                        maxNumSentences = 1,
                    )
                    nativeVoice = OfflineTts(config = config)
                    loadedLanguage = language
                }
                if (token != generation.get()) return@execute
                val voice = checkNotNull(nativeVoice)
                val rate = voice.sampleRate()
                val minimum = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
                check(minimum > 0) { "Audio output does not support the voice sample rate." }
                val audio = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_FLOAT).build())
                    .setBufferSizeInBytes(maxOf(minimum * 2, 8192))
                    .setTransferMode(AudioTrack.MODE_STREAM).build()
                track = audio
                try {
                    check(audio.state == AudioTrack.STATE_INITIALIZED) { "Audio output could not initialize." }
                    audio.play()
                    var writtenSamples = 0L
                    voice.generateWithCallback(text.take(6000), sid = 0, speed = 1f) { samples ->
                        var offset = 0
                        while (offset < samples.size && token == generation.get()) {
                            val written = audio.write(samples, offset, samples.size - offset, AudioTrack.WRITE_BLOCKING)
                            if (written <= 0) return@generateWithCallback 0
                            offset += written; writtenSamples += written
                        }
                        if (token == generation.get()) 1 else 0
                    }
                    val drainDeadline = System.currentTimeMillis() + 10_000
                    while (token == generation.get() && audio.playbackHeadPosition.toLong() < writtenSamples && System.currentTimeMillis() < drainDeadline) Thread.sleep(20)
                    if (token == generation.get()) main.post { if (token == generation.get()) onDone() }
                } finally { if (track === audio) track = null; runCatching { audio.stop() }; audio.release() }
            } catch (_: Throwable) {
                if (token == generation.get()) main.post { if (token == generation.get()) onError("Offline voice could not run on this device. Check the model and available memory.") }
            }
        }
    }

    fun stop() { generation.incrementAndGet(); runCatching { track?.pause() }; runCatching { track?.flush() }; runCatching { track?.stop() } }
    fun shutdown() { stop(); serial.execute { nativeVoice?.release(); nativeVoice = null }; serial.shutdown() }
}
