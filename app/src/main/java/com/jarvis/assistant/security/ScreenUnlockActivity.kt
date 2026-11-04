package com.jarvis.assistant.security

import android.app.KeyguardManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.choose
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Notification-tap destination: public status only, no pending private-tool continuation. */
class ScreenUnlockActivity:ComponentActivity(){
    private var stage by mutableStateOf(SecurityStage.AUTHENTICATION_REQUIRED)
    private var message by mutableStateOf("")
    private var inFlight by mutableStateOf(false)
    private var generation=0L
    private var validRequest=false
    private var homeGeneration:Long?=null
    private var homeExpiresAt=0L
    private var audioGeneration:Long?=null
    private val runtime get()=(application as JarvisApplication).runtime
    private val language get()=(application as JarvisApplication).runtime.store.settings.value.language
    private fun words(en:String,es:String,fa:String)=language.choose(en,es,fa)
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val ticket=ScreenUnlockRequests.take(intent.getStringExtra("request_id"))
        validRequest=ticket!=null
        homeGeneration=ticket?.homeGeneration;homeExpiresAt=(ticket?.issuedAt ?: 0)+120_000
        if(homeGeneration!=null && homeGeneration!=runtime.currentCommandSerial()){finish();return}
        if(!validRequest || !ScreenSecurityPreferences(this).enabled){finish();return}
        setShowWhenLocked(true);setTurnScreenOn(true)
        message=words("Device authentication required. Android controls unlocking.","Autenticación requerida. Android controla el desbloqueo.","احراز هویت لازم است. بازکردن قفل در کنترل اندروید است.")
        getSystemService(android.app.NotificationManager::class.java).cancel(ScreenSecurityController.NOTIFICATION)
        setContent{CompositionLocalProvider(LocalLayoutDirection provides if(language==AppLanguage.FA)LayoutDirection.Rtl else LayoutDirection.Ltr){MaterialTheme(colorScheme=darkColorScheme()){
            Surface(Modifier.fillMaxSize()){Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
                Text(words("JARVIS · ANDROID SECURITY","JARVIS · SEGURIDAD ANDROID","جارویس · امنیت اندروید"),style=MaterialTheme.typography.headlineMedium)
                if(ScreenSecurityPreferences(this@ScreenUnlockActivity).publicReactor)SecurityReactor(stage,com.jarvis.assistant.hud.HudPreferences(this@ScreenUnlockActivity).read().reducedMotion)
                Text(message)
                Text(words("Jarvis does not read, store or enter your PIN, password or pattern.","Jarvis no lee, guarda ni introduce PIN, contraseña o patrón.","جارویس پین، رمز یا الگو را نمی‌خواند، ذخیره نمی‌کند و وارد نمی‌کند."))
                Button(enabled=!inFlight && stage!=SecurityStage.VERIFIED,onClick=::requestAndroid){Text(words("Authenticate with Android","Autenticar con Android","احراز هویت با اندروید"))}
                TextButton(onClick={generation++;inFlight=false;finish()}){Text(words("Close","Cerrar","بستن"))}
            }}
        }}}
    }
    override fun onPostResume(){super.onPostResume();if(validRequest && generation==0L)requestAndroid()}
    private fun requestAndroid(){
        if(inFlight || isFinishing || !ScreenSecurityPreferences(this).enabled)return
        val manager=getSystemService(KeyguardManager::class.java)
        if(!manager.isKeyguardLocked && !manager.isDeviceLocked){stage=SecurityStage.VERIFIED;message=words("Android reports that your phone is already unlocked.","Android indica que ya está desbloqueado.","اندروید گزارش می‌دهد گوشی شما هم‌اکنون باز است.");generation++;openHomeIfCurrent();return}
        val token=++generation;inFlight=true;stage=SecurityStage.AUTHENTICATION_REQUIRED
        if(runCatching{SystemCredentialPrivacy.begin(this);audioGeneration=runtime.pauseForAndroidUi();if(homeGeneration!=null)homeGeneration=audioGeneration}.isFailure){failed(token,words("Audio privacy pause unavailable. Unlock normally with Android.","Pausa de privacidad no disponible. Desbloquea normalmente.","توقف صوتی برای حریم خصوصی در دسترس نیست. با اندروید معمولی باز کنید."));return}
        lifecycleScope.launch{delay(120_000);if(token==generation && inFlight){generation++;inFlight=false;stage=SecurityStage.CANCELLED;message=words("The request expired. Unlock normally or tap to request a fresh Android challenge.","La solicitud caducó. Desbloquea normalmente o pide un nuevo reto Android.","مهلت درخواست پایان یافت. معمولی باز کنید یا یک احراز هویت تازه از اندروید بخواهید.")}}
        try{manager.requestDismissKeyguard(this,object:KeyguardManager.KeyguardDismissCallback(){
            override fun onDismissSucceeded(){if(token!=generation || isFinishing)return
                inFlight=false;finishAudioPrivacy()
                if(!manager.isDeviceLocked && !manager.isKeyguardLocked){openHomeIfCurrent();stage=SecurityStage.VERIFIED;message=words("Android confirms the screen is unlocked. No private action was executed.","Android confirma el desbloqueo. No se ejecutó ninguna acción privada.","اندروید باز بودن صفحه را تأیید می‌کند. اقدام خصوصی اجرا نشد.")}
                else failed(token,words("Android did not report an unlocked screen. Retry only when ready.","Android no confirmó desbloqueo. Reintenta cuando quieras.","اندروید صفحهٔ باز را گزارش نکرد. در زمان مناسب دوباره تلاش کنید."))
            }
            override fun onDismissCancelled(){failed(token,words("Authentication cancelled. Android remains in control of the lock.","Autenticación cancelada. Android mantiene el control del bloqueo.","احراز هویت لغو شد. کنترل قفل با اندروید می‌ماند."))}
            override fun onDismissError(){failed(token,words("Android cannot dismiss keyguard here. Close this screen and unlock normally.","Android no puede desbloquear aquí. Cierra y desbloquea normalmente.","اندروید نمی‌تواند قفل را اینجا باز کند. صفحه را ببندید و معمولی باز کنید."))}
        })}catch(_:Exception){failed(token,words("Android keyguard dismissal is unavailable here.","Desbloqueo Android no disponible aquí.","درخواست بازکردن قفل اندروید اینجا در دسترس نیست."))}
    }
    private fun openHomeIfCurrent(){
        val expected=homeGeneration ?: return
        if(expected!=runtime.currentCommandSerial() || android.os.SystemClock.elapsedRealtime()>homeExpiresAt || getSystemService(KeyguardManager::class.java).isDeviceLocked)return
        homeGeneration=null
        runtime.selectModule(com.jarvis.assistant.omnisystem.HudModule.ASSISTANT)
        runCatching{startActivity(android.content.Intent(this,com.jarvis.assistant.MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("rebirth_home",true))}.onSuccess{runtime.resumeWakeAfterNavigation(expected);finish()}.onFailure{message="Android did not allow Home to open. Open Jarvis manually."}
    }
    private fun finishAudioPrivacy(){runCatching{SystemCredentialPrivacy.end(this)};audioGeneration?.let(runtime::finishAndroidUi);audioGeneration=null}
    private fun failed(token:Long,text:String){if(token!=generation || isFinishing)return;inFlight=false;finishAudioPrivacy();stage=SecurityStage.CANCELLED;message=text}
    override fun onDestroy(){generation++;validRequest=false;super.onDestroy()}
}
