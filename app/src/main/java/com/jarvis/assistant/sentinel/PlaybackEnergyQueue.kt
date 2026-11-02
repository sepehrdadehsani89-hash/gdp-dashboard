package com.jarvis.assistant.sentinel

import com.jarvis.assistant.hud.PcmLevel

/** Bounded PCM bins indexed by frames actually submitted to a mono AudioTrack. */
internal class PlaybackEnergyQueue(private val capacity:Int=192) {
    private data class Bin(val start:Long,val end:Long,val rms:Float)
    private val bins=ArrayDeque<Bin>()
    init{require(capacity>0)}
    @Synchronized fun written(samples:FloatArray,offset:Int,count:Int,start:Long){
        require(start>=0 && offset>=0 && count>=0 && offset<=samples.size && count<=samples.size-offset)
        if(count==0)return
        require(bins.lastOrNull()?.let{start>=it.end}!=false){"Playback frames must be submitted in order"}
        bins.addLast(Bin(start,start+count,PcmLevel.floats(samples,offset,count)))
        while(bins.size>capacity)bins.removeFirst()
    }
    @Synchronized fun readingAt(cursor:Long,playing:Boolean):Float? {
        if(!playing || cursor<0)return null
        while(bins.isNotEmpty() && bins.first().end<=cursor)bins.removeFirst()
        return bins.firstOrNull()?.takeIf{cursor>=it.start && cursor<it.end}?.rms
    }
    fun levelAt(cursor:Long,playing:Boolean):Float=readingAt(cursor,playing) ?: 0f
    @Synchronized fun clear(){bins.clear()}
    val size:Int get()=synchronized(this){bins.size}
}
