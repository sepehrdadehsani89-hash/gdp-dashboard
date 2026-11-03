package com.jarvis.assistant.voice
import org.junit.Assert.*
import org.junit.Test
class SpeechInactivityClockTest {
    @Test fun activeRecognizedUtterancesCanExceedTenSeconds(){
        var now=0L;val c=SpeechInactivityClock{now}
        now=9000;assertTrue(c.progress("set an alarm"));assertEquals(19000L,c.deadline())
        now=18000;assertTrue(c.progress("set an alarm for tomorrow at seven"));assertFalse(c.expired())
        now=27999;assertFalse(c.expired());now=28000;assertTrue(c.expired())
    }
    @Test fun silenceAndRepeatedPartialsCannotExtendAWindow(){
        var now=0L;val c=SpeechInactivityClock{now}
        now=9000;assertFalse(c.energy(.8f));assertFalse(c.progress(""));assertTrue(c.progress("hello"))
        now=18000;assertFalse(c.progress("hello"));assertFalse(c.energy(Float.NaN));assertFalse(c.energy(0f))
        now=19000;assertTrue(c.expired());assertFalse(c.progress("late reply"))
    }
    @Test fun utterancesRemainBoundedEvenWithContinuousEnergy(){
        var now=0L;val c=SpeechInactivityClock{now};c.progress("hello")
        repeat(119){now+=1000;assertTrue(c.energy(.5f))}
        assertEquals(120000L,c.deadline());now=120000;assertTrue(c.expired());assertFalse(c.energy(.8f))
    }
    @Test fun wakeGateExtendsOnlyTheCurrentRecognizedCommand(){
        var now=0L;val gate=ConversationWindow{now};gate.enable();assertFalse(gate.speechProgress("hello"));gate.wake()
        now=9000;assertTrue(gate.speechProgress("weather tomorrow"));assertEquals(19000L,gate.snapshot().deadline)
        now=14000;assertTrue(gate.accept());assertFalse(gate.speechProgress("and Friday"))
        gate.resume(true);assertFalse(gate.speechProgress("old echo"));gate.captureReady();assertEquals(24000L,gate.snapshot().deadline)
    }
}
