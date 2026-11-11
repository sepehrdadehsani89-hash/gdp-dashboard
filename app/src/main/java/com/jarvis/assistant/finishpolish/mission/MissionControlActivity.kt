package com.jarvis.assistant.finishpolish.mission

import android.content.Intent
import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.companion.CompanionActivity
import com.jarvis.assistant.omnisystem.PrivateModuleActivity

/** Compact settings surface using the original theme; no new home layout or cosmetic telemetry. */
class MissionControlActivity:PrivateModuleActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
        screen("MISSION CONTROL · PAIRED COMPUTER"){
            val mission by runtime.mission.state.collectAsStateWithLifecycle()
            var name by remember{mutableStateOf("")}
            Text("${mission.label} · ${mission.phase}")
            Text(mission.detail)
            mission.deviceId?.let{Text("Selected trusted device: …${it.takeLast(8)}")}
            if(mission.sampledAt>0)Text("Last actual check: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(mission.sampledAt))}")
            if(mission.active){
                Text("An active task is tracked. Only a signed, verified browser postcondition can show VERIFIED.")
                Button(onClick={runtime.mission.cancel()}){Text("Stop mission · best-effort cancellation")}
            }
            Button(enabled=!working && !mission.active,onClick={task{runtime.mission.refresh()}}){Text("Check connection and actual capabilities")}
            OutlinedButton(enabled=!mission.active,onClick={startActivity(Intent(this@MissionControlActivity,CompanionActivity::class.java))}){Text("Pair / disconnect and revoke computer")}
            OutlinedTextField(name,{name=it.take(60)},label={Text("Friendly name for this paired computer")},enabled=!mission.active)
            OutlinedButton(enabled=!working && !mission.active && name.isNotBlank() && mission.deviceId!=null,onClick={authorize{task{runtime.mission.setName(name);status="Friendly name saved for this device only."}}}){Text("Save selected device name")}
            mission.readiness?.let{readiness->
                Text("Agent: ${readiness.agentVersion} · platform: ${readiness.platform}")
                Text(if(readiness.authenticated)"Nonce-bound authenticated capability manifest. Installed-app readiness is a check, not execution." else "Legacy agent: connectivity checked; capability readiness is unknown until an action is attempted. Update the companion for current readiness checks.")
                readiness.capabilities.forEach{capability->Text("${capability.actionId}: ${capability.status}${capability.reason?.let{" · $it"}.orEmpty()}")}
            }
            Button(enabled=!mission.active,onClick={runtime.submit("open Chrome on my computer and search for ChatGPT");finish()}){Text("Review approved Chrome search test")}
            Text("Android remains the microphone and speaker. Your computer needs no audio hardware. Chrome uses its separate Jarvis profile; private-LAN pairing and command confirmation are required. No desktop shell, arbitrary app/file access, hidden monitoring or unapproved screen capture is enabled.")
            Text("One paired target is supported in this release. A disconnected or unavailable action is Not sent/Failed, never Done. Cancellation cannot undo an already opened page. A lost response can leave completion unknown: check your desktop before approving a new request.")
            Text("Windows/macOS adapters are source-supported but native hardware acceptance and signed installers remain unverified. Linux was tested with controlled local Chromium pages, not a real Google/phone-audio round trip.")
        }
    }
}
