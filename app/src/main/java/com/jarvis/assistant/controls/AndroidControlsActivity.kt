package com.jarvis.assistant.controls

import android.content.Intent
import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.omnisystem.PrivateModuleActivity

/** Existing Jarvis styling, real handlers, no decorative global toggles. */
class AndroidControlsActivity:PrivateModuleActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
        screen("JARVIS · ANDROID CONTROLS"){
            val results by runtime.androidControls.results.collectAsStateWithLifecycle()
            val recording by com.jarvis.assistant.screen.VideoRecordingService.state.collectAsStateWithLifecycle()
            var search by remember{mutableStateOf("")}
            Text("A settings shortcut is a request for your action. SUCCESS requires an actual Android result/readback; this screen does not certify your Redmi 9 without running an action.")
            Button(onClick={startActivity(Intent(this@AndroidControlsActivity,VoiceCommandLabActivity::class.java))}){Text("Voice command test lab")}
            OutlinedButton(onClick={startActivity(Intent(this@AndroidControlsActivity,com.jarvis.assistant.quantum.ControlCenterActivity::class.java))}){Text("Existing accessory / Bluetooth control center")}
            Text("Screen recording: ${recording.stage}\n${recording.message}")
            OutlinedTextField(search,{search=it.take(100)},label={Text("Find a control")})
            AndroidControlCatalog.entries.filter{search.isBlank() || (it.name+it.setup).contains(search,true)}.forEach{capability->
                HorizontalDivider();Text(capability.name,style=MaterialTheme.typography.titleMedium)
                Text("${capability.support}\n${capability.permission}")
                val result=results[capability.id]
                Text(result?.let{"Last result: ${it.status}\n${it.detail}\n${java.util.Date(it.at)}"} ?: "No action has been tested in this app session.")
                val command=AndroidControlCommand(capability.id,if(capability.id==AndroidControlId.VOLUME_SET)40 else null)
                val permission=runtime.androidControls.runtimePermission(command)
                Text(if(permission==null)"Permission: ${capability.permission}" else "Permission currently granted: ${runtime.androidControls.hasPermission(permission)}")
                Text(capability.setup)
                OutlinedButton(enabled=!working,onClick={task{val response=runtime.androidControls.execute(command);status="${response.status} · ${response.detail}"}}){Text(if(capability.id==AndroidControlId.VOLUME_SET)"Test action · media volume 40%" else "Test action / Android setup")}
            }
        }
    }
}
