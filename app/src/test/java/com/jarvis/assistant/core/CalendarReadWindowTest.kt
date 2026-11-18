package com.jarvis.assistant.core

import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class CalendarReadWindowTest {
    private fun clock(at:String="2026-11-17T15:00:00Z",zone:String="America/Guayaquil")=Clock.fixed(Instant.parse(at),ZoneId.of(zone))
    @Test fun todayStartsAtActualCurrentInstant(){val c=clock();assertEquals(c.instant(),CalendarReadWindow.create(1,0,c).start)}
    @Test fun todayEndsAtLocalMidnight(){assertEquals(Instant.parse("2026-11-18T05:00:00Z"),CalendarReadWindow.create(1,0,clock()).endExclusive)}
    @Test fun tomorrowStartsAtNextLocalMidnight(){assertEquals(Instant.parse("2026-11-18T05:00:00Z"),CalendarReadWindow.create(1,1,clock()).start)}
    @Test fun tomorrowEndsAtFollowingLocalMidnight(){assertEquals(Instant.parse("2026-11-19T05:00:00Z"),CalendarReadWindow.create(1,1,clock()).endExclusive)}
    @Test fun tomorrowIsNotNextTwentyFourHoursFromNow(){val c=clock();assertNotEquals(c.instant(),CalendarReadWindow.create(1,1,c).start)}
    @Test fun DSTSpringTomorrowCanHaveTwentyThreeHours(){val c=clock("2026-03-07T16:00:00Z","America/New_York");val w=CalendarReadWindow.create(1,1,c);assertEquals(23,Duration.between(w.start,w.endExclusive).toHours().toInt())}
    @Test fun DSTAutumnTomorrowCanHaveTwentyFiveHours(){val c=clock("2026-10-31T16:00:00Z","America/New_York");val w=CalendarReadWindow.create(1,1,c);assertEquals(25,Duration.between(w.start,w.endExclusive).toHours().toInt())}
    @Test fun weekUsesLocalCalendarDaysAcrossDST(){val c=clock("2026-03-06T16:00:00Z","America/New_York");val w=CalendarReadWindow.create(7,0,c);assertEquals(167,Duration.between(w.start,w.endExclusive).toHours().toInt())}
    @Test fun exactEndBoundaryDoesNotLeakIntoWindow(){val w=CalendarReadWindow.create(1,1,clock());assertFalse(w.contains(w.endExclusive,w.endExclusive.plusSeconds(60)))}
    @Test fun eventEndingAtStartIsExcluded(){val w=CalendarReadWindow.create(1,1,clock());assertFalse(w.contains(w.start.minusSeconds(60),w.start))}
    @Test fun eventSpanningWindowStartIsIncluded(){val w=CalendarReadWindow.create(1,1,clock());assertTrue(w.contains(w.start.minusSeconds(60),w.start.plusSeconds(60)))}
    @Test fun eventStartingAtWindowStartIsIncluded(){val w=CalendarReadWindow.create(1,1,clock());assertTrue(w.contains(w.start,w.start.plusSeconds(60)))}
    @Test fun unsupportedWindowLengthIsRejected(){assertThrows(IllegalArgumentException::class.java){CalendarReadWindow.create(30,0,clock())}}
    @Test fun negativeDayOffsetIsRejected(){assertThrows(IllegalArgumentException::class.java){CalendarReadWindow.create(1,-1,clock())}}
    @Test fun unsupportedWeekOffsetIsRejected(){assertThrows(IllegalArgumentException::class.java){CalendarReadWindow.create(7,1,clock())}}
    @Test fun englishTomorrowRequestDoesNotMeanToday(){assertEquals(PhoneRequest.Calendar(1,1),PhoneRequestParser.parse("What's on my calendar tomorrow?"))}
    @Test fun spanishTomorrowRequestHasTheSameWindow(){assertEquals(PhoneRequest.Calendar(1,1),PhoneRequestParser.parse("¿Qué tengo mañana?"))}
    @Test fun persianTomorrowRequestHasTheSameWindow(){assertEquals(PhoneRequest.Calendar(1,1),PhoneRequestParser.parse("فردا چه برنامه ای دارم"))}
    @Test fun existingTodayRequestIsPreserved(){assertEquals(PhoneRequest.Calendar(1),PhoneRequestParser.parse("show my calendar today"))}
    @Test fun mutationDoesNotBecomeCalendarRead(){assertNull(PhoneRequestParser.parse("delete my calendar tomorrow"))}
    @Test fun questionAboutTheWordTomorrowDoesNotQueryCalendar(){assertNull(PhoneRequestParser.parse("What does tomorrow mean?"))}
    @Test fun targetZoneIsRetainedForDisplay(){assertEquals(ZoneId.of("America/Guayaquil"),CalendarReadWindow.create(1,1,clock()).zone)}
}
