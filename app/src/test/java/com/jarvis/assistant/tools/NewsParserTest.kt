package com.jarvis.assistant.tools

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class NewsParserTest {
    @Test fun rssPreservesAttributionAndFiltersRealPublicationDates() {
        val feed = """<rss><channel>
            <item><title>Yesterday &amp; the world - Example</title><link>https://example.com/story</link><source>Example</source><pubDate>Fri, 30 Oct 2026 12:00:00 GMT</pubDate></item>
            <item><title>Today</title><link>https://example.com/new</link><pubDate>Sat, 31 Oct 2026 12:00:00 GMT</pubDate></item>
            <item><title>No date</title><link>https://example.com/unknown</link></item>
            <item><title>Unsafe source</title><link>file:///data/private</link><pubDate>Fri, 30 Oct 2026 12:00:00 GMT</pubDate></item>
            </channel></rss>"""
        val window = ToolPolicy.yesterday(Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"), ZoneOffset.UTC))
        val stories = NewsParser.parse(feed.toByteArray(), window)
        assertEquals(1, stories.size)
        assertEquals("Yesterday & the world - Example", stories.single().title)
        assertEquals("Example", stories.single().publisher)
        assertEquals("https://example.com/story", stories.single().url)
        assertEquals(Instant.parse("2026-10-30T12:00:00Z"), stories.single().publishedAt)
    }

    @Test fun duplicateHeadlinesAreNotRepeated() {
        val feed = """<rss><channel><item><title>One</title><link>https://example.com/1</link></item><item><title>One</title><link>https://example.com/2</link></item></channel></rss>"""
        assertEquals(1, NewsParser.parse(feed.toByteArray()).size)
    }

    @Test(expected = IllegalArgumentException::class) fun oversizedFeedsAreRejected() {
        NewsParser.parse(ByteArray(NewsParser.MAX_FEED_BYTES + 1))
    }

    @Test(expected = IllegalArgumentException::class) fun xmlExternalEntityDeclarationsAreRejected() {
        NewsParser.parse("""<!DOCTYPE rss [<!ENTITY secret SYSTEM "file:///etc/passwd">]><rss><channel><item><title>&secret;</title><link>https://example.com</link></item></channel></rss>""".toByteArray())
    }
}
