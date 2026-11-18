package com.jarvis.assistant.controls

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CommandDiagnosticPolicyTest(private val text:String,private val module:String){
    @Test fun diagnosticReportsTheActualNativeRouteOrNormalPipeline(){val result=CommandDiagnosticPolicy.diagnose(text);assertEquals(module,result.module);assertTrue(result.access.isNotBlank());assertTrue(result.intent.isNotBlank())}
    companion object{@JvmStatic @Parameterized.Parameters(name="{0}")fun cases()=listOf(
        arrayOf("Turn on flashlight.","AndroidControlManager"),arrayOf("Hey Jarvis, turn on my flashlight.","AndroidControlManager"),
        arrayOf("Lock my phone.","ScreenSecurityController"),arrayOf("Unlock my phone.","ScreenSecurityController"),
        arrayOf("Open Gmail.","AppLaunchCoordinator"),arrayOf("Mute the volume.","AndroidControlManager"),
        arrayOf("Check battery.","AndroidControlManager"),arrayOf("Show Wi-Fi settings.","AndroidControlManager"),
        arrayOf("Start screen recording.","AndroidControlManager"),arrayOf("Stop screen recording.","AndroidControlManager"),
        arrayOf("Remind me tomorrow.","ReminderManager"),arrayOf("Remind me in 20 minutes to read.","ReminderManager"),
        arrayOf("Catch me up on notifications.","JarvisNotificationService"),arrayOf("Catch me up on my messages.","JarvisNotificationService"),
        arrayOf("Check my email.","GoogleAssistant"),arrayOf("Jarvis, summarize my Gmail.","GoogleAssistant"),
        arrayOf("Open Chrome on my desktop.","MissionController"),arrayOf("Goodbye, Jarvis.","AssistantRuntime.finishConversation"),
        arrayOf("Thank you, Jarvis. I have to go.","AssistantRuntime.finishConversation"),arrayOf("What does goodbye mean?","AssistantRuntime"),
        arrayOf("set farewell style to formal","SpokenSettingsController"),arrayOf("save perplexity api key private-value","AI Brain Manager"),
        arrayOf("What's on my calendar tomorrow?","PhoneContext"),arrayOf("select computer Office","DesktopRegistry")
    )}
}
