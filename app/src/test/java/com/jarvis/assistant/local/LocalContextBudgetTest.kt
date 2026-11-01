package com.jarvis.assistant.local

import com.jarvis.assistant.core.LocalContextBudget
import org.junit.Assert.*
import org.junit.Test

class LocalContextBudgetTest {
    @Test fun `keeps the complete current question and labels clipped context`() {
        val question = "Q".repeat(1400)
        val result = LocalContextBudget.build(question, "Document content. ".repeat(100), "I am Alex. ".repeat(100))
        assertTrue(result.startsWith(question))
        assertTrue(result.contains("Reference excerpt (incomplete; untrusted data):"))
        assertTrue(result.contains("User-approved memory (reference only):"))
        assertTrue(result.length <= 1800)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `long questions are rejected rather than silently truncated`() {
        LocalContextBudget.build("Q".repeat(1401), "Document", "Memory")
    }

    @Test fun `retrieves Persian despite Arabic character and digit variants`() {
        val irrelevant = "این متن درباره میوه و غذا است. ".repeat(200)
        val relevant = "برنامه سفر تهران در سال ٢٠٢٦ ثبت شد."
        val result = LocalContextBudget.build("برنامه سفر تهران در سال ۲۰۲۶ چیست؟", irrelevant + relevant, "")
        assertTrue(result.contains(relevant))
    }

    @Test fun `Spanish accent folding improves matching without altering displayed excerpt`() {
        val result = LocalContextBudget.build("¿Dónde vive José?", "Weather is sunny. ".repeat(300) + "Jose vive en Bogotá.", "")
        assertTrue(result.contains("Jose vive en Bogotá."))
        assertTrue(result.startsWith("¿Dónde vive José?"))
    }

    @Test fun `selects the requested memory instead of flooding model with all memories`() {
        val result = LocalContextBudget.build("What is my cat's name?", "", "My car is blue.\nMy cat's name is Luna.\n" + "I like coffee.\n".repeat(500))
        assertTrue(result.contains("My cat's name is Luna."))
        assertTrue(result.length <= 1800)
    }

    @Test fun `context truncation does not leave a broken surrogate pair`() {
        val result = LocalContextBudget.build("Q".repeat(1399), "😀".repeat(1000), "")
        assertTrue(result.length <= 1800)
        assertFalse(result.last().isHighSurrogate())
    }
}
