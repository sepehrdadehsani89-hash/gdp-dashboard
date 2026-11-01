package com.jarvis.assistant

import android.Manifest
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantPhase
import com.jarvis.assistant.core.AssistantReply
import com.jarvis.assistant.core.AssistantSettings
import com.jarvis.assistant.core.AttachmentReader
import com.jarvis.assistant.core.ChatMessage
import com.jarvis.assistant.core.LanguageRules
import com.jarvis.assistant.core.LocalContextBudget
import com.jarvis.assistant.core.MessageRole
import com.jarvis.assistant.core.PhoneContext
import com.jarvis.assistant.core.choose
import com.jarvis.assistant.data.JarvisStore
import com.jarvis.assistant.data.LlmClient
import com.jarvis.assistant.local.LocalBrain
import com.jarvis.assistant.tools.DeviceTools
import com.jarvis.assistant.ui.JarvisUiState
import com.jarvis.assistant.voice.SpeechController
import com.jarvis.assistant.voice.WakeListenerService
import com.jarvis.assistant.voice.WakeModelManager
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Process-scoped session owner: no hidden microphone start, no LLM-authorized actions. */
class AssistantRuntime(private val context: Context) {
    val store = JarvisStore(context)
    private val mutableState = MutableStateFlow(JarvisUiState())
    val state = mutableState.asStateFlow()
    private val errors = CoroutineExceptionHandler { _, _ ->
        fail(language().choose("Something interrupted that request. Please try again.", "La solicitud se interrumpió. Inténtalo otra vez.", "درخواست متوقف شد. لطفاً دوباره تلاش کنید."))
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + errors)
    private val speech = SpeechController(context)
    private val models = WakeModelManager(context)
    private val tools = DeviceTools(context, store)
    private val phone = PhoneContext(context)
    private val llm = LlmClient(store)
    private val brain by lazy { LocalBrain(context) }
    private val attachments by lazy { AttachmentReader(context) }
    private val selectedUris = mutableListOf<Uri>()
    private var session: Job? = null
    private var cloudVoiceJob: Job? = null
    private var wakeDownload: Job? = null
    private var brainDownload: Job? = null
    private var voiceDownload: Job? = null
    private var initialized = false
    @Volatile private var sessionGeneration = 0L
    private var lastReplyLanguage = AppLanguage.EN
    private var lastRequestWasVoice = false

    fun initialize() {
        if (initialized) return
        initialized = true
        scope.launch { store.settings.collectLatest { settings ->
            mutableState.update { it.copy(settings = settings, hasApiKey = store.getApiKey(settings.provider).isNotBlank()) }
            refreshCapabilities()
        } }
        scope.launch { store.messages.collectLatest { messages -> mutableState.update { it.copy(messages = messages) } } }
        scope.launch { store.memories.collectLatest { memories -> mutableState.update { it.copy(memories = memories) } } }
        scope.launch { WakeListenerService.state.collectLatest { wake -> mutableState.update { it.copy(wakeStatus = wake.status) } } }
        ContextCompat.registerReceiver(context, object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WakeListenerService.ACTION_COMMAND -> {
                        val text = intent.getStringExtra(WakeListenerService.EXTRA_TEXT).orEmpty()
                        if (text.isNotBlank()) submit(text, true)
                    }
                    WakeListenerService.ACTION_STOP -> stopEverything()
                }
            }
        }, IntentFilter().apply {
            addAction(WakeListenerService.ACTION_COMMAND)
            addAction(WakeListenerService.ACTION_STOP)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        scope.launch { delay(1000); refreshCapabilities() }
    }

    fun refreshCapabilities() {
        val lang = language()
        mutableState.update { it.copy(
            hasMicrophone = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
            brainStatus = runCatching {
                if (brain.modelReady()) brain.modelStatus() else lang.choose("Local brain not installed · optional ~400 MB download", "Cerebro local no instalado · descarga opcional de ~400 MB", "مدل محلی نصب نشده · دانلود اختیاری حدود ۴۰۰ مگابایت")
            }.getOrElse { lang.choose("Local brain not installed", "Cerebro local no instalado", "مدل محلی نصب نشده است") },
            voiceStatus = speech.voiceStatus(lang),
        ) }
    }

    fun saveSettings(value: AssistantSettings, key: String) {
        scope.launch {
            if (key.isNotBlank() && value.provider != "local") withContext(Dispatchers.IO) { store.setApiKey(value.provider, key) }
            if (value.language != language()) stopWake()
            store.updateSettings(value)
            mutableState.update { it.copy(error = null, hasApiKey = store.getApiKey(value.provider).isNotBlank()) }
            refreshCapabilities()
        }
    }

    fun setLanguage(value: AppLanguage) {
        stopWake()
        store.updateSettings(store.settings.value.copy(language = value))
        refreshCapabilities()
    }

    fun listen() {
        if (locked()) { fail(unlockMessage()); return }
        cancelSession()
        pauseWake()
        val lang = language()
        mutableState.update { it.copy(phase = AssistantPhase.LISTENING, transcript = "", error = null, pendingAction = null) }
        speech.listen(lang, store.settings.value.allowNetworkSpeech,
            onPartial = { text -> mutableState.update { it.copy(transcript = text) } },
            onResult = { text -> submit(text, true) },
            onError = { message -> fail(message); resumeWake() },
        )
    }

    fun submit(input: String, voiced: Boolean = false) {
        val text = input.trim().take(8000)
        if (text.isBlank()) return
        if (LanguageRules.isStop(text)) { stopEverything(); return }
        if (locked()) { fail(unlockMessage()); return }
        val pending = state.value.pendingAction
        if (pending != null) {
            when (LanguageRules.confirmation(text)) {
                true -> { confirmAction(pending.id); return }
                false -> { cancelAction(); return }
                null -> Unit
            }
        }
        cancelSession()
        pauseWake()
        lastRequestWasVoice = voiced
        val lang = LanguageRules.languageRequest(text) ?: LanguageRules.detect(text, language())
        lastReplyLanguage = lang
        val generation = sessionGeneration
        mutableState.update { it.copy(phase = AssistantPhase.THINKING, transcript = text, error = null, pendingAction = null) }
        val uris = selectedUris.toList()
        session = scope.launch {
            try {
                store.addMessage(ChatMessage(role = MessageRole.USER, text = text))
                val requestedLanguage = LanguageRules.languageRequest(text)
                val result = if (requestedLanguage != null) {
                    setLanguage(requestedLanguage)
                    AssistantReply(requestedLanguage.choose("Of course. I'll answer in English.", "Por supuesto. Responderé en español.", "حتماً. از این به بعد فارسی پاسخ می‌دهم."))
                } else {
                    // Read-only private tool output stays local and is never added to cloud context.
                    phone.route(text, lang) ?: tools.route(text, lang) ?: generate(text, lang, uris, generation)
                }
                if (generation == sessionGeneration) deliver(result, lang, voiced, generation)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (generation == sessionGeneration) {
                    val message = if (store.settings.value.provider == "local" && !brain.modelReady()) lang.choose(
                        "News, YouTube, time, and saved memories work without an API key. For general questions, download the optional local brain in Settings (about 400 MB).",
                        "Noticias, YouTube, hora y recuerdos funcionan sin clave. Para preguntas generales, descarga el cerebro local opcional en Ajustes (unos 400 MB).",
                        "خبرها، یوتیوب، ساعت و حافظه بدون کلید API کار می‌کنند. برای پرسش‌های عمومی، مدل محلی اختیاری را از تنظیمات دانلود کنید؛ حدود ۴۰۰ مگابایت است.",
                    ) else error.message?.take(450) ?: lang.choose("I couldn't finish that request.", "No pude completar la solicitud.", "نتوانستم درخواست را کامل کنم.")
                    deliver(AssistantReply(message), lang, voiced, generation)
                }
            }
        }
    }

    private suspend fun generate(text: String, lang: AppLanguage, uris: List<Uri>, generation: Long): AssistantReply {
        val selectedText = if (uris.isEmpty()) "" else withContext(Dispatchers.IO) {
            uris.take(3).map { uri ->
                val extracted = attachments.read(uri, lang)
                "USER-SELECTED DOCUMENT EXCERPT (may be incomplete): ${attachments.displayName(uri)}\n${extracted.take(16_000)}"
            }.joinToString("\n\n")
        }
        // Selection authorizes this question, not automatic sharing on later unrelated requests.
        if (uris.isNotEmpty() && selectedUris.toList() == uris) clearAttachments()
        if (selectedText.isNotBlank() && store.settings.value.provider == "local" && !brain.modelReady()) {
            return AssistantReply(lang.choose(
                "Here is the text extracted on your phone. Download the optional local brain for translation or questions about it. This is text recognition, not a visual description.\n\n",
                "Este es el texto extraído en tu teléfono. Descarga el cerebro local para traducirlo o hacer preguntas. Es reconocimiento de texto, no una descripción visual.\n\n",
                "این متن روی گوشی استخراج شده است. برای ترجمه یا پرسش درباره آن، مدل محلی را دانلود کنید. این تشخیص متن است، نه توصیف تصویر.\n\n",
            ) + selectedText.take(20_000))
        }
        var streamed = ""
        val onChunk: (String) -> Unit = { delta ->
            streamed = (streamed + delta).take(32_000)
            if (generation == sessionGeneration) mutableState.update { it.copy(transcript = streamed) }
        }
        return if (store.settings.value.provider == "local") {
            // Private memory is usable on-device; no automatic sharing with a cloud provider.
            val memory = store.memories.value.take(12).joinToString("\n") { it.text }.take(4000)
            val localPrompt = LocalContextBudget.build(text, selectedText, memory)
            AssistantReply(brain.answer(localPrompt, lang, emptyList(), onChunk))
        } else {
            // No automatic transfer of prior screen/notification/contact outputs or saved memories.
            AssistantReply(llm.answer(text, lang, selectedText.take(24_000), emptyList(), onChunk))
        }
    }

    private suspend fun deliver(reply: AssistantReply, lang: AppLanguage, voiced: Boolean, generation: Long) {
        store.addMessage(ChatMessage(role = MessageRole.ASSISTANT, text = reply.text, sources = reply.sources))
        mutableState.update { it.copy(transcript = "", pendingAction = reply.action, phase = AssistantPhase.READY) }
        if (voiced || store.settings.value.speakReplies) speak(reply.text, lang, generation) else resumeWake()
    }

    private fun speak(text: String, lang: AppLanguage, generation: Long = sessionGeneration) {
        if (locked()) { fail(unlockMessage()); return }
        mutableState.update { it.copy(phase = AssistantPhase.SPEAKING) }
        val done = {
            if (generation == sessionGeneration) {
                mutableState.update { it.copy(phase = AssistantPhase.READY) }
                resumeWake()
            }
        }
        val error: (String) -> Unit = { message -> if (generation == sessionGeneration) { fail(message); resumeWake() } }
        if (store.settings.value.useCloudVoice) {
            cloudVoiceJob = scope.launch {
                try {
                    val audio = llm.synthesizeSpeech(text.take(3500), lang)
                    val file = withContext(Dispatchers.IO) { File.createTempFile("reply-", ".mp3", context.cacheDir).apply { writeBytes(audio) } }
                    if (generation == sessionGeneration) speech.playAudio(file, { file.delete(); done() }, { message -> file.delete(); error(message) }) else file.delete()
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error(lang.choose("Cloud voice is unavailable. Check its key, or turn it off to use a downloaded voice.", "La voz en la nube no está disponible. Revisa su clave o usa una voz descargada.", "صدای ابری در دسترس نیست. کلید را بررسی کنید یا از صدای دانلودشده استفاده کنید.")) }
            }
        } else speech.speak(text.take(5000), lang, done, error)
    }

    fun confirmAction(id: String) {
        if (locked()) { fail(unlockMessage()); return }
        val proposal = state.value.pendingAction?.takeIf { it.id == id } ?: return
        ++sessionGeneration
        val generation = sessionGeneration
        session?.cancel()
        cloudVoiceJob?.cancel()
        runCatching { brain.cancel() }
        speech.stop()
        pauseWake()
        mutableState.update { it.copy(pendingAction = null, phase = AssistantPhase.THINKING) }
        session = scope.launch {
            val result = tools.execute(proposal)
            if (generation == sessionGeneration) deliver(AssistantReply(result), lastReplyLanguage, lastRequestWasVoice, generation)
        }
    }

    fun cancelAction() {
        tools.cancelPendingActions()
        mutableState.update { it.copy(pendingAction = null) }
    }

    fun startWake() {
        if (locked()) { fail(unlockMessage()); return }
        if (models.modelPath(language()) == null) {
            fail(language().choose("Download this language's offline speech model first.", "Descarga primero el modelo de voz de este idioma.", "ابتدا مدل گفتار آفلاین این زبان را دانلود کنید.")); return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            fail(language().choose("Allow the microphone first.", "Permite primero el micrófono.", "ابتدا دسترسی میکروفن را بدهید.")); return
        }
        try {
            ContextCompat.startForegroundService(context, Intent(context, WakeListenerService::class.java).apply {
                action = WakeListenerService.ACTION_START
                putExtra(WakeListenerService.EXTRA_LANGUAGE, language().code)
            })
        } catch (_: Exception) {
            fail(language().choose("Android blocked background listening. Open Jarvis and tap Start listening again.", "Android bloqueó la escucha. Abre Jarvis y vuelve a tocar Iniciar escucha.", "اندروید شنیدن در پس‌زمینه را مسدود کرد. جارویس را باز کنید و دوباره شروع را بزنید."))
        }
    }

    fun stopWake() { context.stopService(Intent(context, WakeListenerService::class.java)) }
    private fun pauseWake() {
        if (WakeListenerService.state.value.running) context.startService(Intent(context, WakeListenerService::class.java).setAction(WakeListenerService.ACTION_PAUSE))
    }
    private fun resumeWake() {
        if (WakeListenerService.state.value.running && !locked()) runCatching {
            context.startService(Intent(context, WakeListenerService::class.java).setAction(WakeListenerService.ACTION_RESUME))
        }
    }

    fun downloadWake() {
        if (wakeDownload?.isActive == true) return
        val lang = language()
        wakeDownload = scope.launch {
            mutableState.update { it.copy(modelProgress = 0f, error = null) }
            try { models.download(lang) { progress -> mutableState.update { it.copy(modelProgress = progress) } } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { fail(lang.choose("Speech download failed. Check Wi-Fi and free space, then retry.", "Falló la descarga de voz. Comprueba Wi-Fi y espacio libre.", "دانلود گفتار ناموفق بود. وای‌فای و فضای خالی را بررسی کنید.")) }
            finally { mutableState.update { it.copy(modelProgress = null) }; refreshCapabilities() }
        }
    }

    fun downloadBrain() {
        if (brainDownload?.isActive == true) return
        brainDownload = scope.launch {
            mutableState.update { it.copy(brainProgress = 0f, error = null) }
            try { brain.download { progress -> mutableState.update { it.copy(brainProgress = progress) } } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { fail(language().choose("Brain download failed. Use Wi-Fi and allow at least 1 GB free storage before retrying.", "Falló la descarga. Usa Wi-Fi y al menos 1 GB de espacio libre.", "دانلود مدل ناموفق بود. از وای‌فای استفاده کنید و حداقل یک گیگابایت فضای خالی داشته باشید.")) }
            finally { mutableState.update { it.copy(brainProgress = null) }; refreshCapabilities() }
        }
    }

    fun downloadVoice() {
        if (voiceDownload?.isActive == true) return
        val lang = language()
        voiceDownload = scope.launch {
            mutableState.update { it.copy(voiceProgress = 0f, error = null) }
            try { speech.downloadVoice(lang) { progress -> mutableState.update { it.copy(voiceProgress = progress) } } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { fail(lang.choose("Voice download failed. Check Wi-Fi and free space, then retry.", "Falló la descarga de voz. Comprueba Wi-Fi y espacio libre.", "دانلود صدا ناموفق بود. وای‌فای و فضای خالی را بررسی کنید.")) }
            finally { mutableState.update { it.copy(voiceProgress = null) }; refreshCapabilities() }
        }
    }

    fun voiceCheck() {
        cancelSession(); pauseWake()
        speak(language().choose("Hello, I'm Jarvis. Your voice is ready.", "Hola, soy Jarvis. Tu voz está lista.", "سلام، من جارویس هستم. صدای شما آماده است."), language())
    }

    fun addAttachments(uris: List<Uri>) {
        selectedUris.clear(); selectedUris.addAll(uris.take(3))
        mutableState.update { it.copy(attachedFiles = selectedUris.map { uri -> attachments.displayName(uri) }, error = null) }
    }
    fun clearAttachments() { selectedUris.clear(); mutableState.update { it.copy(attachedFiles = emptyList()) } }
    fun deleteMemory(id: Long) { scope.launch { store.forget(id) } }
    fun clearConversation() { cancelSession(); scope.launch { store.clearConversation() } }

    fun wipe() {
        val cancelledJobs = listOfNotNull(session, cloudVoiceJob, wakeDownload, brainDownload, voiceDownload)
        stopEverything()
        clearAttachments()
        scope.launch {
            cancelledJobs.forEach { it.join() }
            brain.release()
            store.clearAll()
            withContext(Dispatchers.IO) {
                listOf("wake-models", "voice-models", "local-brain", "ocr").forEach { File(context.filesDir, it).deleteRecursively() }
                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            }
            mutableState.value = JarvisUiState()
            refreshCapabilities()
        }
    }

    fun stopEverything() {
        cancelSession()
        wakeDownload?.cancel(); brainDownload?.cancel(); voiceDownload?.cancel()
        stopWake()
        mutableState.update { it.copy(phase = AssistantPhase.PAUSED, transcript = "", pendingAction = null, modelProgress = null, brainProgress = null, voiceProgress = null) }
    }

    private fun cancelSession() {
        ++sessionGeneration
        session?.cancel(); session = null
        cloudVoiceJob?.cancel(); cloudVoiceJob = null
        runCatching { brain.cancel() }
        speech.stop()
        tools.cancelPendingActions()
    }
    fun trimMemory() {
        if (session?.isActive != true) scope.launch { runCatching { brain.release() } }
    }
    private fun fail(message: String) { mutableState.update { it.copy(error = message, phase = AssistantPhase.ERROR) } }
    private fun language() = store.settings.value.language
    private fun locked() = context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked == true
    private fun unlockMessage() = language().choose("Unlock your phone to use Jarvis with private information.", "Desbloquea el teléfono para usar Jarvis con información privada.", "برای استفاده از اطلاعات خصوصی، قفل گوشی را باز کنید.")
}
