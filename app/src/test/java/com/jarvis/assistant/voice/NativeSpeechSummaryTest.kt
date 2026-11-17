package com.jarvis.assistant.voice
import com.jarvis.assistant.core.AppLanguage
import org.junit.Assert.*
import org.junit.Test
class NativeSpeechSummaryTest {
    private fun en(k:String)=NativeSpeechSummary.confirmation(k,AppLanguage.EN)!!
    @Test fun contactRequiresFinalCall(){assertTrue(en("dial_contact").contains("press Call"))}
    @Test fun alarmRequiresReview(){assertTrue(en("set_alarm").contains("Review the time"))}
    @Test fun timerRequiresSave(){assertTrue(en("set_timer").contains("save it there"))}
    @Test fun reminderNotReportedSaved(){assertTrue(en("reminder_draft").contains("no reminder has been saved"))}
    @Test fun screenTapNotReportedDone(){assertTrue(en("live_control").contains("not been activated"))}
    @Test fun linkDoesNotClaimMessageSent(){assertTrue(en("open_url").contains("manual review and sending"))}
    @Test fun panelDoesNotClaimToggle(){assertTrue(en("system_panel").contains("No setting has changed"))}
    @Test fun mediaIsRequestNotPlaybackClaim(){assertTrue(en("media_control").contains("sending your requested"))}
    @Test fun openingAppNeedsApproval(){assertTrue(en("open_app").contains("Confirm"))}
    @Test fun aliasIsLocalAndPending(){assertTrue(en("app_alias").contains("Confirm saving"));assertTrue(en("app_alias").contains("locally"))}
    @Test fun diagnosticTestsDoNotAutoRun(){assertTrue(en("infinity_controls").contains("require your action"))}
    @Test fun homeNavigationCanBeCancelled(){assertTrue(en("nav_home").contains("cancel"))}
    @Test fun backNavigationCanBeCancelled(){assertTrue(en("nav_back").contains("cancel"))}
    @Test fun unknownToolDefaultsPrivate(){assertNull(NativeSpeechSummary.confirmation("read_private_calendar",AppLanguage.EN));assertFalse(NativeSpeechSummary.publicResult("read_private_calendar"))}
    @Test fun appLabelsNeverAutomaticallyPublic(){assertFalse(NativeSpeechSummary.publicResult("open_app"))}
    @Test fun calendarContentNeverAutomaticallyPublic(){assertFalse(NativeSpeechSummary.publicResult("calendar"))}
    @Test fun fileContentNeverAutomaticallyPublic(){assertFalse(NativeSpeechSummary.publicResult("knowledge"))}
    @Test fun transcriptIsNotAcceptedAsKind(){assertNull(NativeSpeechSummary.confirmation("private name and number 5551234567",AppLanguage.EN))}
    @Test fun SpanishContactKeepsManualCall(){assertTrue(NativeSpeechSummary.confirmation("dial_contact",AppLanguage.ES)!!.contains("pulsar Llamar"))}
    @Test fun authoredResultStatusesCanSpeak(){listOf("dial_contact","set_timer","set_alarm","media_control","system_panel").forEach{assertTrue(NativeSpeechSummary.publicResult(it))}}
}
