package com.jarvis.assistant.neural

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WeatherFollowUpTest {
    @Test fun weekdayRequestsUseForecastReferenceDateAcrossLanguages(){
        val monday=LocalDate.of(2026,11,2)
        assertEquals(4,ForecastPolicy.day("weather on Friday",monday))
        assertEquals(4,ForecastPolicy.day("clima el viernes",monday))
        assertEquals(4,ForecastPolicy.day("هوای روز جمعه",monday))
        assertEquals(7,ForecastPolicy.day("next Monday",monday))
        assertEquals(0,ForecastPolicy.day("Monday",monday))
        assertEquals(2,ForecastPolicy.day("weather the day after tomorrow",monday))
    }
    @Test fun weekdayCityNamesStayCityNames(){
        assertEquals("Friday Harbor",ForecastPolicy.city("weather in Friday Harbor"))
        assertEquals("Quito",ForecastPolicy.city("weather in Quito on Friday"))
        assertNull(ForecastPolicy.weekday("weather in Friday Harbor"))
    }
    @Test fun contextualRainAndColdFollowupsRetainDayAndExpire(){
        val conversation=ConversationState()
        assertNull(conversation.weatherFollowUp("will it rain",1000))
        conversation.weather("Quito",2,1000)
        assertEquals(2,conversation.weatherFollowUp("will it rain?",2000))
        assertEquals(2,conversation.weatherFollowUp("how cold will it be",2000))
        assertEquals(2,conversation.weatherFollowUp("چتر ببرم؟",2000))
        assertNull(conversation.weatherFollowUp("will it rain",601001))
        assertNull(conversation.weatherFollowUp("open Friday Harbor maps",2000))
        conversation.clear();assertNull(conversation.weatherFollowUp("how cold",3000))
    }
}
