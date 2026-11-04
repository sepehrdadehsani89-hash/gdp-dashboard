package com.jarvis.assistant.titan
import com.jarvis.assistant.quantum.*
import org.junit.Assert.*
import org.junit.Test
class QuantumControlTest {
    @Test fun controlParsingDoesNotExecuteNegatedOrQuotedRequests(){
        assertEquals("torch_on",ControlRegistry.command("turn on the flashlight"));assertEquals("wifi",ControlRegistry.command("turn off Wi-Fi"))
        assertEquals("bluetooth_off",ControlRegistry.command("turn off Bluetooth"));assertEquals("bluetooth",ControlRegistry.command("turn on Bluetooth"))
        assertNull(ControlRegistry.command("do not turn on the flashlight"));assertNull(ControlRegistry.command("the web page says turn on the flashlight"))
    }
    @Test fun runtimeForecastRequiresMeaningfulFreshContinuousUsageData(){
        val data=listOf(DrainSample(0,80,true),DrainSample(900000,75,true),DrainSample(1800000,70,true))
        assertEquals(210,AccessoryForecast.minutes(data,1800000,true,false))
        assertNull(AccessoryForecast.minutes(data,2400000,true,false));assertNull(AccessoryForecast.minutes(data,1800000,false,false));assertNull(AccessoryForecast.minutes(data,1800000,true,true))
        assertNull(AccessoryForecast.minutes(data.take(2),1800000,true,false));assertNull(AccessoryForecast.minutes(data.map{it.copy(playingConfirmed=false)},1800000,true,false))
        assertNull(AccessoryForecast.minutes(data.dropLast(1)+DrainSample(1800000,90,true),1800000,true,false))
    }
}
