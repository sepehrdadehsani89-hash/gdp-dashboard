package com.jarvis.assistant.checkin

import java.time.*
import java.util.UUID

object CallMePolicy {
    fun isRequest(text:String)=Regex("^(?:(?:hey )?jarvis[, ]+)?(?:please )?(?:call me|schedule (?:a )?(?:jarvis )?check[ -]?in|open (?:my )?check[ -]?ins|llamame|llámame|programa una llamada)(?:\\b|$)",RegexOption.IGNORE_CASE).containsMatchIn(text.trim())
    fun suggested(text:String,now:ZonedDateTime):ZonedDateTime? {
        val command=text.trim().replace(Regex("^(?:hey )?jarvis[, ]+",RegexOption.IGNORE_CASE),"")
        Regex("^(?:please )?call me in (\\d{1,4}) (minutes?|hours?)$",RegexOption.IGNORE_CASE).matchEntire(command)?.let{m->val n=m.groupValues[1].toLong();if(n !in 1..1440)return null;return if(m.groupValues[2].startsWith("hour",true))now.plusHours(n)else now.plusMinutes(n)}
        val m=Regex("^(?:please )?call me (?:on (\\d{4}-\\d{2}-\\d{2}) |(tomorrow) )?at (\\d{1,2}):(\\d{2})(?: (am|pm))?$",RegexOption.IGNORE_CASE).matchEntire(command) ?: return null
        return runCatching{
            var hour=m.groupValues[3].toInt();val minute=m.groupValues[4].toInt();val half=m.groupValues[5].lowercase()
            if(half.isNotBlank()){require(hour in 1..12);hour=hour%12+if(half=="pm")12 else 0}
            val date=if(m.groupValues[1].isNotBlank())LocalDate.parse(m.groupValues[1])else now.toLocalDate().plusDays(if(m.groupValues[2].isNotBlank())1 else 0)
            strict(date,LocalTime.of(hour,minute),now.zone).takeIf{it.isAfter(now)}
        }.getOrNull()
    }
    fun strict(date:LocalDate,time:LocalTime,zone:ZoneId):ZonedDateTime {
        val local=LocalDateTime.of(date,time);val offsets=zone.rules.getValidOffsets(local)
        require(offsets.size==1){"Choose an unambiguous local date/time; this time crosses a daylight-saving change."}
        return ZonedDateTime.ofStrict(local,offsets.single(),zone)
    }
    fun validate(c:CallMeEntry){require(UUID.fromString(c.id).toString()==c.id && UUID.fromString(c.revision).toString()==c.revision && c.at>0 && c.title.length in 1..100 && c.title.none(Char::isISOControl));ZoneId.of(c.zone);require(c.status in setOf("SCHEDULED","NOTIFIED","DELIVERING","SPOKEN","TEXT_ONLY","MISSED","PERMISSION_REQUIRED","SCHEDULE_UNAVAILABLE"))}
    fun due(c:CallMeEntry,revision:String,now:Long)=c.revision==revision && c.status=="SCHEDULED" && now>=c.at
    fun invitation(id:String,spanish:Boolean)=when(Math.floorMod(id.hashCode(),3)){
        0->if(spanish)"Señor, me pediste que te llamara. Estoy aquí. ¿En qué te ayudo?" else "You wanted me to call you, sir. I'm here. What can I help you with?"
        1->if(spanish)"Es nuestra hora acordada, señor. Te escucho. ¿Qué hacemos?" else "It's our scheduled check-in, sir. I'm ready when you are. What shall we do?"
        else->if(spanish)"Tu llamada de Jarvis está lista, señor. ¿Alguna pregunta o tarea?" else "Your Jarvis check-in is ready, sir. Any questions or tasks for me?"
    }
}
data class CallMeEntry(val id:String,val revision:String,val at:Long,val zone:String,val title:String,val exact:Boolean,val spoken:Boolean,val status:String="SCHEDULED")
