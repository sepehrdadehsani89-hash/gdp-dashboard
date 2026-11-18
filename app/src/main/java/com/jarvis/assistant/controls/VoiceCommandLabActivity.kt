package com.jarvis.assistant.controls

import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.omnisystem.PrivateModuleActivity

class VoiceCommandLabActivity:PrivateModuleActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
        screen("JARVIS · COMMAND TEST LAB"){
            var text by remember{mutableStateOf("")}
            var baselineLaunch by remember{mutableStateOf<com.jarvis.assistant.rebirth.LaunchResult?>(null)}
            var submitted by remember{mutableStateOf(false)}
            val traces by runtime.androidControls.traces.collectAsStateWithLifecycle()
            val shared by runtime.state.collectAsStateWithLifecycle()
            Text("Typed tests do not test the microphone. Speech-origin entries below come from actual commands delivered by the existing recognition pipeline. Transcript traces stay in this app session and can be cleared.")
            OutlinedTextField(text,{text=it.take(500)},label={Text("Command text · typed input")})
            val parsed=AndroidControlParser.parse(text)
            val diagnosis=CommandDiagnosticPolicy.diagnose(text)
            Text("Detected typed intent: ${diagnosis.intent}\nSelected route: ${diagnosis.module}\nRequired access: ${diagnosis.access}")
            parsed?.let{Text("Required access: ${AndroidControlCatalog.capability(it.id).permission}")}
            Button(enabled=parsed!=null && !working,onClick={task{val result=runtime.androidControls.execute(parsed!!,text,"TYPED_TEST_LAB");status="${result.status} · ${result.detail}"}}){Text("Run this native command")}
            Button(enabled=text.isNotBlank(),onClick={baselineLaunch=runtime.lastApplicationLaunch;submitted=true;runtime.submit(text,voiced=false);status="Submitted as typed input through the real assistant. Any required confirmation remains in Jarvis."}){Text("Run through the normal assistant pipeline")}
            Text("Normal pipeline state: ${shared.phase} · ${shared.neuralStage}")
            if(submitted){val launch=runtime.lastApplicationLaunch;if(launch!=null && launch!==baselineLaunch)Text("Actual Android launch result: ${launch.status}\n${launch.detail}")}
            shared.error?.let{Text("Actual error details: $it")}
            Text("Actual selected module / task: ${shared.activeTask}")
            shared.messages.lastOrNull{it.role==com.jarvis.assistant.core.MessageRole.ASSISTANT}?.let{Text("Last actual assistant result: ${it.text}")}
            shared.pendingAction?.let{Text("Pending action: ${it.kind}. Review and confirm in the main assistant; this lab does not auto-approve it.")}
            TextButton(onClick={runtime.stopEverything();status="Stop All Listening requested. Inspect the live microphone indicator to verify release."}){Text("Stop All Listening")}
            Text("Application launches, Google data, reminders, companion actions and farewell commands use the normal assistant pipeline and its own confirmations; typed input does not certify speech recognition or physical-device completion.")
            val app=shared.recognition
            app?.let{Text("Last actual recognition: ${it.heard}\nUnderstood: ${it.understood}")}
            traces.lastOrNull()?.let{Text("Most recent operation: ${it.actualOperation}")}
            traces.asReversed().forEach{trace->HorizontalDivider();Text("Origin: ${trace.origin}\n${if(trace.origin=="SPEECH_RECOGNITION")"Recognized command" else "Typed/button command"}: ${trace.input}\nIntent: ${trace.command.id}\nModule: AndroidControlManager\nPermission: ${trace.permission}\nActual operation: ${trace.actualOperation}\nExecution result: ${trace.outcome.status}\n${trace.outcome.detail}")}
            TextButton(onClick={runtime.androidControls.clearDiagnostics()}){Text("Clear temporary diagnostics")}
        }
    }
}
