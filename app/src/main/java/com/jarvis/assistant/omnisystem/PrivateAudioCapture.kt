package com.jarvis.assistant.omnisystem

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

data class CapturedVoice(val pcm:ByteArray,val sampleRate:Int=16000) {
    fun floats()=FloatArray(pcm.size/2){i->((pcm[2*i].toInt() and 255) or (pcm[2*i+1].toInt() shl 8)).toShort()/32768f}
    fun wav():ByteArray=WavePcm.encode(pcm,sampleRate)
}
object WavePcm {
    fun encode(pcm:ByteArray,rate:Int=16000):ByteArray {
        require(pcm.isNotEmpty() && pcm.size%2==0 && rate in 8000..48000 && pcm.size<=rate*2*30)
        val h=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        h.put("RIFF".toByteArray()).putInt(pcm.size+36).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(1).putInt(rate).putInt(rate*2).putShort(2).putShort(16)
            .put("data".toByteArray()).putInt(pcm.size)
        return h.array()+pcm
    }
}
object AudioSpectrum {
    val frequencies=listOf(63,125,250,500,1000,2000,4000)
    fun bands(pcm:ByteArray,count:Int,rate:Int=16000):List<Float>{
        val n=minOf(count,pcm.size)/2
        if(n<2)return frequencies.map{0f}
        return frequencies.map{frequency->
            val coefficient=2*cos(2*PI*frequency/rate);var previous=0.0;var before=0.0
            for(i in 0 until n){
                val value=((pcm[2*i].toInt() and 255) or (pcm[2*i+1].toInt() shl 8)).toShort()/32768.0
                val next=value+coefficient*previous-before;before=previous;previous=next
            }
            (2*sqrt((previous*previous+before*before-coefficient*previous*before).coerceAtLeast(0.0))/n).toFloat().coerceIn(0f,1f)
        }
    }
}
/** Visible bounded capture only; no files, hidden service or persistent audio samples. */
object PrivateAudioCapture {
    @android.annotation.SuppressLint("MissingPermission")
    suspend fun record(context:Context,seconds:Int,spectrum:Boolean=false,onCaptureInfo:(String)->Unit={},onLevel:(Float,Float,List<Float>)->Unit):CapturedVoice=withContext(Dispatchers.IO){
        require(seconds in 3..25)
        check(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){"Microphone permission required."}
        val rate=16000;val target=seconds*rate*2
        var managed:com.jarvis.assistant.voice.ManagedMicrophone?=null
        var captured:ByteArray?=null
        var buffer:ByteArray?=null
        val meter=Any();var completed=false
        try{
            withTimeout(1800){while(managed==null){ensureActive();managed=com.jarvis.assistant.voice.ManagedMicrophone.open(context,com.jarvis.assistant.rebirth.MicrophoneOwner.PRIVATE_CAPTURE,MediaRecorder.AudioSource.MIC);if(managed==null)delay(100)}}
            val active=checkNotNull(managed);val recorder=active.audio
            val output=ByteArray(target);captured=output
            val chunk=ByteArray(3200);buffer=chunk
            recorder.startRecording();check(recorder.recordingState==AudioRecord.RECORDSTATE_RECORDING)
            withContext(Dispatchers.Main.immediate){onCaptureInfo("MIC · "+(recorder.routedDevice?.productName?.toString() ?: "Android default input")+" · "+active.processing.report)}
            com.jarvis.assistant.hud.AudioSignals.start(meter,"Private foreground audio capture")
            withTimeout((seconds+5)*1000L){
                val cancelWatch=launch(Dispatchers.Default,start=CoroutineStart.UNDISPATCHED){try{awaitCancellation()}finally{runCatching{recorder.stop()}}}
                try{
                    var position=0
                    while(position<target){
                        ensureActive();val n=recorder.read(chunk,0,minOf(chunk.size,target-position),AudioRecord.READ_BLOCKING)
                        check(n>0 && n%2==0){"Microphone capture failed."}
                        System.arraycopy(chunk,0,output,position,n);position+=n
                        val level=com.jarvis.assistant.hud.PcmLevel.pcm16(chunk,n)
                        com.jarvis.assistant.hud.AudioSignals.level(meter,level)
                        val bands=if(spectrum)AudioSpectrum.bands(chunk,n,rate) else emptyList()
                        withContext(Dispatchers.Main.immediate){onLevel(level,position.toFloat()/target,bands)}
                    }
                }finally{cancelWatch.cancel()}
            }
            completed=true;CapturedVoice(output,rate)
        }finally{managed?.close();com.jarvis.assistant.hud.AudioSignals.stop(meter);buffer?.fill(0);if(!completed)captured?.fill(0)}

    }
}
