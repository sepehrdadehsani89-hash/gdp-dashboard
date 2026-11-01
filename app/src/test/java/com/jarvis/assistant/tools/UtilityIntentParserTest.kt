package com.jarvis.assistant.tools

import org.junit.Assert.*
import org.junit.Test

class UtilityIntentParserTest {
    @Test fun timersUnderstandThreeLanguagesAndUnicodeDigits() {
        assertEquals(300, UtilityIntentParser.timerSeconds("Set a timer for five minutes"))
        assertEquals(120, UtilityIntentParser.timerSeconds("temporizador de dos minutos"))
        assertEquals(300, UtilityIntentParser.timerSeconds("تایمر ۵ دقیقه"))
        assertEquals(30, UtilityIntentParser.timerSeconds("تایمر سی ثانیه"))
        assertNull(UtilityIntentParser.timerSeconds("timer 99999 hours"))
        assertNull(UtilityIntentParser.timerSeconds("timer 0 seconds"))
        assertNull(UtilityIntentParser.timerSeconds("timer 5"))
        assertNull(UtilityIntentParser.timerSeconds("timer twenty five minutes"))
        assertNull(UtilityIntentParser.timerSeconds("timer 1 hour and 5 minutes"))
    }

    @Test fun alarmHoursAreBoundedAndAmPmIsExplicit() {
        assertEquals(7 to 30, UtilityIntentParser.alarmTime("alarm at 07:30"))
        assertEquals(19 to 30, UtilityIntentParser.alarmTime("alarma 7:30 pm"))
        assertEquals(0 to 15, UtilityIntentParser.alarmTime("آلارم ۱۲:۱۵ صبح"))
        assertNull(UtilityIntentParser.alarmTime("alarm 99:30"))
        assertNull(UtilityIntentParser.alarmTime("alarm 07:99"))
    }

    @Test fun weatherOnlySendsAnExplicitCityName() {
        assertEquals("quito", UtilityIntentParser.weatherCity("What's the weather in Quito?"))
        assertEquals("madrid", UtilityIntentParser.weatherCity("¿Cómo está el clima en Madrid?"))
        assertEquals("تهران", UtilityIntentParser.weatherCity("هوای تهران چطوره؟"))
        assertNull(UtilityIntentParser.weatherCity("What's the weather?"))
        assertNull(UtilityIntentParser.weatherCity("weather in https://evil.example/private"))
        assertNull(UtilityIntentParser.weatherCity("weather in 127.0.0.1"))
        assertFalse(UtilityIntentParser.isWeather("Tell me about هواپیما"))
    }

    @Test fun mapQueriesRemainNavigationAndDoNotGuessDestinations() {
        assertEquals("museo del prado", UtilityIntentParser.mapQuery("Cómo llegar a Museo del Prado"))
        assertEquals("تجریش", UtilityIntentParser.mapQuery("مسیریابی به تجریش"))
        assertNull(UtilityIntentParser.mapQuery("where am I?"))
    }

    @Test fun helpQuestionsDoNotTriggerPrivateScreenOrNotificationReads() {
        assertTrue(LocalIntentParser.isScreen("What's on my screen?"))
        assertTrue(LocalIntentParser.isScreen("Lee mi pantalla"))
        assertTrue(LocalIntentParser.isNotifications("اعلان ها را بخوان"))
        assertFalse(LocalIntentParser.isScreen("How do I disable screen read?"))
        assertFalse(LocalIntentParser.isNotifications("How to turn on notifications?"))
    }
}
