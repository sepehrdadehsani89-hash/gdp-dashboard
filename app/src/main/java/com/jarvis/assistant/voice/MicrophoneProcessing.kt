package com.jarvis.assistant.voice

import android.content.Context
import android.media.audiofx.*
import com.jarvis.assistant.titan.VoicePreferences

/** Only Android-supported session effects, never a claim of speaker separation or universal noise rejection. */
class MicrophoneProcessing(context:Context,session:Int):AutoCloseable {
    private val effects=mutableListOf<AudioEffect>()
    val report:String
    init{
        val p=VoicePreferences(context)
        fun attach(name:String,requested:Boolean,available:Boolean,create:()->AudioEffect?):String {
            if(!available)return "$name: unavailable"
            if(!requested)return "$name: disabled"
            return runCatching{
                val e=create() ?: return@runCatching "$name: unavailable";effects+=e
                val result=e.setEnabled(true)
                "$name: "+if(result==AudioEffect.SUCCESS && e.enabled)"enabled by Android" else "not enabled"
            }.getOrElse{"$name: unavailable for this source"}
        }
        report=listOf(attach("AEC",p.echoCancellation,AcousticEchoCanceler.isAvailable()){AcousticEchoCanceler.create(session)},
            attach("NS",p.noiseReduction,NoiseSuppressor.isAvailable()){NoiseSuppressor.create(session)},
            attach("AGC",p.gainControl,AutomaticGainControl.isAvailable()){AutomaticGainControl.create(session)}).joinToString(" · ")
    }
    override fun close(){effects.forEach{runCatching{it.release()}};effects.clear()}
}
