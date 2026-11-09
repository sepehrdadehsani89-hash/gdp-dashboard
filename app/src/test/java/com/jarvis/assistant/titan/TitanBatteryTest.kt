package com.jarvis.assistant.titan

import com.jarvis.assistant.core.AppLanguage
import org.junit.Assert.*
import org.junit.Test

class TitanBatteryTest {
    @Test fun everyPercentageHasExactlyTheSpecifiedBand(){
        for(p in 0..100){val expected=when{p>=70->BatteryBand.NORMAL;p>=31->BatteryBand.MODERATE;p>=11->BatteryBand.LOW;p>=6->BatteryBand.VERY_LOW;else->BatteryBand.CRITICAL};assertEquals("$p%",expected,BatteryPolicy.band(p))}
        assertEquals(BatteryBand.UNKNOWN,BatteryPolicy.band(null));assertEquals(BatteryBand.UNKNOWN,BatteryPolicy.band(-1));assertEquals(BatteryBand.UNKNOWN,BatteryPolicy.band(101))
        assertEquals(0xFFF8FAFC,BatteryBand.NORMAL.argb);assertEquals(0xFFFF1744,BatteryBand.CRITICAL.argb)
    }
    @Test fun downwardObservedLevelsAreAnnouncedOnceIncludingAfterRestart(){
        var state=BatteryAlertState()
        state=BatteryAlertPolicy.observe(state,11,false).state
        for(p in 10 downTo 1){val update=BatteryAlertPolicy.observe(state,p,false);assertEquals(p,update.candidate);state=BatteryAlertPolicy.reserve(update.state,p)
            val restored=BatteryAlertState(state.previous,state.powered,state.announcedMask,state.episode)
            assertNull(BatteryAlertPolicy.observe(restored,p,false).candidate)
        }
        assertNull(BatteryAlertPolicy.observe(state,0,false).candidate)
    }
    @Test fun skippedLevelsAreNeverReplayed(){
        val update=BatteryAlertPolicy.observe(BatteryAlertState(previous=11),8,false)
        assertEquals(8,update.candidate);val state=BatteryAlertPolicy.reserve(update.state,8)
        assertEquals(1 shl 8,state.announcedMask);assertNull(BatteryAlertPolicy.observe(state,8,false).candidate)
    }
    @Test fun chargingSuppressesAndGenuineDischargeStartsANewEpisode(){
        val old=BatteryAlertState(5,false,1 shl 5,3)
        val charge=BatteryAlertPolicy.observe(old,6,true);assertNull(charge.candidate)
        val unplug=BatteryAlertPolicy.observe(charge.state,6,false);assertNull(unplug.candidate);assertEquals(4L,unplug.state.episode)
        assertEquals(5,BatteryAlertPolicy.observe(unplug.state,5,false).candidate)
    }
    @Test fun bouncingReadingsDoNotRepeatAndRecoveryAllowsNewAlerts(){
        var state=BatteryAlertPolicy.reserve(BatteryAlertState(previous=8),8)
        state=BatteryAlertPolicy.observe(state,9,false).state
        assertNull(BatteryAlertPolicy.observe(state,8,false).candidate)
        val recovered=BatteryAlertPolicy.observe(state,15,false).state
        assertEquals(0,recovered.announcedMask);assertEquals(10,BatteryAlertPolicy.observe(recovered,10,false).candidate)
    }
    @Test fun coldStartAndUnknownSamplesNeverInventADecrement(){
        assertNull(BatteryAlertPolicy.observe(BatteryAlertState(),5,false).candidate)
        val state=BatteryAlertState(9,false,512,4)
        assertEquals(state,BatteryAlertPolicy.observe(state,null,true).state)
        assertNull(BatteryAlertPolicy.observe(state,900,false).candidate)
    }
    @Test fun exactEnglishWarningsAndCriticalBoundary(){
        assertEquals("Please charge your phone. Battery percentage is at ten percent.",BatteryPolicy.phrase(10,AppLanguage.EN))
        assertEquals("Please charge your phone immediately. Critical battery percentage is at five percent.",BatteryPolicy.phrase(5,AppLanguage.EN))
        assertTrue(BatteryPolicy.phrase(6,AppLanguage.EN).contains("six percent"));assertFalse(BatteryPolicy.phrase(6,AppLanguage.EN).contains("Critical"))
        assertTrue(BatteryPolicy.phrase(1,AppLanguage.ES).contains("inmediatamente"));assertTrue(BatteryPolicy.phrase(9,AppLanguage.ES).contains("nueve"))
    }
    @Test fun quietHoursCoverOvernightAndDaytimeWithoutSuppressingAllDay(){
        assertTrue(BatteryPolicy.quiet(23,22,7));assertTrue(BatteryPolicy.quiet(6,22,7));assertFalse(BatteryPolicy.quiet(7,22,7))
        assertTrue(BatteryPolicy.quiet(12,9,17));assertFalse(BatteryPolicy.quiet(18,9,17));assertFalse(BatteryPolicy.quiet(8,8,8))
    }
}
