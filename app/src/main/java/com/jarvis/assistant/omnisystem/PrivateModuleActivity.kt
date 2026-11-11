package com.jarvis.assistant.omnisystem

import android.app.Activity
import android.app.KeyguardManager
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.choose
import kotlinx.coroutines.*

/** Foreground private module, no overlays, microphone auto-start or lock-screen disclosure. */
abstract class PrivateModuleActivity:ComponentActivity() {
    protected val runtime get()=(application as JarvisApplication).runtime
    protected val vault get()=runtime.features
    protected var operation:Job?=null
    protected var status by mutableStateOf("")
    protected var working by mutableStateOf(false)
    private var approvedAction:(()->Unit)?=null
    private var approvalAt=0L
    private val credentialOwner=Any()
    private val authentication=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val action=approvedAction;approvedAction=null
        val current=runtime.captureIsCurrent(credentialOwner)
        runCatching{com.jarvis.assistant.security.SystemCredentialPrivacy.end(this)}
        runtime.endExclusiveCapture(credentialOwner)
        if(current && result.resultCode==Activity.RESULT_OK && !locked() && SystemClock.elapsedRealtime()-approvalAt in 0..120_000)action?.let(::applyAuthorized)
    }
    protected fun words(en:String,es:String,fa:String)=runtime.store.settings.value.language.choose(en,es,fa)
    protected fun locked()=getSystemService(KeyguardManager::class.java)?.isDeviceLocked!=false
    private fun applyAuthorized(action:()->Unit){try{action()}catch(_:Exception){status=words("Change could not be saved. Check private storage and retry.","No se guardó el cambio. Revisa el almacenamiento privado.","تغییر ذخیره نشد. فضای خصوصی را بررسی و دوباره تلاش کنید.")}}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if(locked()){finish();return}
        WindowCompat.getInsetsController(window,window.decorView).apply{
            systemBarsBehavior=androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    protected fun authorize(action:()->Unit){
        if(locked()){finish();return}
        val manager=getSystemService(KeyguardManager::class.java)
        if(!manager.isDeviceSecure){applyAuthorized(action);return}
        val intent=manager.createConfirmDeviceCredentialIntent("Jarvis",words("Confirm this private change","Confirma este cambio privado","این تغییر خصوصی را تأیید کنید"))
        if(intent==null){status=words("Android authentication unavailable","Autenticación no disponible","احراز هویت در دسترس نیست");return}
        if(!runtime.beginExclusiveCapture(credentialOwner)){status=words("Finish the current capture before authenticating.","Termina la captura antes de autenticar.","پیش از احراز هویت، ضبط فعلی را پایان دهید.");return}
        approvedAction=action;approvalAt=SystemClock.elapsedRealtime()
        try{com.jarvis.assistant.security.SystemCredentialPrivacy.begin(this);authentication.launch(intent)}catch(_:Exception){approvedAction=null;runCatching{com.jarvis.assistant.security.SystemCredentialPrivacy.end(this)};runtime.endExclusiveCapture(credentialOwner);status=words("Android authentication unavailable","Autenticación no disponible","احراز هویت در دسترس نیست")}
    }
    protected fun task(block:suspend ()->Unit){
        if(working || locked())return
        working=true
        val initialStatus=words("Working…","Procesando…","در حال انجام…")
        status=initialStatus
        operation=lifecycleScope.launch {
            try{block();if(status==initialStatus)status=""}
            catch(e:CancellationException){status=words("Cancelled","Cancelado","لغو شد");throw e}
            catch(e:com.jarvis.assistant.companion.CompanionFailure){status="Paired computer: ${e.code}. No verified completion is claimed."}
            catch(e:ModuleServiceFailure){status=words("Service rejected the request (${e.code}). Check account access, credentials or quota.","El servicio rechazó la solicitud (${e.code}). Revisa cuenta, credenciales o cuota.","سرویس درخواست را رد کرد (${e.code}). حساب، کلید و سهمیه را بررسی کنید.")}
            catch(_:OutOfMemoryError){status=words("Android could not allocate this operation. Close the feature and retry.","Android no pudo asignar memoria. Cierra la función y reintenta.","اندروید نتوانست حافظهٔ این عملیات را تخصیص دهد. قابلیت را ببندید و دوباره امتحان کنید.")}
            catch(_:LinkageError){status=words("This native capability is unavailable in this build/device.","Esta función nativa no está disponible en esta versión o dispositivo.","این قابلیت بومی در این نسخه یا دستگاه در دسترس نیست.")}
            catch(_:Exception){status=words("Unavailable. Check permissions, configuration and connectivity, then retry.","No disponible. Revisa permisos, configuración y conexión.","در دسترس نیست. مجوزها، تنظیمات و اتصال را بررسی کنید.")}
            finally{working=false}
        }
        operation?.let{runtime.trackModuleOperation(this,it)}
    }
    protected fun screen(title:String,content:@Composable ColumnScope.()->Unit){setContent{
        val settings by runtime.store.settings.collectAsStateWithLifecycle()
        val shared by runtime.state.collectAsStateWithLifecycle()
        CompositionLocalProvider(LocalLayoutDirection provides if(settings.language==AppLanguage.FA)LayoutDirection.Rtl else LayoutDirection.Ltr){
            MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF00E5FF),background=Color.Black,surface=Color(0xFF050B14))){
                Surface(Modifier.fillMaxSize()){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                    TextButton(onClick={finish()}){Text(words("Back to Jarvis","Volver a Jarvis","بازگشت به جارویس"))}
                    Text(title,style=MaterialTheme.typography.headlineMedium)
                    val clock=remember(shared.hud.telemetry.epochMillis,settings.language,shared.hud.settings.clockFormat){
                        val t=shared.hud.telemetry
                        com.jarvis.assistant.hud.ClockDisplay.time(t.epochMillis,t.timezone,java.util.Locale.forLanguageTag(settings.language.tag),shared.hud.settings.clockFormat,t.system24Hour)
                    }
                    Text(clock+" · "+words("Weather","Clima","هوا")+": "+(shared.hud.weather.reading?.let{reading->
                        reading.place+" · "+(reading.temperatureC?.let{value->String.format(java.util.Locale.ROOT,"%.1f°%s",if(shared.hud.settings.fahrenheit)value*9/5+32 else value,if(shared.hud.settings.fahrenheit)"F" else "C")} ?: "—")
                    } ?: words("Unavailable","No disponible","در دسترس نیست")))
                    val actualPhase=when{
                        shared.phase==com.jarvis.assistant.core.AssistantPhase.SPEAKING->shared.phase
                        shared.audio.recording->com.jarvis.assistant.core.AssistantPhase.LISTENING
                        working->com.jarvis.assistant.core.AssistantPhase.THINKING
                        else->shared.phase
                    }
                    com.jarvis.assistant.ui.HudCore(shared.copy(phase=actualPhase,neuralStage=status,authorization=null),Modifier.fillMaxWidth().height(170.dp))
                    if(status.isNotBlank())Text(status)
                    if(working){LinearProgressIndicator(Modifier.fillMaxWidth());OutlinedButton(onClick={operation?.cancel()}){Text(words("Cancel","Cancelar","لغو"))}}
                    content()
                }}
            }
        }
    }}
    override fun onStart(){super.onStart();if(!isFinishing)runtime.hud.startVisible(this)}
    override fun onResume(){super.onResume();if(locked())finish()}
    override fun onPause(){operation?.cancel();super.onPause()}
    override fun onStop(){operation?.cancel();runtime.hud.stopVisible(this);super.onStop()}
    override fun onDestroy(){approvedAction=null;runtime.abandonExclusiveCapture(credentialOwner);super.onDestroy()}
}
