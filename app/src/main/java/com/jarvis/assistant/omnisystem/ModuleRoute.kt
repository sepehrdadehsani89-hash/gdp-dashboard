package com.jarvis.assistant.omnisystem

import com.jarvis.assistant.neural.AppMatching
import com.jarvis.assistant.infinity.DevicePolicy

/** Presentation only. A module selection never grants tool authority or starts hardware. */
enum class HudModule { ASSISTANT, WEATHER, LOCATION, BATTERY, WIFI, BLUETOOTH, NETWORK, MUSIC, CAMERA, CLOCK, SYSTEM }
object ModuleRoute {
    fun fromUserInput(input: String): HudModule? {
        val n = AppMatching.normalize(input)
        if (DevicePolicy.informationalRequest(n) || Regex("^(remember|do not|dont|don t|never|no quiero|no muestres|no abras|recuerda|یادت باشد)\\b").containsMatchIn(n) || Regex("نکن|نخوان|نشان نده").containsMatchIn(n)) return null
        // A contextual follow-up retains the selected device/module; "its" is not the phone.
        if(n in setOf("what is its battery","whats its battery percentage","what s its battery","its battery","باتری ان چقدر است"))return null
        // Direct app launching retains its own path, including music and camera app names.
        val target=AppMatching.commandTarget(input)
        if (target != null && target !in setOf("selfie camera","front camera","camara frontal","camara selfie","دوربین جلو","دوربین سلفی") && !Regex("settings|ajustes|تنظیمات").containsMatchIn(n)) return null
        return when {
            Regex("speed test|internet speed|wifi speed|wi fi speed|velocidad.*internet|سرعت.*اینترنت").containsMatchIn(n) -> HudModule.NETWORK
            Regex("bluetooth|بلوتوث").containsMatchIn(n) -> HudModule.BLUETOOTH
            Regex("battery|charging|bateria|باتری|شارژ").containsMatchIn(n) -> HudModule.BATTERY
            Regex("\\bwifi\\b|wi fi|وای فای").containsMatchIn(n) -> HudModule.WIFI
            Regex("weather|forecast|umbrella|clima|pronostico|paraguas|اب و هوا|هوای|چتر").containsMatchIn(n) -> HudModule.WEATHER
            Regex("where am i|my location|donde estoy|ubicacion|کجا هستم|موقعیت من").containsMatchIn(n) -> HudModule.LOCATION
            Regex("identify.*song|identify.*music|que cancion|چه اهنگی").containsMatchIn(n) -> HudModule.MUSIC
            Regex("selfie|front camera|camara frontal|camara selfie|دوربین جلو|سلفی").containsMatchIn(n) -> HudModule.CAMERA
            Regex("\\balarm\\b|\\btimer\\b|\\balarma\\b|temporizador|زنگ|تایمر").containsMatchIn(n) -> HudModule.CLOCK
            Regex("system telemetry|system status|estado del sistema|وضعیت سیستم").containsMatchIn(n) -> HudModule.SYSTEM
            else -> null
        }
    }
}
