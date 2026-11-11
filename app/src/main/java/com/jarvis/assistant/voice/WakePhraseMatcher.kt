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

    /** Short whole-prefix phrase, never an action/stop override. No audio is saved. */
    fun validCustomPhrase(phrase:String):Boolean {
        val n=normalize(phrase)
        val words=n.split(" ")
        return n.length in 8..60 && words.size in 2..6 && words.all{it.length in 2..24 && it.all(Char::isLetter)} &&
            words.none{it in setOf("stop","cancel","delete","erase","send","unlock","lock","call","open","pay","buy","para","cancela","borra","envia","llama","abre","paga")}
    }
    fun extractCommand(text: String, language: AppLanguage,customPhrase:String?=null): String? {
        if(language !in AppLanguage.supported)return null
        val value = normalize(text)
        val prefixes = when (language) {
            AppLanguage.EN -> listOf("jarvis are you up", "jarvis are you there", "hey jarvis", "hello jarvis", "hey jervis", "hey jarves")
            AppLanguage.ES -> listOf("jarvis estas ahi", "jarvis estás ahí", "jarvis estas despierto", "jarvis estás despierto", "hola jarvis", "oye jarvis", "hey jarvis", "hola yarvis")
            AppLanguage.FA -> listOf("سلام جارویس", "هی جارویس", "سلام جرویس", "هی جرویس")
        }
        val configured=customPhrase?.takeIf(::validCustomPhrase)?.let(::normalize)
        val prefix = (prefixes+listOfNotNull(configured)).sortedByDescending{it.length}.firstOrNull { value == it || value.startsWith("$it ") } ?: return null
        return value.removePrefix(prefix).trim()
    }

    fun isStopCommand(text: String): Boolean = normalize(text) in setOf(
        "stop", "stop everything", "cancel", "para", "para todo", "detente", "cancelar",
        "بس کن", "لغو کن", "همه چیز را متوقف کن",
    )
}
