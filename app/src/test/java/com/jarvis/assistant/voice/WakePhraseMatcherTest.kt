package com.jarvis.assistant.voice

import com.jarvis.assistant.core.AppLanguage
import org.junit.Assert.*
import org.junit.Test

class WakePhraseMatcherTest {
    @Test fun recognizesLocalizedWakePrefixesAndKeepsCommand() {
        assertEquals("read the news", WakePhraseMatcher.extractCommand("Hey Jarvis, read the news!", AppLanguage.EN))
        assertEquals("las noticias", WakePhraseMatcher.extractCommand("Hola Jarvis, las noticias", AppLanguage.ES))
        assertNull(WakePhraseMatcher.extractCommand("سلام جارويس، خبرها را بخوان", AppLanguage.FA))
    }

    @Test fun wakeOnlyWaitsForNextUtteranceAndUnrelatedSpeechDoesNotActivate() {
        assertEquals("", WakePhraseMatcher.extractCommand("Hey Jarvis", AppLanguage.EN))
        assertNull(WakePhraseMatcher.extractCommand("I said hey Jarvis yesterday", AppLanguage.EN))
        assertNull(WakePhraseMatcher.extractCommand("Hey Jarvison", AppLanguage.EN))
        assertNull(WakePhraseMatcher.extractCommand("Hola Jarvis", AppLanguage.FA))
    }

    @Test fun stopRequiresWholeCommandAndNormalizesPersianVariants() {
        assertTrue(WakePhraseMatcher.isStopCommand("بس كن"))
        assertTrue(WakePhraseMatcher.isStopCommand("PARA TODO!"))
        assertFalse(WakePhraseMatcher.isStopCommand("Explain a bus stop"))
    }
}
