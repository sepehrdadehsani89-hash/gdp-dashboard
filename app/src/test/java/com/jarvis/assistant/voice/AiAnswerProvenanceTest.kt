package com.jarvis.assistant.voice

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.data.ProviderException
import org.junit.Assert.*
import org.junit.Test

/** These checks verify spoken-text preparation and evidence tracking, not a physical TTS engine. */
class AiAnswerProvenanceTest {
    @Test fun englishFallbackDisclosesLocalSourceInSpokenText(){val spoken=SpeakableReply.normalize(AiAnswerProvenance.localFallback("An actual local answer.",AppLanguage.EN));assertTrue(spoken.startsWith("The cloud AI is unavailable."));assertTrue(spoken.contains("Local Brain"));assertTrue(spoken.endsWith("An actual local answer."))}
    @Test fun spanishFallbackDisclosesLocalSource(){assertTrue(AiAnswerProvenance.localFallback("Respuesta real",AppLanguage.ES).contains("cerebro local"))}
    @Test fun persianFallbackDisclosesLocalSource(){assertTrue(AiAnswerProvenance.localFallback("پاسخ واقعی",AppLanguage.FA).contains("مغز محلی"))}
    @Test fun failedLocalInferenceCannotBecomeCannedSuccess(){assertTrue(runCatching{AiAnswerProvenance.localFallback("",AppLanguage.EN)}.exceptionOrNull() is ProviderException)}
    @Test fun whitespaceLocalInferenceCannotBecomeCannedSuccess(){assertTrue(runCatching{AiAnswerProvenance.localFallback(" \n ",AppLanguage.EN)}.isFailure)}
    @Test fun fallbackTrackingDoesNotCreditOpenAiWithLocalReply(){val t=VoiceDeliveryTracker();t.request(1,"openai");t.inferenceFailed(1);t.request(1,"local");t.response(1,50);assertEquals("local",t.state.value.provider);assertEquals(InferenceOutcome.RESPONSE_RECEIVED,t.state.value.inference);assertEquals(VoiceOutputOutcome.NOT_REQUESTED,t.state.value.output)}
    @Test fun fallbackFailureCannotLeaveFalseResponseEvidence(){val t=VoiceDeliveryTracker();t.request(1,"openai");t.request(1,"local");t.inferenceFailed(1);assertEquals("local",t.state.value.provider);assertEquals(InferenceOutcome.FAILED,t.state.value.inference);assertEquals(0,t.state.value.responseCharacters)}
    @Test fun stoppedFallbackCannotReviveOldUtterance(){val t=VoiceDeliveryTracker();t.request(1,"local");t.stop(2);t.response(1,50);assertEquals(InferenceOutcome.NOT_REQUESTED,t.state.value.inference);assertEquals(VoiceOutputOutcome.STOPPED,t.state.value.output)}
}
