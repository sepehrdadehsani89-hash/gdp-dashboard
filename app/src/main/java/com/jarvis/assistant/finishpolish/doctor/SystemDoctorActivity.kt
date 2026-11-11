package com.jarvis.assistant.finishpolish.doctor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.omnisystem.PrivateModuleActivity

/** Existing cyan PrivateModuleActivity surface; no dashboard replacement or decorative scan. */
class SystemDoctorActivity:PrivateModuleActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);if(isFinishing)return
        screen("SYSTEM DOCTOR"){
            val shared by runtime.state.collectAsStateWithLifecycle()
            val report by runtime.doctor.state.collectAsStateWithLifecycle()
            val repair by runtime.doctor.repairState.collectAsStateWithLifecycle()
            val sharedAction by runtime.polishActions.state.collectAsStateWithLifecycle()
            var expanded by remember{mutableStateOf(false)}
            var proposal by remember{mutableStateOf<RepairProposal?>(null)}
            Text("Read-only evidence first. A settings readback does not prove the microphone, network, voice service or hardware was repaired.")
            Button(enabled=!working,onClick={task{runtime.doctor.inspect();status="Systems checked using actual local metadata. No recorder, model inference or server request was started."}}){Text("Check current systems")}
            OutlinedButton(enabled=!working,onClick={task{runtime.doctor.checkCompanion();status="Explicit paired connection check finished. Review identity/connectivity evidence; this is not command-execution verification."}}){Text("Check paired connection · network request")}
            report?.let{r->
                Text("${r.probes.size} signals checked · ${r.issues.size} need review · sampled ${java.time.Instant.ofEpochMilli(r.sampledAt)}")
                Text(r.explanation(shared.settings.language==com.jarvis.assistant.core.AppLanguage.ES))
                TextButton(onClick={expanded=!expanded}){Text(if(expanded)"Hide evidence" else "Show evidence and unmeasured checks")}
                if(expanded)r.probes.forEach{p->
                    HorizontalDivider();Text("${p.id} · ${p.status} · ${p.certainty}",style=MaterialTheme.typography.titleSmall)
                    Text(p.evidence);Text(p.nextStep,style=MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick={
                    val text=runtime.doctor.exportReport(r)
                    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_TEXT,text),"Share reviewed Doctor metadata"))
                }){Text("Share this reviewed metadata report…")}
            }
            HorizontalDivider()
            OutlinedButton(enabled=!working,onClick={proposal=runtime.doctor.proposal()}){Text("Review reversible voice-settings repair")}
            repair?.let{Text("${it.state}: ${it.message}")}
            OutlinedButton(enabled=!working,onClick={authorize{task{val result=runtime.doctor.rollbackBalanced(true);status=result.message}}}){Text("Approve restoring previous Doctor settings")}
            Text("Repair attempts: one at a time, one-minute cooldown, at most three in ten minutes. Private voices, keys, owner templates, pairings and models are outside this repair. No permission is granted automatically.")
            Text("Shared action evidence: ${sharedAction.action} · ${sharedAction.stage} · ${sharedAction.reason}",style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")))}){Text("Open Android app permissions")}
            TextButton(onClick={startActivity(Intent(this@SystemDoctorActivity,com.jarvis.assistant.voice.VoiceHealthActivity::class.java))}){Text("Voice health / speech-pack / output checks")}
            TextButton(onClick={startActivity(Intent(this@SystemDoctorActivity,com.jarvis.assistant.sentinel.WallpaperRepairActivity::class.java))}){Text("Existing wallpaper and rendering checks")}
            TextButton(onClick={startActivity(Intent(this@SystemDoctorActivity,com.jarvis.assistant.neural.NeuralSettingsActivity::class.java))}){Text("Existing local brain settings")}
            proposal?.let{p->AlertDialog(
                onDismissRequest={proposal=null},title={Text(p.title)},
                text={Text("${p.tier} · ${p.preconditions}\n\n${p.risk}\n\nExpected: ${p.expectedOutcome}\n\nVerification: ${p.verification}\n\nRollback: ${p.rollback}\n\nOne attempt; no automatic retry. Cancel leaves settings unchanged.")},
                confirmButton={TextButton(onClick={proposal=null;authorize{task{status=runtime.doctor.applyBalanced(true).message}}}){Text("Approve these settings only")}},
                dismissButton={TextButton(onClick={proposal=null}){Text("Cancel")}}
            )}
        }
    }
}
