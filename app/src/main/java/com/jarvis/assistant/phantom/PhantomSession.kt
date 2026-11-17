package com.jarvis.assistant.phantom

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ExecutionState { IDLE, WAKE, LISTENING, TRANSCRIBING, UNDERSTANDING, EXECUTING, GENERATING_RESPONSE, GENERATING_AUDIO, SPEAKING, WAITING_USER, COMPLETED, FAILED, CANCELLED }
enum class TraceEvent { WAKE_DETECTED, CAPTURE_STARTED, FIRST_PARTIAL, FINAL_TRANSCRIPT, AUTOCORRECT_STARTED, AUTOCORRECT_COMPLETED, INTENT_RESOLVED, CONTEXT_STARTED, CONTEXT_READY, MODEL_LOADING, INFERENCE_STARTED, FIRST_TOKEN, TOOL_STARTED, TOOL_COMPLETED, TTS_REQUESTED, AUDIO_BUFFERED, FIRST_AUDIO, WAITING_USER, COMPLETED, FAILED, CANCELLED }
data class RequestTrace(val id:Long,val started:Long,val events:Map<TraceEvent,Long> = emptyMap())
data class LiveCaption(val user:String="",val assistant:String="",val entity:String?=null,val expiresAt:Long=0,val provisional:Boolean=false) {
    fun visible(now:Long)= assistant.isNotBlank() && (expiresAt==0L || now<expiresAt)
    fun alpha(now:Long,reducedMotion:Boolean):Float=if(!visible(now))0f else if(expiresAt==0L || reducedMotion)1f else ((expiresAt-now)/1000f).coerceIn(0f,1f)
}
data class PhantomState(val execution:ExecutionState=ExecutionState.IDLE,val caption:LiveCaption=LiveCaption(),val trace:RequestTrace?=null)

/** Monotonic timings and ephemeral captions only. No request text, keys or audio in traces. */
class PhantomSession(private val now:()->Long) {
    private val mutable=MutableStateFlow(PhantomState())
    val state=mutable.asStateFlow()
    private val history=ArrayDeque<RequestTrace>()
    private var sequence=0L
    private var progressAt=0L
    private var stateAt=0L
    private var workAt=0L
    private var lastSpeechId=Long.MIN_VALUE
    private var activeSpeechId:Long?=null
    @Synchronized fun begin(capture:Boolean=false):Long {
        mutable.value.trace?.let{remember(it)}
        val time=now();progressAt=time;stateAt=time;workAt=time
        activeSpeechId=null
        val trace=RequestTrace(++sequence,time)
        mutable.value=PhantomState(if(capture)ExecutionState.LISTENING else ExecutionState.UNDERSTANDING,trace=trace)
        return trace.id
    }
    @Synchronized fun event(event:TraceEvent,id:Long?=null) {
        val old=mutable.value;val trace=old.trace ?: return
        if(id!=null && trace.id!=id)return
        progressAt=now()
        mutable.value=old.copy(trace=trace.copy(events=if(event in trace.events)trace.events else trace.events+(event to progressAt)))
    }
    @Synchronized fun stage(value:ExecutionState,id:Long?=null) {
        if(id!=null && mutable.value.trace?.id!=id)return
        if(mutable.value.execution!=value){stateAt=now();progressAt=stateAt}
        mutable.value=mutable.value.copy(execution=value)
    }
    @Synchronized fun partial(text:String) {
        if(mutable.value.execution !in listOf(ExecutionState.WAKE,ExecutionState.LISTENING,ExecutionState.TRANSCRIBING))begin(true)
        if(text.isNotBlank()){event(TraceEvent.FIRST_PARTIAL);stage(ExecutionState.TRANSCRIBING)}
        clearCaption()
    }
    @Synchronized fun submitted(text:String,voiced:Boolean):Long {
        if(!voiced || mutable.value.execution !in listOf(ExecutionState.WAKE,ExecutionState.LISTENING,ExecutionState.TRANSCRIBING))begin()
        workAt=now();event(TraceEvent.FINAL_TRANSCRIPT);stage(ExecutionState.UNDERSTANDING)
        clearCaption()
        return mutable.value.trace!!.id
    }
    @Synchronized fun corrected(text:String){clearCaption()}
    /** Only actual audio onset may publish a caption. Speech IDs are monotonic per runtime. */
    @Synchronized fun spokenStart(text:String,speechId:Long,id:Long?=null):Boolean {
        if(id!=null && mutable.value.trace?.id!=id)return false
        if(speechId<=lastSpeechId || text.isBlank() || mutable.value.execution in setOf(ExecutionState.CANCELLED,ExecutionState.FAILED,ExecutionState.COMPLETED))return false
        lastSpeechId=speechId;activeSpeechId=speechId
        mutable.value=mutable.value.copy(caption=LiveCaption(assistant=text.take(3500)))
        return true
    }
    /** A stale completion cannot remove the caption belonging to a newer audio segment. */
    @Synchronized fun spokenEnd(speechId:Long,lingerMs:Long=1200):Boolean {
        if(activeSpeechId!=speechId)return false
        activeSpeechId=null
        val linger=lingerMs.coerceIn(0,5000)
        mutable.value=mutable.value.copy(caption=if(linger==0L)LiveCaption() else mutable.value.caption.copy(expiresAt=now()+linger))
        return true
    }
    @Synchronized fun finish(value:ExecutionState=ExecutionState.COMPLETED) {
        event(when(value){ExecutionState.FAILED->TraceEvent.FAILED;ExecutionState.CANCELLED->TraceEvent.CANCELLED;ExecutionState.WAITING_USER->TraceEvent.WAITING_USER;else->TraceEvent.COMPLETED})
        stage(value)
        if(value in setOf(ExecutionState.CANCELLED,ExecutionState.FAILED) || activeSpeechId!=null)clearCaption()
    }
    @Synchronized fun clearCaption(){activeSpeechId=null;mutable.value=mutable.value.copy(caption=LiveCaption())}
    @Synchronized fun traces():List<RequestTrace> = (history.toList()+listOfNotNull(mutable.value.trace)).distinctBy{it.id}.takeLast(24)
    private fun remember(trace:RequestTrace){history.addLast(trace);while(history.size>23)history.removeFirst()}
    @Synchronized fun timedOut():Boolean {
        val s=mutable.value.execution;val elapsed=now()-progressAt
        // Recognition retains its own energy-aware inactivity clock. Never truncate active speech here.
        if(s !in setOf(ExecutionState.UNDERSTANDING,ExecutionState.EXECUTING,ExecutionState.GENERATING_RESPONSE,ExecutionState.GENERATING_AUDIO,ExecutionState.SPEAKING))return false
        val budget=when(s){ExecutionState.GENERATING_AUDIO->30_000;ExecutionState.SPEAKING->90_000;else->45_000}
        return elapsed>budget || now()-workAt>240_000
    }
}

enum class Presentation { HIDDEN, APP, WALLPAPER, OVERLAY }
object PhantomPolicy {
    fun presentation(locked:Boolean,credential:Boolean,stopped:Boolean,appVisible:Boolean,wallpaperVisible:Boolean,enabled:Boolean,permission:Boolean):Presentation=when{
        locked || credential || stopped->Presentation.HIDDEN
        appVisible->Presentation.APP
        wallpaperVisible->Presentation.WALLPAPER
        enabled && permission->Presentation.OVERLAY
        else->Presentation.HIDDEN
    }
    fun fastTool(text:String):String? {
        val t=com.jarvis.assistant.neural.AppMatching.normalize(text).removePrefix("hey jarvis ").removePrefix("jarvis ")
        return when(t){"battery","battery level","what is my battery","what is my battery level","what s my battery","whats my battery","bateria","nivel de bateria"->"battery"
            "what time is it","what time","time","que hora es","hora actual"->"what time is it"
            else->null}
    }
    fun expand(text:String)=com.jarvis.assistant.neural.AppMatching.normalize(text) in setOf("expand this","show more","show me that","take me to it","open full interface","expandelo","muestra mas","muestrame eso")
    fun clampPosition(value:Float,available:Int):Int=(value.coerceIn(0f,1f)*available.coerceAtLeast(0)).toInt()
}
