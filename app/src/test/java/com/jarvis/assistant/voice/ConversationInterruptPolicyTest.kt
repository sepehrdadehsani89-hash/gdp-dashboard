package com.jarvis.assistant.voice
import org.junit.Assert.*
import org.junit.Test
class ConversationInterruptPolicyTest {
    @Test fun explicitInterruptionsBypassFullModelReasoning(){for(text in listOf("Wait, wait!","Hold on","Stop talking","One second","Pause","espera espera","یک لحظه"))assertTrue(text,ConversationInterruptPolicy.matches(text))}
    @Test fun QuotedAndUnrelatedPlaybackRequestsAreNotInterruptions(){for(text in listOf("explain the words wait wait","pause Spotify","wait for my call","the webpage says stop talking"))assertFalse(text,ConversationInterruptPolicy.matches(text))}
}
