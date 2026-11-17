package com.jarvis.assistant.hud

import com.jarvis.assistant.sentinel.CoreMotionPolicy
import com.jarvis.assistant.sentinel.SentinelMode
import org.junit.Assert.*
import org.junit.Test

class MotionSettingsPolicyTest {
    private fun state(reduced:Boolean=false)=HudState(settings=HudSettings(reducedMotion=reduced))
    @Test fun normalProjectionPreservesFullMotion(){assertFalse(MotionSettingsPolicy.project(state(),false).settings.reducedMotion)}
    @Test fun memoryPressureCannotForceUserSwitchOn(){assertFalse(MotionSettingsPolicy.project(state(),true).settings.reducedMotion)}
    @Test fun reducedPreferenceSurvivesMemoryRecovery(){assertTrue(MotionSettingsPolicy.project(state(true),false).settings.reducedMotion)}
    @Test fun reducedPreferenceSurvivesMemoryPressure(){assertTrue(MotionSettingsPolicy.project(state(true),true).settings.reducedMotion)}
    @Test fun togglingOffRemainsOffUnderPressure(){val before=state(true);val changed=before.copy(settings=before.settings.copy(reducedMotion=false));assertFalse(MotionSettingsPolicy.project(changed,true).settings.reducedMotion)}
    @Test fun savingAnotherControlDoesNotPersistForcedReduction(){val visible=MotionSettingsPolicy.project(state(),true);val saved=visible.settings.copy(accent="purple");assertFalse(saved.reducedMotion)}
    @Test fun particlePreferenceIsNotDestroyedByTemporaryPressure(){val s=state();assertEquals(s.settings.sentinel.particleDensity,MotionSettingsPolicy.project(s,true).settings.sentinel.particleDensity)}
    @Test fun repeatedPressureCyclesDoNotLatchMotion(){var s=state();repeat(50){s=MotionSettingsPolicy.project(s,it%2==0)};assertFalse(s.settings.reducedMotion)}
    @Test fun memoryAdaptationKeepsAnimationAlive(){val s=MotionSettingsPolicy.project(state(),true);assertTrue(CoreMotionPolicy.fps(s.settings,BatteryReading(),false,true,SentinelMode.LISTENING,true)>0)}
    @Test fun systemAccessibilityStillStopsMotion(){assertEquals(0,CoreMotionPolicy.fps(state().settings,BatteryReading(),false,false,SentinelMode.LISTENING))}
    @Test fun explicitUserReductionStillStopsMotion(){assertEquals(0,CoreMotionPolicy.fps(state(true).settings,BatteryReading(),false,true,SentinelMode.LISTENING))}
    @Test fun saverAdaptsFrameBudgetWithoutChangingSwitch(){val s=state();assertEquals(8,CoreMotionPolicy.fps(s.settings,BatteryReading(),true,true,SentinelMode.LISTENING));assertFalse(s.settings.reducedMotion)}
    @Test fun thermalAdaptationIsDistinctFromUserReduction(){assertEquals(8,CoreMotionPolicy.fps(state().settings,BatteryReading(),false,true,SentinelMode.LISTENING,thermal=4))}
    @Test fun memoryRestrictionIsExplained(){assertTrue(MotionSettingsPolicy.description(false,true,false,true).contains("Temporary memory"))}
    @Test fun fullMotionPreferenceIsExplained(){assertTrue(MotionSettingsPolicy.description(false,true,false,false).contains("Full Motion"))}
    @Test fun reducedPreferenceIsExplained(){assertTrue(MotionSettingsPolicy.description(true,true,false,false).contains("Reduced Motion"))}
    @Test fun accessibilityReasonIsNotReportedAsUserChoice(){val text=MotionSettingsPolicy.description(false,false,false,false);assertTrue(text.contains("Full Motion"));assertTrue(text.contains("Android animations are disabled"))}
    @Test fun saverReasonIsSeparate(){assertTrue(MotionSettingsPolicy.description(false,true,true,false).contains("Battery Saver"))}
    @Test fun noSystemRestrictionIsInvented(){assertFalse(MotionSettingsPolicy.description(false,true,false,false).contains("disabled"))}
    @Test fun multipleConstraintsRemainVisible(){val text=MotionSettingsPolicy.description(false,false,true,true);assertTrue(text.contains("Android"));assertTrue(text.contains("Battery Saver"));assertTrue(text.contains("Temporary memory"))}
}
