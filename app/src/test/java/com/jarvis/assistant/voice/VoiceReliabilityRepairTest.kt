package com.jarvis.assistant.voice

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.data.ProviderException
import com.jarvis.assistant.rebirth.ConversationGreetingPolicy
import com.jarvis.assistant.rebirth.RecognitionPayload
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class VoiceReliabilityRepairTest {
    @Test fun repeatedWakeGreetingTimeAnswerAndFollowUpHaveOneGreetingPerConversation(){
        var now=1000L;val machine=VoiceSessionStateMachine({now});var greetings=0
        repeat(20){iteration->
            val generation=iteration*3L+1
            machine.wake(generation);val ticket=VoiceSessionStateMachine.nextWakeTicket()
            assertTrue(machine.claimGreeting(ticket));machine.greeting(generation);greetings++
            assertEquals("Hello sir, how can I help you?",ConversationGreetingPolicy.text(AppLanguage.EN))
            assertFalse("Do not record a command during the greeting",machine.captureReady(generation,true))
            machine.audioStarted(generation);machine.audioCompleted(generation);machine.armCapture(generation)
            assertTrue(machine.captureReady(generation,true));assertFalse(machine.claimGreeting(ticket))
            val payload=RecognitionPayload.vosk(JSONObject("""{"alternatives":[{"text":"what time is it","result":[{"word":"what","conf":0.9},{"word":"time","conf":0.95}]}]}"""))
            assertEquals(FinalRecognitionKind.ACCEPTED,RecognitionConfidence.kind(payload.primary))
            machine.recognized(generation,payload.primary.getString("text").length)
            // Runtime retires the old capture session before dispatching a native or AI turn.
            val processingGeneration=generation+1
            machine.cancel(processingGeneration);machine.recognized(processingGeneration,payload.primary.getString("text").length)
            assertEquals(VoiceStage.PROCESSING,machine.snapshot().stage)
            assertFalse(machine.captureReady(processingGeneration,true))
            val clock=Clock.fixed(Instant.parse("2026-11-09T20:15:00Z").plusSeconds(iteration*60L),ZoneId.of("America/Guayaquil"))
            val answer=LocalVoiceQueries.answer(requireNotNull(LocalVoiceQueries.parse(payload.primary.getString("text"))),AppLanguage.EN,clock,true)
            assertEquals("Sir, it's 15:${(15+iteration).toString().padStart(2,'0')}.",answer)
            machine.audioStarted(processingGeneration);assertEquals(VoiceStage.SPEAKING,machine.snapshot().stage)
            machine.audioCompleted(processingGeneration);machine.armCapture(processingGeneration);assertTrue(machine.captureReady(processingGeneration,true))
            machine.recognized(processingGeneration,29);machine.backendFailed(processingGeneration)
            assertEquals(VoiceReason.BACKEND_FAILED,machine.recent().last().reason)
            machine.audioStarted(processingGeneration);machine.audioCompleted(processingGeneration);machine.idle(processingGeneration);now+=1000
        }
        assertEquals(20,greetings);assertTrue(machine.recent().size<=64)
    }
    @Test fun duplicateAndReplayedGreetingTicketsDoNotRestartAGreeting(){
        val machine=VoiceSessionStateMachine({0})
        val first=VoiceSessionStateMachine.nextWakeTicket();val second=VoiceSessionStateMachine.nextWakeTicket()
        assertTrue(machine.claimGreeting(first));assertFalse(machine.claimGreeting(first));assertTrue(machine.claimGreeting(second));assertFalse(machine.claimGreeting(first))
    }
    @Test fun restartingAWakeServiceGetsAFreshTicket(){
        val machine=VoiceSessionStateMachine({0})
        assertTrue(machine.claimGreeting(VoiceSessionStateMachine.nextWakeTicket()))
        machine.cancel(3);assertTrue(machine.claimGreeting(VoiceSessionStateMachine.nextWakeTicket()))
    }
    @Test fun oldPlaybackCompletionCannotOverwriteTheNewCaptureState(){
        val machine=VoiceSessionStateMachine({0});machine.audioStarted(2);machine.cancel(3);machine.armCapture(3)
        assertTrue(machine.captureReady(3,false));machine.audioCompleted(2)
        assertEquals(VoiceStage.LISTENING,machine.snapshot().stage)
    }
    @Test fun microphoneReadinessIsRejectedDuringProcessingAndSpeaking(){
        val machine=VoiceSessionStateMachine({0});machine.recognized(1,10);assertFalse(machine.captureReady(1,false))
        machine.audioStarted(1);assertFalse(machine.captureReady(1,true));machine.audioCompleted(1);assertTrue(machine.captureReady(1,true))
    }
    @Test fun oneRecognitionRetryDoesNotBecomeAnInfinitePromptLoop(){
        val machine=VoiceSessionStateMachine({0});assertTrue(machine.asrFailed(1));machine.cancel(2)
        assertFalse(machine.asrFailed(2));machine.wake(3);assertTrue(machine.asrFailed(3))
    }
    @Test fun backendAndOwnerFailuresDoNotGetClassifiedAsSpeechRecognitionFailures(){
        val machine=VoiceSessionStateMachine({0});machine.recognized(1,15);machine.ownerCheck(1);machine.ownerResult(1,false)
        assertEquals(VoiceReason.OWNER_REJECTED,machine.recent().last().reason)
        machine.backendFailed(1);assertEquals(VoiceReason.BACKEND_FAILED,machine.recent().last().reason)
        assertFalse(machine.recent().any{it.reason==VoiceReason.ASR_FAILED})
    }
    @Test fun aLowConfidenceFunctionWordCannotDiscardAnExactHarmlessClockQuery(){
        val json=JSONObject("""{"text":"what time is it","result":[{"word":"what","conf":0.1},{"word":"time","conf":0.4},{"word":"is","conf":0.1},{"word":"it","conf":0.1}]}""")
        assertEquals(FinalRecognitionKind.ACCEPTED,RecognitionConfidence.kind(json));assertTrue(UtteranceAssembler.fastCommand(json.getString("text")))
    }
    @Test fun theReadOnlyExceptionDoesNotRelaxSideEffectfulOrCompoundActions(){
        for(text in listOf("open my bank","lock my phone","what time is it and call Alex","what is my password")){
            val json=JSONObject().put("text",text).put("result",org.json.JSONArray().put(JSONObject().put("conf",.1)))
            assertEquals(text,FinalRecognitionKind.UNCERTAIN,RecognitionConfidence.kind(json))
        }
    }
    @Test fun invalidWordScoresCannotBeAcceptedEvenForPublicFacts(){
        val malformed=JSONObject("""{"text":"what time is it","result":[{"word":"time","conf":-3}]}""")
        assertEquals(FinalRecognitionKind.UNCERTAIN,RecognitionConfidence.kind(malformed))
    }
    @Test fun understandableOrdinaryQuestionsReachTheBackendInsteadOfAFalseNoMatch(){
        val json=JSONObject("""{"text":"why is the sky blue","result":[{"word":"why","conf":0.1},{"word":"sky","conf":0.4}]}""")
        assertEquals(FinalRecognitionKind.ACCEPTED,RecognitionConfidence.kind(json))
        assertFalse(LocalVoiceQueries.ordinaryQuestion("can you call Alex"));assertFalse(LocalVoiceQueries.ordinaryQuestion("what is my password"))
    }
    @Test fun emptyVoskFinalsAreSilenceBoundariesAndKeepPreviouslyAcceptedWords(){
        var now=0L;val turn=UtteranceAssembler({now});turn.segment("explain time zones");turn.partial("noise")
        val empty=RecognitionPayload.vosk(JSONObject("""{"alternatives":[{"text":"","confidence":0}]}"""))
        assertEquals(FinalRecognitionKind.EMPTY,RecognitionConfidence.kind(empty.primary))
        turn.emptyFinal(true);assertEquals("explain time zones",turn.acceptedText());assertTrue(turn.ready())
    }
    @Test fun noiseOnlyEmptyFinalsCannotBecomeCommandsOrImmediateRetryErrors(){
        val turn=UtteranceAssembler({0});repeat(10){turn.partial("");assertEquals(FinalRecognitionKind.EMPTY,RecognitionConfidence.kind(JSONObject("""{"text":""}""")));turn.emptyFinal(true)}
        assertFalse(turn.ready());assertEquals("",turn.acceptedText())
    }
    @Test fun exactDateBatteryAndTemperatureRemainNativeWhileAppAndWorldQueriesKeepTheirRoutes(){
        assertEquals(LocalVoiceQuery.DATE,LocalVoiceQueries.parse("What day is it?"))
        assertTrue(LocalVoiceQueries.readOnly("what's my battery"));assertTrue(LocalVoiceQueries.readOnly("battery temperature"))
        assertNull(LocalVoiceQueries.parse("what time is it in Tokyo"));assertNull(LocalVoiceQueries.parse("open the clock"));assertNull(LocalVoiceQueries.parse("explain time zones"))
    }
    @Test fun timeAndDateUseTheSuppliedRealClockAndZoneInsteadOfConstantValues(){
        val instant=Instant.parse("2026-11-10T00:15:00Z")
        val utc=Clock.fixed(instant,ZoneId.of("UTC"));val quito=Clock.fixed(instant,ZoneId.of("America/Guayaquil"))
        assertEquals("Sir, it's 00:15.",LocalVoiceQueries.answer(LocalVoiceQuery.TIME,AppLanguage.EN,utc,true))
        assertEquals("Sir, it's 19:15.",LocalVoiceQueries.answer(LocalVoiceQuery.TIME,AppLanguage.EN,quito,true))
        assertTrue(LocalVoiceQueries.answer(LocalVoiceQuery.DATE,AppLanguage.EN,quito).contains("November 9"))
        assertTrue(LocalVoiceQueries.answer(LocalVoiceQuery.TIME,AppLanguage.ES,quito,true).startsWith("Señor, son las 19:15"))
    }
    @Test fun providerHttpErrorsStayProviderErrorsAndDoNotMasqueradeAsNoMatch(){
        val message=BackendVoiceErrors.message(ProviderException("The provider quota was reached (HTTP 429)."),AppLanguage.EN)
        assertTrue(requireNotNull(message).contains("HTTP 429"));assertFalse(message.contains("speech",true))
        assertTrue(requireNotNull(BackendVoiceErrors.message(java.net.SocketTimeoutException("private-url-secret"),AppLanguage.EN)).contains("timed out"))
        assertFalse(requireNotNull(BackendVoiceErrors.message(java.net.UnknownHostException("secret-host"),AppLanguage.EN)).contains("secret-host"))
    }
    @Test fun metadataTraceNeverStoresRecognizedTextOrProviderSecretPayloads(){
        val machine=VoiceSessionStateMachine({100});machine.recognized(1,"my private credential".length);machine.backendFailed(1)
        val trace=machine.recent().toString();assertFalse(trace.contains("my private"));assertTrue(trace.contains("FINAL_RECEIVED"));assertTrue(trace.contains("BACKEND_FAILED"))
    }
    @Test fun greetingCacheOnlyAcceptsFixedPublicCourtesyClips(){
        val dir=Files.createTempDirectory("jarvis-public-greeting").toFile()
        try{
            val cache=PublicGreetingCache(dir);val c=CustomVoiceConfig(key="synthetic-test-key",enabled=true)
            cache.write(c,"a private conversation",byteArrayOf(1,2,3));assertEquals(0,dir.listFiles().orEmpty().size)
            val greeting=ConversationGreetingPolicy.text(AppLanguage.EN);val bytes=byteArrayOf(1,2,3)
            cache.write(c,greeting,bytes);assertArrayEquals(bytes,cache.read(c,greeting));assertNull(cache.read(CustomVoiceConfig(key="synthetic-test-key",enabled=false),greeting))
            assertNull(cache.read(CustomVoiceConfig(key="different-test-key",enabled=true),greeting))
            cache.clear();assertNull(cache.read(c,greeting))
        }finally{dir.deleteRecursively()}
    }
    @Test fun expiredCachedGreetingCannotMaskAFailedCurrentVoiceConfiguration(){
        val dir=Files.createTempDirectory("jarvis-greeting-expiry").toFile()
        try{
            var now=System.currentTimeMillis();val cache=PublicGreetingCache(dir){now};val c=CustomVoiceConfig(key="test-key",enabled=true)
            cache.write(c,ConversationGreetingPolicy.text(AppLanguage.EN),byteArrayOf(1,2,3));now=dir.listFiles()!!.single().lastModified()+604_800_001
            assertNull(cache.read(c,ConversationGreetingPolicy.text(AppLanguage.EN)))
        }finally{dir.deleteRecursively()}
    }
}
