package com.jarvis.assistant.reminders

import com.jarvis.assistant.neural.AppMatching
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

const val REMINDER_YEAR=366L*86_400_000

enum class ReminderStatus { SCHEDULED,DELIVERING,DELIVERY_UNCONFIRMED,NOTIFIED,PERMISSION_REQUIRED,MISSED,SCHEDULE_FAILED }
data class OwnedReminder(val id:String,val revision:String,val title:String,val at:Long,val zone:String,val status:ReminderStatus=ReminderStatus.SCHEDULED){override fun toString()="OwnedReminder(id=$id, at=$at, status=$status, content=redacted)"}
sealed interface ReminderIntent {
    data class Create(val title:String,val at:Long,val zone:String):ReminderIntent
    data class Cancel(val query:String):ReminderIntent
    data object ListReminders:ReminderIntent
    data object NeedsDetails:ReminderIntent
}
object ReminderPolicy {
    fun validate(row:OwnedReminder){require(runCatching{UUID.fromString(row.id).toString()==row.id && UUID.fromString(row.revision).toString()==row.revision}.getOrDefault(false));require(row.title.isNotBlank() && row.title.length<=300 && !row.title.any{it.isISOControl()});require(row.at>0);ZoneId.of(row.zone)}
    fun validateFuture(at:Long,now:Long){require(at-now in 1001..REMINDER_YEAR){"Choose a future time within the next366 days."}}
    fun due(row:OwnedReminder,revision:String,now:Long)=row.revision==revision && row.status==ReminderStatus.SCHEDULED && row.at<=now
    fun parse(raw:String,clock:Clock=Clock.systemDefaultZone()):ReminderIntent? {
        val normalized=AppMatching.normalize(raw).removePrefix("hey jarvis ").removePrefix("jarvis ")
        if(normalized in setOf("show my reminders","list my reminders","what are my reminders","what reminders do i have","mis recordatorios","یاداوری های من"))return ReminderIntent.ListReminders
        val clean=raw.trim().replace(Regex("^(?:hey\\s+)?jarvis[, ]+",RegexOption.IGNORE_CASE),"").trim().trimEnd('.','!')
        Regex("^cancel (?:my )?reminder (.+)$",RegexOption.IGNORE_CASE).matchEntire(clean)?.let{return ReminderIntent.Cancel(it.groupValues[1].trim())}
        if(!clean.startsWith("remind me ",true))return null
        Regex("^remind me in ([0-9]{1,6}) (minute|minutes|hour|hours|day|days) to (.+)$",RegexOption.IGNORE_CASE).matchEntire(clean)?.let{m->
            val count=m.groupValues[1].toLong();val unit=when(m.groupValues[2].lowercase()){ "minute","minutes"->60_000L;"hour","hours"->3_600_000L;else->86_400_000L}
            val at=clock.millis()+count*unit;val title=m.groupValues[3].trim();if(count>0 && at-clock.millis() in 1001..REMINDER_YEAR && validTitle(title))return ReminderIntent.Create(title,at,clock.zone.id)
            return ReminderIntent.NeedsDetails
        }
        Regex("^remind me (today|tomorrow) at ([0-9]{1,2}):([0-9]{2})(?: (am|pm))? to (.+)$",RegexOption.IGNORE_CASE).matchEntire(clean)?.let{m->
            val h=m.groupValues[2].toInt();val min=m.groupValues[3].toInt();val meridiem=m.groupValues[4].lowercase();val title=m.groupValues[5].trim()
            if(min !in 0..59 || (if(meridiem.isBlank())h !in 0..23 else h !in 1..12) || !validTitle(title))return ReminderIntent.NeedsDetails
            val hour=if(meridiem.isBlank())h else h%12+if(meridiem=="pm")12 else 0
            val day=LocalDate.now(clock).plusDays(if(m.groupValues[1].equals("tomorrow",true))1 else 0)
            val local=LocalDateTime.of(day,LocalTime.of(hour,min));val offsets=clock.zone.rules.getValidOffsets(local)
            // DST gaps and repeated local times require an explicit choice in the UI.
            if(offsets.size!=1)return ReminderIntent.NeedsDetails
            val at=local.toInstant(offsets.single()).toEpochMilli()
            if(at-clock.millis() !in 1001..REMINDER_YEAR)return ReminderIntent.NeedsDetails
            return ReminderIntent.Create(title,at,clock.zone.id)
        }
        return ReminderIntent.NeedsDetails
    }
    private fun validTitle(title:String)=title.isNotBlank() && title.length<=300 && !title.any{it.isISOControl()}
}
