package com.jarvis.assistant.voice

import org.junit.Assert.*
import org.junit.Test

class ConversationWindowTest {
    @Test fun aWakeOpensExactlyTenSecondsAndNoUnprefixedCommandBeforeIt(){
        var now=100L;val gate=ConversationWindow{now};gate.enable()
        assertFalse(gate.acceptsCommand());assertTrue(gate.wake())
        assertEquals(10_100,gate.snapshot().deadline)
        now=10_099;assertTrue(gate.acceptsCommand())
        now=10_100;assertFalse(gate.acceptsCommand());assertTrue(gate.expire())
        assertEquals(ConversationWindow.Mode.WAKE_ONLY,gate.snapshot().mode)
        assertFalse(gate.accept());assertTrue(gate.wake())
    }
    @Test fun repeatedWakePartialsNeverExtendTheWindow(){
        var now=0L;val gate=ConversationWindow{now};gate.enable();gate.wake()
        now=9_000;assertFalse(gate.wake());assertEquals(10_000,gate.snapshot().deadline)
        now=10_001;assertTrue(gate.expire());assertFalse(gate.accept())
    }
    @Test fun acceptingACommandPausesUntilTheResponseHasFinished(){
        var now=0L;val gate=ConversationWindow{now};gate.enable();gate.wake()
        now=1000;assertTrue(gate.accept());assertFalse(gate.accept())
        now=100_000;assertFalse(gate.acceptsCommand())
        gate.resume(true);assertEquals(ConversationWindow.Mode.WAITING_FOR_CAPTURE,gate.snapshot().mode)
        assertFalse(gate.acceptsCommand())
        now=101_000;assertTrue(gate.captureReady());assertEquals(111_000,gate.snapshot().deadline)
        now=110_999;assertTrue(gate.acceptsCommand()) // no second wake phrase
        now=111_000;assertFalse(gate.acceptsCommand());assertTrue(gate.expire())
    }
    @Test fun everyReplyMayOpenOneFreshFollowUpWindow(){
        var now=0L;val gate=ConversationWindow{now};gate.enable();gate.wake();gate.accept()
        repeat(3){now+=30_000;gate.resume(true);assertTrue(gate.captureReady());assertFalse(gate.captureReady());assertEquals(now+10_000,gate.snapshot().deadline);now+=500;assertTrue(gate.accept())}
        gate.resume(true);gate.captureReady();now+=10_000;gate.expire();assertFalse(gate.accept())
    }
    @Test fun staleRecognizerResultsCannotReopenExpiredWindows(){
        var now=0L;val gate=ConversationWindow{now};gate.enable();gate.wake();val old=gate.snapshot().resetSerial
        now=10_000;gate.expire();assertFalse(gate.accept(old));gate.wake();assertFalse(gate.accept(old));assertTrue(gate.accept())
    }
    @Test fun unrelatedResumeReturnsToWakeOnlyAndStopCannotBeResurrected(){
        val gate=ConversationWindow{0L};gate.enable();gate.wake();gate.pause();gate.resume(false)
        assertEquals(ConversationWindow.Mode.WAKE_ONLY,gate.snapshot().mode);assertFalse(gate.accept())
        gate.disable();gate.resume(true);assertFalse(gate.captureReady());assertFalse(gate.wake());assertFalse(gate.accept())
    }
}
