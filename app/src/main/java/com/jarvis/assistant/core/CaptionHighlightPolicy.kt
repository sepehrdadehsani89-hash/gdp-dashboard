package com.jarvis.assistant.core

/** A deterministic resolver supplies the label. Guesses and provider text never set it. */
object CaptionHighlightPolicy {
    fun ranges(text:String,confirmedLabel:String?):List<IntRange>{
        val label=confirmedLabel?.trim()?.takeIf{it.length in 1..80 && it.none(Char::isISOControl)} ?: return emptyList()
        return Regex("(?<![\\p{L}\\p{N}])${Regex.escape(label)}(?![\\p{L}\\p{N}])",RegexOption.IGNORE_CASE)
            .findAll(text.take(8000)).map{it.range}.take(20).toList()
    }
}
