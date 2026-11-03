package com.jarvis.assistant.neural

import org.junit.Assert.*
import org.junit.Test

class CompoundDevicePlanTest {
    @Test fun explicitWeatherBluetoothAndTimerPlanUsesBoundedReviewedTools(){
        val input="weather in Quito then show my Bluetooth devices then set a timer for 5 minutes"
        val plan=ToolRegistry.directPlan(input)!!
        assertEquals(listOf("weather","bluetooth_devices","set_timer"),plan.map{it.id})
        assertTrue(plan.all{ToolRegistry.authorizedByInput(it,input) && ToolRegistry.publicArgumentsBound(it,input,"Quito")})
        assertEquals("set a timer for 5 minutes",ToolRegistry.boundDeviceClause(plan.last(),input))
        assertFalse(ToolRegistry.publicArgumentsBound(ToolRequest("set_timer",mapOf("seconds" to "60")),input,"Quito"))
    }
    @Test fun modelPlansCannotProposeTheseDeviceActions(){
        for(id in listOf("set_timer","set_alarm","bluetooth_devices"))assertThrows(IllegalArgumentException::class.java){ToolRegistry.parse("""{"tools":[{"id":"$id","arguments":{}}]}""")}
    }
    @Test fun noScanConnectionOrNegativeClauseCanBecomeAutomaticBluetoothQuery(){
        assertNull(ToolRegistry.directPlan("weather in Quito then connect Bluetooth"))
        assertNull(ToolRegistry.directPlan("weather in Quito then scan Bluetooth"))
        assertNull(ToolRegistry.directPlan("weather in Quito then do not set a timer for 5 minutes"))
        assertFalse(ToolRegistry.authorizedByInput(ToolRequest("bluetooth_devices",emptyMap()),"weather in Quito"))
        assertFalse(ToolRegistry.authorizedByInput(ToolRequest("set_timer",mapOf("seconds" to "null")),"weather in Quito"))
    }
}
