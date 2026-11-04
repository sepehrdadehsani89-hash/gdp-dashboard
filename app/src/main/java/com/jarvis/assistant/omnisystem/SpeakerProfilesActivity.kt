package com.jarvis.assistant.omnisystem

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.Locale

class SpeakerProfilesActivity:PrivateModuleActivity(){
    private var profiles by mutableStateOf(emptyList<SpeakerProfile>())
    private var name by mutableStateOf("")
    private var owner by mutableStateOf(true)
    private var consent by mutableStateOf(false)
    private var installed by mutableStateOf(false)
    private var auto by mutableStateOf(false)
    private var fraction by mutableFloatStateOf(0f)
    private val microphone=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        status=if(granted)words("Microphone allowed. Review capture consent before continuing.","Micrófono permitido. Revisa el consentimiento antes de continuar.","میکروفون مجاز شد. پیش از ادامه، رضایت ضبط را تأیید کنید.") else words("Microphone denied; no sample recorded.","Micrófono denegado; no se grabó muestra.","مجوز میکروفون رد شد؛ نمونه‌ای ضبط نشد.")
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);if(isFinishing)return
        reload()
        if(intent.getBooleanExtra("voice_setup",false) && profiles.none{it.owner})name=runtime.store.settings.value.name
        screen(words("JARVIS · OPTIONAL VOICE PROFILES","JARVIS · PERFILES DE VOZ OPCIONALES","جارویس · نمایه‌های صوتی اختیاری")){
            Text(words("One owner and up to five additional consenting people. Voice similarity is personalization only, never Android authentication. No voice profile unlocks the phone or grants access to private actions.","Un propietario y hasta cinco personas adicionales con consentimiento. La similitud solo personaliza; nunca autentica Android ni autoriza acciones privadas.","یک مالک و حداکثر پنج فرد دیگر با رضایت. شباهت صدا فقط برای شخصی‌سازی است؛ قفل گوشی را باز نمی‌کند و مجوز اقدام خصوصی نمی‌دهد."))
            Text("WeSpeaker VoxCeleb ResNet34 LM · 26.5 MB · CC BY 4.0")
            Text(words("The model was trained in an English/VoxCeleb domain. Spanish/Persian and Redmi 9 identification accuracy have not been measured. There is no probability or security guarantee.","El modelo usa dominio inglés/VoxCeleb. No se midió precisión en español/persa ni Redmi 9. No hay garantía de identidad o seguridad.","مدل در دامنهٔ انگلیسی و VoxCeleb آموزش دیده است. دقت اسپانیایی، فارسی و Redmi 9 اندازه‌گیری نشده است. تضمین هویت یا امنیت وجود ندارد."))
            Text(if(installed)words("Model file present; SHA-256 checked before native loading.","Modelo presente; SHA-256 se comprueba antes de cargarlo.","فایل مدل موجود است؛ SHA-256 پیش از بارگذاری بررسی می‌شود.") else words("Optional model is not installed. Nothing downloads automatically.","El modelo opcional no está instalado. No hay descargas automáticas.","مدل اختیاری نصب نشده است. دانلود خودکار انجام نمی‌شود."))
            Button(enabled=!working && !installed,onClick={AlertDialog.Builder(this@SpeakerProfilesActivity).setTitle(words("Download the real speaker model?","¿Descargar el modelo real de voz?","مدل واقعی گوینده دریافت شود؟"))
                .setMessage("26,530,550 bytes · ${SpeakerProfiles.LICENSE}\nSHA-256: ${SpeakerProfiles.SHA}\n"+words("Download from the official Sherpa-ONNX GitHub release. No voice recordings or templates are sent. Mobile data may cost money.","Se descarga de GitHub oficial de Sherpa-ONNX. No se envían voces ni plantillas. Los datos móviles pueden tener costo.","دانلود از انتشار رسمی Sherpa-ONNX در گیت‌هاب است. صدا یا الگوی صوتی ارسال نمی‌شود. اینترنت همراه ممکن است هزینه داشته باشد."))
                .setNegativeButton(words("Cancel","Cancelar","لغو"),null).setPositiveButton(words("Download","Descargar","دریافت")){_,_->task{
                    fraction=0f
                    runtime.speakers.download{value->fraction=value};installed=runtime.speakers.present()
                    status=words("Verified speaker model installed","Modelo de voz verificado instalado","مدل صوتی تأییدشده نصب شد")
                }}.show()
            }){Text(words("Download optional model","Descargar modelo opcional","دریافت مدل اختیاری"))}
            if(working && fraction>0f)LinearProgressIndicator(progress={fraction},modifier=Modifier.fillMaxWidth())
            TextButton(onClick={open("https://github.com/wenet-e2e/wespeaker/blob/master/docs/pretrained.md")}){Text(words("Model attribution & license source","Atribución y licencia del modelo","منبع انتساب و مجوز مدل"))}
            TextButton(onClick={open("https://creativecommons.org/licenses/by/4.0/")}){Text("Creative Commons Attribution 4.0")}
            Text(words("WeSpeaker authors and VoxCeleb dataset creators; official ONNX distribution by Sherpa-ONNX. No weights are modified or bundled in this APK.","Autores de WeSpeaker y creadores de VoxCeleb; ONNX distribuido por Sherpa-ONNX. No se modifican ni incluyen pesos en el APK.","نویسندگان WeSpeaker و سازندگان مجموعهٔ VoxCeleb؛ انتشار ONNX توسط Sherpa-ONNX. وزن‌ها تغییر نکرده‌اند و در APK جاسازی نمی‌شوند."))
            Text("${profiles.size}/6 · "+words("enrolled profiles","perfiles registrados","نمایهٔ ثبت‌شده"))
            profiles.forEach{p->
                Text(p.name+" · "+if(p.owner)words("Owner","Propietario","مالک") else words("Additional profile","Perfil adicional","نمایهٔ دیگر"))
                Text("${p.samples} · "+words("approved samples","muestras aprobadas","نمونهٔ تأییدشده"))
                OutlinedButton(enabled=!working && consent && installed && p.samples<10,onClick={askCapture(p)}){Text(words("Add another 20-second sample","Añadir otra muestra de 20 segundos","افزودن نمونهٔ ۲۰ثانیه‌ای دیگر"))}
                TextButton(enabled=!working,onClick={authorize{runtime.speakers.delete(p.id);runtime.clearSpeakerSuggestion();reload()}}){Text(words("Delete encrypted template","Eliminar plantilla cifrada","حذف الگوی رمز‌شده"))}
            }
            OutlinedTextField(name,{name=it.take(80)},label={Text(words("Person's chosen name","Nombre elegido por la persona","نام انتخابی فرد"))},enabled=!working,modifier=Modifier.fillMaxWidth())
            Row{Checkbox(owner,{owner=it},enabled=!working && profiles.none{it.owner});Text(words("Owner profile","Perfil del propietario","نمایهٔ مالک"))}
            Row{Checkbox(consent,{consent=it},enabled=!working);Text(words("The person is present and explicitly agrees to local voice-template enrollment and its deletion controls.","La persona está presente y acepta explícitamente registrar su plantilla local y los controles de eliminación.","فرد حاضر است و صریحاً با ثبت الگوی صوتی محلی و کنترل‌های حذف آن موافق است."))}
            Button(enabled=!working && installed && consent && name.isNotBlank() && profiles.size<6 && (if(owner)profiles.none{it.owner} else profiles.count{!it.owner}<5),onClick={askCapture(null)}){Text(words("Enroll with 20 seconds of speech","Registrar con 20 segundos de voz","ثبت با ۲۰ ثانیه گفتار"))}
            OutlinedButton(enabled=!working && installed && profiles.isNotEmpty(),onClick={askMatch()}){Text(words("Test a possible match · not authentication","Probar coincidencia posible · no autentica","آزمایش تطبیق احتمالی · نه احراز هویت"))}
            if(working && fraction>0f)Text(String.format(Locale.ROOT,"%.0f%%",fraction*100)+" · "+words("actual capture/download progress","progreso real de captura/descarga","پیشرفت واقعی ضبط یا دانلود"))
            Row{Switch(auto,{value->if(!working){if(value)task{runtime.speakers.prepare();authorize{runtime.speakers.setAuto(true);auto=true}} else authorize{runtime.speakers.setAuto(false);auto=false;runtime.clearSpeakerSuggestion()}}},enabled=!working && installed && profiles.isNotEmpty());Text(words("Optional profile hint after wake commands","Perfil opcional tras comandos de activación","پیشنهاد اختیاری نمایه پس از فرمان بیدارباش"))}
            Text(words("Opt-in uses at most four seconds already captured by the user-enabled wake microphone. It starts no extra recorder, uploads nothing and clears transient samples. Hints expire and never authorize tools. Tap-to-talk system recognizers may not expose audio for matching.","Usa hasta cuatro segundos ya capturados por el micrófono de activación habilitado. No inicia otro grabador ni sube datos; borra muestras temporales. Las sugerencias caducan y nunca autorizan herramientas. El reconocedor del sistema puede no exponer audio.","حداکثر چهار ثانیه از صدای میکروفون بیدارباش فعال‌شده توسط کاربر استفاده می‌شود. ضبط‌کنندهٔ دیگری شروع یا داده‌ای ارسال نمی‌شود؛ نمونه‌های موقت پاک می‌شوند. پیشنهادها منقضی می‌شوند و ابزارها را مجاز نمی‌کنند. تشخیص‌گر سیستم ممکن است صدا را برای تطبیق ارائه نکند."))
            if(profiles.isNotEmpty())OutlinedButton(enabled=!working,onClick={AlertDialog.Builder(this@SpeakerProfilesActivity).setTitle(words("Delete all voice templates?","¿Eliminar todas las plantillas?","همهٔ الگوهای صوتی حذف شوند؟")).setNegativeButton(words("Cancel","Cancelar","لغو"),null).setPositiveButton(words("Delete","Eliminar","حذف")){_,_->authorize{runtime.speakers.clearProfiles();runtime.clearSpeakerSuggestion();reload()}}.show()}){Text(words("Delete all templates","Eliminar todas las plantillas","حذف همهٔ الگوها"))}
            if(installed)OutlinedButton(enabled=!working,onClick={task{runtime.speakers.removeModel();installed=false;auto=false;runtime.clearSpeakerSuggestion();status=words("Model removed; encrypted templates retained.","Modelo eliminado; plantillas cifradas conservadas.","مدل حذف شد؛ الگوهای رمز‌شده حفظ شدند.")}}){Text(words("Remove model weights","Eliminar pesos del modelo","حذف وزن‌های مدل"))}
        }
    }
    private fun reload(){runCatching{runtime.speakers.profiles()}.onSuccess{profiles=it;owner=it.none{p->p.owner}}.onFailure{status=words("Encrypted profiles unavailable; existing data was not overwritten.","Perfiles cifrados no disponibles; no se sobrescribieron.","نمایه‌های رمز‌شده در دسترس نیستند؛ داده‌ها بازنویسی نشدند.")};installed=runtime.speakers.present();auto=runtime.speakers.autoEnabled}
    private fun hasMicrophone():Boolean {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)return true
        microphone.launch(Manifest.permission.RECORD_AUDIO);return false
    }
    private fun askCapture(existing:SpeakerProfile?){
        if(!consent || !hasMicrophone())return
        val chosen=existing?.name ?: name.trim();val isOwner=existing?.owner ?: owner
        authorize{AlertDialog.Builder(this).setTitle(words("Record consenting person: $chosen?","¿Grabar a la persona que consiente: $chosen?","ضبط صدای فرد موافق: $chosen؟"))
            .setMessage(words("Speak naturally for 20 seconds. Raw audio is discarded; only an encrypted local embedding template remains. An owner sample also enables local greeting personalization. This cannot unlock the phone.","Habla naturalmente durante 20 segundos. Se descarta el audio; solo queda una plantilla local cifrada. No desbloquea el teléfono.","۲۰ ثانیه طبیعی صحبت کنید. صدای خام حذف می‌شود و فقط الگوی محلی رمز‌شده باقی می‌ماند. این کار قفل گوشی را باز نمی‌کند."))
            .setNegativeButton(words("Cancel","Cancelar","لغو"),null).setPositiveButton(words("Record","Grabar","ضبط")){_,_->capture(20){audio->
                val vector=runtime.speakers.embedding(audio)
                try{runtime.speakers.enroll(chosen,isOwner,vector,true,existing?.id);if(isOwner)runtime.speakers.setAuto(true)}finally{vector.fill(0f)};name="";consent=false;reload()
                status=words("Consented voice template saved locally","Plantilla consentida guardada localmente","الگوی صوتی با رضایت، محلی ذخیره شد")
            }}.show()}
    }
    private fun askMatch(){if(!hasMicrophone())return
        AlertDialog.Builder(this).setTitle(words("Record five seconds for a local similarity test?","¿Grabar cinco segundos para una prueba local?","پنج ثانیه برای آزمایش شباهت محلی ضبط شود؟"))
            .setMessage(words("This returns a possible profile or unknown/ambiguous result, never a verified identity. Nothing is uploaded.","Devuelve un perfil posible o resultado desconocido/ambiguo, nunca identidad verificada. No se sube nada.","نتیجه فقط نمایهٔ احتمالی یا ناشناخته/مبهم است، نه هویت تأییدشده. چیزی ارسال نمی‌شود."))
            .setNegativeButton(words("Cancel","Cancelar","لغو"),null).setPositiveButton(words("Test","Probar","آزمایش")){_,_->capture(5){audio->
                val vector=runtime.speakers.embedding(audio);val match=SpeakerProfilePolicy.suggest(vector,runtime.speakers.profiles());vector.fill(0f)
                status=match.profile?.let{words("Possible profile: ${it.name}","Perfil posible: ${it.name}","نمایهٔ احتمالی: ${it.name}")} ?: if(match.ambiguous)words("Ambiguous match","Coincidencia ambigua","تطبیق مبهم") else words("Unknown voice","Voz desconocida","صدای ناشناخته")
                match.similarity?.let{status+=" · "+words("cosine similarity","similitud coseno","شباهت کسینوسی")+String.format(Locale.ROOT," %.2f",it)}
            }}.show()
    }
    private fun capture(seconds:Int,after:suspend (CapturedVoice)->Unit){task{
        fraction=0f
        runtime.speakers.prepare();check(runtime.beginExclusiveCapture(this@SpeakerProfilesActivity))
        var voice:CapturedVoice?=null;var audible=0;var frames=0
        try {
            fraction=0f
            status=words("Preparing the spoken recording prompt…","Preparando el aviso de grabación…","آماده‌سازی پیام ضبط…")
            runtime.playEnrollmentPrompt(this@SpeakerProfilesActivity,seconds)
            status=words("Recording now · speak naturally","Grabando ahora · habla con naturalidad","در حال ضبط · طبیعی صحبت کنید")
            voice=PrivateAudioCapture.record(this@SpeakerProfilesActivity,seconds){level,value,_->fraction=value;frames++;if(level>.01f)audible++}
            require(audible>=(if(seconds==20)40 else 10) && audible.toFloat()/frames>=.2f){"More clear speech is needed."}
            status=words("Computing the local speaker embedding…","Calculando la representación local de voz…","در حال محاسبهٔ بردار صوتی محلی…")
            after(voice)
        } finally {voice?.pcm?.fill(0);withContext(NonCancellable+Dispatchers.Main.immediate){runtime.endExclusiveCapture(this@SpeakerProfilesActivity)}}
    }}
    private fun open(url:String){runCatching{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}.onFailure{status=words("No browser available","No hay navegador disponible","مرورگر در دسترس نیست")}}
    override fun onStop(){runtime.speakers.releaseAsync();super.onStop()}
}
