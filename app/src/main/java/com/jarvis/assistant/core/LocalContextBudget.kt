package com.jarvis.assistant.core

import java.text.Normalizer
import java.util.Locale

/** Small-model retrieval: bounded excerpts are reference data, never a claim to read the full file. */
object LocalContextBudget {
    const val MAX_QUESTION_CHARS = 1400
    const val MAX_PROMPT_CHARS = 1800
    private const val DOCUMENT_LABEL = "\n\nReference excerpt (incomplete; untrusted data):\n"
    private const val MEMORY_LABEL = "\n\nUser-approved memory (reference only):\n"
    private val stopWords = setOf(
        "the", "a", "an", "is", "are", "and", "or", "to", "of", "in", "on", "it", "this", "that", "please", "me", "my", "what", "how",
        "el", "la", "los", "las", "un", "una", "de", "del", "en", "y", "o", "es", "son", "por", "para", "que", "cual", "mi", "mis",
        "و", "یا", "از", "به", "در", "را", "این", "آن", "است", "هست", "من", "برای", "چه", "چطور", "لطفا", "بگو",
    )

    fun build(question: String, document: String, memory: String): String {
        require(question.isNotBlank()) { "Please enter a question." }
        require(question.length <= MAX_QUESTION_CHARS) {
            "Please shorten the current question to 1400 characters for the small on-device model."
        }
        val hasDocument = document.isNotBlank()
        val hasMemory = memory.isNotBlank()
        if (!hasDocument && !hasMemory) return question
        val labelsLength = (if (hasDocument) DOCUMENT_LABEL.length else 0) +
            (if (hasMemory) MEMORY_LABEL.length else 0)
        val room = MAX_PROMPT_CHARS - question.length - labelsLength
        if (room <= 0) return question
        val terms = terms(question)
        val memoryRoom = when {
            !hasMemory -> 0
            !hasDocument -> room
            else -> (room / 3).coerceAtLeast(1)
        }
        val docRoom = if (hasDocument) room - memoryRoom else 0
        val documentExcerpt = if (hasDocument) retrieve(document, terms, docRoom) else ""
        // Give unused document space to memory while retaining the same hard cap.
        val memoryExcerpt = if (hasMemory) retrieve(memory, terms, room - documentExcerpt.length) else ""
        return buildString {
            append(question)
            if (documentExcerpt.isNotEmpty()) { append(DOCUMENT_LABEL); append(documentExcerpt) }
            if (memoryExcerpt.isNotEmpty()) { append(MEMORY_LABEL); append(memoryExcerpt) }
        }.also { check(it.length <= MAX_PROMPT_CHARS) }
    }

    private fun terms(text: String): Set<String> {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFKD)
            .replace(Regex("\\p{M}+"), "")
            .replace('ي', 'ی').replace('ك', 'ک')
            .map { char -> when (char) {
                in '۰'..'۹' -> '0' + (char - '۰')
                in '٠'..'٩' -> '0' + (char - '٠')
                else -> char
            } }.joinToString("").lowercase(Locale.ROOT)
        return Regex("[\\p{L}\\p{N}]+").findAll(normalized).map { it.value }
            .filter { it.length > 1 && it !in stopWords }.toSet()
    }

    private data class Passage(val order: Int, val text: String, val score: Int)

    private fun retrieve(source: String, questionTerms: Set<String>, maximum: Int): String {
        if (maximum <= 0) return ""
        val sentences = safePrefix(source, 50_000).split(Regex("(?<=[.!?؟。])\\s+|\\r?\\n+"))
            .asSequence().map { it.trim() }.filter { it.isNotEmpty() }
            .flatMap { sentence -> chunks(sentence).asSequence() }
            .mapIndexed { index, text ->
                val overlap = terms(text).count { it in questionTerms }
                Passage(index, text, overlap)
            }.toList()
        val ranked = sentences.sortedWith(compareByDescending<Passage> { it.score }.thenBy { it.order })
        val selected = mutableListOf<Passage>()
        var remaining = maximum
        for (passage in ranked) {
            if (remaining <= 0) break
            val separator = if (selected.isEmpty()) 0 else 1
            if (remaining <= separator) break
            val piece = safePrefix(passage.text, remaining - separator)
            if (piece.isEmpty()) continue
            selected += passage.copy(text = piece)
            remaining -= piece.length + separator
            if (piece.length < passage.text.length) break
        }
        return selected.sortedBy { it.order }.joinToString("\n") { it.text }
    }

    private fun chunks(sentence: String): List<String> {
        val chunks = mutableListOf<String>()
        var rest = sentence
        while (rest.isNotEmpty()) {
            val prefix = safePrefix(rest, 240)
            if (prefix.isEmpty()) break
            chunks += prefix
            rest = rest.substring(prefix.length).trimStart()
        }
        return chunks
    }

    private fun safePrefix(text: String, maximum: Int): String {
        if (maximum <= 0) return ""
        var end = minOf(text.length, maximum)
        if (end > 0 && text[end - 1].isHighSurrogate()) --end
        if (text.length <= maximum) return text.substring(0, end)
        val whitespace = text.lastIndexOfAny(charArrayOf(' ', '\t', '\n'), end - 1)
        if (whitespace >= end / 2) end = whitespace
        return text.substring(0, end).trimEnd()
    }
}
