package com.jarvis.assistant.voice
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.omnisystem.ModuleServiceFailure
import org.junit.Assert.*
import org.junit.Test
class VoiceOutputErrorsTest {
    @Test fun actualVoiceHttpErrorsAreNotMisreportedAsUnrecognizedSpeech(){val text=VoiceOutputErrors.message(ModuleServiceFailure(429),AppLanguage.EN);assertTrue(text.contains("HTTP 429"));assertFalse(text.contains("recognition",true))}
    @Test fun quietModeIsNotMisreportedAsABadApiKey(){val text=VoiceOutputErrors.message(java.io.IOException("Voice reply paused by Silent or Do Not Disturb mode."),AppLanguage.EN);assertTrue(text.contains("Do Not Disturb"));assertFalse(text.contains("quota"))}
    @Test fun rawUrlsSecretsAndProviderBodiesNeverEnterTheVoiceError(){val text=VoiceOutputErrors.message(java.io.IOException("https://private.example?token=private-token"),AppLanguage.EN);assertFalse(text.contains("private-token"));assertFalse(text.contains("private.example"))}
    @Test fun understoodMathQuestionsAreNotRejectedJustBecauseTheyContainNumbers(){val json=org.json.JSONObject("""{"text":"what is 2 plus 2","result":[{"word":"what","conf":0.1},{"word":"two","conf":0.2}]}""");assertEquals(FinalRecognitionKind.ACCEPTED,RecognitionConfidence.kind(json));assertFalse(LocalVoiceQueries.ordinaryQuestion("what time is it and call 123"))}
}
