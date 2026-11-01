package com.jarvis.assistant.tools

import com.jarvis.assistant.core.AppLanguage
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WeatherSummaryTest {
    private fun sample(code: Int = 3) = JSONObject("""{"current":{"temperature_2m":15.2,"apparent_temperature":13.4,"wind_speed_10m":6.7,"weather_code":$code,"time":"2026-10-31T12:00"}}""")

    @Test fun weatherUsesObservedFieldsAndAttributesTheSource() {
        val reply = WeatherSummary.describe("Quito, Ecuador", sample(), AppLanguage.EN)
        assertTrue(reply.contains("15 degrees Celsius"))
        assertTrue(reply.contains("feels like 13"))
        assertTrue(reply.contains("Wind 7 kilometers"))
        assertTrue(reply.contains("2026-10-31 12:00"))
        assertTrue(reply.contains("Open-Meteo"))
    }

    @Test fun spanishAndPersianConditionsAreLocalized() {
        assertTrue(WeatherSummary.describe("Madrid", sample(61), AppLanguage.ES).contains("lluvia"))
        assertTrue(WeatherSummary.describe("تهران", sample(0), AppLanguage.FA).contains("صاف"))
        assertTrue(WeatherSummary.describe("Quito", sample(999), AppLanguage.EN).contains("conditions unavailable"))
    }

    @Test(expected = org.json.JSONException::class) fun missingMeasurementsAreNotInvented() {
        WeatherSummary.describe("Quito", JSONObject("""{"current":{"weather_code":0}}"""), AppLanguage.EN)
    }
}
