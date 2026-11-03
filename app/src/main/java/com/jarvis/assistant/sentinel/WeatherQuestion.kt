package com.jarvis.assistant.sentinel

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.tools.ToolPolicy
import com.jarvis.assistant.core.choose
import com.jarvis.assistant.hud.WeatherReading
import kotlin.math.roundToInt

object WeatherQuestion {
    fun isRequest(text:String):Boolean {
        val t=ToolPolicy.normalize(text).trimEnd('?','؟','.','!').replace("what’s","what is").replace("what's","what is")
        if(t in listOf("what is weather","qué es el clima","que es el clima"))return false
        return matches(t) || Regex("^(?:(?:tell me|show me|check|what is|how is)\\s+)?(?:the\\s+)?weather(?:\\s|$)").containsMatchIn(t) ||
            Regex("^(?:(?:cómo está|como esta|dime|consulta|cuál es|cual es)\\s+)?(?:el\\s+)?(?:clima|pronóstico|pronostico|tiempo)(?:\\s|$)").containsMatchIn(t) ||
            Regex("^(?:آب و هوا|هوای|هوا)(?:\\s|$)").containsMatchIn(t)
    }
    fun matches(text:String):Boolean {
        val t=ToolPolicy.normalize(text).trimEnd('?','؟','.','!').replace("what’s","what is").replace("what's","what is")
        return Regex("^(what s|what is|how is|what)? ?(the )?(weather|clima|tiempo)( like)?( today| now| this evening| hoy| ahora)?$").matches(t) ||
            t.removeSuffix(" hoy").removeSuffix(" ahora") in listOf("qué tiempo hace","que tiempo hace","cómo está el clima","como esta el clima","هوا چطوره","هوا امروز چطوره","آب و هوا","آب و هوا امروز")
    }
    fun describe(r:WeatherReading,language:AppLanguage,fahrenheit:Boolean):String {
        val units=if(fahrenheit)"°F" else "°C"
        fun temp(v:Double?)=v?.let{(if(fahrenheit)it*9/5+32 else it).roundToInt().toString()+units} ?: language.choose("unavailable","no disponible","نامشخص")
        val freshness=if(com.jarvis.assistant.hud.WeatherRepository.stale(r,System.currentTimeMillis()))language.choose("Cached/stale observation; current conditions are not verified.\n","Observación en caché/desactualizada; no se verificaron las condiciones actuales.\n","مشاهدهٔ ذخیره‌شده یا قدیمی؛ وضعیت کنونی تأیید نشده است.\n") else ""
        return freshness+language.choose(
            "Current weather in ${r.place}: ${temp(r.temperatureC)}, feels like ${temp(r.feelsLikeC)}. Humidity ${r.humidity?.let{"$it%"} ?: "unavailable"}; wind ${r.windKmh?.let{"$it km/h"} ?: "unavailable"}. Rain probability for this hour: ${r.precipitationProbability?.let{"$it%"} ?: "unavailable"}. Observed ${r.observationTime.replace('T',' ')} local time. Source: Open-Meteo.",
            "Clima actual en ${r.place}: ${temp(r.temperatureC)}, sensación ${temp(r.feelsLikeC)}. Humedad ${r.humidity?.let{"$it%"} ?: "no disponible"}; viento ${r.windKmh?.let{"$it km/h"} ?: "no disponible"}. Probabilidad de lluvia esta hora: ${r.precipitationProbability?.let{"$it%"} ?: "no disponible"}. Observado ${r.observationTime.replace('T',' ')} hora local. Fuente: Open-Meteo.",
            "هوای کنونی ${r.place}: ${temp(r.temperatureC)}، دمای احساس‌شده ${temp(r.feelsLikeC)}. رطوبت ${r.humidity?.let{"$it%"} ?: "نامشخص"}؛ باد ${r.windKmh?.let{"$it km/h"} ?: "نامشخص"}. احتمال بارش این ساعت: ${r.precipitationProbability?.let{"$it%"} ?: "نامشخص"}. زمان مشاهده ${r.observationTime.replace('T',' ')} به وقت همان مکان. منبع: Open-Meteo.")
    }
}
