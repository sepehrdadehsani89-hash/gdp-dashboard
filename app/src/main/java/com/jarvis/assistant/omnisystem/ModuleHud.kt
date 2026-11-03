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
import com.jarvis.assistant.hud.ClockDisplay
import com.jarvis.assistant.ui.*
import java.util.Locale

@Composable
fun ModuleTabs(selected: HudModule, select: (HudModule)->Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=12.dp), horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        HudModule.entries.forEach { module -> FilterChip(selected==module, {select(module)}, label={Text(if(module==HudModule.ASSISTANT)"Core" else module.name.lowercase().replaceFirstChar(Char::uppercaseChar))}) }
    }
}

/** All displayed values come from the process-owned HUD state or an actual tool response.
 * Merely selecting a panel never records audio, starts a scan, or authorizes a transfer. */
@Composable
fun ModuleHud(state: JarvisUiState, actions: JarvisUiActions) {
    val h=state.hud; val t=h.telemetry; val l=state.settings.language
    val locale=Locale.forLanguageTag(l.tag)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("J.A.R.V.I.S. · ${state.module.name}",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(ClockDisplay.time(t.epochMillis,t.timezone,locale,h.settings.clockFormat,t.system24Hour))
            Text(h.weather.reading?.let { "${it.place} · ${temperature(it.temperatureC,h.settings.fahrenheit)}" } ?: l.choose("Weather unavailable","Clima no disponible","هوا در دسترس نیست"))
        }
        Text(ClockDisplay.date(t.epochMillis,t.timezone,locale),style=MaterialTheme.typography.bodySmall)
        HudCore(state,Modifier.fillMaxWidth().height(210.dp),actions)
        if(state.transcript.isNotBlank())Text("YOU · ${state.transcript}")
        if(state.liveReply.isNotBlank())Text("JARVIS · ${state.liveReply}")
        else if(state.moduleResponse.isNotBlank())Text("JARVIS · ${state.moduleResponse}")
        when(state.module) {
            HudModule.WEATHER -> HudPanel("Atmospheric intelligence",Modifier.fillMaxWidth()) {
                WeatherAtmosphere(state)
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
                Button(onClick=actions.onWeatherRefresh,enabled=!h.weather.loading){Text("Refresh configured city")}
                OutlinedButton(onClick=actions.onApproximateWeather,enabled=!h.weather.loading){Text("Use approximate location · sends to Open-Meteo")}
            }
            HudModule.BATTERY -> HudPanel("Battery diagnostics",Modifier.fillMaxWidth()) {
                Text(t.battery.percent?.let{"$it%"} ?: "Unavailable",style=MaterialTheme.typography.headlineLarge)
                Text("${if(t.battery.charging)"Charging" else if(t.battery.full)"Full" else "Not charging"} · ${t.battery.source}")
                Text("Android health classification: ${t.battery.health}")
                Text("Temperature: ${t.battery.temperatureC?.let{"$it °C"} ?: "Unavailable"}")
                Text("Power saver: ${t.powerSaver}\nCapacity-health percentage is not measured.")
                Button(onClick={actions.onSend("show my battery health")}){Text("Read voltage, current & charge counter")}
            }
            HudModule.WIFI -> HudPanel("Wi-Fi intelligence",Modifier.fillMaxWidth()) {
                Text("Transport: ${t.network}\nAndroid validated internet: ${t.internetValidated}")
                Button(onClick={actions.onSend("what is my Wi-Fi")}){Text("Read SSID, signal & radio link rate")}
                OutlinedButton(onClick=actions.onOpenDeviceControls){Text("Permissions & Android connection controls")}
                Text("Radio link rate is not measured internet throughput. SSID may be redacted by Android.")
            }
            HudModule.BLUETOOTH -> HudPanel("Bluetooth radar",Modifier.fillMaxWidth()) {
                Text("Device markers require actual discovery results. Selecting this module does not start a scan.")
                Button(onClick={actions.onSend("show my Bluetooth devices")}){Text("Query paired devices & profiles")}
                OutlinedButton(onClick=actions.onOpenDeviceControls){Text("Discover · nicknames · approved BLE battery read")}
                Text("Discovery, pairing and opening settings do not prove a connection. Unsupported battery readings stay unavailable.")
            }
            HudModule.NETWORK -> HudPanel("Network diagnostics",Modifier.fillMaxWidth()) {
                Text("Active transport: ${t.network}\nAndroid internet validation: ${t.internetValidated}")
                Text("A real test transfers about 5 MiB plus HTTP overhead with Cloudflare. Mobile data charges may apply. No test starts merely by opening this panel.")
                Button(onClick=actions.onOpenDeviceControls){Text("Review and start speed test")}
                Text("Download, upload and HTTP timing results appear only after the consented transfer. Packet loss is not measured.")
            }
            HudModule.SYSTEM -> HudPanel("System telemetry",Modifier.fillMaxWidth()) {
                Text("Network: ${t.network}\nInternet validated: ${t.internetValidated}\nProvider: ${state.settings.provider}\nMicrophone: ${if(state.audio.recording)"Recording" else "Inactive"}\nStage: ${state.neuralStage}\n${state.brainStatus}")
                Text("Available app-volume storage: ${t.storageAvailableBytes?.let{"${it/(1024*1024)} MiB"} ?: "Unavailable"}\nAndroid thermal status: ${when(t.thermalStatus){0->"None";1->"Light";2->"Moderate";3->"Severe";4->"Critical";5->"Emergency";6->"Shutdown";else->"Unavailable"}}")
                Text("No CPU utilization or unavailable hardware statistics are inferred.")
            }
            HudModule.LOCATION -> HudPanel("Location intelligence",Modifier.fillMaxWidth()) {
                Text("No location history is recorded by this panel. Location requires permission and an actual fix; no decorative map represents your position.")
                Button(onClick={actions.onSend("where am I")}){Text("Request current location")}
                OutlinedButton(onClick={actions.onOpenPermission("location")}){Text("Allow approximate location")}
            }
            HudModule.CLOCK -> HudPanel("Clock and alarms",Modifier.fillMaxWidth()) {
                ClockDial(t.epochMillis,t.timezone)
                Text("Device timezone: ${t.timezone}. Say the requested date and time. A Clock request requires review and does not prove an alarm was saved.")
                Button(onClick={actions.onSend("set a timer for 5 minutes")}){Text("Review a five-minute timer")}
            }
            HudModule.MUSIC -> HudPanel("Music recognition",Modifier.fillMaxWidth()) {
                Text("Unavailable: no licensed fingerprint provider is configured. No audio has been recorded or uploaded by this panel; no song match is fabricated.")
            }
            HudModule.CAMERA -> HudPanel("Camera vision",Modifier.fillMaxWidth()) {
                Text("Open a visible front-camera preview. Still capture is explicit and temporary; nothing is uploaded automatically.")
                Button(onClick=actions.onOpenCamera){Text("Open front camera preview")}
                Button(onClick=actions.onPickPhotos){Text("Choose an image explicitly")}
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
