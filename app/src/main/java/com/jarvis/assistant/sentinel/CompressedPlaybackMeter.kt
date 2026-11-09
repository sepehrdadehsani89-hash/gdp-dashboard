package com.jarvis.assistant.sentinel

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.io.File

/** Decode the already-authorized local speech file off main; never opens a microphone.
 * Source-PCM energy is synchronized to MediaPlayer's cursor, not wall time or text length. */
internal class CompressedPlaybackMeter(private val player:MediaPlayer,file:File) {
    private val envelope=PcmPlaybackEnvelope()
    private val worker=HandlerThread("JarvisSpeechLevelDecoder").apply{start()}
    private val decoder=Handler(worker.looper)
    private val main=Handler(Looper.getMainLooper())
    @Volatile private var closed=false
    private val poll=object:Runnable { override fun run(){
        if(closed)return
        val reading=runCatching{envelope.levelAt(player.currentPosition.toLong()*1000,player.isPlaying)}.getOrNull()
        PlaybackLevels.update(this@CompressedPlaybackMeter,reading ?: 0f,reading!=null)
        if(!closed)main.postDelayed(this,50)
    }}
    init { PlaybackLevels.start(this,"Fish voice");decoder.post{decode(file)};main.post(poll) }

    private fun decode(file:File) {
        if(closed)return
        var extractor:MediaExtractor?=null;var codec:MediaCodec?=null
        try {
            val input=MediaExtractor();extractor=input;input.setDataSource(file.absolutePath)
            val track=(0 until input.trackCount).firstOrNull{input.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/")==true} ?: return
            input.selectTrack(track)
            val format=input.getTrackFormat(track)
            val output=MediaCodec.createDecoderByType(requireNotNull(format.getString(MediaFormat.KEY_MIME)));codec=output
            output.configure(format,null,null,0);output.start()
            var outputFormat=format
            val info=MediaCodec.BufferInfo();var inputEnded=false
            val started=SystemClock.elapsedRealtime()
            while(!closed && SystemClock.elapsedRealtime()-started<8000){
                if(!inputEnded){
                    val index=output.dequeueInputBuffer(10_000)
                    if(index>=0){
                        val buffer=requireNotNull(output.getInputBuffer(index));buffer.clear()
                        val count=input.readSampleData(buffer,0)
                        if(count<0){output.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnded=true}
                        else{output.queueInputBuffer(index,0,count,input.sampleTime.coerceAtLeast(0),0);input.advance()}
                    }
                }
                val index=output.dequeueOutputBuffer(info,10_000)
                if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)outputFormat=output.outputFormat
                else if(index>=0){
                    try {
                        if(info.size>0 && (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){
                            if(info.presentationTimeUs>PcmPlaybackEnvelope.MAX_DURATION_US)break
                            val buffer=requireNotNull(output.getOutputBuffer(index))
                            check(info.size<=2_000_000)
                            buffer.position(info.offset);buffer.limit(info.offset+info.size)
                            val encoding=when(if(outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING))outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)else AudioFormat.ENCODING_PCM_16BIT){
                                AudioFormat.ENCODING_PCM_16BIT->PlaybackPcmEncoding.S16
                                AudioFormat.ENCODING_PCM_FLOAT->PlaybackPcmEncoding.F32
                                else->error("Unsupported decoded PCM encoding")
                            }
                            if(!closed)envelope.append(buffer,info.presentationTimeUs.coerceAtLeast(0),outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),encoding)
                        }
                    }finally{output.releaseOutputBuffer(index,false)}
                    if((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0)break
                }
            }
        }catch(_:Exception){Log.d("JarvisVoice","DECODED_SPEECH_LEVEL_UNAVAILABLE")}
        catch(_:LinkageError){Log.d("JarvisVoice","DECODED_SPEECH_LEVEL_UNAVAILABLE")}
        catch(_:OutOfMemoryError){Log.d("JarvisVoice","DECODED_SPEECH_LEVEL_UNAVAILABLE")}
        finally{
            runCatching{codec?.stop()};runCatching{codec?.release()};runCatching{extractor?.release()}
            if(closed)envelope.clear()
        }
    }
    fun close(){closed=true;main.removeCallbacks(poll);PlaybackLevels.stop(this);envelope.clear();worker.quitSafely()}
}
