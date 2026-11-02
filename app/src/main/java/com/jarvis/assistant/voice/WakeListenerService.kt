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
    private var language = AppLanguage.EN
    private var awakeUntil = 0L
    private var lastActivation = -2000L
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
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Jarvis microphone", NotificationManager.IMPORTANCE_LOW).apply { description = "Visible status for user-enabled offline listening" },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                disableRecovery(this)
                sendBroadcast(Intent(ACTION_STOP).setPackage(packageName))
                stopSelf(); return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                synchronized(audioLock) { paused = true; runCatching { recorder?.stop() } }
                publish(language.choose("Wake listening paused", "Activación en pausa", "بیدارباش مکث شده است"), false)
                return if (running) START_STICKY else START_NOT_STICKY
            }
            ACTION_RESUME -> {
                if (running) {
                    paused = false
                    awakeUntil = if (intent.getBooleanExtra("follow_up", false)) SystemClock.elapsedRealtime() + 12_000 else 0
                    publish(if (awakeUntil > 0) language.choose("Listening for your follow-up", "Escucho tu siguiente pregunta", "در انتظار پرسش بعدی شما") else listeningText(), true)
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
        running = true; paused = false
        recovery.edit().putBoolean("enabled", true).putString("language", language.code).apply()
        cpuLock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Jarvis:WakeListening").apply { setReferenceCounted(false) }
        renewCpu.run()
        worker = Thread({
            try {
                Model(modelDirectory.absolutePath).use { model ->
                    while (running) {
                        if (paused) { Thread.sleep(100); continue }
                        if(audioManager.mode in listOf(android.media.AudioManager.MODE_IN_CALL,android.media.AudioManager.MODE_IN_COMMUNICATION)){
                            publish("Microphone paused during a call",false);Thread.sleep(500);continue
                        }
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) throw SecurityException("Microphone permission was revoked.")
                        val minimum = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                        check(minimum > 0) { "16 kHz microphone capture is not supported." }
                        val audio = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum * 2, 8192))
                        recorder = audio
                        audio.registerAudioRecordingCallback(mainExecutor,object:android.media.AudioManager.AudioRecordingCallback(){
                            override fun onRecordingConfigChanged(configs:MutableList<android.media.AudioRecordingConfiguration>){
                                if(configs.any { it.clientAudioSessionId==audio.audioSessionId && it.isClientSilenced }){
                                    publish("Android silenced microphone capture. Stop other recording or call apps, then reopen Jarvis to retry.",false)
                                    stopSelf()
                                }
                            }
                        })
                        check(audio.state == AudioRecord.STATE_INITIALIZED) { "Microphone is unavailable." }
                        try {
                            Recognizer(model, 16000f).use { recognizer ->
                                val started = synchronized(audioLock) {
                                    if (running && !paused) { audio.startRecording(); true } else false
                                }
                                if (!started) return@use
                                com.jarvis.assistant.hud.AudioSignals.start(audio,"Wake microphone · PCM RMS")
                                publish(listeningText(), true)
                                val bytes = ByteArray(4096)
                                var suppressed = false
                                while (running && !paused) {
                                    val count = audio.read(bytes, 0, bytes.size)
                                    if (paused || !running) break
                                    check(count >= 0) { "Microphone capture was interrupted." }
                                    if (count == 0) continue
                                    if(audioManager.mode in listOf(android.media.AudioManager.MODE_IN_CALL,android.media.AudioManager.MODE_IN_COMMUNICATION)){publish("Microphone paused during a call",false);break}
                                    com.jarvis.assistant.hud.AudioSignals.level(audio,com.jarvis.assistant.hud.PcmLevel.pcm16(bytes,count))
                                    if (SystemClock.elapsedRealtime() < suppressionUntil) {
                                        if (!suppressed) { recognizer.reset(); suppressed = true }
                                        continue
                                    }
                                    if (suppressed) { recognizer.reset(); suppressed = false }
                                    if (awakeUntil > 0 && SystemClock.elapsedRealtime() > awakeUntil) {
                                        awakeUntil = 0; recognizer.reset(); publish(listeningText(), true)
                                    }
                                    val final = recognizer.acceptWaveForm(bytes, count)
                                    val text = JSONObject(if (final) recognizer.result else recognizer.partialResult).optString(if (final) "text" else "partial")
                                    handleTranscript(text, final)
                                }
                            }
                        } finally { com.jarvis.assistant.hud.AudioSignals.stop(audio); runCatching { audio.stop() }; audio.release(); recorder = null }
                    }
                }
            } catch (_: InterruptedException) {
                // Deliberate local shutdown.
            } catch (_: Throwable) {
                if (running) {
                    publish(language.choose("Offline listening stopped. Check the microphone/model and open Jarvis to retry.", "La escucha local se detuvo. Revisa el micrófono o modelo y abre Jarvis.", "گوش‌دادن آفلاین متوقف شد. میکروفن یا مدل را بررسی و جارویس را باز کنید."), false)
                    stopSelf()
                }
            }
        }, "JarvisWakeListener").also { it.start() }
        return START_STICKY
    }

    private fun handleTranscript(text: String, final: Boolean) {
        if (text.isBlank()) return
        val afterWake = WakePhraseMatcher.extractCommand(text, language)
        if (afterWake != null) {
            if (awakeUntil == 0L) {
                val now=SystemClock.elapsedRealtime()
                if(now-lastActivation<1500)return
                lastActivation=now
                activateHud()
                awakeUntil = SystemClock.elapsedRealtime() + 12_000
                publish(language.choose("I'm listening to your command", "Escucho tu petición", "دستور شما را می‌شنوم"), true, awaitingCommand = true)
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            }
            if (final && afterWake.isNotBlank()) deliver(afterWake)
        } else if (final && awakeUntil > SystemClock.elapsedRealtime()) {
            deliver(text)
        }
    }

    private fun activateHud() {
        val launch=Intent(this,com.jarvis.assistant.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("jarvis_activation","Jarvis activation",NotificationManager.IMPORTANCE_DEFAULT))
        val open=PendingIntent.getActivity(this,2,launch,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(3102,NotificationCompat.Builder(this,"jarvis_activation").setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Jarvis is listening").setContentText("Tap to open your HUD. Unlock normally for protected actions.")
            .setContentIntent(open).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setSilent(true).build())
        if(com.jarvis.assistant.hud.HudPreferences(this).read().soundEffects)runCatching {
            val tone=android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION,25)
            tone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP,70);main.postDelayed({tone.release()},150)
        }
        // Android grants a background-activity exception to a user-enabled accessibility service.
        // Always retain the notification fallback; never use a full-screen intent or keyguard bypass.
        val locked=getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked
        if(!locked && com.jarvis.assistant.tools.JarvisAccessibilityService.connected)runCatching{startActivity(launch)}
    }

    private fun deliver(command: String) {
        if(paused)return
        awakeUntil = 0; paused = true
        publish(language.choose("Processing your command · microphone paused", "Procesando tu petición · micrófono en pausa", "در حال پردازش دستور · میکروفن مکث شده"), false)
        sendBroadcast(Intent(ACTION_COMMAND).setPackage(packageName).putExtra(EXTRA_TEXT, command).putExtra(EXTRA_LANGUAGE, language.code))
    }

    private fun listeningText() = language.choose("Listening for “${language.wakePhrase}” · on-device", "Escuchando “${language.wakePhrase}” · local", "در انتظار «${language.wakePhrase}» · روی دستگاه")

    private fun publish(text: String, active: Boolean, updateNotification: Boolean = true, awaitingCommand: Boolean = false) {
        mutableState.value = WakeState(running = running, listening = active, status = text, language = language, awaitingCommand = awaitingCommand)
        sendBroadcast(Intent(ACTION_STATUS).setPackage(packageName).putExtra(EXTRA_STATUS, text).putExtra(EXTRA_LANGUAGE, language.code))
        if (updateNotification) getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
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

    override fun onDestroy() {
        main.removeCallbacks(renewCpu)
        cpuLock?.let { if (it.isHeld) it.release() }; cpuLock = null
        synchronized(audioLock) { running = false; paused = true; awakeUntil = 0; runCatching { recorder?.stop() } }
        worker?.interrupt(); worker = null
        mutableState.value = WakeState(status = language.choose("Wake listening is off", "Activación desactivada", "بیدارباش خاموش است"), language = language)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    data class WakeState(val running: Boolean = false, val listening: Boolean = false, val status: String = "", val language: AppLanguage = AppLanguage.EN, val awaitingCommand: Boolean = false)
    companion object {
        const val ACTION_START = "com.jarvis.next.WAKE_START"
        const val ACTION_STOP = "com.jarvis.next.WAKE_STOP"
        const val ACTION_PAUSE = "com.jarvis.next.WAKE_PAUSE"
        const val ACTION_RESUME = "com.jarvis.next.WAKE_RESUME"
        const val ACTION_COMMAND = "com.jarvis.next.WAKE_COMMAND"
        const val ACTION_STATUS = "com.jarvis.next.WAKE_STATUS"
        const val EXTRA_TEXT = "text"
        const val EXTRA_LANGUAGE = "language"
        const val EXTRA_STATUS = "status"
        private const val CHANNEL = "jarvis_wake"
        private const val NOTIFICATION_ID = 3101
        @Volatile private var suppressionUntil = 0L
        private val mutableState = MutableStateFlow(WakeState())
        val state: StateFlow<WakeState> = mutableState
        fun suppressFor(milliseconds: Long) { suppressionUntil = SystemClock.elapsedRealtime() + milliseconds.coerceAtLeast(0) }
        fun disableRecovery(context: Context) { context.getSharedPreferences("wake_session", MODE_PRIVATE).edit().putBoolean("enabled", false).apply() }
    }
}
