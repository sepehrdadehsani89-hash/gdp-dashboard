package com.jarvis.assistant.tools

import org.junit.Assert.*
import org.junit.Test

class LiveControlPolicyTest {
    @Test fun explicitCommandsOnly() {
        assertEquals(LiveRequest.Tap("Search"), LiveControlPolicy.parse("tap Search"))
        assertEquals(LiveRequest.Edit("Hello, Sofia!"), LiveControlPolicy.parse("type Hello, Sofia!"))
        assertNull(LiveControlPolicy.parse("How do I tap Search?"))
        assertNull(LiveControlPolicy.parse("I suggest you tap Search"))
        assertNull(LiveControlPolicy.parse("type " + "x".repeat(2001)))
    }
    @Test fun commitsAndDestructionStayManual() {
        listOf("Send", "SEND message", "Delete", "Confirm purchase", "Pagar", "Enviar", "ارسال", "Allow").forEach { assertFalse(it, LiveControlPolicy.allowedLabel(it)) }
        assertTrue(LiveControlPolicy.allowedLabel("Search"))
        assertTrue(LiveControlPolicy.allowedLabel("New chat"))
    }
    @Test fun whatsappKeepsOriginalDraftAndValidatesPhone() {
        assertEquals(WhatsAppDraft("593991234567", "Hello Sofia!"), WhatsAppDraft.parse("whatsapp draft +593 99 123 4567: Hello Sofia!"))
        assertNull(WhatsAppDraft.parse("send whatsapp to Sofia"))
        assertNull(WhatsAppDraft.parse("whatsapp draft 123: hello"))
    }
}
