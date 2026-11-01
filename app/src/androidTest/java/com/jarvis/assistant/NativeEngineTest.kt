package com.jarvis.assistant

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.googlecode.tesseract.android.TessBaseAPI
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AttachmentLimits
import com.jarvis.assistant.core.OcrModelCatalog
import com.jarvis.assistant.voice.OfflineVoiceManager
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Native Android inference checks; not microphone, speaker-audibility, or phone benchmarks. */
@RunWith(AndroidJUnit4::class)
class NativeEngineTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun bundledEnglishOcrReadsActualRenderedPixels() {
        val root = File(context.cacheDir, "native-ocr-check").apply { mkdirs() }
        val data = File(root, "tessdata").apply { mkdirs() }
        val model = File(data, "eng.traineddata")
        context.assets.open("ocr/eng.traineddata").use { source -> model.outputStream().use(source::copyTo) }
        assertEquals(OcrModelCatalog.english.sha256, model.inputStream().use(AttachmentLimits::sha256))
        val bitmap = Bitmap.createBitmap(840, 180, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 84f; typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("HELLO JARVIS", 30f, 120f, paint)
        val engine = TessBaseAPI()
        try {
            assertTrue(engine.init(root.absolutePath, "eng", TessBaseAPI.OEM_LSTM_ONLY))
            engine.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE)
            engine.setImage(bitmap)
            val actual = engine.getUTF8Text().orEmpty().trim().uppercase()
            assertTrue("Actual OCR output: $actual", actual.contains("HELLO") && actual.contains("JARVIS"))
        } finally { engine.recycle(); bitmap.recycle(); root.deleteRecursively() }
    }

    @Test fun installedNeuralVoicesGenerateNonSilentAudioInAllThreeLanguages() {
        val manager = OfflineVoiceManager(context)
        // A normal CI run can omit these explicit downloads. A prepared native validation run
        // must install all three verified models; its skipped/passed result is reported exactly.
        assumeTrue("Install the three documented, verified voice models before this check", AppLanguage.entries.all { manager.modelPath(it) != null })
        for (language in AppLanguage.entries) {
            val directory = checkNotNull(manager.modelPath(language))
            val spec = manager.spec(language)
            val voice = OfflineTts(config = OfflineTtsConfig(model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = File(directory, spec.modelName).absolutePath,
                    tokens = File(directory, "tokens.txt").absolutePath,
                    dataDir = File(directory, "espeak-ng-data").absolutePath,
                ), numThreads = 2, debug = false, provider = "cpu",
            ), maxNumSentences = 1))
            try {
                val sentence = when (language) {
                    AppLanguage.EN -> "Hello, I'm Jarvis."
                    AppLanguage.ES -> "Hola, soy Jarvis."
                    AppLanguage.FA -> "سلام، من جارویس هستم."
                }
                val audio = voice.generate(sentence, sid = 0, speed = 1f)
                assertTrue("${language.code}: sample rate", audio.sampleRate in 16000..48000)
                assertTrue("${language.code}: real audio length", audio.samples.size > audio.sampleRate / 4)
                assertTrue("${language.code}: non-silent audio", audio.samples.any { abs(it) > 0.001f })
            } finally { voice.release() }
        }
        manager.shutdown()
    }
}
