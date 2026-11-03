package com.jarvis.assistant.security

import android.app.Activity
import android.app.AlertDialog
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import com.jarvis.assistant.sentinel.SentinelSound

class SecuritySettingsActivity:PrivateModuleActivity(){
    private val verification=SecurityVerification(SystemClock::elapsedRealtime)
    private val preferences by lazy{ScreenSecurityPreferences(this)}
    private var revision by mutableIntStateOf(0)
    private var pendingId:String?=null
    private var expiry:kotlinx.coroutines.Job?=null
    private var compatibility by mutableStateOf(false)
    private val credentialOwner=Any()
    private val notifications=registerForActivityResult(ActivityResultContracts.RequestPermission()){status=words("Notification permission result received. No unlock was performed.","Permiso de notificaciones recibido. No se desbloqueó nada.","نتیجهٔ مجوز اعلان دریافت شد. قفلی باز نشد.")}
    private val credential=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        val id=pendingId;pendingId=null;expiry?.cancel()
        val current=runtime.captureIsCurrent(credentialOwner)
        runCatching{SystemCredentialPrivacy.end(this)};runtime.endExclusiveCapture(credentialOwner)
        if(id!=null){
            val accepted=verification.systemResult(id,current && result.resultCode==Activity.RESULT_OK,preferences.secure())
            if(!accepted){preferences.resetVerification();status=words("Verification not completed. Start again when ready.","Verificación no completada. Puedes empezar de nuevo.","تأیید کامل نشد. در زمان مناسب دوباره شروع کنید.");runtime.effects.play(SentinelSound.AUTH_DENIED)}
            else finishedStage()
            revision++
        }
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);if(isFinishing)return
        screen(words("SCREEN SECURITY & VOICE UNLOCK","SEGURIDAD Y DESBLOQUEO POR VOZ","امنیت صفحه و درخواست بازکردن قفل با صدا")){
            revision
            val observedHud by runtime.hud.state.collectAsStateWithLifecycle()
            val observedSettings by runtime.store.settings.collectAsStateWithLifecycle()
            val secure=preferences.secure();val enabled=preferences.enabled
            if(SystemCredentialPrivacy.pending(this@SecuritySettingsActivity) && pendingId==null){
                Text(words("An earlier Android authentication was interrupted. Finish or cancel that system prompt before resetting this pause.","Una autenticación anterior se interrumpió. Termina o cancela el aviso Android antes de restablecer esta pausa.","احراز هویت قبلی اندروید قطع شده است. پیش از بازنشانی این توقف، آن پنجرهٔ سیستم را تمام یا لغو کنید."))
                OutlinedButton(onClick={if(hasWindowFocus() && !locked()){abort();SystemCredentialPrivacy.end(this@SecuritySettingsActivity);runtime.stopEverything();revision++}}){Text(words("Clear interrupted setup · keep listening stopped","Borrar configuración interrumpida · mantener escucha detenida","پاک کردن تنظیم قطع‌شده · شنود خاموش می‌ماند"))}
            }
            Text(words("JARVIS SCREEN CONTROL","CONTROL DE PANTALLA JARVIS","کنترل صفحهٔ جارویس"),style=MaterialTheme.typography.titleLarge)
            Text(words("Secure Android lock","Bloqueo seguro de Android","قفل امن اندروید")+": "+if(secure)words("CONFIGURED","CONFIGURADO","تنظیم شده") else words("NOT CONFIGURED","NO CONFIGURADO","تنظیم نشده"))
            Text(words("Android reports only whether security is configured. Jarvis cannot read your PIN, password or pattern. Each future unlock remains controlled by Android.","Android solo indica si hay bloqueo seguro. Jarvis no lee PIN, contraseña ni patrón. Cada desbloqueo futuro sigue bajo control de Android.","اندروید فقط وجود قفل امن را گزارش می‌دهد. جارویس رمز، پین یا الگو را نمی‌خواند. هر بار بازکردن قفل در کنترل اندروید می‌ماند."))
            val state=if(verification.state==SecurityStage.VERIFYING)verification.state else if(!secure)SecurityStage.UNAVAILABLE else if(preferences.verifiedAt>0)SecurityStage.VERIFIED else verification.state
            SecurityReactor(state,observedHud.settings.reducedMotion,verification.completed)
            Text(words("Setup state","Estado de configuración","وضعیت تنظیم")+": "+state.label(observedSettings.language))
            if(preferences.verifiedAt>0)Text(words("Setup last verified","Última verificación de configuración","آخرین تأیید تنظیم")+": "+java.time.Instant.ofEpochMilli(preferences.verifiedAt))
            OutlinedButton(onClick={open(Intent(Settings.ACTION_SECURITY_SETTINGS))}){Text(words("Configure device security in Android","Configurar seguridad en Android","تنظیم امنیت دستگاه در اندروید"))}
            if(verification.state!=SecurityStage.VERIFYING){
                Row{Checkbox(compatibility,{compatibility=it});Text(words("Compatibility mode: one Android challenge + two setup confirmations","Compatibilidad: un reto Android + dos confirmaciones de configuración","حالت سازگاری: یک احراز هویت اندروید + دو تأیید تنظیم"))}
                Button(enabled=secure,onClick={preferences.resetVerification();pendingId=null
                    verification.begin(preferences.secure(),if(compatibility)VerificationMode.ANDROID_AND_TWO_CONFIRMATIONS else VerificationMode.THREE_ANDROID_CHALLENGES)
                    status="";revision++
                }){Text(words("Configure voice unlock · start verification","Configurar desbloqueo por voz · verificar","تنظیم درخواست بازکردن قفل با صدا · شروع تأیید"))}
            }else{
                Text(words("STEP ${verification.completed+1} OF 3","PASO ${verification.completed+1} DE 3","گام ${verification.completed+1} از ۳"),style=MaterialTheme.typography.headlineSmall)
                if(verification.needsAndroid()){
                    Text(words("Android validates the credential. Jarvis receives only the result.","Android valida la credencial. Jarvis recibe solo el resultado.","اندروید اعتبار رمز را بررسی می‌کند. جارویس فقط نتیجه را دریافت می‌کند."))
                    Button(enabled=pendingId==null,onClick=::verifyWithAndroid){Text(words("Verify with Android","Verificar con Android","تأیید با اندروید"))}
                }else{
                    Text(words(if(verification.completed==1)"Setup confirmation: I understand that saying the command never replaces Android authentication." else "Setup confirmation: I understand Android may require a notification tap before showing authentication.",if(verification.completed==1)"Confirmación de configuración: decir el comando no sustituye la autenticación de Android." else "Confirmación de configuración: Android puede requerir tocar una notificación para autenticar.",if(verification.completed==1)"تأیید تنظیم: گفتن فرمان جایگزین احراز هویت اندروید نیست." else "تأیید تنظیم: اندروید ممکن است پیش از احراز هویت به لمس اعلان نیاز داشته باشد."))
                    Button(onClick={if(verification.confirmSetup(preferences.secure()))finishedStage();revision++}){Text(words("Confirm setup understanding · not another authentication","Confirmar comprensión · no es otra autenticación","تأیید آگاهی از تنظیم · احراز هویت دیگری نیست"))}
                }
                TextButton(onClick={abort()}){Text(words("Cancel verification","Cancelar verificación","لغو تأیید"))}
            }
            Row{Switch(enabled,{value->authorize{preferences.enable(value);revision++}},enabled=secure && preferences.verifiedAt>0 && verification.state!=SecurityStage.VERIFYING);Text(words("Enable voice-initiated unlock requests","Permitir solicitudes de desbloqueo por voz","فعال‌سازی درخواست بازکردن قفل با صدا"))}
            Text("Hey Jarvis, unlock my phone")
            OutlinedButton(onClick={if(android.os.Build.VERSION.SDK_INT>=33)notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS) else open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName))}){Text(words("Android notification permission / settings","Permiso y ajustes de notificaciones Android","مجوز و تنظیمات اعلان اندروید"))}
            Button(enabled=enabled,onClick={ScreenSecurityController(this@SecuritySettingsActivity).requestUnlock(forceNotification=false);status=words("Unlock workflow requested; Android decides the result.","Flujo solicitado; Android decide el resultado.","روند درخواست شد؛ نتیجه را اندروید تعیین می‌کند.")}){Text(words("Test unlock workflow","Probar desbloqueo","آزمایش روند بازکردن قفل"))}
            Row{Switch(preferences.requireSensitive,{value->authorize{preferences.sensitive(value);revision++}});Text(words("Require authentication for protected operations","Exigir autenticación para acciones protegidas","احراز هویت برای کارهای حفاظت‌شده"))}
            Text(words("Existing Codex protection still applies when this additional requirement is off.","La protección Codex existente sigue activa según su configuración.","با خاموش شدن این گزینه نیز حفاظت فعلی کدکس طبق تنظیم خود برقرار است."))
            Row{Switch(preferences.publicReactor,{value->authorize{preferences.reactor(value);revision++}});Text(words("Public reactor on unlock request screen","Reactor público al solicitar desbloqueo","نمایش رآکتور عمومی هنگام درخواست بازکردن قفل"))}
            Text(words("Private memories, contacts, transcripts, photos and locations are never shown on that locked screen.","La pantalla bloqueada no muestra recuerdos, contactos, transcripciones, fotos ni ubicaciones privadas.","یادداشت، مخاطب، متن گفتگو، عکس و مکان خصوصی در صفحهٔ قفل نمایش داده نمی‌شود."))
            HorizontalDivider()
            Text(words("Optional Android device-admin locking","Bloqueo opcional con administrador Android","قفل اختیاری با مدیر دستگاه اندروید"))
            val admin=ComponentName(this@SecuritySettingsActivity,JarvisLockAdminReceiver::class.java)
            val policy=getSystemService(DevicePolicyManager::class.java)
            Text(words("Force-lock authorization","Autorización de bloqueo","مجوز قفل‌کردن")+": "+policy.isAdminActive(admin))
            OutlinedButton(onClick={authorize{
                val intent=Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,admin)
                    .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,words("Optional lock-only capability. Android owns approval. No password collection or reset policy.","Capacidad opcional solo para bloquear. Android pide autorización. No recoge ni restablece contraseñas.","مجوز اختیاری فقط برای قفل‌کردن است. تأیید با اندروید است. رمز جمع‌آوری یا بازنشانی نمی‌شود."))
                open(intent)
            }}){Text(words("Review Android lock-only administrator permission","Revisar permiso de administrador solo para bloquear","بررسی مجوز مدیر دستگاه فقط برای قفل‌کردن"))}
            if(policy.isAdminActive(admin))OutlinedButton(onClick={authorize{policy.removeActiveAdmin(admin);revision++}}){Text(words("Remove Jarvis device-admin permission","Retirar permiso de administrador Jarvis","حذف مجوز مدیر دستگاه جارویس"))}
            OutlinedButton(onClick={AlertDialog.Builder(this@SecuritySettingsActivity).setTitle(words("Reset Jarvis unlock setup?","¿Reiniciar configuración Jarvis?","تنظیم درخواست بازکردن قفل جارویس بازنشانی شود؟")).setMessage(words("This disables Jarvis requests only. It does not change your Android credential.","Solo desactiva solicitudes Jarvis. No cambia la credencial Android.","فقط درخواست‌های جارویس غیرفعال می‌شوند. رمز اندروید تغییر نمی‌کند.")).setNegativeButton(words("Cancel","Cancelar","لغو"),null).setPositiveButton(words("Reset","Restablecer","بازنشانی")){_,_->authorize{abort();preferences.resetVerification();ScreenUnlockRequests.clear();revision++}}.show()}){Text(words("Reset Jarvis unlock configuration","Restablecer desbloqueo Jarvis","بازنشانی تنظیم درخواست بازکردن قفل جارویس"))}
        }
    }
    @Suppress("DEPRECATION") private fun verifyWithAndroid(){
        val challenge=verification.challenge(preferences.secure()) ?: run{revision++;return}
        val intent=getSystemService(KeyguardManager::class.java).createConfirmDeviceCredentialIntent("Jarvis · ${challenge.stage}/3",words("Confirm directly with Android","Confirma directamente con Android","مستقیماً با اندروید تأیید کنید"))
        if(intent==null){abort();status=words("Android credential confirmation unavailable. Configure device security or retry compatibility mode.","Confirmación no disponible. Configura seguridad o prueba compatibilidad.","تأیید رمز اندروید در دسترس نیست. امنیت دستگاه را تنظیم کنید یا حالت سازگاری را امتحان کنید.");return}
        if(!runtime.beginExclusiveCapture(credentialOwner)){abort();status=words("Finish the current capture before verification.","Termina la captura antes de verificar.","پیش از تأیید، ضبط فعلی را پایان دهید.");return}
        pendingId=challenge.id
        try{SystemCredentialPrivacy.begin(this);credential.launch(intent)
            expiry?.cancel();expiry=lifecycleScope.launch{delay(120_001);if(pendingId==challenge.id){abort();status=words("Verification expired. Restart when ready.","La verificación caducó. Reinicia cuando quieras.","مهلت تأیید پایان یافت. در زمان مناسب دوباره شروع کنید.")}}
        }catch(_:Exception){runCatching{SystemCredentialPrivacy.end(this)};runtime.endExclusiveCapture(credentialOwner);abort();status=words("Android could not show authentication.","Android no pudo mostrar autenticación.","اندروید نتوانست احراز هویت را نمایش دهد.")}
    }
    private fun finishedStage(){
        if(verification.state==SecurityStage.VERIFIED){
            if(runCatching{preferences.complete(verification.mode)}.isFailure){verification.invalidate();status=words("Setup storage unavailable. Voice requests were not enabled.","No se pudo guardar. No se habilitaron solicitudes por voz.","ذخیرهٔ تنظیم در دسترس نیست. درخواست صوتی فعال نشد.");revision++;return}
            runtime.effects.play(SentinelSound.AUTH_SUCCESS)
            status=words("Security verification successful. Voice-initiated unlocking is ready to request Android authentication.","Verificación correcta. Las solicitudes por voz están listas para pedir autenticación Android.","تأیید امنیت موفق بود. درخواست صوتی برای درخواست احراز هویت اندروید آماده است.")
            runtime.speakPrivatePreview(status)
        }else status=words("Verification ${verification.completed} complete. Continue when ready.","Verificación ${verification.completed} completada. Continúa cuando quieras.","تأیید ${verification.completed} کامل شد. در زمان مناسب ادامه دهید.")
    }
    private fun abort(){pendingId=null;expiry?.cancel();verification.abort();runCatching{preferences.resetVerification()};revision++}
    private fun open(intent:Intent){runCatching{startActivity(intent)}.onFailure{status=words("Android settings unavailable.","Ajustes Android no disponibles.","تنظیمات اندروید در دسترس نیست.")}}
    override fun onResume(){super.onResume();if(!isFinishing){preferences.reconcile();if(!preferences.secure() && verification.state==SecurityStage.VERIFYING)abort();revision++}}
    override fun onDestroy(){verification.abort();pendingId=null;runtime.abandonExclusiveCapture(credentialOwner);super.onDestroy()}
}
