package com.jarvis.assistant.tools

object UtilityIntentParser {
    fun digits(text: String): String = text.map { character ->
        Character.digit(character, 10).takeIf { it >= 0 }?.let { ('0'.code + it).toChar() } ?: character
    }.joinToString("")

    fun isTimer(text: String): Boolean = Regex("\\b(timer|temporizador)\\b|تایمر|زمان سنج").containsMatchIn(ToolPolicy.normalize(text))
    fun timerSeconds(text: String): Int? {
        if (!isTimer(text)) return null
        var normalized = digits(ToolPolicy.normalize(text))
        val words = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "ten" to 10, "fifteen" to 15, "thirty" to 30,
            "un" to 1, "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5, "diez" to 10, "quince" to 15, "treinta" to 30,
            "یک" to 1, "دو" to 2, "سه" to 3, "چهار" to 4, "پنج" to 5, "ده" to 10, "پانزده" to 15, "سی" to 30,
        )
        words.forEach { (word, number) -> normalized = normalized.replace(Regex("(?<![\\p{L}\\p{Nd}])${Regex.escape(word)}(?![\\p{L}\\p{Nd}])"), number.toString()) }
        // Reject compound or unsupported spoken amounts instead of silently
        // turning “twenty five minutes” into a five-minute timer.
        if (Regex("\\b(twenty|forty|fifty|sixty|seventy|eighty|ninety|hundred|veinte|cuarenta|cincuenta|sesenta|cien)\\b|بیست|چهل|پنجاه|شصت|صد").containsMatchIn(normalized)) return null
        val amounts = Regex("(?<!\\d)(\\d{1,5})(?!\\d)").findAll(normalized).toList()
        if (amounts.size != 1) return null
        val amount = amounts.single().groupValues[1].toLongOrNull() ?: return null
        val factor = when {
            Regex("\\b(hours?|horas?)\\b|ساعت").containsMatchIn(normalized) -> 3_600L
            Regex("\\b(minutes?|mins?|minutos?)\\b|دقیقه").containsMatchIn(normalized) -> 60L
            Regex("\\b(seconds?|secs?|segundos?)\\b|ثانیه").containsMatchIn(normalized) -> 1L
            else -> return null
        }
        return (amount * factor).takeIf { it in 1..86_400 }?.toInt()
    }

    fun isAlarm(text: String): Boolean = Regex("\\b(alarm|alarma)\\b|آلارم|زنگ بیدار|ساعت بیدار").containsMatchIn(ToolPolicy.normalize(text))
    fun alarmTime(text: String): Pair<Int, Int>? {
        if (!isAlarm(text)) return null
        val value = digits(ToolPolicy.normalize(text))
        val match = Regex("(?<!\\d)([01]?\\d|2[0-3])[:：]([0-5]\\d)(?!\\d)").find(value) ?: return null
        var hour = match.groupValues[1].toInt()
        if(Regex("\\b(?:am|pm)\\b|صبح|بعد از ظهر|عصر|شب").containsMatchIn(value) && hour !in 1..12)return null
        if (Regex("\\bpm\\b|بعد از ظهر|عصر|شب").containsMatchIn(value) && hour in 1..11) hour += 12
        if (Regex("\\bam\\b|صبح").containsMatchIn(value) && hour == 12) hour = 0
        return hour to match.groupValues[2].toInt()
    }

    fun mapQuery(text: String): String? {
        val value = ToolPolicy.normalize(text)
        val prefixes = listOf("directions to ", "navigate to ", "map of ", "cómo llegar a ", "como llegar a ", "ruta a ", "mapa de ", "مسیر به ", "مسیریابی به ", "نقشه ")
        return prefixes.firstOrNull(value::startsWith)?.let { value.removePrefix(it).trim().take(150).takeIf(String::isNotBlank) }
    }

    fun isWeather(text: String): Boolean = Regex("(?<![\\p{L}])(?:weather|clima|pronóstico|pronostico|آب و هوا|هوا|هوای)(?![\\p{L}])").containsMatchIn(ToolPolicy.normalize(text))
    fun weatherCity(text: String): String? {
        val value = ToolPolicy.normalize(text).replace("what's", "what is").replace("what’s", "what is")
        if (!isWeather(text)) return null
        val markers = listOf(" in ", " en ", " در ", " برای ")
        val marked = markers.firstOrNull(value::contains)?.let { value.substringAfter(it) }
        val city = (marked ?: value.replace(Regex("(?<![\\p{L}])(?:what|what's|is|the|weather|how|today|please|clima|pronóstico|pronostico|cómo|como|está|esta|el|hoy|آب و هوا|هوا|هوای|امروز|چطوره|چطور است|چیست|لطفا)(?![\\p{L}])"), " "))
            .replace(Regex("[?!؟،,]+"), " ")
            .replace(Regex("\\b(today|please|hoy)\\b|چطوره|چطور است|امروز|لطفا|چیست"), " ")
            .replace(Regex("\\s+"), " ").trim()
        return city.takeIf { it.length in 2..80 && Regex("[\\p{L}\\p{M} '.()–-]+").matches(it) }
    }
}
