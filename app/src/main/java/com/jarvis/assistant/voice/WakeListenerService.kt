package com.jarvis.assistant.voice

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.PowerManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.choose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer

/** User-enabled local recognition. Never started from boot, alarms, or a watchdog. */
class WakeListenerService : Service() {
    @Volatile private var running = false
    @Volatile private var paused = false
    @Volatile private var recorder: AudioRecord? = null
    private val audioLock = Any()
    private var worker: Thread? = null
    private val speakerBuffer=com.jarvis.assistant.omnisystem.SpeakerSampleBuffer()
    @Volatile private var failureReason:String?=null
    private var language = AppLanguage.EN
    private val window = ConversationWindow { SystemClock.elapsedRealtime() }
    private val windowTimeout = Runnable { if (window.expire()) publish(listeningText(), true) }
    private val audioManager by lazy { getSystemService(android.media.AudioManager::class.java) }
    private var cpuLock: PowerManager.WakeLock? = null
    private val main = Handler(Looper.getMainLooper())
    private val renewCpu = object : Runnable {
        override fun run() {
            if (running) { cpuLock?.acquire(10 * 60_000L); main.postDelayed(this, 5 * 60_000L) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        current=java.lang.ref.WeakReference(this)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Jarvis microphone", NotificationManager.IMPORTANCE_LOW).apply { description = "Visible status for user-enabled offline listening" },
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("jarvis_activation","Jarvis activation",NotificationManager.IMPORTANCE_DEFAULT))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                disableRecovery(this)
                sendBroadcast(Intent(ACTION_STOP).setPackage(packageName))
                stopSelf(); return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                synchronized(audioLock) { paused = true; window.pause(); main.removeCallbacks(windowTimeout); runCatching { recorder?.stop() } }
                publish(language.choose("Wake listening paused", "Activación en pausa", "بیدارباش مکث شده است"), false)
                return if (running) START_STICKY else START_NOT_STICKY
            }
            ACTION_RESUME -> {
                if (running) {
                    window.resume(intent.getBooleanExtra("follow_up", false))
                    main.removeCallbacks(windowTimeout)
                    paused = false
                    publish(if (window.snapshot().mode == ConversationWindow.Mode.WAITING_FOR_CAPTURE)
                        language.choose("Preparing a 10-second follow-up window", "Preparando 10 segundos para tu siguiente pregunta", "آماده‌سازی فرصت ۱۰ ثانیه‌ای پرسش بعدی") else listeningText(), true)
                }
                return if (running) START_STICKY else START_NOT_STICKY
            }
        }
        if (running) return START_STICKY
        val recovery = getSharedPreferences("wake_session", MODE_PRIVATE)
        if (intent == null && !recovery.getBoolean("enabled", false)) { stopSelf(); return START_NOT_STICKY }
        language = AppLanguage.fromCode(intent?.getStringExtra(EXTRA_LANGUAGE) ?: recovery.getString("language", "en").orEmpty())
        val modelDirectory = WakeModelManager(this).modelPath(language)
        val text = language.choose("Preparing offline listening…", "Preparando activación local…", "در حال آماده‌سازی بیدارباش آفلاین…")
        try {
            val notification = notification(text)
            if (Build.VERSION.SDK_INT >= 30) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            else startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            publish(language.choose("Android did not allow microphone listening. Open Jarvis and try again.", "Android no permitió escuchar. Abre Jarvis e inténtalo de nuevo.", "اندروید اجازهٔ گوش‌دادن نداد. جارویس را باز کنید و دوباره تلاش کنید."), false, updateNotification = false)
            stopSelf(); return START_NOT_STICKY
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || modelDirectory == null) {
            publish(language.choose("Microphone permission and an offline model are required.", "Se necesitan permiso del micrófono y un modelo local.", "اجازهٔ میکروفن و مدل آفلاین لازم است."), false)
            stopSelf(); return START_NOT_STICKY
        }
        running = true; paused = false; window.enable()
        publish(text,false)
        recovery.edit().putBoolean("enabled", true).putString("language", language.code).apply()
        cpuLock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Jarvis:WakeListening").apply { setReferenceCounted(false) }
        renewCpu.run()
        worker = Thread({
            try {
                Model(modelDirectory.absolutePath).use { model ->
                    while (running) {
                        if (paused) { Thread.sleep(100); continue }
                        if(audioManager.mode in listOf(android.media.AudioManager.MODE_IN_CALL,android.media.AudioManager.MODE_IN_COMMUNICATION)){
                            if(window.snapshot().mode!=ConversationWindow.Mode.WAKE_ONLY){window.resume(false);main.removeCallbacks(windowTimeout)}
                            publish("Microphone paused during a call",false,interrupted=true);Thread.sleep(500);continue
                        }
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) throw SecurityException("Microphone permission was revoked.")
                        val minimum = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                        check(minimum > 0) { "16 kHz microphone capture is not supported." }
                        val audio = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum * 2, 8192))
                        recorder = audio
                        audio.registerAudioRecordingCallback(mainExecutor,object:android.media.AudioManager.AudioRecordingCallback(){
                            override fun onRecordingConfigChanged(configs:MutableList<android.media.AudioRecordingConfiguration>){
                                if(configs.any { it.clientAudioSessionId==audio.audioSessionId && it.isClientSilenced }){
                                    failureReason="Android silenced microphone capture. Stop other recording or call apps, then reopen Jarvis to retry."
                                    publish(failureReason!!,false)
                                    stopSelf()
                                }
                            }
                        })
                        check(audio.state == AudioRecord.STATE_INITIALIZED) { "Microphone is unavailable." }
                        try {
                            Recognizer(model, 16000f).use { recognizer ->
                                recognizer.setWords(true)
                                val started = synchronized(audioLock) {
                                    if (running && !paused) { audio.startRecording(); true } else false
                                }
                                if (!started) return@use
                                com.jarvis.assistant.hud.AudioSignals.start(audio,"Wake microphone · PCM RMS")
                                publish(listeningText(), true)
                                val bytes = ByteArray(4096)
                                var suppressed = false
                                var recognizerSerial = window.snapshot().resetSerial
                                while (running && !paused) {
                                    val count = audio.read(bytes, 0, bytes.size)
                                    if (paused || !running) break
                                    check(count >= 0) { "Microphone capture was interrupted." }
                                    if (count == 0) continue
                                    if(audioManager.mode in listOf(android.media.AudioManager.MODE_IN_CALL,android.media.AudioManager.MODE_IN_COMMUNICATION)){window.resume(false);main.removeCallbacks(windowTimeout);publish("Microphone paused during a call",false,interrupted=true);break}
                                    com.jarvis.assistant.hud.AudioSignals.level(audio,com.jarvis.assistant.hud.PcmLevel.pcm16(bytes,count))
                                    if (speechActive || SystemClock.elapsedRealtime() < suppressionUntil) {
                                        speakerBuffer.clear()
                                        if (!suppressed) { recognizer.reset(); suppressed = true }
                                        continue
                                    }
                                    if (suppressed) { recognizer.reset(); suppressed = false }
                                    // Opt-in tap of this existing authorized recorder; never a second microphone.
                                    runCatching{speakerBuffer.feed(bytes,count,(application as com.jarvis.assistant.JarvisApplication).runtime.speakers.autoEnabled)}.onFailure{speakerBuffer.clear()}
                                    if (window.expire()) { recognizer.reset(); recognizerSerial = window.snapshot().resetSerial; publish(listeningText(), true); continue }
                                    val serial = window.snapshot().resetSerial
                                    if (recognizerSerial != serial) { recognizer.reset(); recognizerSerial = serial; continue }
                                    if (window.captureReady()) {
                                        publish(language.choose("Follow-up · listening for 10 seconds", "Siguiente pregunta · escucho durante 10 segundos", "پرسش بعدی · ۱۰ ثانیه گوش می‌دهم"), true, awaitingCommand = true)
                                        scheduleTimeout()
                                    }
                                    val final = recognizer.acceptWaveForm(bytes, count)
                                    val json=JSONObject(if(final)recognizer.result else recognizer.partialResult)
                                    val text=json.optString(if(final)"text" else "partial")
                                    if(!final && window.acceptsCommand(serial)){
                                        val partial=WakePhraseMatcher.extractCommand(text,language) ?: text
                                        sendBroadcast(Intent(ACTION_PARTIAL).setPackage(packageName).putExtra(EXTRA_TEXT,partial))
                                    }
                                    if(!final || RecognitionConfidence.acceptsFinal(json))handleTranscript(text,final,serial)
                                }
                            }
                        } finally { speakerBuffer.clear();com.jarvis.assistant.hud.AudioSignals.stop(audio); runCatching { audio.stop() }; audio.release(); recorder = null }
                    }
                }
            } catch (_: InterruptedException) {
                // Deliberate local shutdown.
            } catch (_: Throwable) {
                if (running) {
                    failureReason=language.choose("Offline listening stopped. Check the microphone/model and open Jarvis to retry.", "La escucha local se detuvo. Revisa el micrófono o modelo y abre Jarvis.", "گوش‌دادن آفلاین متوقف شد. میکروفن یا مدل را بررسی و جارویس را باز کنید.")
                    publish(failureReason!!,false)
                    stopSelf()
                }
            }
        }, "JarvisWakeListener").also { it.start() }
        return START_STICKY
    }

    private fun scheduleTimeout() {
        main.removeCallbacks(windowTimeout)
        val deadline = window.snapshot().deadline
        if (deadline > 0) main.postDelayed(windowTimeout, (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0))
    }

    private fun handleTranscript(text: String, final: Boolean, serial: Long) {
        if (text.isBlank() || serial != window.snapshot().resetSerial) return
        // A result recognized before expiry must not reactivate itself after the deadline.
        if (window.expire()) { publish(listeningText(), true); return }
        val afterWake = WakePhraseMatcher.extractCommand(text, language)
        if (afterWake != null) {
            if (window.wake()) {
                activateHud()
                publish(language.choose("Command window · listening for 10 seconds", "Tu petición · escucho durante 10 segundos", "فرصت دستور · ۱۰ ثانیه گوش می‌دهم"), true, awaitingCommand = true)
                scheduleTimeout()
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            }
            if (final && afterWake.isNotBlank()) deliver(afterWake, serial)
        } else if (final) deliver(text, serial)
    }

    private fun activateHud() {
        val launch=Intent(this,com.jarvis.assistant.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("sentinel_wake",true)
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("jarvis_activation","Jarvis activation",NotificationManager.IMPORTANCE_DEFAULT))
        val presentation=if(getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked)Intent(this,com.jarvis.assistant.neural.SafeActivationActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP) else launch
        val open=PendingIntent.getActivity(this,2,presentation,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(3102,NotificationCompat.Builder(this,"jarvis_activation").setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Jarvis is listening").setContentText("Tap to open your HUD. Unlock normally for protected actions.")
            .setContentIntent(open).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setSilent(true).build())
        main.post { (application as com.jarvis.assistant.JarvisApplication).runtime.wakeActivated() }
        // Android grants a background-activity exception to a user-enabled accessibility service.
        // Always retain the notification fallback; never use a full-screen intent or keyguard bypass.
        val locked=getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked
        val launcher=packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        val screenAccess=com.jarvis.assistant.tools.JarvisAccessibilityService.connected
        // A wallpaper preview is not the launcher, and known foreign targets stay intact.
        val allowed=com.jarvis.assistant.sentinel.ActivationPolicy.mayOpen(locked,com.jarvis.assistant.MainActivity.isVisible,
            com.jarvis.assistant.sentinel.JarvisWallpaperService.homeVisible,screenAccess,
            if(screenAccess)com.jarvis.assistant.tools.JarvisAccessibilityService.activePackage() else null,launcher)
        if(allowed)runCatching{startActivity(launch)}
    }

    private fun deliver(command: String, serial: Long) {
        if (paused || !window.accept(serial)) return
        main.removeCallbacks(windowTimeout)
        paused = true
        val speakerId=runCatching{speakerBuffer.take()?.let(com.jarvis.assistant.omnisystem.SpeakerSamples::deposit)}.getOrNull()
        publish(language.choose("Processing your command · microphone paused", "Procesando tu petición · micrófono en pausa", "در حال پردازش دستور · میکروفن مکث شده"), false)
        sendBroadcast(Intent(ACTION_COMMAND).setPackage(packageName).putExtra(EXTRA_TEXT, command).putExtra(EXTRA_LANGUAGE, language.code).putExtra("speaker_sample_id",speakerId ?: -1L))
    }

    private fun listeningText() = language.choose("Listening for “${language.wakePhrase}” · on-device", "Escuchando “${language.wakePhrase}” · local", "در انتظار «${language.wakePhrase}» · روی دستگاه")

    private fun publish(text: String, active: Boolean, updateNotification: Boolean = true, awaitingCommand: Boolean = false, interrupted:Boolean=false) {
        mutableState.value = WakeState(running = running, listening = active, status = text, language = language, awaitingCommand = awaitingCommand, commandDeadlineElapsed = window.snapshot().deadline, waitingForCapture=window.snapshot().mode==ConversationWindow.Mode.WAITING_FOR_CAPTURE,interrupted=interrupted)
        sendBroadcast(Intent(ACTION_STATUS).setPackage(packageName).putExtra(EXTRA_STATUS, text).putExtra(EXTRA_LANGUAGE, language.code))
        if (updateNotification) {
            val manager=getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID,notification(text))
            when(window.snapshot().mode){
                ConversationWindow.Mode.WAKE_ONLY,ConversationWindow.Mode.OFF->manager.cancel(3102)
                ConversationWindow.Mode.PAUSED->updateActivationNotification("Command accepted · microphone paused")
                ConversationWindow.Mode.WAITING_FOR_CAPTURE->updateActivationNotification("Preparing your follow-up window")
                ConversationWindow.Mode.FOLLOW_UP->updateActivationNotification("Follow-up · up to 10 seconds, no wake phrase needed")
                else->Unit
            }
        }
    }

    private fun updateActivationNotification(text:String){
        val launch=Intent(this,com.jarvis.assistant.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("sentinel_wake",true)
        val presentation=if(getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked)Intent(this,com.jarvis.assistant.neural.SafeActivationActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP) else launch
        val open=PendingIntent.getActivity(this,2,presentation,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        getSystemService(NotificationManager::class.java).notify(3102,NotificationCompat.Builder(this,"jarvis_activation").setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Jarvis").setContentText(text).setContentIntent(open).setAutoCancel(true).setSilent(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build())
    }

    private fun notification(text: String): android.app.Notification {
        val launch = packageManager.getLaunchIntentForPackage(packageName)?.apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val open = launch?.let { PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
        val stop = PendingIntent.getService(this, 1, Intent(this, WakeListenerService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Jarvis")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setOngoing(true).setSilent(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .addAction(android.R.drawable.ic_media_pause, language.choose("Stop everything", "Detener todo", "توقف همه‌چیز"), stop)
            .build()
    }

    private fun stopCaptureImmediately(){
        synchronized(audioLock){running=false;paused=true;window.disable();runCatching{recorder?.stop()}}
        main.removeCallbacks(windowTimeout)
        mutableState.value=WakeState(status=failureReason ?: language.choose("Wake listening is off","Activación desactivada","بیدارباش خاموش است"),language=language)
    }

    override fun onDestroy() {
        stopCaptureImmediately()
        if(current.get()===this)current.clear()
        main.removeCallbacks(renewCpu)
        getSystemService(NotificationManager::class.java).cancel(3102)
        main.removeCallbacks(windowTimeout)
        window.disable()
        cpuLock?.let { if (it.isHeld) it.release() }; cpuLock = null
        synchronized(audioLock) { running = false; paused = true; runCatching { recorder?.stop() } }
        worker?.interrupt(); worker = null
        mutableState.value = WakeState(status = failureReason ?: language.choose("Wake listening is off", "Activación desactivada", "بیدارباش خاموش است"), language = language)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    data class WakeState(val running: Boolean = false, val listening: Boolean = false, val status: String = "", val language: AppLanguage = AppLanguage.EN, val awaitingCommand: Boolean = false, val commandDeadlineElapsed: Long = 0, val waitingForCapture:Boolean=false,val interrupted:Boolean=false)
    companion object {
        const val ACTION_START = "com.jarvis.next.WAKE_START"
        const val ACTION_STOP = "com.jarvis.next.WAKE_STOP"
        const val ACTION_PAUSE = "com.jarvis.next.WAKE_PAUSE"
        const val ACTION_RESUME = "com.jarvis.next.WAKE_RESUME"
        const val ACTION_PARTIAL = "com.jarvis.next.WAKE_PARTIAL"
        const val ACTION_COMMAND = "com.jarvis.next.WAKE_COMMAND"
        const val ACTION_STATUS = "com.jarvis.next.WAKE_STATUS"
        const val EXTRA_TEXT = "text"
        const val EXTRA_LANGUAGE = "language"
        const val EXTRA_STATUS = "status"
        @Volatile private var current=java.lang.ref.WeakReference<WakeListenerService>(null)
        fun stopImmediately(){current.get()?.stopCaptureImmediately()}
        private const val CHANNEL = "jarvis_wake"
        private const val NOTIFICATION_ID = 3101
        @Volatile private var suppressionUntil = 0L
        @Volatile private var speechActive=false
        fun setSpeechActive(value:Boolean){speechActive=value;if(!value)suppressFor(250)}
        private val mutableState = MutableStateFlow(WakeState())
        val state: StateFlow<WakeState> = mutableState
        fun suppressFor(milliseconds: Long) { suppressionUntil = SystemClock.elapsedRealtime() + milliseconds.coerceAtLeast(0) }
        fun disableRecovery(context: Context) { context.getSharedPreferences("wake_session", MODE_PRIVATE).edit().putBoolean("enabled", false).apply() }
    }
}
