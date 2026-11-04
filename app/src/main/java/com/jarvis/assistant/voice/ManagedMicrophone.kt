package com.jarvis.assistant.voice

import android.content.Context
import android.media.*
import com.jarvis.assistant.rebirth.*

/** Shared ownership covers wake, foreground STT and visible music/enrollment capture. */
class ManagedMicrophone private constructor(val audio:AudioRecord,val processing:MicrophoneProcessing,private val lease:MicrophoneLease):AutoCloseable {
    private var closed=false
    private val callbacks=mutableListOf<android.media.AudioManager.AudioRecordingCallback>()
    fun registerCallback(executor:java.util.concurrent.Executor,callback:android.media.AudioManager.AudioRecordingCallback){audio.registerAudioRecordingCallback(executor,callback);callbacks.add(callback)}
    @Synchronized override fun close(){
        if(closed)return;closed=true
        try{callbacks.forEach{runCatching{audio.unregisterAudioRecordingCallback(it)}};callbacks.clear();runCatching{audio.stop()};audio.release()}finally{try{processing.close()}finally{MicrophoneSessions.manager.release(lease)}}
    }
    companion object {
        @android.annotation.SuppressLint("MissingPermission")
        fun open(context:Context,owner:MicrophoneOwner,source:Int=MediaRecorder.AudioSource.VOICE_RECOGNITION):ManagedMicrophone? {
            val lease=MicrophoneSessions.manager.acquire(owner) ?: return null
            var audio:AudioRecord?=null;var processing:MicrophoneProcessing?=null
            try{
                val size=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
                check(size>0){"16 kHz microphone format unavailable."}
                val recorder=AudioRecord(source,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,maxOf(size*2,8192));audio=recorder
                check(recorder.state==AudioRecord.STATE_INITIALIZED){"Microphone unavailable."}
                val effects=MicrophoneProcessing(context,recorder.audioSessionId);processing=effects
                return ManagedMicrophone(recorder,effects,lease)
            }catch(e:Throwable){runCatching{audio?.release()};runCatching{processing?.close()};MicrophoneSessions.manager.release(lease);throw e}
        }
    }
}
