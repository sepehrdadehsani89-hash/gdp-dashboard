package com.jarvis.assistant.voice

import android.content.Context
import com.jarvis.assistant.core.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/** Official Apache-2.0 Vosk small models. Catalog verified against alphacep/vosk-space/models.md. */
class WakeModelManager(context: Context) {
    private val root = File(context.filesDir, "wake-models").apply { mkdirs() }
    private val mutex = Mutex()
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).callTimeout(15, TimeUnit.MINUTES).build()

    data class ModelSpec(val name: String, val approximateMegabytes: Int) {
        val url get() = "https://alphacephei.com/vosk/models/$name.zip"
    }

    fun spec(language: AppLanguage): ModelSpec = when (language) {
        AppLanguage.EN -> ModelSpec("vosk-model-small-en-us-0.15", 40)
        AppLanguage.ES -> ModelSpec("vosk-model-small-es-0.42", 39)
        AppLanguage.FA -> ModelSpec("vosk-model-small-fa-0.42", 53)
    }

    fun modelPath(language: AppLanguage): File? {
        if(language !in AppLanguage.supported)return null
        val model = File(root, spec(language).name)
        return model.takeIf { File(it, ".complete").isFile && File(it, "am/final.mdl").isFile && File(it, "conf/mfcc.conf").isFile }
    }

    suspend fun download(language: AppLanguage, onProgress: (Float) -> Unit): File = mutex.withLock {
        require(language in AppLanguage.supported)
        withContext(Dispatchers.IO) {
            modelPath(language)?.let { onProgress(1f); return@withContext it }
            require(root.usableSpace > 600L * 1024 * 1024) { "At least 600 MB of free storage is needed for the offline model." }
            val spec = spec(language)
            val archive = File(root, ".${spec.name}.download")
            val stage = File(root, ".${spec.name}.extract")
            archive.delete(); stage.deleteRecursively(); stage.mkdirs()
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                client.newCall(Request.Builder().url(spec.url).build()).execute().use { response ->
                    check(response.isSuccessful) { "Model download failed (HTTP ${response.code})." }
                    val body = checkNotNull(response.body) { "Model download was empty." }
                    val expectedSize = body.contentLength()
                    require(expectedSize <= MAX_ARCHIVE_BYTES) { "Model archive is unexpectedly large." }
                    var transferred = 0L
                    body.byteStream().use { input ->
                        FileOutputStream(archive).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                transferred += count
                                require(transferred <= MAX_ARCHIVE_BYTES) { "Model download exceeded its limit." }
                                digest.update(buffer, 0, count); output.write(buffer, 0, count)
                                if (expectedSize > 0) onProgress((transferred.toFloat() / expectedSize).coerceIn(0f, 1f) * 0.85f)
                            }
                        }
                    }
                    if (expectedSize >= 0) check(transferred == expectedSize) { "Model download was incomplete." }
                }
                var expanded = 0L
                var entries = 0
                val canonicalRoot = stage.canonicalPath + File.separator
                ZipInputStream(archive.inputStream().buffered()).use { zip ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val entry = zip.nextEntry ?: break
                        check(++entries <= 5000) { "Model archive has too many entries." }
                        val target = File(stage, entry.name)
                        check(target.canonicalPath.startsWith(canonicalRoot)) { "Unsafe model archive path." }
                        if (entry.isDirectory) target.mkdirs() else {
                            target.parentFile?.mkdirs()
                            target.outputStream().use { output ->
                                val buffer = ByteArray(64 * 1024)
                                while (true) {
                                    currentCoroutineContext().ensureActive()
                                    val count = zip.read(buffer)
                                    if (count < 0) break
                                    expanded += count
                                    check(expanded <= 800L * 1024 * 1024) { "Model expansion exceeded its limit." }
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                        zip.closeEntry()
                    }
                }
                val extracted = File(stage, spec.name)
                check(File(extracted, "am/final.mdl").isFile && File(extracted, "conf/mfcc.conf").isFile) { "Downloaded archive is not the expected Vosk model." }
                val destination = File(root, spec.name)
                destination.deleteRecursively()
                check(extracted.renameTo(destination)) { "Unable to install offline model." }
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                // Upstream does not publish an expected digest: this records the HTTPS-downloaded artifact, not an independently verified upstream checksum.
                File(destination, ".complete").writeText("source=${spec.url}\nsha256=$hash\nlicense=Apache-2.0\n")
                onProgress(1f)
                destination
            } finally {
                archive.delete(); stage.deleteRecursively()
            }
        }
    }

    companion object { private const val MAX_ARCHIVE_BYTES = 160L * 1024 * 1024 }
}
