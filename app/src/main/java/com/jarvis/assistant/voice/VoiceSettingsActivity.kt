package com.jarvis.assistant.voice

import android.media.MediaPlayer
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VoiceSettingsActivity:PrivateModuleActivity(){
    private var id by mutableStateOf(CustomVoicePolicy.REQUESTED_ID)
    private var model by mutableStateOf("s2.1-pro")
    private var key by mutableStateOf("")
    private var confirmed by mutableStateOf(false)
    private var tested:CustomVoiceConfig?=null
    private var active by mutableStateOf(false)
    private var previewReady by mutableStateOf(false)
    private var player:MediaPlayer?=null
    private var audioFile:File?=null
    private val owner=Any()
    private fun invalidate(){tested=null;previewReady=false;stopPreview()}
    private fun stopPreview(){player?.let{runCatching{it.stop()};it.release()};player=null;audioFile?.delete();audioFile=null;runtime.endExclusiveCapture(owner)}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);if(isFinishing)return
        val saved=runtime.customVoice.config();id=saved.id;model=saved.model;active=saved.enabled
        screen(words("JARVIS · OUTPUT VOICE","JARVIS · VOZ DE SALIDA","جارویس · صدای خروجی")){
            Text(words("Requested voice ID is editable. A voice ID is not an API key. Confirm its service before testing. This adapter supports Fish Audio; the ID's provider has not been independently verified.","El ID solicitado es editable y no es una clave API. Confirma el servicio antes de probar. Este adaptador admite Fish Audio; el proveedor del ID no se ha verificado.","شناسهٔ صدا قابل ویرایش است و کلید API نیست. پیش از آزمایش سرویس را تأیید کنید. این اتصال برای Fish Audio است؛ سرویس این شناسه مستقلاً تأیید نشده است."))
            Text(if(active)words("Custom voice enabled for eligible general replies","Voz personalizada activa para respuestas generales permitidas","صدای سفارشی برای پاسخ‌های عمومی مجاز فعال است") else words("Custom voice disabled · existing voice remains available","Voz personalizada desactivada · voz anterior disponible","صدای سفارشی غیرفعال · صدای قبلی در دسترس است"))
            OutlinedTextField(id,{invalidate();id=it.take(100)},enabled=!working,label={Text("Voice ID")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(model,{invalidate();model=it.take(60)},enabled=!working,label={Text("Fish Audio model")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(key,{invalidate();key=it.take(1000)},enabled=!working,label={Text(words("Fish Audio API key · blank keeps saved key","Clave API Fish Audio · vacío conserva la guardada","کلید API سرویس Fish Audio · خالی یعنی حفظ کلید ذخیره‌شده"))},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
            RowServiceConfirmation()
            Text(words("Preview sends only a fixed sample to api.fish.audio and may incur account charges. Enabling sends eligible general reply text there. Private tool, file, screen, memory and security replies stay on-device. Service failure falls back to the local voice.","La prueba envía una muestra fija a api.fish.audio y puede tener coste. Al activar, se envían respuestas generales permitidas. Herramientas, archivos, pantalla, memoria y seguridad quedan en el dispositivo. Si falla, se usa la voz local.","آزمایش فقط متن نمونهٔ ثابت را به api.fish.audio می‌فرستد و ممکن است هزینه داشته باشد. فعال‌سازی متن پاسخ عمومی مجاز را ارسال می‌کند. پاسخ ابزار، فایل، صفحه، حافظه و امنیت روی گوشی می‌ماند. در خطا، صدای محلی استفاده می‌شود."))
            Button(enabled=!working && confirmed && CustomVoicePolicy.validId(id),onClick={task{
                invalidate()
                val c=CustomVoiceConfig(id.trim(),model.trim(),key.trim().ifBlank{runtime.customVoice.config().key},false)
                check(c.key.isNotBlank())
                if(!runtime.beginExclusiveCapture(owner)){status=words("Finish the current microphone session first.","Termina la sesión de micrófono.","ابتدا نشست میکروفن فعلی را پایان دهید.");return@task}
                try{
                    val bytes=runtime.customVoice.preview(c)
                    val f=withContext(Dispatchers.IO){File.createTempFile("voice-preview-",".mp3",cacheDir).apply{writeBytes(bytes)}}
                    bytes.fill(0);audioFile=f
                    val p=MediaPlayer();player=p;p.setDataSource(f.absolutePath)
                    withContext(Dispatchers.IO){p.prepare()}
                    suspendCancellableCoroutine<Unit>{continuation->
                        continuation.invokeOnCancellation{runOnUiThread{stopPreview()}}
                        p.setOnCompletionListener{if(continuation.isActive)continuation.resume(Unit)}
                        p.setOnErrorListener{_,_,_->if(continuation.isActive)continuation.resumeWithException(java.io.IOException("Preview playback failed."));true}
                        status=words("Playing actual service preview…","Reproduciendo prueba real…","در حال پخش نمونهٔ واقعی سرویس…")
                        p.start()
                    }
                    check(runtime.captureIsCurrent(owner))
                    stopPreview();tested=c;previewReady=true
                    status=words("Preview completed. Enable only if this is your intended voice.","Prueba terminada. Activa solo si es la voz deseada.","آزمایش تمام شد. فقط اگر صدای مورد نظر است فعال کنید.")
                }catch(e:Exception){stopPreview();throw e}
            }}){Text(words("Consent and play service preview","Aceptar y reproducir prueba","تأیید و پخش نمونهٔ سرویس"))}
            Button(enabled=previewReady && !working,onClick={val c=tested ?: return@Button;authorize{
                runtime.customVoice.save(CustomVoiceConfig(c.id,c.model,c.key,true));key="";active=true;tested=null;previewReady=false
                status=words("Selected voice saved and enabled","Voz guardada y activada","صدای انتخاب‌شده ذخیره و فعال شد")
            }}){Text(words("Use this voice for replies","Usar esta voz en respuestas","استفاده از این صدا برای پاسخ‌ها"))}
            OutlinedButton(enabled=!working,onClick={authorize{invalidate();runtime.customVoice.disable();active=false}}){Text(words("Disable custom voice","Desactivar voz personalizada","غیرفعال کردن صدای سفارشی"))}
            TextButton(enabled=!working,onClick={authorize{invalidate();runtime.customVoice.clear();key="";active=false;id=CustomVoicePolicy.REQUESTED_ID}}){Text(words("Remove saved voice credentials","Eliminar credenciales de voz","حذف کلید ذخیره‌شدهٔ صدا"))}
        }
    }
    @Composable private fun RowServiceConfirmation(){
        androidx.compose.foundation.layout.Row{
            Checkbox(confirmed,{invalidate();confirmed=it},enabled=!working)
            Text(words("I confirm this ID and API key are for Fish Audio","Confirmo que ID y clave son de Fish Audio","تأیید می‌کنم این شناسه و کلید برای Fish Audio هستند"))
        }
    }
    override fun onStop(){stopPreview();tested=null;previewReady=false;key="";super.onStop()}
    override fun onDestroy(){stopPreview();super.onDestroy()}
}
