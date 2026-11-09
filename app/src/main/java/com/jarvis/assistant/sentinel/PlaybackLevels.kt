package com.jarvis.assistant.sentinel

import android.media.AudioTrack
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** PCM levels tied to an actual AudioTrack/MediaPlayer cursor, not future synthesis callbacks. */
data class PlaybackReading(val active:Boolean=false,val level:Float=0f,val history:List<Float> = emptyList(),val source:String="Neural voice")
object PlaybackLevels {
    private var owner:Any?=null
    private var source="Neural voice"
    private val mutable=MutableStateFlow(PlaybackReading())
    val state=mutable.asStateFlow()
    @Synchronized fun start(token:Any,source:String="Neural voice"){owner=token;this.source=source;mutable.value=PlaybackReading(active=false,source=source)}
    @Synchronized fun update(token:Any,level:Float,available:Boolean){if(owner!==token)return;val v=level.takeIf{it.isFinite()}?.coerceIn(0f,1f) ?: 0f;mutable.value=PlaybackReading(available,v,(mutable.value.history+v).takeLast(32),source)}
    @Synchronized fun stop(token:Any){if(owner===token){owner=null;mutable.value=PlaybackReading()}}
}
internal class PlaybackCursorMeter(private val track:AudioTrack) {
    private val energy=PlaybackEnergyQueue()
    private val worker=HandlerThread("JarvisPlaybackMeter").apply{start()}
    private val handler=Handler(worker.looper)
    @Volatile private var closed=false
    @Volatile var lastCursor=0L;private set
    @Volatile var observedPeak=0f;private set
    @Volatile var samplesTaken=0L;private set
    private var publishedAt=0L
    private var publishedLevel=0f
    private var publishedAvailable=false
    private val refresh=Runnable{sampleCursor()}
    private val poll=object:Runnable{override fun run(){
        if(closed)return
        sampleCursor()
        if(!closed)handler.postDelayed(this,80)
    }}
    init{PlaybackLevels.start(this);handler.post(poll)}
    private fun sampleCursor(){
        if(closed)return
        val cursor=runCatching{track.playbackHeadPosition.toLong() and 0xffffffffL}.getOrDefault(0)
        val playing=runCatching{track.playState==AudioTrack.PLAYSTATE_PLAYING}.getOrDefault(false)
        lastCursor=cursor;samplesTaken++
        // A queued synthesis buffer alone is not playback. Wait for cursor advancement.
        val reading=energy.readingAt(cursor,playing && cursor>0)
        val level=reading ?: 0f
        val available=reading!=null
        observedPeak=maxOf(observedPeak,level)
        val now=SystemClock.elapsedRealtime()
        if(now-publishedAt>=65 || (publishedLevel==0f)!=(level==0f) || publishedAvailable!=available){
            publishedAt=now;publishedLevel=level;publishedAvailable=available;PlaybackLevels.update(this,level,available)
        }
    }
    fun written(samples:FloatArray,offset:Int,count:Int,start:Long){
        if(closed)return
        energy.written(samples,offset,count,start)
        // Writes can resume after an underrun. Refresh without waiting on the UI thread.
        handler.removeCallbacks(refresh);handler.post(refresh)
    }
    fun close(){closed=true;handler.removeCallbacksAndMessages(null);energy.clear();PlaybackLevels.stop(this);worker.quitSafely()}
}
