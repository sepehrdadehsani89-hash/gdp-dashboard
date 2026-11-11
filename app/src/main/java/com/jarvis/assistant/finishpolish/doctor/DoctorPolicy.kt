package com.jarvis.assistant.finishpolish.doctor

enum class ProbeStatus { HEALTHY, DEGRADED, ACTION_NEEDED, UNKNOWN }
enum class HealthSeverity { INFO, DEGRADED, ACTION_NEEDED, CRITICAL }
enum class HealthCertainty { MEASURED, CONFIGURATION, UNKNOWN }
enum class FailureCategory { NONE, PERMISSION, MICROPHONE, RECOGNITION, AUDIO_ROUTE, VOICE_OUTPUT, LOCAL_MODEL, BACKEND, CONNECTION, RENDERING, RESOURCE, PROCESS }
data class HealthProbe(
    val id:String,val owner:String,val requiredPermission:String?=null,val platform:String="Android",
    val sampledAt:Long,val privacy:String="OPERATIONAL_METADATA",val status:ProbeStatus,
    val certainty:HealthCertainty,val category:FailureCategory,val evidence:String,val nextStep:String,
    val evidenceReference:String,val severity:HealthSeverity=when(status){ProbeStatus.HEALTHY,ProbeStatus.UNKNOWN->HealthSeverity.INFO;ProbeStatus.DEGRADED->HealthSeverity.DEGRADED;ProbeStatus.ACTION_NEEDED->HealthSeverity.ACTION_NEEDED},
)
data class DoctorReport(val sampledAt:Long,val probes:List<HealthProbe>){
    val issues get()=probes.filter{it.status in setOf(ProbeStatus.ACTION_NEEDED,ProbeStatus.DEGRADED)}
    fun explanation(spanish:Boolean=false):String {
        val first=issues.firstOrNull()
        return if(first==null){
            if(spanish)"No detecté una avería en las señales comprobadas. Eso no demuestra que el micrófono reconozca bien tu voz. Abre System Doctor para ver lo comprobado y las pruebas pendientes."
            else "I found no failure in the signals checked. That does not prove the microphone recognizes your voice correctly. Open System Doctor to see the evidence and remaining tests."
        } else if(spanish)when(first.category){
            FailureCategory.PERMISSION->"Falta un permiso del micrófono o de las notificaciones. Revisa los permisos de Android en System Doctor; no puedo concederlos automáticamente."
            FailureCategory.LOCAL_MODEL->"El paquete local seleccionado no está disponible. Los comandos escritos siguen funcionando. Abre System Doctor para revisar el modelo."
            FailureCategory.BACKEND->"El último fallo registrado fue del servicio de respuestas, no un fallo demostrado del micrófono. Revisa la conexión y el proveedor en System Doctor."
            FailureCategory.VOICE_OUTPUT->"Se registró un fallo de salida de voz. Revisa el volumen, el servicio y la configuración de voz en System Doctor."
            FailureCategory.RECOGNITION->"Se registró un fallo de reconocimiento. La causa acústica es desconocida; System Doctor muestra los datos y una propuesta reversible de ajustes."
            else->"Hay una señal que necesita revisión. Abre System Doctor para ver la evidencia y las opciones seguras."
        } else first.evidence+" "+first.nextStep
    }
}

data class RepairProposal(
    val id:String="balanced_voice_v1",val title:String="Restore balanced microphone settings",val tier:String="T2",
    val preconditions:String="Phone unlocked, no connected or ringing AI call, no active private capture; user review and approval.",
    val risk:String="Changes only sensitivity, noise reduction and echo cancellation. Microphone processing applies on its next capture.",
    val expectedOutcome:String="Saved balanced sensitivity and enabled available noise/echo processing preferences.",
    val verification:String="Read back the three saved preference fields. Acoustic recognition and hardware processing still require a phone test.",
    val rollback:String="Restore the encrypted last-known settings. No model, voice key, owner template or pairing is deleted.",
    val timeoutMs:Long=5_000,val maxAttempts:Int=3,
)
data class VoiceRepairSettings(val sensitivity:String,val noiseReduction:Boolean,val echoCancellation:Boolean){
    init{require(sensitivity in setOf("LOW","BALANCED","HIGH","CUSTOM"))}
}
enum class RepairState { PREVIEW, APPLYING, VERIFYING, VERIFIED_SETTINGS_ONLY, ROLLED_BACK, FAILED, BLOCKED }
data class RepairOutcome(val state:RepairState,val message:String,val acousticVerified:Boolean=false)

/** Bounded attempts survive via caller-supplied timestamps; no retry loop is started. */
object RepairAdmission {
    const val WINDOW_MS=600_000L
    const val COOLDOWN_MS=60_000L
    fun recent(attempts:List<Long>,now:Long)=attempts.filter{now-it in 0 until WINDOW_MS}.takeLast(3)
    fun denial(approved:Boolean,unlocked:Boolean,idle:Boolean,attempts:List<Long>,now:Long):String?=when {
        !approved->"Review and approve this exact repair first. No settings changed."
        !unlocked->"Unlock the phone before changing voice settings."
        !idle->"Finish the current AI call or private capture before repairing settings."
        attempts.any{it>now}->"The system clock changed. Wait or use Android settings; no repair was attempted."
        recent(attempts,now).size>=3->"Repair circuit breaker: three attempts in ten minutes. Wait before trying again."
        recent(attempts,now).lastOrNull()?.let{now-it<COOLDOWN_MS}==true->"Repair cooldown: wait one minute before another attempt."
        else->null
    }
    fun verified(expected:VoiceRepairSettings,observed:VoiceRepairSettings)=expected==observed
}
