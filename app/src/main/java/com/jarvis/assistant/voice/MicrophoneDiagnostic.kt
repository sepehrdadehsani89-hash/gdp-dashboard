package com.jarvis.assistant.voice

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.jarvis.assistant.AssistantRuntime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MicrophoneTestReading(val active:Boolean=false,val captureAt:Long=0,val summary:String="",val preview:String="")

/** Explicit foreground test of the real recognizer. Results never enter AI/tool dispatch or persistent storage. */
class MicrophoneDiagnostic(context:Context,private val runtime:AssistantRuntime):AutoCloseable {
    private val context=context.applicationContext
    private val main=Handler(Looper.getMainLooper())
    private val owner=Any()
    private var generation=0L
    private var speech:SpeechController?=null
    private var timeout:Runnable?=null
    private val mutable=MutableStateFlow(MicrophoneTestReading())
    val state=mutable.asStateFlow()
    private fun unlocked()=context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked==false && !com.jarvis.assistant.security.SystemCredentialPrivacy.pending(context)
    private fun current(token:Long)=token==generation && mutable.value.active && runtime.captureIsCurrent(owner) && unlocked()
    fun start(){
        if(mutable.value.active || !unlocked())return
        if(!runtime.beginExclusiveCapture(owner)){mutable.value=MicrophoneTestReading(summary="Capture unavailable. Finish the current recording first.");return}
        val token=++generation
        mutable.value=MicrophoneTestReading(active=true,summary="Preparing configured recognizer…")
        val controller=try{speech ?: SpeechController(context).also{speech=it}}catch(_:Exception){
            mutable.value=mutable.value.copy(summary="Recognition could not initialize. No command was executed.")
            stop(false);return
        }
        timeout=Runnable{if(token==generation && mutable.value.active){mutable.value=mutable.value.copy(summary="Diagnostic timed out. Recording stopped; check the speech service and retry.");stop(false)}}.also{main.postDelayed(it,65_000)}
        val settings=runtime.store.settings.value
        controller.listen(settings.language,settings.allowNetworkSpeech,
            onPartial={text->main.post{if(current(token) && com.jarvis.assistant.titan.VoicePreferences(context).transcriptionPreview)mutable.value=mutable.value.copy(preview=text)}},
            onCaptureStarted={main.post{if(current(token))mutable.value=mutable.value.copy(captureAt=SystemClock.elapsedRealtime(),summary="Listening for one test utterance. No command will run.")}},
            onResult={text->main.post{if(current(token)){
                val started=mutable.value.captureAt
                val elapsed=if(started>0)SystemClock.elapsedRealtime()-started else null
                mutable.value=mutable.value.copy(summary="Final recognition received · ${text.length} characters · capture to final ${elapsed ?: "unavailable"} ms",preview=if(com.jarvis.assistant.titan.VoicePreferences(context).transcriptionPreview)text else "")
                stop(false)
            }}},
            onError={message->main.post{if(token==generation && mutable.value.active){mutable.value=mutable.value.copy(summary=message.take(300));stop(false)}}})
    }
    fun stop(cancelled:Boolean=true){
        generation++
        timeout?.let(main::removeCallbacks);timeout=null
        val old=mutable.value
        mutable.value=old.copy(active=false,summary=if(cancelled && old.active)"Diagnostic stopped. No command was executed." else old.summary,preview=if(cancelled)"" else old.preview)
        speech?.stop()
        runtime.endExclusiveCapture(owner)
    }
    fun checkOwnership(){if(mutable.value.active && (!runtime.captureIsCurrent(owner) || !unlocked() || ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED))stop()}
    fun clearPreview(){mutable.value=mutable.value.copy(preview="")}
    override fun close(){stop();speech?.shutdown();speech=null;mutable.value=MicrophoneTestReading()}
}
