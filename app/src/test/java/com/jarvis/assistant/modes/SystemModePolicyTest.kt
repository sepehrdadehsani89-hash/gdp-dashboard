package com.jarvis.assistant.modes

import com.jarvis.assistant.hud.HudState
import org.junit.Assert.*
import org.junit.Test

class SystemModePolicyTest {
    private val active=SystemModeState();private val quiet=SystemModeState(silent=true)
    @Test fun activePreservesWakePolicy(){assertTrue(SystemModePolicy.wakeAllowed(active))}
    @Test fun silentDefaultsToNoWake(){assertFalse(SystemModePolicy.wakeAllowed(quiet))}
    @Test fun explicitSilentWakeExceptionIsAllowed(){assertTrue(SystemModePolicy.wakeAllowed(quiet.copy(wakeInSilent=true)))}
    @Test fun deepAlwaysBlocksWake(){assertFalse(SystemModePolicy.wakeAllowed(active.copy(deep=true,wakeInSilent=true)))}
    @Test fun deepBlocksEvenSilentWakeException(){assertFalse(SystemModePolicy.wakeAllowed(quiet.copy(deep=true,wakeInSilent=true)))}
    @Test fun silentBlocksOptionalBackgroundActivity(){assertFalse(SystemModePolicy.optionalActivityAllowed(quiet))}
    @Test fun deepBlocksOptionalBackgroundActivity(){assertFalse(SystemModePolicy.optionalActivityAllowed(active.copy(deep=true)))}
    @Test fun activeAllowsExistingOptionalActivity(){assertTrue(SystemModePolicy.optionalActivityAllowed(active))}
    @Test fun silentPreservesReminderExceptionByDefault(){assertTrue(SystemModePolicy.remindersAllowed(quiet))}
    @Test fun explicitSilentReminderOptOutIsRespected(){assertFalse(SystemModePolicy.remindersAllowed(quiet.copy(keepReminders=false)))}
    @Test fun deepPreservesOnlyChosenReminderException(){assertTrue(SystemModePolicy.remindersAllowed(active.copy(deep=true,keepReminders=true)))}
    @Test fun deepCanPauseRemindersWithoutDeletingThem(){assertFalse(SystemModePolicy.remindersAllowed(active.copy(deep=true,keepReminders=false)))}
    @Test fun activeRemindersWorkWithoutQuietException(){assertTrue(SystemModePolicy.remindersAllowed(active.copy(keepReminders=false)))}
    @Test fun silentHidesPrivatePreviews(){assertFalse(SystemModePolicy.privatePreviewsAllowed(quiet))}
    @Test fun deepHidesPrivatePreviews(){assertFalse(SystemModePolicy.privatePreviewsAllowed(active.copy(deep=true)))}
    @Test fun activeAllowsSeparateSourceConsents(){assertTrue(SystemModePolicy.privatePreviewsAllowed(active))}
    @Test fun exactSilentCommandHasTypedIntent(){assertEquals(SystemModeCommand.SILENT,SystemModePolicy.parse("Activate Silent Mode."))}
    @Test fun naturalQuietCommandHasTypedIntent(){assertEquals(SystemModeCommand.SILENT,SystemModePolicy.parse("Enter quiet mode"))}
    @Test fun focusProtocolIsRecognized(){assertEquals(SystemModeCommand.SILENT,SystemModePolicy.parse("Activate focus protocol"))}
    @Test fun deepWakePrefixIsRecognized(){assertEquals(SystemModeCommand.DEEP,SystemModePolicy.parse("Hey Jarvis, shut down your systems."))}
    @Test fun deactivateAssistantRequiresDeepConfirmation(){assertEquals(SystemModeCommand.DEEP,SystemModePolicy.parse("Deactivate Jarvis completely"))}
    @Test fun deepCommandIsRecognized(){assertEquals(SystemModeCommand.DEEP,SystemModePolicy.parse("Enter Deep Shutdown"))}
    @Test fun modeSettingsAreSeparateFromMutation(){assertEquals(SystemModeCommand.SETTINGS,SystemModePolicy.parse("Show system modes"))}
    @Test fun exitQuietIsASeparateIntent(){assertEquals(SystemModeCommand.ACTIVE,SystemModePolicy.parse("Exit Silent Mode"))}
    @Test fun negatedShutdownDoesNotTrigger(){assertNull(SystemModePolicy.parse("Do not enter Deep Shutdown"))}
    @Test fun questionAboutModeDoesNotExecuteIt(){assertNull(SystemModePolicy.parse("What is Silent Mode?"))}
    @Test fun quotedTaskDoesNotTriggerShutdown(){assertNull(SystemModePolicy.parse("Translate enter deep shutdown to Spanish"))}
    @Test fun unrelatedPhoneShutdownIsNotAssistantShutdown(){assertNull(SystemModePolicy.parse("Shut down my phone"))}
    @Test fun originalVisualSettingsStayIdenticalInActiveMode(){val h=HudState();assertSame(h,ModePresentation.hud(h,active))}
    @Test fun quietVisualsDoNotMutateSavedSettings(){val h=HudState();val result=ModePresentation.hud(h,quiet);assertEquals(.65f,h.settings.sentinel.brightness,0f);assertTrue(result.settings.sentinel.brightness<h.settings.sentinel.brightness);assertEquals("saver",result.settings.animationProfile)}
    @Test fun explicitReducedMotionRemainsRespected(){val h=HudState();val saved=h.copy(settings=h.settings.copy(reducedMotion=true));assertTrue(ModePresentation.hud(saved,quiet).settings.reducedMotion)}
    @Test fun deepDisablesOwnedMotion(){val r=ModePresentation.hud(HudState(),active.copy(deep=true));assertTrue(r.settings.reducedMotion);assertEquals(0f,r.settings.sentinel.animationSpeed,0f)}
    @Test fun highQualityDoesNotChangeSavedDefault(){val h=HudState();assertEquals("cinematic",ModePresentation.hud(h,active,"high").settings.animationProfile);assertEquals("balanced",h.settings.animationProfile)}
    @Test fun silentWakeAnimationHasFourSecondTimeline(){assertEquals(4000L,SystemModePolicy.activationMillis(quiet.copy(wakeInSilent=true)))}
    @Test fun normalActivationDurationIsPreserved(){assertEquals(2100L,SystemModePolicy.activationMillis(active))}
    @Test fun animationMappingDoesNotDelayExecution(){assertEquals(1050L,SystemModePolicy.animationElapsed(2000,4000));assertEquals(2100L,SystemModePolicy.animationElapsed(4000,4000))}
    @Test fun animationMappingHandlesStaleTimes(){assertEquals(0L,SystemModePolicy.animationElapsed(-1,4000));assertEquals(2100L,SystemModePolicy.animationElapsed(5000,4000))}
}
