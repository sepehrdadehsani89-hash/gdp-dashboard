package com.jarvis.assistant.voice

import android.content.Intent
import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.omnisystem.PrivateModuleActivity

class VoiceHealthActivity:PrivateModuleActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
        screen("VOICE HEALTH · LANGUAGE & PICKUP"){
            val shared by runtime.state.collectAsStateWithLifecycle()
            val diagnostic by RecognitionDiagnostics.state.collectAsStateWithLifecycle()
            val sensors by com.jarvis.assistant.pickup.PickupMonitor.state.collectAsStateWithLifecycle()
            var pickup by remember{mutableStateOf(runtime.pickupPreferences.enabled)}
            Text("Selected language: ${shared.settings.language.label}. Listening language: ${WakeListenerService.state.value.language.label}. ${shared.wakeStatus}")
            Text(diagnostic)
            Text("Metadata only; no audio, transcript or secret is stored in this diagnostic. Exact AI-call start and language-mode controls are handled locally. Phone/contact calls and protected actions keep their confidence/owner/confirmation checks.")
            val preferences=remember{com.jarvis.assistant.titan.VoicePreferences(this@VoiceHealthActivity)}
            Text("Recognition: ${preferences.sensitivity}; word threshold ${(preferences.confidence*100).toInt()}%. First-speaker focus: ${if(preferences.conversationFocus)"enabled (optional speaker model required)" else "off"}.")
            OutlinedButton(onClick={authorize{preferences.sensitivity="BALANCED";preferences.noiseReduction=true;preferences.echoCancellation=true;status="Balanced recognition restored. Stop and restart listening to apply microphone processing. Voices, private-speech consent and owner profiles were preserved."}}){Text("Restore balanced speech settings")}
            Button(onClick={runtime.requestAiCall(showUi=true)}){Text("Test local AI call · ring / Answer / End")}
            AppLanguage.supported.forEach{language->
                val modelMegabytes=runtime.wakeModelMegabytes(language)
                val installed=runtime.wakeModelInstalled(language)
                Text("${language.label}: speech pack ${if(installed)"installed" else "missing"}; saved Fish voice ${runtime.customVoice.config(language).let{if(it.enabled)"enabled · ${it.id}" else "disabled"}}")
                OutlinedButton(onClick={runtime.switchSpokenLanguage(language,false)}){Text("Select ${language.label} and its saved voice")}
                if(language==shared.settings.language && !installed)Button(enabled=shared.modelProgress==null,onClick={runtime.downloadWake()}){Text("Download selected speech pack · about ${modelMegabytes} MB")}
            }
            shared.modelProgress?.let{LinearProgressIndicator(progress={it});Text("Downloading selected speech model…")}
            shared.error?.let{Text(it)}
            TextButton(onClick={startActivity(Intent(this@VoiceHealthActivity,VoiceSettingsActivity::class.java))}){Text("Open Fish Audio output voice / saved Spanish voice")}
            HorizontalDivider()
            Switch(pickup,{enabled->authorize{runtime.setPickupEnabled(enabled);pickup=runtime.pickupPreferences.enabled}})
            Text("Pickup greetings · six English and six Spanish variations, with a five-minute cooldown. Uses accelerometer + uncovered proximity; screen must be on and unlocked. Requires existing wake listening. Silent/DND, quiet hours, calls, music and active requests suppress greetings. Missing proximity sensing disables the heuristic; it cannot establish identity or whether you are awake.")
            Text(sensors)
            OutlinedButton(onClick={status=if(runtime.greetPickup(test=true))"Greeting requested; watch actual output status." else "Greeting suppressed: enable wake listening, leave the phone unlocked and idle, and check Silent/DND, quiet hours and audio. No microphone was enabled by this button."}){Text("Test next pickup greeting · respects quiet/audio settings")}
            Text("Sensor test: set the unlocked phone down for two seconds with the top sensor uncovered, then lift and tilt it. Pickup greetings stop with the microphone service; they never start from boot or an alarm.")
        }
    }
}
