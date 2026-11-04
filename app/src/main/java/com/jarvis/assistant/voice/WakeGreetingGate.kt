package com.jarvis.assistant.voice

/** A stable bare wake gets an acknowledgement even when the recognizer delays its final result. */
class WakeGreetingGate(private val now:()->Long,private val settleMs:Long=700){
    private var serial:Long?=null
    private var at=0L
    @Synchronized fun arm(value:Long){serial=value;at=now()}
    @Synchronized fun cancel(){serial=null}
    @Synchronized fun due(value:Long)=serial==value && now()-at>=settleMs
}
