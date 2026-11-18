package com.jarvis.assistant.controls

import com.jarvis.assistant.farewell.FarewellIntent
import com.jarvis.assistant.google.GoogleCommandParser
import com.jarvis.assistant.google.GoogleServicesPolicy
import com.jarvis.assistant.neural.AppMatching
import com.jarvis.assistant.notifications.NotificationAwarenessPolicy
import com.jarvis.assistant.reminders.ReminderPolicy
import com.jarvis.assistant.companion.CompanionPolicy
import com.jarvis.assistant.companion.DesktopRegistry
import com.jarvis.assistant.settings.SpokenSettings
import com.jarvis.assistant.core.PhoneRequestParser
import com.jarvis.assistant.core.PhoneRequest

data class CommandDiagnosis(val intent:String,val module:String,val access:String)
/** Classification for diagnostics only. It cannot dispatch, approve, or certify an operation. */
object CommandDiagnosticPolicy {
    fun diagnose(text:String):CommandDiagnosis {
        if(SpokenSettings.credentialRequest(text))return CommandDiagnosis("PRIVATE_CREDENTIAL_WITHHELD","AI Brain Manager","Private credential entry only; never send credentials to general AI")
        com.jarvis.assistant.security.ScreenCommandPolicy.parse(text)?.let{return CommandDiagnosis("SCREEN_$it","ScreenSecurityController",if(it==com.jarvis.assistant.security.ScreenCommand.LOCK)"Enabled lock-only Android device administrator" else "Normal Android keyguard authentication; no voice bypass")}
        if(FarewellIntent.matches(text))return CommandDiagnosis("CONVERSATION_COMPLETE","AssistantRuntime.finishConversation","Optional context requires explicit consent and source access; microphone closes after speech or failure")
        runCatching{SpokenSettings.parse(text)}.getOrNull()?.let{return CommandDiagnosis("SPOKEN_SETTING","SpokenSettingsController","Unlocked phone; privacy changes require confirmation and normal authentication")}
        CompanionPolicy.parse(text)?.let{return CommandDiagnosis(it.action,"MissionController","Explicit target/session-bound approval and a pinned authenticated companion")}
        DesktopRegistry.selectionCommand(text)?.let{return CommandDiagnosis("SELECT_COMPUTER","DesktopRegistry","Unlocked phone and an existing approved target")}
        GoogleCommandParser.parse(text)?.let{return CommandDiagnosis("GOOGLE_${it.product}_${it.kind}","GoogleAssistant",GoogleServicesPolicy.definition(it.product).scope ?: "Official app / picker only")}
        if(GoogleCommandParser.navigation(text))return CommandDiagnosis("CONNECTED_ACCOUNTS","GoogleAccountsActivity","Unlocked phone; Google owns consent")
        if(GoogleCommandParser.requiresNativeRead(text))return CommandDiagnosis("GOOGLE_QUERY_UNSUPPORTED","GoogleAssistant","Supported authorized query required; no inbox inference from general AI")
        if(NotificationAwarenessPolicy.request(text))return CommandDiagnosis("READ_ACTIVE_NOTIFICATION_PREVIEWS","JarvisNotificationService","Unlocked phone, Android Notification access, per-source consent and a connected listener")
        ReminderPolicy.parse(text)?.let{return CommandDiagnosis("OWNED_REMINDER","ReminderManager","Unlocked phone, notification access and an unambiguous future time")}
        AndroidControlParser.parse(text)?.let{return CommandDiagnosis(it.id.name,"AndroidControlManager",AndroidControlCatalog.capability(it.id).permission)}
        AppMatching.commandTarget(text)?.let{return CommandDiagnosis("OPEN_VISIBLE_APP","AppLaunchCoordinator","Android-visible enabled launcher app; foreground dispatch or user notification handoff")}
        PhoneRequestParser.parse(text)?.let{return CommandDiagnosis("LOCAL_PHONE_READ","PhoneContext",when(it){is PhoneRequest.Calendar->"Optional READ_CALENDAR and unlocked Android";is PhoneRequest.Contacts->"Optional READ_CONTACTS and unlocked Android";else->"Location access and unlocked Android"})}
        return CommandDiagnosis("NORMAL_ASSISTANT_PIPELINE","AssistantRuntime","Native routing and the selected brain's actual configuration; no action completion is predicted")
    }
}
