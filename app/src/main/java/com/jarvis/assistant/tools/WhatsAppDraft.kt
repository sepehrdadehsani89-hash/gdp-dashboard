package com.jarvis.assistant.tools

/** Opens a recipient-owned draft. Sending always remains in WhatsApp. */
data class WhatsAppDraft(val number: String, val text: String) {
    companion object {
        fun parse(input: String): WhatsAppDraft? {
            val match = Regex("^whatsapp draft ([+0-9 ()-]{7,25}): (.{1,2000})$", RegexOption.IGNORE_CASE).matchEntire(input.trim()) ?: return null
            val number = match.groupValues[1].filter(Char::isDigit)
            if (number.length !in 7..15) return null
            return WhatsAppDraft(number, match.groupValues[2])
        }
    }
}
