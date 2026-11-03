package com.jarvis.assistant.omnisystem
import com.jarvis.assistant.hud.WeatherRepository
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
class WeatherPanelTest {
    @Test fun parsesOnlyActualForecastFieldsAndBoundsPanels() {
        val j=JSONObject("""{"current":{"time":"2026-11-02T10:30","temperature_2m":18},"hourly":{"time":["2026-11-02T09:00","2026-11-02T11:00","invalid"],"temperature_2m":[17,19,20],"precipitation_probability":[0,45,100]},"daily":{"time":["2026-11-02","invalid","2026-11-03"],"temperature_2m_max":[21,22,null],"temperature_2m_min":[10,11,12],"precipitation_probability_max":[30,40,900]}}""")
        val w=WeatherRepository.parse(j,"Quito",123)
        assertEquals(1,w.hourly.size);assertEquals(19.0,w.hourly.single().temperatureC!!,0.0)
        assertEquals(45,w.hourly.single().rain);assertEquals(2,w.daily.size)
        assertNull(w.daily.last().high);assertNull(w.daily.last().rain)
        assertEquals(123L,w.fetchedAt)
    }
    @Test fun staleSourceTimestampCannotBeMadeFreshByRetrievalTime() {
        val now=java.time.Instant.parse("2026-11-02T15:00:00Z").toEpochMilli()
        val old=WeatherRepository.parse(JSONObject("""{"timezone":"America/Guayaquil","current":{"time":"2026-11-01T10:00"}}"""),"Quito",now)
        assertTrue(WeatherRepository.stale(old,now))
        val fresh=WeatherRepository.parse(JSONObject("""{"timezone":"America/Guayaquil","current":{"time":"2026-11-02T10:00"}}"""),"Quito",now)
        assertFalse(WeatherRepository.stale(fresh,now))
    }
    @Test fun missingForecastRemainsUnavailable() {
        val w=WeatherRepository.parse(JSONObject("""{"current":{"time":"2026-11-02T10:00"}}"""),"Quito",123)
        assertTrue(w.hourly.isEmpty());assertTrue(w.daily.isEmpty());assertNull(w.temperatureC)
        assertTrue(WeatherRepository.stale(w,123+WeatherRepository.MAX_AGE))
        assertTrue(WeatherRepository.stale(w,122))
    }
}
