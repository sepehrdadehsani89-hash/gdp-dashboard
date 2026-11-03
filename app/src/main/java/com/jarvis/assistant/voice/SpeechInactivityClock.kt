package com.jarvis.assistant.voice

/** Ten seconds of inactivity, bounded to two minutes per utterance. Energy extends only after recognized speech. */
class SpeechInactivityClock(private val now:()->Long){
    private var opened=now()
    private var last=opened
    private var recognized=""
    @Synchronized fun start(){opened=now();last=opened;recognized=""}
    @Synchronized fun deadline()=minOf(last+IDLE_MS,opened+MAX_MS)
    @Synchronized fun expired()=now()<opened || now()>=deadline()
    @Synchronized fun progress(text:String):Boolean{
        val clean=text.trim().take(8000)
        if(expired() || clean.isBlank() || clean==recognized)return false
        recognized=clean;last=now();return true
    }
    @Synchronized fun energy(level:Float):Boolean{
        if(recognized.isBlank() || expired() || !level.isFinite() || level<.015f)return false
        last=now();return true
    }
    companion object {const val IDLE_MS=10_000L;const val MAX_MS=120_000L}
}
