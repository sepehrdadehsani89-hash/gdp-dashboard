package com.jarvis.assistant.core

import java.util.Locale

sealed class PhoneRequest {
    data class Contacts(val query: String = "") : PhoneRequest()
    data class Calendar(val days: Int = 7) : PhoneRequest()
    data object Location : PhoneRequest()
}

/** Recognizes only explicit, read-only phone requests. It never infers permission from model text. */
object PhoneRequestParser {
    fun parse(input: String): PhoneRequest? {
        val text = input.trim()
            .replace('ي', 'ی').replace('ك', 'ک')
            .replace('\u200c', ' ')
            .replace(Regex("[\\p{Cc}\\p{Cf}]"), "")
            .replace(Regex("\\s+"), " ")
            .trimStart('¿', '¡')
            .trimEnd('.', '!', '?', '؟')
            .replace(Regex("^(?:hey jarvis|hola jarvis|سلام جارویس)[,، ]+", RegexOption.IGNORE_CASE), "")
            .trim()
        if (text.isBlank() || text.length > 240) return null
        val lower = text.lowercase(Locale.ROOT)
        // Compound requests with sharing/mutations need a separately authorized route.
        if (Regex("(?:^|[\\s,،])(?:send|share|email|text|delete|remove|add|create|edit|enviar|envía|envia|comparte|compartir|borrar|eliminar|crea|crear|añade|agrega|ارسال|بفرست|بفرستم|حذف|پاک|اضافه|بساز|ایجاد)(?:$|[\\s,،])").containsMatchIn(lower)) return null

        val contactLists = listOf(
            "^(?:(?:show|list|read)(?: me)?|what are) (?:my )?contacts$",
            "^(?:my )?contacts$",
            "^(?:muestra|muéstrame|muestrame|lista|lee)(?: mis| los)? contactos$",
            "^(?:mis )?contactos$",
            "^(?:مخاطبین|مخاطبان|مخاطب های)(?: من| گوشی)?(?: را)?(?: نشان بده| بخوان| بگو)?$",
        )
        if (contactLists.any { Regex(it, RegexOption.IGNORE_CASE).matches(text) }) return PhoneRequest.Contacts()

        val contactQueries = listOf(
            "^(?:find|look up|show|search)(?: me)?(?: the)? (?:contact|phone number)(?: for| of)? (.+)$",
            "^(?:what(?:'s| is)|tell me)(?: the)? phone number (?:for|of) (.+)$",
            "^(?:contact|phone number)(?: for| of)? (.+)$",
            "^(?:(?:busca|buscar|muestra|muéstrame|muestrame|dame) (?:el )?)?(?:contacto|número de teléfono|numero de telefono)(?: de| para)? (.+)$",
            "^(?:شماره(?: تلفن| موبایل)?|تلفن) (.+?) (?:چیه|چیست)$",
            "^(?:شماره(?: تلفن| موبایل)?|تلفن|مخاطب) (.+?)(?: را (?:پیدا کن|نشان بده|بگو|بخوان))?$",
        )
        for (pattern in contactQueries) {
            val match = Regex(pattern, RegexOption.IGNORE_CASE).matchEntire(text) ?: continue
            val query = match.groupValues[1]
                .replace(Regex("(?:,? please|,? por favor| لطفا| لطفاً)$", RegexOption.IGNORE_CASE), "")
                .trim()
            if (query.isNotBlank() && query.length <= 80 && !query.contains(Regex("[;\\n\\r]"))) return PhoneRequest.Contacts(query)
            return null
        }

        val calendar = listOf(
            "^(?:(?:show|read)(?: me)?(?: my| the)? |what(?:'s| is) (?:on )?my )?(?:calendar|schedule|upcoming events)(?: (?:for |this |next )?(?:today|week|seven days|7 days))?$",
            "^(?:what(?:'s| is) on my calendar|show my events|my calendar)$",
            "^(?:(?:muestra|muéstrame|muestrame|lee|dime)(?: mi| el)? )?(?:calendario|agenda|próximos eventos|proximos eventos)(?: (?:de |esta |la próxima |la proxima )?(?:hoy|semana|siete días|7 días))?$",
            "^(?:تقویم|برنامه|رویدادهای)(?: من| این هفته| هفته آینده| امروز| هفت روز آینده)?(?: را)?(?: نشان بده| بخوان| بگو)?$",
            "^(?:برنامه ام|تقویم من)(?: برای)? (?:امروز|این هفته|هفته آینده)(?: چیست| چیه)?$",
        )
        if (calendar.any { Regex(it, RegexOption.IGNORE_CASE).matches(text) }) {
            val today = listOf("today", "hoy", "امروز").any(lower::contains)
            return PhoneRequest.Calendar(if (today) 1 else 7)
        }

        val location = setOf(
            "where am i", "my location", "show my location", "tell me my location", "what is my location", "what's my location",
            "dónde estoy", "donde estoy", "mi ubicación", "mi ubicacion", "muestra mi ubicación", "muéstrame mi ubicación", "cuál es mi ubicación", "cual es mi ubicacion",
            "کجا هستم", "من کجا هستم", "موقعیت من", "مکان من", "موقعیت من را بگو", "موقعیت من را نشان بده", "مکان من را نشان بده",
        )
        if (lower in location) return PhoneRequest.Location
        return null
    }
}
