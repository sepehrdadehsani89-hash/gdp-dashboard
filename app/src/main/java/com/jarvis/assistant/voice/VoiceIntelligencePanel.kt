package com.jarvis.assistant.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.AssistantRuntime
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.choose
import com.jarvis.assistant.titan.VoicePreferences
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun VoiceIntelligencePanel(runtime:AssistantRuntime){
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val p=remember{VoicePreferences(context)}
    val diagnostic=remember{MicrophoneDiagnostic(context,runtime)}
    val test by diagnostic.state.collectAsStateWithLifecycle()
    val input by com.jarvis.assistant.hud.AudioSignals.state.collectAsStateWithLifecycle()
    val endpoint by EndpointDiagnostics.state.collectAsStateWithLifecycle()
    val shared by runtime.state.collectAsStateWithLifecycle()
    val l=shared.settings.language
    var revision by remember{mutableIntStateOf(0)}
    @Suppress("UNUSED_VARIABLE") val refresh=revision
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){revision++}
    val allowed=ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED
    DisposableEffect(lifecycle,diagnostic){
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_PAUSE)diagnostic.stop()}
        lifecycle.addObserver(observer)
        onDispose{lifecycle.removeObserver(observer);diagnostic.close()}
    }
    fun words(en:String,es:String)=l.choose(en,es,en)
    Text(words("Microphone & end of turn","Micrófono y fin del turno"),style=MaterialTheme.typography.titleLarge)
    FlowRow{listOf("LOW","BALANCED","HIGH","CUSTOM").forEach{value->FilterChip(p.sensitivity==value,{p.sensitivity=value;revision++},enabled=!test.active,label={Text(value)})}}
    if(p.sensitivity=="CUSTOM"){
        Text("RMS ${String.format(Locale.ROOT,"%.3f",p.customEnergy)}")
        Slider(p.customEnergy,{p.customEnergy=it;revision++},valueRange=.005f.. .08f,enabled=!test.active)
        Text(words("Final word confidence","Confianza final por palabra")+": ${(p.customConfidence*100).toInt()}%")
        Slider(p.customConfidence,{p.customConfidence=it;revision++},valueRange=.45f.. .9f,enabled=!test.active)
    }
    Text(words("Natural pause allowance","Pausa natural permitida")+": ${p.endpointPauseMs} ms")
    Slider(p.endpointPauseMs.toFloat(),{p.endpointPauseMs=it.toLong();revision++},valueRange=650f..1800f,enabled=!test.active)
    Text(words("Shorter pauses finish faster; longer pauses allow complex questions. Speech confidence and bounded recovery still apply. One microphone cannot perfectly separate nearby speakers.","Las pausas cortas responden antes; las largas permiten preguntas complejas. Se conservan confianza y recuperación limitada. Un micrófono no separa perfectamente otras voces."))
    Row{Switch(p.noiseReduction,{p.noiseReduction=it;revision++},enabled=!test.active);Text(words("Request Android noise suppression","Solicitar reducción de ruido Android"))}
    Row{Switch(p.echoCancellation,{p.echoCancellation=it;revision++},enabled=!test.active);Text(words("Request acoustic echo cancellation","Solicitar cancelación de eco"))}
    Row{Switch(p.gainControl,{p.gainControl=it;revision++},enabled=!test.active);Text(words("Request automatic gain control","Solicitar ganancia automática"))}
    Text(words("Device availability","Disponibilidad del dispositivo")+": NS ${android.media.audiofx.NoiseSuppressor.isAvailable()} · AEC ${android.media.audiofx.AcousticEchoCanceler.isAvailable()}")
    Text(words("Changes apply to the next local recorder. Android recognition services manage their own processing; availability does not establish active noise rejection.","Los cambios se aplican al siguiente grabador local. Los servicios Android gestionan su audio; disponibilidad no demuestra reducción activa."))
        Text("Microphone interruption during replies is disabled. Use Stop Speaking.")
    Text(words("Echo-checked stop/wait phrases use the existing opt-in wake service. General acoustic interruption needs a supported simultaneous capture path.","Las frases de parada/espera usan la activación voluntaria y comprueban eco. La interrupción acústica general requiere captura simultánea compatible."))
    Row{Switch(p.transcriptionPreview,{p.transcriptionPreview=it;if(!it)diagnostic.clearPreview();revision++},enabled=!test.active);Text(words("Optional user transcript preview · main app only","Vista opcional de transcripción · solo aplicación"))}
    Text(words("Assistant caption delay after playback","Permanencia de subtítulos tras reproducir")+": ${p.captionLingerMs} ms")
    Slider(p.captionLingerMs.toFloat(),{p.captionLingerMs=it.toLong();revision++},valueRange=0f..5000f,enabled=!test.active)
    Text(words("Speech playback speed","Velocidad de reproducción")+": ${String.format(Locale.ROOT,"%.2f",shared.hud.settings.voiceRate)}×")
    Slider(shared.hud.settings.voiceRate,{runtime.saveHud(runtime.hud.state.value.settings.copy(voiceRate=it))},valueRange=.7f..1.3f,enabled=!test.active)
    OutlinedButton(enabled=!test.active,onClick={context.startActivity(Intent(context,com.jarvis.assistant.omnisystem.SpeakerProfilesActivity::class.java))}){Text(words("Optional speaker model & consent","Modelo de hablante y consentimiento"))}
    Text(words("Speaker model installed","Modelo de hablante instalado")+": ${runtime.speakers.present()}. "+words("Enrollment is optional and never unlocks Android.","El registro es opcional y nunca desbloquea Android."))
    HorizontalDivider()
    Text(words("Controlled microphone diagnostic","Diagnóstico controlado del micrófono"),style=MaterialTheme.typography.titleLarge)
    Text(words("Permission","Permiso")+": $allowed · "+words("Recording","Grabando")+": ${input.recording}")
    LinearProgressIndicator(progress={input.level.coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
    Text("${input.source} · RMS ${String.format(Locale.ROOT,"%.3f",input.level)}")
    Text("${endpoint.engine} · ${endpoint.state} · ${endpoint.elapsedMs} ms")
    Text(endpoint.vadCapability)
    endpoint.finalizationMs?.let{Text(words("Finalization wait","Espera de resultado final")+": $it ms")}
    endpoint.error?.let{Text(it)}
    val localPack=runtime.wakeModelInstalled(l)
    val onDevice=android.os.Build.VERSION.SDK_INT>=31 && android.speech.SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    val systemService=android.speech.SpeechRecognizer.isRecognitionAvailable(context)
    val usable=localPack || onDevice || shared.settings.allowNetworkSpeech && systemService
    Text(words("Usable with current settings","Utilizable con los ajustes actuales")+": $usable · ${l.label}")
    Text(words("Local pack / Android on-device / system service / network speech allowed","Modelo local / Android local / servicio del sistema / voz por red permitida")+": $localPack / $onDevice / $systemService / ${shared.settings.allowNetworkSpeech}")
    Text(words("No neural VAD confidence is reported by this path. Level is measured audio, not a speech or identity probability.","Esta ruta no entrega confianza de VAD neuronal. El nivel es audio medido, no probabilidad de voz ni identidad."))
    Text(words("Tap Start, ask one question, then leave background sound playing. This test only checks recognition and ending: it never calls AI or executes commands. Your configured recognizer may use network only when network speech is enabled. Audio is not saved here.","Pulsa Iniciar, pregunta y deja sonar el fondo. Solo prueba reconocimiento y cierre: no llama a IA ni ejecuta órdenes. El reconocedor puede usar internet solo si se permite voz por red. No se guarda audio."))
    FlowRow{
        if(!allowed)OutlinedButton(onClick={permission.launch(Manifest.permission.RECORD_AUDIO)}){Text(words("Allow microphone","Permitir micrófono"))}
        Button(enabled=allowed && !test.active,onClick=diagnostic::start){Text(words("Start diagnostic","Iniciar diagnóstico"))}
        if(test.active)OutlinedButton(onClick={diagnostic.stop()}){Text(words("Stop test","Detener prueba"))}
    }
    var clock by remember{mutableLongStateOf(SystemClock.elapsedRealtime())}
    LaunchedEffect(test.active,test.captureAt){while(test.active){diagnostic.checkOwnership();clock=SystemClock.elapsedRealtime();delay(100)}}
    if(test.active && test.captureAt>0)Text(words("Capture elapsed","Tiempo de captura")+": ${(clock-test.captureAt).coerceAtLeast(0)} ms")
    if(test.summary.isNotBlank())Text(test.summary)
    if(p.transcriptionPreview && test.preview.isNotBlank())Text(test.preview)
    TextButton(enabled=!test.active,onClick={context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))}){Text(words("Android settings · speech service and input route","Ajustes Android · servicio de voz y entrada"))}
}
