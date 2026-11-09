package com.jarvis.assistant.power

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.hud.BatteryReading
import com.jarvis.assistant.hud.HudSettings
import com.jarvis.assistant.sentinel.SentinelMode
import com.jarvis.assistant.sentinel.WallpaperRenderPolicy
import com.jarvis.assistant.titan.BatteryPolicy
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class OverchargePolicyTest {
    private val config=PowerAlertConfig()
    private fun reading(percent:Int?,condition:PowerCondition=PowerCondition.DISCHARGING,estimate:Long?=null)=PowerReading(
        BatteryReading(percent,condition==PowerCondition.CHARGING,condition==PowerCondition.FULL,if(condition in setOf(PowerCondition.CHARGING,PowerCondition.FULL,PowerCondition.PLUGGED_NOT_CHARGING))"USB" else "Battery"),
        condition,condition in setOf(PowerCondition.CHARGING,PowerCondition.FULL,PowerCondition.PLUGGED_NOT_CHARGING),estimate?.let{ChargingEstimate(it,1000)},1000)
    private fun start(r:PowerReading)=PowerAlertPolicy.observe(PowerAlertState(),r)
    private fun next(s:PowerAlertState,r:PowerReading,now:Long=1000,c:PowerAlertConfig=config)=PowerAlertPolicy.next(s,r,c,now)
    private fun assertCritical(level:Int){
        val before=reading(level+1);var s=start(before)
        val current=reading(level);s=PowerAlertPolicy.observe(s,current)
        val alert=requireNotNull(next(s,current));assertEquals(PowerAlertKind.CRITICAL,alert.kind);assertEquals(level,alert.level)
        val requested=PowerAlertPolicy.requested(s,alert,1000);assertEquals(0,requested.mask and (1 shl level))
        val done=PowerAlertPolicy.completed(requested,alert,config,1500)
        val restored=requireNotNull(PowerAlertCodec.decode(PowerAlertCodec.encode(done)))
        assertNull(next(PowerAlertPolicy.observe(restored,current),current,5000))
    }
    @Test fun threePercentIsAcknowledgedOnlyAfterPlaybackAndNotRepeatedAfterRestart(){assertCritical(3)}
    @Test fun twoPercentIsAcknowledgedOnlyAfterPlaybackAndNotRepeatedAfterRestart(){assertCritical(2)}
    @Test fun onePercentIsAcknowledgedOnlyAfterPlaybackAndNotRepeatedAfterRestart(){assertCritical(1)}
    @Test fun everyObservedThresholdFromTenToOneRemainsAvailable(){
        var s=start(reading(11))
        for(level in 10 downTo 1){val r=reading(level);s=PowerAlertPolicy.observe(s,r);val a=requireNotNull(next(s,r));assertEquals(level,a.level);s=PowerAlertPolicy.completed(PowerAlertPolicy.requested(s,a,1000),a,config,1500)}
    }
    @Test fun onePercentUsesTheRequestedSpecialEnglishPhrase(){
        assertEquals("Sir, battery percentage critical. Please charge your phone immediately. Battery percentage is at one percent.",BatteryPolicy.phrase(1,AppLanguage.EN))
        assertTrue(BatteryPolicy.phrase(1,AppLanguage.ES).contains("Señor"));assertTrue(BatteryPolicy.phrase(3,AppLanguage.ES).contains("tres"))
    }
    @Test fun skippedFourToTwoProducesOnlyTheObservedTwoPercentAlert(){
        val r=reading(2);val s=PowerAlertPolicy.observe(start(reading(4)),r);val a=requireNotNull(next(s,r))
        assertEquals(2,a.level);val done=PowerAlertPolicy.completed(s,a,config,1000)
        assertEquals(1 shl 2,done.mask);assertNull(next(done,r));assertEquals(0,done.mask and (1 shl 3))
    }
    @Test fun aNewMoreCriticalReadingReplacesTheOlderFailedCandidate(){
        var s=start(reading(3));val a=requireNotNull(next(s,reading(3)));s=PowerAlertPolicy.failed(PowerAlertPolicy.requested(s,a,1000),a,1500)
        s=PowerAlertPolicy.observe(s,reading(2));val newer=requireNotNull(next(s,reading(2),2000));assertEquals(2,newer.level);assertNotEquals(a.key,newer.key)
    }
    @Test fun failureCanRetryWithoutClaimingThatSpeechWasHeard(){
        val r=reading(3);val s=start(r);val a=requireNotNull(next(s,r));val failed=PowerAlertPolicy.failed(PowerAlertPolicy.requested(s,a,1000),a,1500)
        assertEquals(0,failed.mask);assertNull(next(failed,r,31_499));assertEquals(a,next(failed,r,31_500))
    }
    @Test fun processDeathAfterRequestRespectsTheDurableCooldown(){
        val r=reading(2);val s=start(r);val a=requireNotNull(next(s,r));val requested=PowerAlertPolicy.requested(s,a,1000)
        val restored=requireNotNull(PowerAlertCodec.decode(PowerAlertCodec.encode(requested)))
        assertNull(next(PowerAlertPolicy.observe(restored,r),r,2000));assertEquals(a,next(restored,r,121_000));assertEquals(0,restored.mask)
    }
    @Test fun failedSpeechHasAtMostThreeAttemptsForTheSameLevel(){
        val r=reading(1);var s=start(r);var now=1000L
        repeat(3){val a=requireNotNull(next(s,r,now));s=PowerAlertPolicy.failed(PowerAlertPolicy.requested(s,a,now),a,now);now+=120_001}
        assertNull(next(s,r,now));assertEquals(0,s.mask)
        s=PowerAlertPolicy.observe(s,reading(15));s=PowerAlertPolicy.observe(s,reading(1));assertNotNull(next(s,reading(1),now))
    }
    @Test fun chargingAtTwoPercentSuppressesCriticalDischargeSpeech(){
        val r=reading(2,PowerCondition.CHARGING);val a=next(PowerAlertPolicy.observe(start(reading(3)),r),r)
        assertEquals(PowerAlertKind.START,a?.kind)
    }
    @Test fun firstReliableCriticalReadingCanRecoverWithoutAnEarlierBroadcast(){
        val r=reading(3);assertEquals(3,next(start(r),r)?.level)
        val unknown=r.copy(condition=PowerCondition.UNKNOWN,connected=null)
        val known=PowerAlertPolicy.observe(start(unknown),r);assertEquals(3,next(known,r)?.level)
    }
    @Test fun unknownOrMissingReadingsCannotInventLowBatterySpeech(){
        assertNull(next(start(reading(null)),reading(null)))
        val unknown=reading(2).copy(condition=PowerCondition.UNKNOWN,connected=null)
        assertNull(next(start(unknown),unknown));assertNull(next(start(reading(0)),reading(0)))
    }
    @Test fun disablingCriticalOrAllAlertsIsRespected(){
        val r=reading(3);assertNull(next(start(r),r,c=config.copy(critical=false)));assertNull(next(start(r),r,c=config.copy(enabled=false)))
        val nine=reading(9);assertEquals(9,next(start(nine),nine,c=config.copy(critical=false))?.level)
    }
    @Test fun newDischargeEpisodeCanWarnAgainAfterCharging(){
        val r=reading(3);var s=start(r);val a=requireNotNull(next(s,r));s=PowerAlertPolicy.completed(s,a,config,1000)
        s=PowerAlertPolicy.observe(s,reading(3,PowerCondition.CHARGING));val episode=s.episode
        s=PowerAlertPolicy.observe(s,r);assertEquals(episode+1,s.episode);assertEquals(3,next(s,r)?.level)
    }
    @Test fun pluggedButNotChargingDoesNotProduceAChargingStart(){
        val r=reading(60,PowerCondition.PLUGGED_NOT_CHARGING);assertNull(next(start(r),r));assertTrue(ChargingText.answer(r,1000,0,"UTC",AppLanguage.EN).contains("not charging"))
    }
    @Test fun actualChargingTransitionProducesOneStartAnnouncement(){
        var s=start(reading(40,PowerCondition.PLUGGED_NOT_CHARGING));val r=reading(40,PowerCondition.CHARGING)
        s=PowerAlertPolicy.observe(s,r);val a=requireNotNull(next(s,r));assertEquals(PowerAlertKind.START,a.kind)
        s=PowerAlertPolicy.completed(s,a,config,1000)
        repeat(5){s=PowerAlertPolicy.observe(s,r);assertNull(next(s,r))}
    }
    @Test fun estimateArrivingAfterTheStartAllowsOneUpdatedAnnouncement(){
        var r=reading(40,PowerCondition.CHARGING);var s=start(r);s=PowerAlertPolicy.completed(s,requireNotNull(next(s,r)),config,1000)
        r=r.copy(estimate=ChargingEstimate(1_000_000,1000));s=PowerAlertPolicy.observe(s,r)
        val a=requireNotNull(next(s,r));assertEquals(PowerAlertKind.ESTIMATE,a.kind);s=PowerAlertPolicy.completed(s,a,config,2000)
        r=r.copy(estimate=ChargingEstimate(900_000,2000));assertNull(next(PowerAlertPolicy.observe(s,r),r))
    }
    @Test fun chargingStateAndPercentageAreBothRequiredForAFullAnnouncement(){
        for(condition in PowerCondition.entries){val r=reading(100,condition);assertEquals(condition in setOf(PowerCondition.CHARGING,PowerCondition.FULL),r.verifiedFull)}
        assertFalse(reading(99,PowerCondition.FULL).verifiedFull);assertFalse(reading(null,PowerCondition.FULL).verifiedFull)
    }
    @Test fun repeatedHundredPercentBroadcastsDoNotRepeatTheInitialFullSpeech(){
        val r=reading(100,PowerCondition.FULL);var s=start(r);val a=requireNotNull(next(s,r));assertEquals(PowerAlertKind.FULL,a.kind)
        s=PowerAlertPolicy.completed(PowerAlertPolicy.requested(s,a,1000),a,config,2000)
        repeat(10){s=PowerAlertPolicy.observe(s,r);assertNull(next(s,r,3000))}
        assertEquals(902_000,s.reminderAt)
    }
    @Test fun fullReminderIsShortPeriodicAndRescheduledFromActualCompletion(){
        val r=reading(100,PowerCondition.FULL);var s=start(r);s=PowerAlertPolicy.completed(s,requireNotNull(next(s,r)),config,2000)
        val a=requireNotNull(next(s,r,902_000));assertEquals(PowerAlertKind.REMINDER,a.kind)
        s=PowerAlertPolicy.completed(s,a,config,905_000);assertEquals(1_805_000,s.reminderAt);assertNull(next(s,r,905_000))
    }
    @Test fun fullSessionAndDueTimeSurviveRestart(){
        val r=reading(100,PowerCondition.FULL);var s=start(r);s=PowerAlertPolicy.completed(s,requireNotNull(next(s,r)),config,2000)
        s=requireNotNull(PowerAlertCodec.decode(PowerAlertCodec.encode(s)));s=PowerAlertPolicy.observe(s,r)
        assertNull(next(s,r,4000));assertEquals(PowerAlertKind.REMINDER,next(s,r,902_000)?.kind)
    }
    @Test fun overdueOvernightWorkCreatesOneReminderWithoutACatchUpBurst(){
        val r=reading(100,PowerCondition.FULL);var s=start(r);s=PowerAlertPolicy.completed(s,requireNotNull(next(s,r)),config,2000)
        val muchLater=50_000_000L;val a=requireNotNull(next(s,r,muchLater));s=PowerAlertPolicy.completed(s,a,config,muchLater)
        assertNull(next(s,r,muchLater));assertEquals(muchLater+900_000,s.reminderAt)
    }
    @Test fun unpluggingCancelsTheOldFullSessionAndItsScheduledCheck(){
        val full=reading(100,PowerCondition.FULL);var s=start(full);val oldAlert=requireNotNull(next(s,full));s=PowerAlertPolicy.completed(s,oldAlert,config,2000)
        val unplugged=reading(99);s=PowerAlertPolicy.observe(s,unplugged)
        assertNull(next(s,unplugged,1_000_000));assertNull(PowerAlertPolicy.fullCheckAt(s,unplugged,config,1_000_000))
        val replug=PowerAlertPolicy.observe(s,full);assertTrue(replug.session>s.session);assertEquals(PowerAlertKind.FULL,next(replug,full)?.kind)
    }
    @Test fun dismissedFullSessionStaysDismissedUntilUnplugged(){
        val r=reading(100,PowerCondition.FULL);var s=PowerAlertPolicy.dismiss(start(r));s=PowerAlertPolicy.observe(s,r)
        assertNull(next(s,r,99_000_000));assertNull(PowerAlertPolicy.fullCheckAt(s,r,config,99_000_000))
        s=PowerAlertPolicy.observe(s,reading(99));s=PowerAlertPolicy.observe(s,r);assertNotNull(next(s,r))
    }
    @Test fun snoozeUsesOnePersistentFutureDeadline(){
        val r=reading(100,PowerCondition.FULL);val s=PowerAlertPolicy.snooze(start(r),1000)
        assertNull(next(s,r,900_999));assertEquals(901_000L,PowerAlertPolicy.fullCheckAt(s,r,config,1000));assertNotNull(next(s,r,901_000))
    }
    @Test fun disabledFullAlertsCannotLeakThroughTheChargingStartRoute(){
        val r=reading(100,PowerCondition.CHARGING);assertNull(next(start(r),r,c=config.copy(full=false)))
        assertNull(PowerAlertPolicy.fullCheckAt(start(r),r,config.copy(full=false),1000))
    }
    @Test fun disabledRepeatsKeepOnlyTheInitialFullAnnouncement(){
        val r=reading(100,PowerCondition.FULL);val c=config.copy(repeatMinutes=0);var s=start(r)
        s=PowerAlertPolicy.completed(s,requireNotNull(next(s,r,c=c)),c,1000);assertEquals(0,s.reminderAt)
        assertNull(next(s,r,999_000_000,c));assertNull(PowerAlertPolicy.fullCheckAt(s,r,c,1000))
    }
    @Test fun lateCompletionFromAnOldChargingSessionCannotCompleteANewerOne(){
        val r=reading(100,PowerCondition.FULL);var s=start(r);val old=requireNotNull(next(s,r))
        s=PowerAlertPolicy.observe(s,reading(99));s=PowerAlertPolicy.observe(s,r)
        assertFalse(PowerAlertPolicy.completed(s,old,config,2000).fullDone)
    }
    @Test fun temporaryUnknownDataDoesNotInventANewChargingSession(){
        val r=reading(80,PowerCondition.CHARGING);var s=start(r);val id=s.session
        s=PowerAlertPolicy.observe(s,r.copy(condition=PowerCondition.UNKNOWN,connected=null));s=PowerAlertPolicy.observe(s,r);assertEquals(id,s.session)
    }
    @Test fun failuresAtFullChargeDoNotScheduleAnInfiniteWorkerLoop(){
        val r=reading(100,PowerCondition.FULL);var s=start(r);var now=1000L
        repeat(3){val a=requireNotNull(next(s,r,now));s=PowerAlertPolicy.failed(PowerAlertPolicy.requested(s,a,now),a,now);now+=120_001}
        assertNull(PowerAlertPolicy.fullCheckAt(s,r,config,now))
    }
    @Test fun progressReportsAreOptInAndUseTheRealSessionSchedule(){
        val r=reading(40,PowerCondition.CHARGING,3_000_000);val c=config.copy(progressMinutes=15);var s=start(r)
        s=PowerAlertPolicy.completed(s,requireNotNull(next(s,r,c=c)),c,1000)
        assertNull(next(s,r,900_999,c));val a=requireNotNull(next(s,r,901_000,c));assertEquals(PowerAlertKind.PROGRESS,a.kind)
        s=PowerAlertPolicy.completed(s,a,c,902_000);assertEquals(1_802_000,s.progressAt)
        assertNull(next(s,r,2_000_000,config))
    }
    @Test fun unpluggedAndNotChargingStatesNeverRetainAUsableEta(){
        val filter=ChargingEstimateFilter();assertNotNull(filter.update(3_000_000,true,1000))
        assertNull(filter.update(3_000_000,false,2000));assertNull(filter.update(-1,true,3000));assertNull(filter.update(0,true,4000));assertNull(filter.update(Long.MAX_VALUE,true,5000))
    }
    @Test fun missingAndroidEtaNeverProducesAnInventedFinishTime(){
        val r=reading(3,PowerCondition.CHARGING)
        val answer=ChargingText.answer(r,1000,0,"UTC",AppLanguage.EN)
        assertTrue(answer.contains("three percent"));assertTrue(answer.contains("isn't available yet"));assertFalse(answer.contains("Estimated finish:"))
    }
    @Test fun singleLargeEtaSpikeRequiresASecondConsistentMeasurement(){
        val filter=ChargingEstimateFilter();val first=filter.update(5_000_000,true,1000)
        assertEquals(first,filter.update(14_000_000,true,2000));val sustained=filter.update(14_010_000,true,3000)
        assertEquals(14_010_000L,sustained?.remainingMs)
    }
    @Test fun etaCanChangeWithoutAnyPercentageChangeAndUnavailableClearsIt(){
        val filter=ChargingEstimateFilter();filter.update(5_000_000,true,1000)
        assertEquals(4_940_000L,filter.update(4_940_000,true,61_000)?.remainingMs)
        assertNull(filter.update(null,true,62_000))
    }
    @Test fun staleOrExpiredEstimatesDisappearInsteadOfClaimingCompletion(){
        assertNull(ChargingEstimate(4_000_000,1000).remaining(151_001))
        assertNull(ChargingEstimate(10_000,1000).remaining(11_000));assertNull(ChargingEstimate(10_000,1000).remaining(999))
    }
    @Test fun durationsUseNaturalRoundedEnglishAndSpanish(){
        assertEquals("two hours and five minutes",ChargingText.duration(125*60_000L))
        assertEquals("forty-three minutes",ChargingText.duration(43*60_000L));assertEquals("eight minutes",ChargingText.duration(8*60_000L))
        assertEquals("2 horas y 5 minutos",ChargingText.duration(125*60_000L,true))
        assertEquals("one minute",ChargingText.duration(1000))
    }
    @Test fun estimatedCompletionCrossingMidnightIncludesTheNewDate(){
        val epoch=Instant.parse("2026-11-09T23:50:00Z").toEpochMilli()
        val value=ChargingText.completion(20*60_000L,epoch,"UTC");assertTrue(value.contains("10 Nov"));assertTrue(value.contains("00:10"))
        assertEquals("19:10",ChargingText.completion(20*60_000L,epoch,"America/Guayaquil"))
    }
    @Test fun statusParsingDoesNotConfusePluggedWithCharging(){
        assertEquals(PowerCondition.CHARGING,PowerReading.condition(2,1));assertEquals(PowerCondition.FULL,PowerReading.condition(5,1))
        assertEquals(PowerCondition.PLUGGED_NOT_CHARGING,PowerReading.condition(4,1));assertEquals(PowerCondition.DISCHARGING,PowerReading.condition(3,0))
        assertEquals(PowerCondition.UNKNOWN,PowerReading.condition(2,0));assertEquals(PowerCondition.UNKNOWN,PowerReading.condition(2,null))
    }
    @Test fun audioPolicyHonorsQuietHoursCallsSilentDndAndBusySessions(){
        fun blocked(quiet:Boolean=false,permission:Boolean=true,idle:Boolean=true,normal:Boolean=true,silent:Boolean=false,dnd:Boolean=false,ready:Boolean=true)=PowerAudioPolicy.blocked(quiet,23,22,7,permission,idle,normal,silent,dnd,ready)
        assertNull(blocked());assertNotNull(blocked(quiet=true));assertNotNull(blocked(permission=false));assertNotNull(blocked(idle=false));assertNotNull(blocked(normal=false))
        assertNotNull(blocked(silent=true));assertNotNull(blocked(dnd=true));assertNotNull(blocked(ready=false))
    }
    @Test fun alertMetadataRoundTripDoesNotContainConversationOrRawAudio(){
        val r=reading(3);val s=PowerAlertPolicy.requested(start(r),requireNotNull(next(start(r),r)),1000)
        val encoded=PowerAlertCodec.encode(s);assertEquals(s,PowerAlertCodec.decode(encoded));assertFalse(encoded.contains("transcript"));assertFalse(encoded.contains("audio"))
        assertNull(PowerAlertCodec.decode("not json"));assertNull(PowerAlertCodec.decode("x".repeat(20_000)))
    }
    @Test fun batteryCommandsUseNativeRoutesWithoutHijackingOtherTimersOrTools(){
        listOf("how long until full","is my phone charging","what's up with my battery","when will my phone be fully charged").forEach{assertEquals(it,PowerCommand.STATUS,PowerCommands.parse(it))}
        assertEquals(PowerCommand.OPEN,PowerCommands.parse("take me to the Power Core"));assertEquals(PowerCommand.DISMISS,PowerCommands.parse("stop charging reminders"))
        assertEquals(PowerCommand.SNOOZE,PowerCommands.parse("remind me later",true));assertNull(PowerCommands.parse("remind me later",false))
        assertNull(PowerCommands.parse("how much time is left",false));assertEquals(PowerCommand.STATUS,PowerCommands.parse("how much time is left",true))
        assertNull(PowerCommands.parse("turn off Bluetooth"));assertNull(PowerCommands.parse("lock my phone"))
    }
    @Test fun chargingLayerRotatesContinuouslyAndDoesNotDependOnSpeechPhase(){
        val clock=ChargingVisualClock();clock.frame(0,true);var last=0f
        for(i in 1..30){val frame=clock.frame(i*50L,true);assertTrue(frame.angle>=last);last=frame.angle}
        assertTrue(last>0);clock.pause();assertEquals(last,clock.frame(50_000,true).angle,0f)
        assertTrue(clock.frame(50_050,true).angle>last)
    }
    @Test fun chargingFadeAndReducedMotionLeaveAnAccurateStaticState(){
        val clock=ChargingVisualClock();clock.frame(0,true);assertTrue(clock.frame(200,true).alpha in 0f..1f)
        assertEquals(1f,clock.frame(400,true,false).alpha,0f);assertEquals(0f,clock.frame(500,false,false).alpha,0f)
    }
    @Test fun powerStripClockAndCaptionsDoNotOverlapAcrossPhoneAndLandscapeSizes(){
        for((w,h) in listOf(360f to 640f,1080f to 2340f,1080f to 1200f,1920f to 1080f)){
            val cy=h*.46f;val radius=PowerWallpaperGeometry.radius(w,h,minOf(w*.43f,h*.32f)*1.1f,cy,true)
            val layout=PowerWallpaperGeometry.layout(w,h,cy,radius,true,true)
            assertTrue(layout.stripTop>=cy+radius);assertTrue(layout.captionTop>=layout.stripTop+layout.stripHeight)
            assertTrue("${w}x$h",layout.captionHeight>0);assertTrue(layout.captionTop+layout.captionHeight<=h)
            if(w<=h){assertTrue(layout.clockY>layout.stripTop+layout.stripHeight);assertTrue(layout.captionTop>layout.dateY)}
        }
    }
    @Test fun chargingColorDoesNotOverwriteThePercentageBand(){
        assertEquals(0xFFFF1744,BatteryPolicy.band(3).argb);assertEquals(0xFFF8FAFC,BatteryPolicy.band(80).argb)
        assertEquals(20,WallpaperRenderPolicy.fps(true,true,true,HudSettings(),BatteryReading(80,true),false,true,SentinelMode.DORMANT))
        assertEquals(0,WallpaperRenderPolicy.fps(false,true,true,HudSettings(),BatteryReading(80,true),false,true,SentinelMode.SPEAKING))
    }
    @Test fun renderDiagnosticsStayUnavailableUntilRealSamplesAreRecorded(){
        val timing=RenderMeasurements();assertTrue(timing.summary().contains("not measured"));repeat(130){timing.record(50,3)}
        assertTrue(timing.summary().contains("130 observed frames"));assertTrue(timing.summary().contains("50 ms"));assertTrue(timing.summary().contains("not GPU completion"))
    }
}
