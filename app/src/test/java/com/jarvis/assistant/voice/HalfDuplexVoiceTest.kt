package com.jarvis.assistant.voice

import org.junit.Assert.*
import org.junit.Test

/** Real coordinator transitions and replayed callbacks; not an acoustic/device test. */
class HalfDuplexVoiceTest {
    private class Turn {var now=1000L;val machine=VoiceSessionStateMachine({now});init{machine.cancel(1)};fun start(){machine.prepareSpeech(1);machine.audioStarted(1)}}
    @Test fun synthesisAlreadyOwnsTheMicrophone(){val t=Turn();assertTrue(t.machine.prepareSpeech(1));assertTrue(t.machine.blocksRecognition())}
    @Test fun playingNeverAcceptsRecognition(){val t=Turn();t.start();assertFalse(t.machine.recognitionAllowed(1))}
    @Test fun staleFinalCannotReplaceSpeakingState(){val t=Turn();t.start();t.machine.recognized(1,100);assertEquals(VoiceStage.SPEAKING,t.machine.snapshot().stage)}
    @Test fun noiseErrorCannotReplaceSpeakingState(){val t=Turn();t.start();assertFalse(t.machine.asrFailed(1));assertEquals(VoiceStage.SPEAKING,t.machine.snapshot().stage)}
    @Test fun wakeWordCannotReplaceSpeakingState(){val t=Turn();t.start();t.machine.wake(1);assertEquals(VoiceStage.SPEAKING,t.machine.snapshot().stage)}
    @Test fun captureReadinessCannotInterruptSpeech(){val t=Turn();t.start();assertFalse(t.machine.captureReady(1,true))}
    @Test fun armingCannotOverridePlaybackReservation(){val t=Turn();t.start();t.machine.armCapture(1);assertEquals(VoiceStage.SPEAKING,t.machine.snapshot().stage)}
    @Test fun idleNotificationCannotReleaseSpeech(){val t=Turn();t.start();t.machine.idle(1);assertTrue(t.machine.blocksRecognition())}
    @Test fun readyCannotReleaseUnfinishedSpeech(){val t=Turn();t.start();assertFalse(t.machine.ready(1))}
    @Test fun elapsedTimeAloneCannotCompleteAudio(){val t=Turn();t.start();t.now+=120000;assertTrue(t.machine.blocksRecognition());assertFalse(t.machine.ready(1))}
    @Test fun completionStartsCooldownWithoutMicrophone(){val t=Turn();t.start();t.machine.audioCompleted(1);assertEquals(VoiceStage.COOLDOWN,t.machine.snapshot().stage);assertTrue(t.machine.blocksRecognition())}
    @Test fun completionNeedsTheEntireCooldown(){val t=Turn();t.start();t.machine.audioCompleted(1,700);t.now+=699;assertFalse(t.machine.ready(1));t.now++;assertTrue(t.machine.ready(1))}
    @Test fun readyReturnsToWakeOnlyUnlessExplicitlyArmed(){val t=Turn();t.start();t.machine.audioCompleted(1);t.now+=500;t.machine.ready(1);assertEquals(VoiceStage.READY,t.machine.snapshot().stage)}
    @Test fun finalInCooldownIsIgnored(){val t=Turn();t.start();t.machine.audioCompleted(1);t.machine.recognized(1,100);assertEquals(VoiceStage.COOLDOWN,t.machine.snapshot().stage)}
    @Test fun greetingCannotStartInsideCooldown(){val t=Turn();t.start();t.machine.audioCompleted(1);assertFalse(t.machine.claimGreeting(VoiceSessionStateMachine.nextWakeTicket()))}
    @Test fun manualStopRetiresEveryOldPlaybackCallback(){val t=Turn();t.start();t.machine.cancel(2);t.machine.audioCompleted(1);assertEquals(VoiceStage.IDLE,t.machine.snapshot().stage);assertFalse(t.machine.blocksRecognition())}
    @Test fun lateOldFailureCannotReleaseNewSpeech(){val t=Turn();t.start();t.machine.cancel(2);t.machine.prepareSpeech(2);t.machine.audioStarted(2);t.machine.audioFailed(1);assertTrue(t.machine.blocksRecognition());assertEquals(VoiceStage.SPEAKING,t.machine.snapshot().stage)}
    @Test fun lateOldCaptureCannotAffectNewTurn(){val t=Turn();t.machine.cancel(2);assertFalse(t.machine.captureReady(1,false))}
    @Test fun backendWaitAlsoBlocksCapture(){val t=Turn();t.machine.recognized(1,20);assertEquals(VoiceStage.THINKING,t.machine.snapshot().stage);assertFalse(t.machine.recognitionAllowed(1))}
    @Test fun responseReadyReleasesThinkingBeforeSpeechPreparation(){val t=Turn();t.machine.recognized(1,20);assertTrue(t.machine.ready(1));assertTrue(t.machine.prepareSpeech(1));assertFalse(t.machine.recognitionAllowed(1))}
    @Test fun finalizationIsRecordedBeforeInference(){val t=Turn();t.machine.recognized(1,20);assertEquals(listOf(VoiceStage.FINALIZING,VoiceStage.THINKING),t.machine.recent().takeLast(2).map{it.stage})}
    @Test fun audioFailureAlsoClearsEchoBeforeReady(){val t=Turn();t.start();t.machine.audioFailed(1,800);assertEquals(800L,t.machine.cooldownRemaining(1));t.now+=800;assertTrue(t.machine.ready(1))}
    @Test fun cooldownCannotBeDisabledAccidentally(){val t=Turn();t.start();t.machine.audioCompleted(1,0);assertEquals(250L,t.machine.cooldownRemaining(1))}
    @Test fun cooldownHasAFiniteUpperBound(){val t=Turn();t.start();t.machine.audioCompleted(1,Long.MAX_VALUE);assertEquals(2000L,t.machine.cooldownRemaining(1))}
    @Test fun oldCompletionCannotReopenMicAfterHangup(){val t=Turn();t.start();t.machine.cancel(2);t.machine.audioCompleted(1);assertFalse(t.machine.captureReady(1,true))}
    @Test fun defaultConversationDoesNotAutoListen(){assertFalse(VoiceContinuationPolicy.autoResume(true))}
    @Test fun explicitContinuousConversationCanRearm(){assertTrue(VoiceContinuationPolicy.autoResume(true,true))}
    @Test fun explicitlyAnsweredAiCallCanRearm(){assertTrue(VoiceContinuationPolicy.autoResume(true,false,true))}
    @Test fun noRequestNeverStartsListening(){assertFalse(VoiceContinuationPolicy.autoResume(false,true,true))}
    @Test fun repeatedConversationHasNoSpeechCaptureOverlap(){val t=Turn();repeat(40){i->val token=i+2L;t.machine.cancel(token);assertTrue(t.machine.captureReady(token,false));t.machine.recognized(token,50);t.machine.prepareSpeech(token);t.machine.audioStarted(token);assertFalse(t.machine.captureReady(token,true));t.machine.audioCompleted(token);t.now+=500;assertTrue(t.machine.ready(token))};assertTrue(t.machine.recent().size<=64)}
}
