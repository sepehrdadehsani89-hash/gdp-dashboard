package com.jarvis.assistant.core

import java.util.UUID

enum class AppLanguage(val code: String, val tag: String, val label: String, val wakePhrase: String) {
    EN("en", "en-US", "English", "Hey Jarvis"),
    ES("es", "es-ES", "Español", "Oye Jarvis"),
    FA("fa", "fa-IR", "فارسی", "هی جارویس");
    companion object { val supported=listOf(EN,ES); fun fromCode(code: String) = supported.firstOrNull { it.code == code } ?: EN }
}

enum class AssistantPhase { READY, LISTENING, THINKING, SPEAKING, PAUSED, ERROR }
enum class MessageRole { USER, ASSISTANT, STATUS }
data class SourceLink(val title: String, val url: String)
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sources: List<SourceLink> = emptyList(),
)
data class MemoryItem(val id: Long = 0, val text: String, val createdAt: Long = System.currentTimeMillis())
data class AssistantSettings(
    val name: String = "",
    val language: AppLanguage = AppLanguage.EN,
    val provider: String = "local",
    val model: String = "qwen2.5-0.5b",
    val speakReplies: Boolean = true,
    val allowNetworkSpeech: Boolean = false,
    val useCloudVoice: Boolean = false,
    val shareSavedMemories: Boolean = false,
    val allowLockedChat: Boolean = false,
    val onboardingComplete: Boolean = false,
)
data class PendingAction(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val kind: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
)
data class AssistantReply(
    val text: String,
    val sources: List<SourceLink> = emptyList(),
    val action: PendingAction? = null,
    // Default deny: private tool/file content must not leak via an optional speech provider.
    val allowCloudSpeech: Boolean = false,
    val speechHandled: Boolean = false,
    val confirmedEntity:String?=null,
)

data class ConnectionCheck(val model: String, val sample: String, val elapsedMs: Long)

fun AppLanguage.choose(en: String, es: String, fa: String): String = when (this) {
    AppLanguage.EN -> en
    AppLanguage.ES -> es
    AppLanguage.FA -> fa
}
