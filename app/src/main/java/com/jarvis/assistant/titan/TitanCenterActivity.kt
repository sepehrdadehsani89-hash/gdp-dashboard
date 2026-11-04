package com.jarvis.assistant.titan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.core.*
import com.jarvis.assistant.omnisystem.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
class TitanCenterActivity:PrivateModuleActivity(){
    private var page by mutableStateOf("Capabilities")
    private var revision by mutableIntStateOf(0)
    private val voice by lazy{VoicePreferences(this)}
    private var noise:AcousticMeasurement?=null
    private var result by mutableStateOf("")
    private var source by mutableStateOf("")
    private var level by mutableFloatStateOf(0f)
    private var progress by mutableFloatStateOf(0f)
    private var recording by mutableStateOf(false)
    private var routines by mutableStateOf(emptyList<Routine>())
    private var routineId:String?=null
    private var routineName by mutableStateOf("")
    private var trigger by mutableStateOf("")
    private var steps by mutableStateOf("battery\ntime")
    private var routineEnabled by mutableStateOf(false)
    private val owner=Any()
    private val microphone=registerForActivityResult(ActivityResultContracts.RequestPermission()){revision++;status=words("Microphone permission result received. Recording has not started.","Permiso de micrófono recibido. No se inició grabación.","Permission result")}
    private val phoneState=registerForActivityResult(ActivityResultContracts.RequestPermission()){revision++;runtime.batteryAlerts.drain()}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);if(isFinishing)return
        page=intent.getStringExtra("page")?.takeIf{it in listOf("Capabilities","Voice Lab","Battery","Routines","Setup")} ?: "Capabilities"
        reloadRoutines()
        screen("JARVIS · QUANTUM CORE 10.9"){
            @Suppress("UNUSED_VARIABLE")val current=revision
            FlowRow{listOf("Capabilities","Voice Lab","Battery","Routines","Setup").forEach{label->FilterChip(page==label,{if(!working)page=label},label={Text(label)},enabled=!working)}}
            when(page){"Voice Lab"->voiceLab();"Battery"->battery();"Routines"->routineStudio();"Setup"->setup();else->capabilities()}
        }
    }
    @Composable private fun voiceLab(){
        Text(words("VOICE CALIBRATION LAB","LABORATORIO DE VOZ","Voice Lab"),style=MaterialTheme.typography.titleLarge)
        Text(words("Samples are captured only after a tap, processed locally and erased from memory afterward. dBFS is a digital level, not environmental dBA. Noise suppression availability does not prove improved recognition.","Las muestras requieren pulsar, se procesan localmente y se borran de memoria. dBFS es nivel digital, no dBA ambiental. Tener reducción de ruido no demuestra mejor reconocimiento.","Local measurements"))
        OutlinedButton(onClick={microphone.launch(Manifest.permission.RECORD_AUDIO)}){Text(words("Allow microphone · optional","Permitir micrófono · opcional","Microphone"))}
        FlowRow{listOf("LOW","BALANCED","HIGH","CUSTOM").forEach{value->FilterChip(voice.sensitivity==value,{voice.sensitivity=value;revision++},label={Text(value)},enabled=!working)}}
        if(voice.sensitivity=="CUSTOM"){
            Text("Voice activity RMS threshold: ${voice.customEnergy}");Slider(voice.customEnergy,{voice.customEnergy=it;revision++},valueRange=.005f.. .08f,enabled=!working)
            Text("Final word confidence threshold: ${voice.customConfidence}");Slider(voice.customConfidence,{voice.customConfidence=it;revision++},valueRange=.45f.. .9f,enabled=!working)
        }
        Toggle("Android noise suppression",voice.noiseReduction){voice.noiseReduction=it;revision++}
        Toggle("Android acoustic echo cancellation",voice.echoCancellation){voice.echoCancellation=it;revision++}
        Toggle("Android automatic gain control · optional",voice.gainControl){voice.gainControl=it;revision++}
        Text(words("Changes apply when the recorder next starts. Echo cancellation does not enable automatic TTS barge-in; supported recording and output paths remain separate.","Los cambios se aplican al reiniciar el grabador. La cancelación de eco no activa interrupción acústica automática del TTS; las rutas siguen separadas.","Restart capture"))
        Button(enabled=!working,onClick={calibrate(false)}){Text(words("Measure 3 seconds of background noise","Medir 3 segundos de ruido ambiente","Noise test"))}
        Button(enabled=!working,onClick={calibrate(true)}){Text(words("Record 6-second wake phrase test · no command runs","Grabar prueba de activación de 6 segundos · sin ejecutar comandos","Wake test"))}
        if(recording){LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth());Text("Live PCM RMS: "+String.format(Locale.ROOT,"%.4f",level))}
        if(source.isNotBlank())Text(source)
        if(result.isNotBlank())Text(result)
        Text("Greeting boundaries · device local time")
        var morning by remember{mutableStateOf(voice.morning.toString())};var afternoon by remember{mutableStateOf(voice.afternoon.toString())}
        var evening by remember{mutableStateOf(voice.evening.toString())};var night by remember{mutableStateOf(voice.night.toString())}
        OutlinedTextField(morning,{morning=it.take(2)},label={Text("Morning starts 0–23")})
        OutlinedTextField(afternoon,{afternoon=it.take(2)},label={Text("Afternoon starts")})
        OutlinedTextField(evening,{evening=it.take(2)},label={Text("Evening starts")})
        OutlinedTextField(night,{night=it.take(2)},label={Text("Night starts")})
        Button(onClick={val m=morning.toIntOrNull();val a=afternoon.toIntOrNull();val e=evening.toIntOrNull();val n=night.toIntOrNull()
            if(m!=null && a!=null && e!=null && n!=null && GreetingPolicy.validBounds(m,a,e,n)){voice.morning=m;voice.afternoon=a;voice.evening=e;voice.night=n;status="Greeting boundaries saved"}else status="Use four increasing hours from 0 through 23"
        }){Text("Save greeting boundaries")}
        Text(words("Sir is the requested courtesy greeting, not proof of identity. A current local match may personalize the greeting; uncertain matches stay neutral. Voice never authenticates private access or unlocks Android.","El propietario recibe señor solo tras coincidencia fiable de la muestra actual. Con identidad incierta o dispositivo bloqueado, saludo neutral. La voz no autentica acceso privado.","No voice authentication"))
        OutlinedButton(onClick={startActivity(Intent(this,SpeakerProfilesActivity::class.java))}){Text("Six consenting speaker profiles")}
        OutlinedButton(onClick={startActivity(Intent(this,com.jarvis.assistant.voice.VoiceSettingsActivity::class.java))}){Text("English / exclusive Spanish Fish Audio preview")}
    }
    private fun calibrate(speech:Boolean){task{
        check(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)
        check(getSystemService(AudioManager::class.java).mode==AudioManager.MODE_NORMAL){"Finish calls or other communication first."}
        check(runtime.beginExclusiveCapture(owner))
        recording=true;progress=0f;result="";source=""
        var pcm:CapturedVoice?=null
        try{
            val captured=PrivateAudioCapture.record(this,if(speech)6 else 3,onCaptureInfo={source=it}){rms,done,_->level=rms;progress=done}
            pcm=captured
            check(runtime.captureIsCurrent(owner))
            val measured=VoiceMeasurements.measure(captured.pcm)
            var recognized=""
            if(speech){
                val model=runtime.installedWakeModelForCalibration()
                recognized=if(model==null)"Offline speech model not installed; no recognition score invented." else withContext(Dispatchers.IO){
                    Model(model.absolutePath).use{engine->Recognizer(engine,16000f).use{recognizer->
                        recognizer.setWords(true);recognizer.acceptWaveForm(captured.pcm,captured.pcm.size)
                        val json=JSONObject(recognizer.finalResult);val text=json.optString("text")
                        val array=json.optJSONArray("result");val confidence=if(array!=null && array.length()>0)(0 until array.length()).map{array.getJSONObject(it).optDouble("conf",Double.NaN)}.takeIf{it.all(Double::isFinite)}?.average() else null
                        "Actual transcript: $text\nWake phrase matched: ${com.jarvis.assistant.voice.WakePhraseMatcher.extractCommand(text,runtime.store.settings.value.language)!=null}\nMean reported word confidence: ${confidence?.let{String.format(Locale.ROOT,"%.3f",it)} ?: "unavailable"}"
                    }}
                }
            }else noise=measured
            check(runtime.captureIsCurrent(owner))
            result=String.format(Locale.ROOT,"Measured %.1f s · RMS %.5f · %.1f dBFS · clipping %.2f%%",measured.seconds,measured.rms,measured.dbfs,measured.clippingFraction*100)+
                (if(speech && noise!=null)"\nSignal/background difference: "+String.format(Locale.ROOT,"%.1f dB",VoiceMeasurements.signalNoiseDb(measured.rms,noise!!.rms)) else "")+"\n"+recognized
            status=words("Local measurement completed; raw audio erased","Medición local completada; audio original borrado","Completed")
        }finally{pcm?.pcm?.fill(0);recording=false;runtime.endExclusiveCapture(owner)}
    }}
    @Composable private fun battery(){
        val state by runtime.state.collectAsStateWithLifecycle()
        val p=runtime.batteryAlerts.preferences
        val reading=state.hud.telemetry.battery
        Text(words("BATTERY INTELLIGENCE 10.7","INTELIGENCIA DE BATERÍA 10.7","Battery"),style=MaterialTheme.typography.titleLarge)
        Text("Android: ${reading.percent ?: "unavailable"}% · charging ${reading.charging} · ${reading.source}\nBand: ${BatteryPolicy.band(reading.percent)}\nTemperature: ${reading.temperatureC ?: "unavailable"} °C\nHealth classification: ${reading.health}")
        Toggle("BATTERY VOICE ALERTS",p.enabled){p.enabled=it;revision++;runtime.batteryAlerts.drain()}
        Toggle("CRITICAL ALERTS · 5% through 1%",p.critical){p.critical=it;revision++}
        Toggle("Text notifications when speech is blocked",p.notifications){p.notifications=it;revision++}
        Toggle("Quiet hours · device local time",p.quiet){p.quiet=it;revision++}
        var start by remember{mutableStateOf(p.start.toString())};var end by remember{mutableStateOf(p.end.toString())}
        OutlinedTextField(start,{start=it.take(2)},label={Text("Quiet start hour")});OutlinedTextField(end,{end=it.take(2)},label={Text("Quiet end hour")})
        Button(onClick={val a=start.toIntOrNull();val b=end.toIntOrNull();if(a in 0..23 && b in 0..23){p.start=a!!;p.end=b!!;revision++}else status="Use hours 0–23"}){Text("Save quiet hours")}
        var volume by remember{mutableFloatStateOf(p.volume)}
        Text("Alert volume ${(volume*100).toInt()}%");Slider(volume,{volume=it},onValueChangeFinished={p.volume=volume})
        OutlinedButton(onClick={phoneState.launch(Manifest.permission.READ_PHONE_STATE)}){Text(words("Optional call-state permission · needed for safe spoken alerts","Permiso opcional de llamadas · necesario para avisos de voz seguros","Call-state permission"))}
        Text(words("Alerts need an alive Android app process and safe audio availability. Android may stop background delivery. Missing call-state permission, active calls, Silent/DND, credential UI or quiet hours prevent speech. No repeated percentage timer or missed-level replay.","Los avisos requieren proceso Android activo y audio seguro. Android puede detener entregas. Sin permiso de llamadas, durante llamadas, Silencio/No molestar, autenticación o descanso, no se habla. Sin temporizador repetitivo ni repetición de niveles omitidos.","Best effort Android delivery"))
        Text("Last alert status: "+p.lastStatus)
        TextButton(onClick={revision++}){Text("Refresh alert status")}
    }
    @Composable private fun routineStudio(){
        val shared by runtime.state.collectAsStateWithLifecycle()
        Text("ROUTINE STUDIO",style=MaterialTheme.typography.titleLarge)
        Text(words("Up to 12 routines, each 1–8 steps. Existing checked tool execution is reused. Permitted steps: battery, time, weather in a named city, my calendar today, my tasks, open an app. Protected/ambiguous actions pause; later steps do not run behind approval.","Hasta 12 rutinas, con 1–8 pasos. Se reutiliza el motor verificado. Pasos: battery, time, weather in Quito, my calendar today, my tasks, open Spotify. Acciones ambiguas o protegidas pausan los pasos posteriores.","Checked existing engine"))
        OutlinedTextField(routineName,{routineName=it.take(80)},label={Text("Routine name")},enabled=!working)
        OutlinedTextField(trigger,{trigger=it.take(100)},label={Text("Exact spoken trigger")},enabled=!working)
        OutlinedTextField(steps,{steps=it.take(1400)},label={Text("One supported command per line")},modifier=Modifier.fillMaxWidth(),enabled=!working)
        Toggle("Enabled",routineEnabled){routineEnabled=it}
        Button(enabled=!working,onClick={val id=routineId;val name=routineName;val phrase=trigger;val commands=steps.lines().filter{it.isNotBlank()};val enabled=routineEnabled;authorize{
            runtime.routines.save(id,name,phrase,commands,enabled);resetRoutine();reloadRoutines();status="Routine saved privately"
        }}){Text("Approve and save routine")}
        TextButton(onClick=::resetRoutine){Text("Cancel editor / new routine")}
        routines.forEach{r->
            HorizontalDivider();Text("${r.name} · ${if(r.enabled)"ENABLED" else "DISABLED"}\nTrigger: ${r.trigger}\n"+r.steps.joinToString(" → "))
            TextButton(onClick={routineId=r.id;routineName=r.name;trigger=r.trigger;steps=r.steps.joinToString("\n");routineEnabled=r.enabled}){Text("Edit")}
            TextButton(enabled=r.enabled,onClick={runtime.submit(r.trigger);status="Routine requested through existing runtime; inspect actual results below."}){Text("Run / test enabled trigger")}
            TextButton(onClick={authorize{runtime.routines.remove(r.id);reloadRoutines()}}){Text("Delete")}
        }
        if(shared.phase==AssistantPhase.THINKING)Text("Executing · "+shared.neuralStage)
        if(shared.liveReply.isNotBlank())Text(shared.liveReply)
        shared.messages.lastOrNull{it.role==MessageRole.ASSISTANT}?.let{Text(it.text)}
        if(shared.pendingAction!=null)OutlinedButton(onClick={startActivity(Intent(this,com.jarvis.assistant.MainActivity::class.java))}){Text("Review pending action in Assistant · later routine steps paused")}
    }
    private fun resetRoutine(){routineId=null;routineName="";trigger="";steps="battery\ntime";routineEnabled=false}
    private fun reloadRoutines(){runCatching{runtime.routines.all()}.onSuccess{routines=it}.onFailure{status="Encrypted routines unavailable; existing data was not overwritten."}}
    @Composable private fun capabilities(){
        OutlinedButton(onClick={startActivity(Intent(this,com.jarvis.assistant.quantum.ControlCenterActivity::class.java))}){Text("Quantum Control Center · actual device controls and accessory intelligence")}
        Text("CAPABILITY CENTER",style=MaterialTheme.typography.titleLarge)
        Text(words("Available means configured for an on-device test, not proof of recognition, acoustic quality, account quota or background/OEM behavior. No simulated successes.","Disponible significa configurado para probar en el teléfono, no prueba de reconocimiento, calidad acústica, cuota ni funcionamiento del fabricante. Sin éxitos simulados.","Actual current configuration"))
        val readings=CapabilityCenter.read(this,runtime)
        readings.forEach{r->Text("${r.name} · ${r.status}\n${r.evidence}")}
        OutlinedButton(onClick={page="Voice Lab"}){Text("Open microphone / recognition tests")}
        OutlinedButton(onClick={startActivity(Intent(this,com.jarvis.assistant.voice.VoiceSettingsActivity::class.java))}){Text("Open real TTS previews")}
        OutlinedButton(onClick={startActivity(Intent(this,com.jarvis.assistant.neural.NeuralSettingsActivity::class.java))}){Text("Open actual local-model benchmark / aliases / private backup")}
        OutlinedButton(onClick={startActivity(Intent(this,LocationHudActivity::class.java))}){Text("Open real location test")}
        OutlinedButton(onClick={startActivity(Intent(this,CameraHudActivity::class.java))}){Text("Open camera permission / preview test")}
        OutlinedButton(onClick={startActivity(Intent(this,com.jarvis.assistant.infinity.InfinityControlsActivity::class.java))}){Text("Open real Wi-Fi / Bluetooth / network diagnostics")}
        OutlinedButton(onClick={startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName))}){Text("Android notification settings")}
        Text("Last measured local inference: "+(runtime.brain.lastMetrics?.json() ?: "NOT TESTED · run a configured model on this phone"))
        TextButton(onClick={revision++}){Text("Refresh actual capabilities")}
    }
    @Composable private fun setup(){
        Text("PERSONAL AI SETUP",style=MaterialTheme.typography.titleLarge)
        Text(words("All steps are optional. Existing settings, downloads and memories are preserved. Skip any item; permissions and microphone activation require your own taps.","Todos los pasos son opcionales. Se conservan ajustes, descargas y recuerdos. Omite lo que quieras; permisos y micrófono requieren tus pulsaciones.","Optional setup"))
        FlowRow{AppLanguage.supported.forEach{lang->FilterChip(runtime.store.settings.collectAsStateWithLifecycle().value.language==lang,{runtime.setLanguage(lang);revision++},label={Text(lang.label)})}}
        OutlinedButton(onClick={startActivity(Intent(this,com.jarvis.assistant.voice.VoiceSettingsActivity::class.java))}){Text("1 · Configure and test English / exclusive Spanish voice")}
        OutlinedButton(onClick={page="Voice Lab"}){Text("2 · Calibrate microphone and wake sensitivity")}
        OutlinedButton(onClick={microphone.launch(Manifest.permission.RECORD_AUDIO)}){Text("3 · Optional microphone permission")}
        OutlinedButton(onClick={runtime.downloadWake()}){Text("4 · Download selected language's offline recognition model")}
        OutlinedButton(onClick={runtime.startWake()}){Text("5 · Explicitly start wake listening")}
        OutlinedButton(onClick={runtime.stopEverything()}){Text("Stop all Jarvis listening and operations")}
        OutlinedButton(onClick={startActivity(Intent(this,com.jarvis.assistant.neural.NeuralSettingsActivity::class.java))}){Text("6 · Local model preferences / 25 aliases / memory import")}
        OutlinedButton(onClick={startActivity(Intent(this,MemoryCardsActivity::class.java))}){Text("7 · Approved memory cards")}
        OutlinedButton(onClick={startActivity(Intent(this,com.jarvis.assistant.sentinel.SentinelWallpaperSettingsActivity::class.java))}){Text("8 · Wallpaper settings and Android picker")}
        OutlinedButton(onClick={startActivity(Intent(this,LocationHudActivity::class.java))}){Text("9 · Optional location / Home setup")}
        OutlinedButton(onClick={startActivity(Intent(this,com.jarvis.assistant.contacts.ContactShortcutsActivity::class.java))}){Text("10 · Optional 15 contact shortcuts")}
        OutlinedButton(onClick={runtime.submit("what time is it");status="First command requested; inspect Assistant for the actual result."}){Text("11 · Test a harmless time command")}
        OutlinedButton(onClick={page="Capabilities"}){Text("12 · Review current capabilities")}
        TextButton(onClick={finish()}){Text("Skip / finish setup")}
    }
    @Composable private fun Toggle(label:String,value:Boolean,change:(Boolean)->Unit){Row{Switch(value,change,enabled=!working);Text(label)}}
    override fun onResume(){super.onResume();revision++}
    override fun onDestroy(){runtime.abandonExclusiveCapture(owner);super.onDestroy()}
}
