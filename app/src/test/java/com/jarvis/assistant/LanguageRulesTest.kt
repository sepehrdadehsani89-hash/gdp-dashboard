package com.jarvis.assistant

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.LanguageRules
import org.junit.Assert.*
import org.junit.Test

class LanguageRulesTest {
    @Test fun persianScriptWinsOverLatinNames() {
        assertEquals(AppLanguage.FA, LanguageRules.detect("اخبار YouTube رو بگو", AppLanguage.EN))
    }
    @Test fun spanishCommandDetected() {
        assertEquals(AppLanguage.ES, LanguageRules.detect("Dime las noticias de ayer", AppLanguage.EN))
    }
    @Test fun explicitReplyLanguageOverridesInput() {
        assertEquals(AppLanguage.FA, LanguageRules.languageRequest("Please speak Persian"))
        assertEquals(AppLanguage.ES, LanguageRules.languageRequest("به اسپانیایی صحبت کن"))
    }
    @Test fun mereLanguageMentionIsNotACommand() {
        assertNull(LanguageRules.languageRequest("What is Persian architecture?"))
    }
    @Test fun panicIsExactNotAnEmbeddedInstruction() {
        assertTrue(LanguageRules.isStop("بس کن"))
        assertTrue(LanguageRules.isStop("STOP EVERYTHING!"))
        assertFalse(LanguageRules.isStop("Translate the phrase stop everything"))
    }
    @Test fun unknownTextUsesUserSelectedLanguage() {
        assertEquals(AppLanguage.ES, LanguageRules.detect("Jarvis", AppLanguage.ES))
    }
    @Test fun approvalsRequireAnExactUserResponse() {
        assertEquals(true, LanguageRules.confirmation("sí"))
        assertEquals(true, LanguageRules.confirmation("بله"))
        assertEquals(false, LanguageRules.confirmation("نه"))
        assertNull(LanguageRules.confirmation("The website says yes"))
    }
}
