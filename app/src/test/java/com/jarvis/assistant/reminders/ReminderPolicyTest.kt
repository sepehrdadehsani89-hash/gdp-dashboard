package com.jarvis.assistant.reminders

import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ReminderPolicyTest {
    private val clock=Clock.fixed(Instant.parse("2026-11-17T15:00:00Z"),ZoneId.of("America/Guayaquil"))
    private fun parse(text:String)=ReminderPolicy.parse(text,clock)
    @Test fun relativeMinutesHaveAnActualFutureInstant(){val r=parse("Remind me in20 minutes to take a break");assertEquals(ReminderIntent.NeedsDetails,r);val q=parse("Remind me in 20 minutes to take a break") as ReminderIntent.Create;assertEquals(clock.millis()+1_200_000,q.at);assertEquals("take a break",q.title)}
    @Test fun relativeHoursAreBounded(){val q=parse("remind me in 2 hours to leave") as ReminderIntent.Create;assertEquals(clock.millis()+7_200_000,q.at)}
    @Test fun relativeDaysAreExplicitElapsedTime(){val q=parse("remind me in 1 day to review notes") as ReminderIntent.Create;assertEquals(clock.millis()+86_400_000,q.at)}
    @Test fun tomorrowUsesPhonesTimeZone(){val q=parse("remind me tomorrow at 09:00 to read") as ReminderIntent.Create;assertEquals(Instant.parse("2026-11-18T14:00:00Z").toEpochMilli(),q.at);assertEquals(clock.zone.id,q.zone)}
    @Test fun pmConvertsCorrectly(){val q=parse("remind me today at 2:30 pm to call") as ReminderIntent.Create;assertEquals(Instant.parse("2026-11-17T19:30:00Z").toEpochMilli(),q.at)}
    @Test fun noonIsNotMidnight(){val q=parse("remind me today at 12:00 pm to eat") as ReminderIntent.Create;assertEquals(Instant.parse("2026-11-17T17:00:00Z").toEpochMilli(),q.at)}
    @Test fun midnightIsConvertedCorrectly(){val q=parse("remind me tomorrow at 12:00 am to check") as ReminderIntent.Create;assertEquals(Instant.parse("2026-11-18T05:00:00Z").toEpochMilli(),q.at)}
    @Test fun vagueMorningDoesNotInventNineAM(){assertEquals(ReminderIntent.NeedsDetails,parse("remind me tomorrow morning to call"))}
    @Test fun missingReminderTextNeedsDetails(){assertEquals(ReminderIntent.NeedsDetails,parse("remind me tomorrow at 09:00"))}
    @Test fun ambiguousHourWithoutMinutesNeedsDetails(){assertEquals(ReminderIntent.NeedsDetails,parse("remind me tomorrow at 9 to read"))}
    @Test fun pastTimeNeedsDetails(){assertEquals(ReminderIntent.NeedsDetails,parse("remind me today at 09:00 to read"))}
    @Test fun invalidHoursAreRejected(){assertEquals(ReminderIntent.NeedsDetails,parse("remind me tomorrow at 25:00 to read"))}
    @Test fun invalidMinutesAreRejected(){assertEquals(ReminderIntent.NeedsDetails,parse("remind me tomorrow at 09:75 to read"))}
    @Test fun invalidTwelveHourClockIsRejected(){assertEquals(ReminderIntent.NeedsDetails,parse("remind me tomorrow at 14:00 pm to read"))}
    @Test fun zeroDelayIsRejected(){assertEquals(ReminderIntent.NeedsDetails,parse("remind me in 0 minutes to read"))}
    @Test fun hugeDelayIsRejected(){assertEquals(ReminderIntent.NeedsDetails,parse("remind me in 999999 days to read"))}
    @Test fun controlCharactersCannotEnterReminderTitles(){assertEquals(ReminderIntent.NeedsDetails,parse("remind me in 2 minutes to read\nnow"))}
    @Test fun titleCaseAndContentArePreserved(){val q=parse("Jarvis, remind me in 2 minutes to Call Ma about AI") as ReminderIntent.Create;assertEquals("Call Ma about AI",q.title)}
    @Test fun explicitListIsRecognized(){assertEquals(ReminderIntent.ListReminders,parse("Show my reminders."))}
    @Test fun cancelTargetIsPreserved(){assertEquals("Call Ma",(parse("cancel my reminder Call Ma") as ReminderIntent.Cancel).query)}
    @Test fun negationDoesNotCreateReminder(){assertNull(parse("Do not remind me in 2 minutes to read"))}
    @Test fun questionAboutRemindersDoesNotCreateOne(){assertNull(parse("How do I remind me tomorrow at 9?"))}
    @Test fun localTimeInDSTGapNeedsAnExplicitChoice(){val c=Clock.fixed(Instant.parse("2026-03-07T16:00:00Z"),ZoneId.of("America/New_York"));assertEquals(ReminderIntent.NeedsDetails,ReminderPolicy.parse("remind me tomorrow at 02:30 to read",c))}
    @Test fun repeatedTimeAtDSTEndNeedsAnExplicitChoice(){val c=Clock.fixed(Instant.parse("2026-10-31T16:00:00Z"),ZoneId.of("America/New_York"));assertEquals(ReminderIntent.NeedsDetails,ReminderPolicy.parse("remind me tomorrow at 01:30 to read",c))}
}
