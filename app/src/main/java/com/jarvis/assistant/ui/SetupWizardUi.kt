package com.jarvis.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.setup.SetupPolicy
import com.jarvis.assistant.voice.VoiceOutputOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun SetupWizardUi(state:JarvisUiState,actions:JarvisUiActions){
    var step by rememberSaveable{mutableIntStateOf(0)}
    var name by rememberSaveable{mutableStateOf(state.settings.name)}
    var testing by remember{mutableStateOf(false)}
    var result by remember{mutableStateOf("No setup inference test has run.")}
    var inference by remember{mutableStateOf(false)}
    var testConfig by remember{mutableStateOf("")}
    var heard by rememberSaveable{mutableStateOf(false)}
    var voiceGeneration by remember{mutableLongStateOf(-1L)}
    val scope=rememberCoroutineScope()
    val config="${state.settings.provider}:${state.settings.model}"
    val tested=inference && testConfig==config && state.selectedBrainVerified
    val played=voiceGeneration>=0 && state.voiceDelivery.generation==voiceGeneration && state.voiceDelivery.output==VoiceOutputOutcome.COMPLETED
    LaunchedEffect(state.voiceDelivery.generation,state.voiceDelivery.output,voiceGeneration,step){if(step==7 && voiceGeneration==-2L && state.voiceDelivery.output in setOf(VoiceOutputOutcome.PREPARING,VoiceOutputOutcome.STARTED,VoiceOutputOutcome.COMPLETED,VoiceOutputOutcome.FAILED))voiceGeneration=state.voiceDelivery.generation}
    androidx.activity.compose.BackHandler{if(step>0)step=SetupPolicy.back(step)}
    fun finish(){actions.onSaveSettings(state.settings.copy(name=name.trim(),onboardingComplete=true,setupRevision=SetupPolicy.REVISION),"")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text("JARVIS · ${step+1} / ${SetupPolicy.steps.size}",style=MaterialTheme.typography.labelLarge)
        HudCore(state,Modifier.size(160.dp).align(Alignment.CenterHorizontally))
        Text(SetupPolicy.steps[step],style=MaterialTheme.typography.headlineMedium)
        when(step){
            0->{Text("Your existing holographic assistant, settings and data stay in place. Setup can be skipped and reopened in Settings → About Jarvis.");OutlinedTextField(name,{name=it.take(80)},label={Text("Your name · optional")},modifier=Modifier.fillMaxWidth())}
            1->{AppLanguage.supported.forEach{l->OutlinedButton(onClick={actions.onLanguage(l)}){Text(l.label+if(state.settings.language==l)" · selected" else "")}};Text("Recognition packs and output voices are configured for each supported language.")}
            2->{Text("Microphone permission: ${state.hasMicrophone}. Wake service running: ${state.wakeEnabled}. Android requires your approval. Listening pauses while Jarvis speaks, and can be stopped at any time.");Button(onClick=actions.onStartWake){Text("Enable microphone & wake listening")};OutlinedButton(onClick=actions.onStopWake){Text("Stop listening")};Text(state.wakeStatus);state.modelProgress?.let{LinearProgressIndicator(progress={it})}}
            3->{Text("Owner voice enrollment is optional and does not unlock Android. Open microphone settings, then Optional speaker model & consent.");Button(onClick=actions.onOpenVoiceIntelligence){Text("Voice identity & consent")}}
            4->{Text("Selected: ${state.settings.provider} · ${state.settings.model}. A brain is selected only after its authorized inference check.");Button(onClick=actions.onOpenBrainManager){Text("Select AI Brain")}}
            5->{Text("Use the provider's supported sign-in or your own API access. Consumer subscriptions and API billing are separate. Jarvis does not invent access or credits.");Button(onClick=actions.onOpenBrainManager){Text("Connect Provider")}}
            6->{Text("Run one real request against the selected model. Provider quota or charges may apply. No personal conversation is included.");Button(enabled=!testing,onClick={testing=true;inference=false;testConfig=config;scope.launch{try{val check=actions.onTestSelectedBrain();inference=true;result="Response received from ${check.model} in ${check.elapsedMs} ms: ${check.sample}"}catch(e:CancellationException){throw e}catch(_:Exception){result="Inference was not verified. Open AI Brain Manager for account, quota and model diagnostics."}finally{testing=false}}}){Text("Test AI Response")};if(testing)LinearProgressIndicator();Text(result)}
            7->{Text("Test your configured output voice, then confirm whether you actually heard it. A playback callback alone cannot prove audibility.");Button(onClick={heard=false;voiceGeneration=-2;actions.onVoiceCheck()}){Text("Test Voice Output")};Text("Engine prepared: ${state.voiceDelivery.engineReady}; playback: ${state.voiceDelivery.output}");Row(verticalAlignment=Alignment.CenterVertically){Checkbox(heard,{heard=it},enabled=played);Text("I heard the test voice")};if(state.canRetrySpeech)OutlinedButton(onClick={heard=false;voiceGeneration=-2;actions.onRetrySpeech()}){Text("Retry voice")}}
            8->{Text("Grant only permissions for the features you want. Android remains responsible for permission decisions.");Button(onClick={actions.onOpenPermission("app")}){Text("Review Android permissions")};OutlinedButton(onClick=actions.onOpenScreenSecurity){Text("Security settings")}}
            9->{Text("Test supported flashlight, volume or battery controls on your device. Opening this panel does not count as a successful hardware test.");Button(onClick=actions.onOpenDeviceControls){Text("Open Device Control Test")}}
            10->{Text("Your saved Reduced Motion preference is separate from system animation settings and Battery Saver. The live wallpaper needs Android's explicit selection.");Button(onClick=actions.onPreviewWallpaper){Text("Preview Wallpaper")};Row(verticalAlignment=Alignment.CenterVertically){Switch(state.hud.settings.reducedMotion,{actions.onSaveHud(state.hud.settings.copy(reducedMotion=it))});Text("Reduced Motion")}}
            11->{Text("Microphone permission: ${state.hasMicrophone}\nSelected AI test verified: $tested\nVoice API completion: $played\nVoice heard by you: $heard\nWake service running: ${state.wakeEnabled}");Button(onClick=actions.onOpenVoiceIntelligence){Text("Open Final Diagnostics")}}
            12->Text(SetupPolicy.status(state.hasMicrophone,tested,played,heard))
        }
        state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            OutlinedButton(enabled=step>0 && !testing,onClick={step=SetupPolicy.back(step)}){Text("Back")}
            Button(enabled=!testing,onClick={if(step==SetupPolicy.steps.lastIndex)finish()else step=SetupPolicy.next(step)}){Text(if(step==SetupPolicy.steps.lastIndex)"Save setup" else "Next")}
            TextButton(enabled=!testing,onClick={if(step==0 || step==SetupPolicy.steps.lastIndex)finish()else step=SetupPolicy.next(step)}){Text(if(step==0)"Skip setup" else "Skip")}
        }
    }
}
