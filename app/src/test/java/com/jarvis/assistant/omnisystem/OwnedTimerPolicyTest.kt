package com.jarvis.assistant.omnisystem

import org.junit.Assert.*
import org.junit.Test

class OwnedTimerPolicyTest {
    private val timer=OwnedTimer(1,"fixture-nonce-123456789",60,1000,61000,4,"SCHEDULED")
    @Test fun timerUsesMonotonicDeadlineAndClampsAtZero(){
        OwnedTimerPolicy.validate(timer)
        assertEquals(60000L,OwnedTimerPolicy.remaining(timer,1000,4))
        assertEquals(30000L,OwnedTimerPolicy.remaining(timer,31000,4))
        assertEquals(0L,OwnedTimerPolicy.remaining(timer,90000,4))
        assertEquals(.5f,OwnedTimerPolicy.fraction(timer,31000,4)!!,0f)
        assertEquals(1f,OwnedTimerPolicy.fraction(timer,90000,4)!!,0f)
    }
    @Test fun rebootsAndNotificationsCannotPretendToBeActiveCountdowns(){
        assertNull(OwnedTimerPolicy.remaining(timer,2000,5))
        assertNull(OwnedTimerPolicy.remaining(timer,999,4))
        for(state in listOf("INTERRUPTED","ALERT_REQUESTED","ALERT_BLOCKED","CANCELLED"))assertNull(OwnedTimerPolicy.remaining(timer.copy(status=state),3000,4))
    }
    @Test fun invalidDurationsDeadlinesAndStatesCannotBeRestored(){
        for(bad in listOf(timer.copy(seconds=0),timer.copy(seconds=86401),timer.copy(deadline=61001),timer.copy(boot=-1),timer.copy(id=-1),timer.copy(nonce="short"),timer.copy(status="DELIVERED"))){
            assertThrows(IllegalArgumentException::class.java){OwnedTimerPolicy.validate(bad)}
        }
    }
}
