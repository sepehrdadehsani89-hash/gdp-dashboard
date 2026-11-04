package com.jarvis.assistant.voice

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.core.content.ContextCompat
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.choose
import java.io.File
import java.util.Locale
import java.util.UUID

/** Owns foreground, user-initiated speech. All platform speech APIs are called on main. */
class SpeechController(context: Context) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val audio = this.context.getSystemService(AudioManager::class.java)
    private val offlineVoice = OfflineVoiceManager(this.context)
    private var recognizer: SpeechRecognizer? = null
    @Volatile var lastRecognitionLocale:String="not started";private set
    private var androidMicrophoneLease:com.jarvis.assistant.rebirth.MicrophoneLease?=null
    private var offlineSpeech: OfflineSpeechSession? = null
    private var player: MediaPlayer? = null
    private var temporaryPlayback:File?=null
    private var ttsReady = false
    private var ttsFailed = false
    private var pendingSpeech: (() -> Unit)? = null
    @Volatile private var generation = 0
    private var speakingDone: (() -> Unit)? = null
    private var speakingError: ((String) -> Unit)? = null
    private var focus: AudioFocusRequest? = null
    private var utterancePrefix: String? = null
    private var lastUtterance: String? = null
    private val tts: TextToSpeech = TextToSpeech(this.context) { status ->
        main.post {
            ttsReady = status == TextToSpeech.SUCCESS
            ttsFailed = !ttsReady
            pendingSpeech?.also { pendingSpeech = null; it() }
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { main.post { if (utteranceId == lastUtterance && utteranceId != null) finishSpeaking() } }
            @Deprecated("Platform callback")
            override fun onError(utteranceId: String?) { main.post { if (utteranceId?.startsWith(utterancePrefix ?: "never-match") == true) failSpeaking("Speech playback failed. Try another installed voice.") } }
            override fun onError(utteranceId: String?, errorCode: Int) { main.post { if (utteranceId?.startsWith(utterancePrefix ?: "never-match") == true) failSpeaking("Speech playback failed ($errorCode).") } }
        })
    }

    fun listen(
        language: AppLanguage,
        allowNetwork: Boolean,
        onPartial: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onCaptureStarted:()->Unit={},
        onAlternatives:(List<com.jarvis.assistant.rebirth.RecognitionHypothesis>)->Unit={},
    ) {
        main.post {
            stopInternal()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                onError(language.choose("Microphone permission is required.", "Hace falta permiso del micrófono.", "اجازهٔ دسترسی به میکروفن لازم است."))
                return@post
            }
            val localModel = WakeModelManager(context).modelPath(language)
            if (localModel != null) {
                val token = generation
                offlineSpeech = OfflineSpeechSession(context, localModel,
                    onPartial = { if (token == generation) onPartial(it) },
                    onResult = { if (token == generation) { offlineSpeech = null; onResult(it) } },
                    onError = { if (token == generation) { offlineSpeech = null; onError(it) } },
                    onCaptureStarted={if(token==generation)onCaptureStarted()},
                    onAlternatives={if(token==generation)onAlternatives(it)},
                ).also { it.start() }
                return@post
            }
            val onDevice = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
            if (!onDevice && !allowNetwork) {
                onError(language.choose(
                    "This phone has no available on-device speech recognizer. Enable network speech in Settings or use the keyboard. Wake mode uses its downloaded offline model.",
                    "Este teléfono no ofrece reconocimiento de voz local. Activa voz por internet en Ajustes o usa el teclado. La activación usa su modelo local descargado.",
                    "تشخیص گفتار روی دستگاه در دسترس نیست. در تنظیمات، تشخیص گفتار اینترنتی را فعال کنید یا بنویسید. حالت بیدارباش از مدل آفلاین دانلودشده استفاده می‌کند.",
                ))
                return@post
            }
            if (!onDevice && !SpeechRecognizer.isRecognitionAvailable(context)) {
                onError(language.choose("Install or enable an Android speech recognition service.", "Instala o activa un servicio de reconocimiento de voz de Android.", "یک سرویس تشخیص گفتار اندروید نصب یا فعال کنید."))
                return@post
            }
            val lease=com.jarvis.assistant.rebirth.MicrophoneSessions.manager.acquire(com.jarvis.assistant.rebirth.MicrophoneOwner.FOREGROUND_RECOGNIZER)
            if(lease==null){onError("RECOGNIZER_BUSY: previous audio capture is still releasing.");return@post}
            androidMicrophoneLease=lease
            try {
                val token = generation
                var completed = false
                val captureTiming=SpeechInactivityClock(android.os.SystemClock::elapsedRealtime)
                var speechDetected=false
                val instance = if (onDevice && Build.VERSION.SDK_INT >= 31) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    else SpeechRecognizer.createSpeechRecognizer(context)
                recognizer = instance
                instance.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { if(token!=generation || completed)return;onCaptureStarted();com.jarvis.assistant.hud.AudioSignals.start(instance,"Android recognizer · RMS level") }
                    override fun onBeginningOfSpeech() { if(token!=generation || completed)return;speechDetected=true;captureTiming.progress("[Android speech started]") }
                    override fun onRmsChanged(rmsdB: Float) { if(token==generation){val level=((rmsdB+2)/12).coerceIn(0f,1f);com.jarvis.assistant.hud.AudioSignals.level(instance,level);if(speechDetected)captureTiming.energy(level)} }
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() { if(token!=generation || completed)return;speechDetected=false;com.jarvis.assistant.hud.AudioSignals.stop(instance) }
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                    override fun onError(error: Int) {
                        if (token != generation || completed) return
                        completed = true
                        com.jarvis.assistant.hud.AudioSignals.stop(instance); runCatching{instance.destroy()}; recognizer = null;releaseAndroidMicrophone()
                        onError(recognitionError(error, language))
                    }
                    override fun onResults(results: Bundle?) {
                        if (token != generation || completed) return
                        completed = true
                        val hypotheses=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty().take(5)
                        val scores=results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                        onAlternatives(hypotheses.mapIndexed{i,text->com.jarvis.assistant.rebirth.RecognitionHypothesis(text.take(4000),scores?.getOrNull(i)?.takeIf{it.isFinite() && it in 0f..1f})})
                        val text=hypotheses.firstOrNull().orEmpty()
                        com.jarvis.assistant.hud.AudioSignals.stop(instance); runCatching{instance.destroy()}; recognizer = null;releaseAndroidMicrophone()
                        if(captureTiming.expired())onError("Speech inactivity or two-minute utterance limit reached.") else if (text.isBlank()) onError(recognitionError(SpeechRecognizer.ERROR_NO_MATCH, language)) else onResult(text)
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        if (token == generation && !completed){val text=partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty();captureTiming.progress(text);onPartial(text)}
                    }
                })
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.tag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, !allowNetwork)
                }
                var started=false
                fun startWithLocale(locale:String,supportReported:Boolean){
                    if(started || completed || token!=generation)return
                    started=true;lastRecognitionLocale="$locale · "+if(supportReported)"reported by recognizer" else "configured fallback; support not reported"
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,locale)
                    try{instance.startListening(intent)}catch(_:Exception){completed=true;runCatching{instance.destroy()};recognizer=null;releaseAndroidMicrophone();onError("AUDIO_ERROR: Android could not start this recognizer.")}
                }
                if(Build.VERSION.SDK_INT>=33){
                    try{instance.checkRecognitionSupport(intent,context.mainExecutor,object:android.speech.RecognitionSupportCallback{
                        override fun onSupportResult(support:android.speech.RecognitionSupport){
                            if(token!=generation || completed)return
                            val locales=support.installedOnDeviceLanguages+(if(!onDevice && allowNetwork)support.onlineLanguages else emptyList())
                            val chosen=com.jarvis.assistant.rebirth.SpeechLocalePolicy.select(language==AppLanguage.ES,locales)
                            startWithLocale(chosen ?: language.tag,chosen!=null)
                        }
                        override fun onError(error:Int){startWithLocale(language.tag,false)}
                    })}catch(_:Exception){startWithLocale(language.tag,false)}
                    main.postDelayed({startWithLocale(language.tag,false)},1500)
                }else startWithLocale(language.tag,false)
                val timeout=object:Runnable{override fun run(){
                    if(token!=generation || completed)return
                    if(!captureTiming.expired()){main.postDelayed(this,(captureTiming.deadline()-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(1));return}
                    completed=true;runCatching{instance.cancel()};com.jarvis.assistant.hud.AudioSignals.stop(instance);runCatching{instance.destroy()};recognizer=null;releaseAndroidMicrophone()
                    onError("Speech inactivity or two-minute utterance limit reached.")
                }}
                main.postDelayed(timeout,SpeechInactivityClock.IDLE_MS)
            } catch (_: Exception) {
                runCatching{recognizer?.destroy()}; recognizer = null;releaseAndroidMicrophone()
                onError(language.choose("Speech capture is unavailable. Check the microphone and speech service.", "No se puede escuchar. Revisa el micrófono y el servicio de voz.", "ضبط گفتار در دسترس نیست. میکروفن و سرویس گفتار را بررسی کنید."))
            }
        }
    }

    fun availableVoices(language: AppLanguage): List<String> = if (!ttsReady) emptyList() else tts.voices.orEmpty().filter { it.locale.language == language.code && !it.isNetworkConnectionRequired && !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }.map { it.name }.sorted()

    fun voiceStatus(language: AppLanguage): String {
        val selected=com.jarvis.assistant.hud.HudPreferences(context).read().voiceEngine
        if(selected=="neural")return if(offlineVoice.modelPath(language)!=null)"Piper / sherpa-onnx · selected on-device neural voice" else "Selected neural voice is not downloaded for ${language.label}"
        if (selected!="system" && offlineVoice.modelPath(language) != null && systemVoice(language) == null) {
            return "Piper / sherpa-onnx · ${language.choose("on-device neural voice", "voz neuronal local", "صدای عصبی روی دستگاه") }"
        }
        if (!ttsReady) return language.choose(if (ttsFailed) "Voice engine unavailable" else "Voice engine initializing", "Inicializando motor de voz", "موتور گفتار در حال آماده‌شدن است")
        val voice = systemVoice(language)
        return if (voice != null) "${voice.name} · ${language.choose("on-device", "local", "روی دستگاه")}" else language.choose(
            "No offline ${language.label} voice installed. Download this language's offline voice in Jarvis Settings or install a system voice.",
            "No hay una voz local de ${language.label}. Descárgala en Ajustes de Jarvis o instala una voz del sistema.",
            "صدای آفلاین ${language.label} نصب نیست. صدای آفلاین این زبان را از تنظیمات جارویس دانلود کنید یا صدای گوشی را نصب کنید.",
        )
    }

    fun speak(text: String, language: AppLanguage, onDone: () -> Unit, onError: (String) -> Unit,volumeOverride:Float?=null) {
        main.post {
            if(language!=AppLanguage.EN){onError("La voz española requiere la configuración exclusiva de Fish Audio; no se sustituyó otra voz.");return@post}
            val engine = com.jarvis.assistant.hud.HudPreferences(context).read().voiceEngine
            if (engine == "neural" && offlineVoice.modelPath(language) == null) { onError("Download the selected language's neural voice in Settings first."); return@post }
            val hasSystemVoice = systemVoice(language) != null && com.jarvis.assistant.hud.HudPreferences(context).read().voiceEngine != "neural"
            if (engine != "system" && !hasSystemVoice && offlineVoice.modelPath(language) != null) {
                if (isQuietMode()) { onError(language.choose("Voice reply paused by Silent or Do Not Disturb mode.", "Respuesta silenciada por Silencio o No molestar.", "پاسخ صوتی به‌دلیل حالت بی‌صدا یا مزاحم نشوید پخش نشد.")); return@post }
                stopInternal(); speakingDone = onDone; speakingError = onError
                if (!requestFocus()) { failSpeaking(language.choose("Audio is in use by another app.", "Otra aplicación está usando el audio.", "صدا در اختیار برنامهٔ دیگری است.")); return@post }
                WakeListenerService.suppressFor(60_000)
                val cleaned = cleanForSpeech(text)
                offlineVoice.speak(cleaned, language, onDone = { finishSpeaking() }, onError = { failSpeaking(it) },volumeOverride=volumeOverride)
                return@post
            }
            if (!ttsReady && !ttsFailed) {
                val token = generation
                pendingSpeech = { if (token == generation) speak(text, language, onDone, onError,volumeOverride) }
                main.postDelayed({ if (token == generation && pendingSpeech != null) { pendingSpeech = null; onError(voiceStatus(language)) } }, 6000)
                return@post
            }
            if (!ttsReady) { onError(voiceStatus(language)); return@post }
            if (isQuietMode()) {
                onError(language.choose("Voice reply paused by Silent or Do Not Disturb mode.", "Respuesta de voz silenciada por Silencio o No molestar.", "پاسخ صوتی به‌دلیل حالت بی‌صدا یا مزاحم نشوید پخش نشد."))
                return@post
            }
            val voice = systemVoice(language)
            if (voice == null) { onError(voiceStatus(language)); return@post }
            stopInternal()
            speakingDone = onDone; speakingError = onError
            tts.voice = voice
            val controls = com.jarvis.assistant.hud.HudPreferences(context).read()
            tts.setSpeechRate(controls.voiceRate)
            tts.setPitch(controls.voicePitch)
            tts.setAudioAttributes(speechAttributes())
            if (!requestFocus()) { failSpeaking(language.choose("Audio is in use by another app.", "Otra aplicación está usando el audio.", "صدا در اختیار برنامهٔ دیگری است.")); return@post }
            WakeListenerService.suppressFor(60_000)
            val cleaned = cleanForSpeech(text)
            if (cleaned.isBlank()) { finishSpeaking(); return@post }
            val parts = cleaned.chunked(TextToSpeech.getMaxSpeechInputLength() - 100)
            val prefix = UUID.randomUUID().toString()
            utterancePrefix = prefix; lastUtterance = "$prefix-${parts.lastIndex}"
            parts.forEachIndexed { index, part ->
                val result = tts.speak(part, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,volumeOverride ?: controls.voiceVolume) }, "$prefix-$index")
                if (result == TextToSpeech.ERROR) { failSpeaking(voiceStatus(language)); return@post }
            }
        }
    }

    fun playAudio(file: File, onDone: () -> Unit, onError: (String) -> Unit,volumeOverride:Float?=null,onStarted:()->Unit={}) {
        main.post {
            if (isQuietMode()) { onError("Voice reply paused by Silent or Do Not Disturb mode."); return@post }
            stopInternal(); temporaryPlayback=file; speakingDone = onDone; speakingError = onError
            if (!requestFocus()) { failSpeaking("Audio is in use by another app."); return@post }
            WakeListenerService.suppressFor(60_000)
            try {
                player = MediaPlayer().apply {
                    setAudioAttributes(speechAttributes())
                    setDataSource(file.absolutePath)
                    setOnCompletionListener { finishSpeaking() }
                    setOnErrorListener { _, _, _ -> failSpeaking("Cloud voice playback failed."); true }
                    prepareAsync()
                    setOnPreparedListener { p ->
                        val controls=com.jarvis.assistant.hud.HudPreferences(context).read()
                        p.setVolume(volumeOverride ?: controls.voiceVolume,volumeOverride ?: controls.voiceVolume)
                        runCatching { p.playbackParams = android.media.PlaybackParams().setSpeed(controls.voiceRate).setPitch(controls.voicePitch) }
                        try{p.start();onStarted()}catch(_:Exception){failSpeaking("Cloud voice playback failed.")}
                    }
                }
            } catch (_: Exception) { failSpeaking("Cloud voice playback failed.") }
        }
    }

    fun stop() { main.post { stopInternal() } }
    suspend fun downloadVoice(language: AppLanguage, onProgress: (Float) -> Unit): File = offlineVoice.download(language, onProgress)
    fun voiceModelReady(language: AppLanguage): Boolean = offlineVoice.modelPath(language) != null
    fun shutdown() { main.post { stopInternal(); tts.shutdown(); offlineVoice.shutdown() } }

    private fun releaseAndroidMicrophone(){
        val lease=androidMicrophoneLease ?: return;androidMicrophoneLease=null
        // Android recognition service teardown is asynchronous; avoid an immediate busy restart.
        main.postDelayed({com.jarvis.assistant.rebirth.MicrophoneSessions.manager.release(lease)},200)
    }
    private fun stopInternal() {
        generation++
        pendingSpeech = null; speakingDone = null; speakingError = null
        utterancePrefix = null; lastUtterance = null
        offlineSpeech?.close(); offlineSpeech = null
        offlineVoice.stop()
        recognizer?.let { com.jarvis.assistant.hud.AudioSignals.stop(it); runCatching{it.cancel()};runCatching{it.destroy()} }; recognizer = null;releaseAndroidMicrophone()
        tts.stop()
        player?.run { runCatching { stop() }; release() }; player = null
        temporaryPlayback?.delete();temporaryPlayback=null
        focus?.let { audio.abandonAudioFocusRequest(it) }; focus = null
        WakeListenerService.setSpeechActive(false)
    }

    private fun finishSpeaking() {
        val callback = speakingDone
        speakingDone = null; speakingError = null
        utterancePrefix = null; lastUtterance = null
        player?.release(); player = null
        focus?.let { audio.abandonAudioFocusRequest(it) }; focus = null
        WakeListenerService.setSpeechActive(false)
        callback?.invoke()
    }

    private fun failSpeaking(message: String) {
        val callback = speakingError
        stopInternal()
        callback?.invoke(message)
    }

    private fun speechAttributes() = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private fun requestFocus(): Boolean {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(speechAttributes())
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) main.post {
                    if (speakingError != null) failSpeaking("Voice playback was interrupted by another audio session.") else stopInternal()
                }
            }
            .build()
        focus = request
        val granted=audio.requestAudioFocus(request)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if(granted)WakeListenerService.setSpeechActive(true)
        return granted
    }

    private fun isQuietMode(): Boolean {
        val notification = context.getSystemService(NotificationManager::class.java)
        return audio.ringerMode == AudioManager.RINGER_MODE_SILENT || notification.currentInterruptionFilter in setOf(
            NotificationManager.INTERRUPTION_FILTER_NONE,
            NotificationManager.INTERRUPTION_FILTER_ALARMS,
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        )
    }

    private fun cleanForSpeech(text: String) = text.replace(Regex("https?://\\S+"), "").replace(Regex("[*#`]+"), "").take(16_000)

    private fun systemVoice(language: AppLanguage): Voice? {
        if (!ttsReady || runCatching { tts.isLanguageAvailable(Locale.forLanguageTag(language.tag)) < TextToSpeech.LANG_AVAILABLE }.getOrDefault(true)) return null
        val voices = runCatching { tts.voices }.getOrNull().orEmpty().filter {
            it.locale.language == language.code && !it.isNetworkConnectionRequired && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty()
        }
        return voices.firstOrNull { it.name == com.jarvis.assistant.hud.HudPreferences(context).read().voiceId } ?: voices.firstOrNull()
    }

    private fun recognitionError(code: Int, language: AppLanguage) = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> language.choose("I didn't catch that. Tap the microphone and try again.", "No entendí. Toca el micrófono e inténtalo otra vez.", "متوجه نشدم. میکروفن را بزنید و دوباره بگویید.")
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> language.choose("Microphone access was denied.", "Se denegó el acceso al micrófono.", "دسترسی به میکروفن داده نشد.")
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_AUDIO -> "RECOGNIZER_BUSY: "+language.choose("The microphone is busy. Stop wake listening or another recording app, then retry.", "El micrófono está ocupado. Detén la activación o la otra aplicación y vuelve a probar.", "میکروفن مشغول است. حالت بیدارباش یا برنامهٔ ضبط دیگر را متوقف و دوباره تلاش کنید.")
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> language.choose("This recognizer has no downloaded support for ${language.label}. Check Android speech settings.", "Este reconocedor no tiene soporte descargado para ${language.label}. Revisa los ajustes de voz.", "پشتیبانی دانلودشده برای ${language.label} در این سرویس نیست. تنظیمات گفتار اندروید را بررسی کنید.")
        else -> language.choose("Speech recognition failed ($code). Check your speech service and connection.", "Falló el reconocimiento ($code). Revisa el servicio de voz y la conexión.", "تشخیص گفتار ناموفق بود ($code). سرویس گفتار و اتصال را بررسی کنید.")
    }
}
