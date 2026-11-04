package com.jarvis.assistant.omnisystem

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.core.choose
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.hud.ClockDisplay
import com.jarvis.assistant.ui.*
import java.util.Locale

@Composable
fun ModuleTabs(selected: HudModule, language:AppLanguage, select: (HudModule)->Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=12.dp), horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        HudModule.entries.forEach { module -> FilterChip(selected==module, {select(module)}, label={Text(module.label(language))}) }
    }
}

/** All displayed values come from the process-owned HUD state or an actual tool response.
 * Merely selecting a panel never records audio, starts a scan, or authorizes a transfer. */
@Composable
fun ModuleHud(state: JarvisUiState, actions: JarvisUiActions) {
    val h=state.hud; val t=h.telemetry; val l=state.settings.language
    val locale=Locale.forLanguageTag(l.tag)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("J.A.R.V.I.S. · ${state.module.label(l)}",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
        state.speakerProfileName?.let{name->Text(l.choose("Possible voice profile: $name · not authentication","Perfil de voz posible: $name · no autentica","نمایهٔ صوتی احتمالی: $name · نه احراز هویت"))}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(ClockDisplay.time(t.epochMillis,t.timezone,locale,h.settings.clockFormat,t.system24Hour))
            Text(h.weather.reading?.let { "${it.place} · ${temperature(it.temperatureC,h.settings.fahrenheit)}" } ?: l.choose("Weather unavailable","Clima no disponible","هوا در دسترس نیست"))
        }
        Text(ClockDisplay.date(t.epochMillis,t.timezone,locale),style=MaterialTheme.typography.bodySmall)
        HudCore(state,Modifier.fillMaxWidth().height(210.dp),actions)
        if(state.transcript.isNotBlank())Text(confirmedCaption(l.choose("YOU","TÚ","شما")+" · ${state.transcript}",state.confirmedEntity))
        if(state.liveReply.isNotBlank())Text("JARVIS · ${state.liveReply}")
        else if(state.moduleResponse.isNotBlank())Text("JARVIS · ${state.moduleResponse}")
        when(state.module) {
            HudModule.WEATHER -> HudPanel(l.choose("Atmospheric intelligence","Inteligencia atmosférica","اطلاعات هواشناسی"),Modifier.fillMaxWidth()) {
                WeatherAtmosphere(state)
                com.jarvis.assistant.titan.WeatherGlobe(h)
                val w=h.weather.reading
                if(w==null)Text(h.weather.error ?: "No weather received. Configure a city in Settings or explicitly request approximate location.")
                else {
                    Text("${w.place} · ${temperature(w.temperatureC,h.settings.fahrenheit)}",style=MaterialTheme.typography.headlineMedium)
                    Text("Feels like ${temperature(w.feelsLikeC,h.settings.fahrenheit)} · Humidity ${w.humidity?.let{"$it%"} ?: "Unavailable"}")
                    Text("Wind ${w.windKmh?.let{"$it km/h"} ?: "Unavailable"} · Rain probability ${w.precipitationProbability?.let{"$it%"} ?: "Unavailable"}")
                    Text("Sunrise ${w.sunrise ?: "Unavailable"} · Sunset ${w.sunset ?: "Unavailable"}")
                    Text("Open-Meteo · observation ${w.observationTime} (${w.sourceTimezone})\nRetrieved ${java.time.Instant.ofEpochMilli(w.fetchedAt)}${if(h.weather.stale)" · STALE" else ""}")
                    w.hourly.forEach { Text("${it.time} · ${temperature(it.temperatureC,h.settings.fahrenheit)} · ${it.rain?.let{"$it% rain"} ?: "Rain unavailable"}") }
                    w.daily.forEach { Text("${it.date} · ${temperature(it.low,h.settings.fahrenheit)} / ${temperature(it.high,h.settings.fahrenheit)} · ${it.rain?.let{"$it% rain"} ?: "Rain unavailable"}") }
                }
                if(h.weather.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
                if(h.weather.error!=null)Text(h.weather.error)
                Button(onClick=actions.onWeatherRefresh,enabled=!h.weather.loading){Text(l.choose("Refresh configured city","Actualizar ciudad configurada","به‌روزرسانی شهر تنظیم‌شده"))}
                OutlinedButton(onClick=actions.onApproximateWeather,enabled=!h.weather.loading){Text(l.choose("Use approximate location · sends to Open-Meteo","Usar ubicación aproximada · se envía a Open-Meteo","استفاده از مکان تقریبی · ارسال به Open-Meteo"))}
            }
            HudModule.BATTERY -> HudPanel(l.choose("Battery diagnostics","Diagnóstico de batería","بررسی باتری"),Modifier.fillMaxWidth()) {
                Text(t.battery.percent?.let{"$it%"} ?: "Unavailable",style=MaterialTheme.typography.headlineLarge)
                Text("${if(t.battery.charging)"Charging" else if(t.battery.full)"Full" else "Not charging"} · ${t.battery.source}")
                Text("Android health classification: ${t.battery.health}")
                Text("Temperature: ${t.battery.temperatureC?.let{"$it °C"} ?: "Unavailable"}")
                Text("Power saver: ${t.powerSaver}\nCapacity-health percentage is not measured.")
                Button(onClick={actions.onSend("show my battery health")}){Text(l.choose("Read voltage, current & charge counter","Leer voltaje, corriente y carga","خواندن ولتاژ، جریان و شمارندهٔ شارژ"))}
            }
            HudModule.WIFI -> HudPanel(l.choose("Wi-Fi intelligence","Información Wi-Fi","اطلاعات وای‌فای"),Modifier.fillMaxWidth()) {
                Text("Transport: ${t.network}\nAndroid validated internet: ${t.internetValidated}")
                Button(onClick={actions.onSend("what is my Wi-Fi")}){Text(l.choose("Read SSID, signal & radio link rate","Leer SSID, señal y velocidad de enlace","خواندن نام، سیگنال و نرخ پیوند رادیویی"))}
                OutlinedButton(onClick=actions.onOpenDeviceControls){Text(l.choose("Permissions & Android connection controls","Permisos y conexión de Android","مجوزها و تنظیمات اتصال اندروید"))}
                Text(l.choose("Radio link rate is not measured internet throughput. SSID may be redacted by Android.","La velocidad del enlace no mide el rendimiento de Internet. Android puede ocultar el SSID.","نرخ پیوند، سرعت اندازه‌گیری‌شدهٔ اینترنت نیست. اندروید ممکن است نام شبکه را پنهان کند."))
            }
            HudModule.BLUETOOTH -> HudPanel(l.choose("Bluetooth radar","Radar Bluetooth","رادار بلوتوث"),Modifier.fillMaxWidth()) {
                Text(l.choose("Device markers require actual discovery results. Selecting this module does not start a scan.","Los marcadores requieren descubrimientos reales. Elegir este módulo no inicia búsquedas.","نشانگر دستگاه‌ها به نتیجهٔ واقعی جستجو نیاز دارد. انتخاب این ماژول جستجو را شروع نمی‌کند."))
                Button(onClick={actions.onSend("show my Bluetooth devices")}){Text(l.choose("Query paired devices & profiles","Consultar dispositivos vinculados y perfiles","بررسی دستگاه‌های جفت‌شده و نمایه‌ها"))}
                OutlinedButton(onClick=actions.onOpenDeviceControls){Text(l.choose("Discover · nicknames · approved BLE battery read","Descubrir · alias · batería BLE autorizada","جستجو · نام مستعار · خواندن باتری BLE با تأیید"))}
                Text(l.choose("Discovery, pairing and opening settings do not prove a connection. Unsupported battery readings stay unavailable.","Descubrir, vincular o abrir ajustes no prueba conexión. La batería no compatible permanece no disponible.","جستجو، جفت‌سازی و باز کردن تنظیمات، اتصال را ثابت نمی‌کند. باتری پشتیبانی‌نشده در دسترس نمی‌ماند."))
            }
            HudModule.NETWORK -> HudPanel(l.choose("Network diagnostics","Diagnóstico de red","بررسی شبکه"),Modifier.fillMaxWidth()) {
                Text("Active transport: ${t.network}\nAndroid internet validation: ${t.internetValidated}")
                Text(l.choose("A real test transfers about 5 MiB plus HTTP overhead with Cloudflare. Mobile data charges may apply. No test starts merely by opening this panel.","Una prueba real transfiere unos 5 MiB más sobrecarga HTTP con Cloudflare. Puede tener cargos móviles. Abrir este panel no inicia la prueba.","سنجش واقعی حدود ۵ مگابایت همراه سربار HTTP با Cloudflare جابه‌جا می‌کند. ممکن است دادهٔ همراه هزینه داشته باشد. باز کردن پنل سنجش را شروع نمی‌کند."))
                Button(onClick=actions.onOpenDeviceControls){Text(l.choose("Review and start speed test","Revisar e iniciar prueba de velocidad","تأیید و شروع سنجش سرعت"))}
                Text(l.choose("Download, upload and HTTP timing results appear only after the consented transfer. Packet loss is not measured.","Los resultados de descarga, subida y tiempo HTTP solo aparecen tras la transferencia autorizada. No se mide pérdida de paquetes.","نتیجهٔ دانلود، آپلود و زمان HTTP پس از انتقال تأییدشده ظاهر می‌شود. اتلاف بسته اندازه‌گیری نمی‌شود."))
            }
            HudModule.SYSTEM -> HudPanel(l.choose("System telemetry","Telemetría del sistema","داده‌های واقعی سیستم"),Modifier.fillMaxWidth()) {
                Text("Network: ${t.network}\nInternet validated: ${t.internetValidated}\nProvider: ${state.settings.provider}\nMicrophone: ${if(state.audio.recording)"Recording" else "Inactive"}\nStage: ${state.neuralStage}\n${state.brainStatus}")
                Text("Available app-volume storage: ${t.storageAvailableBytes?.let{"${it/(1024*1024)} MiB"} ?: "Unavailable"}\nAndroid thermal status: ${when(t.thermalStatus){0->"None";1->"Light";2->"Moderate";3->"Severe";4->"Critical";5->"Emergency";6->"Shutdown";else->"Unavailable"}}")
                Text(l.choose("No CPU utilization or unavailable hardware statistics are inferred.","No se infieren uso de CPU ni estadísticas no disponibles.","مصرف پردازنده یا داده‌های سخت‌افزاری ناموجود حدس زده نمی‌شود."))
            }
            HudModule.LOCATION -> HudPanel(l.choose("Location intelligence","Información de ubicación","اطلاعات مکان"),Modifier.fillMaxWidth()) {
                Text(l.choose("No location history is recorded by this panel. Location requires permission and an actual fix; no decorative map represents your position.","Este panel no guarda historial. La ubicación requiere permiso y una medición real.","این پنل تاریخچهٔ مکان ذخیره نمی‌کند. مکان به مجوز و اندازه‌گیری واقعی نیاز دارد."))
                Button(onClick={actions.onSend("where am I")}){Text(l.choose("Request current location","Solicitar ubicación actual","درخواست مکان فعلی"))}
                OutlinedButton(onClick={actions.onOpenPermission("location")}){Text(l.choose("Allow approximate location","Permitir ubicación aproximada","مجوز مکان تقریبی"))}
                OutlinedButton(onClick=actions.onOpenDeviceControls){Text(l.choose("Map, address & saved destinations","Mapa, dirección y destinos guardados","نقشه، نشانی و مقصدهای ذخیره‌شده"))}
            }
            HudModule.CLOCK -> HudPanel(l.choose("Clock and alarms","Reloj y alarmas","ساعت و زنگ"),Modifier.fillMaxWidth()) {
                ClockDial(t.epochMillis,t.timezone)
                Text("Device timezone: ${t.timezone}. Say the requested date and time. A Clock request requires review and does not prove an alarm was saved.")
                Button(onClick={actions.onSend("set a timer for 5 minutes")}){Text(l.choose("Review a five-minute timer","Revisar temporizador de cinco minutos","تأیید تایمر پنج‌دقیقه‌ای"))}
                OutlinedButton(onClick=actions.onOpenDeviceControls){Text(l.choose("Own timers & Android alarm review","Temporizadores propios y revisión de alarmas","تایمرهای داخلی و تأیید زنگ اندروید"))}
            }
            HudModule.MUSIC -> HudPanel(l.choose("Music recognition","Reconocimiento musical","شناسایی موسیقی"),Modifier.fillMaxWidth()) {
                Text(l.choose("AudD recognition uses an explicitly approved ten-second sample and your private service token. Selecting this panel records nothing. Spotify authorization is optional.","AudD usa una muestra autorizada de diez segundos y tu token privado. Abrir este panel no graba nada. Spotify es opcional.","شناسایی AudD از نمونهٔ ده‌ثانیه‌ای تأییدشده و توکن خصوصی شما استفاده می‌کند. انتخاب این پنل چیزی ضبط نمی‌کند. مجوز اسپاتیفای اختیاری است."))
                Button(onClick=actions.onOpenDeviceControls){Text(l.choose("Music recognition & Spotify","Reconocimiento musical y Spotify","شناسایی موسیقی و اسپاتیفای"))}
            }
            HudModule.CAMERA -> HudPanel(l.choose("Camera vision","Visión de cámara","بینایی دوربین"),Modifier.fillMaxWidth()) {
                Text(l.choose("Open a visible front-camera preview. Still capture is explicit and temporary; nothing is uploaded automatically.","Abre una vista visible de la cámara frontal. La captura es explícita y temporal; nada se sube automáticamente.","پیش‌نمایش آشکار دوربین جلو را باز کنید. ثبت عکس صریح و موقت است؛ چیزی خودکار بارگذاری نمی‌شود."))
                Button(onClick=actions.onOpenCamera){Text(l.choose("Open front camera preview","Abrir vista de cámara frontal","باز کردن پیش‌نمایش دوربین جلو"))}
                Button(onClick=actions.onPickPhotos){Text(l.choose("Choose an image explicitly","Elegir una imagen","انتخاب صریح تصویر"))}
                OutlinedButton(onClick=actions.onOpenVision){Text(l.choose("Describe or compare selected images","Describir o comparar imágenes seleccionadas","توضیح یا مقایسهٔ تصاویر انتخابی"))}
            }
            HudModule.ASSISTANT -> Unit
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick=actions.onMic){Text(l.choose("Microphone","Micrófono","میکروفون"))}
            OutlinedButton(onClick=actions.onStop){Text(l.choose("Stop","Detener","توقف"))}
            TextButton(onClick={actions.onModule(HudModule.ASSISTANT)}){Text(l.choose("Back to conversation","Volver a conversación","بازگشت به گفتگو"))}
        }
    }
}
private fun temperature(c:Double?,fahrenheit:Boolean):String = c?.let { String.format(Locale.ROOT,"%.1f°%s",if(fahrenheit)it*9/5+32 else it,if(fahrenheit)"F" else "C") } ?: "Unavailable"

fun HudModule.label(l:AppLanguage):String=when(this){
    HudModule.ASSISTANT->l.choose("Core","Núcleo","هسته")
    HudModule.WEATHER->l.choose("Weather","Clima","هوا")
    HudModule.LOCATION->l.choose("Location","Ubicación","مکان")
    HudModule.BATTERY->l.choose("Battery","Batería","باتری")
    HudModule.WIFI->l.choose("Wi-Fi","Wi-Fi","وای‌فای")
    HudModule.BLUETOOTH->l.choose("Bluetooth","Bluetooth","بلوتوث")
    HudModule.NETWORK->l.choose("Network","Red","شبکه")
    HudModule.MUSIC->l.choose("Music","Música","موسیقی")
    HudModule.CAMERA->l.choose("Camera","Cámara","دوربین")
    HudModule.CLOCK->l.choose("Clock","Reloj","ساعت")
    HudModule.SYSTEM->l.choose("System","Sistema","سیستم")
}
