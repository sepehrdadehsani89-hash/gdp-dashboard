package com.jarvis.assistant.tools

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class ToolPolicyTest {
    @Test fun memoryPrefixPreservesOriginalContentWithExtraSpacesAndPersianUiPhrase() {
        assertEquals("My flight is Friday", LocalIntentParser.rememberedText("Remember   that   My flight is Friday"))
        assertEquals("فردا می‌روم تهران", LocalIntentParser.rememberedText("یادت بماند که فردا می‌روم تهران"))
        assertEquals("Mi vuelo es el viernes", LocalIntentParser.rememberedText("Recuerda que Mi vuelo es el viernes"))
    }
    @Test fun confirmationsExpireAndCannotBeFutureDated() {
        assertTrue(ToolPolicy.confirmationCurrent(1_000, 1_000))
        assertTrue(ToolPolicy.confirmationCurrent(1_000, 121_000))
        assertFalse(ToolPolicy.confirmationCurrent(1_000, 121_001))
        assertFalse(ToolPolicy.confirmationCurrent(1_001, 1_000))
    }

    @Test fun onlyPublicHttpsNavigationIsAccepted() {
        assertTrue(ToolPolicy.allowedWebUrl("https://www.youtube.com/results?search_query=music"))
        assertTrue(ToolPolicy.allowedWebUrl("https://news.google.com/rss/articles/example"))
        listOf("http://example.com", "file:///data/private", "javascript:alert(1)", "https://user:pass@example.com", "https://localhost/path", "https://example.local", "https://127.0.0.1", "https://10.2.3.4", "https://[::1]", "https://example.com:8443", "intent://app").forEach {
            assertFalse(it, ToolPolicy.allowedWebUrl(it))
        }
    }

    @Test fun protectedBaselineCoversPaymentsPasswordsAndSystemSettings() {
        listOf("com.bank.mobile", "com.paypal.android.p2pmobile", "com.x8bit.bitwarden", "com.onepassword.android", "com.android.settings", "com.google.android.apps.walletnfcrel", "com.example.authenticator").forEach {
            assertTrue(it, ToolPolicy.protectedPackage(it))
        }
        assertFalse(ToolPolicy.protectedPackage("com.google.android.youtube"))
    }

    @Test fun privateCodesAndCardDigitsAreRedactedInBothScripts() {
        val text = ToolPolicy.sensitiveText("OTP: 123456; card 4111 1111 1111 1111; شماره ۴۱۱۱۱۱۱۱۱۱۱۱۱۱۱۱")
        assertFalse(text.contains("123456"))
        assertFalse(text.contains("4111"))
        assertFalse(text.contains("۴۱۱۱"))
        assertTrue(text.contains("[private code]"))
    }

    @Test fun yesterdayUsesCalendarBoundsAcrossDaylightSavings() {
        val clock = Clock.fixed(Instant.parse("2026-03-09T12:00:00Z"), ZoneId.of("America/New_York"))
        val window = ToolPolicy.yesterday(clock)
        assertEquals("2026-03-08", window.date.toString())
        assertEquals(23, Duration.between(window.start, window.endExclusive).toHours())
        assertTrue(window.contains(window.start))
        assertFalse(window.contains(window.endExclusive))
    }

    @Test fun englishSpanishAndPersianCommandsRouteLocally() {
        listOf("Give me yesterday news", "Dame las noticias de ayer", "اخبار دیروز را بگو").forEach {
            assertTrue(LocalIntentParser.isNews(it))
            assertTrue(LocalIntentParser.isYesterday(it))
        }
        assertEquals("My flight is Friday", LocalIntentParser.rememberedText("Remember that My flight is Friday"))
        assertEquals("می‌روم تهران", LocalIntentParser.rememberedText("یادت باشه می‌روم تهران"))
        assertEquals("música", LocalIntentParser.youtubeQuery("Abre YouTube y busca música"))
        assertEquals("موسیقی", LocalIntentParser.youtubeQuery("یوتیوب موسیقی جستجو کن"))
        assertEquals("ایران", LocalIntentParser.youtubeQuery("یوتیوب ایران را جستجو کن"))
        assertNull(LocalIntentParser.rememberedText("What should I remember?"))
    }
}
