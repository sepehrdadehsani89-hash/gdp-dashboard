package com.jarvis.assistant.core

import org.junit.Assert.*
import org.junit.Test

class ConversationContextTest {
    @Test fun conversationRetainsOnlyBoundedCompleteTurnsAndCanBeCleared() {
        val context = ConversationContext()
        repeat(20) { context.rememberTurn("Question $it", "Answer $it") }
        assertEquals(8, context.snapshot().size)
        assertEquals(MessageRole.USER, context.snapshot().first().role)
        assertEquals("Answer 19", context.snapshot().last().text)
        context.clear()
        assertTrue(context.snapshot().isEmpty())
    }
    @Test fun oversizedTurnsRespectBudgetWithoutLeavingOrphanModelMessage() {
        val context = ConversationContext()
        repeat(10) { context.rememberTurn("q".repeat(10_000), "a".repeat(10_000)) }
        assertTrue(context.snapshot().sumOf { it.text.length } <= 12_000)
        assertEquals(0, context.snapshot().size % 2)
        assertEquals(MessageRole.USER, context.snapshot().first().role)
    }
    @Test fun screenshotExpiresAndItsBytesAreZeroedOnClearOrReplacement() {
        val session = ScreenSession()
        val first = byteArrayOf(1, 2, 3)
        session.set(CapturedScreen(first, "visible", "test.app", 100, true))
        assertNotNull(session.current(1000))
        assertNull(session.current(120_101))
        assertArrayEquals(byteArrayOf(0, 0, 0), first)
        val second = byteArrayOf(4)
        session.set(CapturedScreen(second, "visible", "test.app", 200_000, true))
        session.clear()
        assertEquals(0, second[0].toInt())
    }
    @Test fun captureAndFollowUpPhrasesWorkInThreeLanguagesWithoutTreatingHelpAsCapture() {
        listOf("analyze my screen", "scan my screen", "analiza mi pantalla", "escanea mi pantalla", "صفحه را تحلیل کن", "صفحه گوشی را اسکن کن").forEach { assertTrue(it, ScreenContextPolicy.isCaptureRequest(it)) }
        assertFalse(ScreenContextPolicy.isCaptureRequest("how can I scan my screen?"))
        listOf("summarize this", "give me small information about this", "resume esto", "این را خلاصه کن").forEach { assertTrue(ScreenContextPolicy.isFollowUp(it)) }
        assertFalse(ScreenContextPolicy.isFollowUp("Tell me about volcanoes"))
    }
    @Test fun publicLockedChatNeverAuthorizesPrivateDataOrPhoneActions() {
        listOf("read my notifications", "open YouTube", "what do you remember", "mis contactos", "صفحه را بخوان", "تماس با دوستم").forEach { assertTrue(it, LockedChatPolicy.requiresUnlock(it)) }
        assertFalse(LockedChatPolicy.requiresUnlock("Explain photosynthesis"))
    }
    @Test fun speechCanStartBeforeTheFullAnswerArrivesAndFlushesRemainderExactly() {
        val speech = SpeechChunks()
        assertTrue(speech.add("Here is the beginning of").isEmpty())
        val ready = speech.add(" a useful answer. The rest")
        assertEquals(listOf("Here is the beginning of a useful answer."), ready)
        assertEquals("The rest", speech.finish())
        assertEquals("", speech.finish())
        val long = SpeechChunks()
        val chunks = long.add("word ".repeat(80)) + long.finish()
        assertTrue(chunks.all { it.length <= 180 })
        assertEquals("word ".repeat(80).trim(), chunks.joinToString(" "))
    }
}
