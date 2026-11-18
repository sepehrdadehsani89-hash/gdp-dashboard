package com.jarvis.assistant.modes

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.omnisystem.PrivateModuleActivity

class SystemModesActivity:PrivateModuleActivity(){
    override fun onCreate(state:Bundle?){super.onCreate(state);if(isFinishing)return
        screen("JARVIS · SILENT MODE & DEEP SHUTDOWN"){
            val mode by runtime.systemModes.state.collectAsStateWithLifecycle()
            val actual by runtime.state.collectAsStateWithLifecycle()
            var wake by remember(mode){mutableStateOf(mode.wakeInSilent)}
            var reminders by remember(mode){mutableStateOf(mode.keepReminders)}
            var deepReview by remember{mutableStateOf(false)}
            Text("Actual saved mode: ${if(mode.deep)"DEEP SHUTDOWN"else if(mode.silent)"SILENT"else"ACTIVE"}. Microphone capture: ${actual.audio.recording}. Wake service: ${actual.wakeEnabled}.")
            runtime.systemModes.storageError?.let{Text(it);TextButton(onClick={authorize{runtime.systemModes.recoverExplicitly();status="Mode preferences reset explicitly; the previous raw value was retained privately. No app data or account was removed."}}){Text("Recover only mode preferences · preserve previous value")}}
            Text("Silent Mode dims the Jarvis wallpaper, reduces motion, hides private wallpaper previews and suppresses optional Jarvis briefings/alerts. Outside messages still arrive. Android's required microphone/foreground-service indicator remains visible when a service is running.")
            Text("Optional wake listening in Silent Mode");Switch(wake,{wake=it})
            Text("Preserve Jarvis reminder / timer / check-in notifications as exceptions");Switch(reminders,{reminders=it})
            Text("Allowing wake listening does not grant microphone permission or start capture automatically. Use Start listening deliberately after granting Android permission. Conversation farewell stops active conversation capture; Stop All Listening also stops wake monitoring.")
            Button(enabled=!working,onClick={authorize{runtime.setSilentMode(true,wake,reminders);status="Silent Mode saved. Private previews hidden; optional activity paused. Android DND is separate; inspect its actual status below."}}){Text("Activate Silent Mode / save exceptions")}
            Button(enabled=!working,onClick={authorize{runtime.setSilentMode(false,wake,reminders);status="Silent Mode disabled. Saved wallpaper layout remains intact. Listening has not been started automatically."}}){Text("Exit Silent Mode")}
            val nm=getSystemService(NotificationManager::class.java)
            Text("Android DND access: ${nm.isNotificationPolicyAccessGranted}; actual interruption filter: ${nm.currentInterruptionFilter}. Filter 1=all, 2=priority, 3=none, 4=alarms. The effective filter can also reflect other Android rules.")
            OutlinedButton(onClick={startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))}){Text("Review Android DND permission / exceptions")}
            OutlinedButton(enabled=!working,onClick={authorize{if(!nm.isNotificationPolicyAccessGranted)status="PERMISSION_REQUIRED · Allow Notification Policy access through Android first."else{try{nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);status=if(nm.currentInterruptionFilter==NotificationManager.INTERRUPTION_FILTER_PRIORITY)"Android reports priority interruption filtering. Your Android exceptions remain controlled by Android."else"USER ACTION REQUIRED · Android did not report the requested effective filter. Review DND settings; no global toggle success is claimed."}catch(_:Exception){status="Android rejected the DND request. Review system policy settings."}}}}){Text("Request Android priority DND · verify actual filter")}
            OutlinedButton(onClick={startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))}){Text("Review / restore Android DND settings")}
            Button(onClick={runtime.stopEverything();status="Stop All Listening requested: conversation and wake recovery stopped. Check the real microphone/service indicators; no physical test is claimed here."}){Text("Stop All Listening")}
            Text("Deep Shutdown stops Jarvis capture/wake recovery, current AI and companion requests, optional downloads and optional background activity. Saved accounts, pairing identities, files and essential reminder data are preserved. It does not turn off the phone or stop unrelated applications; a remote action already completed cannot be undone.")
            Button(enabled=!working,onClick={deepReview=true}){Text("Review Deep Shutdown")}
            if(deepReview)AlertDialog(onDismissRequest={deepReview=false},title={Text("Deactivate Jarvis completely?")},text={Text("Stop active Jarvis capture and optional activity now? Your saved data and pairings remain. Reminder exceptions follow the setting above. Manually opening the main Jarvis application reactivates it without silently starting its microphone.")},confirmButton={TextButton(onClick={deepReview=false;authorize{runtime.enterDeepShutdown(reminders);status="Deep Shutdown recorded; active Jarvis stop/cancellation requested. Open the main app manually to reactivate. Actual physical microphone release still requires device verification."}}){Text("Confirm Deep Shutdown")}},dismissButton={TextButton(onClick={deepReview=false}){Text("Cancel")}})
            if(mode.deep)OutlinedButton(onClick={runtime.reactivateJarvisManually();status="Jarvis reactivated. Listening remains an explicit user action."}){Text("Reactivate Jarvis manually")}
            intent.getStringExtra("requested_mode")?.let{Text("Voice requested: $it. No change was executed from that sentence; review the appropriate controls above.")}
        }
    }
}
