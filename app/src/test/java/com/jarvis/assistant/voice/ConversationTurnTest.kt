package com.jarvis.assistant.voice

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.hud.HudSettings
import com.jarvis.assistant.rebirth.RecognitionPayload
import com.jarvis.assistant.security.ScreenCommand
import com.jarvis.assistant.security.ScreenCommandPolicy
import com.jarvis.assistant.sentinel.ReactorFrameClock
import com.jarvis.assistant.sentinel.ReactorGeometry
import com.jarvis.assistant.sentinel.SentinelMode
import com.jarvis.assistant.titan.SpeechRoute
import com.jarvis.assistant.titan.SpeechRoutePolicy
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ConversationTurnTest {
    @Test fun stablePartialInNoisyRoomRequestsFinalBeforeTheTwelveSecondTimeout(){
        var now=0L;val turn=UtteranceAssembler({now})
        turn.partial("lock my phone")
        repeat(24){now+=100;turn.energy(.1f);assertFalse(turn.needsFinalization())}
        now=2500;turn.energy(.1f)
        assertTrue(turn.needsFinalization())
        assertFalse("A partial must never lock the phone",turn.ready())
        val final=RecognitionPayload.vosk(JSONObject("""{"alternatives":[{"text":"lock my phone","confidence":24.3,"result":[{"word":"lock","conf":0.95}]}]}"""))
        assertTrue(RecognitionConfidence.acceptsFinal(final.primary))
        turn.segment(final.primary.getString("text"));turn.finalizedEndpoint()
        assertTrue(turn.ready());assertEquals(ScreenCommand.LOCK,ScreenCommandPolicy.parse(turn.text()))
    }
    @Test fun stalledDecoderIsFlushedAfterNaturalSilence(){
        var now=0L;val turn=UtteranceAssembler({now});turn.partial("what is the capital of Ecuador")
        now=999;assertFalse(turn.needsFinalization());now=1000;assertTrue(turn.needsFinalization());assertFalse(turn.ready())
    }
    @Test fun changingSpeechIsNotCutOffAtTheStalePartialDeadline(){
        var now=0L;val turn=UtteranceAssembler({now})
        repeat(60){i->turn.partial("explain the following part $i");now+=500;turn.energy(.1f);assertFalse(turn.needsFinalization());assertFalse(turn.ready())}
    }
    @Test fun anIncompleteClauseKeepsItsLongerPause(){
        var now=0L;val turn=UtteranceAssembler({now});turn.partial("tell me about")
        now=2500;turn.energy(.2f);assertFalse(turn.needsFinalization())
        now=4000;turn.energy(.2f);assertTrue(turn.needsFinalization())
    }
    @Test fun constantRmsCannotHoldAnAcceptedQuestionOpenForever(){
        var now=0L;val turn=UtteranceAssembler({now});turn.segment("why is the sky blue")
        repeat(17){now+=100;turn.energy(.2f);assertFalse(turn.ready())}
        now=1800;turn.energy(.2f);assertTrue(turn.ready());assertFalse(turn.needsFinalization())
    }
    @Test fun shortPauseAndCompoundRequestsRemainOneTurn(){
        var now=0L;val turn=UtteranceAssembler({now});turn.segment("open the clock and")
        now=1500;assertFalse(turn.ready());turn.partial("show my alarms")
        now=2000;turn.segment("show my alarms");now=3000
        assertTrue(turn.ready());assertEquals("open the clock and show my alarms",turn.text())
        assertFalse(UtteranceAssembler.fastCommand(turn.text()))
    }
    @Test fun noiseAndEmptyPartialsCannotInventAnyRequest(){
        var now=0L;val turn=UtteranceAssembler({now})
        repeat(150){now+=100;turn.energy(.9f);turn.partial("")}
        assertFalse(turn.ready());assertFalse(turn.needsFinalization());assertEquals("",turn.text())
    }
    @Test fun rejectedFinalCannotBeReplacedWithItsMoreConvenientPartial(){
        val turn=UtteranceAssembler({0L});turn.partial("lock my phone")
        val rejected=RecognitionPayload.vosk(JSONObject("""{"alternatives":[{"text":"lock the phone","result":[{"word":"lock","conf":0.1}]}]}"""))
        assertFalse(RecognitionConfidence.acceptsFinal(rejected.primary))
        assertFalse(turn.ready());turn.reset();assertFalse(turn.ready());assertEquals("",turn.text())
    }
    @Test fun engineFinalMayCorrectTheDisplayedPartial(){
        val turn=UtteranceAssembler({0L});turn.partial("lock my phone");turn.segment("what time is it");turn.finalizedEndpoint()
        assertTrue(turn.ready());assertNull(ScreenCommandPolicy.parse(turn.text()));assertEquals("what time is it",turn.text())
    }
    @Test fun stopOrNewSessionCannotConsumeThePreviousSessionsFinal(){
        var now=0L;val gate=ConversationWindow{now};gate.enable();gate.wake();val original=gate.snapshot().resetSerial
        gate.pause();assertFalse(gate.accept(original));gate.resume(true);gate.captureReady();assertFalse(gate.accept(original))
        assertTrue(gate.accept());assertFalse(gate.accept())
    }
    @Test fun screenOffPhrasesReachTheSameLockActionWithoutCompoundGuessing(){
        listOf("lock my phone","lock my screen","turn off","turn off my phone","turn off the screen","turn my screen off","apaga la pantalla").forEach{
            assertEquals(it,ScreenCommand.LOCK,ScreenCommandPolicy.parse(it));assertTrue(it,UtteranceAssembler.fastCommand(it))
        }
        listOf("turn off bluetooth","turn off the flashlight","lock my phone and call Alex","can you explain screen locks").forEach{assertNull(it,ScreenCommandPolicy.parse(it))}
    }
    @Test fun ordinaryAnswersUseConfiguredFishVoiceAndPrivateContentKeepsItsConsent(){
        val id=CustomVoicePolicy.REQUESTED_ID
        val general=SpeechRoutePolicy.route(AppLanguage.EN,true,id,true,false,false)
        val privateRoute=SpeechRoutePolicy.route(AppLanguage.EN,true,id,false,true,false)
        assertEquals(SpeechRoute.FISH,general);assertEquals(SpeechRoute.TEXT_ONLY,privateRoute)
        assertFalse(SpeechFeedbackPolicy.needsPublicAcknowledgement(general,general))
        assertTrue(SpeechFeedbackPolicy.needsPublicAcknowledgement(privateRoute,general))
        assertEquals(SpeechRoute.FISH,SpeechRoutePolicy.route(AppLanguage.EN,true,id,false,true,false,true))
    }
    @Test fun disabledVoiceDoesNotClaimAnAudibleAcknowledgement(){
        assertFalse(SpeechFeedbackPolicy.needsPublicAcknowledgement(SpeechRoute.TEXT_ONLY,SpeechRoute.TEXT_ONLY))
    }
    @Test fun pendingActionAcknowledgementDoesNotClaimTheActionWasDone(){
        assertEquals("Please review the confirmation on your screen, sir.",SpeechFeedbackPolicy.publicAcknowledgement(AppLanguage.EN,true))
        assertEquals("Sir, the result is ready on your screen.",SpeechFeedbackPolicy.publicAcknowledgement(AppLanguage.EN,false))
        assertTrue(SpeechFeedbackPolicy.publicAcknowledgement(AppLanguage.ES,true).contains("confirmación"))
    }
    @Test fun reactorMovesContinuouslyAcrossPhaseChangesAndDoesNotReset(){
        val clock=ReactorFrameClock();assertEquals(0f,clock.frame(1000,4f),0f)
        val first=clock.frame(1100,4f);assertTrue(first>0)
        val second=clock.frame(1200,5f);assertTrue(second>first)
        assertEquals(first+2.5f,second,.001f)
    }
    @Test fun reactorResumeDoesNotCountTimeSpentHidden(){
        val clock=ReactorFrameClock();clock.frame(0,4f);val before=clock.frame(100,4f);clock.pause()
        assertEquals(before,clock.frame(120_000,4f),0f);assertTrue(clock.frame(120_100,4f)>before)
    }
    @Test fun activeAnimationIsVisibleAndStillHonorsZeroIntensity(){
        val settings=HudSettings();assertTrue(ReactorGeometry.speed(settings,SentinelMode.LISTENING)>ReactorGeometry.speed(settings,SentinelMode.DORMANT))
        assertEquals(0f,ReactorGeometry.speed(settings.copy(animationIntensity=0f),SentinelMode.SPEAKING),0f)
        val clock=ReactorFrameClock();clock.frame(0,4f);assertEquals(0f,clock.frame(100,Float.NaN),0f)
    }
}
