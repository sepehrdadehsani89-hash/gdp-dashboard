package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.assistant.core.AssistantSettings
import com.jarvis.assistant.omnisystem.CameraHudActivity
import org.junit.*
import org.junit.Assert.*
import java.io.File

/** Real CameraX pipeline against the emulator's emulated camera, not a handset-quality test. */
class CameraHudFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var runtime:AssistantRuntime
    private lateinit var original:AssistantSettings
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun shell(command:String)=ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)).bufferedReader().use{it.readText()}
    @Before fun prepare(){compose.activityRule.scenario.onActivity{runtime=(it.application as JarvisApplication).runtime;runtime.stopEverything();runtime.clearAttachments();original=runtime.store.settings.value;runtime.store.updateSettings(AssistantSettings(onboardingComplete=true,speakReplies=false));runtime.hud.save(runtime.hud.state.value.settings.copy(reducedMotion=true))};compose.waitUntil(30_000){runtime.state.value.settings.onboardingComplete}}
    @After fun restore(){compose.activityRule.scenario.onActivity{runtime.stopEverything();runtime.clearAttachments();runtime.store.updateSettings(original)}}
    @Test fun cameraCaptureDecodesAndRequiresExplicitAttachment(){
        shell("pm grant ${context.packageName} ${Manifest.permission.CAMERA}")
        compose.runOnUiThread{compose.activity.startActivity(Intent(compose.activity,CameraHudActivity::class.java))}
        compose.waitUntil(90_000){runCatching{compose.onAllNodesWithText("Front camera active · preview only").fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)}
        assertFalse(runtime.state.value.audio.recording);assertTrue(runtime.state.value.attachedFiles.isEmpty())
        compose.onNodeWithText("Capture still").performClick()
        compose.waitUntil(120_000){runCatching{compose.onAllNodesWithText("Use this photo in Assistant").fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)}
        compose.onNode(hasText("Captured JPEG:",substring=true)).assertExists()
        val files=File(context.cacheDir,"jarvis-camera").listFiles().orEmpty().filter{it.extension=="jpg"}
        assertTrue(files.any{it.length()>0 && android.graphics.BitmapFactory.decodeFile(it.path)!=null})
        assertTrue(runtime.state.value.attachedFiles.isEmpty())
        compose.onNodeWithText("Use this photo in Assistant").performClick()
        compose.waitUntil(20_000){runtime.state.value.attachedFiles.size==1}
        assertTrue(runtime.state.value.attachedFiles.single().endsWith(".jpg"));assertFalse(runtime.state.value.audio.recording)
    }
}
