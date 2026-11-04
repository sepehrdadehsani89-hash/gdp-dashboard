package com.jarvis.assistant.core

import java.util.Locale

object LanguageRules {
    fun confirmation(text: String): Boolean? = when (text.trim().lowercase(Locale.ROOT).trim('.', '!', '?', '؟')) {
        "yes", "confirm", "sí", "si", "confirmar", "بله", "آره", "تأیید", "تایید" -> true
        "no", "cancel", "cancela", "نه", "خیر", "لغو کن" -> false
        else -> null
    }
    fun detect(text: String, fallback: AppLanguage): AppLanguage {
        if (text.any { it in '\u0600'..'\u06ff' }) return if(fallback in AppLanguage.supported)fallback else AppLanguage.EN
        val lowered = text.lowercase(Locale.ROOT)
        if (Regex("\\b(hola|noticias|ayer|español|habla|recuerda|abre|qué|quiero|buenos|gracias|dime|por favor)\\b").containsMatchIn(lowered)) return AppLanguage.ES
        if (Regex("\\b(hey|hello|news|yesterday|remember|open|what|please|tell|english|speak)\\b").containsMatchIn(lowered)) return AppLanguage.EN
        return if(fallback in AppLanguage.supported)fallback else AppLanguage.EN
    }
    fun languageRequest(text: String): AppLanguage? {
        val lowered = text.lowercase(Locale.ROOT)
        val request = listOf("speak", "answer in", "habla", "responde en", "صحبت", "جواب", "حرف بزن").any(lowered::contains)
        if (!request) return null
        return when {
            listOf("persian", "farsi", "فارسی", "persa").any(lowered::contains) -> null
            listOf("spanish", "español", "اسپانیایی").any(lowered::contains) -> AppLanguage.ES
            listOf("english", "inglés", "انگلیسی").any(lowered::contains) -> AppLanguage.EN
            else -> null
        }
    }
    fun isStop(text: String): Boolean = text.trim().lowercase(Locale.ROOT).trim('.', '!', '?', '؟') in setOf(
        "stop", "stop everything", "jarvis stop", "cancel", "para", "detente", "cancela", "بس کن", "توقف", "لغو کن", "همه چیز را متوقف کن",
    )
}
