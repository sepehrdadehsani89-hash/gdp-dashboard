package com.jarvis.assistant.core

import com.jarvis.assistant.tools.ToolPolicy

/** Only general AI turns enter this buffer; phone tools and screen answers never do. */
class ConversationContext {
    private val turns = ArrayDeque<ChatMessage>()
    fun snapshot(): List<ChatMessage> = turns.toList()
    fun rememberTurn(question: String, answer: String) {
        turns.addLast(ChatMessage(role = MessageRole.USER, text = question.take(2000)))
        turns.addLast(ChatMessage(role = MessageRole.ASSISTANT, text = answer.take(3000)))
        while (turns.size > 8 || turns.sumOf { it.text.length } > 12_000) { turns.removeFirst(); turns.removeFirst() }
    }
    fun expire(before:Long){if(turns.any{it.timestamp<before})clear()}
    fun clear() = turns.clear()
}

data class CapturedScreen(val jpeg: ByteArray, val visibleText: String, val packageName: String, val capturedAt: Long, val cloudConsent: Boolean, val provider: String = "gemini")

class ScreenSession {
    private var image: CapturedScreen? = null
    fun set(value: CapturedScreen) { clear(); image = value }
    fun current(now: Long): CapturedScreen? {
        val value = image ?: return null
        if (now < value.capturedAt || now - value.capturedAt > 120_000) { clear(); return null }
        return value
    }
    fun clear() { image?.jpeg?.fill(0); image = null }
}

object LockedChatPolicy {
    fun requiresUnlock(text: String): Boolean = Regex("\\b(screen|screenshot|notification|notifications|contact|contacts|calendar|location|remember|memories|memory|open|launch|send|call|file|photo|pantalla|notificaciones|contactos|calendario|ubicación|recuerda|recuerdos|abre|envía|llama|archivo|foto)\\b|صفحه|اعلان|مخاطب|تقویم|مکان|یادت|حافظه|باز کن|ارسال|تماس|فایل|عکس").containsMatchIn(ToolPolicy.normalize(text))
}

object ScreenContextPolicy {
    fun isCaptureRequest(text: String): Boolean {
        val t = ToolPolicy.normalize(text)
        if (listOf("how to", "how do i", "how can i", "cómo", "como puedo", "چطور", "چگونه").any(t::startsWith)) return false
        val screen = Regex("\\b(screen|screenshot|pantalla|captura)\\b|صفحه|اسکرین").containsMatchIn(t)
        return screen && Regex("\\b(analy[sz]e|scan|capture|screenshot|analiza|analizar|escanea|captura)\\b|تحلیل|اسکن|عکس بگیر").containsMatchIn(t)
    }
    fun isFollowUp(text: String): Boolean = Regex("\\b(this|that|it|screen|summarize|summary|resume|resumen|explain|translate|esto|eso|pantalla|explica|traduce)\\b|این|آن|صفحه|خلاصه|توضیح|ترجمه").containsMatchIn(ToolPolicy.normalize(text))
}

/** Flushes complete short sentences while an AI reply is still arriving. */
class SpeechChunks {
    private val pending = StringBuilder()
    fun add(delta: String): List<String> {
        pending.append(delta)
        val ready = mutableListOf<String>()
        while (pending.isNotEmpty()) {
            val sentence = pending.indices.firstOrNull { index -> pending[index] == '\n' || (pending[index] in ".!?؟" && index + 1 < pending.length && pending[index + 1].isWhitespace()) } ?: -1
            val cut = when {
                sentence >= 0 && (sentence + 1 >= 24 || pending.length >= 100) -> sentence + 1
                pending.length >= 180 -> pending.lastIndexOf(' ', 180).takeIf { it >= 50 } ?: 180
                else -> break
            }
            pending.substring(0, cut).trim().takeIf { it.isNotEmpty() }?.let(ready::add)
            pending.delete(0, cut)
        }
        return ready
    }
    fun finish(): String = pending.toString().trim().also { pending.clear() }
}
