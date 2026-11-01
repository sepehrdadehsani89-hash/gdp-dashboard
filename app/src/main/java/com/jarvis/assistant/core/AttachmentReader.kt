package com.jarvis.assistant.core

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.googlecode.tesseract.android.TessBaseAPI
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Reads only a URI that the user selected through Android's document/photo picker. */
class AttachmentReader(
    context: Context,
    private val onStatus: (String) -> Unit = {},
) {
    private val app = context.applicationContext
    private val resolver = app.contentResolver
    private val models = File(app.filesDir, "ocr/tessdata")
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS).followRedirects(false).build()

    fun displayName(uri: Uri): String {
        require(uri.scheme == ContentResolver.SCHEME_CONTENT) { "Select a file using the Android picker." }
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.take(160) else null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Selected file"
    }

    suspend fun read(uri: Uri, language: AppLanguage): String = withContext(Dispatchers.IO) {
        require(uri.scheme == ContentResolver.SCHEME_CONTENT) {
            language.choose("Choose a file using the Android picker.", "Elige un archivo con el selector de Android.", "فایل را با انتخابگر اندروید انتخاب کنید.")
        }
        currentCoroutineContext().ensureActive()
        val name = displayName(uri)
        val mime = resolver.getType(uri)?.lowercase().orEmpty()
        val extension = name.substringAfterLast('.', "").lowercase()
        val isImage = mime.startsWith("image/") || extension in setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "bmp")
        val isPdf = mime == "application/pdf" || extension == "pdf"
        val isText = mime.startsWith("text/") || mime in setOf("application/json", "application/xml", "application/csv") ||
            extension in setOf("txt", "md", "csv", "json", "xml", "html", "htm", "log", "srt", "vtt")
        require(isImage || isPdf || isText) {
            language.choose("Choose a text document, PDF, or photo. This file type is not supported.", "Elige texto, PDF o una foto. Este tipo de archivo no es compatible.", "فایل متنی، PDF یا عکس انتخاب کنید. این نوع فایل پشتیبانی نمی‌شود.")
        }
        val limit = if (isText && !isPdf && !isImage) AttachmentLimits.MAX_TEXT_BYTES else AttachmentLimits.MAX_DOCUMENT_BYTES
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) require(cursor.getLong(0) <= limit) {
                language.choose("This file is too large. Text is limited to 1 MB; photos and PDFs to 20 MB.", "El archivo es demasiado grande. Máximo: texto 1 MB; fotos y PDF 20 MB.", "فایل خیلی بزرگ است. متن تا ۱ مگابایت و عکس یا PDF تا ۲۰ مگابایت پشتیبانی می‌شود.")
            }
        }
        val bytes = resolver.openInputStream(uri)?.use { AttachmentLimits.readBounded(it, limit) }
            ?: throw IOException(language.choose("Cannot open this file.", "No se puede abrir el archivo.", "این فایل باز نمی‌شود."))
        currentCoroutineContext().ensureActive()
        AttachmentLimits.truncate(when {
            isImage -> readImage(bytes, language)
            isPdf -> readPdf(bytes, language)
            else -> AttachmentLimits.decodeText(bytes)
        })
    }

    private fun readPdf(bytes: ByteArray, language: AppLanguage): String {
        onStatus(language.choose("Reading PDF on this phone…", "Leyendo el PDF en este teléfono…", "در حال خواندن PDF روی گوشی…"))
        PDFBoxResourceLoader.init(app)
        return PDDocument.load(ByteArrayInputStream(bytes)).use { document ->
            require(!document.isEncrypted) {
                language.choose("Unlock or export this PDF before sharing it.", "Desbloquea o exporta este PDF antes de compartirlo.", "پیش از اشتراک، قفل PDF را باز کنید یا از آن خروجی بگیرید.")
            }
            val text = PDFTextStripper().apply { startPage = 1; endPage = minOf(document.numberOfPages, 8); sortByPosition = true }.getText(document).trim()
            if (text.isBlank()) language.choose("This PDF has no readable text layer. Share a screenshot to extract printed text; image contents are not described.", "El PDF no tiene una capa de texto. Comparte una captura para extraer texto impreso; no se describen las imágenes.", "این PDF لایهٔ متنی خوانا ندارد. برای خواندن نوشته‌ها، تصویر صفحه را انتخاب کنید؛ محتوای تصویری توصیف نمی‌شود.")
            else if (document.numberOfPages > 8) language.choose("Text from the first 8 of ${document.numberOfPages} PDF pages:\n", "Texto de las primeras 8 de ${document.numberOfPages} páginas:\n", "متن ۸ صفحهٔ اول از ${document.numberOfPages} صفحهٔ PDF:\n") + text else text
        }
    }

    private suspend fun readImage(bytes: ByteArray, language: AppLanguage): String = ocrLock.withLock {
        val selected = OcrModelCatalog.selected(language)
        ensureModel(selected, language)
        if (selected.id != OcrModelCatalog.english.id) ensureModel(OcrModelCatalog.english, language)
        onStatus(language.choose("Reading photo text on this phone…", "Leyendo el texto de la foto en este teléfono…", "در حال خواندن نوشته‌های عکس روی گوشی…"))
        currentCoroutineContext().ensureActive()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = AttachmentLimits.imageSampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw IOException(language.choose("The selected image could not be decoded.", "No se pudo decodificar la imagen.", "تصویر انتخابی خوانده نمی‌شود."))
        val bitmap = correctOrientation(decoded, bytes)
        if (bitmap !== decoded) decoded.recycle()
        var tess: TessBaseAPI? = null
        try {
            val operationContext = currentCoroutineContext()
            val engine = TessBaseAPI { if (!operationContext.isActive) tess?.stop() }
            tess = engine
            val modelLanguage = if (selected.id == "eng") "eng" else "${selected.id}+eng"
            check(engine.init(models.parentFile!!.absolutePath, modelLanguage, TessBaseAPI.OEM_LSTM_ONLY)) {
                language.choose("The offline OCR model could not start.", "No se pudo iniciar el modelo OCR local.", "مدل خواندن متن روی گوشی اجرا نشد.")
            }
            engine.setDebug(false)
            engine.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
            engine.setImage(bitmap)
            // HOCR recognition installs upstream's cancellation/progress monitor;
            // calling getUTF8Text first would be an uninterruptible recognition pass.
            engine.getHOCRText(0)
            currentCoroutineContext().ensureActive()
            val text = engine.getUTF8Text().orEmpty().trim()
            currentCoroutineContext().ensureActive()
            if (text.isBlank()) language.choose("No readable text was found. Photo reading extracts printed text; it does not identify people, objects, or scenes.", "No se encontró texto legible. Esta función extrae texto impreso; no identifica personas, objetos ni escenas.", "نوشتهٔ خوانایی پیدا نشد. خواندن عکس برای نوشته‌هاست و افراد، اشیا یا منظره را شناسایی نمی‌کند.")
            else language.choose("Photo text (OCR; check important details):\n", "Texto de la foto (OCR; verifica los datos importantes):\n", "نوشته‌های عکس (تشخیص خودکار؛ جزئیات مهم را بررسی کنید):\n") + text
        } finally {
            tess?.recycle()
            bitmap.recycle()
        }
    }

    private fun correctOrientation(bitmap: Bitmap, bytes: ByteArray): Bitmap {
        val orientation = runCatching { ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(270f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private suspend fun ensureModel(asset: OcrLanguageAsset, language: AppLanguage) {
        models.mkdirs()
        val target = File(models, "${asset.id}.traineddata")
        if (target.isFile && target.inputStream().use { AttachmentLimits.sha256(it) } == asset.sha256) return
        val bundled = try { app.assets.open("ocr/${asset.id}.traineddata") } catch (_: FileNotFoundException) { null }
        if (bundled != null) {
            val staging = File(models, ".${asset.id}-${UUID.randomUUID()}.part")
            try {
                bundled.use { input -> staging.outputStream().use { output -> input.copyTo(output) } }
                check(staging.inputStream().use { AttachmentLimits.sha256(it) } == asset.sha256) { "Bundled OCR model integrity check failed." }
                java.nio.file.Files.move(staging.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
                return
            } finally { staging.delete() }
        }
        onStatus(language.choose("Downloading offline photo-reading data (${asset.id}); up to 4 MB per language…", "Descargando datos locales de lectura (${asset.id}); hasta 4 MB por idioma…", "در حال دانلود دادهٔ خواندن عکس (${asset.id})؛ تا ۴ مگابایت برای هر زبان…"))
        val temporary = File(models, ".${asset.id}-${UUID.randomUUID()}.part")
        try {
            client.newCall(Request.Builder().url(OcrModelCatalog.url(asset)).build()).execute().use { response ->
                check(response.isSuccessful) { "OCR model download failed (${response.code})." }
                val body = response.body ?: throw IOException("OCR model download was empty.")
                require(body.contentLength() <= 8 * 1_048_576) { "OCR model exceeds the expected size." }
                body.byteStream().use { input -> temporary.outputStream().use { output ->
                    val buffer = ByteArray(8_192)
                    var total = 0
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        total += count
                        require(total <= 8 * 1_048_576) { "OCR model exceeds the expected size." }
                        output.write(buffer, 0, count)
                    }
                } }
            }
            check(temporary.inputStream().use { AttachmentLimits.sha256(it) } == asset.sha256) { "OCR model integrity check failed." }
            java.nio.file.Files.move(temporary.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
        } finally {
            temporary.delete()
        }
    }

    companion object { private val ocrLock = Mutex() }
}
