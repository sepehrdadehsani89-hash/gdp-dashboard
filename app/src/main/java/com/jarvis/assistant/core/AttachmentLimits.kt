package com.jarvis.assistant.core

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

/** Bounds applied before parsing user-selected documents; also JVM-testable. */
object AttachmentLimits {
    const val MAX_TEXT_BYTES = 1_048_576
    const val MAX_DOCUMENT_BYTES = 20 * 1_048_576
    const val MAX_TEXT_CHARS = 24_000
    const val MAX_IMAGE_EDGE = 1_600

    fun readBounded(input: InputStream, maxBytes: Int): ByteArray {
        require(maxBytes > 0)
        val output = ByteArrayOutputStream(minOf(maxBytes, 8_192))
        val buffer = ByteArray(8_192)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            if (count == 0) continue
            require(count <= maxBytes - total) { "Selected file exceeds the supported size." }
            output.write(buffer, 0, count)
            total += count
        }
        return output.toByteArray()
    }

    fun decodeText(bytes: ByteArray): String {
        val text = if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            Charsets.UTF_16LE.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes, 2, bytes.size - 2)).toString()
        } else if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            Charsets.UTF_16BE.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes, 2, bytes.size - 2)).toString()
        } else {
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
                .removePrefix("\uFEFF")
        }
        require(!text.contains('\u0000')) { "The selected file is not a supported text document." }
        return truncate(text)
    }

    fun truncate(text: String): String {
        if (text.length <= MAX_TEXT_CHARS) return text
        val end = if (text[MAX_TEXT_CHARS - 1].isHighSurrogate()) MAX_TEXT_CHARS - 1 else MAX_TEXT_CHARS
        return text.substring(0, end)
    }

    fun imageSampleSize(width: Int, height: Int): Int {
        require(width > 0 && height > 0 && width <= 100_000 && height <= 100_000) {
            "The selected image dimensions are unsupported."
        }
        var sample = 1
        while (width / sample > MAX_IMAGE_EDGE || height / sample > MAX_IMAGE_EDGE) sample *= 2
        return sample
    }

    fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8_192)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            if (count > 0) digest.update(buffer, 0, count)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

data class OcrLanguageAsset(val id: String, val sha256: String)

object OcrModelCatalog {
    // Official repository HEAD audited 2026-10-31. Pinning avoids mutable model downloads.
    const val COMMIT = "87416418657359cb625c412a48b6e1d6d41c29bd"
    val english = OcrLanguageAsset("eng", "7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2")
    val spanish = OcrLanguageAsset("spa", "6f2e04d02774a18f01bed44b1111f2cd7f3ba7ac9dc4373cd3f898a40ea6b464")
    val persian = OcrLanguageAsset("fas", "db1c0a91208aff00d3cf1ed2c1d23f76419afd5f024688b4f71adc3f2ce4a505")
    fun selected(language: AppLanguage): OcrLanguageAsset = when (language) {
        AppLanguage.EN -> english
        AppLanguage.ES -> spanish
        AppLanguage.FA -> persian
    }
    fun url(asset: OcrLanguageAsset) = "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/$COMMIT/${asset.id}.traineddata"
}
