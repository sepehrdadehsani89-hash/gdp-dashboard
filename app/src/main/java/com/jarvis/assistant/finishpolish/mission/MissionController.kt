package com.jarvis.assistant.finishpolish.mission

import com.jarvis.assistant.companion.*
import com.jarvis.assistant.omnisystem.FeatureVault
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import org.json.JSONObject

/** One selected, already trusted computer. State is metadata only and does not persist searches. */
class MissionController(private val companion:CompanionClient,private val vault:FeatureVault,private val now:()->Long=System::currentTimeMillis){
    private val mutable=MutableStateFlow(MissionState());val state=mutable.asStateFlow()
    private val execution=Mutex()
    private val sequence=AtomicLong()
    @Volatile private var activeJob:Job?=null
    private fun name()=runCatching{val saved=JSONObject(vault.read(MissionPolicy.DEVICE_NAME_KEY) ?: return@runCatching null);if(saved.getString("device_id")==companion.paired()?.deviceId)MissionPolicy.friendly(saved.getString("name"))else null}.getOrNull() ?: "My computer"
    suspend fun setName(value:String){withContext(Dispatchers.IO){val pair=companion.paired() ?: throw CompanionFailure("NO_PAIRED_COMPUTER");vault.write(MissionPolicy.DEVICE_NAME_KEY,JSONObject().put("device_id",pair.deviceId).put("name",MissionPolicy.friendly(value)).toString())};mutable.value=mutable.value.copy(label=value.trim())}
    suspend fun refresh():MissionState {
        if(mutable.value.active)return mutable.value
        return probe(false,sequence.incrementAndGet())
    }
    private suspend fun probe(inMission:Boolean,ticket:Long):MissionState {
        if(ticket!=sequence.get() || mutable.value.active && !inMission)return mutable.value
        val label=withContext(Dispatchers.IO){name()}
        if(ticket!=sequence.get())return mutable.value
        mutable.value=mutable.value.copy(phase="CHECKING",label=label,detail="Checking the selected paired computer",verified=false)
        val p=withContext(Dispatchers.IO){runCatching{companion.paired()}.getOrNull()}
        if(ticket!=sequence.get())return mutable.value
        if(p==null){mutable.value=MissionState(phase="AWAITING_PAIRING",label=mutable.value.label,detail="No available private pairing. Pair or unlock before checking.",sampledAt=now());return mutable.value}
        try{
            val readiness=try{companion.capabilities()}catch(e:CompanionFailure){
                if(e.code!="ROUTE_NOT_FOUND")throw e
                companion.health() // Legacy protocol compatibility; never call its readiness verified.
                DesktopReadiness(p.deviceId,"legacy",now(),"legacy",listOf("desktop.open_app","desktop.search_web").map{DesktopCapability(it,"REQUIRES_CONFIRMATION","LEGACY_READINESS_UNVERIFIED")},false,p.sessionId)
            }
            if(ticket!=sequence.get())return mutable.value
            if(readiness.deviceId!=p.deviceId || readiness.sessionId!=p.sessionId)throw CompanionFailure("PAIRED_TARGET_CHANGED_REVIEW_AGAIN")
            mutable.value=mutable.value.copy(phase="CONNECTED",deviceId=p.deviceId,detail=if(readiness.authenticated)"Authenticated capability manifest checked. Each mission still needs review and its own verified receipt." else "Legacy companion reachable; installed-app readiness is unknown. Update the companion for capability checks. Missions still require signed execution receipts.",sampledAt=now(),readiness=readiness)
        }catch(e:CancellationException){throw e}
        catch(e:Exception){if(ticket!=sequence.get())return mutable.value;mutable.value=mutable.value.copy(phase="OFFLINE",deviceId=p.deviceId,detail=error(e),sampledAt=now(),readiness=null)}
        return mutable.value
    }
    fun cancel(){
        val current=mutable.value
        if(!current.active)return
        mutable.value=current.copy(phase="CANCEL_REQUESTED",detail="Cancellation requested. A page already opened cannot be undone.")
        activeJob?.cancel(CancellationException("MISSION_CANCELLED_BY_USER"))
    }
    suspend fun execute(request:DesktopRequest,confirmed:Boolean=true,expectedDeviceId:String?=null,expectedSessionId:String?=null):MissionOutcome {
        if(!confirmed)return MissionOutcome(false,"Review and confirm the desktop action first.","CONFIRMATION_REQUIRED")
        if(!execution.tryLock())return MissionOutcome(false,"A desktop mission is already active. Wait or cancel it.","MISSION_BUSY")
        val id=UUID.randomUUID().toString()
        var commandAttempted=false
        try{
            activeJob=currentCoroutineContext()[Job]
            mutable.value=mutable.value.copy(jobId=id,phase="CHECKING",active=true,verified=false,detail="Checking authorization and current desktop readiness")
            val entryPair=withContext(Dispatchers.IO){companion.paired()}
            if(entryPair==null){mutable.value=mutable.value.copy(phase="NOT_SENT",active=false,detail="No computer was paired when this mission started. Nothing was sent.");return MissionOutcome(false,mutable.value.detail,"NO_PAIRED_COMPUTER")}
            val reviewedDevice=expectedDeviceId ?: entryPair.deviceId
            val reviewedSession=expectedSessionId ?: entryPair.sessionId
            val checked=withTimeout(10_000){probe(true,sequence.incrementAndGet())}
            if(checked.phase!="CONNECTED"){
                mutable.value=checked.copy(jobId=id,phase="NOT_SENT",active=false,verified=false)
                return MissionOutcome(false,checked.detail,"DESKTOP_NOT_READY")
            }
            if(!MissionPolicy.reviewedIdentityMatches(checked.deviceId,checked.readiness?.sessionId,reviewedDevice,reviewedSession)){
                mutable.value=checked.copy(jobId=id,phase="NOT_SENT",active=false,verified=false,detail="The paired computer or authorization session changed. Review this mission again. Nothing was sent.")
                return MissionOutcome(false,mutable.value.detail,"PAIRED_TARGET_CHANGED_REVIEW_AGAIN")
            }
            if(checked.readiness?.allows(request.action)!=true){
                val reason=checked.readiness?.capabilities?.firstOrNull{it.actionId==request.action}?.reason ?: "CAPABILITY_UNAVAILABLE"
                mutable.value=checked.copy(jobId=id,phase="NOT_SENT",detail="Computer reports $reason. Nothing was sent.",active=false,verified=false)
                return MissionOutcome(false,mutable.value.detail,reason)
            }
            mutable.value=checked.copy(jobId=id,phase="REQUESTED",active=true,verified=false,detail="Desktop action requested; no execution result yet.")
            mutable.value=mutable.value.copy(phase="AUTHORIZED",detail="Reviewed action authorized for the selected trusted computer.")
            commandAttempted=true
            val result=withTimeout(30_000){companion.executeObserved(request,reviewedDevice,reviewedSession){progress->
                mutable.value=MissionPolicy.progress(mutable.value,id,progress)
            }}
            mutable.value=mutable.value.copy(phase=if(result.verified)"VERIFIED" else if(result.state=="CANCELLED")"CANCELLED" else "FAILED",active=false,verified=result.verified,detail=result.detail,sampledAt=now())
            return MissionOutcome(result.verified,result.detail,result.errorCode)
        }catch(e:CancellationException){
            mutable.value=mutable.value.copy(jobId=id,phase=if(e is TimeoutCancellationException)"FAILED" else "CANCELLED",active=false,verified=false,detail=if(!commandAttempted)"Mission ${if(e is TimeoutCancellationException)"readiness check timed out" else "cancelled"} before command dispatch. Nothing was sent." else if(e is TimeoutCancellationException)"Mission timed out. Completion is unknown; check the computer before retrying." else "Mission cancelled. Best-effort cancellation was requested; it cannot undo an opened page.")
            if(e is TimeoutCancellationException && currentCoroutineContext().isActive)return MissionOutcome(false,mutable.value.detail,"MISSION_TIMEOUT_STATUS_UNKNOWN")
            throw e
        }catch(e:Exception){
            mutable.value=mutable.value.copy(jobId=id,phase="FAILED",active=false,verified=false,detail=error(e),sampledAt=now())
            return MissionOutcome(false,mutable.value.detail,(e as? CompanionFailure)?.code ?: "MISSION_FAILED")
        }finally{activeJob=null;execution.unlock()}
    }
    private fun error(e:Exception)="Paired computer: ${if(e is CompanionFailure)e.code else "CONNECTION_OR_VERIFICATION_FAILED"}. No verified completion is claimed."
}
