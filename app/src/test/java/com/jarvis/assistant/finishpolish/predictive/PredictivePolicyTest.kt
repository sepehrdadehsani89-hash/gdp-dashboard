package com.jarvis.assistant.finishpolish.predictive

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class PredictivePolicyTest {
    private val now=Instant.parse("2026-11-11T17:00:00Z").toEpochMilli()
    private val config=PredictiveConfig(enabled=true,battery=true,calendar=true,quiet=false)
    private fun env(at:Long=now)=SuggestionEnvironment(at,"America/Guayaquil",true,true)
    private fun routine(at:Long=now+20*60_000L)=PredictiveRoutine("routine-one","Coding session","America/Guayaquil",onceAt=at,recipeTrigger="coding time",recipeId="approved-recipe")
    @Test fun optOutDoesNotOfferAnythingEvenWithRealInputs(){val s=PredictivePolicy.battery(9,false,now,now)!!;assertNull(PredictivePolicy.choose(listOf(s),config.copy(enabled=false),env(),SuggestionHistory()))}
    @Test fun actualBatteryReadingsNeedExplicitConsentFreshnessAndDischarge(){
        val s=PredictivePolicy.battery(10,false,now,now)!!;assertEquals(SuggestionActionKind.BATTERY_SETTINGS,s.action?.kind)
        assertNull(PredictivePolicy.battery(11,false,now,now));assertNull(PredictivePolicy.battery(null,false,now,now));assertNull(PredictivePolicy.battery(9,null,now,now));assertNull(PredictivePolicy.battery(9,true,now,now));assertNull(PredictivePolicy.battery(9,false,now-150_001,now));assertNull(PredictivePolicy.battery(9,false,now+1,now))
        assertNull(PredictivePolicy.choose(listOf(s),config.copy(battery=false),env(),SuggestionHistory()))
    }
    @Test fun routineUsesAnActualApprovedDatedOccurrenceAndPinsRecipeIdentity(){
        val r=routine();val s=PredictivePolicy.routine(r,now)!!
        assertEquals(r.onceAt,s.dueAt);assertEquals("approved-recipe",s.action?.referenceId);assertEquals("coding time",s.action?.target)
        assertNull(PredictivePolicy.routine(r,now-1));assertNull(PredictivePolicy.routine(r,requireNotNull(r.onceAt)+300_001))
    }
    @Test fun reminderOnlyRoutineNeverPretendsAnActionWasPrepared(){val s=PredictivePolicy.routine(routine().copy(recipeTrigger="",recipeId=""),now)!!;assertNull(s.action)}
    @Test fun quietHoursUseTheDeviceTimeZoneAndOvernightBoundaries(){
        val c=config.copy(quiet=true,quietStart=22,quietEnd=7)
        val evening=env(Instant.parse("2026-11-12T03:00:00Z").toEpochMilli())
        assertTrue(PredictivePolicy.quiet(c,evening));assertFalse(PredictivePolicy.quiet(c,evening.copy(zone="Asia/Tokyo")))
        assertTrue(PredictivePolicy.quiet(c,env(Instant.parse("2026-11-11T11:59:00Z").toEpochMilli())));assertFalse(PredictivePolicy.quiet(c,env(Instant.parse("2026-11-11T12:00:00Z").toEpochMilli())))
    }
    @Test fun deniedCalendarConsentCannotBeInventedByCandidateData(){
        val s=FutureSuggestion("calendar:1",SuggestionSource.CALENDAR,"calendar:1","Real event","Provider observation",now,now+60_000,now+120_000,SuggestionAction(SuggestionActionKind.REVIEW_CALENDAR,"1"))
        assertNull(PredictivePolicy.choose(listOf(s),config.copy(calendar=false),env(),SuggestionHistory()));assertNotNull(PredictivePolicy.choose(listOf(s),config,env(),SuggestionHistory()))
    }
    @Test fun lockedBusyDndSilentMusicAndBatterySaverAllSuppressSuggestions(){
        val s=PredictivePolicy.battery(9,false,now,now)!!
        for(e in listOf(env().copy(unlocked=false),env().copy(idle=false),env().copy(dnd=true),env().copy(silent=true),env().copy(music=true),env().copy(powerSave=true)))assertNull(PredictivePolicy.choose(listOf(s),config,e,SuggestionHistory()))
    }
    @Test fun oneSuggestionIsOfferedAndRepeatedOccurrenceIsDeduplicated(){
        val first=PredictivePolicy.routine(routine(),now)!!;val second=PredictivePolicy.routine(routine(now+10*60_000).copy(id="second"),now)!!
        assertEquals(second.id,PredictivePolicy.choose(listOf(first,second),config,env(),SuggestionHistory())?.id)
        val h=PredictivePolicy.shown(second,config,env(),SuggestionHistory());assertNull(PredictivePolicy.choose(listOf(first,second),config,env(),h))
        assertNull(PredictivePolicy.choose(listOf(second),config,env(now+31*60_000),h))
    }
    @Test fun snoozedNeverAndExpiredSuggestionsNeverRun(){
        val s=PredictivePolicy.routine(routine(),now)!!
        assertFalse(PredictivePolicy.eligible(s,config,env(),SuggestionHistory(snoozed=mapOf(s.sourceKey to now+30*60_000))))
        assertFalse(PredictivePolicy.eligible(s,config,env(),SuggestionHistory(never=setOf(s.sourceKey))))
        assertFalse(PredictivePolicy.eligible(s,config,env(s.expiresAt),SuggestionHistory()))
    }
    @Test fun dailyCapAndClockRollbackPreventNotificationLoops(){
        val s=PredictivePolicy.routine(routine(),now)!!
        val day=Instant.ofEpochMilli(now).atZone(ZoneId.of("America/Guayaquil")).toLocalDate().toString()
        assertNull(PredictivePolicy.choose(listOf(s),config,env(),SuggestionHistory(shownDay=day,shownToday=3)))
        assertNull(PredictivePolicy.choose(listOf(s),config,env(),SuggestionHistory(lastShown=now+1)))
    }
    @Test fun datedClockChangeGapAndOverlapRequireUserChoiceRatherThanGuessing(){
        for(local in listOf("2026-03-08T02:30","2026-11-01T01:30")){try{PredictivePolicy.exactTime(local,"America/New_York");fail("Ambiguous/nonexistent time accepted") }catch(_:IllegalArgumentException){}}
        assertEquals(Instant.parse("2026-11-11T21:00:00Z").toEpochMilli(),PredictivePolicy.exactTime("2026-11-11T16:00","America/Guayaquil"))
    }
    @Test fun repeatingRoutinesSkipDstAnomaliesAndHonorWeekdaySelection(){
        val before=Instant.parse("2026-03-08T05:00:00Z").toEpochMilli()
        val r=PredictiveRoutine("daily","Daily routine","America/New_York",dailyTime="02:30")
        val next=PredictivePolicy.occurrence(r,before)!!
        assertEquals(LocalDate.of(2026,3,9),Instant.ofEpochMilli(next).atZone(ZoneId.of(r.zone)).toLocalDate())
        val friday=r.copy(weekdays=setOf(5));assertEquals(5,Instant.ofEpochMilli(PredictivePolicy.occurrence(friday,before)!!).atZone(ZoneId.of(r.zone)).dayOfWeek.value)
    }
    @Test fun encryptedDocumentCodecRoundTripKeepsConsentAndSuppressionSeparate(){
        val p=PredictiveProfile(config,listOf(routine()),SuggestionHistory(seen=mapOf("event" to now),snoozed=mapOf("routine:one" to now+60_000),never=setOf("battery-low"),lastShown=now,shownDay="2026-11-11",shownToday=1,accepted=2,dismissed=3))
        assertEquals(p,PredictiveCodec.decode(PredictiveCodec.encode(p)))
        val raw=PredictiveCodec.encode(p).replace("\"version\":1","\"version\":2")
        try{PredictiveCodec.decode(raw);fail("Unknown schema accepted")}catch(_:IllegalArgumentException){}
    }
    @Test fun twentyDistinctApprovedRoutinesKeepIndependentOccurrenceIds(){
        val list=(1..20).map{routine().copy(id="routine-$it",title="Routine $it")}.map{PredictivePolicy.routine(it,now)!!}
        assertEquals(20,list.map{it.id}.distinct().size);assertEquals(20,list.map{it.sourceKey}.distinct().size)
    }
}
