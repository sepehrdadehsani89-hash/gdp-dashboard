package com.jarvis.assistant.titan

import com.jarvis.assistant.core.*
import com.jarvis.assistant.voice.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class TitanVoiceTest {
    @Test fun spanishNeverFallsBackToSystemOpenAiOrAnUnapprovedId(){
        for(enabled in listOf(false,true))for(allow in listOf(false,true))for(privateTurn in listOf(false,true))for(consent in listOf(false,true)){
            val route=SpeechRoutePolicy.route(AppLanguage.ES,enabled,CustomVoicePolicy.SPANISH_ID,allow,privateTurn,true,consent)
            val expected=if(enabled && (allow && !privateTurn || privateTurn && consent))SpeechRoute.FISH else SpeechRoute.TEXT_ONLY
            assertEquals(expected,route)
            assertEquals(SpeechRoute.TEXT_ONLY,SpeechRoutePolicy.route(AppLanguage.ES,enabled,CustomVoicePolicy.REQUESTED_ID,allow,privateTurn,true,consent))
        }
    }
    @Test fun privateSpeechRequiresSeparateExplicitConsentAndCannotUseLegacyCloud(){
        assertEquals(SpeechRoute.LOCAL,SpeechRoutePolicy.route(AppLanguage.EN,true,"approved",true,true,true))
        assertEquals(SpeechRoute.FISH,SpeechRoutePolicy.route(AppLanguage.EN,true,"approved",false,true,true,true))
        assertEquals(SpeechRoute.OPENAI,SpeechRoutePolicy.route(AppLanguage.EN,false,"approved",true,false,true))
        assertEquals(SpeechRoute.TEXT_ONLY,SpeechRoutePolicy.route(AppLanguage.FA,true,"approved",true,false,true))
    }
    @Test fun onlyEnglishAndSpanishCanBeActivatedWithoutDeletingLegacyEnumData(){
        assertEquals(listOf(AppLanguage.EN,AppLanguage.ES),AppLanguage.supported)
        assertEquals(AppLanguage.EN,AppLanguage.fromCode("fa"));assertNull(LanguageRules.languageRequest("speak Persian"))
        assertNotEquals(AppLanguage.FA,LanguageRules.detect("سلام",AppLanguage.EN));assertNull(WakePhraseMatcher.extractCommand("هی جارویس",AppLanguage.FA))
    }
    @Test fun ownerGreetingsUseActualBoundaryHourAndSirNotFirstName(){
        val expected=mapOf(4 to "Good night",5 to "Good morning",11 to "Good morning",12 to "Good afternoon",17 to "Good afternoon",18 to "Good evening",21 to "Good evening",22 to "Good night")
        expected.forEach{(hour,prefix)->val text=GreetingPolicy.greeting(hour,AppLanguage.EN,true,true,"Sepehr");assertTrue(text.startsWith("$prefix, sir."));assertFalse(text.contains("Sepehr"))}
    }
    @Test fun unknownAndGuestGreetingsDoNotClaimOwnerIdentity(){
        assertEquals("Hello. How can I help you?",GreetingPolicy.greeting(9,AppLanguage.EN,false,true,"Sepehr"))
        val guest=GreetingPolicy.greeting(9,AppLanguage.EN,true,false,"Alex");assertTrue(guest.contains("Alex"));assertFalse(guest.contains("sir"))
        assertEquals("Hola. ¿Cómo puedo ayudarte?",GreetingPolicy.greeting(9,AppLanguage.ES,false,false,null))
        assertTrue(GreetingPolicy.greeting(9,AppLanguage.ES,true,true,"Sepehr").contains("señor"))
    }
    @Test fun customGreetingBoundariesAreOrderedAndApplied(){
        assertFalse(GreetingPolicy.validBounds(12,5,18,22));assertTrue(GreetingPolicy.validBounds(7,13,19,23))
        assertTrue(GreetingPolicy.greeting(6,AppLanguage.EN,true,true,null,7,13,19,23).startsWith("Good night"))
        assertThrows(IllegalArgumentException::class.java){GreetingPolicy.greeting(25,AppLanguage.EN,false,false,null)}
    }
    @Test fun rmsAndClippingComeFromActualPcmSamples(){
        val raw=ByteArray(32000)
        for(i in 0 until 16000){val v=(sin(2*PI*440*i/16000)*.5*32767).toInt();raw[i*2]=(v and 255).toByte();raw[i*2+1]=(v shr 8).toByte()}
        val measured=VoiceMeasurements.measure(raw);assertEquals(1.0,measured.seconds,.0001);assertEquals(.5/sqrt(2.0),measured.rms,.001);assertEquals(0.0,measured.clippingFraction,0.0)
        assertEquals(-120.0,VoiceMeasurements.measure(ByteArray(32)).dbfs,.01)
    }
    @Test fun speechSegmentsRemainOneTurnAcrossShortPauses(){
        var now=0L;val a=UtteranceAssembler(now={now});a.segment("explain artificial intelligence")
        now=500;assertFalse(a.ready());a.partial("more simply");now=1700;assertFalse(a.ready())
        a.segment("more simply");now=2701;assertTrue(a.ready());assertEquals("explain artificial intelligence more simply",a.text())
    }
    @Test fun incompleteClauseAllowsLongerPauseAndSpeechResetsEndpoint(){
        var now=0L;val a=UtteranceAssembler(now={now});a.segment("tell me about")
        now=1200;assertFalse(a.ready());a.energy(.1f);now=3000;assertFalse(a.ready());now=3701;assertTrue(a.ready())
        assertTrue(UtteranceAssembler.fastCommand("open Spotify"));assertFalse(UtteranceAssembler.fastCommand("open Spotify and show weather"))
    }
    @Test fun abandonedOrNoiseOnlySessionsCannotProduceACompletedUtterance(){
        var now=0L;val a=UtteranceAssembler(now={now});now=30_000;a.energy(.9f);assertFalse(a.ready());assertEquals("",a.text())
        a.segment("very");a.segment("very");assertEquals("very very",a.text());a.reset();assertFalse(a.ready())
    }
}
