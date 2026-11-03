package com.jarvis.assistant.security

import android.Manifest
import android.app.*
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jarvis.assistant.MainActivity
import com.jarvis.assistant.R
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.choose
import java.util.UUID

/** One-use in-process request. Process death or a stale notification requires a fresh command. */
object ScreenUnlockRequests {
    private var request:Pair<String,Long>?=null
    @Synchronized fun issue():String=UUID.randomUUID().toString().also{request=it to SystemClock.elapsedRealtime()}
    @Synchronized fun consume(id:String?):Boolean {
        val current=request ?: return false
        if(id!=current.first)return false
        request=null
        return SystemClock.elapsedRealtime()-current.second in 0..120_000
    }
    @Synchronized fun clear(){request=null}
}
class ScreenSecurityController(private val context:Context){
    companion object {const val CHANNEL="jarvis_screen_requests_v1";const val NOTIFICATION=4104}
    val preferences=ScreenSecurityPreferences(context)
    private fun words(en:String,es:String,fa:String)=((context.applicationContext as? com.jarvis.assistant.JarvisApplication)?.runtime?.store?.settings?.value?.language ?: AppLanguage.EN).choose(en,es,fa)
    fun requestUnlock(forceNotification:Boolean=true):String{
        preferences.reconcile()
        val manager=context.getSystemService(KeyguardManager::class.java)
        val notifications=context.getSystemService(NotificationManager::class.java)
        val permission=android.os.Build.VERSION.SDK_INT<33 || androidx.core.content.ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED
        val notifyAllowed=permission && NotificationManagerCompat.from(context).areNotificationsEnabled() && notifications.getNotificationChannel(CHANNEL)?.importance!=NotificationManager.IMPORTANCE_NONE
        val foreground=!forceNotification && context is Activity && context.hasWindowFocus()
        val plan=UnlockPresentationPolicy.choose(preferences.enabled,manager.isKeyguardLocked,manager.isDeviceLocked,foreground,notifyAllowed)
        if(plan==UnlockPresentation.DISABLED)return words("Configure and enable voice-initiated unlock in Screen Security & Voice Unlock first.","Primero configura y activa las solicitudes por voz en Seguridad de pantalla.","ابتدا درخواست صوتی را در امنیت صفحه تنظیم و فعال کنید.")
        if(plan==UnlockPresentation.ALREADY_UNLOCKED)return words("Your phone is already unlocked.","Tu teléfono ya está desbloqueado.","گوشی شما هم‌اکنون باز است.")
        val manual=words("Android authentication requires opening Jarvis. Enable its notification permission for a lock-screen request.","Abre Jarvis para autenticar. Activa notificaciones para solicitudes con pantalla bloqueada.","برای احراز هویت جارویس را باز کنید. برای درخواست از صفحهٔ قفل، مجوز اعلان را فعال کنید.")
        if(plan==UnlockPresentation.OPEN_MANUALLY)return manual
        val request=ScreenUnlockRequests.issue()
        val intent=Intent(context,ScreenUnlockActivity::class.java).putExtra("request_id",request).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if(plan==UnlockPresentation.FOREGROUND_REQUEST){
            try{context.startActivity(intent);return words("Android authentication requested.","Autenticación de Android solicitada.","احراز هویت اندروید درخواست شد.")}catch(_:Exception){}
        }
        if(!notifyAllowed){ScreenUnlockRequests.clear();return manual}
        notifications.createNotificationChannel(NotificationChannel(CHANNEL,words("Jarvis screen authentication","Autenticación de pantalla Jarvis","احراز هویت صفحهٔ جارویس"),NotificationManager.IMPORTANCE_DEFAULT))
        val pending=PendingIntent.getActivity(context,NOTIFICATION,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return try{
            if(android.os.Build.VERSION.SDK_INT>=33 && androidx.core.content.ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){ScreenUnlockRequests.clear();return manual}
            notifications.notify(NOTIFICATION,NotificationCompat.Builder(context,CHANNEL).setSmallIcon(R.drawable.ic_jarvis)
                .setContentTitle(words("Jarvis · Android authentication required","Jarvis · Autenticación Android requerida","جارویس · احراز هویت اندروید لازم است"))
                .setContentText(words("Tap to request Android unlocking. No credential is stored.","Toca para solicitar desbloqueo Android. No se guarda la credencial.","برای درخواست بازکردن قفل با اندروید لمس کنید. رمز ذخیره نمی‌شود."))
                .setContentIntent(pending).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setTimeoutAfter(120_000).build())
            words("Device authentication required. Tap the Jarvis notification to continue with Android.","Autenticación requerida. Toca la notificación Jarvis para continuar con Android.","احراز هویت لازم است. برای ادامه با اندروید، اعلان جارویس را لمس کنید.")
        }catch(_:SecurityException){ScreenUnlockRequests.clear();words("Android blocked the notification. Open Jarvis to request authentication.","Android bloqueó la notificación. Abre Jarvis para autenticar.","اندروید اعلان را مسدود کرد. برای درخواست احراز هویت، جارویس را باز کنید.")}
    }
    fun lockRequest():String {
        val policy=context.getSystemService(DevicePolicyManager::class.java)
        val admin=ComponentName(context,JarvisLockAdminReceiver::class.java)
        if(!policy.isAdminActive(admin))return words("Lock-only Android administrator permission is not enabled. Review it in Screen Security & Voice Unlock, or use the phone's power button.","No está habilitado el permiso solo para bloquear. Revísalo en Seguridad de pantalla o usa el botón de encendido.","مجوز مدیر دستگاه فقط برای قفل‌کردن فعال نیست. آن را در امنیت صفحه بررسی کنید یا از دکمهٔ روشن/خاموش گوشی استفاده کنید.")
        return try{policy.lockNow();if(context.getSystemService(KeyguardManager::class.java).isDeviceLocked)words("Android confirms the device is locked.","Android confirma el dispositivo bloqueado.","اندروید قفل بودن دستگاه را تأیید می‌کند.") else words("Android accepted the lock request; locked state has not yet been observed.","Android aceptó la solicitud; el bloqueo aún no se observó.","اندروید درخواست قفل را پذیرفت؛ وضعیت قفل هنوز مشاهده نشده است.")}catch(_:SecurityException){words("Android rejected the lock request. Review its device-administrator settings.","Android rechazó el bloqueo. Revisa los ajustes de administrador.","اندروید درخواست قفل را رد کرد. تنظیمات مدیر دستگاه را بررسی کنید.")}
    }
}
/** Declares force-lock only. No password reset, wipe-data, camera or credential policy. */
class JarvisLockAdminReceiver:DeviceAdminReceiver()
