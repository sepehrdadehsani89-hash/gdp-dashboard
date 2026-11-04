package com.jarvis.assistant.rebirth

import com.jarvis.assistant.core.*
import com.jarvis.assistant.neural.*
import org.junit.Assert.*
import org.junit.Test

class RebirthPolicyTest {
    private val apps=listOf(DiscoveredApp("claude","Claude"),DiscoveredApp("instagram","Instagram"),DiscoveredApp("whatsapp","WhatsApp"),DiscoveredApp("spotify","Spotify",listOf("Music")))
    @Test fun onlyOneMicrophoneSurvivesLateReleases(){
        val m=MicrophoneSessionManager();val old=m.acquire(MicrophoneOwner.WAKE)!!
        assertNull(m.acquire(MicrophoneOwner.FOREGROUND_RECOGNIZER));m.release(old)
        val current=m.acquire(MicrophoneOwner.PRIVATE_CAPTURE)!!;m.release(old)
        assertTrue(m.isCurrent(current));assertNotEquals(old.id,current.id);m.release(current);assertNull(m.snapshot())
    }
    @Test fun ordinaryConversationIsBoundedByCharactersAndTurns(){
        val messages=List(500){ChatMessage(role=MessageRole.USER,text="x".repeat(32000))}
        val live=LiveConversationPolicy.bounded(messages)
        assertTrue(live.size<=24);assertTrue(live.sumOf{it.text.length}<=24000);assertTrue(live.all{it.text.length<=8000})
    }
    @Test fun recoveryCannotSpinForeverAndRequiresHealthyAudioToReplenish(){
        var time=0L;val r=RecoveryBudget(now={time})
        assertEquals(400L,r.retryDelay());assertEquals(1200L,r.retryDelay());assertNull(r.retryDelay())
        r.healthy();time=4999;r.healthy();assertNull(r.retryDelay());r.healthy();time=10000;r.healthy();assertEquals(400L,r.retryDelay())
    }
    @Test fun cloudClaudeCorrectionDependsOnOpeningIntentAndActualEntities(){
        assertEquals("open Claude",SemanticAutocorrector.correct("Open cloud",emptyList(),apps).understood)
        for(text in listOf("Explain cloud storage","My cloud storage is full","Upload this file to the cloud","Ask Claude to explain my code"))assertEquals(text,SemanticAutocorrector.correct(text,emptyList(),apps).understood)
        assertEquals("Open cloud",SemanticAutocorrector.correct("Open cloud",emptyList(),emptyList()).understood)
        assertEquals("cloud-storage",SemanticAutocorrector.correct("Open cloud",emptyList(),apps+DiscoveredApp("cloud-storage","Cloud")).targetPackage)
    }
    @Test fun installedVariantsAndApprovedAliasesResolveWithoutInventingPackages(){
        assertEquals("open Instagram",SemanticAutocorrector.correct("open instant gram",emptyList(),apps).understood)
        assertEquals("spotify",SemanticAutocorrector.correct("open Music",emptyList(),apps).targetPackage)
        assertEquals("whatsapp",SemanticAutocorrector.correct("open what's up",emptyList(),apps).targetPackage)
        assertEquals("What's up with my phone?",SemanticAutocorrector.correct("What's up with my phone?",emptyList(),apps).understood)
    }
    @Test fun ambiguityAndSensitiveTargetsCannotBeAutocorrectedIntoAnAction(){
        val twice=apps+DiscoveredApp("instagram.clone","Instagram")
        assertEquals(CorrectionKind.CLARIFY,SemanticAutocorrector.correct("open Instagram",emptyList(),twice).kind)
        for(text in listOf("call Dad","send 250 dollars","open account 123","email alice@example.com","take me home","check whether Bluetooth is on"))assertEquals(text,SemanticAutocorrector.correct(text,emptyList(),apps).understood)
    }
    @Test fun anAlternativeMustPreserveTheSameIntentAndHaveAnActualTarget(){
        assertEquals("instagram",SemanticAutocorrector.correct("open instagrum",listOf(RecognitionHypothesis("open Instagram",.9f)),apps).targetPackage)
        assertNull(SemanticAutocorrector.correct("Explain cloud storage",listOf(RecognitionHypothesis("open Claude",.99f)),apps).targetPackage)
    }
    @Test fun cookAndHomeScreenAreExactNativeCommandsRatherThanRecipeOrNavigation(){
        assertTrue(RebirthFastCommands.openSelf("Hey Jarvis, let's cook"));assertFalse(RebirthFastCommands.openSelf("let's cook pasta"))
        assertTrue(RebirthFastCommands.homeScreen("Go to the home screen"));assertFalse(RebirthFastCommands.homeScreen("Take me home"))
        assertEquals("You're welcome.",RebirthFastCommands.social("thank you Jarvis",false,false));assertEquals("Ready, sir.",RebirthFastCommands.social("are you ready",false,true))
    }
    @Test fun longNativeSpeechIsSegmentedWithoutDroppingAnEmailOrPhoneNumber(){
        val text=("Actual content. ".repeat(120))+"Call +593991234567 or email teacher@example.test."
        val parts=SpeechSegments.split(text)
        assertTrue(parts.size>1);assertTrue(parts.all{it.length<=900})
        assertEquals(text.trim().replace(Regex("\\s+")," "),parts.joinToString(" ").replace(Regex("\\s+")," "))
        assertTrue(parts.last().contains("+593991234567"));assertTrue(parts.last().contains("teacher@example.test"))
    }
    @Test fun spanishLocaleUsesOnlyReportedSupportAndPrefersEcuadorWhenAvailable(){
        assertEquals("es-EC",SpeechLocalePolicy.select(true,listOf("en-US","es-ES","es-EC")))
        assertEquals("es-ES",SpeechLocalePolicy.select(true,listOf("en-US","es-ES")))
        assertNull(SpeechLocalePolicy.select(true,listOf("en-US","fr-FR")))
        assertEquals("en-US",SpeechLocalePolicy.select(false,listOf("es-EC","en-US")))
    }
    @Test fun interfaceNavigationDoesNotHijackMailboxOrFileQuestions(){
        assertNull(NativeNavigation.destination("open my account settings"))
        assertEquals(NativeNavigation.Destination.PHOTOS,NativeNavigation.destination("open my photos"))
        assertNull(NativeNavigation.destination("read my latest email"));assertNull(NativeNavigation.destination("explain how to open my files"))
    }
    @Test fun approvedAliasOverridesConflictingLabelsAndNeverFallsBackWhenTargetDisappears(){
        val collision=apps+DiscoveredApp("system.music","Music")+DiscoveredApp("other.spotify","Spotify")
        val pinned=mapOf("Music" to "spotify")
        val chosen=SemanticAutocorrector.correct("open Music",emptyList(),collision,pinned)
        assertEquals("spotify",chosen.targetPackage)
        assertEquals(CorrectionKind.EXACT_ALIAS,chosen.kind)
        val missing=SemanticAutocorrector.correct("open Music",emptyList(),collision.filterNot{it.packageId=="spotify"},pinned)
        assertEquals(CorrectionKind.CLARIFY,missing.kind);assertNull(missing.targetPackage)
        assertTrue(missing.candidates.isEmpty())
    }
    @Test fun simultaneousMicrophoneRequestsHaveExactlyOneWinner(){
        val manager=MicrophoneSessionManager()
        val start=java.util.concurrent.CountDownLatch(1)
        val winners=java.util.Collections.synchronizedList(mutableListOf<MicrophoneLease>())
        val workers=List(16){Thread{start.await();manager.acquire(MicrophoneOwner.WAKE)?.let(winners::add)}}
        workers.forEach(Thread::start);start.countDown();workers.forEach{it.join(5000)}
        assertTrue(workers.none{it.isAlive});assertEquals(1,winners.size)
        assertTrue(manager.isCurrent(winners.single()));manager.release(winners.single())
    }
    @Test fun worldClockUsesRealZoneRulesAndRefusesToGuessUnknownCities(){
        val winter=java.time.Instant.parse("2026-01-15T12:00:00Z")
        val summer=java.time.Instant.parse("2026-07-15T12:00:00Z")
        assertEquals("2026-01-15 07:00:00 -05:00",WorldClocks.time(winter,"America/Guayaquil"))
        assertEquals("2026-07-15 13:00:00 +01:00",WorldClocks.time(summer,"Europe/London"))
        assertTrue(WorldClocks.answer("what time is it in Atlantis",winter)!!.text.contains("No time zone was guessed"))
        assertTrue(WorldClocks.isRequest("¿Qué hora es en Quito?"))
        assertNull(WorldClocks.answer("explain time zones",winter))
    }

}
