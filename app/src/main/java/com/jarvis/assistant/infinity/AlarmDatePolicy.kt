package com.jarvis.assistant.infinity

import com.jarvis.assistant.tools.UtilityIntentParser
import com.jarvis.assistant.tools.ToolPolicy
import java.time.*

/** AlarmClock has no portable one-off date extra. A date must stay a reviewed request, not become a weekly alarm. */
object AlarmDatePolicy {
    data class Schedule(val hour:Int,val minute:Int,val requestedDate:LocalDate?,val repeatDays:List<Int>,val zone:String)
    fun parse(input:String,now:ZonedDateTime):Schedule? {
        val time=UtilityIntentParser.alarmTime(input) ?: return null
        val text=UtilityIntentParser.digits(ToolPolicy.normalize(input))
        if(Regex("\\b(next week|next month|after tomorrow|day after|january|february|march|april|may|june|july|august|september|october|november|december|enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre|pasado)\\b|پس فردا|پس‌فردا|هفته بعد|ماه بعد").containsMatchIn(text))return null
        val days=mapOf(
            DayOfWeek.MONDAY to listOf("monday","lunes","دوشنبه"),DayOfWeek.TUESDAY to listOf("tuesday","martes","سه شنبه","سه‌شنبه"),DayOfWeek.WEDNESDAY to listOf("wednesday","miercoles","miércoles","چهارشنبه"),DayOfWeek.THURSDAY to listOf("thursday","jueves","پنجشنبه"),DayOfWeek.FRIDAY to listOf("friday","viernes","جمعه"),DayOfWeek.SATURDAY to listOf("saturday","sabado","sábado","شنبه"),DayOfWeek.SUNDAY to listOf("sunday","domingo","یکشنبه")
        )
        val found=days.filterValues{names->names.any{Regex("(?<![\\p{L}])${Regex.escape(it)}(?![\\p{L}])").containsMatchIn(text)}}.keys
        if(found.isNotEmpty() && Regex("\\b(next|following|próximo|proximo)\\b").containsMatchIn(text))return null
        val daily=Regex("\\b(daily|every day|everyday|diariamente|cada dia|cada día)\\b|هر روز").containsMatchIn(text)
        val repeat=daily || Regex("\\b(every|each|weekly|cada|todos los)\\b|هر ").containsMatchIn(text)
        if(repeat){if(!daily && found.isEmpty())return null;return Schedule(time.first,time.second,null,(if(daily)DayOfWeek.values().toList() else found).map{it.value%7+1}.sorted(),now.zone.id)}
        val iso=Regex("(?<!\\d)\\d{4}-\\d{2}-\\d{2}(?!\\d)").find(text)
        val tomorrow=Regex("\\b(tomorrow|mañana|manana)\\b|فردا").containsMatchIn(text)
        val today=Regex("\\b(today|hoy)\\b|امروز").containsMatchIn(text)
        if(found.size>1 || listOf(iso!=null,tomorrow,today,found.isNotEmpty()).count{it}>1)return null
        val date=when {
            iso!=null->runCatching{LocalDate.parse(iso.value)}.getOrNull() ?: return null
            tomorrow->now.toLocalDate().plusDays(1)
            today->now.toLocalDate()
            found.isNotEmpty()->{val day=found.single();var d=now.toLocalDate();while(d.dayOfWeek!=day)d=d.plusDays(1);if(d==now.toLocalDate() && !LocalTime.of(time.first,time.second).isAfter(now.toLocalTime()))d=d.plusWeeks(1);d}
            Regex("\\b(next|in|on|after|próximo|proximo|después|despues|en)\\b|بعد|هفته|ماه").containsMatchIn(text)->return null
            else->null
        }
        if(date!=null && (date<now.toLocalDate() || date==now.toLocalDate() && !LocalTime.of(time.first,time.second).isAfter(now.toLocalTime())))return null
        return Schedule(time.first,time.second,date,emptyList(),now.zone.id)
    }
}
