package com.jarvis.assistant.omnisystem

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/** Bounded tap of an already-authorized wake recorder; never opens another microphone. */
class SpeakerSampleBuffer(private val capacity:Int=16000*2*4){
    private var buffer:ByteArray?=null
    private var position=0
    private var filled=0
    @Synchronized fun feed(bytes:ByteArray,count:Int,allowed:Boolean){
        if(!allowed){clear();return}
        require(capacity>0 && capacity%2==0 && count in 0..bytes.size && count%2==0)
        val data=buffer ?: ByteArray(capacity).also{buffer=it}
        var offset=(count-capacity).coerceAtLeast(0)
        while(offset<count){val n=minOf(count-offset,capacity-position);System.arraycopy(bytes,offset,data,position,n);offset+=n;position=(position+n)%capacity;filled=minOf(capacity,filled+n)}
    }
    @Synchronized fun take():CapturedVoice? {
        val data=buffer ?: return null
        if(filled<32000){clear();return null}
        val start=if(filled==capacity)position else 0
        val result=ByteArray(filled);val tail=minOf(filled,capacity-start)
        System.arraycopy(data,start,result,0,tail);if(tail<filled)System.arraycopy(data,0,result,tail,filled-tail)
        clear()
        if(com.jarvis.assistant.hud.PcmLevel.pcm16(result,result.size)<.008f){result.fill(0);return null}
        return CapturedVoice(result)
    }
    @Synchronized fun clear(){buffer?.fill(0);buffer=null;position=0;filled=0}
}
/** One transient PCM handoff bound to a private command broadcast ID, with expiry and zeroing. */
object SpeakerSamples {
    private var next=0L
    private data class Pending(val id:Long,val at:Long,val audio:CapturedVoice)
    private var pending:Pending?=null
    private val handler by lazy{Handler(Looper.getMainLooper())}
    @Synchronized fun deposit(audio:CapturedVoice):Long {
        clear();val id=++next;pending=Pending(id,SystemClock.elapsedRealtime(),audio)
        handler.postDelayed({synchronized(this){if(pending?.id==id)clear()}},10_000)
        return id
    }
    @Synchronized fun take(id:Long):CapturedVoice? {
        val value=pending ?: return null
        if(value.id!=id)return null
        pending=null
        if(SystemClock.elapsedRealtime()-value.at !in 0..10_000){value.audio.pcm.fill(0);return null}
        return value.audio
    }
    @Synchronized fun clear(){pending?.audio?.pcm?.fill(0);pending=null}
}
