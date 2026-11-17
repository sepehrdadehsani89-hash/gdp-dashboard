package com.jarvis.assistant.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** One bounded command captured locally; no SpeechRecognizer service or network involved. */
internal class OfflineSpeechSession(
    private val context: Context,
    private val modelDirectory: File,
    private val onPartial: (String) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onCaptureStarted:()->Unit={},
    private val onAlternatives:(List<com.jarvis.assistant.rebirth.RecognitionHypothesis>)->Unit={},
    private val captureSpeaker:Boolean=false,
    private val onSpeakerSample:(Long)->Unit={},
) {
    private val closed = AtomicBoolean(false)
    private val audioLock = Any()
    private val main = Handler(Looper.getMainLooper())
    private val preferences=com.jarvis.assistant.titan.VoicePreferences(context)
    private val timing=SpeechInactivityClock(android.os.SystemClock::elapsedRealtime,preferences.energy)
    private val utterance=UtteranceAssembler(android.os.SystemClock::elapsedRealtime,preferences.energy,preferences.endpointPauseMs)
    private val diagnosticToken=EndpointDiagnostics.begin("Vosk foreground",android.os.SystemClock.elapsedRealtime())
    @Volatile private var finalRequestedAt:Long?=null
    private val watchdog=object:Runnable{override fun run(){
        if(closed.get())return
        val at=android.os.SystemClock.elapsedRealtime()
        if(finalRequestedAt?.let{at-it>=2500}==true){emitError("SPEECH_UNCLEAR: The local decoder did not finish. Please retry.","FINAL_TIMEOUT");return}
        if(timing.expired()){emitError("Speech inactivity or one-minute utterance limit reached. Please retry.","CAPTURE_TIMEOUT");return}
        main.postDelayed(this,100)
    }}
    @Volatile private var capture:ManagedMicrophone?=null
    @Volatile private var recorder: AudioRecord? = null
    private val speakerSample=com.jarvis.assistant.omnisystem.SpeakerSampleBuffer()

    fun start() {
        main.post(watchdog)
        Thread({
            val profiles=(context.applicationContext as com.jarvis.assistant.JarvisApplication).runtime.speakers
            val focusedAudio=SpeakerAudioFocus(profiles.liveFocusReady(),profiles.firstSpeakerFocus,profiles::liveEmbedding)
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) throw SecurityException("Microphone permission is required.")
                RecognitionModelCache.acquire(modelDirectory).use { modelReference ->
                    val model=modelReference.model
                    if (closed.get()) return@Thread
                    Recognizer(model, 16000f).use { recognizer ->
                        recognizer.setWords(true);recognizer.setMaxAlternatives(3)
                        var opened:ManagedMicrophone?=null
                        for(attempt in 0..15){
                            if(closed.get())return@Thread
                            opened=ManagedMicrophone.open(context,com.jarvis.assistant.rebirth.MicrophoneOwner.FOREGROUND_RECOGNIZER)
                            if(opened!=null)break
                            Thread.sleep(100)
                        }
                        val managed=opened ?: error("RECOGNIZER_BUSY: the previous microphone is still releasing. Try again.")
                        capture=managed
                        val audio=managed.audio;recorder=audio
                        managed.registerCallback(context.mainExecutor,object:android.media.AudioManager.AudioRecordingCallback(){
                            override fun onRecordingConfigChanged(configs:MutableList<android.media.AudioRecordingConfiguration>){
                                if(capture!==managed || closed.get())return
                                if(configs.any { it.clientAudioSessionId==audio.audioSessionId && it.isClientSilenced }){
                                    emitError("Android silenced this microphone. Check calls and other recording apps.")
                                    runCatching { audio.stop() }
                                }
                            }
                        })
                        check(audio.state == AudioRecord.STATE_INITIALIZED) { "The microphone is unavailable." }
                        try {
                            synchronized(audioLock) {
                                if (closed.get()) return@Thread
                                audio.startRecording()
                                EndpointDiagnostics.captureReady(diagnosticToken,android.os.SystemClock.elapsedRealtime())
                                onCaptureStarted()
                            }
                            com.jarvis.assistant.hud.AudioSignals.start(this, "Offline microphone · PCM RMS")
                            val bytes = ByteArray(4096)
                            timing.start();utterance.reset()
                            val readClock=MicrophoneReadClock(android.os.SystemClock::elapsedRealtime)
                            var previousPartial = ""
                            fun acceptFinal(json:JSONObject,endpoint:Boolean):Boolean {
                                val payload=com.jarvis.assistant.rebirth.RecognitionPayload.vosk(json)
                                main.post{if(!closed.get())onAlternatives(payload.alternatives)}
                                val result=payload.primary
                                val text=result.optString("text")
                                val threshold=com.jarvis.assistant.titan.VoicePreferences(context).confidence.toDouble()
                                RecognitionDiagnostics.record(result,threshold,"foreground")
                                val kind=RecognitionConfidence.kind(result,threshold)
                                if(kind==FinalRecognitionKind.EMPTY){
                                    // Vosk emits empty endpoints for silence/noise. They are not a failed
                                    // command and cannot invalidate an earlier accepted final segment.
                                    if(!utterance.emptyFinal(endpoint)){emitError("SPEECH_UNCLEAR: Part of your request was not recognized. Please repeat the complete request.","UNFINISHED_FINAL");return true}
                                    if(endpoint && utterance.acceptedText().isNotBlank() && (!captureSpeaker || speakerSample.hasEnoughAudio())){emitResult(utterance.acceptedText());return true}
                                    return false
                                }
                                // Never execute a partial or skip a rejected part of a compound request.
                                if(kind==FinalRecognitionKind.UNCERTAIN){
                                    if(text.isNotBlank() || utterance.hasUnfinishedSpeech){emitError("SPEECH_UNCLEAR: Please repeat one request clearly.");return true}
                                    return false
                                }
                                timing.progress(text);utterance.segment(text)
                                if(endpoint)utterance.finalizedEndpoint()
                                if((UtteranceAssembler.fastCommand(utterance.text()) || utterance.ready()) && (!captureSpeaker || speakerSample.hasEnoughAudio())){
                                    emitResult(utterance.text());return true
                                }
                                return false
                            }
                            while (!closed.get() && !timing.expired()) {
                                val count = audio.read(bytes, 0, bytes.size,AudioRecord.READ_NON_BLOCKING)
                                check(count >= 0) { "Microphone capture interrupted." }
                                if (count == 0) {check(!readClock.stalled()){ "Microphone input stalled." };Thread.sleep(12);continue}
                                readClock.progress()
                                com.jarvis.assistant.hud.AudioSignals.level(this,com.jarvis.assistant.hud.PcmLevel.pcm16(bytes,count))
                                val focusedPackets=focusedAudio.packets(bytes,count)
                                try{for(packet in focusedPackets){
                                val bytes=packet.pcm;val count=packet.count
                                val level=com.jarvis.assistant.hud.PcmLevel.pcm16(bytes,count)
                                speakerSample.feed(bytes,count,captureSpeaker)
                                com.jarvis.assistant.hud.AudioSignals.level(this,level);timing.energy(level);utterance.energy(level)
                                if (recognizer.acceptWaveForm(bytes, count)) {
                                    if(acceptFinal(JSONObject(recognizer.result),false))return@Thread
                                } else {
                                    val partial = JSONObject(recognizer.partialResult).optString("partial")
                                    timing.progress(partial);utterance.partial(partial)
                                    EndpointDiagnostics.activity(diagnosticToken,android.os.SystemClock.elapsedRealtime(),utterance.hasUnfinishedSpeech || utterance.acceptedText().isNotBlank())
                                    if (partial != previousPartial) {
                                        previousPartial = partial
                                        main.post { if (!closed.get()) onPartial(utterance.text()) }
                                    }
                                }
                                if(utterance.needsFinalization()){
                                    val at=android.os.SystemClock.elapsedRealtime()
                                    EndpointDiagnostics.candidate(diagnosticToken,at);EndpointDiagnostics.finalizing(diagnosticToken,at);finalRequestedAt=at
                                    val final=JSONObject(recognizer.finalResult)
                                    if(acceptFinal(final,true))return@Thread
                                    finalRequestedAt=null
                                    if(com.jarvis.assistant.rebirth.RecognitionPayload.vosk(final).primary.optString("text").isBlank() && !utterance.ready())EndpointDiagnostics.resumeAfterEmptyBoundary(diagnosticToken,android.os.SystemClock.elapsedRealtime())
                                    // A blank forced final is a decoder boundary, not an ASR error.
                                    recognizer.reset()
                                }
                                if((utterance.ready() || !utterance.hasUnfinishedSpeech && UtteranceAssembler.fastCommand(utterance.acceptedText())) && (!captureSpeaker || speakerSample.hasEnoughAudio())){emitResult(utterance.acceptedText());return@Thread}
                                }}finally{focusedPackets.filter{it.owned}.forEach{it.pcm.fill(0)}}
                            }
                            if (!closed.get()) {
                                emitError("Speech inactivity or one-minute utterance limit reached. Tap the microphone or say your wake phrase again.","CAPTURE_TIMEOUT")
                            }
                        } finally { com.jarvis.assistant.hud.AudioSignals.stop(this); runCatching { audio.stop() }; capture?.close();capture=null;recorder = null }
                    }
                }
            } catch (error: Throwable) {
                if (!closed.get()) emitError(if(error.message?.startsWith("SPEAKER_FOCUS_")==true)"SPEAKER_FOCUS_UNAVAILABLE: Check the local model or disable conversation focus, then restart listening. No command was executed." else if(error is SecurityException)"PERMISSION_REQUIRED: microphone access unavailable." else if(error.message?.startsWith("RECOGNIZER_BUSY")==true)error.message!! else "AUDIO_ERROR: offline recognition stopped. Check input route and model, then retry.")
            }finally{focusedAudio.close();capture?.close();capture=null}
        }, "JarvisOfflineSpeech").start()
    }

    fun close() { main.removeCallbacks(watchdog);EndpointDiagnostics.cancel(diagnosticToken,android.os.SystemClock.elapsedRealtime());synchronized(audioLock) { closed.set(true);speakerSample.clear();runCatching { recorder?.stop() } } }
    private fun emitResult(text: String) {
        val sample=if(captureSpeaker)speakerSample.take(.002f)else null
        main.post {
            if (closed.compareAndSet(false,true)) {
                main.removeCallbacks(watchdog)
                if(!timing.expired()){
                    EndpointDiagnostics.complete(diagnosticToken,android.os.SystemClock.elapsedRealtime())
                    sample?.let{onSpeakerSample(com.jarvis.assistant.omnisystem.SpeakerSamples.deposit(it))}
                    onResult(text)
                }else{sample?.pcm?.fill(0);EndpointDiagnostics.fail(diagnosticToken,android.os.SystemClock.elapsedRealtime(),"CAPTURE_TIMEOUT");onError("Speech inactivity or one-minute utterance limit reached.")}
            }else sample?.pcm?.fill(0)
        }
    }
    private fun emitError(message: String,code:String="AUDIO_ERROR") {speakerSample.clear();main.post { if (closed.compareAndSet(false,true)) {main.removeCallbacks(watchdog);EndpointDiagnostics.fail(diagnosticToken,android.os.SystemClock.elapsedRealtime(),code);runCatching{recorder?.stop()}; onError(message) } } }
}
