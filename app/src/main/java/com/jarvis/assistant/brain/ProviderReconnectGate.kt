package com.jarvis.assistant.brain

/** Event-driven restoration only. No polling loop, inference request or credential material. */
class ProviderReconnectGate(private val now:()->Long=System::currentTimeMillis){
    private var current="";private var attempts=0;private var next=0L;private var inFlight=false;private var terminal=false;private var window=0L
    @Synchronized fun acquire(key:String):Boolean {
        val time=now()
        if(key!=current){current=key;attempts=0;next=0;terminal=false;inFlight=false;window=time}
        if(!terminal && !inFlight && time-window>=300_000){attempts=0;window=time}
        if(inFlight || terminal || attempts>=3 || time<next)return false
        inFlight=true;attempts++;return true
    }
    @Synchronized fun finish(key:String,failure:ProviderFailureKind?){
        if(key!=current)return
        inFlight=false
        if(failure==null){attempts=0;next=now()+60_000;return}
        terminal=failure in setOf(ProviderFailureKind.AUTHENTICATION,ProviderFailureKind.AUTHORIZATION,ProviderFailureKind.NOT_VERIFIED,ProviderFailureKind.CREDITS,ProviderFailureKind.MODEL_UNAVAILABLE)
        next=now()+(1000L shl (attempts-1).coerceIn(0,5))
    }
    @Synchronized fun reset(){current="";attempts=0;next=0;inFlight=false;terminal=false;window=0}
}
