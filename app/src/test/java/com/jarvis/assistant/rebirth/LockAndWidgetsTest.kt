package com.jarvis.assistant.rebirth

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.security.LockCountdown
import com.jarvis.assistant.widgets.WidgetHostPolicy
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LockAndWidgetsTest {
    @Test fun lockingWaitsForActualSpeechCompletionAndNeverAddsASecondSpokenCountdown()=runTest {
        val audio=CompletableDeferred<Boolean>();var locks=0;val ticks=mutableListOf<Int>()
        val job=launch{LockCountdown.run({true},{audio.await()},{ticks+=it},{locks++})}
        runCurrent();assertEquals(0,locks)
        advanceTimeBy(3000);runCurrent();assertEquals(0,locks)
        audio.complete(true);job.join();assertEquals(1,locks);assertTrue(ticks.isEmpty())
    }
    @Test fun unavailableSpeechUsesThreeRealSecondsBeforeTheLock()=runTest {
        var locks=0;val ticks=mutableListOf<Int>()
        val job=launch{LockCountdown.run({true},{false},{ticks+=it},{locks++})}
        runCurrent();assertEquals(listOf(3),ticks);assertEquals(0,locks)
        advanceTimeBy(1000);runCurrent();assertEquals(listOf(3,2),ticks)
        advanceTimeBy(1000);runCurrent();assertEquals(listOf(3,2,1),ticks);assertEquals(0,locks)
        advanceTimeBy(1000);runCurrent();job.join();assertEquals(1,locks)
    }
    @Test fun stopWhileSpeechIsPendingPreventsAnyLock()=runTest {
        val audio=CompletableDeferred<Boolean>();var locks=0
        val job=launch{LockCountdown.run({true},{audio.await()},{},{locks++})}
        runCurrent();job.cancelAndJoin();audio.complete(true);advanceUntilIdle();assertEquals(0,locks)
    }
    @Test fun aNewCommandInvalidatesAnOldCountdownEvenIfItsAudioFinishesLate()=runTest {
        var generation=1;val audio=CompletableDeferred<Boolean>();var locks=0
        val job=launch{assertFalse(LockCountdown.run({generation==1},{audio.await()},{},{locks++}))}
        runCurrent();generation=2;audio.complete(true);job.join();assertEquals(0,locks)
    }
    @Test fun cancellationDuringTheVisualTimerPreventsTheAction()=runTest {
        var locks=0;val job=launch{LockCountdown.run({true},{false},{},{locks++})}
        runCurrent();advanceTimeBy(1500);runCurrent();job.cancelAndJoin();advanceUntilIdle();assertEquals(0,locks)
    }
    @Test fun revokedReadinessDoesNotAnnounceOrDispatchAnAction()=runTest {
        var announcements=0;var locks=0
        assertFalse(LockCountdown.run({false},{announcements++;true},{},{locks++}))
        assertEquals(0,announcements);assertEquals(0,locks)
    }
    @Test fun lockAnnouncementMatchesTheRequestedEnglishAndSpanishPath(){
        assertEquals("Locking initiated, locking your screen in 3, 2, 1.",LockCountdown.announcement(AppLanguage.EN))
        assertTrue(LockCountdown.announcement(AppLanguage.ES).contains("3, 2, 1"))
    }
    @Test fun savedIdsCannotAdoptAnotherLaunchersWidgetsOrMalformedIds(){
        val approved=WidgetHostPolicy.restored(setOf("1","01","2","-1","garbage","777"),setOf(1,2)){true}
        assertEquals(listOf(1,2),approved)
    }
    @Test fun removedOrUnboundProvidersCannotAppearAsApprovedAccess(){
        assertEquals(listOf(2),WidgetHostPolicy.restored(setOf("1","2"),setOf(1,2)){it==2})
    }
    @Test fun widgetRestorationIsBoundedAndDeterministic(){
        val result=WidgetHostPolicy.restored((1..100).map(Int::toString).toSet(),(1..100).toSet()){true}
        assertEquals((1..WidgetHostPolicy.MAX_WIDGETS).toList(),result)
    }
    @Test fun widgetsNavigationDoesNotRemoveAnyJarvisModule(){
        assertEquals(NativeNavigation.Destination.WIDGETS,NativeNavigation.destination("Open my widgets"))
        assertEquals(NativeNavigation.Destination.WIDGETS,NativeNavigation.destination("Abre mis widgets"))
        assertNull(NativeNavigation.destination("Explain Android widgets"))
        assertTrue(com.jarvis.assistant.omnisystem.HudModule.entries.map{it.name}.containsAll(listOf("WEATHER","CLOCK","BATTERY","CAMERA","MUSIC","BLUETOOTH")))
    }
    @Test fun widgetResultsBelongToOneRequestAndOneRuntimeSession(){
        assertTrue(WidgetHostPolicy.currentResult(32001,32001,10,10))
        assertFalse(WidgetHostPolicy.currentResult(32002,32001,10,10))
        assertFalse(WidgetHostPolicy.currentResult(32001,32001,10,11))
        assertFalse(WidgetHostPolicy.currentResult(-1,-1,10,10))
        assertFalse(WidgetHostPolicy.currentResult(32001,32001,-1,-1))
    }
}
