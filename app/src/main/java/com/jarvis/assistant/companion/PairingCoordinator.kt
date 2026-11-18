package com.jarvis.assistant.companion

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

enum class PairingStage { UNPAIRED,SCANNER_OPENING,SCANNING,INVITATION_SCANNED,INVITATION_VALIDATING,INVITATION_READY,REQUESTING_PAIRING,AWAITING_DESKTOP_APPROVAL,DESKTOP_APPROVED,FINALIZING_PAIRING,PAIRED_UNVERIFIED,PAIRED_VERIFIED,EXPIRED,FAILED,CANCELLED,REVOKED }
enum class QrScannerEngine { GOOGLE,CAMERAX_MLKIT,GALLERY_LOCAL }
enum class QrScannerOutcome { OPENING,RESULT_RECEIVED,CANCELLED,FAILED }
data class PairingViewState(val stage:PairingStage=PairingStage.UNPAIRED,val message:String="No live computer connection has been checked.",val address:String="",val pin:String="",val name:String="",val expires:Long=0,val ticket:String="",val comparison:String="",val lastSuccess:Long?=null,val revision:Long=0,val operationInProgress:Boolean=false,val scannerReport:String="NOT_STARTED",val tlsCheckedAt:Long?=null){
    val busy get()=operationInProgress || stage in setOf(PairingStage.SCANNER_OPENING,PairingStage.SCANNING)
    override fun toString()="PairingViewState(stage=$stage, details=redacted)"
}
interface PairingGateway {
    suspend fun inspectInvitation(i:DesktopInvitation){throw CompanionFailure("INVITATION_DIAGNOSTICS_UNSUPPORTED")}
    suspend fun requestPair(i:DesktopInvitation,clientId:String,nonce:String):org.json.JSONObject
    suspend fun pairingStatus(i:DesktopInvitation,c:PairingChallenge,clientId:String,nonce:String):Boolean
    suspend fun finishPair(i:DesktopInvitation,ticket:String,clientId:String,nonce:String):DesktopPair
    suspend fun verifyPair(pair:DesktopPair)
}

/** Secrets remain in RAM, survive rotation through a ViewModel, and never enter saved state/logs. */
class PairingCoordinator(private val gateway:PairingGateway,private val scope:CoroutineScope,private val now:()->Long=System::currentTimeMillis,private val track:(Job)->Unit={}) {
    private val mutable=MutableStateFlow(PairingViewState())
    val state:StateFlow<PairingViewState> = mutable
    var input:String="";private set
    private var invite:DesktopInvitation?=null
    private var challenge:PairingChallenge?=null
    private var client="";private var nonce="";private var epoch=0L
    private var operation:Job?=null
    val scannerOwner=Any()
    private fun stage(value:PairingStage,message:String){mutable.value=mutable.value.copy(stage=value,message=message,revision=mutable.value.revision+1)}
    fun scannerOpening():Boolean {if(mutable.value.busy)return false;setInvitation("");stage(PairingStage.SCANNER_OPENING,"Opening the real QR scanner. The previous in-progress invitation was cleared; saved pairings remain intact.");return true}
    fun scanning(){stage(PairingStage.SCANNING,"Scan the private QR displayed by your computer. Cancel is available in the scanner.")}
    fun scannerInfo(engine:QrScannerEngine,outcome:QrScannerOutcome,code:Int?=null){val previous=mutable.value.scannerReport.takeUnless{it=="NOT_STARTED"}?.lines().orEmpty();val event="$engine · $outcome"+(code?.let{" · SDK code $it"} ?: "");mutable.value=mutable.value.copy(scannerReport=(previous+event).takeLast(6).joinToString("\n"))}
    fun scanCancelled(){stage(PairingStage.CANCELLED,"Scanning cancelled. Retry scanning or paste the private invitation; no pairing request was sent.")}
    fun scanFailure(code:String){stage(PairingStage.FAILED,explain(code))}
    fun setInvitation(raw:String,scanned:Boolean=false){
        if(mutable.value.operationInProgress)return
        epoch++;operation?.cancel();invite=null;challenge=null;client="";nonce=""
        mutable.value=PairingViewState(revision=mutable.value.revision+1,scannerReport=mutable.value.scannerReport)
        input=if(raw.length<=16_384)raw else ""
        if(raw.isBlank()){stage(PairingStage.UNPAIRED,"Paste or scan a fresh private computer invitation.");return}
        if(scanned)stage(PairingStage.INVITATION_SCANNED,"Actual QR payload received.")
        stage(PairingStage.INVITATION_VALIDATING,"Validating invitation…")
        try {
            val i=PairingInvitation.parse(raw,now());invite=i;input=PairingInvitation.normalize(raw)
            mutable.value=mutable.value.copy(address=i.url,pin=i.pin,name=i.deviceName,expires=i.expires)
            stage(PairingStage.INVITATION_READY,"Invitation ready. Review this computer and certificate; pairing starts only when you request it.")
        }catch(e:InvitationFailure){stage(if(e.code=="INVITATION_EXPIRED")PairingStage.EXPIRED else PairingStage.FAILED,explain(e.code))}
    }
    fun tick(){if(invite!=null && now()>=mutable.value.expires && mutable.value.stage in setOf(PairingStage.INVITATION_READY,PairingStage.AWAITING_DESKTOP_APPROVAL,PairingStage.DESKTOP_APPROVED)){stage(PairingStage.EXPIRED,explain("INVITATION_EXPIRED"))};if(mutable.value.stage==PairingStage.PAIRED_VERIFIED && now()-(mutable.value.lastSuccess ?: 0)>60_000)stage(PairingStage.PAIRED_UNVERIFIED,"The last authenticated check is historical. Check the current connection again before assuming it is online.")}
    private fun invitation():DesktopInvitation {
        val i=invite ?: throw CompanionFailure("INVALID_INVITATION")
        if(now()>=i.expires)throw CompanionFailure("INVITATION_EXPIRED")
        return i
    }
    private fun launch(phase:PairingStage,message:String,block:suspend ()->Unit){
        if(mutable.value.busy)return
        val generation=epoch
        stage(phase,message);mutable.value=mutable.value.copy(operationInProgress=true)
        operation=scope.launch {
            try{block()}
            catch(e:CancellationException){if(epoch==generation)stage(PairingStage.CANCELLED,"Operation cancelled. No live connection is assumed. Retry deliberately.");throw e}
            catch(e:Exception){if(epoch==generation){val code=when(e){is CompanionFailure->e.code;is InvitationFailure->e.code;else->"PAIRING_FAILED"};stage(if(code in setOf("INVITATION_EXPIRED","PAIRING_EXPIRED"))PairingStage.EXPIRED else if(code=="PAIRING_DENIED")PairingStage.CANCELLED else PairingStage.FAILED,explain(code))}}
            finally{if(epoch==generation)mutable.value=mutable.value.copy(operationInProgress=false)}
        }.also(track)
    }
    fun request(){
        if(mutable.value.busy)return
        if(challenge!=null){stage(PairingStage.AWAITING_DESKTOP_APPROVAL,"A ticket already exists. Compare the code and check approval; do not submit a different request.");return}
        launch(PairingStage.REQUESTING_PAIRING,"Sending a real pairing request over pinned HTTPS…"){
            val i=invitation()
            if(client.isEmpty()){client=UUID.randomUUID().toString();nonce=UUID.randomUUID().toString()}
            challenge=PairingChallenge.parse(gateway.requestPair(i,client,nonce),i,now())
            val c=requireNotNull(challenge)
            mutable.value=mutable.value.copy(ticket=c.ticket,comparison=c.comparison,expires=c.expires)
            stage(PairingStage.AWAITING_DESKTOP_APPROVAL,"Request received by the companion. Type pending on that computer, compare the six-character code, then approve the actual ticket.")
        }
    }
    fun approval(){launch(PairingStage.REQUESTING_PAIRING,"Checking this ticket's desktop approval…"){
        val approved=gateway.pairingStatus(invitation(),requireNotNull(challenge),client,nonce)
        stage(if(approved)PairingStage.DESKTOP_APPROVED else PairingStage.AWAITING_DESKTOP_APPROVAL,if(approved)"Desktop approved this ticket. Confirm Finish pairing on this phone." else "APPROVAL_PENDING · Compare the code and approve the actual ticket in the companion terminal.")
    }}
    fun inspect(){launch(PairingStage.INVITATION_VALIDATING,"Checking invited HTTPS endpoint, certificate pin, validity, hostname and service identity. No pairing request is being created…"){
        gateway.inspectInvitation(invitation());mutable.value=mutable.value.copy(tlsCheckedAt=now());stage(if(challenge==null)PairingStage.INVITATION_READY else PairingStage.AWAITING_DESKTOP_APPROVAL,"NETWORK REACHABLE · TLS / invited service identity verified by this real read. Authentication and pairing are still separate; no new ticket was created by diagnostics.")
    }}
    fun finish(){launch(PairingStage.FINALIZING_PAIRING,"Finalizing the reviewed pairing…"){
        val i=invitation();val c=challenge ?: throw CompanionFailure("NO_PAIRING_TICKET")
        if(now()>=c.expires)throw CompanionFailure("PAIRING_EXPIRED")
        val p=gateway.finishPair(i,c.ticket,client,nonce)
        input="";invite=null;challenge=null;client="";nonce=""
        mutable.value=mutable.value.copy(ticket="",comparison="",expires=0)
        stage(PairingStage.PAIRED_UNVERIFIED,"Saved pairing. Verifying the authenticated session and capability signature…")
        try{gateway.verifyPair(p);mutable.value=mutable.value.copy(lastSuccess=now());stage(PairingStage.PAIRED_VERIFIED,"PAIRED · ONLINE · AUTHENTICATED · CAPABILITIES VERIFIED now. Each command still needs review.")}
        catch(e:CancellationException){throw e}
        catch(e:Exception){stage(PairingStage.PAIRED_UNVERIFIED,"Pairing saved; authenticated connection is unverified. ${explain((e as? CompanionFailure)?.code ?: "CONNECTION_CHECK_FAILED")}")}
    }}
    fun check(pair:DesktopPair){launch(PairingStage.FINALIZING_PAIRING,"Checking the saved session using a signed, nonce-bound capability response…"){
        gateway.verifyPair(pair);mutable.value=mutable.value.copy(address=pair.url,pin=pair.pin,lastSuccess=now());stage(PairingStage.PAIRED_VERIFIED,"PAIRED · ONLINE · AUTHENTICATED · CAPABILITIES VERIFIED now. This is not command completion.")
    }}
    fun selected(){cancel(clear=true);stage(PairingStage.PAIRED_UNVERIFIED,"Saved target selected. Current connectivity and authentication are unverified.")}
    fun revoked(){cancel(clear=true);stage(PairingStage.REVOKED,"This local pairing was removed. Remote revocation depends on the action's actual result.")}
    fun cancel(clear:Boolean=true){epoch++;operation?.cancel();operation=null;if(clear){input="";invite=null;challenge=null;client="";nonce="";mutable.value=PairingViewState(revision=mutable.value.revision+1,scannerReport=mutable.value.scannerReport)};stage(PairingStage.CANCELLED,"Cancelled. Private in-progress invitation cleared; saved computer pairings were preserved. Generate a fresh invitation if needed.")}
    fun diagnostics():String=mutable.value.let{"Jarvis pairing diagnostics v1\nState: ${it.stage}\nScanner: ${it.scannerReport}\nEndpoint: ${it.address.ifEmpty{"not selected"}}\nCertificate SHA-256: ${it.pin.ifEmpty{"not selected"}}\nLast invited TLS identity check: ${it.tlsCheckedAt ?: "not verified"}\nLast successful authenticated check: ${it.lastSuccess ?: "not verified"}\nStatus: ${it.message}\nInvitation, proof, ticket, comparison code and session credentials: omitted\n"}
    companion object {
        fun explain(code:String):String=when(code){
            "INVITATION_EXPIRED","PAIRING_EXPIRED"->"INVITATION_EXPIRED · Type invite on the computer and scan its fresh two-minute invitation. Check both clocks."
            "INVITATION_CLOCK_OR_EXPIRATION"->"INVITATION_CLOCK_OR_EXPIRATION · Check the phone and computer clocks and generate a fresh invitation."
            "INVALID_INVITATION","INVITATION_TOO_LARGE"->"$code · Use the full JARVIS-PAIR-1 invitation, not the QR filename, URL, certificate or ticket. Nothing was truncated."
            "UNSUPPORTED_PROTOCOL"->"UNSUPPORTED_PROTOCOL · Update the companion using the supplied source, preserving its private state."
            "TLS_PIN_OR_CERTIFICATE_ERROR"->"TLS_CERTIFICATE_MISMATCH · Certificate, validity or hostname verification failed. Check the original computer identity and address; never disable TLS verification."
            "DESKTOP_TIMEOUT_STATUS_UNKNOWN"->"PAIRING_TIMEOUT · The response was not received. Check pending on the computer; retry uses the same request while this invitation is valid."
            "DESKTOP_CONNECTION_UNAVAILABLE"->"NETWORK_UNREACHABLE · Check the companion is running on its actual private LAN address, both devices' network, guest isolation and Windows private-network firewall permission."
            "DESKTOP_APPROVAL_REQUIRED"->"APPROVAL_PENDING · Compare the code and approve the actual ticket on the computer."
            "PAIRING_DENIED"->"APPROVAL_DENIED · The computer operator denied this request. Generate a new invitation to try again."
            "PAIR_STATUS_UNSUPPORTED"->"The older companion cannot report approval status. Approve its ticket, then use Finish pairing; update the companion source while preserving its identity."
            "UNAUTHORIZED"->"REAUTHENTICATION_REQUIRED · The saved session was rejected. Check revocation and pair again deliberately."
            "CAMERA_PERMISSION_DENIED"->"CAMERA_PERMISSION_DENIED · Allow Camera for the bundled scanner using Android permission settings, or use Google Code Scanner, paste or gallery import."
            "CAMERA_UNAVAILABLE"->"CAMERA_UNAVAILABLE · Android reports no camera on this device. Use private invitation paste or local gallery import."
            "CAPTURE_BUSY"->"CAPTURE_BUSY · Finish the active camera/capture or Android credential screen, then retry scanning."
            "SCANNER_UNAVAILABLE"->"SCANNER_UNAVAILABLE · Check Google Play services; try the bundled camera scanner or import a QR image."
            "SCANNER_FAILED"->"SCANNER_FAILED · Retry or use the bundled camera scanner, paste or local gallery import."
            "NO_QR_IN_IMAGE"->"NO_QR_IN_IMAGE · Choose a clear image containing the complete QR, including its white border. SVG files should be displayed and scanned, or use the private text invitation."
            "PAIRING_FAILED"->"PAIRING_FAILED · The operation failed. Existing pairings were preserved; check permissions, private storage and the companion."
            else->if(code in setOf("REQUEST_REJECTED","INVALID_PAIRING_CHALLENGE","NO_PAIRING_TICKET","INVALID_DESKTOP_RESPONSE","CAPABILITY_VERIFICATION_FAILED","QR_IMAGE_TOO_LARGE","PAIRING_PROOF_INVALID","RATE_LIMITED","PENDING_LIMIT","TICKET_NOT_FOUND","INVALID_TICKET","PAIRING_ALREADY_COMPLETED","ALREADY_APPROVED","CONNECTION_CHECK_FAILED","NO_PAIRED_COMPUTER","PAIRED_TARGET_CHANGED_REVIEW_AGAIN","EMPTY_RECEIPT","RECEIPT_TOO_LARGE","COMPANION_INTERNAL_ERROR"))"$code · No verified completion is claimed. Check the companion and retry deliberately." else "PAIRING_FAILED · No verified completion is claimed."
        }
    }
}
