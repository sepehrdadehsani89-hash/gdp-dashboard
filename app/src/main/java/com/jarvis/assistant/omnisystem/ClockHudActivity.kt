package com.jarvis.assistant.omnisystem

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.jarvis.assistant.tools.UtilityIntentParser
import kotlinx.coroutines.delay
import java.util.Locale

class ClockHudActivity:PrivateModuleActivity(){
    private var amount by mutableStateOf("5")
    private var multiplier by mutableIntStateOf(60)
    private var boot by mutableIntStateOf(-1)
    private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        status=if(granted)words("Notifications allowed. Review the timer to start it.","Notificaciones permitidas. Revisa el temporizador para iniciarlo.","اعلان‌ها مجاز شدند. برای شروع، تایمر را تأیید کنید.") else words("Notifications denied. No timer was started.","Notificaciones denegadas. No se inició un temporizador.","اعلان‌ها رد شدند. تایمری شروع نشد.")
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);if(isFinishing)return
        val request=intent.getStringExtra("request").orEmpty().take(8000)
        UtilityIntentParser.timerSeconds(request)?.let{s->multiplier=if(s%3600==0)3600 else if(s%60==0)60 else 1;amount=(s/multiplier).toString()}
        screen(words("JARVIS · CLOCK & OWN TIMERS","JARVIS · RELOJ Y TEMPORIZADORES","جارویس · ساعت و تایمرهای داخلی")){
            val timers by runtime.timers.state.collectAsStateWithLifecycle()
            val healthy by runtime.timers.readable.collectAsStateWithLifecycle()
            var now by remember{mutableLongStateOf(SystemClock.elapsedRealtime())}
            LaunchedEffect(Unit){lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){while(true){now=SystemClock.elapsedRealtime();delay(1000)}}}
            Text(words("These countdowns belong to Jarvis. They do not claim to show an external Clock app's timers or saved alarms.","Estas cuentas pertenecen a Jarvis; no muestran ni verifican temporizadores o alarmas de otra app.","این شمارش‌ها متعلق به جارویس هستند؛ وضعیت تایمر یا زنگ برنامهٔ ساعت دیگری را نشان نمی‌دهند."))
            Text(words("Android timezone","Zona horaria de Android","منطقهٔ زمانی اندروید")+": "+java.time.ZoneId.systemDefault().id)
            if(!healthy)Text(words("Stored timers are unavailable. Existing data has not been overwritten.","Los temporizadores guardados no están disponibles. No se sobrescribieron datos.","تایمرهای ذخیره‌شده در دسترس نیستند. داده‌ها بازنویسی نشده‌اند."))
            if(Build.VERSION.SDK_INT>=31)OutlinedButton(onClick={open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:$packageName")))}){Text(words("Android exact-alarm permission","Permiso de alarmas exactas de Android","مجوز زنگ دقیق اندروید"))}
            OutlinedButton(onClick={if(Build.VERSION.SDK_INT>=33)notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else openNotificationSettings()}){Text(words("Allow timer notifications","Permitir notificaciones de temporizador","مجوز اعلان تایمر"))}
            OutlinedButton(onClick=::openNotificationSettings){Text(words("Android timer sound & notification settings","Sonido y notificaciones de Android","تنظیمات صدا و اعلان تایمر اندروید"))}
            OutlinedTextField(amount,{amount=UtilityIntentParser.digits(it).filter(Char::isDigit).take(5)},label={Text(words("Duration amount","Duración","مدت"))},enabled=!working,modifier=Modifier.fillMaxWidth())
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                listOf(1 to words("Seconds","Segundos","ثانیه"),60 to words("Minutes","Minutos","دقیقه"),3600 to words("Hours","Horas","ساعت")).forEach{(factor,label)->FilterChip(multiplier==factor,{multiplier=factor},label={Text(label)})}
            }
            val seconds=amount.toLongOrNull()?.times(multiplier)?.takeIf{it in 1..86400}?.toInt()
            Button(enabled=seconds!=null && healthy && !working,onClick={val duration=seconds ?: return@Button
                if(!runtime.timers.exactAllowed()){status=words("Allow exact alarms in Android first. Nothing was scheduled.","Permite alarmas exactas primero. No se programó nada.","ابتدا زنگ دقیق را در اندروید مجاز کنید. چیزی زمان‌بندی نشد.");return@Button}
                if(!runtime.timers.alertsAllowed()){status=words("Allow timer notifications first. Nothing was scheduled.","Permite notificaciones primero. No se programó nada.","ابتدا اعلان تایمر را مجاز کنید. چیزی زمان‌بندی نشد.");return@Button}
                AlertDialog.Builder(this@ClockHudActivity).setTitle(words("Start Jarvis timer: $duration seconds?","¿Iniciar temporizador de $duration segundos?","تایمر $duration ثانیه‌ای شروع شود؟"))
                    .setMessage(words("Android receives an exact elapsed-time alarm request. Restarting the phone interrupts this timer. Sound, Do Not Disturb and notification delivery remain controlled by Android. No microphone or background screen starts.","Android recibe una alarma exacta por tiempo transcurrido. Reiniciar interrumpe el temporizador. Android controla sonido, No molestar y entrega. No se inicia micrófono ni pantalla en segundo plano.","درخواست زنگ دقیق بر پایهٔ زمان سپری‌شده به اندروید داده می‌شود. راه‌اندازی دوباره تایمر را قطع می‌کند. صدا، مزاحم‌نشوید و تحویل اعلان در کنترل اندروید است. میکروفون یا صفحهٔ پس‌زمینه شروع نمی‌شود."))
                    .setNegativeButton(words("Cancel","Cancelar","لغو"),null).setPositiveButton(words("Start","Iniciar","شروع")){_,_->task{
                        runtime.cancelAction();runtime.timers.start(duration)
                        status=words("Android accepted this Jarvis timer schedule.","Android aceptó programar este temporizador.","اندروید زمان‌بندی این تایمر جارویس را پذیرفت.")
                    }}.show()
            }){Text(words("Review and start own timer","Revisar e iniciar temporizador propio","تأیید و شروع تایمر داخلی"))}
            timers.forEach{t->
                val remaining=OwnedTimerPolicy.remaining(t,now,boot)
                Text("Jarvis · ${t.seconds} s")
                if(remaining!=null){
                    val total=(remaining+999)/1000
                    Text(String.format(Locale.ROOT,"%02d:%02d:%02d",total/3600,total/60%60,total%60),style=MaterialTheme.typography.headlineMedium)
                    CircularProgressIndicator(progress={OwnedTimerPolicy.fraction(t,now,boot) ?: 0f},modifier=Modifier.size(80.dp))
                    if(remaining==0L)Text(words("Countdown elapsed; alert delivery not yet observed.","Cuenta finalizada; alerta aún no observada.","شمارش پایان یافت؛ تحویل اعلان هنوز مشاهده نشده است."))
                } else Text(when{
                    boot<0->words("Android boot identity unavailable; timer state unknown","Identidad de arranque no disponible; estado desconocido","شناسهٔ راه‌اندازی در دسترس نیست؛ وضعیت تایمر نامشخص است")
                    t.boot!=boot || now<t.created->words("Interrupted by device restart","Interrumpido por reinicio","قطع‌شده بر اثر راه‌اندازی دوباره")
                    t.status=="INTERRUPTED"->words("Exact-alarm permission changed. Recreate this timer after restoring permission.","Cambió el permiso de alarmas exactas. Crea el temporizador de nuevo tras restaurarlo.","مجوز زنگ دقیق تغییر کرد. پس از مجاز کردن، تایمر را دوباره بسازید.")
                    t.status=="ALERT_REQUESTED"->words("Notification requested; this does not prove it was seen or heard.","Notificación solicitada; no confirma que se vio u oyó.","اعلان درخواست شد؛ این به معنی دیده یا شنیده شدن نیست.")
                    t.status=="ALERT_BLOCKED"->words("Alert blocked by Android notification settings","Alerta bloqueada por ajustes de Android","اعلان توسط تنظیمات اندروید مسدود شد")
                    else->words("Not active","No activo","غیرفعال")
                })
                OutlinedButton(onClick={task{runtime.timers.cancel(t.id);status=words("Own timer dismissed/cancelled","Temporizador propio descartado/cancelado","تایمر داخلی بسته یا لغو شد")}}){Text(words("Cancel / dismiss this timer","Cancelar / descartar este temporizador","لغو یا بستن این تایمر"))}
            }
            HorizontalDivider()
            Text(words("External Android Clock alarms","Alarmas del Reloj de Android","زنگ‌های برنامهٔ ساعت اندروید"),style=MaterialTheme.typography.titleLarge)
            Text(words("Use Assistant to review a requested date/time. Opening a Clock intent is awaiting your interaction, not proof an alarm was saved. A one-time date is never silently converted into a weekly repeat.","Usa Asistente para revisar fecha y hora. Abrir Reloj espera tu interacción; no prueba que se guardó una alarma. Una fecha única no se convierte en repetición semanal.","برای تأیید تاریخ و ساعت از دستیار استفاده کنید. باز شدن ساعت، منتظر تعامل شماست و ثبت زنگ را ثابت نمی‌کند. تاریخ یک‌باره پنهانی به تکرار هفتگی تبدیل نمی‌شود."))
            if(UtilityIntentParser.isAlarm(request))OutlinedButton(onClick={runtime.submit(request);finish()}){Text(words("Review requested alarm in Assistant","Revisar alarma solicitada en Asistente","تأیید زنگ در دستیار"))}
        }
    }
    private fun openNotificationSettings(){open(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName).putExtra(Settings.EXTRA_CHANNEL_ID,OwnedTimers.CHANNEL))}
    private fun open(intent:Intent){runCatching{startActivity(intent)}.onFailure{status=words("Android settings unavailable","Ajustes de Android no disponibles","تنظیمات اندروید در دسترس نیست")}}
    override fun onResume(){super.onResume();if(!isFinishing){boot=runtime.timers.boot();runCatching{runtime.timers.reconcile()}.onFailure{status=words("Stored timer state unavailable","Estado guardado no disponible","وضعیت ذخیره‌شدهٔ تایمر در دسترس نیست")}}}
}
