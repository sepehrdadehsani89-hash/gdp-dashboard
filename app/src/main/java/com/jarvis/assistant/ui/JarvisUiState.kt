package com.jarvis.assistant.ui

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantPhase
import com.jarvis.assistant.core.AssistantSettings
import com.jarvis.assistant.core.ChatMessage
import com.jarvis.assistant.core.MemoryItem
import com.jarvis.assistant.core.PendingAction

data class JarvisUiState(
    val settings: AssistantSettings = AssistantSettings(),
    val messages: List<ChatMessage> = emptyList(),
    val memories: List<MemoryItem> = emptyList(),
    val phase: AssistantPhase = AssistantPhase.READY,
    val transcript: String = "",
    val error: String? = null,
    val wakeStatus: String = "",
    val modelProgress: Float? = null,
    val hasApiKey: Boolean = false,
    val hasMicrophone: Boolean = false,
    val pendingAction: PendingAction? = null,
    val attachedFiles: List<String> = emptyList(),
    val brainStatus: String = "",
    val brainProgress: Float? = null,
    val voiceStatus: String = "",
    val voiceProgress: Float? = null,
)

data class JarvisUiActions(
    val onSend: (String) -> Unit = {},
    val onMic: () -> Unit = {},
    val onStop: () -> Unit = {},
    val onLanguage: (AppLanguage) -> Unit = {},
    val onSaveSettings: (AssistantSettings, String) -> Unit = { _, _ -> },
    val onListGeminiModels: suspend (String) -> List<String> = { emptyList() },
    val onTestProvider: suspend (AssistantSettings, String) -> String = { _, _ -> "" },
    val onRememberDelete: (Long) -> Unit = {},
    val onClearConversation: () -> Unit = {},
    val onWipe: () -> Unit = {},
    val onStartWake: () -> Unit = {},
    val onStopWake: () -> Unit = {},
    val onDownloadWake: () -> Unit = {},
    val onDownloadBrain: () -> Unit = {},
    val onVoiceCheck: () -> Unit = {},
    val onDownloadVoice: () -> Unit = {},
    val onPickFiles: () -> Unit = {},
    val onPickPhotos: () -> Unit = {},
    val onClearAttachments: () -> Unit = {},
    val onOpenPermission: (String) -> Unit = {},
    val onConfirmAction: (String) -> Unit = {},
    val onCancelAction: () -> Unit = {},
    val onOpenSource: (String) -> Unit = {},
)
