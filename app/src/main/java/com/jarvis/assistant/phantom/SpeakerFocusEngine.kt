package com.jarvis.assistant.phantom

import com.jarvis.assistant.omnisystem.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/** Personalization only. These decisions never authorize a phone operation or Android unlock. */
class SpeakerAffinity(private val now:()->Long) {
    private var profile:String?=null
    private var until=0L
    @Synchronized fun accepts(candidate:String?,ambiguous:Boolean):Boolean {
        if(now()>=until){profile=null;until=0}
        if(ambiguous)return false
        if(profile==null){if(candidate!=null){profile=candidate;until=now()+30_000};return true}
        if(candidate!=profile)return false
        until=now()+30_000;return true
    }
    @Synchronized fun clear(){profile=null;until=0}
}

/** Optional bounded classifier for Vosk-final segments from the existing microphone tap.
 * Inference runs off the capture thread. Uncertain audio after a match is rejected, never separated
 * or attributed by guessing. Short samples without an active identity remain neutral.
 */
class SpeakerFocusEngine(private val profiles:SpeakerProfiles,private val now:()->Long) {
    data class Decision(val serial:Long,val text:String,val accepted:Boolean)
    private data class Segment(val id:Long,val serial:Long,val text:String,val audio:CapturedVoice?)
    private data class Result(val id:Long,val decision:Decision)
    private val affinity=SpeakerAffinity(now)
    private val sequence=AtomicLong()
    private data class Pending(val serial:Long,val text:String,val at:Long)
    private val pending=ConcurrentHashMap<Long,Pending>()
    private val results=ConcurrentLinkedQueue<Result>()
    private val queue=Channel<Segment>(2,onUndeliveredElement={it.audio?.pcm?.fill(0)})
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    @Volatile private var serial=-1L
    init {scope.launch{for(segment in queue){
        try{
            if(segment.serial!=serial || !profiles.focusEnabled){results.add(Result(segment.id,Decision(segment.serial,segment.text,false)));continue}
            val began=now()
            val match=segment.audio?.let{audio->
                val vector=profiles.embedding(audio)
                try{SpeakerProfilePolicy.suggest(vector,profiles.profiles(),.78f,.10f)}finally{vector.fill(0f)}
            }
            val accepted=segment.serial==serial && profiles.focusEnabled && now()-began<=2000 && affinity.accepts(match?.profile?.id,match?.ambiguous==true)
            results.add(Result(segment.id,Decision(segment.serial,segment.text,accepted)))
        }catch(e:CancellationException){throw e}
        catch(_:Exception){results.add(Result(segment.id,Decision(segment.serial,segment.text,false)))}
        catch(_:LinkageError){results.add(Result(segment.id,Decision(segment.serial,segment.text,false)))}
        catch(_:OutOfMemoryError){results.add(Result(segment.id,Decision(segment.serial,segment.text,false)))}
        finally{segment.audio?.pcm?.fill(0)}
    }}}
    fun setSerial(value:Long){serial=value}
    fun newWake(){affinity.clear()}
    fun offer(text:String,value:Long,audio:CapturedVoice?):Boolean {
        val id=sequence.incrementAndGet();pending[id]=Pending(value,text.take(8000),now())
        if(queue.trySend(Segment(id,value,text.take(8000),audio)).isSuccess)return true
        pending.remove(id);audio?.pcm?.fill(0);return false
    }
    fun hasPending(value:Long)=pending.values.any{it.serial==value}
    fun drain():List<Decision> = buildList{
        pending.entries.forEach{(id,p)->if(now()-p.at>3000 && pending.remove(id,p) && p.serial==serial)add(Decision(p.serial,p.text,false))}
        while(true){val result=results.poll() ?: break;if(pending.remove(result.id)!=null && result.decision.serial==serial)add(result.decision)}
    }
    fun reset(){serial=-1;affinity.clear();results.clear();pending.clear();while(true){val s=queue.tryReceive().getOrNull() ?: break;s.audio?.pcm?.fill(0)}}
    fun close(){reset();queue.cancel();scope.cancel()}
}
