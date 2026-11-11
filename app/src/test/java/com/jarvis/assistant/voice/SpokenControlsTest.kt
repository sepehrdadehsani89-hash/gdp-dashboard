package com.jarvis.assistant.voice

import com.jarvis.assistant.call.*
import com.jarvis.assistant.core.*
import com.jarvis.assistant.rebirth.RecognitionPayload
import org.json.*
import org.junit.Assert.*
import org.junit.Test

class SpokenControlsTest {
    private fun final(text:String,confidence:Double=.14)=JSONObject().put("text",text).put("result",JSONArray().put(JSONObject().put("word",text).put("conf",confidence)))
    @Test fun lowWordScoresStillReachTheLocalRingingHandlerAcrossRepeatedWakeCycles(){
        repeat(20){
            var at=0L;val window=ConversationWindow{at};val call=AiCallController{at}
            window.enable();assertEquals(FinalRecognitionKind.ACCEPTED,RecognitionConfidence.kind(final("hey jarvis"),.9))
            assertTrue(window.wake());assertTrue(window.accept());window.resume(true);assertTrue(window.captureReady())
            val payload=RecognitionPayload.vosk(JSONObject().put("alternatives",JSONArray().put(final("call me now")).put(final("call Alex",.05))))
            assertEquals(FinalRecognitionKind.ACCEPTED,RecognitionConfidence.kind(payload.primary,.9))
            assertTrue(UtteranceAssembler.fastCommand(payload.primary.getString("text")));assertTrue(window.accept())
            assertEquals(AiCallCommand.START,AiCallPolicy.command(payload.primary.getString("text")))
            val c=call.start(AiCallSource.USER,true)!!;assertEquals(AiCallPhase.RINGING,c.phase);assertFalse(c.connected)
            at=1000;assertTrue(call.answer(c.id));call.stage(c.id,AiCallPhase.SPEAKING);call.stage(c.id,AiCallPhase.LISTENING)
            window.callMode(true);window.resume(true);window.captureReady();assertTrue(window.acceptsCommand())
            assertEquals(FinalRecognitionKind.ACCEPTED,RecognitionConfidence.kind(final("what time is it")));assertNull(AiCallPolicy.command("what time is it"))
            call.finish(c.id,"ENDED_BY_USER");assertFalse(call.state.value.active)
        }
    }
    @Test fun lowRiskExceptionNeverAuthorizesTelephoneContactsOrProtectedCompoundActions(){
        for(text in listOf("call Alex","call my bank","call me and unlock my phone","call me now and delete my files","answer call","unlock my phone"))assertEquals(text,FinalRecognitionKind.UNCERTAIN,RecognitionConfidence.kind(final(text),.9))
    }
    @Test fun malformedScoresAndBlankResultsCannotCreateAnAiCall(){
        for(c in listOf(-1.0,2.0))assertEquals(FinalRecognitionKind.UNCERTAIN,RecognitionConfidence.kind(final("call me now",c)))
        assertEquals(FinalRecognitionKind.EMPTY,RecognitionConfidence.kind(final("")));assertNull(AiCallPolicy.command(""))
    }
    @Test fun allExactCallStartAliasesKeepPickupAsASeparateAction(){
        for(text in listOf("call me now","call me","let's do a call","llámame ahora")){
            assertEquals(FinalRecognitionKind.ACCEPTED,RecognitionConfidence.kind(final(text),.9));assertEquals(AiCallCommand.START,AiCallPolicy.command(text))
        }
    }
    @Test fun requestedEnglishAndSpanishModeAliasesAreRecognizedLocally(){
        for(text in listOf("change to Spanish","Hey Jarvis switch to Spanish mode","please speak Spanish","can you speak Spanish","could you please speak to me in Spanish","puedes hablar en español","cambia a español","modo español","responde en español")){
            assertEquals(text,AppLanguage.ES,LanguageRules.languageRequest(text));assertEquals(FinalRecognitionKind.ACCEPTED,RecognitionConfidence.kind(final(text),.9));assertTrue(UtteranceAssembler.fastCommand(text))
        }
        for(text in listOf("change to English","switch language to English","cambia al inglés","English mode"))assertEquals(text,AppLanguage.EN,LanguageRules.languageRequest(text))
    }
    @Test fun languageMentionsQuestionsNegationsAndCompoundsDoNotChangeSettings(){
        for(text in listOf("do not change to Spanish","what does speak Spanish mean","can you explain Spanish architecture","change to Spanish and call Alex","speak Persian","change to German"))assertNull(text,LanguageRules.languageRequest(text))
    }
    @Test fun languagePartialsWaitForConfidenceCheckedFinals(){
        var now=0L;val turn=UtteranceAssembler({now});turn.partial("change to Spanish");assertEquals("",turn.acceptedText());assertFalse(turn.ready())
        now=1100;assertTrue(turn.needsFinalization());turn.segment("change to Spanish");assertTrue(UtteranceAssembler.fastCommand(turn.acceptedText()))
    }
    @Test fun explicitSpanishModeKeepsTheSpanishVoiceLanguageForEnglishWordedQuestions(){
        for(text in listOf("what causes rain","hello what can you do","tell me a story"))assertEquals(AppLanguage.ES,LanguageRules.replyLanguage(text,AppLanguage.ES,true))
        assertEquals(AppLanguage.EN,LanguageRules.replyLanguage("dime una historia",AppLanguage.EN,true))
    }
    @Test fun automaticReplyLanguageDetectionRemainsAvailableWhenModeIsNotPinned(){
        assertEquals(AppLanguage.EN,LanguageRules.replyLanguage("what causes rain",AppLanguage.ES,false))
        assertEquals(AppLanguage.ES,LanguageRules.replyLanguage("hola dime una historia",AppLanguage.EN,false))
    }
    @Test fun diagnosticsKeepOnlyMetadataWithoutStoringTheHeardWords(){
        val privateText="my private password is secret"
        RecognitionDiagnostics.record(final(privateText),.55,"wake/en")
        assertFalse(RecognitionDiagnostics.state.value.contains(privateText));assertFalse(RecognitionDiagnostics.state.value.contains("secret"));assertTrue(RecognitionDiagnostics.state.value.contains("UNCERTAIN"))
    }
}
