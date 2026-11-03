package com.jarvis.assistant.contacts

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.jarvis.assistant.omnisystem.PrivateAudioCapture
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** New visible MIC samples only when Android reports idle. Never requests a call-audio source. */
class PersonalMemoActivity:PrivateModuleActivity(){
    private var wav:ByteArray?=null
    private var available by mutableStateOf(false)
    private var fraction by mutableFloatStateOf(0f)
    private var capturing by mutableStateOf(false)
    private var saveCopy:ByteArray?=null
    private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){status=words("Permission result received. Recording still needs your explicit approval.","Permiso recibido. La grabación aún requiere tu aprobación.","نتیجهٔ مجوز دریافت شد. ضبط همچنان به تأیید صریح شما نیاز دارد.")}
    private val save=registerForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")){uri->
        val bytes=saveCopy;saveCopy=null
        if(uri==null || bytes==null || locked()){bytes?.fill(0);return@registerForActivityResult}
        task{try{
            withContext(Dispatchers.IO){contentResolver.openOutputStream(uri,"wt").use{out->check(out!=null);out.write(bytes);out.flush()}}
            status=words("Memo written to your selected destination.","Nota escrita en el destino elegido.","پیام در مقصد انتخابی شما نوشته شد.")
        }finally{bytes.fill(0)}}
    }
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
        screen(words("JARVIS · PERSONAL VOICE MEMO","JARVIS · NOTA DE VOZ PERSONAL","جارویس · پیام صوتی شخصی")){
            Text(words("This records a new message from you, not a call. Android must report an idle phone and no call/communication audio mode. Call outcome remains unknown. No recording starts automatically.","Graba un mensaje nuevo tuyo, no una llamada. Android debe indicar teléfono inactivo y sin audio de llamada. El resultado de la llamada sigue desconocido. No se graba automáticamente.","این قابلیت پیام جدید شما را ضبط می‌کند، نه تماس را. اندروید باید تلفن را بی‌تماس و بدون حالت صوتی تماس گزارش کند. نتیجهٔ تماس نامشخص می‌ماند. ضبط خودکار شروع نمی‌شود."))
            OutlinedButton(onClick={permission.launch(Manifest.permission.RECORD_AUDIO)}){Text(words("Allow microphone","Permitir micrófono","مجوز میکروفون"))}
            OutlinedButton(onClick={permission.launch(Manifest.permission.READ_PHONE_STATE)}){Text(words("Allow call-state check · not call logs","Permitir estado de llamada · no historial","مجوز بررسی وضعیت تماس · نه سابقهٔ تماس"))}
            Button(enabled=!working,onClick=::consent){Text(words("Review and record a 20-second memo","Revisar y grabar nota de 20 segundos","تأیید و ضبط پیام ۲۰ثانیه‌ای"))}
            if(capturing)LinearProgressIndicator(progress={fraction})
            if(available){
                OutlinedButton(enabled=!working,onClick=::play){Text(words("Play locally","Reproducir localmente","پخش روی گوشی"))}
                OutlinedButton(enabled=!working,onClick={saveCopy?.fill(0);saveCopy=wav?.copyOf();runCatching{save.launch("Jarvis-personal-memo.wav")}.onFailure{saveCopy?.fill(0);saveCopy=null;status=words("Android save picker unavailable","Selector de guardado no disponible","انتخاب‌گر ذخیرهٔ اندروید در دسترس نیست")}}){Text(words("Save with Android file picker","Guardar con selector Android","ذخیره با انتخاب‌گر فایل اندروید"))}
                OutlinedButton(enabled=!working,onClick=::share){Text(words("Share · choose app and recipient manually","Compartir · elegir app y destinatario","اشتراک · انتخاب دستی برنامه و گیرنده"))}
                TextButton(onClick=::clear){Text(words("Discard this memo","Descartar esta nota","حذف این پیام"))}
            }
            Text(words("Leaving clears the unsaved preview. A pending Android Save choice retains only its approved in-memory copy until the result. Shared files stay in private cache for the receiving app; files older than 24 hours are removed on the next explicit memo operation.","Salir borra la vista no guardada. Una selección Guardar pendiente conserva solo su copia aprobada hasta el resultado. Los archivos compartidos quedan en caché privada; los de más de 24 horas se borran en la siguiente operación de nota.","خروج پیش‌نمایش ذخیره‌نشده را پاک می‌کند. انتخاب ذخیرهٔ در انتظار فقط نسخهٔ تأییدشده را تا دریافت نتیجه در حافظه نگه می‌دارد. فایل اشتراک در حافظهٔ نهان خصوصی برای برنامهٔ گیرنده می‌ماند؛ فایل‌های قدیمی‌تر از ۲۴ ساعت در عملیات صریح بعدی پیام پاک می‌شوند."))
        }
    }
    private fun allowed(p:String)=ContextCompat.checkSelfPermission(this,p)==PackageManager.PERMISSION_GRANTED
    @Suppress("DEPRECATION") private fun idle():Boolean=allowed(Manifest.permission.READ_PHONE_STATE) && runCatching{getSystemService(TelephonyManager::class.java).callState==TelephonyManager.CALL_STATE_IDLE && getSystemService(AudioManager::class.java).mode==AudioManager.MODE_NORMAL}.getOrDefault(false)
    private fun consent(){
        if(!allowed(Manifest.permission.RECORD_AUDIO) || !idle()){status=words("Microphone permission and an Android-reported idle call state are required. Nothing recorded.","Se requieren permiso de micrófono y estado inactivo de llamada. No se grabó nada.","مجوز میکروفون و گزارش بی‌تماس بودن تلفن از اندروید لازم است. چیزی ضبط نشد.");return}
        AlertDialog.Builder(this).setTitle(words("Record only your new personal message?","¿Grabar solo tu mensaje nuevo?","فقط پیام شخصی جدید شما ضبط شود؟"))
            .setMessage(words("Twenty seconds from the microphone, no upload. Recording is refused during a reported call. Playback, saving and sharing are separate choices.","Veinte segundos de micrófono, sin subir. Se rechaza durante una llamada reportada. Reproducir, guardar y compartir son decisiones separadas.","بیست ثانیه از میکروفون، بدون بارگذاری. هنگام گزارش تماس، ضبط انجام نمی‌شود. پخش، ذخیره و اشتراک انتخاب‌های جدا هستند."))
            .setNegativeButton(words("Cancel","Cancelar","لغو"),null).setPositiveButton(words("Record","Grabar","ضبط")){_,_->task{
                check(idle());check(runtime.beginExclusiveCapture(this@PersonalMemoActivity));clear();cleanup()
                var voice:com.jarvis.assistant.omnisystem.CapturedVoice?=null
                try{fraction=0f;capturing=true;voice=PrivateAudioCapture.record(this@PersonalMemoActivity,20){_,value,_->check(idle()){ "Call state changed; recording stopped." };fraction=value}
                    check(idle());wav=voice.wav();available=true;status=words("New personal memo captured locally. Review it before saving or sharing.","Nota nueva capturada localmente. Revísala antes de guardar o compartir.","پیام شخصی جدید روی گوشی ضبط شد. پیش از ذخیره یا اشتراک بررسی کنید.")
                }finally{capturing=false;voice?.pcm?.fill(0);withContext(NonCancellable+Dispatchers.Main.immediate){runtime.endExclusiveCapture(this@PersonalMemoActivity)}}
            }}.show()
    }
    private fun play(){task{
        val bytes=wav ?: return@task;check(idle());check(runtime.beginExclusiveCapture(this@PersonalMemoActivity))
        val file=File.createTempFile("jarvis-memo-preview-",".wav",cacheDir);var player:MediaPlayer?=null
        try{withContext(Dispatchers.IO){file.writeBytes(bytes)}
            withTimeout(30_000){suspendCancellableCoroutine<Unit>{continuation->
                val media=MediaPlayer();player=media
                media.setOnCompletionListener{if(continuation.isActive)continuation.resume(Unit)}
                media.setOnErrorListener{_,_,_->if(continuation.isActive)continuation.resumeWithException(IllegalStateException("Playback failed"));true}
                media.setDataSource(file.path);media.setOnPreparedListener{if(continuation.isActive)it.start()};media.prepareAsync()
            }}
            status=words("Local playback finished","Reproducción local terminada","پخش محلی پایان یافت")
        }finally{runCatching{player?.stop()};player?.release();file.delete();withContext(NonCancellable+Dispatchers.Main.immediate){runtime.endExclusiveCapture(this@PersonalMemoActivity)}}
    }}
    private fun directory()=File(cacheDir,"voice-memos").apply{check(exists() || mkdirs())}
    private fun cleanup(){directory().listFiles()?.filter{System.currentTimeMillis()-it.lastModified()>86_400_000}?.forEach{it.delete()}}
    private fun share(){task{
        val bytes=wav?.copyOf() ?: return@task;cleanup()
        val file=File.createTempFile("personal-memo-",".wav",directory());var handedOff=false
        try{
            withContext(Dispatchers.IO){file.writeBytes(bytes)}
            val uri=FileProvider.getUriForFile(this@PersonalMemoActivity,"$packageName.camera-files",file)
            val send=Intent(Intent.ACTION_SEND).setType("audio/wav").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            send.clipData=android.content.ClipData.newUri(contentResolver,"User-approved personal memo",uri)
            startActivity(Intent.createChooser(send,words("Choose app and recipient; send manually","Elige app y destinatario; envía manualmente","برنامه و گیرنده را انتخاب و دستی ارسال کنید")));handedOff=true
        }finally{bytes.fill(0);if(!handedOff)file.delete()}
    }}
    private fun clear(){wav?.fill(0);wav=null;available=false}
    override fun onStop(){operation?.cancel();clear();super.onStop()}
    override fun onDestroy(){saveCopy?.fill(0);saveCopy=null;super.onDestroy()}
}
