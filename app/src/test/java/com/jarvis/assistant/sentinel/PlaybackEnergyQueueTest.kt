package com.jarvis.assistant.sentinel

import org.junit.Assert.*
import org.junit.Test

class PlaybackEnergyQueueTest {
    @Test fun queuedFutureSamplesNeverPretendToBeCurrentPlayback(){
        val q=PlaybackEnergyQueue();q.written(floatArrayOf(.8f,.8f),0,2,100)
        assertEquals(0f,q.levelAt(99,true),0f);assertEquals(.8f,q.levelAt(100,true),.001f)
        assertEquals(0f,q.levelAt(102,true),0f)
    }
    @Test fun playbackCursorChoosesTheCorrectSubmittedBin(){
        val q=PlaybackEnergyQueue();q.written(floatArrayOf(.2f,.2f,.6f,.6f),0,2,0);q.written(floatArrayOf(.2f,.2f,.6f,.6f),2,2,2)
        assertEquals(.2f,q.levelAt(1,true),.001f);assertEquals(.6f,q.levelAt(2,true),.001f);assertEquals(0f,q.levelAt(4,true),0f)
    }
    @Test fun pausedAndStoppedTracksHaveNoOutputEnergy(){
        val q=PlaybackEnergyQueue();q.written(floatArrayOf(.5f),0,1,0)
        assertEquals(0f,q.levelAt(0,false),0f);assertEquals(0f,q.levelAt(-1,true),0f)
        q.clear();assertEquals(0,q.size)
    }
    @Test fun buffersStayBoundedAndDoNotReturnStaleRms(){
        val q=PlaybackEnergyQueue(3);repeat(20){q.written(floatArrayOf(.4f),0,1,it.toLong())}
        assertEquals(3,q.size);assertEquals(0f,q.levelAt(0,true),0f);assertEquals(.4f,q.levelAt(19,true),.001f)
    }
    @Test fun measuredSilenceIsDistinctFromUnavailablePlayback(){
        val q=PlaybackEnergyQueue();q.written(floatArrayOf(0f,0f),0,2,10)
        assertNull(q.readingAt(9,true));assertEquals(0f,q.readingAt(10,true)!!,0f);assertNull(q.readingAt(10,false))
    }
    @Test(expected=IllegalArgumentException::class)fun overlappingFrameRangesAreRejected(){
        val q=PlaybackEnergyQueue();q.written(FloatArray(10),0,10,0);q.written(FloatArray(10),0,10,1)
    }
}
