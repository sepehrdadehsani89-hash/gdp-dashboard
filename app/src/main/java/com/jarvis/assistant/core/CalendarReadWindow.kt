package com.jarvis.assistant.core

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CalendarReadWindow(val start:Instant,val endExclusive:Instant,val zone:ZoneId,val startDay:Int,val days:Int){
    fun contains(started:Instant,ended:Instant)=started<endExclusive && ended>start
    companion object {
        fun create(days:Int=1,startDay:Int=0,clock:Clock=Clock.systemDefaultZone()):CalendarReadWindow {
            require(days in setOf(1,7) && startDay in 0..1 && (startDay==0 || days==1))
            val day=LocalDate.now(clock);val zone=clock.zone
            val start=if(startDay==0)clock.instant()else day.plusDays(1).atStartOfDay(zone).toInstant()
            val end=if(days==1)day.plusDays((startDay+1).toLong()).atStartOfDay(zone).toInstant()else clock.instant().atZone(zone).plusDays(7).toInstant()
            return CalendarReadWindow(start,end,zone,startDay,days)
        }
    }
}
