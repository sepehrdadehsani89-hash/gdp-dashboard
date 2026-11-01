package com.jarvis.assistant.tools

import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

data class NewsArticle(val title: String, val url: String, val publisher: String, val publishedAt: Instant?)

/** Parse bounded RSS. External entities and DTDs are disabled; no remote XML resources. */
object NewsParser {
    const val MAX_FEED_BYTES = 1_000_000
    fun parse(bytes: ByteArray, window: DateWindow? = null): List<NewsArticle> {
        require(bytes.size <= MAX_FEED_BYTES) { "News response is too large" }
        require(!Regex("<!DOCTYPE|<!ENTITY", RegexOption.IGNORE_CASE).containsMatchIn(bytes.toString(Charsets.UTF_8))) { "DTD declarations are not supported" }
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        // Some Android parser implementations do not expose this setter.
        // XInclude is off by default; DTD/entity declarations are independently rejected.
        runCatching { factory.isXIncludeAware = false }
        factory.setExpandEntityReferences(false)
        // Android's parser supports SAX external-entity features; disallow-doctype
        // may be unavailable. EntityResolver is an independent deny-all boundary.
        listOf(
            "http://xml.org/sax/features/external-general-entities" to false,
            "http://xml.org/sax/features/external-parameter-entities" to false,
            "http://apache.org/xml/features/nonvalidating/load-external-dtd" to false,
            "http://apache.org/xml/features/disallow-doctype-decl" to true,
        ).forEach { (name, value) -> runCatching { factory.setFeature(name, value) } }
        val builder = factory.newDocumentBuilder()
        builder.setEntityResolver { _, _ -> org.xml.sax.InputSource(java.io.StringReader("")) }
        val document = ByteArrayInputStream(bytes).use(builder::parse)
        val nodes = document.getElementsByTagName("item")
        return (0 until minOf(nodes.length, 100)).mapNotNull { index ->
            val element = nodes.item(index) as? Element ?: return@mapNotNull null
            fun value(tag: String): String = element.getElementsByTagName(tag).item(0)?.textContent.orEmpty().trim()
            val title = value("title").replace(Regex("<[^>]*>"), "").take(400)
            val url = value("link")
            val date = runCatching { ZonedDateTime.parse(value("pubDate"), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }.getOrNull()
            if (title.isBlank() || !ToolPolicy.allowedWebUrl(url) || (window != null && (date == null || !window.contains(date)))) return@mapNotNull null
            NewsArticle(title, url, value("source").take(100), date)
        }.distinctBy { it.title.lowercase() }.take(8)
    }
}
