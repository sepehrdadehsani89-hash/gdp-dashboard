package com.jarvis.assistant.voice

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Deterministic timing and lifecycle checks; these do not simulate acoustic/device accuracy. */
class EndpointReliabilityTest {
    @Test fun configuredNineHundredMillisecondPauseRequestsAnEngineFinal() {
        var now = 0L
        val turn = UtteranceAssembler({ now }, pauseMs = 900)
        turn.partial("explain solar eclipses")
        now = 899; assertFalse(turn.needsFinalization())
        now = 900; assertTrue(turn.needsFinalization())
        assertFalse(turn.ready()); assertEquals("", turn.acceptedText())
    }

    @Test fun acceptedFinalWaitsForConfiguredNaturalPause() {
        var now = 0L
        val turn = UtteranceAssembler({ now }, pauseMs = 900)
        turn.segment("explain solar eclipses")
        now = 899; assertFalse(turn.ready())
        now = 900; assertTrue(turn.ready())
    }

    @Test fun minimumPauseAllowsSixHundredFiftyMilliseconds() {
        var now = 0L
        val turn = UtteranceAssembler({ now }, pauseMs = 650)
        turn.partial("describe the moon")
        now = 649; assertFalse(turn.needsFinalization())
        now = 650; assertTrue(turn.needsFinalization())
    }

    @Test fun maximumPauseAllowsEighteenHundredMilliseconds() {
        var now = 0L
        val turn = UtteranceAssembler({ now }, pauseMs = 1800)
        turn.partial("describe the moon")
        now = 1799; assertFalse(turn.needsFinalization())
        now = 1800; assertTrue(turn.needsFinalization())
    }

    @Test fun invalidShortPauseCannotCutOffSpeechImmediately() {
        var now = 0L
        val turn = UtteranceAssembler({ now }, pauseMs = -100)
        turn.partial("describe the moon")
        now = 649; assertFalse(turn.needsFinalization())
        now = 650; assertTrue(turn.needsFinalization())
    }

    @Test fun InvalidLongPauseCannotDisableEndpointing() {
        var now = 0L
        val turn = UtteranceAssembler({ now }, pauseMs = Long.MAX_VALUE)
        turn.partial("describe the moon")
        now = 1799; assertFalse(turn.needsFinalization())
        now = 1800; assertTrue(turn.needsFinalization())
    }

    @Test fun incompleteEnglishClauseGetsALongerPause() {
        var now = 0L
        val turn = UtteranceAssembler({ now }, pauseMs = 900)
        turn.partial("tell me about")
        now = 900; assertFalse(turn.needsFinalization())
        now = 1799; assertFalse(turn.needsFinalization())
        now = 1800; assertTrue(turn.needsFinalization())
    }

    @Test fun incompleteSpanishClauseGetsALongerPause() {
        var now = 0L
        val turn = UtteranceAssembler({ now }, pauseMs = 900)
        turn.partial("explica por qué porque")
        now = 900; assertFalse(turn.needsFinalization())
        now = 1800; assertTrue(turn.needsFinalization())
    }

    @Test fun stableWordsReachFinalizationDespiteContinuousRoomEnergy() {
        var now = 0L
        val turn = UtteranceAssembler({ now })
        turn.partial("explain solar eclipses")
        repeat(24) { now += 100; turn.energy(.8f); assertFalse(turn.needsFinalization()) }
        now = 2500; turn.energy(.8f)
        assertTrue(turn.needsFinalization()); assertFalse(turn.ready())
    }

    @Test fun incompleteStableWordsHaveABoundedFourSecondNoiseFallback() {
        var now = 0L
        val turn = UtteranceAssembler({ now })
        turn.partial("tell me about")
        repeat(39) { now += 100; turn.energy(.8f); assertFalse(turn.needsFinalization()) }
        now = 4000; turn.energy(.8f)
        assertTrue(turn.needsFinalization())
    }

    @Test fun sameLengthDecoderRewritesCannotRenewTheTurnForever() {
        var now = 0L
        val turn = UtteranceAssembler({ now })
        turn.partial("explain this topic")
        repeat(4) { index ->
            now += 500; turn.partial("explain topic $index"); turn.energy(.8f)
            assertFalse(turn.needsFinalization())
        }
        now = 2500; turn.partial("explain another topic"); turn.energy(.8f)
        assertTrue(turn.needsFinalization()); assertEquals("", turn.acceptedText())
    }

    @Test fun ShrinkingAndOscillatingHypothesesCannotRenewGrowthDeadline() {
        var now = 0L
        val turn = UtteranceAssembler({ now })
        turn.partial("please explain this scientific topic")
        now = 1000; turn.partial("please explain"); turn.energy(.8f)
        now = 2000; turn.partial("please explain this scientific topic"); turn.energy(.8f)
        now = 2500; turn.partial("please explain this topic"); turn.energy(.8f)
        assertTrue(turn.needsFinalization())
    }

    @Test fun genuinelyGrowingSpeechCanExceedTheStablePartialDeadline() {
        var now = 0L
        val turn = UtteranceAssembler({ now })
        repeat(20) { index ->
            turn.partial((0..index).joinToString(" ") { "word$it" })
            now += 500; turn.energy(.8f)
            assertFalse(turn.needsFinalization()); assertFalse(turn.ready())
        }
    }

    @Test fun continuouslyGrowingBackgroundSpeechStillReachesAbsoluteCaptureLimit() {
        var now = 0L
        val clock = SpeechInactivityClock({ now })
        val turn = UtteranceAssembler({ now })
        repeat(120) { index ->
            val partial = (0..index).joinToString(" ") { "word$it" }
            clock.progress(partial); turn.partial(partial)
            now += 500; turn.energy(.8f)
            assertFalse(turn.needsFinalization())
            if (now < 60_000) assertFalse(clock.expired())
        }
        assertTrue(clock.expired()); assertFalse(clock.progress("a late command"))
        assertFalse(turn.ready()); assertEquals("", turn.acceptedText())
    }

    @Test fun noiseWithoutRecognizedWordsCannotCreateACommand() {
        var now = 0L
        val turn = UtteranceAssembler({ now })
        repeat(100) { now += 100; turn.energy(.9f); turn.partial("") }
        assertFalse(turn.ready()); assertFalse(turn.needsFinalization())
        assertEquals("", turn.text()); assertEquals("", turn.acceptedText())
    }

    @Test fun aPartialActionNeverBecomesAnAcceptedFinalByWaiting() {
        var now = 0L
        val turn = UtteranceAssembler({ now })
        turn.partial("lock my phone")
        now = 60_000
        assertTrue(turn.needsFinalization()); assertFalse(turn.ready())
        assertEquals("", turn.acceptedText())
    }

    @Test fun aLowConfidenceActionFinalCannotBeReplacedByDisplayedPartial() {
        var now = 0L
        val turn = UtteranceAssembler({ now })
        turn.partial("lock my phone")
        val rejected = JSONObject("""{"text":"lock my phone","result":[{"word":"lock","conf":0.1}]}""")
        assertEquals(FinalRecognitionKind.UNCERTAIN, RecognitionConfidence.kind(rejected))
        now = 2500
        assertTrue(turn.needsFinalization()); assertFalse(turn.ready()); assertEquals("", turn.acceptedText())
    }

    @Test fun anUnfinishedCompoundTailPreventsDispatchOfEarlierAcceptedWords() {
        var now = 0L
        val turn = UtteranceAssembler({ now })
        turn.segment("open the clock and")
        turn.partial("call Alex")
        now = 10_000
        assertFalse(turn.ready()); assertEquals("open the clock and", turn.acceptedText())
        assertEquals("open the clock and call Alex", turn.text())
    }

    @Test fun emptyFinalWithoutAnyAcceptedWordsCannotCreateACommand() {
        val turn = UtteranceAssembler({ 0L })
        turn.partial("uncertain noise"); turn.emptyFinal(true)
        assertFalse(turn.ready()); assertFalse(turn.hasUnfinishedSpeech); assertEquals("", turn.acceptedText())
    }

    @Test fun emptyDecoderBoundaryPreservesPreviouslyAcceptedFinalWords() {
        val turn = UtteranceAssembler({ 0L })
        turn.segment("explain solar eclipses"); turn.emptyFinal(true)
        assertTrue(turn.ready()); assertEquals("explain solar eclipses", turn.acceptedText())
    }

    @Test fun emptyPartialDoesNotEraseAnUnfinishedUtterance() {
        val turn = UtteranceAssembler({ 0L })
        turn.partial("describe the moon"); turn.partial("  ")
        assertTrue(turn.hasUnfinishedSpeech); assertEquals("describe the moon", turn.text()); assertFalse(turn.ready())
    }

    @Test fun resetCannotCarryAcceptedWordsOrGrowthDeadlineIntoNewCapture() {
        var now = 0L
        val turn = UtteranceAssembler({ now })
        turn.segment("old command"); turn.partial("unfinished tail"); turn.finalizedEndpoint()
        now = 10_000; turn.reset(); turn.partial("new question")
        assertFalse(turn.ready()); assertFalse(turn.needsFinalization()); assertEquals("", turn.acceptedText())
        assertEquals("new question", turn.text())
    }

    @Test fun invalidAndSubthresholdEnergyCannotDelayNaturalSilence() {
        var now = 0L
        val turn = UtteranceAssembler({ now }, threshold = .1f)
        turn.partial("explain eclipses")
        now = 900; listOf(Float.NaN, Float.POSITIVE_INFINITY, -.1f, .09f).forEach(turn::energy)
        now = 1000; assertTrue(turn.needsFinalization())
    }

    @Test fun captureThatReturnsNoPcmStopsAtThreeSecondBoundary() {
        var now = 1000L
        val reads = MicrophoneReadClock({ now })
        now = 3999; assertFalse(reads.stalled())
        now = 4000; assertTrue(reads.stalled())
    }

    @Test fun actualPcmProgressRenewsOnlyTheReadStallDeadline() {
        var now = 0L
        val reads = MicrophoneReadClock({ now })
        now = 2500; reads.progress()
        now = 5499; assertFalse(reads.stalled())
        now = 5500; assertTrue(reads.stalled())
    }

    @Test fun backwardMonotonicClockFailsReadStallClosed() {
        var now = 1000L
        val reads = MicrophoneReadClock({ now })
        now = 999; assertTrue(reads.stalled())
    }

    @Test fun customReadDeadlineStillHasAnExactFiniteBoundary() {
        var now = 0L
        val reads = MicrophoneReadClock({ now }, maximumGapMs = 500)
        now = 499; assertFalse(reads.stalled())
        now = 500; assertTrue(reads.stalled())
    }

    @Test(expected = IllegalArgumentException::class)
    fun aNonpositiveReadDeadlineIsRejected() { MicrophoneReadClock({ 0L }, maximumGapMs = 0) }

    @Test fun diagnosticCaptureReadinessStartsActualCaptureClock() {
        val token = EndpointDiagnostics.begin("Vosk local", 1000)
        assertEquals(EndpointStage.WAITING_FOR_MIC.name, EndpointDiagnostics.state.value.state)
        EndpointDiagnostics.captureReady(token, 5000)
        EndpointDiagnostics.activity(token, 5500, true)
        val reading = EndpointDiagnostics.state.value
        assertEquals(EndpointStage.SPEECH_ACTIVE.name, reading.state); assertEquals(500L, reading.elapsedMs)
        assertNull(reading.finalizationMs); assertTrue(reading.vadCapability.contains("no neural VAD"))
    }

    @Test fun diagnosticReportsMeasuredTimeFromFinalizationRequestToCompletion() {
        val token = EndpointDiagnostics.begin("Android provider", 1000)
        EndpointDiagnostics.captureReady(token, 1100)
        EndpointDiagnostics.candidate(token, 1800); EndpointDiagnostics.finalizing(token, 1800)
        EndpointDiagnostics.complete(token, 2140)
        val reading = EndpointDiagnostics.state.value
        assertEquals(EndpointStage.COMPLETED.name, reading.state); assertEquals(1040L, reading.elapsedMs)
        assertEquals(340L, reading.finalizationMs)
    }

    @Test fun repeatedFinalizationCallbacksCannotResetLatencyMeasurement() {
        val token = EndpointDiagnostics.begin("Android provider", 0)
        EndpointDiagnostics.captureReady(token, 0)
        EndpointDiagnostics.finalizing(token, 1000); EndpointDiagnostics.finalizing(token, 1200)
        EndpointDiagnostics.complete(token, 1400)
        assertEquals(400L, EndpointDiagnostics.state.value.finalizationMs)
    }

    @Test fun callbacksFromRetiredCaptureCannotOverwriteCurrentDiagnostic() {
        val old = EndpointDiagnostics.begin("old provider", 0)
        val current = EndpointDiagnostics.begin("current provider", 2000)
        EndpointDiagnostics.captureReady(current, 2100)
        val expected = EndpointDiagnostics.state.value
        EndpointDiagnostics.captureReady(old, 3000); EndpointDiagnostics.activity(old, 3100, true)
        EndpointDiagnostics.finalizing(old, 3200); EndpointDiagnostics.fail(old, 3300, "OLD_ERROR")
        EndpointDiagnostics.complete(old, 3400); EndpointDiagnostics.cancel(old, 3500)
        assertEquals(expected, EndpointDiagnostics.state.value)
    }

    @Test fun invalidRawErrorMessagesAreReplacedWithMetadataOnlyCode() {
        val token = EndpointDiagnostics.begin("Vosk local", 0)
        EndpointDiagnostics.fail(token, 1000, "private transcript / secret-token")
        assertEquals("ENDPOINT_ERROR", EndpointDiagnostics.state.value.error)
        assertFalse(EndpointDiagnostics.state.value.toString().contains("secret-token"))
    }

    @Test fun boundedDiagnosticErrorCodeIsRetainedForTroubleshooting() {
        val token = EndpointDiagnostics.begin("Vosk local", 0)
        EndpointDiagnostics.fail(token, 1000, "MICROPHONE_STALLED")
        assertEquals(EndpointStage.ERROR.name, EndpointDiagnostics.state.value.state)
        assertEquals("MICROPHONE_STALLED", EndpointDiagnostics.state.value.error)
    }

    @Test fun completionCannotBeReopenedByLateCaptureCallbacks() {
        val token = EndpointDiagnostics.begin("Vosk local", 0)
        EndpointDiagnostics.captureReady(token, 100); EndpointDiagnostics.finalizing(token, 900)
        EndpointDiagnostics.complete(token, 1000)
        assertTerminalReadingRemains(token)
    }

    @Test fun failureCannotBeReplacedByLateSuccessOrActivity() {
        val token = EndpointDiagnostics.begin("Vosk local", 0)
        EndpointDiagnostics.fail(token, 1000, "MICROPHONE_STALLED")
        assertTerminalReadingRemains(token)
    }

    @Test fun userCancellationCannotBeReopenedByLateProviderCallbacks() {
        val token = EndpointDiagnostics.begin("Android provider", 0)
        EndpointDiagnostics.cancel(token, 1000)
        assertTerminalReadingRemains(token)
    }

    @Test fun newCaptureClearsPriorErrorAndFinalizationMetadata() {
        val old = EndpointDiagnostics.begin("old provider", 0)
        EndpointDiagnostics.finalizing(old, 900); EndpointDiagnostics.fail(old, 1000, "FINAL_TIMEOUT")
        EndpointDiagnostics.begin("new provider", 2000)
        val reading = EndpointDiagnostics.state.value
        assertEquals("new provider", reading.engine); assertEquals(0L, reading.elapsedMs)
        assertNull(reading.error); assertNull(reading.finalizationMs)
        assertEquals(EndpointStage.WAITING_FOR_MIC.name, reading.state)
    }

    @Test fun backwardClockNeverPublishesNegativeElapsedOrFinalizationTime() {
        val token = EndpointDiagnostics.begin("test provider", 1000)
        EndpointDiagnostics.captureReady(token, 1000); EndpointDiagnostics.finalizing(token, 1200)
        EndpointDiagnostics.complete(token, 500)
        assertEquals(0L, EndpointDiagnostics.state.value.elapsedMs)
        assertEquals(0L, EndpointDiagnostics.state.value.finalizationMs)
    }

    private fun assertTerminalReadingRemains(token: Long) {
        val expected = EndpointDiagnostics.state.value
        EndpointDiagnostics.captureReady(token, 2000); EndpointDiagnostics.activity(token, 2100, true)
        EndpointDiagnostics.candidate(token, 2200); EndpointDiagnostics.finalizing(token, 2300)
        EndpointDiagnostics.fail(token, 2400, "LATE_ERROR"); EndpointDiagnostics.complete(token, 2500)
        EndpointDiagnostics.cancel(token, 2600)
        assertEquals(expected, EndpointDiagnostics.state.value)
    }

    @Test fun lateActivityCannotTurnFinalizationBackIntoListening(){
        val token=EndpointDiagnostics.begin("Android provider",0)
        EndpointDiagnostics.captureReady(token,100);EndpointDiagnostics.finalizing(token,1000)
        EndpointDiagnostics.activity(token,1100,true);EndpointDiagnostics.candidate(token,1200)
        assertEquals(EndpointStage.FINALIZING.name,EndpointDiagnostics.state.value.state)
        EndpointDiagnostics.complete(token,1400)
        assertEquals(400L,EndpointDiagnostics.state.value.finalizationMs)
    }

    @Test fun emptyBoundaryCannotDropAnUnfinishedCompoundTail(){
        val turn=UtteranceAssembler({0L})
        turn.segment("open the clock and");turn.partial("call Alex")
        assertFalse(turn.emptyFinal(true));assertFalse(turn.ready())
        assertEquals("open the clock and call Alex",turn.text())
    }
    @Test fun emptyBoundaryCannotDropUnfinishedTailDuringOrdinaryDecoderCallback(){
        val turn=UtteranceAssembler({0L})
        turn.segment("show weather");turn.partial("and send it")
        assertFalse(turn.emptyFinal(false));assertFalse(turn.ready())
    }
    @Test fun resumedEmptyBoundaryClearsAbandonedLatencyButNotCaptureClock(){
        val token=EndpointDiagnostics.begin("Vosk",0)
        EndpointDiagnostics.captureReady(token,100);EndpointDiagnostics.finalizing(token,1000)
        EndpointDiagnostics.resumeAfterEmptyBoundary(token,1200)
        assertEquals(EndpointStage.LISTENING.name,EndpointDiagnostics.state.value.state)
        assertNull(EndpointDiagnostics.state.value.finalizationMs)
        EndpointDiagnostics.finalizing(token,1500);EndpointDiagnostics.complete(token,1700)
        assertEquals(200L,EndpointDiagnostics.state.value.finalizationMs)
        assertEquals(1600L,EndpointDiagnostics.state.value.elapsedMs)
    }
    @Test fun abandonedBoundaryCallbackCannotReopenCancelledCapture(){
        val token=EndpointDiagnostics.begin("Vosk",0)
        EndpointDiagnostics.cancel(token,100)
        EndpointDiagnostics.resumeAfterEmptyBoundary(token,200)
        assertEquals(EndpointStage.CANCELLED.name,EndpointDiagnostics.state.value.state)
    }
}
