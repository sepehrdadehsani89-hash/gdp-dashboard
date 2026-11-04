package com.jarvis.assistant.rebirth

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.voice.*
import com.jarvis.assistant.titan.*
import org.junit.Assert.*
import org.junit.Test

class ConversationUpdateTest {
    @Test fun stableBareWakeCanBeAcknowledgedWithoutWaitingForEngineFinal(){
        var now=0L;val prompt=WakeGreetingGate(now={now});prompt.arm(7)
        now=699;assertFalse(prompt.due(7));now=700;assertTrue(prompt.due(7));assertFalse(prompt.due(8))
        prompt.cancel();assertFalse(prompt.due(7))
    }
    @Test fun inlineCommandOrStopCancelsAnOldGreetingAndFreshWakeGetsItsOwnDelay(){
        var now=0L;val prompt=WakeGreetingGate(now={now});prompt.arm(1)
        now=500;prompt.cancel();now=1000;assertFalse(prompt.due(1))
        prompt.arm(2);assertFalse(prompt.due(2));now=1700;assertTrue(prompt.due(2));assertFalse(prompt.due(1))
    }
    @Test fun acknowledgementConsumesTheCommandGateOnlyOnceDespiteALateFinal(){
        var now=0L;val window=ConversationWindow{now};val prompt=WakeGreetingGate(now={now})
        window.enable();window.wake();val serial=window.snapshot().resetSerial;prompt.arm(serial)
        now=800;assertTrue(prompt.due(serial));assertTrue(window.accept(serial));prompt.cancel()
        assertFalse(window.accept(serial));window.resume(true);assertFalse(window.accept(serial))
    }
    @Test fun explicitCompletionPhrasesEndWithoutDeletingForAnEmbeddedOrQuotedRequest(){
        for(text in listOf("Okay, I'm done.","I'm done","That's it!","Okay, that's enough","Ya terminé","Eso es todo")){
            assertTrue(text,ConversationCompletionPolicy.matches(text));assertTrue(UtteranceAssembler.fastCommand(text))
        }
        for(text in listOf("I'm done with my homework, help me review it","Explain the phrase that's it","Tell her I'm done","Okay, that's enough sugar for this cake"))assertFalse(text,ConversationCompletionPolicy.matches(text))
    }
    @Test fun greetingIsExactPublicCourtesyAndNeverLeaksAnEnrolledPersonsName(){
        assertEquals("Hello sir, how can I help you?",ConversationGreetingPolicy.text(AppLanguage.EN))
        assertEquals("Hola, señor. ¿Cómo puedo ayudarte?",ConversationGreetingPolicy.text(AppLanguage.ES))
        assertTrue(ConversationGreetingPolicy.text(AppLanguage.EN,true).contains("not sure"))
        assertFalse(ConversationGreetingPolicy.text(AppLanguage.EN,true).contains("sir"))
        val courtesy=RebirthFastCommands.socialReply("thank you",false,false)!!
        assertTrue(courtesy.allowCloudSpeech)
        assertEquals(SpeechRoute.FISH,SpeechRoutePolicy.route(AppLanguage.EN,true,CustomVoicePolicy.REQUESTED_ID,courtesy.allowCloudSpeech,false,false,false))
        assertTrue(WorldClocks.answer("what time is it in Quito")!!.allowCloudSpeech)
        assertEquals(SpeechRoute.FISH,SpeechRoutePolicy.route(AppLanguage.EN,true,CustomVoicePolicy.REQUESTED_ID,true,false,false,false))
    }
    @Test fun notesHaveProvenanceLimitsAndCannotPersistPrivateToolsOrCredentials(){
        val notes=SessionNoteDraft()
        notes.record("Read the selected document","Private file excerpt",false)
        notes.record("My password is hidden","Acknowledged",true)
        notes.record("Remember an API key","No",true)
        assertNull(notes.takeSummary())
        notes.record("Help plan a study timetable","Review algebra each afternoon",true)
        val text=notes.takeSummary()!!;assertTrue(text.contains("study timetable"));assertTrue(text.contains("unverified"))
        assertNull(notes.takeSummary())
    }
    @Test fun credentialsAreScreenedBeforeClippingAndTheRamDraftStaysBounded(){
        val notes=SessionNoteDraft()
        notes.record("General question "+"word ".repeat(100)+"password: hidden","Answer",true)
        assertNull(notes.takeSummary())
        repeat(100){notes.record("Topic $it","Ordinary answer "+"word ".repeat(200),true)}
        val text=notes.takeSummary()!!;assertTrue(text.length<=1800);assertFalse(text.contains("Topic 0;"));assertTrue(text.contains("Topic 99"))
        notes.record("New ordinary session","Safe answer",true);notes.clear();assertNull(notes.takeSummary())
    }
    @Test fun secretTokenAndEmailPatternsAreNotSessionMemory(){
        for(t in listOf("PIN: 1234","Mi contraseña es ejemplo","Bearer abc","ghp_"+"a".repeat(35),"teacher@example.test","content://documents/private","4111 1111 1111 1111"))assertFalse(t,SessionNoteDraft.safe(t))
    }
    @Test fun voiceAndFileSetupUseRealAndroidInterfacesRatherThanClaimingAccess(){
        assertEquals(NativeNavigation.Destination.VOICE,NativeNavigation.destination("Can you recognize these voices?"))
        assertEquals(NativeNavigation.Destination.VOICE,NativeNavigation.destination("Remember my voice"))
        assertEquals(NativeNavigation.Destination.CONTENT,NativeNavigation.destination("Connect all my files"))
        assertNull(NativeNavigation.destination("Explain how voice recognition works"))
    }
}
