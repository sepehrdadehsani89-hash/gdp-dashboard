package com.jarvis.assistant.setup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.google.GoogleProduct
import com.jarvis.assistant.omnisystem.PrivateModuleActivity

object NexusSetupPolicy {
    val steps=listOf("Welcome","Device permissions","AI provider setup","Google Cloud configuration","Google account selection","OAuth permissions","Test Gmail","Test Calendar and Drive","Notification listener setup","Android system controls","Voice setup","Wallpaper customization","Computer pairing","Live verification","Completion")
    fun next(index:Int)=(index+1).coerceIn(0,steps.lastIndex)
    fun back(index:Int)=(index-1).coerceIn(0,steps.lastIndex)
    fun integration(account:Boolean,authorized:Boolean,currentRead:Boolean,skipped:Boolean=false):String=when{skipped->"SKIPPED · not connected by this wizard";account&&authorized&&currentRead->"LIVE API VERIFIED · current process proof";account&&authorized->"USER ACTION REQUIRED · real read not verified";account->"USER ACTION REQUIRED · authorize this service";else->"USER ACTION REQUIRED · select an account"}
    fun progress(value:String)=value.takeIf{it in setOf("PENDING","REVIEWED_BY_USER","SKIPPED")} ?: "PENDING"
    fun completion()="Setup choices saved. Skipped, configured-only and unverified integrations remain unconnected. No physical device test is inferred."
}
class NexusSetupActivity:PrivateModuleActivity(){
    private val preferences by lazy{getSharedPreferences("jarvis_nexus_setup_v1",MODE_PRIVATE)}
    private var step by mutableIntStateOf(0)
    private var revision by mutableIntStateOf(0)
    private fun open(cls:Class<*>,product:GoogleProduct?=null){startActivity(Intent(this,cls).apply{product?.let{putExtra("google_product",it.name)}})}
    private fun record(value:String){if(value !in setOf("REVIEWED_BY_USER","SKIPPED"))return;runCatching{check(preferences.edit().putString("step_$step",value).putInt("cursor",NexusSetupPolicy.next(step)).commit())}.onSuccess{revision++;step=NexusSetupPolicy.next(step)}.onFailure{status="Setup progress could not be saved. Existing settings and connections were preserved."}}
    override fun onCreate(state:Bundle?){super.onCreate(state);if(isFinishing)return;step=(state?.getInt("nexus_step") ?: preferences.getInt("cursor",0)).coerceIn(0,NexusSetupPolicy.steps.lastIndex)
        screen("JARVIS · SETUP / RECONFIGURATION"){
            val actual by runtime.state.collectAsStateWithLifecycle()
            val connections by runtime.googleConnections.state.collectAsStateWithLifecycle()
            val selected=connections.accounts.firstOrNull{it.id==connections.selected}
            Text("${step+1}/${NexusSetupPolicy.steps.size} · ${NexusSetupPolicy.steps[step]}")
            Text("Optional services may be skipped. Reviewing a step does not verify an integration; only actual provider/API results can do that. Existing accounts, settings, wallpaper and pairings are preserved.")
            Text("Progress: ${remember(revision,step){NexusSetupPolicy.progress(preferences.getString("step_$step","PENDING").orEmpty())}}")
            when(step){
                0->Text("Continue your existing Jarvis application. No replacement identity or signing key is created. Use Back / Skip to control optional setup.")
                1->{Text("Microphone permission: ${actual.hasMicrophone}. This is permission status, not proof of audible speech or recognition.");Button(onClick={startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")))}){Text("Review Android app permissions")}}
                2->{Text("Selected: ${actual.settings.provider} / ${actual.settings.model}. Current inference proof: ${actual.selectedBrainVerified}.");Button(onClick={open(com.jarvis.assistant.brain.BrainManagerActivity::class.java)}){Text("Configure / test the actual selected AI")}}
                3->Button(onClick={open(com.jarvis.assistant.google.GoogleCloudConfigurationActivity::class.java)}){Text("Google Cloud Configuration · setup instructions")}
                4->Button(onClick={open(com.jarvis.assistant.google.GoogleAccountsActivity::class.java)}){Text("Use the real Android Google account selector")}
                5->{Text("Select and authorize each service independently. Account selection alone never grants Gmail/Drive access.");Button(onClick={open(com.jarvis.assistant.google.GoogleAccountsActivity::class.java)}){Text("Review per-service OAuth permissions")}}
                6->{Text(NexusSetupPolicy.integration(selected!=null,selected?.services?.contains(GoogleProduct.GMAIL)==true,selected?.let{runtime.googleConnections.currentlyVerified(it.id,GoogleProduct.GMAIL)}==true));Button(onClick={open(com.jarvis.assistant.google.GoogleAccountsActivity::class.java,GoogleProduct.GMAIL)}){Text("Authorize and perform a real Gmail read")}}
                7->{for(p in listOf(GoogleProduct.CALENDAR,GoogleProduct.DRIVE)){Text(p.name+" · "+NexusSetupPolicy.integration(selected!=null,selected?.services?.contains(p)==true,selected?.let{runtime.googleConnections.currentlyVerified(it.id,p)}==true));Button(onClick={open(com.jarvis.assistant.google.GoogleAccountsActivity::class.java,p)}){Text("Open ${p.name} real read test")}}}
                8->Button(onClick={open(com.jarvis.assistant.notifications.NotificationAwarenessActivity::class.java)}){Text("Notification listener · explicit source consent")}
                9->Button(onClick={open(com.jarvis.assistant.controls.AndroidControlsActivity::class.java)}){Text("Android capability dashboard / actual tests")}
                10->{Button(onClick={open(com.jarvis.assistant.voice.VoiceSettingsActivity::class.java)}){Text("Voice output / privacy consent")};Button(onClick={open(com.jarvis.assistant.omnisystem.SpeakerProfilesActivity::class.java)}){Text("Recognition and owner voice setup")};Text("Playback state: ${actual.voiceDelivery.output}. A completion callback does not establish that you heard speech; perform the physical voice check.")}
                11->Button(onClick={open(com.jarvis.assistant.studio.WallpaperStudioActivity::class.java)}){Text("Wallpaper Studio · preserve or customize")}
                12->{Text("Saved pairings are distinct from online authenticated sessions. Complete the real QR / request / desktop approval / finish / signed connection check.");Button(onClick={open(com.jarvis.assistant.companion.CompanionActivity::class.java)}){Text("Pair computers / scan QR / diagnostics")}}
                13->{Button(onClick={open(com.jarvis.assistant.controls.VoiceCommandLabActivity::class.java)}){Text("Actual typed / voice command diagnostics")};Button(onClick={open(com.jarvis.assistant.modes.SystemModesActivity::class.java)}){Text("Silent Mode / Deep Shutdown / Stop All")};Text("Physical Redmi-to-Windows pairing and actual Google account reads require those authorized devices/accounts. Do not infer them from this wizard's progress.")}
                14->{Text("Selected inference proof: ${actual.selectedBrainVerified}. Microphone permission: ${actual.hasMicrophone}. Wake service running: ${actual.wakeEnabled}. Each Google service's current result is shown in Connected Services.");Text(NexusSetupPolicy.completion());Button(onClick={check(preferences.edit().putBoolean("completed",true).commit());runtime.saveSettings(runtime.store.settings.value.copy(onboardingComplete=true,setupRevision=SetupPolicy.REVISION),"");status=NexusSetupPolicy.completion()}){Text("Save setup choices · leave unverified services pending")}}
            }
            OutlinedButton(enabled=step>0,onClick={step=NexusSetupPolicy.back(step)}){Text("Back")}
            if(step<NexusSetupPolicy.steps.lastIndex){Button(onClick={record("REVIEWED_BY_USER")}){Text("I reviewed this step · Next")};TextButton(onClick={record("SKIPPED")}){Text("Skip this optional step · not connected")}}
            TextButton(onClick={step=0}){Text("Start reconfiguration from Welcome")}
        }
    }
    override fun onSaveInstanceState(outState:Bundle){outState.putInt("nexus_step",step);super.onSaveInstanceState(outState)}
}
