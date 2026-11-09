package com.jarvis.assistant.sentinel

import com.jarvis.assistant.core.*
import com.jarvis.assistant.data.BrainProtocol
import com.jarvis.assistant.local.LocalPromptBuilder
import com.jarvis.assistant.hud.*
import com.jarvis.assistant.voice.RecognitionConfidence
import com.jarvis.assistant.tools.MediaCommand
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class SentinelPolicyTest {
    @Test fun awakeningHasOrderedStagesWithoutHoldingTheCommandWindow(){
        val start=ReactorGeometry.awakening(0);assertEquals(0f,start.core,0f);assertEquals(0f,start.panels,0f)
        val ignition=ReactorGeometry.awakening(500);assertEquals(1f,ignition.core,0f);assertTrue(ignition.rings>0);assertEquals(0f,ignition.panels,0f)
        val panels=ReactorGeometry.awakening(1400);assertTrue(panels.panels>0);assertFalse(panels.complete)
        assertTrue(ReactorGeometry.awakening(2100).complete)
        var now=0L;val visual=ReactorAnimationController{now};visual.activate();visual.update(AssistantPhase.LISTENING,true,null)
        assertEquals(SentinelMode.ACTIVATING,visual.state.value.mode)
        now=2100;visual.settle();assertEquals(SentinelMode.LISTENING,visual.state.value.mode)
    }
    @Test fun activationDeduplicatesAndBusyOperationsDoNotWaitForTheVisualSequence(){
        var now=0L;val visual=ReactorAnimationController{now};visual.activate();visual.activate();assertEquals(1,visual.state.value.activationId)
        now=200;visual.update(AssistantPhase.THINKING,false,null);assertEquals(SentinelMode.PROCESSING,visual.state.value.mode)
        visual.update(AssistantPhase.SPEAKING,false,null);assertEquals(SentinelMode.SPEAKING,visual.state.value.mode)
    }
    @Test fun followUpArmingKeepsTheSceneAwakeAndExpiryReturnsItToDormant(){
        var now=0L;val visual=ReactorAnimationController{now};visual.update(AssistantPhase.SPEAKING,false,null)
        visual.update(AssistantPhase.READY,true,null);assertEquals(SentinelMode.LISTENING,visual.state.value.mode)
        now=10_000;visual.update(AssistantPhase.READY,false,null);assertEquals(SentinelMode.RETURNING_TO_WALLPAPER,visual.state.value.mode)
        now+=599;visual.settle();assertEquals(SentinelMode.RETURNING_TO_WALLPAPER,visual.state.value.mode)
        now++;visual.settle();assertEquals(SentinelMode.DORMANT,visual.state.value.mode)
    }
    @Test fun authorizationAndErrorStatesUseActualRuntimeState(){
        val visual=ReactorAnimationController{0L}
        visual.update(AssistantPhase.READY,false,AuthorizationStatus.AWAITING_CODEX);assertEquals(SentinelMode.AUTHORIZATION_REQUIRED,visual.state.value.mode)
        visual.update(AssistantPhase.ERROR,false,null);assertEquals(SentinelMode.ERROR,visual.state.value.mode)
    }
    @Test fun sharedRingsCounterRotateAndGeometryDoesNotDependOnFakeMetrics(){
        assertEquals(120,ReactorGeometry.ticks.size);assertEquals(3,ReactorGeometry.arcScales.size)
        assertTrue(ReactorGeometry.angle(1000,1f,0)>0);assertTrue(ReactorGeometry.angle(1000,1f,1)<0)
        assertEquals(ReactorGeometry.angle(12345,1f,2),ReactorGeometry.angle(12345,1f,0),0f)
    }
    @Test fun invisibleSurfacesAndScreenOffNeverRequestAnAnimationLoop(){
        val s=HudSettings();val b=BatteryReading(80)
        assertEquals(0,WallpaperRenderPolicy.fps(false,true,true,s,b,false,true,SentinelMode.LISTENING))
        assertEquals(0,WallpaperRenderPolicy.fps(true,false,true,s,b,false,true,SentinelMode.LISTENING))
        assertEquals(0,WallpaperRenderPolicy.fps(true,true,false,s,b,false,true,SentinelMode.LISTENING))
    }
    @Test fun wallpaperHonorsReducedMotionPowerAndLowBattery(){
        val s=HudSettings();fun fps(settings:HudSettings=s,battery:Int=80,power:Boolean=false,motion:Boolean=true)=WallpaperRenderPolicy.fps(true,true,true,settings,BatteryReading(battery),power,motion,SentinelMode.LISTENING)
        assertEquals(0,fps(s.copy(reducedMotion=true)));assertEquals(0,fps(motion=false));assertEquals(0,fps(s.copy(sentinel=SentinelSettings(animationSpeed=0f))))
        assertEquals(2,fps(power=true));assertEquals(2,fps(battery=9));assertEquals(4,fps(battery=19));assertEquals(30,fps(s.copy(animationProfile="cinematic")))
    }
    @Test fun dormantAndErrorScenesStayCheap(){
        val s=HudSettings();val b=BatteryReading(80)
        assertEquals(3,WallpaperRenderPolicy.fps(true,true,true,s,b,false,true,SentinelMode.DORMANT))
        assertEquals(20,WallpaperRenderPolicy.fps(true,true,true,s,b.copy(charging=true),false,true,SentinelMode.DORMANT))
        assertEquals(3,WallpaperRenderPolicy.fps(true,true,true,s,b,false,true,SentinelMode.ERROR))
    }
    @Test fun soundNeverOverridesMuteDndCallsOrCommandSpeech(){
        fun volume(quiet:Boolean=false,call:Boolean=false,mic:Boolean=false,level:Float=0f,speaking:Boolean=false,event:SentinelSound=SentinelSound.BUTTON)=SoundMixPolicy.volume(true,1f,quiet,call,mic,level,speaking,event)
        assertEquals(0f,volume(quiet=true),0f);assertEquals(0f,volume(call=true),0f);assertEquals(0f,volume(mic=true),0f)
        assertEquals(.12f,volume(mic=true,event=SentinelSound.WAKE),.0001f)
        assertEquals(0f,volume(mic=true,level=.1f,event=SentinelSound.WAKE),0f)
        assertEquals(.15f,volume(speaking=true),.0001f)
        assertEquals(0f,SoundMixPolicy.volume(false,1f,false,false,false,0f,false,SentinelSound.WAKE),0f)
    }
    @Test fun newSettingsHaveSafeAdditiveDefaultsAndRejectInvalidValues(){
        val s=SentinelSettings(reactorScale=100f,animationSpeed=-1f,particleDensity=Float.NaN,effectsVolume=Float.POSITIVE_INFINITY,activationSound="unknown").sanitized()
        assertEquals(1.1f,s.reactorScale,0f);assertEquals(0f,s.animationSpeed,0f);assertTrue(s.particleDensity.isFinite());assertEquals(.35f,s.effectsVolume,0f);assertEquals("chime",s.activationSound)
        assertFalse(SentinelSettings().wallpaperWeather);assertTrue(SentinelSettings().wallpaperBattery)
    }
    @Test fun wordConfidenceRejectsNoiseWithoutPretendingUnavailableScoresExist(){
        assertFalse(RecognitionConfidence.acceptsFinal(JSONObject("""{"text":""}""")))
        assertFalse(RecognitionConfidence.acceptsFinal(JSONObject("""{"text":"open youtube","result":[{"conf":0.1},{"conf":0.2}]}""")))
        assertTrue(RecognitionConfidence.acceptsFinal(JSONObject("""{"text":"open youtube","result":[{"conf":0.9},{"conf":0.8}]}""")))
        assertTrue(RecognitionConfidence.acceptsFinal(JSONObject("""{"text":"open youtube"}""")))
    }
    @Test fun replyStyleChangesTrustedInstructionsAndKeepsToolBoundaries(){
        val short=BrainProtocol.systemPrompt(AppLanguage.EN,"",LocalDate.of(2026,11,2),true)
        val detailed=BrainProtocol.systemPrompt(AppLanguage.EN,"",LocalDate.of(2026,11,2),false)
        assertTrue(short.contains("under 80 words"));assertTrue(detailed.contains("useful detail"));assertTrue(detailed.contains("cannot directly control"))
        val local=LocalPromptBuilder.build("Explain tides",AppLanguage.EN,emptyList(),false)
        assertTrue(local.contains("well-explained"));assertTrue(local.contains("You have no tools"))
    }
    @Test fun weatherQueriesUseConfiguredRealDataWithoutHijackingExplanations(){
        assertTrue(WeatherQuestion.matches("weather today"));assertTrue(WeatherQuestion.matches("What's the weather today?"));assertTrue(WeatherQuestion.matches("هوا امروز چطوره"))
        assertFalse(WeatherQuestion.matches("explain weather models"));assertFalse(WeatherQuestion.isRequest("explain weather models"));assertFalse(WeatherQuestion.isRequest("what is weather?"));assertTrue(WeatherQuestion.isRequest("weather in Quito"))
        val r=WeatherReading("Fixture",null,null,null,null,null,null,null,null,"2026-11-02T10:00",0)
        val text=WeatherQuestion.describe(r,AppLanguage.EN,false);assertTrue(text.contains("unavailable"));assertFalse(text.contains("72"))
    }
    @Test fun backgroundActivationNeverMovesAKnownForeignAppTarget(){
        assertFalse(ActivationPolicy.mayOpen(false,false,true,true,"com.whatsapp","com.android.launcher3"))
        assertFalse(ActivationPolicy.mayOpen(true,false,true,false,null,null))
        assertFalse(ActivationPolicy.mayOpen(false,true,true,false,null,null))
        assertFalse(ActivationPolicy.mayOpen(false,false,false,false,null,null))
        assertTrue(ActivationPolicy.mayOpen(false,false,true,false,null,null))
        assertTrue(ActivationPolicy.mayOpen(false,false,false,true,"com.android.launcher3","com.android.launcher3"))
    }
    @Test fun mediaCommandsAreExplicitAndNeverParsedFromAnExplanation(){
        assertEquals("pause",MediaCommand.parse("pause music"));assertEquals("next",MediaCommand.parse("آهنگ بعدی"));assertEquals("play",MediaCommand.parse("reproduce música"));assertNull(MediaCommand.parse("explain how to pause music"))
    }
}
