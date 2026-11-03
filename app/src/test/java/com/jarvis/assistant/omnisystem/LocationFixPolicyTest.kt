package com.jarvis.assistant.omnisystem
import org.junit.Assert.*
import org.junit.Test
class LocationFixPolicyTest {
    @Test fun acceptsFreshApproximateFixButRejectsStaleFutureAndInvalidReadings() {
        assertTrue(LocationFixPolicy.usable(10_000_000_000,1500f,-2.19,-79.88))
        assertFalse(LocationFixPolicy.usable(-1,10f,0.0,0.0))
        assertFalse(LocationFixPolicy.usable(120_000_000_001,10f,0.0,0.0))
        assertFalse(LocationFixPolicy.usable(0,Float.NaN,0.0,0.0))
        assertFalse(LocationFixPolicy.usable(0,10f,91.0,0.0))
        assertFalse(LocationFixPolicy.usable(0,10f,0.0,Double.NaN))
        assertFalse(LocationFixPolicy.usable(0,10001f,0.0,0.0))
    }
}
