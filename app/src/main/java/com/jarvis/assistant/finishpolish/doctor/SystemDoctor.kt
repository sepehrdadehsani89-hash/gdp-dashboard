package com.jarvis.assistant.finishpolish.doctor

import android.Manifest
import android.app.*
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.*
import androidx.core.content.ContextCompat
import com.jarvis.assistant.AssistantRuntime
import com.jarvis.assistant.core.AssistantPhase
import com.jarvis.assistant.rebirth.MicrophoneSessions
import com.jarvis.assistant.sentinel.JarvisWallpaperService
import com.jarvis.assistant.titan.VoicePreferences
import com.jarvis.assistant.voice.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.*

/** Read-only observations never open a recorder, load weights, change permission or ping a server. */
class SystemDoctor(private val context:Context,private val runtime:AssistantRuntime){
    private val mutable=MutableStateFlow<DoctorReport?>(null)
    val state=mutable.asStateFlow()
    private val repairMutable=MutableStateFlow<RepairOutcome?>(null)
    val repairState=repairMutable.asStateFlow()
    private val vault get()=runtime.features
    private val permission get()=ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED
    private fun unlocked()=!context.getSystemService(KeyguardManager::class.java).isDeviceLocked
    private fun idle()=!runtime.aiCall.state.value.active && MicrophoneSessions.manager.snapshot()?.owner!=com.jarvis.assistant.rebirth.MicrophoneOwner.PRIVATE_CAPTURE
    fun proposal()=RepairProposal()
    fun inspect():DoctorReport {
        val wall=System.currentTimeMillis();val elapsed=SystemClock.elapsedRealtime();val s=runtime.state.value
        val audio=context.getSystemService(AudioManager::class.java)
        val memory=ActivityManager.MemoryInfo();context.getSystemService(ActivityManager::class.java).getMemoryInfo(memory)
        val wake=WakeListenerService.state.value;val lease=MicrophoneSessions.manager.snapshot()
        val recent=runtime.voiceSession.recent().filter{elapsed-it.at in 0..120_000}
        val lastFailure=recent.lastOrNull{it.reason in setOf(VoiceReason.ASR_FAILED,VoiceReason.BACKEND_FAILED,VoiceReason.AUDIO_FAILED)}
        val probes=mutableListOf<HealthProbe>()
        fun add(id:String,status:ProbeStatus,category:FailureCategory,evidence:String,next:String="No change is proposed.",certainty:HealthCertainty=HealthCertainty.MEASURED,permissionName:String?=null,ref:String=id){
            probes+=HealthProbe(id,"System Doctor",permissionName,sampledAt=wall,status=status,certainty=certainty,category=category,evidence=evidence,nextStep=next,evidenceReference=ref)
        }
        add("microphone.permission",if(permission)ProbeStatus.HEALTHY else ProbeStatus.ACTION_NEEDED,FailureCategory.PERMISSION,if(permission)"Android microphone permission is granted." else "Android microphone permission is denied.","Review the app's microphone permission in Android Settings.",permissionName=Manifest.permission.RECORD_AUDIO,ref="ContextCompat.checkSelfPermission")
        val notification=context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        add("foreground.notifications",if(notification)ProbeStatus.HEALTHY else ProbeStatus.ACTION_NEEDED,FailureCategory.PERMISSION,if(notification)"App notifications are enabled." else "App notifications are disabled; visible call and microphone notices may be hidden.","Review notification access in Android Settings.",ref="NotificationManager.areNotificationsEnabled")
        add("microphone.lease",ProbeStatus.HEALTHY,FailureCategory.NONE,if(lease==null)"No recorder currently owns a microphone lease." else "Exactly one microphone lease exists: ${lease.owner.name}.",ref="MicrophoneSessions.manager.snapshot")
        val input=audio.getDevices(AudioManager.GET_DEVICES_INPUTS).isNotEmpty()
        add("microphone.input",if(input)ProbeStatus.HEALTHY else ProbeStatus.ACTION_NEEDED,FailureCategory.MICROPHONE,if(input)"Android reports an audio input device." else "Android reports no audio input device.","Check the selected input and Bluetooth/headset connection.",ref="AudioManager.getDevices")
        add("microphone.level",ProbeStatus.UNKNOWN,FailureCategory.MICROPHONE,if(s.audio.recording)"The existing microphone meter is active; normalized RMS ${(s.audio.level.coerceIn(0f,1f)*100).toInt()}%. Clipping and noise-source attribution are not measured by this probe." else "The microphone meter is inactive. No test recording was started.","Use a consented phone voice test; RMS alone cannot prove intelligibility.",ref="AudioSignals metadata only")
        add("audio.focus",if(audio.mode==AudioManager.MODE_NORMAL)ProbeStatus.HEALTHY else ProbeStatus.DEGRADED,FailureCategory.AUDIO_ROUTE,if(audio.mode==AudioManager.MODE_NORMAL)"Audio mode is normal." else "Another call or communication audio mode is active.","Finish the other call before retrying.",ref="AudioManager.mode")
        val model=runtime.wakeModelInstalled(s.settings.language)
        add("recognition.model",if(model)ProbeStatus.HEALTHY else ProbeStatus.ACTION_NEEDED,FailureCategory.LOCAL_MODEL,if(model)"Selected offline speech pack is installed." else "Selected offline speech pack is unavailable.","Open Voice health to install the selected speech pack explicitly.",HealthCertainty.CONFIGURATION,ref="WakeModelManager.modelPath")
        val mismatch=wake.running && wake.language!=s.settings.language
        add("recognition.language",if(mismatch)ProbeStatus.DEGRADED else ProbeStatus.HEALTHY,FailureCategory.RECOGNITION,if(mismatch)"Selected language differs from the active recognizer; a language handoff may be in progress." else "No selected/active recognizer language mismatch is observed.","Wait for an active handoff; otherwise select the desired mode in Voice health.",ref="WakeState.language versus AssistantSettings.language")
        add("wake.service",if(wake.running)ProbeStatus.HEALTHY else ProbeStatus.UNKNOWN,FailureCategory.NONE,if(wake.running)"The user-enabled wake service is running." else "Wake service is off. This may be the user's choice; it is not proof of a failure.","Use the app's explicit Start listening control when wanted.",ref="WakeListenerService.state")
        add("recognition.final",if(lastFailure?.reason==VoiceReason.ASR_FAILED)ProbeStatus.DEGRADED else ProbeStatus.UNKNOWN,FailureCategory.RECOGNITION,if(lastFailure?.reason==VoiceReason.ASR_FAILED)"A recognition failure was recorded in the last two minutes. Its acoustic cause is not established." else "No recent recognition-failure event is observed; actual recognition accuracy is unmeasured.","Review Voice health and try a quiet, single-command phone test.",ref="VoiceSessionStateMachine.recent")
        add("backend.last_failure",if(lastFailure?.reason==VoiceReason.BACKEND_FAILED)ProbeStatus.DEGRADED else ProbeStatus.UNKNOWN,FailureCategory.BACKEND,if(lastFailure?.reason==VoiceReason.BACKEND_FAILED)"The last recorded failure was a response backend failure, not demonstrated microphone failure." else "No recent backend failure event is observed; server connectivity was not tested.","Review the configured provider, connection and quota. Local deterministic commands remain available.",ref="VoiceReason.BACKEND_FAILED")
        val providerSelected=s.settings.provider!="local"
        val providerKey=if(providerSelected && unlocked())runCatching{runtime.store.getApiKey(s.settings.provider).isNotBlank()}.getOrNull() else null
        add("backend.configuration",when{!providerSelected->ProbeStatus.HEALTHY;providerKey==true->ProbeStatus.HEALTHY;providerKey==false->ProbeStatus.ACTION_NEEDED;else->ProbeStatus.UNKNOWN},FailureCategory.BACKEND,when{!providerSelected->"Local inference mode is selected; no cloud gateway is required for deterministic commands.";providerKey==true->"The selected cloud provider has a saved credential. Access, quota and network were not tested.";providerKey==false->"The selected cloud provider has no available saved credential. Local deterministic commands remain available.";else->"Private provider configuration could not be inspected."},"Review Advanced provider settings if cloud answers are wanted.",HealthCertainty.CONFIGURATION,ref="AssistantSettings.provider and credential presence only")
        val config=if(unlocked())runCatching{runtime.customVoice.config(s.settings.language)}.getOrNull() else null
        add("voice.configuration",if(config?.enabled==true && config.key.isNotBlank())ProbeStatus.HEALTHY else if(config==null)ProbeStatus.UNKNOWN else ProbeStatus.ACTION_NEEDED,FailureCategory.VOICE_OUTPUT,when{config==null->"Private output-voice configuration was not inspected.";config.enabled && config.key.isNotBlank()->"A saved enabled voice profile is configured. Provider access and audible playback are not verified.";else->"The selected output voice is disabled or lacks a saved credential."},"Preview the selected voice in Output voice settings.",HealthCertainty.CONFIGURATION,ref="CustomVoice.config (presence only; no ID/key exported)")
        add("voice.playback",if(lastFailure?.reason==VoiceReason.AUDIO_FAILED)ProbeStatus.DEGRADED else ProbeStatus.UNKNOWN,FailureCategory.VOICE_OUTPUT,if(lastFailure?.reason==VoiceReason.AUDIO_FAILED)"An audio-output failure was recorded recently." else if(s.playback.active)"Playback meter is reporting actual source playback; audible output is not independently measured." else "No playback is active; audible output has not been tested.","Check media volume and use the fixed output-voice preview.",ref="VoiceReason.AUDIO_FAILED and PlaybackLevels")
        val blocked=audio.ringerMode!=AudioManager.RINGER_MODE_NORMAL || context.getSystemService(NotificationManager::class.java).currentInterruptionFilter!=NotificationManager.INTERRUPTION_FILTER_ALL || audio.getStreamVolume(AudioManager.STREAM_MUSIC)==0
        add("voice.volume",if(blocked)ProbeStatus.DEGRADED else ProbeStatus.HEALTHY,FailureCategory.VOICE_OUTPUT,if(blocked)"Silent/DND mode or zero media volume may suppress speech." else "Media volume is nonzero and Silent/DND is not active.","Review audio policy manually; System Doctor does not override it.",ref="AudioManager and NotificationManager")
        val local=runCatching{runtime.brain.modelReady()}.getOrDefault(false)
        add("brain.install",if(local)ProbeStatus.HEALTHY else if(s.settings.provider=="local")ProbeStatus.DEGRADED else ProbeStatus.UNKNOWN,FailureCategory.LOCAL_MODEL,if(local)"Local model installation/verification record is present. Weights were not loaded or rehashed by this scan." else "The selected local conversational model is not ready. Time/date/battery and supported local commands do not require it.","Inspect the existing Neural Core model installer for an explicit download or configuration choice.",HealthCertainty.CONFIGURATION,ref="LocalBrain.modelReady")
        val battery=s.hud.power;val fresh=elapsed-battery.observedAt in 0..150_000 && battery.observedAt>0
        add("battery.telemetry",if(fresh && battery.battery.percent!=null)ProbeStatus.HEALTHY else ProbeStatus.UNKNOWN,FailureCategory.RESOURCE,if(fresh && battery.battery.percent!=null)"Fresh Android battery: ${battery.battery.percent}% · ${battery.condition.name}." else "Fresh battery telemetry is unavailable; no percentage is invented.","Return to the visible app and resample.",ref="BatteryStateRepository")
        add("memory.pressure",if(memory.lowMemory)ProbeStatus.DEGRADED else ProbeStatus.HEALTHY,FailureCategory.RESOURCE,"Android memory now: ${memory.availMem/1048576} MiB available of ${memory.totalMem/1048576} MiB; low-memory flag ${memory.lowMemory}.","Close unused optional features before loading a large model.",ref="ActivityManager.MemoryInfo")
        add("thermal",if(context.getSystemService(PowerManager::class.java).currentThermalStatus>=PowerManager.THERMAL_STATUS_SEVERE)ProbeStatus.DEGRADED else ProbeStatus.HEALTHY,FailureCategory.RESOURCE,"Android thermal status code: ${context.getSystemService(PowerManager::class.java).currentThermalStatus}.","Let the device cool if Android reports severe throttling.",ref="PowerManager.currentThermalStatus")
        val paired=if(unlocked())runCatching{runtime.companion.paired()!=null}.getOrNull() else null
        add("desktop.pairing",ProbeStatus.UNKNOWN,FailureCategory.CONNECTION,when(paired){true->"A saved paired-computer identity exists; its connectivity and session permission were not tested.";false->"No paired computer is saved. This is not a fault if desktop control is unused.";null->"Private pairing could not be inspected."},"Use Check paired connection for an explicit network probe.",HealthCertainty.CONFIGURATION,ref="CompanionClient.paired (identity details excluded)")
        add("renderer.wallpaper",ProbeStatus.UNKNOWN,FailureCategory.RENDERING,"Wallpaper engines ${JarvisWallpaperService.activeEngineCount}, visible ${JarvisWallpaperService.visibleEngineCount}, successfully posted frames ${JarvisWallpaperService.renderedFrames}, recovery errors ${JarvisWallpaperService.recoveredErrors}. A hidden or unselected wallpaper can correctly stop rendering.","Open the existing wallpaper repair screen for visible-surface checks.",ref="JarvisWallpaperService counters")
        add("renderer.motion_policy",ProbeStatus.HEALTHY,FailureCategory.NONE,"Reduced motion: ${s.hud.settings.reducedMotion}; power saver: ${s.hud.telemetry.powerSaver}; system animation enabled: ${s.hud.telemetry.systemMotion}.","Existing accessibility and power preferences are respected.",HealthCertainty.CONFIGURATION,ref="HudSettings and Telemetry")
        if(Build.VERSION.SDK_INT>=30){
            val exits=runCatching{context.getSystemService(ActivityManager::class.java).getHistoricalProcessExitReasons(context.packageName,0,6)}.getOrNull()
            val failure=exits?.firstOrNull{it.reason in setOf(ApplicationExitInfo.REASON_ANR,ApplicationExitInfo.REASON_CRASH,ApplicationExitInfo.REASON_CRASH_NATIVE)}
            add("process.exit",if(failure!=null)ProbeStatus.DEGRADED else ProbeStatus.UNKNOWN,FailureCategory.PROCESS,if(failure!=null)"Android recorded a prior crash/ANR exit: reason ${failure.reason}, time ${failure.timestamp}. Cause is not established by this metadata." else "No crash/ANR exit was returned in the inspected OS history; this is not a measured crash rate.","Use the existing crash metadata screen and a guided reproduction.",ref="ActivityManager.getHistoricalProcessExitReasons; no traces collected")
        }else add("process.exit",ProbeStatus.UNKNOWN,FailureCategory.PROCESS,"OS process-exit history requires Android 11+.",ref="Build.VERSION.SDK_INT")
        val action=runtime.polishActions.state.value
        add("action.last_result",if(action.stage=="FAILED")ProbeStatus.DEGRADED else ProbeStatus.UNKNOWN,if(action.action.startsWith("desktop."))FailureCategory.CONNECTION else FailureCategory.NONE,"Last shared action metadata: ${action.action} · ${action.stage} · sampled ${action.at}. Dispatch is not postcondition verification.","Review the action's own result and permission evidence.",ref="PolishActionJournal")
        return DoctorReport(wall,probes).also{mutable.value=it}
    }
    fun explain(spanish:Boolean=false)=inspect().explanation(spanish)
    suspend fun checkCompanion():DoctorReport {
        val base=inspect();val p=withTimeoutOrNull(8_000){
            try{runtime.companion.health();HealthProbe("desktop.connection","System Doctor",sampledAt=System.currentTimeMillis(),status=ProbeStatus.HEALTHY,certainty=HealthCertainty.MEASURED,category=FailureCategory.CONNECTION,evidence="The saved endpoint responded through certificate-pinned TLS with its expected service/device identity. Command authorization and execution were not tested.",nextStep="Use Mission Control to review a real action.",evidenceReference="CompanionClient.health")}
            catch(e:CancellationException){throw e}
            catch(_:Exception){HealthProbe("desktop.connection","System Doctor",sampledAt=System.currentTimeMillis(),status=ProbeStatus.ACTION_NEEDED,certainty=HealthCertainty.MEASURED,category=FailureCategory.CONNECTION,evidence="The explicit paired connection check failed. The network, agent, pairing or certificate may be unavailable; no specific cause is invented.",nextStep="Open the companion, inspect pairing and retry once.",evidenceReference="CompanionClient.health failure")}
        } ?: HealthProbe("desktop.connection","System Doctor",sampledAt=System.currentTimeMillis(),status=ProbeStatus.ACTION_NEEDED,certainty=HealthCertainty.MEASURED,category=FailureCategory.CONNECTION,evidence="The paired connection probe timed out after eight seconds.",nextStep="Inspect the desktop and network before retrying.",evidenceReference="CompanionClient.health timeout")
        return base.copy(probes=base.probes+p).also{mutable.value=it}
    }
    private fun settings():VoiceRepairSettings=VoicePreferences(context).let{VoiceRepairSettings(it.sensitivity,it.noiseReduction,it.echoCancellation)}
    private fun encode(s:VoiceRepairSettings)=JSONObject().put("schema",1).put("sensitivity",s.sensitivity).put("noise",s.noiseReduction).put("echo",s.echoCancellation).toString()
    private fun decode(raw:String)=JSONObject(raw).let{j->require(j.getInt("schema")==1);VoiceRepairSettings(j.getString("sensitivity"),j.getBoolean("noise"),j.getBoolean("echo"))}
    private fun save(s:VoiceRepairSettings)=context.getSharedPreferences("jarvis_titan_voice_v1",Context.MODE_PRIVATE).edit().putString("sensitivity",s.sensitivity).putBoolean("ns",s.noiseReduction).putBoolean("aec",s.echoCancellation).commit()
    private fun publish(outcome:RepairOutcome):RepairOutcome {repairMutable.value=outcome;return outcome}
    suspend fun applyBalanced(approved:Boolean):RepairOutcome=repairMutex.withLock {
        val now=System.currentTimeMillis()
        val attempts=withContext(Dispatchers.IO){runCatching{
            val raw=vault.read("doctor_attempts")
            if(raw==null){check(!vault.contains("doctor_attempts"));emptyList<Long>()}
            else JSONArray(raw).let{a->require(a.length()<=3);(0 until a.length()).map{a.getLong(it).also{at->require(at>0)}}}
        }.getOrNull()} ?: return@withLock publish(RepairOutcome(RepairState.BLOCKED,"Encrypted repair history is unavailable. No settings changed; inspect private storage before retrying."))
        RepairAdmission.denial(approved,unlocked(),idle(),attempts,now)?.let{return@withLock publish(RepairOutcome(RepairState.BLOCKED,it))}
        withContext(NonCancellable+Dispatchers.IO){
            if(!unlocked() || !idle())return@withContext publish(RepairOutcome(RepairState.BLOCKED,"The phone or capture state changed before applying the repair. No settings changed."))
            var old:VoiceRepairSettings?=null
            val began=SystemClock.elapsedRealtime()
            try{
                vault.write("doctor_attempts",JSONArray((RepairAdmission.recent(attempts,now)+now).takeLast(3)).toString())
                val previous=settings();old=previous
                if(!vault.contains("doctor_last_good"))vault.write("doctor_last_good",encode(previous))
                decode(vault.read("doctor_last_good") ?: error("backup missing")) // Verify an existing encrypted baseline before any setting changes.
                repairMutable.value=RepairOutcome(RepairState.APPLYING,"Applying approved app-local voice settings.")
                runtime.polishActions.record("doctor.balance","EXECUTING","APP_LOCAL_SETTINGS_ONLY")
                val expected=VoiceRepairSettings("BALANCED",true,true);check(save(expected))
                repairMutable.value=RepairOutcome(RepairState.VERIFYING,"Reading back saved settings; no acoustic test is running.")
                runtime.polishActions.record("doctor.balance","VERIFYING","PREFERENCE_READBACK")
                check(RepairAdmission.verified(expected,settings()))
                if(SystemClock.elapsedRealtime()-began>proposal().timeoutMs)error("slow storage")
                runtime.polishActions.record("doctor.balance","VERIFIED","CONFIGURATION_READBACK_ONLY")
                publish(RepairOutcome(RepairState.VERIFIED_SETTINGS_ONLY,"Saved balanced sensitivity, noise-reduction and echo-cancellation preferences were read back successfully. Start a new capture, then run a real voice test. Acoustic recognition and hardware processing were not verified."))
            }catch(_:Exception){
                val restored=old?.let{runCatching{save(it) && settings()==it}.getOrDefault(false)}==true
                runtime.polishActions.record("doctor.balance","FAILED",if(restored)"ROLLED_BACK_AFTER_FAILURE" else "SETTINGS_VERIFICATION_FAILED")
                publish(RepairOutcome(RepairState.FAILED,if(restored)"Repair did not pass verification or exceeded its five-second storage deadline. Previous settings were restored and read back." else "Repair failed; previous settings could not be verified. The encrypted last-known snapshot remains for a manual rollback. No recognition repair is claimed."))
            }
        }
    }
    suspend fun rollbackBalanced(approved:Boolean):RepairOutcome=repairMutex.withLock {
        if(!approved || !unlocked() || !idle())return@withLock publish(RepairOutcome(RepairState.BLOCKED,"Approve rollback while the phone is unlocked and no AI call/private capture is active."))
        withContext(NonCancellable+Dispatchers.IO){
            try{val old=decode(vault.read("doctor_last_good") ?: return@withContext publish(RepairOutcome(RepairState.BLOCKED,"No saved Doctor repair snapshot is available.")));check(save(old));check(settings()==old);runCatching{vault.remove("doctor_last_good")};runtime.polishActions.record("doctor.balance","VERIFIED","ROLLBACK_READBACK_ONLY");publish(RepairOutcome(RepairState.ROLLED_BACK,"Previous app-local voice preferences restored and read back. No model, key, owner profile or pairing changed."))}
            catch(_:Exception){runtime.polishActions.record("doctor.balance","FAILED","ROLLBACK_VERIFICATION_FAILED");publish(RepairOutcome(RepairState.FAILED,"Rollback could not be verified. Review Voice health settings manually."))}
        }
    }
    fun exportReport(report:DoctorReport=state.value ?: inspect()):String=JSONObject().put("schema",1).put("kind","OPERATIONAL_METADATA_ONLY").put("sampled_at",report.sampledAt).put("acoustic_verified",false).put("probes",JSONArray().apply{report.probes.forEach{p->put(JSONObject().put("id",p.id).put("status",p.status.name).put("category",p.category.name).put("certainty",p.certainty.name).put("evidence",p.evidence).put("reference",p.evidenceReference))}}).toString(2)
    companion object{private val repairMutex=Mutex()}
}
