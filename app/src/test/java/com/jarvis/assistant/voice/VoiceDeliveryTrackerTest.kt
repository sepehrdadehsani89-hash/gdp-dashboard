package com.jarvis.assistant.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceDeliveryTrackerTest {
    @Test fun identityDoesNotProveInferenceOrPlayback(){val t=VoiceDeliveryTracker();assertEquals(InferenceOutcome.NOT_REQUESTED,t.state.value.inference);assertEquals(VoiceOutputOutcome.NOT_REQUESTED,t.state.value.output)}
    @Test fun requestIsNotAResponse(){val t=VoiceDeliveryTracker();t.request(1,"openai");assertEquals(InferenceOutcome.REQUESTING,t.state.value.inference)}
    @Test fun responseDoesNotProveSpeech(){val t=VoiceDeliveryTracker();t.request(1,"openai");t.response(1,20);assertEquals(InferenceOutcome.RESPONSE_RECEIVED,t.state.value.inference);assertEquals(VoiceOutputOutcome.NOT_REQUESTED,t.state.value.output)}
    @Test fun blankResponseCannotBecomeSuccess(){val t=VoiceDeliveryTracker();t.request(1,"openai");t.response(1,0);assertEquals(InferenceOutcome.REQUESTING,t.state.value.inference)}
    @Test fun negativeResponseLengthCannotSucceed(){val t=VoiceDeliveryTracker();t.request(1,"openai");t.response(1,-1);assertEquals(0,t.state.value.responseCharacters)}
    @Test fun receivedCountsAreBounded(){val t=VoiceDeliveryTracker();t.response(1,Int.MAX_VALUE);assertEquals(64000,t.state.value.responseCharacters)}
    @Test fun synthesisIsNotPlayback(){val t=VoiceDeliveryTracker();t.prepare(1);assertEquals(VoiceOutputOutcome.PREPARING,t.state.value.output)}
    @Test fun completedWithoutAudioStartCannotReportSuccess(){val t=VoiceDeliveryTracker();t.prepare(1);t.completed(1);assertEquals(VoiceOutputOutcome.PREPARING,t.state.value.output)}
    @Test fun startedNeedsAnActualPreparedUtterance(){val t=VoiceDeliveryTracker();t.started(1);assertEquals(VoiceOutputOutcome.NOT_REQUESTED,t.state.value.output)}
    @Test fun audioStartedDoesNotClaimCompletion(){val t=VoiceDeliveryTracker();t.prepare(1);t.started(1);assertEquals(VoiceOutputOutcome.STARTED,t.state.value.output)}
    @Test fun completionCallbackRecordsPlaybackCompletion(){val t=VoiceDeliveryTracker();t.prepare(1);t.started(1);t.completed(1);assertEquals(VoiceOutputOutcome.COMPLETED,t.state.value.output)}
    @Test fun voiceFailurePreservesTheReceivedAiAnswer(){val t=VoiceDeliveryTracker();t.response(1,30);t.prepare(1);t.failed(1);assertEquals(InferenceOutcome.RESPONSE_RECEIVED,t.state.value.inference);assertEquals(VoiceOutputOutcome.FAILED,t.state.value.output)}
    @Test fun providerFailureDoesNotClaimVoiceFailure(){val t=VoiceDeliveryTracker();t.request(1,"openai");t.inferenceFailed(1);assertEquals(InferenceOutcome.FAILED,t.state.value.inference);assertEquals(VoiceOutputOutcome.NOT_REQUESTED,t.state.value.output)}
    @Test fun manualStopDoesNotReportCompletedPlayback(){val t=VoiceDeliveryTracker();t.prepare(1);t.started(1);t.stop(2);assertEquals(VoiceOutputOutcome.STOPPED,t.state.value.output)}
    @Test fun staleProviderResponseCannotReappearAfterStop(){val t=VoiceDeliveryTracker();t.request(1,"openai");t.stop(2);t.response(1,50);assertEquals(InferenceOutcome.NOT_REQUESTED,t.state.value.inference)}
    @Test fun staleAudioStartCannotAffectNewerTurn(){val t=VoiceDeliveryTracker();t.prepare(1);t.request(2,"gemini");t.started(1);assertEquals(VoiceOutputOutcome.NOT_REQUESTED,t.state.value.output)}
    @Test fun staleFailureCannotReplaceNewSuccess(){val t=VoiceDeliveryTracker();t.prepare(2);t.started(2);t.completed(2);t.failed(1);assertEquals(VoiceOutputOutcome.COMPLETED,t.state.value.output)}
    @Test fun staleCompletionCannotFinishNewUtterance(){val t=VoiceDeliveryTracker();t.prepare(2);t.started(2);t.completed(1);assertEquals(VoiceOutputOutcome.STARTED,t.state.value.output)}
    @Test fun diagnosticsNeverRetainUnknownProviderText(){val t=VoiceDeliveryTracker();t.request(1,"PRIVATE-KEY-USER-TEXT");assertFalse(t.state.value.toString().contains("PRIVATE"))}
    @Test fun newProviderRequestResetsOldSpeechEvidence(){val t=VoiceDeliveryTracker();t.response(1,30);t.prepare(1);t.started(1);t.completed(1);t.request(2,"anthropic");assertEquals(0,t.state.value.responseCharacters);assertEquals(VoiceOutputOutcome.NOT_REQUESTED,t.state.value.output)}
    @Test fun retryCanPrepareASecondActualPlayback(){val t=VoiceDeliveryTracker();t.response(1,10);t.prepare(1);t.failed(1);t.prepare(1);t.started(1);t.completed(1);assertEquals(VoiceOutputOutcome.COMPLETED,t.state.value.output)}
    @Test fun inferenceDoesNotMeanEngineReady(){val t=VoiceDeliveryTracker();t.response(1,30);assertFalse(t.state.value.engineReady)}
    @Test fun synthesisDoesNotMeanEngineReady(){val t=VoiceDeliveryTracker();t.prepare(1);assertFalse(t.state.value.engineReady)}
    @Test fun actualPrepareIsSeparateFromAudioStart(){val t=VoiceDeliveryTracker();t.prepare(1);t.prepared(1);assertTrue(t.state.value.engineReady);assertEquals(VoiceOutputOutcome.PREPARING,t.state.value.output)}
    @Test fun stalePrepareCannotMarkNewUtteranceReady(){val t=VoiceDeliveryTracker();t.prepare(2);t.prepared(1);assertFalse(t.state.value.engineReady)}
    @Test fun retryMustPrepareNewPlayer(){val t=VoiceDeliveryTracker();t.prepare(1);t.prepared(1);t.failed(1);t.prepare(1);assertFalse(t.state.value.engineReady)}
    @Test fun stoppedPrepareIsIgnored(){val t=VoiceDeliveryTracker();t.prepare(1);t.stop(2);t.prepared(1);assertFalse(t.state.value.engineReady)}
    @Test fun normalizationPreservesMeaningfulBatteryAnswer(){assertEquals("Battery level is 72 percent.",SpeakableReply.normalize("**Battery level is 72 percent.**"))}
    @Test fun linkedLabelsRemainSpeakableWithoutRawUrls(){assertEquals("Read the guide",SpeakableReply.normalize("Read [the guide](https://example.test/private)"))}
    @Test fun normalizationDoesNotFabricateTextForUrlOnlyAnswer(){assertEquals("",SpeakableReply.normalize("https://example.test/private"))}
    @Test fun normalizationRemovesControlCharacters(){assertEquals("Safe text",SpeakableReply.normalize("Safe\u0000 text"))}
    @Test fun normalizationPreservesPersianAndSpanish(){assertEquals("شارژ باتری ۷۲ درصد است. Batería lista.",SpeakableReply.normalize("شارژ باتری ۷۲ درصد است. Batería lista."))}
}
