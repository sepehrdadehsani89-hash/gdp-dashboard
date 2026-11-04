package com.jarvis.assistant.rebirth

import android.content.Intent
import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import kotlinx.coroutines.*

class RebirthCenterActivity:PrivateModuleActivity(){
    private var diagnostics by mutableStateOf("")
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
        screen("REBIRTH 11.2 · CONTENT & LISTENING"){
            val shared by runtime.state.collectAsStateWithLifecycle()
            val phantom by runtime.phantom.state.collectAsStateWithLifecycle()
            Text("Ordinary conversations are live-only. Saved memories, shortcuts, locations, settings and prior archived chats remain separate. Stop, lock and idle expiry clear the temporary conversation.")
            Button(onClick={startActivity(Intent(this@RebirthCenterActivity,PersonalContentActivity::class.java))}){Text("Personal Content · files, photos & authorized folders")}
            OutlinedButton(onClick={startActivity(Intent(this@RebirthCenterActivity,com.jarvis.assistant.neural.NeuralSettingsActivity::class.java))}){Text("App aliases, approved memories & previous-history deletion")}
            OutlinedButton(onClick={startActivity(Intent(this@RebirthCenterActivity,com.jarvis.assistant.titan.TitanCenterActivity::class.java).putExtra("page","Voice Lab"))}){Text("Microphone calibration / Voice Lab")}
            OutlinedButton(onClick={startActivity(Intent(this@RebirthCenterActivity,com.jarvis.assistant.phantom.PhantomSettingsActivity::class.java))}){Text("Phantom HUD, wallpaper captions & timing traces")}
            if(shared.performanceRestricted)OutlinedButton(onClick={status=if(runtime.restoreVisualQuality())"Visual quality restored from saved preferences." else "Android still reports low memory; reduced effects remain."}){Text("Restore effects when memory is available")}
            Text("Actual session: ${shared.phase} · ${phantom.execution}\nMicrophone lease: ${MicrophoneSessions.manager.snapshot()?.let{"${it.owner} #${it.id}"} ?: "none"}\nRecognition status: ${shared.wakeStatus}\nAndroid STT locale: ${runtime.recognitionLocaleStatus()}")
            var showRecognition by remember{mutableStateOf(false)}
            Switch(showRecognition,{showRecognition=it});Text("Show temporary heard / understood comparison on this private screen")
            if(showRecognition)shared.recognition?.let{r->Text("HEARD: ${r.heard}\nUNDERSTOOD: ${r.understood}\n${r.kind}"+if(r.candidates.isNotEmpty())"\nCandidates: ${r.candidates.joinToString()}" else "")}
            OutlinedButton(enabled=!working,onClick={task{diagnostics=withContext(Dispatchers.IO){runtime.crashDiagnostics.snapshot()}}}){Text("Read actual memory / process-exit metadata")}
            if(diagnostics.isNotBlank())Text(diagnostics)
            TextButton(onClick={runtime.crashDiagnostics.clear();diagnostics=""}){Text("Clear non-content failure metadata")}
            Text("Microphone, background noise, app launches, Fish playback and stability still need testing on your phone. Gmail account integration is not included in this build.")
        }
    }
}
