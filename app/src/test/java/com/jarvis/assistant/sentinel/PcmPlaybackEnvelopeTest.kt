package com.jarvis.assistant.sentinel

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

class PcmPlaybackEnvelopeTest {
    private fun pcm(vararg values:Float):ByteBuffer=ByteBuffer.allocate(values.size*2).order(ByteOrder.LITTLE_ENDIAN).apply{values.forEach{putShort((it*32768).toInt().coerceIn(-32768,32767).toShort())};flip()}
    @Test fun silenceHasZeroRealEnergyButNoDecodedAudioIsUnavailable(){
        val meter=PcmPlaybackEnvelope();assertNull(meter.levelAt(0,true))
        meter.append(pcm(*FloatArray(320)),0,16000,1,PlaybackPcmEncoding.S16)
        assertEquals(0f,requireNotNull(meter.levelAt(10000,true)),0f);assertNull(meter.levelAt(10000,false));assertNull(meter.levelAt(20000,true))
    }
    @Test fun sourceEnergyChangesAtActualPlaybackTimestamps(){
        val meter=PcmPlaybackEnvelope();val samples=FloatArray(640){if(it<320).25f else .5f}
        meter.append(pcm(*samples),0,16000,1,PlaybackPcmEncoding.S16)
        assertEquals(.25f,requireNotNull(meter.levelAt(10000,true)),.0001f)
        assertEquals(.5f,requireNotNull(meter.levelAt(30000,true)),.0001f)
        // A different playback speed changes the cursor; there is no guessed wall-clock pulse.
        assertEquals(.25f,requireNotNull(meter.levelAt(1000,true)),.0001f)
    }
    @Test fun stereoChannelsDoNotDoubleTheMediaDuration(){
        val meter=PcmPlaybackEnvelope();meter.append(pcm(*FloatArray(640){if(it%2==0).25f else -.25f}),0,16000,2,PlaybackPcmEncoding.S16)
        assertEquals(.25f,requireNotNull(meter.levelAt(19000,true)),.0001f);assertNull(meter.levelAt(20000,true))
    }
    @Test fun floatPcmIsMeasuredAndMalformedSamplesCannotCreateInfiniteEnergy(){
        val bytes=ByteBuffer.allocate(1280).order(ByteOrder.LITTLE_ENDIAN).apply{repeat(320){putFloat(if(it==0)Float.NaN else .5f)};flip()}
        val meter=PcmPlaybackEnvelope();meter.append(bytes,0,16000,1,PlaybackPcmEncoding.F32)
        val rms=requireNotNull(meter.levelAt(10000,true));assertTrue(rms.isFinite());assertTrue(rms in .49f.. .5f)
    }
    @Test fun unavailableGapsAndPausedPlayersNeverInventSound(){
        val meter=PcmPlaybackEnvelope();meter.append(pcm(*FloatArray(320){.5f}),40000,16000,1,PlaybackPcmEncoding.S16)
        assertNull(meter.levelAt(0,true));assertNull(meter.levelAt(30000,true));assertNull(meter.levelAt(45000,false));assertNull(meter.levelAt(-1,true))
    }
    @Test fun onlyTheCodecOutputSliceIsMeasured(){
        val bytes=pcm(*FloatArray(960){if(it in 320..639).25f else .75f});bytes.position(640);bytes.limit(1280)
        val meter=PcmPlaybackEnvelope();meter.append(bytes,0,16000,1,PlaybackPcmEncoding.S16)
        assertEquals(.25f,requireNotNull(meter.levelAt(10000,true)),.0001f);assertNull(meter.levelAt(20000,true))
    }
    @Test fun boundedTimelineAndCleanupRetainNoFutureFakeFrames(){
        val meter=PcmPlaybackEnvelope(2);meter.append(pcm(*FloatArray(960){.5f}),0,16000,1,PlaybackPcmEncoding.S16)
        assertEquals(2,meter.size);assertNull(meter.levelAt(1000,true));assertNotNull(meter.levelAt(45000,true))
        meter.clear();assertEquals(0,meter.size);assertNull(meter.levelAt(45000,true))
    }
    @Test fun invalidFormatsAndNonMonotonicTimestampsAreRejected(){
        val meter=PcmPlaybackEnvelope();val bytes=pcm(*FloatArray(320){.5f})
        assertThrows(IllegalArgumentException::class.java){meter.append(bytes,0,0,1,PlaybackPcmEncoding.S16)}
        assertThrows(IllegalArgumentException::class.java){meter.append(bytes,0,16000,0,PlaybackPcmEncoding.S16)}
        meter.append(bytes,40000,16000,1,PlaybackPcmEncoding.S16)
        assertThrows(IllegalArgumentException::class.java){meter.append(bytes,0,16000,1,PlaybackPcmEncoding.S16)}
    }
    @Test fun anObsoleteDecoderCannotClearOrPublishIntoTheNextVoiceMeter(){
        val first=Any();val second=Any()
        try {
            PlaybackLevels.start(first,"Fish voice");PlaybackLevels.update(first,.5f,true)
            PlaybackLevels.start(second,"Neural voice");PlaybackLevels.update(second,.2f,true)
            PlaybackLevels.update(first,1f,true);PlaybackLevels.stop(first)
            assertEquals(.2f,PlaybackLevels.state.value.level,0f);assertEquals("Neural voice",PlaybackLevels.state.value.source)
        }finally{PlaybackLevels.stop(first);PlaybackLevels.stop(second)}
        assertFalse(PlaybackLevels.state.value.active)
    }
}
