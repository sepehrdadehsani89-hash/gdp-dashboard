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
    @Volatile private var languageChangeTicket=0L
    private var pickupMonitor:com.jarvis.assistant.pickup.PickupMonitor?=null
    @Volatile private var capture:ManagedMicrophone?=null
    private val speakerBuffer=com.jarvis.assistant.omnisystem.SpeakerSampleBuffer()
    private val speakerFocus by lazy{com.jarvis.assistant.phantom.SpeakerFocusEngine((application as com.jarvis.assistant.JarvisApplication).runtime.speakers,SystemClock::elapsedRealtime)}
    @Volatile private var failureReason:String?=null
    private var language = AppLanguage.EN
    @Volatile private var playbackGeneration:Long?=null
    @Volatile private var playbackEcho=""
    @Volatile private var playbackControlPending:Long?=null
    private fun clearPlaybackControls(){playbackGeneration=null;playbackEcho="";playbackControlPending=null}
    private val window = ConversationWindow { SystemClock.elapsedRealtime() }
    private var greetingTicket=0L
    private val greetingGate=WakeGreetingGate(SystemClock::elapsedRealtime)
    private val windowTimeout = Runnable { expireCommandWindow() }
    private fun expireCommandWindow():Boolean {
        if(!window.expire())return false
        (application as com.jarvis.assistant.JarvisApplication).runtime.voiceSession.idle(windowRuntimeGeneration)
        val call=(application as com.jarvis.assistant.JarvisApplication).runtime.aiCall.state.value
        if(call.connected)main.post{(application as com.jarvis.assistant.JarvisApplication).runtime.endAiCall(call.id,"QUIET_TIMEOUT")}
        publish(listeningText(),true);return true
    }
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
            ACTION_REFRESH_PICKUP -> {
                pickupMonitor?.close();pickupMonitor=null
                if(running)pickupMonitor=com.jarvis.assistant.pickup.PickupMonitor(this).also{it.start()}
                return if(running)START_STICKY else START_NOT_STICKY
            }
            ACTION_CHANGE_LANGUAGE -> {
                val target=AppLanguage.fromCode(intent.getStringExtra(EXTRA_LANGUAGE).orEmpty())
                if(running && target!=language)restartLanguage(target,startId)
                return START_STICKY
            }
            ACTION_STOP -> {
                disableRecovery(this)
                sendBroadcast(Intent(ACTION_STOP).setPackage(packageName))
                stopSelf(); return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                clearPlaybackControls()
                synchronized(audioLock) { paused = true; window.pause(); main.removeCallbacks(windowTimeout); runCatching { recorder?.stop() } }
                publish(language.choose("Wake listening paused", "Activación en pausa", "بیدارباش مکث شده است"), false)
                return if (running) START_STICKY else START_NOT_STICKY
            }
            ACTION_RESUME -> {
                clearPlaybackControls()
                if (running) {
                    window.callMode((application as com.jarvis.assistant.JarvisApplication).runtime.aiCall.state.value.connected)
                    window.resume(intent.getBooleanExtra("follow_up", false))
                    main.removeCallbacks(windowTimeout)
                    paused = false
                    publish(if (window.snapshot().mode == ConversationWindow.Mode.WAITING_FOR_CAPTURE)
                        language.choose("Preparing an adaptive follow-up session", "Preparando tu siguiente pregunta", "آماده‌سازی فرصت ۱۰ ثانیه‌ای پرسش بعدی") else listeningText(), true)
                }
                return if (running) START_STICKY else START_NOT_STICKY
            }
            ACTION_PLAYBACK_CONTROLS -> {
                if(!running){stopSelf();return START_NOT_STICKY}
                val runtime=(application as com.jarvis.assistant.JarvisApplication).runtime
                val expected=intent.getLongExtra("runtime_generation",-1)
                if(!PlaybackInterruptPolicy.current(expected,runtime.currentCommandSerial(),runtime.state.value.phase==com.jarvis.assistant.core.AssistantPhase.SPEAKING) ||
                    !com.jarvis.assistant.titan.VoicePreferences(this).playbackInterruption || com.jarvis.assistant.security.SystemCredentialPrivacy.pending(this))return START_STICKY
                playbackEcho=intent.getStringExtra("spoken_segment").orEmpty().take(8000);playbackGeneration=expected;playbackControlPending=null
                window.pause();main.removeCallbacks(windowTimeout);paused=false
                publish("Reply playing · listening only for wait / stop speech controls",true)
                return START_STICKY
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
        running = true; paused = intent?.getBooleanExtra("start_paused",false)==true; window.enable();if(paused)window.pause()
        pickupMonitor?.close();pickupMonitor=com.jarvis.assistant.pickup.PickupMonitor(this).also{it.start()}
        publish(text,false)
        recovery.edit().putBoolean("enabled", true).putString("language", language.code).apply()
        cpuLock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Jarvis:WakeListening").apply { setReferenceCounted(false) }
        renewCpu.run()
        worker = Thread({
            val recovery=com.jarvis.assistant.rebirth.RecoveryBudget(now=SystemClock::elapsedRealtime)
            while(running){try {
                RecognitionModelCache.acquire(modelDirectory).use { modelReference ->
                    val model=modelReference.model
                    while (running) {
                        if (paused || com.jarvis.assistant.security.SystemCredentialPrivacy.pending(this)) { Thread.sleep(100); continue }
                        if(audioManager.mode in listOf(android.media.AudioManager.MODE_IN_CALL,android.media.AudioManager.MODE_IN_COMMUNICATION)){
                            if(window.snapshot().mode!=ConversationWindow.Mode.WAKE_ONLY){window.resume(false);main.removeCallbacks(windowTimeout)}
                            publish("Microphone paused during a call",false,interrupted=true);Thread.sleep(500);continue
                        }
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) throw SecurityException("Microphone permission was revoked.")
                        val managed=ManagedMicrophone.open(this,com.jarvis.assistant.rebirth.MicrophoneOwner.WAKE)
                        if(managed==null){publish("Waiting for the previous microphone to release",false);Thread.sleep(100);continue}
                        capture=managed
                        val audio=managed.audio;recorder=audio
                        val silenced=java.util.concurrent.atomic.AtomicBoolean(false)
                        utterance=UtteranceAssembler(SystemClock::elapsedRealtime,com.jarvis.assistant.titan.VoicePreferences(this).energy)
                        managed.registerCallback(mainExecutor,object:android.media.AudioManager.AudioRecordingCallback(){
                            override fun onRecordingConfigChanged(configs:MutableList<android.media.AudioRecordingConfiguration>){
                                if(capture!==managed || !running)return
                                if(configs.any { it.clientAudioSessionId==audio.audioSessionId && it.isClientSilenced }){
                                    silenced.set(true)
                                    publish("Android silenced capture · releasing before a bounded retry",false,interrupted=true)
                                    runCatching{audio.stop()}
                                }
                            }
                        })
                        check(audio.state == AudioRecord.STATE_INITIALIZED) { "Microphone is unavailable." }
                        val focusProfiles=(application as com.jarvis.assistant.JarvisApplication).runtime.speakers
                        val focusedAudio=SpeakerAudioFocus(focusProfiles.liveFocusReady(),focusProfiles.firstSpeakerFocus,focusProfiles::liveEmbedding)
                        try {
                            Recognizer(model, 16000f).use { recognizer ->
                                recognizer.setWords(true);recognizer.setMaxAlternatives(3)
                                val started = synchronized(audioLock) {
                                    if (running && !paused) { audio.startRecording(); true } else false
                                }
                                if (!started) return@use
                                com.jarvis.assistant.hud.AudioSignals.start(audio,"Wake microphone · PCM RMS")
                                publish(if(playbackGeneration!=null)"Reply playing · wait / stop speech controls" else listeningText(), true)
                                val bytes = ByteArray(4096)
                                var suppressed = false
                                var recognizerSerial = window.snapshot().resetSerial
                                while (running && !paused && !com.jarvis.assistant.security.SystemCredentialPrivacy.pending(this)) {
                                    val count = audio.read(bytes, 0, bytes.size)
                                    if (paused || !running || com.jarvis.assistant.security.SystemCredentialPrivacy.pending(this)) break
                                    check(!silenced.get()){ "Android silenced microphone capture." }
                                    check(count >= 0) { "Microphone capture was interrupted." }
                                    if (count == 0) continue
                                    recovery.healthy()
                                    if(audioManager.mode in listOf(android.media.AudioManager.MODE_IN_CALL,android.media.AudioManager.MODE_IN_COMMUNICATION)){window.resume(false);main.removeCallbacks(windowTimeout);publish("Microphone paused during a call",false,interrupted=true);break}
                                    com.jarvis.assistant.hud.AudioSignals.level(audio,com.jarvis.assistant.hud.PcmLevel.pcm16(bytes,count))
                                    val playbackExpected=playbackGeneration
                                    if(playbackExpected!=null){
                                        speakerBuffer.clear()
                                        if(!managed.processing.echoCancellationActive){
                                            clearPlaybackControls()
                                            synchronized(audioLock){paused=true;window.pause();runCatching{audio.stop()}}
                                            publish("Playback controls paused: Android echo cancellation is unavailable; use Stop",false)
                                            break
                                        }
                                        if(!speechActive){recognizer.reset();continue}
                                        if(recognizer.acceptWaveForm(bytes,count)){
                                            val result=com.jarvis.assistant.rebirth.RecognitionPayload.vosk(JSONObject(recognizer.result)).primary
                                            val control=if(RecognitionConfidence.acceptsFinal(result,.65))PlaybackInterruptPolicy.command(result.optString("text"),playbackEcho)else null
                                            if(control!=null && playbackControlPending!=playbackExpected){
                                                playbackControlPending=playbackExpected
                                                main.post{
                                                    val runtime=(application as com.jarvis.assistant.JarvisApplication).runtime
                                                    if(running && playbackGeneration==playbackExpected && PlaybackInterruptPolicy.current(playbackExpected,runtime.currentCommandSerial(),runtime.state.value.phase==com.jarvis.assistant.core.AssistantPhase.SPEAKING)){
                                                        clearPlaybackControls();synchronized(audioLock){paused=true;window.pause();runCatching{recorder?.stop()}}
                                                        runtime.interruptSpokenReply(playbackExpected)
                                                    }else if(playbackControlPending==playbackExpected)playbackControlPending=null
                                                }
                                            }
                                        }
                                        continue
                                    }
                                    if (speechActive || SystemClock.elapsedRealtime() < suppressionUntil) {
                                        speakerBuffer.clear()
                                        if (!suppressed) { recognizer.reset(); suppressed = true }
                                        continue
                                    }
                                    if (suppressed) { recognizer.reset(); suppressed = false }
                                    val serial=window.snapshot().resetSerial
                                    if(greetingGate.due(serial) && window.snapshot().mode==ConversationWindow.Mode.COMMAND)acknowledgeWake(serial)
                                    if(paused || !running)break
                                    if (window.captureReady()) {
                                        windowRuntimeGeneration=(application as com.jarvis.assistant.JarvisApplication).runtime.currentCommandSerial()
                                        (application as com.jarvis.assistant.JarvisApplication).runtime.voiceSession.captureReady(windowRuntimeGeneration,true)
                                        (application as com.jarvis.assistant.JarvisApplication).runtime.phantom.apply{begin(true);event(com.jarvis.assistant.phantom.TraceEvent.CAPTURE_STARTED)}
                                        utterance.reset();
                                        publish(language.choose("Follow-up · adaptive inactivity", "Siguiente pregunta · inactividad adaptativa", "پرسش بعدی · ۱۰ ثانیه گوش می‌دهم"), true, awaitingCommand = true)
                                        scheduleTimeout()
                                    }
                                    val focusedPackets=focusedAudio.packets(bytes,count,window.acceptsCommand(serial))
                                    try{for(packet in focusedPackets){
                                        if(paused || !running)break
                                        val bytes=packet.pcm;val count=packet.count
                                    // Opt-in tap of this existing authorized recorder; never a second microphone.
                                    runCatching{val profiles=(application as com.jarvis.assistant.JarvisApplication).runtime.speakers;speakerBuffer.feed(bytes,count,profiles.ownerOnlyRequested || profiles.autoEnabled || profiles.liveFocusReady())}.onFailure{speakerBuffer.clear()}
                                    if (expireCommandWindow()) { recognizer.reset(); recognizerSerial = window.snapshot().resetSerial; continue }
                                    speakerFocus.setSerial(serial)
                                    for(decision in speakerFocus.drain()){
                                        if(!window.acceptsCommand(decision.serial))continue
                                        if(decision.accepted){
                                            utterance.segment(decision.text,keepPartial=true)
                                            if(!utterance.hasUnfinishedSpeech && !speakerFocus.hasPending(decision.serial) && UtteranceAssembler.fastCommand(utterance.text()))deliver(utterance.text(),decision.serial)
                                        }
                                        else{
                                            rejectHeardCommand(decision.serial,speakerFailure=true)
                                        }
                                    }
                                    if (recognizerSerial != serial) { recognizer.reset(); recognizerSerial = serial; utterance.reset() }

                                    if(window.acceptsCommand(serial))utterance.energy(com.jarvis.assistant.hud.PcmLevel.pcm16(bytes,count))
                                    if(window.speechProgress("",com.jarvis.assistant.hud.PcmLevel.pcm16(bytes,count)))scheduleTimeout()
                                    val final = recognizer.acceptWaveForm(bytes, count)
                                    val payload=if(final)com.jarvis.assistant.rebirth.RecognitionPayload.vosk(JSONObject(recognizer.result)) else null
                                    val json=payload?.primary ?: JSONObject(recognizer.partialResult)
                                    val text=json.optString(if(final)"text" else "partial")
                                    if(window.speechProgress(text))scheduleTimeout()
                                    if(!final && window.acceptsCommand(serial)){
                                        val partial=WakePhraseMatcher.extractCommand(text,language) ?: text
                                        utterance.partial(partial)
                                        sendBroadcast(Intent(ACTION_PARTIAL).setPackage(packageName).putExtra(EXTRA_TEXT,utterance.text()).putExtra("runtime_generation",windowRuntimeGeneration))
                                    }
                                    if(payload!=null)consumeFinal(payload,serial,false) else handleTranscript(text,false,serial)
                                    if(greetingGate.due(serial) && window.snapshot().mode==ConversationWindow.Mode.COMMAND)acknowledgeWake(serial)
                                    if(window.acceptsCommand(serial) && utterance.needsFinalization()){
                                        consumeFinal(com.jarvis.assistant.rebirth.RecognitionPayload.vosk(JSONObject(recognizer.finalResult)),serial,true)
                                        recognizer.reset()
                                    }
                                    if(window.acceptsCommand(serial) && !speakerFocus.hasPending(serial) && (utterance.ready() || !utterance.hasUnfinishedSpeech && UtteranceAssembler.fastCommand(utterance.acceptedText())))deliver(utterance.acceptedText(),serial)
                                    }}finally{focusedPackets.filter{it.owned}.forEach{it.pcm.fill(0)}}
                                }
                            }
                        } finally { focusedAudio.close();speakerBuffer.clear();com.jarvis.assistant.hud.AudioSignals.stop(audio); runCatching { audio.stop() }; capture?.close();capture=null;recorder=null }
                    }
                }
            } catch (_: InterruptedException) {
                break
            } catch (error: Throwable) {
                if(!running)break
                capture?.close();capture=null
                val focusFailure=error.message?.startsWith("SPEAKER_FOCUS_")==true
                val allowed=running && !paused && !focusFailure && error !is SecurityException && error !is LinkageError && error !is OutOfMemoryError && ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED && !com.jarvis.assistant.security.SystemCredentialPrivacy.pending(this)
                val wait=if(allowed)recovery.retryDelay() else null
                if(wait!=null){
                    publish("Recovering offline recognition · bounded retry",false,interrupted=true)
                    try{Thread.sleep(wait)}catch(_:InterruptedException){break}
                }else{
                    failureReason=if(focusFailure)"Local speaker focus stopped: model unavailable or too slow. Check the model or disable first-speaker focus, then restart listening. No command was executed." else "Offline listening stopped. Check microphone permission, input route and model, then tap Start listening to retry."
                    publish(failureReason!!,false,interrupted=true);main.post{stopSelf()};break
                }
            }finally{capture?.close();capture=null}}
        }, "JarvisWakeListener").also { it.start() }
        return START_STICKY
    }

    private fun scheduleTimeout() {
        main.removeCallbacks(windowTimeout)
        val deadline = window.snapshot().deadline
        if (deadline > 0) main.postDelayed(windowTimeout, (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0))
    }

    @Volatile private var windowRuntimeGeneration=-1L
    private var alternatives:List<com.jarvis.assistant.rebirth.RecognitionHypothesis> = emptyList()
    private var utterance=UtteranceAssembler(SystemClock::elapsedRealtime)
    private fun consumeFinal(payload:com.jarvis.assistant.rebirth.RecognitionPayload,serial:Long,endpoint:Boolean){
        if(serial!=window.snapshot().resetSerial)return
        val text=payload.primary.optString("text")
        val confidence=com.jarvis.assistant.titan.VoicePreferences(this).confidence.toDouble()
        RecognitionDiagnostics.record(payload.primary,confidence,"wake/${language.code}")
        val kind=RecognitionConfidence.kind(payload.primary,confidence)
        if(kind==FinalRecognitionKind.EMPTY){
            utterance.emptyFinal(endpoint)
            return
        }
        if(kind==FinalRecognitionKind.UNCERTAIN){
            if(window.acceptsCommand(serial) && (text.isNotBlank() || utterance.hasUnfinishedSpeech))rejectHeardCommand(serial)
            return
        }
        alternatives=payload.alternatives
        handleTranscript(text,true,serial)
        if(endpoint)utterance.finalizedEndpoint()
    }
    private fun rejectHeardCommand(serial:Long,speakerFailure:Boolean=false){
        if(paused || !window.accept(serial))return
        paused=true;utterance.reset();speakerFocus.reset();greetingGate.cancel();main.removeCallbacks(windowTimeout)
        val expected=windowRuntimeGeneration
        publish(if(speakerFailure)"Speaker matching unavailable or uncertain · no command executed" else "Speech recognition needs a clearer command",false)
        main.post{val runtime=(application as com.jarvis.assistant.JarvisApplication).runtime
            if(running && runtime.currentCommandSerial()==expected){if(speakerFailure)runtime.speakerMatchUnavailable(expected)else runtime.recognitionUnclear(expected)}
        }
    }
    private fun handleTranscript(text: String, final: Boolean, serial: Long) {
        if (text.isBlank() || serial != window.snapshot().resetSerial) return
        // A result recognized before expiry must not reactivate itself after the deadline.
        if (expireCommandWindow()) return
        val afterWake = WakePhraseMatcher.extractCommand(text, language)
        if (afterWake != null) {
            if (window.wake()) {
                greetingTicket=VoiceSessionStateMachine.nextWakeTicket()
                if(afterWake.isBlank())greetingGate.arm(serial) else greetingGate.cancel()
                (application as com.jarvis.assistant.JarvisApplication).runtime.phantomOverlay.reveal()
                windowRuntimeGeneration=(application as com.jarvis.assistant.JarvisApplication).runtime.currentCommandSerial()
                speakerFocus.newWake()
                (application as com.jarvis.assistant.JarvisApplication).runtime.phantom.apply{begin(true);event(com.jarvis.assistant.phantom.TraceEvent.WAKE_DETECTED)}
                activateHud()
                publish(language.choose("Command window · adaptive inactivity", "Tu petición · inactividad adaptativa", "فرصت دستور · ۱۰ ثانیه بی‌گفتاری"), true, awaitingCommand = true)
                scheduleTimeout()
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            }
            if(afterWake.isNotBlank())greetingGate.cancel()
            if(final && afterWake.isBlank() && window.snapshot().mode==ConversationWindow.Mode.COMMAND)acknowledgeWake(serial)
            else if(final && afterWake.isNotBlank())segmentCommand(afterWake,serial)
        } else if(window.acceptsCommand(serial)){
            greetingGate.cancel()
            if(final)segmentCommand(text,serial)
        }
    }
    private fun acknowledgeWake(serial:Long){
        if(paused || !window.accept(serial))return
        greetingGate.cancel();paused=true;main.removeCallbacks(windowTimeout)
        val sample=runCatching{speakerBuffer.take(.002f)?.let(com.jarvis.assistant.omnisystem.SpeakerSamples::deposit)}.getOrNull()
        val expected=windowRuntimeGeneration
        val ticket=greetingTicket
        main.post{val runtime=(application as com.jarvis.assistant.JarvisApplication).runtime;if(running && runtime.currentCommandSerial()==expected)runtime.greetWake(sample,ticket)}
    }
    private fun segmentCommand(text:String,serial:Long){
        if((application as com.jarvis.assistant.JarvisApplication).runtime.speakers.let{it.focusEnabled && !it.ownerOnlyRequested && !it.liveFocusReady()}){
            utterance.clearPartial()
            if(!speakerFocus.offer(text,serial,speakerBuffer.take()))rejectHeardCommand(serial,speakerFailure=true)
            return
        }
        utterance.segment(text)
        if(UtteranceAssembler.fastCommand(utterance.text()))deliver(utterance.text(),serial)
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
        // Wake recognition updates the native reactor and notification only. It never
        // opens generic Main before the user's actual command has been resolved.

    }

    private fun deliver(command: String, serial: Long) {
        val profiles=(application as com.jarvis.assistant.JarvisApplication).runtime.speakers
        if(profiles.ownerOnlyRequested && !speakerBuffer.hasEnoughAudio())return
        if (paused || !window.accept(serial)) return
        main.removeCallbacks(windowTimeout)
        paused = true
        val speakerId=runCatching{speakerBuffer.take(if(profiles.ownerOnlyRequested).002f else .008f)?.let(com.jarvis.assistant.omnisystem.SpeakerSamples::deposit)}.getOrNull()
        publish(language.choose("Processing your command · microphone paused", "Procesando tu petición · micrófono en pausa", "در حال پردازش دستور · میکروفن مکث شده"), false)
        sendBroadcast(Intent(ACTION_COMMAND).setPackage(packageName).putExtra(EXTRA_TEXT, command).putExtra("runtime_generation",windowRuntimeGeneration).putExtra(EXTRA_LANGUAGE, language.code).putExtra("speaker_sample_id",speakerId ?: -1L).putStringArrayListExtra("recognition_alternatives",ArrayList(alternatives.take(5).map{it.text})))
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
                ConversationWindow.Mode.FOLLOW_UP->updateActivationNotification("Follow-up · adaptive inactivity, no wake phrase needed")
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

    /** Retain the visible foreground service; fully close the old decoder/recorder first. */
    private fun restartLanguage(target:AppLanguage,startId:Int){
        val ticket=++languageChangeTicket
        getSharedPreferences("wake_session",MODE_PRIVATE).edit().putString("language",target.code).apply()
        pickupMonitor?.close();pickupMonitor=null
        clearPlaybackControls();greetingGate.cancel();speakerFocus.reset()
        synchronized(audioLock){running=false;paused=true;window.disable();runCatching{recorder?.stop()}}
        main.removeCallbacks(windowTimeout);main.removeCallbacks(renewCpu)
        cpuLock?.let{if(it.isHeld)it.release()};cpuLock=null
        publish("Changing offline recognition language · releasing previous microphone",false)
        val previous=worker;previous?.interrupt()
        Thread({
            try{previous?.join(3500)}catch(_:InterruptedException){return@Thread}
            main.post{
                if(current.get()!==this || ticket!=languageChangeTicket)return@post
                if(previous?.isAlive==true){failureReason="Previous speech engine could not close. Listening stopped; open Voice health and retry.";stopSelf();return@post}
                worker=null
                onStartCommand(Intent(this,WakeListenerService::class.java).setAction(ACTION_START).putExtra(EXTRA_LANGUAGE,target.code).putExtra("start_paused",true),0,startId)
            }
        },"JarvisLanguageHandoff").start()
    }

    private fun stopCaptureImmediately(){
        ++languageChangeTicket
        pickupMonitor?.close();pickupMonitor=null
        clearPlaybackControls()
        greetingGate.cancel()
        speakerFocus.reset()
        synchronized(audioLock){running=false;paused=true;window.disable();runCatching{recorder?.stop()}}
        main.removeCallbacks(windowTimeout)
        mutableState.value=WakeState(status=failureReason ?: language.choose("Wake listening is off","Activación desactivada","بیدارباش خاموش است"),language=language)
    }

    override fun onDestroy() {
        stopCaptureImmediately()
        speakerFocus.close()
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
        const val ACTION_REFRESH_PICKUP="com.jarvis.next.WAKE_REFRESH_PICKUP"
        const val ACTION_CHANGE_LANGUAGE="com.jarvis.next.WAKE_CHANGE_LANGUAGE"
        const val ACTION_START = "com.jarvis.next.WAKE_START"
        const val ACTION_STOP = "com.jarvis.next.WAKE_STOP"
        const val ACTION_PAUSE = "com.jarvis.next.WAKE_PAUSE"
        const val ACTION_RESUME = "com.jarvis.next.WAKE_RESUME"
        const val ACTION_PLAYBACK_CONTROLS = "com.jarvis.next.WAKE_PLAYBACK_CONTROLS"
        const val ACTION_PARTIAL = "com.jarvis.next.WAKE_PARTIAL"
        const val ACTION_COMMAND = "com.jarvis.next.WAKE_COMMAND"
        const val ACTION_STATUS = "com.jarvis.next.WAKE_STATUS"
        const val EXTRA_TEXT = "text"
        const val EXTRA_LANGUAGE = "language"
        const val EXTRA_STATUS = "status"
        fun clearSpeakerFocus(){current.get()?.speakerFocus?.reset()}
        @Volatile private var current=java.lang.ref.WeakReference<WakeListenerService>(null)
        fun stopImmediately(){current.get()?.stopCaptureImmediately()}
        private const val CHANNEL = "jarvis_wake"
        private const val NOTIFICATION_ID = 3101
        @Volatile private var suppressionUntil = 0L
        fun pauseImmediately(){current.get()?.let{service->
            service.clearPlaybackControls()
            synchronized(service.audioLock){service.paused=true;service.window.pause();service.main.removeCallbacks(service.windowTimeout);runCatching{service.recorder?.stop()}}
            service.speakerBuffer.clear()
        }}
        @Volatile private var speechActive=false
        fun setSpeechActive(value:Boolean){speechActive=value;if(!value)suppressFor(250)}
        private val mutableState = MutableStateFlow(WakeState())
        val state: StateFlow<WakeState> = mutableState
        fun suppressFor(milliseconds: Long) { suppressionUntil = SystemClock.elapsedRealtime() + milliseconds.coerceAtLeast(0) }
        fun disableRecovery(context: Context) { context.getSharedPreferences("wake_session", MODE_PRIVATE).edit().putBoolean("enabled", false).apply() }
    }
}
