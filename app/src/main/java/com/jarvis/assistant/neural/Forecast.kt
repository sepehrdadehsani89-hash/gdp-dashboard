package com.jarvis.assistant.neural

import com.jarvis.assistant.core.*
import com.jarvis.assistant.hud.WeatherClient
import org.json.JSONObject
import java.time.LocalDate

data class ForecastDay(val date:String,val high:Double?,val low:Double?,val rain:Int?)
object ForecastPolicy {
    private val weekdays=listOf(
        Regex("(?:monday|lunes|دوشنبه)"),Regex("(?:tuesday|martes|سه شنبه)"),
        Regex("(?:wednesday|miercoles|چهارشنبه)"),Regex("(?:thursday|jueves|پنج شنبه)"),
        Regex("(?:friday|viernes|جمعه)"),Regex("(?:saturday|sabado|شنبه)"),Regex("(?:sunday|domingo|یکشنبه)")
    )
    /** Only an explicit date phrase or terminal weekday; city names such as Friday Harbor stay intact. */
    fun weekday(text:String):Int?{
        val t=AppMatching.normalize(text)
        return weekdays.indexOfFirst{Regex("(?:^| )(?:on |el |روز |next |proximo |آینده )${it.pattern}(?= |$)|(?:^| )${it.pattern}$").containsMatchIn(t)}.takeIf{it>=0}?.plus(1)
    }
    fun day(text:String,reference:LocalDate=LocalDate.now()):Int{
        val t=AppMatching.normalize(text)
        return when{
            Regex("day after tomorrow|pasado manana|پس فردا").containsMatchIn(t)->2
            Regex("next week|proxima semana|هفته بعد").containsMatchIn(t)->7
            Regex("tomorrow|manana|فردا").containsMatchIn(t)->1
            else->weekday(t)?.let{((it-reference.dayOfWeek.value+7)%7).let{offset->if(offset==0 && Regex("next |proximo |آینده ").containsMatchIn(t))7 else offset}} ?: 0
        }
    }
    fun request(text:String)=Regex("weather|umbrella|will it rain|rain today|rain tomorrow|clima|paraguas|lluvia|هوای|آب و هوا|چتر|باران",RegexOption.IGNORE_CASE).containsMatchIn(text) && !Regex("^(explain|how do|what is|define|explica|چگونه|توضیح)",RegexOption.IGNORE_CASE).containsMatchIn(text)
    fun city(text:String):String?{
        val dated=text.replace(Regex("(?:\\b(?:on |el |next |próximo |proximo )|روز )(?:(?:mon|tues|wednes|thurs|fri|satur|sun)day|lunes|martes|miércoles|miercoles|jueves|viernes|sábado|sabado|domingo|دوشنبه|سه شنبه|چهارشنبه|پنج شنبه|جمعه|شنبه|یکشنبه)(?= |$)",RegexOption.IGNORE_CASE)," ")
        val t=dated.replace(Regex("\\b(tomorrow|today|next week|the day after tomorrow|manana|hoy|pasado manana)\\b|پس فردا|فردا|امروز|هفته بعد",RegexOption.IGNORE_CASE)," ")
        val target=Regex("(?:\\bin |\\ben | در | برای )([\\p{L}\\p{M} .'-]{2,80})",RegexOption.IGNORE_CASE).find(t)?.groupValues?.get(1)?.substringBefore(",")?.substringBefore(" then ")?.substringBefore(" and ")?.trim(' ','.','?','؟')
        return target?.takeIf{it.isNotBlank()}
    }
    fun parse(json:JSONObject,index:Int):ForecastDay{
        require(index in 0..7);check(!json.optBoolean("error",false))
        val d=json.getJSONObject("daily");val date=d.getJSONArray("time").getString(index);LocalDate.parse(date)
        fun number(k:String):Double? {val a=d.optJSONArray(k) ?: return null;return if(a.isNull(index) || index>=a.length())null else a.optDouble(index).takeIf{it.isFinite()}}
        return ForecastDay(date,number("temperature_2m_max"),number("temperature_2m_min"),number("precipitation_probability_max")?.toInt()?.takeIf{it in 0..100})
    }
    fun describe(day:ForecastDay,place:String,lang:AppLanguage,umbrella:Boolean):String {
        fun temp(v:Double?)=v?.let{"${kotlin.math.round(it*10)/10}°C"} ?: lang.choose("unavailable","no disponible","در دسترس نیست")
        val rain=day.rain?.let{"$it%"} ?: lang.choose("unavailable","no disponible","در دسترس نیست")
        val base=lang.choose("$place · ${day.date}: high ${temp(day.high)}, low ${temp(day.low)}, maximum rain probability $rain.","$place · ${day.date}: máxima ${temp(day.high)}, mínima ${temp(day.low)}, probabilidad máxima de lluvia $rain.","$place · ${day.date}: بیشینه ${temp(day.high)}، کمینه ${temp(day.low)}، بیشترین احتمال بارش $rain.")
        if(!umbrella)return base
        val advice=when{day.rain==null->lang.choose("Rain probability is missing, so I cannot ground an umbrella recommendation.","Falta la probabilidad de lluvia; no puedo fundamentar la recomendación.","احتمال بارش موجود نیست؛ نمی‌توانم پیشنهاد مستندی درباره چتر بدهم.");day.rain>=40->lang.choose("Taking an umbrella is prudent; this is a forecast, not a guarantee.","Conviene llevar paraguas; es un pronóstico, no una garantía.","همراه داشتن چتر محتاطانه است؛ این پیش‌بینی تضمین نیست.");else->lang.choose("Rain risk is lower; consider a small umbrella if getting wet matters.","El riesgo es menor; considera un paraguas pequeño si necesitas evitar mojarte.","احتمال بارش کمتر است؛ اگر خیس شدن مهم است چتر کوچکی ببرید.")}
        return "$base\n$advice"
    }
}
class ForecastService(private val onCurrent:((com.jarvis.assistant.hud.WeatherReading)->Unit)?=null) {
    private val client=WeatherClient()
    private val cache=mutableMapOf<String,Triple<Long,String,JSONObject>>()
    suspend fun reply(city:String,day:Int,lang:AppLanguage,umbrella:Boolean=false,requestedText:String?=null,onResolvedDay:((Int)->Unit)?=null):AssistantReply{
        require(city.isNotBlank() && city.length<=80)
        val now=System.currentTimeMillis();val prior=cache[city]
        val value=if(prior!=null && now-prior.first in 0..900_000)prior else client.fetch(city,null,null,8).let{Triple(now,it.place,it.json).also{v->if(cache.size>=8)cache.clear();cache[city]=v}}
        runCatching{com.jarvis.assistant.hud.WeatherRepository.parse(value.third,value.second,value.first)}.getOrNull()?.let{onCurrent?.invoke(it)}
        val zone=java.time.ZoneId.of(value.third.getString("timezone"))
        val sourceToday=LocalDate.parse(value.third.getJSONObject("daily").getJSONArray("time").getString(0))
        check(sourceToday==java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()){"Forecast dates are stale for the source timezone; refresh and retry."}
        val resolved=if(requestedText!=null && ForecastPolicy.weekday(requestedText)!=null)ForecastPolicy.day(requestedText,sourceToday) else day
        val reading=ForecastPolicy.parse(value.third,resolved)
        check(LocalDate.parse(reading.date)==sourceToday.plusDays(resolved.toLong())){"Forecast dates are not consecutive."}
        onResolvedDay?.invoke(resolved)
        val observed=zone.id
        return AssistantReply(ForecastPolicy.describe(reading,value.second,lang,umbrella)+"\nOpen-Meteo · $observed · ${java.time.Instant.ofEpochMilli(value.first)}",listOf(SourceLink("Open-Meteo forecast","https://open-meteo.com/")))
    }
}
