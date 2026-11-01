package com.jarvis.assistant.tools

import java.net.URI
import java.text.Normalizer
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** Policy is independent of model output and Android UI classes. */
object ToolPolicy {
    const val CONFIRMATION_TTL_MS = 120_000L

    fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)
        .lowercase(java.util.Locale.ROOT)
        .replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ')
        .replace(Regex("\\s+"), " ").trim()

    fun confirmationCurrent(createdAt: Long, now: Long): Boolean =
        createdAt <= now && now - createdAt <= CONFIRMATION_TTL_MS

    fun allowedWebUrl(value: String): Boolean = try {
        val uri = URI(value)
        val host = uri.host?.lowercase(java.util.Locale.ROOT).orEmpty()
        uri.scheme == "https" && uri.userInfo == null && (uri.port == -1 || uri.port == 443) &&
            host.isNotBlank() && host != "localhost" && !host.endsWith(".localhost") &&
            !host.endsWith(".local") && !host.endsWith(".internal") &&
            !host.contains(':') && !host.matches(Regex("[0-9.]+"))
    } catch (_: Exception) { false }

    fun protectedPackage(packageName: String): Boolean {
        val value = packageName.lowercase(java.util.Locale.ROOT)
        return value in setOf(
            "com.google.android.apps.walletnfcrel", "com.google.android.apps.nbu.paisa.user",
            "com.paypal.android.p2pmobile", "com.venmo", "com.cashapp.android",
            "com.samsung.android.spay", "com.android.settings", "com.android.systemui",
        ) || listOf("bank", "banking", "wallet", "bitwarden", "lastpass", "onepassword", "1password",
            "dashlane", "authenticator", "keepass", "enpass", "paypal", "revolut", "monzo",
            "n26", "chase", "wellsfargo", "capitalone", "coinbase", "binance").any(value::contains)
    }

    fun sensitiveText(value: String): String = value
        .replace(Regex("(?<![\\p{L}\\p{Nd}])(?:[\\p{Nd}][ -]?){13,19}(?![\\p{L}\\p{Nd}])"), "[sensitive number]")
        .replace(Regex("(?i)(?:otp|verification code|security code|one.time code|código|رمز|کد تأیید|کد تایید)[ :：-]*[\\p{Nd} -]{4,10}"), "[private code]")
        .replace(Regex("(?<![\\p{L}\\d])[\\p{Nd}]{4,8}(?![\\p{L}\\d])"), "[number]")

    /** Half-open local-calendar yesterday interval. Handles 23/25-hour DST days. */
    fun yesterday(clock: Clock = Clock.systemDefaultZone()): DateWindow {
        val date = LocalDate.now(clock).minusDays(1)
        return DateWindow(date, date.atStartOfDay(clock.zone).toInstant(), date.plusDays(1).atStartOfDay(clock.zone).toInstant())
    }
}

data class DateWindow(val date: LocalDate, val start: Instant, val endExclusive: Instant) {
    fun contains(instant: Instant): Boolean = instant >= start && instant < endExclusive
}

object LocalIntentParser {
    fun newsTopic(text: String): String {
        val normalized = ToolPolicy.normalize(text)
            .replace("yesterday's", "yesterday").replace("yesterday’s", "yesterday")
            .replace("today's", "today").replace("today’s", "today")
        val marker = listOf(" about ", " sobre ", " acerca de ", " درباره ", " در مورد ").firstOrNull(normalized::contains)
        if (marker != null) return normalized.substringAfter(marker).trim(' ', '?', '!', '.', '؟', '،').take(150)
        return normalized
            .replace(Regex("\\b(hey|jarvis|give|me|the|what|are|is|latest|today|yesterday|news|headlines|please|show|tell|dame|las|los|noticias|ayer|hoy|últimas|ultimas|titulares|por|favor|hola|de)\\b"), " ")
            .replace(Regex("(?<![\\p{L}\\p{N}])(?:سلام|جارویس|اخبار|خبرهای|خبر ها|خبرها|دیروز|امروز|لطفا|بهم بده|به من بگو|بگو|چیست|جدیدترین|آخرین|را)(?![\\p{L}\\p{N}])"), " ")
            .replace(Regex("\\s+"), " ").trim(' ', '?', '!', '.', '؟', '،').take(150)
    }
    private val newsWords = Regex("\\b(news|headlines|noticias|titulares)\\b|اخبار|خبرها|خبر های|خبرهای")
    fun isNews(text: String): Boolean = newsWords.containsMatchIn(ToolPolicy.normalize(text))
    fun isYesterday(text: String): Boolean = Regex("\\byesterday\\b|\\bayer\\b|دیروز").containsMatchIn(ToolPolicy.normalize(text))
    fun isScreen(text: String): Boolean {
        val t = ToolPolicy.normalize(text)
        if (listOf("how do i", "how to", "how can i", "cómo puedo", "como puedo", "چطور می توانم", "چگونه می توانم").any(t::startsWith)) return false
        val target = listOf("my screen", "on screen", "screen read", "mi pantalla", "en pantalla", "صفحه نمایش", "صفحه من", "روی صفحه", "صفحه گوشی").any(t::contains)
        val request = Regex("\\b(read|describe|summarize|translate|what|lee|describe|resume|traduce|qué|que)\\b|بخوان|توضیح|خلاصه|ترجمه|چی|چه چیزی").containsMatchIn(t)
        return target && request
    }
    fun isNotifications(text: String): Boolean {
        val t = ToolPolicy.normalize(text)
        if (listOf("how do i", "how to", "how can i", "cómo puedo", "como puedo", "چطور می توانم", "چگونه می توانم").any(t::startsWith)) return false
        val target = listOf("notification", "notificacion", "notificación", "notificaciones", "اعلان", "نوتیفیکیشن").any(t::contains)
        val request = Regex("\\b(read|summarize|show|what|check|lee|resume|muestra|qué|que)\\b|بخوان|خلاصه|نشون|نشان|چی|چه چیزی").containsMatchIn(t)
        return target && request
    }
    fun rememberedText(text: String): String? {
        // Match the original text so normalization/extra spaces cannot corrupt the saved payload.
        val space = "[\\s\\p{Z}\\u200c]+"
        val prefix = "(?:remember(?:${space}that)?|recuerda(?:${space}que)?|[یي]ادت${space}(?:باشد|باشه|بماند|بمونه)(?:${space}[کك]ه)?|به${space}خاطر${space}بسپار|به${space}[یي]اد${space}داشته${space}باش)"
        return Regex("^$prefix$space(.+)$", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .matchEntire(text.trim())?.groupValues?.get(1)?.trim()?.takeIf(String::isNotBlank)
    }
    fun youtubeQuery(text: String): String? {
        val t = ToolPolicy.normalize(text)
        if (!listOf("youtube", "you tube", "یوتیوب", "یو تیوب").any(t::contains)) return null
        val stripped = t.replace(Regex("youtube|you tube|یوتیوب|یو تیوب"), " ")
            .replace(Regex("\\b(open|launch|search|find|for|on|in|please|and|abre|abrir|busca|buscar|en|por|y)\\b"), " ")
            .replace(Regex("(?<![\\p{L}\\p{Nd}])(?:باز کن|جستجو کن|جستجو|پیدا کن|لطفا|برای|را)(?![\\p{L}\\p{Nd}])"), " ")
            .replace(Regex("\\s+"), " ").trim()
        return stripped
    }
    fun appName(text: String): String? {
        val normalized = ToolPolicy.normalize(text)
        val prefixes = listOf("open ", "launch ", "abre ", "abrir ", "باز کن ", "برنامه ")
        prefixes.firstOrNull(normalized::startsWith)?.let {
            return normalized.removePrefix(it).removeSuffix(" را باز کن").removeSuffix(" باز کن").trim().takeIf(String::isNotBlank)
        }
        if (normalized.endsWith(" را باز کن")) return normalized.removeSuffix(" را باز کن").trim().takeIf(String::isNotBlank)
        return null
    }
}
