package com.jarvis.assistant.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.CharacterCodingException

class AttachmentLimitsTest {
    @Test fun textIsBoundedBeforeParsing() {
        assertEquals(4, AttachmentLimits.readBounded(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)), 4).size)
        assertThrows(IllegalArgumentException::class.java) { AttachmentLimits.readBounded(ByteArrayInputStream(ByteArray(5)), 4) }
    }

    @Test fun preservesMultilingualTextAndUtf16Bom() {
        val source = "Hello, español y فارسی؛ نیم‌فاصله"
        assertEquals(source, AttachmentLimits.decodeText(source.toByteArray()))
        assertEquals(source, AttachmentLimits.decodeText(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + source.toByteArray(Charsets.UTF_16LE)))
        assertThrows(CharacterCodingException::class.java) { AttachmentLimits.decodeText(byteArrayOf(0xC3.toByte())) }
        assertThrows(IllegalArgumentException::class.java) { AttachmentLimits.decodeText(byteArrayOf(65, 0, 66)) }
    }

    @Test fun truncatesWithoutBreakingSurrogatePairs() {
        val result = AttachmentLimits.truncate("x".repeat(23_999) + "😀" + "more")
        assertEquals(23_999, result.length)
        assertFalse(result.last().isHighSurrogate())
    }

    @Test fun imageDownsamplingRespectsPhoneMemoryBudget() {
        assertEquals(1, AttachmentLimits.imageSampleSize(800, 600))
        val sample = AttachmentLimits.imageSampleSize(12_000, 9_000)
        assertTrue(12_000 / sample <= 1_600)
        assertTrue(9_000 / sample <= 1_600)
        assertThrows(IllegalArgumentException::class.java) { AttachmentLimits.imageSampleSize(-1, 20) }
        assertThrows(IllegalArgumentException::class.java) { AttachmentLimits.imageSampleSize(200_000, 200_000) }
    }

    @Test fun hashesAndModelUrlsArePinned() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", AttachmentLimits.sha256(ByteArrayInputStream("abc".toByteArray())))
        for (language in AppLanguage.entries) {
            val asset = OcrModelCatalog.selected(language)
            assertEquals(64, asset.sha256.length)
            assertTrue(OcrModelCatalog.url(asset).startsWith("https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/${OcrModelCatalog.COMMIT}/"))
        }
        assertEquals("fas", OcrModelCatalog.selected(AppLanguage.FA).id)
    }
}
