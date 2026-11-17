package com.jarvis.assistant

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.assistant.hud.HudPreferences
import com.jarvis.assistant.titan.VoicePreferences
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Real Android preference adapters in a unique namespace; never modifies the owner's settings. */
@RunWith(AndroidJUnit4::class)
class StabilizationPreferencesTest {
    private lateinit var isolated:Context
    private val opened=mutableListOf<SharedPreferences>()
    @Before fun prepare(){val prefix="stabilization_fixture_"+java.util.UUID.randomUUID().toString()+"_";isolated=object:ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext){
        override fun getApplicationContext():Context=this
        override fun getSharedPreferences(name:String,mode:Int):SharedPreferences=super.getSharedPreferences(prefix+name,mode).also{opened+=it}
    }}
    @After fun clearFixtures(){opened.forEach{it.edit().clear().commit()}}
    private fun change(edit:(com.jarvis.assistant.hud.HudSettings)->com.jarvis.assistant.hud.HudSettings)=HudPreferences(isolated).let{it.save(edit(it.read()));HudPreferences(isolated).read()}
    @Test fun motionOffSurvivesAdapterReload(){assertFalse(change{it.copy(reducedMotion=false)}.reducedMotion)}
    @Test fun motionOnSurvivesAdapterReload(){assertTrue(change{it.copy(reducedMotion=true)}.reducedMotion)}
    @Test fun memoryProjectionNeverPersistsMotion(){val p=HudPreferences(isolated);p.save(p.read().copy(reducedMotion=false));val projected=com.jarvis.assistant.hud.MotionSettingsPolicy.project(com.jarvis.assistant.hud.HudState(settings=p.read()),true);p.save(projected.settings.copy(weatherCity="Fixture City"));assertFalse(HudPreferences(isolated).read().reducedMotion)}
    @Test fun savingVoiceVolumePreservesMotion(){assertFalse(change{it.copy(voiceVolume=.4f,reducedMotion=false)}.reducedMotion)}
    @Test fun savingReducedMotionPreservesVoice(){val h=change{it.copy(voiceVolume=.4f,reducedMotion=true)};assertEquals(.4f,h.voiceVolume,.001f)}
    @Test fun batterySaverDoesNotEnableUserMotion(){val h=change{it.copy(autoBatterySaver=true,reducedMotion=false)};assertTrue(h.autoBatterySaver);assertFalse(h.reducedMotion)}
    @Test fun appearanceDoesNotEraseWallpaper(){val h=change{it.copy(reducedMotion=false,sentinel=it.sentinel.copy(wallpaperClock=false,wallpaperBattery=true))};assertFalse(h.sentinel.wallpaperClock);assertTrue(h.sentinel.wallpaperBattery)}
    @Test fun ringSpeedPersists(){assertEquals(.5f,change{it.copy(sentinel=it.sentinel.copy(animationSpeed=.5f))}.sentinel.animationSpeed,.001f)}
    @Test fun particlesPersist(){assertEquals(.25f,change{it.copy(sentinel=it.sentinel.copy(particleDensity=.25f))}.sentinel.particleDensity,.001f)}
    @Test fun chargingChoicePersists(){assertFalse(change{it.copy(sentinel=it.sentinel.copy(chargingEffects=false))}.sentinel.chargingEffects)}
    @Test fun legacyInterruptionTrueCannotReenableCapture(){isolated.getSharedPreferences("jarvis_titan_voice_v1",0).edit().putBoolean("playback_interruption",true).commit();assertFalse(VoicePreferences(isolated).playbackInterruption)}
    @Test fun defaultContinuousIsOff(){assertFalse(VoicePreferences(isolated).continuousConversation)}
    @Test fun explicitContinuousPersists(){VoicePreferences(isolated).continuousConversation=true;assertTrue(VoicePreferences(isolated).continuousConversation)}
    @Test fun disablingContinuousPersists(){val p=VoicePreferences(isolated);p.continuousConversation=true;p.continuousConversation=false;assertFalse(VoicePreferences(isolated).continuousConversation)}
    @Test fun cooldownPersists(){VoicePreferences(isolated).speechCooldownMs=900;assertEquals(900L,VoicePreferences(isolated).speechCooldownMs)}
    @Test fun cooldownCannotBeZero(){VoicePreferences(isolated).speechCooldownMs=0;assertEquals(250L,VoicePreferences(isolated).speechCooldownMs)}
    @Test fun cooldownCannotBeUnbounded(){VoicePreferences(isolated).speechCooldownMs=Long.MAX_VALUE;assertEquals(2000L,VoicePreferences(isolated).speechCooldownMs)}
    @Test fun oldFollowUpSettingNotReactivated(){isolated.getSharedPreferences("jarvis_titan_voice_v1",0).edit().putBoolean("continuous_conversation",true).commit();assertFalse(VoicePreferences(isolated).continuousConversation)}
    @Test fun noiseChoicePersists(){VoicePreferences(isolated).noiseReduction=false;assertFalse(VoicePreferences(isolated).noiseReduction)}
    @Test fun sensitivityPersistsWithoutChangingCooldown(){val p=VoicePreferences(isolated);p.speechCooldownMs=800;p.sensitivity="LOW";val read=VoicePreferences(isolated);assertEquals("LOW",read.sensitivity);assertEquals(800L,read.speechCooldownMs)}
}
