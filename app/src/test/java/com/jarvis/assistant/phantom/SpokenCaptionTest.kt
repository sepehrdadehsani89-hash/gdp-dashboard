package com.jarvis.assistant.phantom

import org.junit.Assert.*
import org.junit.Test

/** Speech-event regressions. These do not substitute for Android TTS/device tests. */
class SpokenCaptionTest {
    private class Session {
        var now = 1_000L
        val captions = PhantomSession { now }
        val trace = captions.submitted("Synthetic recognized question", false)
        fun start(text: String = "The actual spoken answer.", speech: Long = 1L): Boolean =
            captions.spokenStart(text, speech, trace)
    }

    @Test fun partialRecognitionDoesNotCreateACaption() {
        val s = Session()
        s.captions.begin(true)
        s.captions.partial("private provisional user words")
        assertEquals(LiveCaption(), s.captions.state.value.caption)
        assertEquals(ExecutionState.TRANSCRIBING, s.captions.state.value.execution)
    }

    @Test fun finalRecognitionDoesNotCreateACaption() {
        val s = Session()
        s.captions.begin(true)
        s.captions.submitted("private final user words", true)
        assertEquals(LiveCaption(), s.captions.state.value.caption)
        assertEquals(ExecutionState.UNDERSTANDING, s.captions.state.value.execution)
    }

    @Test fun recognitionCorrectionDoesNotBecomeAssistantSpeech() {
        val s = Session()
        s.captions.corrected("corrected private user words")
        assertEquals(LiveCaption(), s.captions.state.value.caption)
    }

    @Test fun generationAndAudioPreparationStayCaptionFree() {
        val s = Session()
        s.captions.stage(ExecutionState.GENERATING_RESPONSE)
        s.captions.event(TraceEvent.FIRST_TOKEN)
        s.captions.stage(ExecutionState.GENERATING_AUDIO)
        s.captions.event(TraceEvent.AUDIO_BUFFERED)
        assertFalse(s.captions.state.value.caption.visible(s.now))
    }

    @Test fun textOnlyCompletionDoesNotClaimThatAnythingWasSpoken() {
        val s = Session()
        s.captions.finish()
        assertFalse(s.captions.state.value.caption.visible(s.now))
        assertTrue(s.captions.state.value.caption.assistant.isBlank())
        assertFalse(s.start("late playback after completion"))
    }

    @Test fun actualSpokenPayloadIsShownInsteadOfRecognizedWords() {
        val s = Session()
        s.captions.corrected("a corrected recognized question")
        assertTrue(s.start("Actual spoken response in Español."))
        val caption = s.captions.state.value.caption
        assertEquals("Actual spoken response in Español.", caption.assistant)
        assertEquals("", caption.user)
        assertFalse(caption.provisional)
        assertTrue(caption.visible(s.now))
    }

    @Test fun emptyPlaybackCannotCreateAnAudibleCaption() {
        val s = Session()
        assertFalse(s.start(" \n\t"))
        assertEquals(LiveCaption(), s.captions.state.value.caption)
    }

    @Test fun obsoleteRequestStartCannotOverwriteANewerCaption() {
        val s = Session()
        val newer = s.captions.submitted("new question", false)
        assertTrue(s.captions.spokenStart("new spoken answer", 2, newer))
        assertFalse(s.captions.spokenStart("obsolete answer", 1, s.trace))
        assertEquals("new spoken answer", s.captions.state.value.caption.assistant)
    }

    @Test fun obsoletePlaybackEndCannotClearTheCurrentSegment() {
        val s = Session()
        assertTrue(s.start("first segment", 1))
        assertTrue(s.start("second segment", 2))
        assertFalse(s.captions.spokenEnd(1, 0))
        assertEquals("second segment", s.captions.state.value.caption.assistant)
        assertEquals(0L, s.captions.state.value.caption.expiresAt)
    }

    @Test fun privacyClearInvalidatesTheActivePlaybackEnd() {
        val s = Session()
        assertTrue(s.start())
        s.captions.clearCaption()
        assertFalse(s.captions.spokenEnd(1))
        assertFalse(s.start("delayed duplicate start", 1))
        assertEquals(LiveCaption(), s.captions.state.value.caption)
    }

    @Test fun successfulSpeechExpiresAtTheConfiguredDelayBoundary() {
        val s = Session()
        assertTrue(s.start())
        s.now = 2_000
        assertTrue(s.captions.spokenEnd(1, 750))
        s.now = 2_749
        assertTrue(s.captions.state.value.caption.visible(s.now))
        s.now = 2_750
        assertFalse(s.captions.state.value.caption.visible(s.now))
    }

    @Test fun stoppedSpeechCanClearWithoutAnyLingeringText() {
        val s = Session()
        assertTrue(s.start())
        assertTrue(s.captions.spokenEnd(1, 0))
        assertEquals(LiveCaption(), s.captions.state.value.caption)
    }

    @Test fun negativeDelayCannotLeaveATimelessCaption() {
        val s = Session()
        assertTrue(s.start())
        assertTrue(s.captions.spokenEnd(1, -500))
        assertFalse(s.captions.state.value.caption.visible(s.now))
    }

    @Test fun excessiveDelayCannotRetainSpeechBeyondFiveSeconds() {
        val s = Session()
        assertTrue(s.start())
        assertTrue(s.captions.spokenEnd(1, Long.MAX_VALUE))
        assertEquals(6_000L, s.captions.state.value.caption.expiresAt)
        s.now = 6_000
        assertFalse(s.captions.state.value.caption.visible(s.now))
    }

    @Test fun completionDoesNotRestartTheSpeechExpiryTimer() {
        val s = Session()
        assertTrue(s.start())
        assertTrue(s.captions.spokenEnd(1, 900))
        s.now = 1_500
        s.captions.finish()
        assertEquals(1_900L, s.captions.state.value.caption.expiresAt)
    }

    @Test fun awaitingConfirmationDoesNotExtendAnOldSpokenCaption() {
        val s = Session()
        assertTrue(s.start("Please review the confirmation."))
        assertTrue(s.captions.spokenEnd(1, 500))
        s.now = 1_400
        s.captions.finish(ExecutionState.WAITING_USER)
        assertEquals(1_500L, s.captions.state.value.caption.expiresAt)
    }

    @Test fun outputFailureRemovesTheCaptionImmediately() {
        val s = Session()
        assertTrue(s.start())
        s.captions.finish(ExecutionState.FAILED)
        assertEquals(LiveCaption(), s.captions.state.value.caption)
        assertFalse(s.captions.spokenEnd(1))
        assertFalse(s.start("late playback after failure", 2))
    }

    @Test fun cancellationCannotBeUndoneByLatePlaybackCompletion() {
        val s = Session()
        assertTrue(s.start())
        s.captions.finish(ExecutionState.CANCELLED)
        assertFalse(s.captions.spokenEnd(1))
        assertFalse(s.start("late playback after cancellation", 2))
        assertEquals(LiveCaption(), s.captions.state.value.caption)
    }

    @Test fun newListeningTurnDoesNotReuseThePreviousAnswer() {
        val s = Session()
        assertTrue(s.start("previous answer"))
        s.captions.begin(true)
        s.captions.partial("new user question")
        assertEquals(LiveCaption(), s.captions.state.value.caption)
        assertFalse(s.captions.spokenEnd(1))
    }

    @Test fun speechCaptionFadesWhileReducedMotionKeepsTextLegibleUntilExpiry() {
        val s = Session()
        assertTrue(s.start())
        assertTrue(s.captions.spokenEnd(1, 1_200))
        s.now = 1_700
        val caption = s.captions.state.value.caption
        assertEquals(.5f, caption.alpha(s.now, false), .001f)
        assertEquals(1f, caption.alpha(s.now, true), 0f)
        s.now = 2_200
        assertEquals(0f, caption.alpha(s.now, false), 0f)
        assertEquals(0f, caption.alpha(s.now, true), 0f)
    }
}
