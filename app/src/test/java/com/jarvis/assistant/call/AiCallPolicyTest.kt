package com.jarvis.assistant.call

import com.jarvis.assistant.voice.*
import org.junit.Assert.*
import org.junit.Test

class AiCallPolicyTest {
    @Test fun immediateCallPhrasesAreSeparateFromDatedRemindersAndTelephoneContacts(){
        for(text in listOf("call me","call me now","Hey Jarvis, call me now","let's do a call","let’s do a call","llámame ahora"))assertEquals(text,AiCallCommand.START,AiCallPolicy.command(text))
        for(text in listOf("call Sofia","call me tomorrow at 16:00","call me in 5 minutes","what do you call this?"))assertNull(text,AiCallPolicy.command(text))
    }
    @Test fun ringingDoesNotBecomeAConnectedMicrophoneBeforePickup(){val c=AiCallController{0};val s=requireNotNull(c.start(AiCallSource.USER,false));assertEquals(AiCallPhase.RINGING,c.state.value.phase);assertFalse(c.state.value.connected);assertTrue(c.answer(s.id));assertEquals(AiCallPhase.CONNECTING,c.state.value.phase);assertTrue(c.state.value.connected);assertFalse(c.state.value.audioConnected)}
    @Test fun repeatedRequestsAndDoublePickupCannotCreateOverlappingCalls(){val c=AiCallController{0};val s=requireNotNull(c.start(AiCallSource.USER,true));assertNull(c.start(AiCallSource.USER,true));assertTrue(c.answer(s.id));assertFalse(c.answer(s.id))}
    @Test fun oldAnswerOrEndNotificationsCannotAffectTheNextCall(){val c=AiCallController{0};val old=requireNotNull(c.start(AiCallSource.USER,false));c.finish(old.id,"DECLINED");val fresh=requireNotNull(c.start(AiCallSource.USER,false));assertFalse(c.answer(old.id));assertNull(c.finish(old.id,"STALE_END"));assertEquals(fresh.id,c.state.value.id);assertTrue(c.state.value.active)}
    @Test fun unansweredCallTimesOutWithoutAutoAnswering(){var now=0L;val c=AiCallController{now};val s=requireNotNull(c.start(AiCallSource.USER,false));now=45000;assertEquals("MISSED",AiCallPolicy.timeout(c.state.value,now));assertFalse(c.answer(s.id));assertFalse(c.state.value.connected)}
    @Test fun silentAudioConnectionCannotWaitForeverBehindAThinkingSpinner(){var now=0L;val c=AiCallController{now};val s=requireNotNull(c.start(AiCallSource.USER,false));c.answer(s.id);c.stage(s.id,AiCallPhase.THINKING);now=30000;assertEquals("AUDIO_CONNECTION_TIMEOUT",AiCallPolicy.timeout(c.state.value,now))}
    @Test fun quietCallIsBoundedButRealUserActivityKeepsItReady(){var now=0L;val c=AiCallController{now};val s=requireNotNull(c.start(AiCallSource.USER,false));c.answer(s.id);c.stage(s.id,AiCallPhase.LISTENING);now=44000;assertNull(AiCallPolicy.timeout(c.state.value,now));c.activity();now=45000;assertNull(AiCallPolicy.timeout(c.state.value,now));now=89000;assertEquals("QUIET_TIMEOUT",AiCallPolicy.timeout(c.state.value,now))}
    @Test fun aCallHasAHardMaximumEvenIfSpeechKeepsComing(){var now=0L;val c=AiCallController{now};val s=requireNotNull(c.start(AiCallSource.USER,true));c.answer(s.id);c.stage(s.id,AiCallPhase.SPEAKING);now=600000;c.activity();assertEquals("CALL_LIMIT_REACHED",AiCallPolicy.timeout(c.state.value,now))}
    @Test fun endingRetainsThePriorListeningChoiceWithoutRestartingItself(){for(wake in listOf(false,true)){val c=AiCallController{0};val s=requireNotNull(c.start(AiCallSource.USER,wake));val ended=requireNotNull(c.finish(s.id,"ENDED_BY_USER"));assertEquals(wake,ended.wakeWasRunning);assertFalse(c.state.value.active);c.stage(s.id,AiCallPhase.LISTENING);assertFalse(c.state.value.active);assertNull(c.finish(s.id,"DUPLICATE_END"))}}
    @Test fun scheduledAndBatterySourcesRemainDistinguishableUntilAnswer(){val c=AiCallController{0};val s=requireNotNull(c.start(AiCallSource.CHECK_IN,false,"reminder-id","revision"));assertEquals("reminder-id",s.referenceId);assertEquals(AiCallSource.CHECK_IN,s.source);c.finish(s.id,"MISSED");assertEquals(AiCallSource.BATTERY_20,c.start(AiCallSource.BATTERY_20,true)?.source)}
    @Test fun anOldPlaybackCompletionCannotReopenTheMicAfterHangup(){val voice=VoiceSessionStateMachine({0});voice.audioStarted(10);voice.cancel(11);voice.audioCompleted(10);assertFalse(voice.captureReady(10,true));assertEquals(VoiceStage.IDLE,voice.snapshot().stage)}
    @Test fun callFollowUpGetsFortyFiveSecondsWithoutRequiringAnotherWake(){var now=0L;val w=ConversationWindow{now};w.enable();w.callMode(true);w.resume(true);assertTrue(w.captureReady());assertEquals(45000L,w.snapshot().deadline);now=12001;assertTrue(w.acceptsCommand());assertFalse(w.wake());now=45000;assertTrue(w.expire());assertFalse(w.acceptsCommand())}
    @Test fun endingCallModeRestoresTheNormalBoundedFollowUp(){val w=ConversationWindow{0};w.enable();w.callMode(true);w.resume(true);w.captureReady();w.callMode(false);w.resume(true);w.captureReady();assertEquals(12000L,w.snapshot().deadline)}
    @Test fun fastCachedCallGreetingNeverCachesAnOrdinaryPrivateAnswer(){assertTrue(PublicGreetingCache.eligible(AiCallPolicy.greeting(false)));assertTrue(PublicGreetingCache.eligible(AiCallPolicy.greeting(true)));assertFalse(PublicGreetingCache.eligible("Here is your private password or document"))}
    @Test fun repeatCallAnswerQuestionResponseAndEndUsesOnePickupPerCall(){
        var now=1000L;val c=AiCallController{now};val voice=VoiceSessionStateMachine({now})
        repeat(20){i->val s=requireNotNull(c.start(AiCallSource.USER,false));assertTrue(c.answer(s.id));assertFalse(c.answer(s.id));val token=i*3L+1;voice.audioStarted(token);c.stage(s.id,AiCallPhase.SPEAKING);voice.audioCompleted(token);voice.armCapture(token);assertTrue(voice.captureReady(token,true));c.stage(s.id,AiCallPhase.LISTENING);voice.recognized(token,15);c.activity();c.stage(s.id,AiCallPhase.THINKING);voice.audioStarted(token);c.stage(s.id,AiCallPhase.SPEAKING);voice.audioCompleted(token);c.finish(s.id,"ENDED_BY_USER");voice.cancel(token+1);assertFalse(voice.captureReady(token,true));assertFalse(c.state.value.active);now+=1000}
    }
}
