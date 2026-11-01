package com.jarvis.assistant.voice

import com.jarvis.assistant.core.AppLanguage
import java.text.Normalizer
import java.util.Locale

/** Text matching after local Vosk recognition, not a trained multilingual wake classifier. */
object WakePhraseMatcher {
    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ')
        .replace(Regex("[\\p{P}\\p{S}]+"), " ")
        .replace(Regex("\\s+"), " ").trim()

    fun extractCommand(text: String, language: AppLanguage): String? {
        val value = normalize(text)
        val prefixes = when (language) {
            AppLanguage.EN -> listOf("hey jarvis", "hello jarvis", "hey jervis", "hey jarves")
            AppLanguage.ES -> listOf("hola jarvis", "oye jarvis", "hey jarvis", "hola yarvis")
            AppLanguage.FA -> listOf("سلام جارویس", "هی جارویس", "سلام جرویس", "هی جرویس")
        }
        val prefix = prefixes.firstOrNull { value == it || value.startsWith("$it ") } ?: return null
        return value.removePrefix(prefix).trim()
    }

    fun isStopCommand(text: String): Boolean = normalize(text) in setOf(
        "stop", "stop everything", "cancel", "para", "para todo", "detente", "cancelar",
        "بس کن", "لغو کن", "همه چیز را متوقف کن",
    )
}
