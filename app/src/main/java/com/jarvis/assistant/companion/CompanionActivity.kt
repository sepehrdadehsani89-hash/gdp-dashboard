package com.jarvis.assistant.companion

import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.util.UUID

/** Pairing is explicit on phone and desktop. Credentials never enter chat or diagnostics. */
class CompanionActivity:PrivateModuleActivity(){
    private var invitation by mutableStateOf("")
    private var paired by mutableStateOf(false)
    private var pending:DesktopInvitation?=null
    private var ticket by mutableStateOf("")
    private var compare by mutableStateOf("")
    private var clientId="";private var nonce=""
    private val scannerOwner=Any()
    private val scanner=registerForActivityResult(ScanContract()){result->
        runtime.endExclusiveCapture(scannerOwner)
        result.contents?.let{contents->runCatching{CompanionPolicy.invitation(contents,System.currentTimeMillis())}.onSuccess{invitation=contents;status="Invitation scanned. Review the computer address and certificate, then request pairing."}.onFailure{status="Invalid or expired invitation. Generate a new one on your computer."}}
    }
    override fun onCreate(state:Bundle?){super.onCreate(state);if(isFinishing)return
        paired=runCatching{runtime.companion.paired()!=null}.getOrDefault(false)
        screen("JARVIS · PAIRED COMPUTER"){
            Text("Your phone handles voice and speaks the result. The visible companion on your own computer can open Chrome and search after you approve. No computer microphone or speakers are needed. Other desktop actions and internet relay are not included yet.")
            Text(if(paired)"Paired locally · connection has not been checked in this screen" else "No paired computer")
            OutlinedButton(enabled=!working,onClick={if(runtime.beginExclusiveCapture(scannerOwner))scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Scan the private Jarvis invitation on your computer").setBeepEnabled(false).setOrientationLocked(false))}){Text("Scan computer's one-time QR")}
            OutlinedTextField(invitation,{invitation=it.take(8192)},label={Text("Or paste the private JARVIS-PAIR-1 invitation")},enabled=!working,maxLines=3)
            val preview=remember(invitation){runCatching{CompanionPolicy.invitation(invitation,System.currentTimeMillis())}.getOrNull()}
            preview?.let{Text("Computer: ${it.url}\nCertificate SHA-256: ${it.pin}\nPair only with your own trusted computer. Never paste this invitation into chat or publish it.")}
            Button(enabled=!working && preview!=null,onClick={authorize{task{
                val i=CompanionPolicy.invitation(invitation,System.currentTimeMillis());clientId=UUID.randomUUID().toString();nonce=UUID.randomUUID().toString()
                val r=runtime.companion.requestPair(i,clientId,nonce);pending=i;ticket=r.getString("ticket");compare=r.getString("compare_code")
                status="Compare this code with the computer: $compare. On the computer enter pending, then approve the matching ticket."
            }}}){Text("Request pairing · approve on both devices")}
            if(ticket.isNotBlank()){
                Text("Compare code: $compare\nTicket: $ticket")
                Button(enabled=!working,onClick={authorize{task{runtime.companion.finishPair(requireNotNull(pending),ticket,clientId,nonce);paired=true;pending=null;ticket="";compare="";invitation="";status="Paired. Only Chrome opening and search are allowed; each command needs confirmation."}}}){Text("Finish after approving on the computer")}
            }
            if(paired){
                Button(enabled=!working,onClick={task{val health=runtime.companion.health();status="Companion health verified now · protocol ${health.getInt("schema_version")} · Chrome open/search only. This is not proof a browser command executed."}}){Text("Check actual connection")}
                Button(enabled=!working,onClick={runtime.submit("open Chrome on my computer and search for ChatGPT");finish()}){Text("Review a Chrome search test")}
                OutlinedButton(enabled=!working,onClick={authorize{task{runtime.companion.revoke();paired=false;status="Computer session revoked and local pairing removed."}}}){Text("Unpair on computer and forget locally")}
                TextButton(enabled=!working,onClick={authorize{runtime.companion.forget();paired=false;status="Local pairing forgotten. If the computer is offline, also use revoke-all there to revoke the old session."}}){Text("Forget locally when the computer is offline")}
            }
            Text("Start the companion visibly on the same private network. Pairing expires after two minutes. A certificate mismatch, missing Chrome, denied approval, offline computer or unverified page is reported as a failure, never as completed. Stop sends best-effort cancellation; it cannot undo a page already opened. Stop/revoke-all on the computer revokes control.")
            Text("Google identity and Google account integrations are separate future milestones; pairing does not grant Gmail, files, other apps or Google account access.")
        }
    }
    override fun onDestroy(){runtime.abandonExclusiveCapture(scannerOwner);pending=null;invitation="";super.onDestroy()}
}
