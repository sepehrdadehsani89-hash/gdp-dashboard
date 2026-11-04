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
) {
    private val closed = AtomicBoolean(false)
    private val audioLock = Any()
    private val main = Handler(Looper.getMainLooper())
    private val timing=SpeechInactivityClock(android.os.SystemClock::elapsedRealtime,com.jarvis.assistant.titan.VoicePreferences(context).energy)
    private val utterance=UtteranceAssembler(android.os.SystemClock::elapsedRealtime,com.jarvis.assistant.titan.VoicePreferences(context).energy)
    @Volatile private var recorder: AudioRecord? = null

    fun start() {
        Thread({
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) throw SecurityException("Microphone permission is required.")
                Model(modelDirectory.absolutePath).use { model ->
                    if (closed.get()) return@Thread
                    Recognizer(model, 16000f).use { recognizer ->
                        recognizer.setWords(true)
                        val minimum = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                        check(minimum > 0) { "This microphone does not support 16 kHz recording." }
                        val audio = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum * 2, 8192))
                        recorder = audio
                        val processing=MicrophoneProcessing(context,audio.audioSessionId)
                        audio.registerAudioRecordingCallback(context.mainExecutor,object:android.media.AudioManager.AudioRecordingCallback(){
                            override fun onRecordingConfigChanged(configs:MutableList<android.media.AudioRecordingConfiguration>){
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
                            }
                            com.jarvis.assistant.hud.AudioSignals.start(this, "Offline microphone · PCM RMS")
                            val bytes = ByteArray(4096)
                            timing.start();utterance.reset()
                            var previousPartial = ""
                            while (!closed.get() && !timing.expired()) {
                                val count = audio.read(bytes, 0, bytes.size)
                                check(count >= 0) { "Microphone capture interrupted." }
                                if (count == 0) continue
                                val level=com.jarvis.assistant.hud.PcmLevel.pcm16(bytes,count)
                                com.jarvis.assistant.hud.AudioSignals.level(this,level);timing.energy(level);utterance.energy(level)
                                if (recognizer.acceptWaveForm(bytes, count)) {
                                    val result=JSONObject(recognizer.result)
                                    val text = result.optString("text")
                                    if (text.isNotBlank() && RecognitionConfidence.acceptsFinal(result,com.jarvis.assistant.titan.VoicePreferences(context).confidence.toDouble())) {
                                        timing.progress(text);utterance.segment(text)
                                        if(UtteranceAssembler.fastCommand(utterance.text())){emitResult(utterance.text());return@Thread}
                                    }
                                } else {
                                    val partial = JSONObject(recognizer.partialResult).optString("partial")
                                    timing.progress(partial);utterance.partial(partial)
                                    if (partial != previousPartial) {
                                        previousPartial = partial
                                        main.post { if (!closed.get()) onPartial(utterance.text()) }
                                    }
                                }
                                if(utterance.ready()){emitResult(utterance.text());return@Thread}
                            }
                            if (!closed.get()) {
                                val final = JSONObject(recognizer.finalResult).optString("text")
                                emitError("Speech inactivity or two-minute utterance limit reached. Tap the microphone or say your wake phrase again.")
                            }
                        } finally { com.jarvis.assistant.hud.AudioSignals.stop(this); runCatching { audio.stop() }; audio.release();processing.close(); recorder = null }
                    }
                }
            } catch (error: Throwable) {
                if (!closed.get()) emitError(error.message ?: "Offline recognition failed. Try downloading the model again.")
            }
        }, "JarvisOfflineSpeech").start()
    }

    fun close() { synchronized(audioLock) { closed.set(true); runCatching { recorder?.stop() } } }
    private fun emitResult(text: String) { main.post { if (!closed.get()) { closed.set(true); if(!timing.expired())onResult(text) else onError("Speech inactivity or two-minute utterance limit reached.") } } }
    private fun emitError(message: String) { main.post { if (!closed.get()) { closed.set(true); onError(message) } } }
}
