package com.jarvis.assistant.security

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.omnisystem.PrivateModuleActivity

/** Single official Android authentication; setup metadata never authorizes a future unlock. */
class SecuritySettingsActivity:PrivateModuleActivity(){
    private val preferences by lazy{ScreenSecurityPreferences(this)}
    private var revision by mutableIntStateOf(0)
    private val notifications=registerForActivityResult(ActivityResultContracts.RequestPermission()){revision++}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);if(isFinishing)return
        screen(words("JARVIS · ANDROID SCREEN SECURITY","JARVIS · SEGURIDAD ANDROID","Security")){
            @Suppress("UNUSED_VARIABLE")val observed=revision
            preferences.reconcile()
            val observedHud by runtime.hud.state.collectAsStateWithLifecycle()
            val secure=preferences.secure()
            Text(words("Android screen lock","Bloqueo Android","Android")+": "+if(secure)"CONFIGURED" else "NOT CONFIGURED")
            Text(words("Only one Android-owned authentication enables this request setting. No custom PIN, password, pattern or triple setup. Every future unlock remains controlled by Android; voice and stored setup state are never authentication.","Una sola autenticación oficial activa esta opción. Sin PIN, contraseña, patrón ni triple configuración propios. Android controla cada desbloqueo futuro; voz y configuración no autentican.","Android controls authentication."))
            SecurityReactor(if(preferences.enabled)SecurityStage.VERIFIED else SecurityStage.NOT_CONFIGURED,observedHud.settings.reducedMotion || !observedHud.telemetry.systemMotion,0)
            OutlinedButton(onClick={startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))}){Text(words("Configure device security in Android","Configurar seguridad en Android","Android settings"))}
            Button(enabled=secure,onClick={authorize{
                check(preferences.secure());preferences.complete(VerificationMode.ANDROID_SINGLE_CHALLENGE);revision++
                status=words("Unlock requests enabled. Future unlocks still require Android.","Solicitudes activadas. Cada desbloqueo sigue requiriendo Android.","Enabled")
            }}){Text(words("Enable requests · authenticate once with Android","Activar solicitudes · autenticar una vez con Android","Enable"))}
            Row{Switch(preferences.enabled,{value->authorize{if(value)preferences.complete(VerificationMode.ANDROID_SINGLE_CHALLENGE) else preferences.enable(false);revision++}},enabled=secure);Text(words("Voice-initiated unlock requests","Solicitudes de desbloqueo por voz","Requests"))}
            OutlinedButton(enabled=preferences.enabled,onClick={status=ScreenSecurityController(this@SecuritySettingsActivity).requestUnlock(false)}){Text(words("Test Android unlock request","Probar solicitud Android","Test"))}
            Row{Switch(preferences.publicReactor,{v->authorize{preferences.reactor(v);revision++}});Text(words("Public-only unlock reactor","Reactor público de desbloqueo","Public reactor"))}
            Row{Switch(preferences.requireSensitive,{v->authorize{preferences.sensitive(v);revision++}});Text(words("Authenticate protected operations","Autenticar acciones protegidas","Protected actions"))}
            OutlinedButton(onClick={if(android.os.Build.VERSION.SDK_INT>=33)notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS) else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName))}){Text(words("Notification permission / settings","Permiso y ajustes de notificaciones","Notifications"))}
            val admin=ComponentName(this@SecuritySettingsActivity,JarvisLockAdminReceiver::class.java)
            val policy=getSystemService(DevicePolicyManager::class.java)
            Text(words("Optional force-lock-only device administration","Administración opcional solo para bloquear","Lock-only administration")+": "+policy.isAdminActive(admin))
            OutlinedButton(onClick={startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,admin).putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,words("Allow Jarvis to lock the screen only. No password reset or wipe permission.","Permite solo bloquear la pantalla. Sin restablecer contraseñas ni borrar datos.","Lock only")))}){Text(words("Review Android lock-only permission","Revisar permiso Android solo para bloquear","Review permission"))}
            if(policy.isAdminActive(admin))OutlinedButton(onClick={authorize{policy.removeActiveAdmin(admin);revision++}}){Text(words("Remove administrator permission","Quitar permiso de administrador","Remove"))}
            OutlinedButton(onClick={authorize{preferences.resetVerification();ScreenUnlockRequests.clear();revision++}}){Text(words("Disable and reset request setup","Desactivar y restablecer solicitudes","Reset"))}
            if(SystemCredentialPrivacy.pending(this@SecuritySettingsActivity))OutlinedButton(onClick={if(hasWindowFocus() && !locked()){SystemCredentialPrivacy.end(this@SecuritySettingsActivity);runtime.stopEverything();revision++}}){Text(words("Clear interrupted privacy pause · keep listening stopped","Quitar pausa interrumpida · mantener escucha detenida","Clear pause"))}
        }
    }
}
