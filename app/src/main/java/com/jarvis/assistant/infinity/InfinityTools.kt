package com.jarvis.assistant.infinity

import android.content.Context
import com.jarvis.assistant.core.*
import com.jarvis.assistant.neural.AppMatching
import com.jarvis.assistant.neural.WebResearch
import kotlinx.coroutines.*

/** These operations accept only fresh user input, never retrieved content or model prose. */
class InfinityTools(private val context:Context){
    val connectivity=ConnectivityRepository(context)
    val preferences=InfinityPreferences(context)
    private var listed=emptyList<Accessory>()
    private var listedAt=0L
    private var selected:String?=null
    private fun fresh()=android.os.SystemClock.elapsedRealtime()-listedAt in 0..120_000
    fun clearContext(){listed=emptyList();selected=null;listedAt=0}
    suspend fun route(input:String,lang:AppLanguage,proposal:(String,String,String)->AssistantReply):AssistantReply? {
        val n=AppMatching.normalize(input).removePrefix("jarvis ")
        if(Regex("^(remember|remember that|do not|dont|don t|never|no quiero|recuerda|یادت باشد|نکن) ").containsMatchIn(n))return null
        val bluetooth=Regex("bluetooth|بلوتوث").containsMatchIn(n)
        val network=Regex("\\b(wifi|wi fi|internet|network|red)\\b|وای فای|اینترنت|شبکه").containsMatchIn(n)
        if(com.jarvis.assistant.neural.ContextPrivacyPolicy.localOnly(input) && network && Regex("speed|test|velocidad|سرعت|تست").containsMatchIn(n))return AssistantReply("An internet speed test needs an online transfer. Local-only mode did not start one.")
        val follow=n in setOf("is it connected","esta conectado","متصل است","what is its battery","whats its battery percentage","its battery","باتری آن چقدر است")
        if(network && Regex("speed test|test.*speed|internet speed|wifi speed|wi fi speed|velocidad|سرعت").containsMatchIn(n))return proposal("Open the speed-test controls? A test transfers about 5 MiB plus HTTP overhead with Cloudflare. It starts only after you press Start; mobile data may cost money.","infinity_controls","")
        if(network && !Regex("^(open|turn|enable|disable|abre|activa|desactiva) ").containsMatchIn(n))return AssistantReply(withContext(Dispatchers.IO){connectivity.wifi()})
        if(Regex("battery health|battery temperature|charging status|salud.*bateria|estado.*bateria|سلامت باتری|دمای باتری|وضعیت شارژ").containsMatchIn(n))return AssistantReply(withContext(Dispatchers.IO){connectivity.battery()})
        if(bluetooth && Regex("^(?:turn|switch|enable|disable|open|activa|desactiva|abre) ").containsMatchIn(n))return proposal("Open Android Bluetooth controls? Android manages enable/disable and pairing; a change must be made and verified there.","system_panel","bluetooth")
        if(bluetooth || follow || fresh() && Regex("^(connect|disconnect|conecta|desconecta|اتصال|قطع اتصال) ").containsMatchIn(n)) {
            if(!connectivity.bluetoothAllowed())return proposal("Bluetooth access is off. Open Infinity device controls and grant the optional Nearby devices permission?","infinity_controls","")
            val devices=try{connectivity.paired()}catch(e:CancellationException){throw e}catch(e:Exception){return AssistantReply(e.message ?: "Bluetooth unavailable.")}
            if(follow){
                val device=selected?.takeIf{fresh()}?.let{address->devices.firstOrNull{it.address==address}} ?: return AssistantReply("Which accessory? Ask for your Bluetooth devices first.")
                return AssistantReply(if(n.contains("batter") || n.contains("باتری"))"${device.name}: battery unavailable in this list. In Infinity controls you can explicitly read a paired BLE device’s Battery Service. Classic/earbud/case values are not universally exposed." else "${device.name}: ${if(device.connected)"connected according to Android's A2DP, headset or GATT profile" else "no connection reported by the queried Android profiles; other accessory profiles may be unavailable"}.")
            }
            val target=Regex("^(?:connect(?: to)?|disconnect(?: from)?|conecta(?: a)?|desconecta(?: de)?|اتصال به|قطع اتصال از) (.+)$").matchEntire(n)?.groupValues?.get(1)?.removePrefix("bluetooth ")
            if(target!=null){
                val matches=DevicePolicy.reference(target,listed.takeIf{fresh()} ?: emptyList())?.let{ref->devices.filter{it.address==ref.address}} ?: DevicePolicy.matches(target,devices,preferences.aliases())
                if(matches.isEmpty())return AssistantReply("No currently paired accessory matches that name. Open Infinity controls to discover nearby devices.")
                if(matches.size>1)return AssistantReply("Which device: ${matches.joinToString{it.name}}? Say the complete name or use the controls.")
                val d=matches.single();selected=d.address;listed=devices;listedAt=android.os.SystemClock.elapsedRealtime()
                return proposal("${d.name}: open Android Bluetooth settings to ${if(n.startsWith("disconnect") || n.startsWith("desconecta") || n.startsWith("قطع"))"disconnect" else "connect"}? Choose the accessory there. Jarvis cannot force arbitrary profile connections; ask ‘is it connected?’ afterwards to check the profiles Android exposes.","system_panel","bluetooth")
            }
            if(Regex("scan|discover|find nearby|busca|جستجو|پیدا").containsMatchIn(n))return proposal("Open Bluetooth discovery controls? A scan starts only when you press Discover and stops after 12 seconds or when leaving the screen.","infinity_controls","")
            listed=devices;listedAt=android.os.SystemClock.elapsedRealtime();selected=devices.singleOrNull()?.address
            return AssistantReply(if(devices.isEmpty())"No paired Bluetooth devices reported. Use Android Bluetooth settings to pair an accessory." else devices.mapIndexed{i,d->"${i+1}. ${d.name} · ${d.type} · ${if(d.connected)"connection reported" else "connection not reported by queried profiles"} · battery unavailable"}.joinToString("\n"))
        }
        val panel=when {
            Regex("^(?:open|show|abre|muestra) (?:the )?(?:internet|network|red)(?: settings| panel| ajustes)?$").matches(n)->"internet"
            Regex("^(?:turn|switch) (?:on|off) (?:wi fi|wifi)$|^(?:enable|disable|open) (?:wi fi|wifi)(?: settings)?$").matches(n)->"wifi"
            Regex("^(?:open|show) (?:volume|sound)(?: settings| panel)?$").matches(n)->"volume"
            Regex("^(?:open|show) (?:brightness|display)(?: settings)?$").matches(n)->"display"
            Regex("^(?:open|show) location(?: settings)?$").matches(n)->"location"
            Regex("^(?:open|enable|disable) battery saver(?: settings)?$").matches(n)->"battery_saver"
            Regex("^(?:open|enable|disable) airplane mode(?: settings)?$").matches(n)->"airplane"
            Regex("^(?:open|enable|disable) (?:do not disturb|dnd)(?: settings)?$").matches(n)->"dnd"
            else->null
        }
        if(panel!=null)return proposal("Open Android’s $panel controls? Review and change the setting there. Jarvis will not claim it changed merely because the panel opened.","system_panel",panel)
        val site=Regex("^(open|read|abre|lee) (?:saved website |website |sitio )?(.+)$").matchEntire(n)
        if(site!=null){val found=preferences.websites().firstOrNull{AppMatching.normalize(it.name)==site.groupValues[2]};if(found!=null){
            if(site.groupValues[1] in setOf("read","lee")){if(com.jarvis.assistant.neural.ContextPrivacyPolicy.localOnly(input))return AssistantReply("Reading this site needs network access; no request was made in local-only mode.");return WebResearch().read(found.url,lang)}
            return proposal("Open saved website ${found.name}: ${found.url}?","open_url",found.url)
        }}
        if(Regex("what.*song|identify.*music|identify.*song|que cancion|چه آهنگی").containsMatchIn(n))return AssistantReply(lang.choose("Music identification needs a configured, licensed audio-fingerprint service. This build does not record audio for song matching or invent a song result. You can open your installed music-recognition app.","La identificación musical requiere un servicio autorizado configurado. No grabé audio ni inventé una canción. Puedes abrir tu app de reconocimiento.","شناسایی آهنگ نیازمند سرویس مجاز پیکربندی‌شده است؛ صدایی ضبط نشد و نتیجه‌ای ساخته نشد. می‌توانید برنامهٔ شناسایی موسیقی خود را باز کنید."))
        return null
    }
}
