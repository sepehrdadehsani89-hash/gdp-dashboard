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
    suspend fun record(context:Context,seconds:Int,spectrum:Boolean=false,onLevel:(Float,Float,List<Float>)->Unit):CapturedVoice=withContext(Dispatchers.IO){
        require(seconds in 3..25)
        check(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){"Microphone permission required."}
        val rate=16000;val target=seconds*rate*2
        val minimum=AudioRecord.getMinBufferSize(rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)
        check(minimum>0){"Microphone format unavailable."}
        val recorder=AudioRecord(MediaRecorder.AudioSource.MIC,rate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,maxOf(minimum,8192))
        val meter=Any()
        val captured=ByteArray(target);val buffer=ByteArray(3200)
        var completed=false
        try {
            check(recorder.state==AudioRecord.STATE_INITIALIZED){"Microphone is unavailable."}
            var position=0
            recorder.startRecording();check(recorder.recordingState==AudioRecord.RECORDSTATE_RECORDING)
            com.jarvis.assistant.hud.AudioSignals.start(meter,"Private foreground audio capture")
            withTimeout((seconds+5)*1000L){while(position<target){
                ensureActive()
                val n=recorder.read(buffer,0,minOf(buffer.size,target-position),AudioRecord.READ_BLOCKING)
                check(n>0 && n%2==0){"Microphone capture failed."}
                System.arraycopy(buffer,0,captured,position,n);position+=n
                val level=com.jarvis.assistant.hud.PcmLevel.pcm16(buffer,n)
                com.jarvis.assistant.hud.AudioSignals.level(meter,level)
                val bands=if(spectrum)AudioSpectrum.bands(buffer,n,rate) else emptyList()
                withContext(Dispatchers.Main.immediate){onLevel(level,position.toFloat()/target,bands)}
            }}
            completed=true
            CapturedVoice(captured,rate)
        } finally {
            runCatching{recorder.stop()};recorder.release();com.jarvis.assistant.hud.AudioSignals.stop(meter)
            buffer.fill(0);if(!completed)captured.fill(0)
        }
    }
}
