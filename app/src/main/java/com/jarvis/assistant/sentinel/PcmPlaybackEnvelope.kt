package com.jarvis.assistant.sentinel

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

internal enum class PlaybackPcmEncoding(val bytes:Int) { S16(2), F32(4) }

/** Only RMS/timestamps survive decoding. Lookups use the player's actual media cursor. */
internal class PcmPlaybackEnvelope(private val capacity:Int=32768) {
    private data class Bin(val startUs:Long,val endUs:Long,val rms:Float)
    private val bins=ArrayList<Bin>()
    init { require(capacity>0) }

    @Synchronized fun append(pcm:ByteBuffer,startUs:Long,sampleRate:Int,channels:Int,encoding:PlaybackPcmEncoding) {
        require(startUs in 0..MAX_DURATION_US && sampleRate in 8000..192000 && channels in 1..8)
        require(bins.lastOrNull()?.let{startUs>=it.startUs}!=false)
        val bytes=pcm.slice().order(ByteOrder.LITTLE_ENDIAN)
        val frames=bytes.remaining()/(channels*encoding.bytes)
        val window=(sampleRate/50).coerceAtLeast(1)
        var offset=0
        while(offset<frames){
            val count=minOf(window,frames-offset)
            var squares=0.0
            repeat(count*channels){
                val value=when(encoding){
                    PlaybackPcmEncoding.S16->bytes.short/32768f
                    PlaybackPcmEncoding.F32->bytes.float.takeIf{it.isFinite()}?.coerceIn(-1f,1f) ?: 0f
                }
                squares+=value.toDouble()*value
            }
            val begin=startUs+offset*1_000_000L/sampleRate
            val end=(startUs+(offset+count)*1_000_000L/sampleRate).coerceAtMost(MAX_DURATION_US)
            if(begin>=MAX_DURATION_US)break
            if(end>begin)bins.add(Bin(begin,end,sqrt(squares/(count*channels)).toFloat()))
            offset+=count
        }
        if(bins.size>capacity)bins.subList(0,bins.size-capacity).clear()
    }

    @Synchronized fun levelAt(positionUs:Long,playing:Boolean):Float? {
        if(!playing || positionUs<0)return null
        var low=0;var high=bins.size-1;var found=-1
        while(low<=high){val mid=(low+high) ushr 1;if(bins[mid].startUs<=positionUs){found=mid;low=mid+1}else high=mid-1}
        return bins.getOrNull(found)?.takeIf{positionUs<it.endUs}?.rms
    }
    @Synchronized fun clear(){bins.clear()}
    val size:Int get()=synchronized(this){bins.size}
    companion object { const val MAX_DURATION_US=300_000_000L }
}
