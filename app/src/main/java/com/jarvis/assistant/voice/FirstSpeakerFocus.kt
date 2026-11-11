package com.jarvis.assistant.voice

import com.jarvis.assistant.omnisystem.CapturedVoice
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

data class SpeakerFocusReading(val status:String="Waiting for the first voice",val locked:Boolean=false)
/** Ephemeral similarity reference for one conversation. It is never authentication or enrollment. */
class FirstSpeakerFocus(private val now:()->Long){
    private var reference:FloatArray?=null
    private var expires=0L
    private var revision=0L
    private val mutable=MutableStateFlow(SpeakerFocusReading());val state=mutable.asStateFlow()
    @Synchronized fun generation()=revision
    @Synchronized fun reset(){reference?.fill(0f);reference=null;expires=0;revision++;mutable.value=SpeakerFocusReading()}
    @Synchronized fun unavailable(code:String){mutable.value=SpeakerFocusReading(code,false)}
    @Synchronized fun decide(vector:FloatArray,expected:Long,threshold:Float=.72f):Boolean {
        if(expected!=revision)return false
        require(vector.size in 16..1024 && vector.all(Float::isFinite) && threshold in .5f.. .95f)
        val norm=sqrt(vector.sumOf{it.toDouble()*it});require(norm>1e-10)
        if(reference!=null && now()>=expires){reference?.fill(0f);reference=null}
        val unit=FloatArray(vector.size){(vector[it]/norm).toFloat()}
        try{
            val previous=reference
            if(previous==null){reference=unit.copyOf();expires=now()+45_000;mutable.value=SpeakerFocusReading("Following the first speaker · local similarity, not identity",true);return true}
            if(previous.size!=unit.size)return false
            val score=previous.indices.sumOf{previous[it].toDouble()*unit[it]}
            val accepted=score>=threshold
            if(accepted)expires=now()+45_000
            mutable.value=SpeakerFocusReading(if(accepted)"Following the first speaker" else "Other or uncertain voice ignored · not extending the turn",true)
            return accepted
        }finally{unit.fill(0f)}
    }
}
/** Quarantine one second of PCM before ASR. Rejected speakers become silence, so background
 * conversation cannot extend the target's silence timer or append its recognized words.
 * This is speaker-similarity gating, not separation of overlapping voices in a mono recording. */
class SpeakerAudioFocus(private val enabled:Boolean,private val tracker:FirstSpeakerFocus,private val embedding:suspend(CapturedVoice)->FloatArray){
    data class Packet(val pcm:ByteArray,val count:Int,val owned:Boolean=false)
    private data class Chunk(val pcm:ByteArray,val serial:Long,val referenceGeneration:Long)
    private val generation=AtomicLong()
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val input=Channel<Chunk>(2,onUndeliveredElement={it.pcm.fill(0)})
    private val output=ConcurrentLinkedQueue<Chunk>()
    private val lock=Any()
    private var buffer=ByteArray(32000)
    private var used=0
    @Volatile private var failure:String?=null
    @Volatile private var closed=false
    init{if(enabled)scope.launch{for(chunk in input){
        var vector:FloatArray?=null
        try{
            if(closed || chunk.serial!=generation.get())continue
            val level=com.jarvis.assistant.hud.PcmLevel.pcm16(chunk.pcm,chunk.pcm.size)
            if(level>=.002f){
                vector=withTimeout(2000){embedding(CapturedVoice(chunk.pcm,16000))}
                if(closed || chunk.serial!=generation.get())continue
                if(!tracker.decide(vector,chunk.referenceGeneration))chunk.pcm.fill(0)
            }else chunk.pcm.fill(0)
            if(!closed && chunk.serial==generation.get()){
                if(output.size>=3){failure="SPEAKER_FOCUS_TOO_SLOW";continue}
                output.add(chunk.copy(pcm=chunk.pcm.copyOf()))
            }
        }catch(e:CancellationException){if(!scope.isActive)throw e;failure="SPEAKER_FOCUS_UNAVAILABLE"}
        catch(_:Exception){failure="SPEAKER_FOCUS_UNAVAILABLE"}
        catch(_:LinkageError){failure="SPEAKER_FOCUS_UNAVAILABLE"}
        catch(_:OutOfMemoryError){failure="SPEAKER_FOCUS_UNAVAILABLE"}
        finally{vector?.fill(0f);chunk.pcm.fill(0)}
    }}}
    fun packets(bytes:ByteArray,count:Int,applyFocus:Boolean=true):List<Packet>{
        require(count in 0..bytes.size)
        if(!enabled || !applyFocus)return listOf(Packet(bytes,count))
        failure?.let{tracker.unavailable(it);throw java.io.IOException(it)}
        if(closed)return emptyList()
        synchronized(lock){
            var offset=0
            while(offset<count){
                val n=minOf(buffer.size-used,count-offset);bytes.copyInto(buffer,used,offset,offset+n);used+=n;offset+=n
                if(used==buffer.size){
                    val complete=buffer;buffer=ByteArray(32000);used=0
                    if(!input.trySend(Chunk(complete,generation.get(),tracker.generation())).isSuccess){complete.fill(0);failure="SPEAKER_FOCUS_TOO_SLOW";break}
                }
            }
        }
        return buildList{while(true){val p=output.poll() ?: break;if(p.serial==generation.get())add(Packet(p.pcm,p.pcm.size,true))else p.pcm.fill(0)}}
    }
    fun resetCapture(){generation.incrementAndGet();synchronized(lock){buffer.fill(0);used=0};while(true){input.tryReceive().getOrNull()?.pcm?.fill(0) ?: break};while(true){output.poll()?.pcm?.fill(0) ?: break};failure=null}
    fun close(){closed=true;resetCapture();input.cancel();scope.cancel()}
}
