package com.jarvis.assistant.voice

import com.jarvis.assistant.hud.BatteryReading
import com.jarvis.assistant.hud.HudSettings
import com.jarvis.assistant.omnisystem.SpeakerSampleBuffer
import com.jarvis.assistant.rebirth.NativeNavigation
import com.jarvis.assistant.sentinel.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class OwnerVoiceAndMotionTest {
    private fun vector(first:Float,second:Float=0f)=FloatArray(32).also{it[0]=first;it[1]=second}
    @Test fun enrolledOwnerMatchesAcrossSmallEmbeddingVariation(){
        assertEquals(OwnerVoiceResult.MATCH,OwnerVoicePolicy.decide(vector(.98f,.1f),vector(1f)).result)
    }
    @Test fun differentVoiceCannotPassTheOwnerFilter(){
        assertEquals(OwnerVoiceResult.DIFFERENT_VOICE,OwnerVoicePolicy.decide(vector(0f,1f),vector(1f)).result)
    }
    @Test fun closeButBelowStrictDefaultVoiceIsRejected(){
        val cosine=.886f;val query=vector(cosine,sqrt(1-cosine*cosine))
        assertEquals(OwnerVoiceResult.DIFFERENT_VOICE,OwnerVoicePolicy.decide(query,vector(1f)).result)
        assertEquals(OwnerVoiceResult.MATCH,OwnerVoicePolicy.decide(query,vector(1f),threshold=.82f).result)
    }
    @Test fun aGuestTooCloseToTheOwnerProducesAmbiguityInsteadOfOwnerPrivileges(){
        assertEquals(OwnerVoiceResult.AMBIGUOUS,OwnerVoicePolicy.decide(vector(1f),vector(1f),listOf(vector(.99f,.01f))).result)
    }
    @Test fun malformedOrUnavailableVoiceVectorsNeverMatch(){
        assertEquals(OwnerVoiceResult.UNAVAILABLE,OwnerVoicePolicy.decide(FloatArray(0),vector(1f)).result)
        assertEquals(OwnerVoiceResult.UNAVAILABLE,OwnerVoicePolicy.decide(FloatArray(32),vector(1f)).result)
        assertEquals(OwnerVoiceResult.UNAVAILABLE,OwnerVoicePolicy.decide(vector(Float.NaN),vector(1f)).result)
    }
    @Test fun aMatchPermitCannotBeReplayedInANewSessionOrAfterTemplateChanges(){
        val permit=OwnerVoicePermit.issued(4,7,1000)
        assertTrue(permit.valid(4,7,1000));assertFalse(permit.valid(5,7,1000));assertFalse(permit.valid(4,8,1000))
        assertFalse(permit.valid(4,7,31_001));assertFalse(permit.valid(4,7,999))
    }
    @Test fun deviceAuthenticatedContinuationCannotResurrectAnExpiredOrRevokedVoicePermit(){
        val permit=OwnerVoicePermit.issued(4,7,1000)
        assertTrue(requireNotNull(permit.afterDeviceAuthentication(5,7,2000)).valid(5,7,2000))
        assertNull(permit.afterDeviceAuthentication(5,8,2000));assertNull(permit.afterDeviceAuthentication(5,7,31_001))
    }
    @Test fun calibrationUsesARealSimilarityValueAndRejectsAnUnrelatedSample(){
        assertEquals(.86f,OwnerVoicePolicy.calibratedThreshold(.90f),.0001f)
        assertEquals(.90f,OwnerVoicePolicy.calibratedThreshold(.98f),.0001f)
        assertEquals(.72f,OwnerVoicePolicy.calibratedThreshold(.74f),.0001f)
        assertThrows(IllegalArgumentException::class.java){OwnerVoicePolicy.calibratedThreshold(.2f)}
        assertThrows(IllegalArgumentException::class.java){OwnerVoicePolicy.calibratedThreshold(Float.NaN)}
    }
    @Test fun trainingCommandsOpenTheRealVoiceProfilesScreen(){
        for(text in listOf("train my voice","train owner voice","only listen to my voice","entrena mi voz"))assertEquals(NativeNavigation.Destination.VOICE,NativeNavigation.destination(text))
    }
    @Test fun theExistingRecorderTapWaitsForEnoughAudioAndDoesNotOpenAnotherRecorder(){
        val samples=SpeakerSampleBuffer()
        val pcm=ByteArray(16000){if(it%2==0)0 else 8}
        samples.feed(pcm,pcm.size,true);assertFalse(samples.hasEnoughAudio())
        samples.feed(pcm,pcm.size,true);assertTrue(samples.hasEnoughAudio())
        val voice=requireNotNull(samples.take(.002f));assertEquals(32000,voice.pcm.size);voice.pcm.fill(0)
        assertFalse(samples.hasEnoughAudio());assertNull(samples.take())
    }
    @Test fun activeCoreTargetsSixtyOnlyWhenTheDisplayAndDeviceBudgetAllowIt(){
        val s=HudSettings();val b=BatteryReading(80)
        assertEquals(60,CoreMotionPolicy.fps(s,b,false,true,SentinelMode.LISTENING,refreshRate=59.94f))
        assertEquals(60,CoreMotionPolicy.fps(s,b,false,true,SentinelMode.SPEAKING,refreshRate=120f))
        assertEquals(30,CoreMotionPolicy.fps(s,b,false,true,SentinelMode.PROCESSING,restricted=true))
        assertEquals(30,CoreMotionPolicy.fps(s,b,false,true,SentinelMode.LISTENING,refreshRate=30f))
    }
    @Test fun heatPowerAndLowBatteryAutomaticallyLowerTheCoreBudget(){
        val s=HudSettings();val b=BatteryReading(80)
        assertEquals(8,CoreMotionPolicy.fps(s,b,true,true,SentinelMode.SPEAKING))
        assertEquals(20,CoreMotionPolicy.fps(s,b,false,true,SentinelMode.PROCESSING,thermal=3))
        assertEquals(8,CoreMotionPolicy.fps(s,b,false,true,SentinelMode.PROCESSING,thermal=4))
        assertEquals(8,CoreMotionPolicy.fps(s,BatteryReading(3),false,true,SentinelMode.LISTENING))
    }
    @Test fun pausedMotionHasAnActualReasonInsteadOfASeeminglyBrokenStaticCore(){
        val s=HudSettings();val b=BatteryReading(80)
        assertEquals(0,CoreMotionPolicy.fps(s.copy(reducedMotion=true),b,false,true,SentinelMode.LISTENING))
        assertEquals(0,CoreMotionPolicy.fps(s,b,false,false,SentinelMode.SPEAKING))
        assertEquals(0,CoreMotionPolicy.fps(s.copy(animationIntensity=0f),b,false,true,SentinelMode.SPEAKING))
        assertEquals("Android animations are disabled",CoreMotionPolicy.pauseReason(s,false))
        assertNull(CoreMotionPolicy.pauseReason(s,true))
    }
    @Test fun idleMotionIsVisibleButStillSlowerThanExecutingATool(){
        val s=HudSettings()
        assertTrue(ReactorGeometry.speed(s,SentinelMode.DORMANT)>=1f)
        assertTrue(ReactorGeometry.speed(s,SentinelMode.PROCESSING,true)>ReactorGeometry.speed(s,SentinelMode.DORMANT))
        assertEquals(12,WallpaperRenderPolicy.fps(true,true,true,s,BatteryReading(80),false,true,SentinelMode.DORMANT))
        assertEquals(0,WallpaperRenderPolicy.fps(false,true,true,s,BatteryReading(80),false,true,SentinelMode.SPEAKING))
    }
    @Test fun microphoneAndPlaybackVisualizationHasNoInventedEnergyWhenSilent(){
        assertEquals(0f,CoreMotionPolicy.visualLevel(0f),0f);assertEquals(0f,CoreMotionPolicy.visualLevel(Float.NaN),0f)
        assertTrue(CoreMotionPolicy.visualLevel(.02f)>0f);assertTrue(CoreMotionPolicy.visualLevel(.08f)>CoreMotionPolicy.visualLevel(.02f))
        assertEquals(1f,CoreMotionPolicy.visualLevel(10f),0f)
    }
    @Test fun breathingIsDecorativeIdleMotionAndNotAFakeListeningWaveform(){
        assertNotEquals(CoreMotionPolicy.breathing(1000,SentinelMode.DORMANT,true),CoreMotionPolicy.breathing(2500,SentinelMode.DORMANT,true))
        assertEquals(1f,CoreMotionPolicy.breathing(1000,SentinelMode.LISTENING,true),0f)
        assertEquals(1f,CoreMotionPolicy.breathing(1000,SentinelMode.DORMANT,false),0f)
    }
    @Test fun errorFeedbackIsBoundedAndDoesNotTrapTheCoreInAnErrorLoop(){
        assertTrue(CoreMotionPolicy.errorVisible(1000,1200));assertFalse(CoreMotionPolicy.errorVisible(1000,1651))
        assertFalse(CoreMotionPolicy.errorVisible(0,1000));assertFalse(CoreMotionPolicy.errorVisible(1000,999))
    }
}
