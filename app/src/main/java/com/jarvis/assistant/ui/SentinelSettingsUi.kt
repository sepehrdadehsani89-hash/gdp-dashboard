package com.jarvis.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.core.choose
import com.jarvis.assistant.sentinel.*
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun SentinelSettingsUi(state:JarvisUiState,actions:JarvisUiActions,onBack:()->Unit){
    val s=state.hud.settings;val v=s.sentinel;val l=state.settings.language
    val save:(SentinelSettings)->Unit={actions.onSaveHud(s.copy(sentinel=it))}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        TextButton(onClick={actions.onInteraction(SentinelSound.PANEL_CLOSE);onBack()}){Text(l.choose("Back to app settings","Volver a ajustes","بازگشت به تنظیمات برنامه"))}
        Text("SENTINEL 3.0",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.headlineMedium)
        Text(l.choose("A living wallpaper. Your launcher stays yours.","Un fondo vivo. Conserva tu lanzador.","پس‌زمینهٔ زنده با حفظ لانچر شما."),style=MaterialTheme.typography.bodyLarge)
        HudPanel(l.choose("Wallpaper","Fondo de pantalla","پس‌زمینه"),Modifier.fillMaxWidth()){
            Text(l.choose("Android always asks you to choose and apply the wallpaper. Previewing it never enables the microphone.","Android siempre pide elegir y aplicar el fondo. La vista previa nunca activa el micrófono.","انتخاب و اعمال پس‌زمینه با خود اندروید است؛ پیش‌نمایش میکروفن را فعال نمی‌کند."),fontSize=13.sp)
            Button(onClick=actions.onPreviewWallpaper,modifier=Modifier.fillMaxWidth()){Text(l.choose("Preview / apply Jarvis wallpaper","Ver / aplicar fondo Jarvis","پیش‌نمایش / انتخاب پس‌زمینهٔ جارویس"))}
            Text("Normal icons and navigation remain accessible. Lock-screen selection depends on Android and your phone. Automatic background opening may need you to tap Jarvis's notification.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Toggle("Clock on wallpaper",v.wallpaperClock){save(v.copy(wallpaperClock=it))}
            Toggle("Battery on wallpaper",v.wallpaperBattery){save(v.copy(wallpaperBattery=it))}
            Toggle("Weather on wallpaper",v.wallpaperWeather){save(v.copy(wallpaperWeather=it))}
            Text("Weather uses your configured city/cache; enabling this option permits the normal weather refresh while the wallpaper is visible. It never requests location by itself.",fontSize=12.sp)
            Toggle("Charging energy effect",v.chargingEffects){save(v.copy(chargingEffects=it))}
            Toggle("Cinematic dormant style",v.cinematicWallpaper){save(v.copy(cinematicWallpaper=it))}
            Toggle("Return to wallpaper after voice timeout",v.returnOnTimeout){save(v.copy(returnOnTimeout=it))}
            Text("Automatic return applies to a wake-opened assistant when you have not started interacting with its controls. Manual app use stays open.",fontSize=12.sp)
        }
        HudPanel("Visual design",Modifier.fillMaxWidth()){
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                listOf("cyan","blue","purple").forEach{color->FilterChip(s.accent==color,{actions.onSaveHud(s.copy(accent=color))},label={Text(color)})}
            }
            SettingSlider("Reactor size",v.reactorScale,.65f..1.1f){save(v.copy(reactorScale=it))}
            SettingSlider("Animation speed",v.animationSpeed,0f..2f){save(v.copy(animationSpeed=it))}
            SettingSlider("Particle density",v.particleDensity){save(v.copy(particleDensity=it))}
            SettingSlider("Wallpaper brightness",v.brightness,.15f..1f){save(v.copy(brightness=it))}
            SettingSlider("Ambient glow",v.ambientGlow){save(v.copy(ambientGlow=it))}
            Text("HUD detail")
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("minimal","balanced","cinematic").forEach{detail->FilterChip(v.detail==detail,{save(v.copy(detail=detail))},label={Text(detail)})}}
            Text("Performance profile")
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("cinematic","balanced","saver").forEach{profile->FilterChip(s.animationProfile==profile,{actions.onSaveHud(s.copy(animationProfile=profile))},label={Text(if(profile=="saver")"Eco" else profile)})}}
            Toggle("Reduced motion",s.reducedMotion){actions.onSaveHud(s.copy(reducedMotion=it))}
            Text("Dormant rendering is capped at 3–6 FPS, active at 20–30 FPS. Eco, low battery, Android power saver and reduced motion lower or suspend animation. Hidden/off-screen surfaces stop drawing. These are caps, not device benchmarks.",fontSize=12.sp)
        }
        HudPanel("Original sound design",Modifier.fillMaxWidth()){
            Toggle("Cinematic sound effects",s.soundEffects){actions.onSaveHud(s.copy(soundEffects=it))}
            SettingSlider("Effects volume",v.effectsVolume){save(v.copy(effectsVolume=it))}
            SettingSlider("Activation volume",v.activationVolume){save(v.copy(activationVolume=it))}
            Text("Reply volume is controlled independently in Voice settings. All three respect Android volume, silent mode and Do Not Disturb.",fontSize=12.sp)
            Text("Activation sound")
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("chime","soft","deep").forEach{sound->FilterChip(v.activationSound==sound,{save(v.copy(activationSound=sound))},label={Text(sound)})}}
            Text("Shutdown sound")
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("sweep","soft","silent").forEach{sound->FilterChip(v.shutdownSound==sound,{save(v.copy(shutdownSound=sound))},label={Text(sound)})}}
            Text("26 original synthesized cues. Silent/DND/calls, speech and command capture take priority. Effects can be reduced or skipped so they do not cover your voice.",fontSize=12.sp)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                listOf("Wake" to SentinelSound.WAKE,"Power up" to SentinelSound.REACTOR,"Ready" to SentinelSound.READY,"Success" to SentinelSound.SUCCESS,"Codex" to SentinelSound.CODEX,"Sleep" to SentinelSound.SLEEP).forEach{(label,event)->
                    OutlinedButton(onClick={actions.onInteraction(event)},enabled=s.soundEffects){Text("Preview $label")}
                }
            }
        }
        HudPanel("Conversation",Modifier.fillMaxWidth()){
            Text(l.choose("Say Hey Jarvis once. Active speech extends the listening session. After each completed spoken reply, an adaptive follow-up session opens without another wake phrase. Silence returns to wake-word-only mode.","Di Oye Jarvis una vez. La actividad de voz amplía la escucha. Después de cada respuesta hablada, puedes continuar sin repetir la frase. El silencio vuelve al modo de activación.","یک بار بگویید هی جارویس؛ ۱۰ ثانیه برای دستور فرصت دارید. پس از پایان هر پاسخ صوتی، ۱۰ ثانیه بدون تکرار عبارت فرصت پرسش بعدی دارید. با سکوت، فقط عبارت بیدارباش شنیده می‌شود."),fontSize=14.sp)
            Text("Wake-word detection still uses the user-enabled visible microphone service. The 10-second limit controls command acceptance, not the length of an accepted AI answer.",fontSize=12.sp)
            Toggle("Concise AI responses",v.conciseReplies){save(v.copy(conciseReplies=it))}
            Text("Language models, installed voice selection, pitch, speech rate, reply volume, providers, permissions and privacy remain in app settings.",fontSize=12.sp)
            Button(onClick={actions.onStartWake()}){Text("Enable wake listening")}
            OutlinedButton(onClick=actions.onStopWake){Text("Disable wake listening")}
            Text("Selected wake phrase: ${l.wakePhrase}",color=MaterialTheme.colorScheme.primary)
            Text("Screen-lock and Codex authentication rules still apply. Saying Codex alone never proves identity.",fontSize=12.sp)
        }
    }
}
@Composable private fun Toggle(label:String,value:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f));Switch(value,onChange)}}
@Composable private fun SettingSlider(label:String,value:Float,range:ClosedFloatingPointRange<Float> = 0f..1f,onSave:(Float)->Unit){
    var draft by remember(value){mutableFloatStateOf(value)}
    Text("$label · ${"%.2f".format(draft)}")
    Slider(draft,{draft=it},valueRange=range,onValueChangeFinished={onSave(draft)})
}
