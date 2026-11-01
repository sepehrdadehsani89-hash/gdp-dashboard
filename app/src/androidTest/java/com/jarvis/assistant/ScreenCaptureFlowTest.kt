package com.jarvis.assistant

import android.app.UiAutomation
import android.graphics.BitmapFactory
import android.view.accessibility.AccessibilityNodeInfo
import android.provider.Settings
import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.googlecode.tesseract.android.TessBaseAPI
import com.jarvis.assistant.core.AssistantSettings
import com.jarvis.assistant.tools.JarvisAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.*
import java.io.File

/** Dedicated emulator only: approves Android's real one-shot projection dialog; never uploads. */
class ScreenCaptureFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var runtime: AssistantRuntime

    @Test fun realAndroidProjectionCapturesOneReadableFrameAndClearRemovesIt() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        // The runner reinstalls the test app; enable access after that installation, on the
        // dedicated emulator only. Use the shell identity held by Android's test automation.
        automation.adoptShellPermissionIdentity(Manifest.permission.WRITE_SECURE_SETTINGS)
        try {
            Settings.Secure.putString(instrumentation.targetContext.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                "com.jarvis.next/com.jarvis.assistant.tools.JarvisAccessibilityService")
            Settings.Secure.putInt(instrumentation.targetContext.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        } finally { automation.dropShellPermissionIdentity() }
        compose.activityRule.scenario.onActivity {
            runtime = (it.application as JarvisApplication).runtime
            runtime.stopEverything()
        }
        runBlocking(Dispatchers.IO) { runtime.store.clearAll() }
        compose.runOnUiThread { runtime.store.updateSettings(AssistantSettings(provider = "gemini", model = "gemini-fixture", onboardingComplete = true, speakReplies = false)) }
        compose.waitUntil(20_000) { runtime.state.value.settings.onboardingComplete && JarvisAccessibilityService.connected }
        compose.onNode(hasSetTextAction()).performTextInput("Analyze my screen")
        compose.onNode(hasSetTextAction()).performImeAction()
        clickButton(automation, "Capture")
        clickButton(automation, "Start now")
        compose.waitUntil(20_000) { runtime.state.value.screenReady || runtime.state.value.error != null }
        Assert.assertTrue("Capture failed: ${runtime.state.value.error}", runtime.state.value.screenReady)
        val bytes = runtime.state.value.screenImage!!.copyOf()
        Assert.assertTrue(bytes.size > 1000)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        Assert.assertNotNull(bitmap)
        Assert.assertTrue(maxOf(bitmap.width, bitmap.height) <= 1280)
        val directory = File(compose.activity.cacheDir, "capture-ocr").apply { File(this, "tessdata").mkdirs() }
        compose.activity.assets.open("ocr/eng.traineddata").use { input -> File(directory, "tessdata/eng.traineddata").outputStream().use(input::copyTo) }
        val ocr = TessBaseAPI()
        try {
            Assert.assertTrue(ocr.init(directory.absolutePath, "eng", TessBaseAPI.OEM_LSTM_ONLY))
            ocr.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
            ocr.setImage(bitmap)
            val text = ocr.getUTF8Text().orEmpty().uppercase()
            Assert.assertTrue("Projection must capture app content", text.contains("JARVIS") || text.contains("SERVICE") || text.contains("SCREEN"))
            Assert.assertFalse("Capture dialog must be gone", text.contains("START NOW") || text.contains("CAPTURE THIS SCREEN"))
        } finally { ocr.recycle(); bitmap.recycle(); directory.deleteRecursively(); bytes.fill(0) }
        compose.runOnUiThread { runtime.clearScreenContext() }
        compose.waitUntil(5000) { !runtime.state.value.screenReady && runtime.state.value.screenImage == null }
    }

    private fun clickButton(automation: UiAutomation, label: String) {
        val end = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < end) {
            val root = automation.rootInActiveWindow
            val button = root?.findAccessibilityNodeInfosByText(label)?.firstOrNull { it.text?.toString()?.equals(label, ignoreCase = true) == true }
            if (button != null) {
                val target = if (button.isClickable) button else button.parent
                if (target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return
            }
            Thread.sleep(100)
        }
        Assert.fail("System button not found: $label")
    }
    @After fun stop() { if (::runtime.isInitialized) compose.runOnUiThread { runtime.stopEverything() } }
}
