package com.jarvis.assistant.hud

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AudioReading(val recording:Boolean=false,val level:Float=0f,val history:List<Float> = emptyList(),val source:String="Microphone inactive")
/** Session identity prevents an old recorder's shutdown from clearing a newer recorder's meter. */
object AudioSignals {
    private var owner:Any?=null
    private var last=0L
    private val mutable=MutableStateFlow(AudioReading())
    val state=mutable.asStateFlow()
    @Synchronized fun start(token:Any,source:String){owner=token;last=0;mutable.value=AudioReading(true,source=source)}
    @Synchronized fun level(token:Any,value:Float){if(owner!==token)return;val now=android.os.SystemClock.elapsedRealtime();if(now-last<65)return;last=now;val v=value.takeIf(Float::isFinite)?.coerceIn(0f,1f) ?: 0f;mutable.value=mutable.value.copy(level=v,history=(mutable.value.history+v).takeLast(32))}
    @Synchronized fun stop(token:Any){if(owner===token){owner=null;mutable.value=AudioReading()}}
}
