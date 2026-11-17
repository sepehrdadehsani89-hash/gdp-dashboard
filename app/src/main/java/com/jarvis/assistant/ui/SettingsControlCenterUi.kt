package com.jarvis.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.BuildConfig
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.settings.SettingsDirectory

@Composable
fun SettingsControlCenterUi(state:JarvisUiState,actions:JarvisUiActions,onDetailed:()->Unit,onAppearance:()->Unit){
    var search by rememberSaveable{mutableStateOf("")}
    var selected by rememberSaveable{mutableStateOf<String?>(null)}
    androidx.activity.compose.BackHandler(enabled=selected!=null){selected=null}
    LazyColumn(contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        item{Text("Jarvis · Settings",style=MaterialTheme.typography.headlineMedium)}
        item{OutlinedTextField(search,{search=it.take(160);selected=null},label={Text("Search settings")},modifier=Modifier.fillMaxWidth(),singleLine=true)}
        if(selected==null){
            val sections=SettingsDirectory.search(search)
            if(sections.isEmpty())item{Text("No matching section. Try voice, motion, contacts or privacy.")}
            items(sections,key={it.id}){section->OutlinedButton(onClick={selected=section.id},modifier=Modifier.fillMaxWidth()){
                Column(Modifier.fillMaxWidth()){Text(section.title,style=MaterialTheme.typography.titleMedium);Text(section.detail,style=MaterialTheme.typography.bodySmall)}
            }}
        }else{
            item{TextButton(onClick={selected=null}){Text("Back to sections")};Text(SettingsDirectory.sections.single{it.id==selected}.title,style=MaterialTheme.typography.titleLarge)}
            item{Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                when(selected){
                    "account"->{Button(onClick=onDetailed){Text("Identity & saved preferences")};TextButton(onClick={actions.onOpenVoiceIntelligence()}){Text("Owner voice identity")}}
                    "ai"->{Text("Selected: ${state.settings.provider} · ${state.settings.model}. Authorization alone does not prove inference.");Button(onClick=actions.onOpenBrainManager){Text("Open AI Brain Manager")}}
                    "voice"->{Text("Microphone permission: ${state.hasMicrophone}. Wake service: ${state.wakeEnabled}.");Button(onClick=actions.onStartWake){Text("Enable wake listening")};OutlinedButton(onClick=actions.onStopWake){Text("Stop wake listening")};Button(onClick=actions.onOpenVoiceIntelligence){Text("Microphone, cooldown & diagnostics")};Button(onClick=actions.onOpenOutputVoice){Text("Output voice & private speech consent")};Text("Voice edits: set voice volume to fifty percent; turn captions on. Say voice settings help for supported commands.")}
                    "device"->{Button(onClick=actions.onOpenDeviceControls){Text("Android device controls")};Button(onClick=actions.onOpenContacts){Text("Contact shortcuts")};Text("Say: remember that Ma means Mom. Exact contact matching and confirmation are required. Android owns the final Call action.")}
                    "security"->{Button(onClick=actions.onOpenScreenSecurity){Text("Screen security & authentication")};OutlinedButton(onClick={actions.onOpenPermission("app")}){Text("Android app permissions")};Text("Jarvis cannot bypass Android's screen lock or protected settings.")}
                    "appearance"->{Button(onClick=onAppearance){Text("Appearance, Reduced Motion & wallpaper")};OutlinedButton(onClick=actions.onPreviewWallpaper){Text("Preview live wallpaper")}}
                    "automation"->{Button(onClick=actions.onOpenNeural){Text("Briefings & automation")};OutlinedButton(onClick={actions.onOpenPermission("notifications")}){Text("Notification permission")}}
                    "language"->{AppLanguage.supported.forEach{l->OutlinedButton(onClick={actions.onLanguage(l)}){Text(l.label+(if(state.settings.language==l)" · selected" else ""))}};Button(onClick=onAppearance){Text("Motion accessibility")};Text("System animation settings and Battery Saver remain separate from your saved motion preference.")}
                    "diagnostics"->{Button(onClick=actions.onOpenVoiceIntelligence){Text("Voice pipeline diagnostics")};Button(onClick=actions.onOpenRebirth){Text("System diagnostics")};Button(onClick=actions.onOpenBrainManager){Text("Test provider inference")}}
                    "about"->{Text("Jarvis ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})");Text("Provider requests and Fish Audio synthesis leave this device only through their configured connections. Private speech requires separate consent. Provider credentials stay in encrypted local storage. This preview is not a Google Play approval.");Button(onClick={actions.onSaveSettings(state.settings.copy(setupRevision=0),"")}){Text("Reopen guided setup")};Button(onClick=onDetailed){Text("Data controls & detailed preferences")}}
                }
            }}
        }
    }
}
