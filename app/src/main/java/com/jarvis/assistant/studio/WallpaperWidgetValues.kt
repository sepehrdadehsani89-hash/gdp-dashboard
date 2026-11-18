package com.jarvis.assistant.studio

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.core.content.ContextCompat
import com.jarvis.assistant.AssistantRuntime
import com.jarvis.assistant.hud.ClockDisplay
import java.util.Locale

/** Actual local samples and explicit verified reads. Missing sources remain unavailable. No cloud polling. */
object WallpaperWidgetValues {
    fun sample(context:Context,runtime:AssistantRuntime,profile:StudioProfile,privateAllowed:Boolean):Map<WidgetSource,String> {
        val wanted=profile.layers.filter{it.visible && it.kind.startsWith("WIDGET:")}.map{WidgetSource.valueOf(it.kind.removePrefix("WIDGET:"))}.toSet()
        val state=runtime.state.value;val hud=state.hud;val t=hud.telemetry;val weather=hud.weather.reading;val locale=Locale.forLanguageTag(state.settings.language.tag)
        fun temp(v:Double?)=v?.let{String.format(Locale.ROOT,"%.1f°%s",if(hud.settings.fahrenheit)it*9/5+32 else it,if(hud.settings.fahrenheit)"F"else"C")} ?: "Unavailable"
        val audio=context.getSystemService(AudioManager::class.java)
        return wanted.associateWith{source->runCatching{when(source){
            WidgetSource.CLOCK->ClockDisplay.time(t.epochMillis,t.timezone,locale,hud.settings.clockFormat,t.system24Hour)
            WidgetSource.DATE->ClockDisplay.date(t.epochMillis,t.timezone,locale)
            WidgetSource.CITY->weather?.place ?: "City · configure weather"
            WidgetSource.WEATHER->weather?.let{it.place+" · "+temp(it.temperatureC)+if(hud.weather.stale)" · STALE"else""} ?: "Weather · unavailable"
            WidgetSource.TEMPERATURE->temp(weather?.temperatureC)
            WidgetSource.FORECAST->weather?.daily?.firstOrNull()?.let{"Forecast · ${it.date}\n${temp(it.low)} — ${temp(it.high)}\nRain ${it.rain?.let{x->"$x%"} ?: "unavailable"}"} ?: "Forecast · unavailable"
            WidgetSource.SUNRISE->"Sunrise · "+(weather?.sunrise ?: "unavailable")
            WidgetSource.SUNSET->"Sunset · "+(weather?.sunset ?: "unavailable")
            WidgetSource.BATTERY->"Battery · "+(t.battery.percent?.let{"$it%"} ?: "unavailable")
            WidgetSource.CHARGING->if(t.battery.full)"Battery full" else if(t.battery.charging)"Charging · ${t.battery.source}" else "Battery · not charging"
            WidgetSource.NETWORK->"Network · ${t.network}\nInternet validated: ${t.internetValidated}"
            WidgetSource.BLUETOOTH->if(ContextCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)"Bluetooth · permission required"else "Bluetooth · "+when(context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled){true->"on";false->"off";else->"unavailable"}
            WidgetSource.AI->"Selected AI · ${state.settings.provider}\n${state.settings.model}\n${if(state.selectedBrainVerified)"Inference verified in this app session"else"Inference unverified"}"
            WidgetSource.CONNECTION->"Google accounts · ${runtime.googleConnections.state.value.accounts.size}\nAccount metadata is not API verification"
            WidgetSource.GMAIL_UNREAD->if(!privateAllowed)"Gmail · private data hidden"else runtime.gmailWidgetText()
            WidgetSource.NOTIFICATIONS->if(!privateAllowed)"Notifications · private data hidden"else com.jarvis.assistant.tools.JarvisNotificationService.read(context).let{if(it.status==com.jarvis.assistant.notifications.NotificationReadStatus.AVAILABLE)"Active notifications · ${it.previews.size} shown (bounded)\n"+it.previews.take(2).joinToString("\n")else "Notifications · ${it.status}"}
            WidgetSource.NEXT_REMINDER->if(!privateAllowed)"Reminder · private data hidden"else runtime.reminders.store.list().filter{it.status==com.jarvis.assistant.reminders.ReminderStatus.SCHEDULED}.minByOrNull{it.at}?.let{"Next reminder · ${java.util.Date(it.at)}\n${it.title}"} ?: "No scheduled Jarvis reminder"
            WidgetSource.CALENDAR->if(!privateAllowed)"Calendar · private data hidden"else runtime.googleReadWidgetText(com.jarvis.assistant.google.GoogleProduct.CALENDAR)
            WidgetSource.TASKS->if(!privateAllowed)"Tasks · private data hidden"else runtime.googleReadWidgetText(com.jarvis.assistant.google.GoogleProduct.TASKS)
            WidgetSource.COMPUTER->com.jarvis.assistant.companion.DesktopConnectionProofPolicy.text(runtime.companion.connectionProof.value,runtime.companion.paired(),System.currentTimeMillis())
            WidgetSource.MUSIC->if(audio?.isMusicActive==true)"Audio playback active\nTrack metadata unavailable"else"No active audio playback reported"
            WidgetSource.VOLUME->audio?.let{"Media volume · ${it.getStreamVolume(AudioManager.STREAM_MUSIC)} / ${it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}"} ?: "Volume · unavailable"
            WidgetSource.MICROPHONE->"Capture · ${if(state.audio.recording)"active"else"inactive"}\nWake service · ${if(state.wakeEnabled)"running"else"stopped"}"
            WidgetSource.SILENT->if(runtime.systemModes.state.value.silent)"Silent Mode · on"else"Silent Mode · off"
            WidgetSource.JARVIS->"Jarvis · ${state.phase}"
            WidgetSource.STORAGE->t.storageAvailableBytes?.let{"Available storage · ${it/1_048_576} MiB"} ?: "Storage · unavailable"
            WidgetSource.TELEMETRY->"RAM available · ${t.systemAvailableBytes?.let{"${it/1_048_576} MiB"} ?: "unavailable"}\nApp PSS · ${t.appPssKiB?.let{"$it KiB"} ?: "unavailable"}"
        }}.getOrElse{"$source · unavailable"}}
    }
}
