package com.jarvis.assistant.voice

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.sentinel.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Deterministic text/clock/state regressions. These are not acoustic recognition measurements. */
class FinishPolishVoiceTest {
    @Test fun oneHundredLogicalWakeTrialsPreserveWholePrefixAndRejectEmbeddedMentions(){
        repeat(100){
            assertEquals("",WakePhraseMatcher.extractCommand("Jarvis, are you up?",AppLanguage.EN))
            assertEquals("what time is it",WakePhraseMatcher.extractCommand("Jarvis, are you up? What time is it?",AppLanguage.EN))
            assertNull(WakePhraseMatcher.extractCommand("I asked Jarvis are you up yesterday",AppLanguage.EN))
            assertNull(WakePhraseMatcher.extractCommand("hey jarvison",AppLanguage.EN))
        }
    }
    @Test fun thirtyLogicalFollowUpsStartOnlyWhenCaptureIsReadyAndDoNotNeedAnotherWake(){
        repeat(30){var at=1000L;val window=ConversationWindow{at};window.enable();assertTrue(window.wake());assertTrue(window.accept());window.resume(true)
            at+=1000;assertFalse(window.acceptsCommand());assertTrue(window.captureReady());assertTrue(window.acceptsCommand());assertFalse(window.wake());assertTrue(window.accept())}
    }
    @Test fun twentyCombinedUtterancesKeepAllTheCommandAfterWake(){
        repeat(20){i->val text="open Chrome and search for astronomy $i";assertEquals(text.lowercase(),WakePhraseMatcher.extractCommand("Hey Jarvis, $text",AppLanguage.EN))}
    }
    @Test fun customWakePhraseIsOptionalValidatedAndKeepsItsSuffix(){
        assertTrue(WakePhraseMatcher.validCustomPhrase("Good evening Jarvis"));assertEquals("what time is it",WakePhraseMatcher.extractCommand("Good evening Jarvis, what time is it?",AppLanguage.EN,"Good evening Jarvis"))
        for(p in listOf("stop everything","open bank","Jarvis","say 123","x y","a".repeat(61)))assertFalse(p,WakePhraseMatcher.validCustomPhrase(p))
        assertNull(WakePhraseMatcher.extractCommand("Good evening Jarvis",AppLanguage.EN));assertNull(WakePhraseMatcher.extractCommand("stop everything",AppLanguage.EN,"stop everything"))
    }
    @Test fun localizedReadinessPhrasePreservesCommands(){assertEquals("que hora es",WakePhraseMatcher.extractCommand("Jarvis estas ahi, que hora es?",AppLanguage.ES));assertEquals("",WakePhraseMatcher.extractCommand("Jarvis, estás ahí?",AppLanguage.ES))}
    @Test fun identityIsKnownLocallyWithoutAnInstalledModelOrCloud(){
        assertEquals(LocalVoiceQuery.IDENTITY,LocalVoiceQueries.parse("What's your name?"));assertTrue(LocalKnowledge.answer("who are you",AppLanguage.EN)!!.contains("J.A.R.V.I.S."));assertTrue(LocalKnowledge.answer("cómo te llamas",AppLanguage.ES)!!.contains("Soy J.A.R.V.I.S."))
    }
    @Test fun timeZoneUsesTheActualInjectedDeviceClockAndDoesNotClaimGpsLocation(){
        val answer=LocalVoiceQueries.answer(LocalVoiceQuery.TIME_ZONE,AppLanguage.EN,Clock.fixed(Instant.parse("2026-11-11T20:00:00Z"),ZoneId.of("America/Guayaquil")))
        assertEquals("Your phone's time zone is America/Guayaquil (UTC-05:00).",answer)
    }
    private fun calculate(text:String)=LocalArithmetic.evaluate(requireNotNull(LocalArithmetic.request(text)))
    @Test fun localArithmeticHandlesOrderParenthesesNegativesAndDecimals(){
        assertEquals("14",calculate("what is 2 plus 3 times 4").value);assertEquals("20",calculate("calculate (2 + 3) * 4").value)
        assertEquals("-7",calculate("what is negative five minus two").value);assertEquals("0.3",calculate("what is 0.1 + 0.2").value)
        assertEquals("5.5",calculate("what is two point five plus three").value)
    }
    @Test fun englishAndSpanishNumberWordsAndPercentagesUseTheSameDeterministicGrammar(){
        assertEquals("105",calculate("calculate one hundred and five + zero").value);assertEquals("37",calculate("¿Cuánto es treinta y dos más cinco?").value)
        assertEquals("12",calculate("what is 15 percent of 80").value);assertEquals("12",calculate("cuánto es 15 por ciento de 80").value)
        assertEquals("10",calculate("calculate 20% * 50").value)
    }
    @Test fun divisionByZeroAndIncompleteArithmeticAreTypedCalculatorErrors(){
        assertEquals(LocalArithmetic.Error.DIVIDE_BY_ZERO,calculate("what is 4 divided by zero").error)
        assertEquals(LocalArithmetic.Error.INVALID_EXPRESSION,calculate("calculate 2 +").error)
        assertTrue(LocalArithmetic.answer("what is 4 divided by zero",AppLanguage.EN)!!.contains("undefined"))
    }
    @Test fun longDivisionIsLabeledApproximateAndIntegerPrecisionIsPreserved(){
        val third=calculate("calculate 1 / 3");assertTrue(third.approximate);assertEquals("0.3333333333",third.value)
        assertEquals("10000000000000000",calculate("calculate 9999999999999999 + 1").value)
        assertFalse(calculate("calculate 1 / 4").approximate)
    }
    @Test fun arithmeticCannotEvaluateCodeCredentialsOrACompoundDeviceAction(){
        for(text in listOf("calculate Runtime.getRuntime()", "what is my password", "what is 2 plus 2 and call Alex", "2; delete everything", "1000000 ** 1000000", "hundred + one", "twenty twenty plus one"))assertNull(text,LocalArithmetic.request(text))
    }
    @Test fun calculatorHasExplicitTokenMagnitudeAndRecursionLimits(){
        assertEquals(LocalArithmetic.Error.LIMIT_EXCEEDED,LocalArithmetic.evaluate(LocalArithmetic.Request("1"+"0".repeat(40)+" + 1")).error)
        assertEquals(LocalArithmetic.Error.LIMIT_EXCEEDED,LocalArithmetic.evaluate(LocalArithmetic.Request((List(18){"("}+listOf("1")+List(18){")"}).joinToString(" "))).error)
    }
    @Test fun publicLocalMathDoesNotAccidentallyRelaxADeviceAction(){assertTrue(LocalVoiceQueries.readOnly("what is two plus two"));assertFalse(LocalVoiceQueries.readOnly("what is two plus two and call Alex"))}
    @Test fun rendererColorsAndChargingAccentFollowActualDistinctStates(){
        assertEquals(ReactorPalette.cyan,ReactorPalette.core(SentinelMode.DORMANT));assertEquals(ReactorPalette.purple,ReactorPalette.core(SentinelMode.LISTENING));assertEquals(ReactorPalette.blue,ReactorPalette.core(SentinelMode.SPEAKING));assertEquals(ReactorPalette.warning,ReactorPalette.core(SentinelMode.ERROR))
        assertTrue(ReactorPalette.chargingAccent(true,true,true));assertFalse(ReactorPalette.chargingAccent(true,false,true));assertFalse(ReactorPalette.chargingAccent(false,true,true));assertFalse(ReactorPalette.chargingAccent(null,true,true));assertFalse(ReactorPalette.chargingAccent(true,true,false))
    }
}
