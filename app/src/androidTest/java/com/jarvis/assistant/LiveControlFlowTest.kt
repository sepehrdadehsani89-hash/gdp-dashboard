package com.jarvis.assistant

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.widget.LinearLayout
import android.widget.Button
import android.widget.EditText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.assistant.tools.JarvisAccessibilityService
import com.jarvis.assistant.tools.LiveRequest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveControlFlowTest {
    @get:org.junit.Rule val compose = androidx.compose.ui.test.junit4.createAndroidComposeRule<MainActivity>()
    @Test fun currentTargetTapDraftAndStaleTargetGuard() {
        val i = InstrumentationRegistry.getInstrumentation()
        compose.activityRule.scenario.onActivity { (it.application as JarvisApplication).runtime.stopEverything() }
        compose.waitForIdle()
        val automation = i.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        automation.adoptShellPermissionIdentity(android.Manifest.permission.WRITE_SECURE_SETTINGS)
        try {
            android.provider.Settings.Secure.putString(i.targetContext.contentResolver, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "")
            Thread.sleep(500)
            android.provider.Settings.Secure.putString(i.targetContext.contentResolver, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "com.jarvis.next/com.jarvis.assistant.tools.JarvisAccessibilityService")
            android.provider.Settings.Secure.putInt(i.targetContext.contentResolver, android.provider.Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        } finally { automation.dropShellPermissionIdentity() }
        val connectionDeadline = System.currentTimeMillis() + 30_000
        while (!JarvisAccessibilityService.connected && System.currentTimeMillis() < connectionDeadline) Thread.sleep(100)
        assertTrue("Accessibility must be connected before live requests", JarvisAccessibilityService.connected)
        Thread.sleep(500)
        val runtime = (i.targetContext.applicationContext as JarvisApplication).runtime
        i.runOnMainSync { runtime.submit("open Jarvis Next Test Controls") }
        val openedDeadline = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < openedDeadline && !runtime.state.value.messages.any { it.text == "Opened Jarvis Next Test Controls." }) Thread.sleep(200)
        assertTrue(runtime.state.value.messages.any { it.text == "Opened Jarvis Next Test Controls." })
        assertNull(runtime.state.value.pendingAction)
        val deadline = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < deadline && !JarvisAccessibilityService.snapshot(i.targetContext).text.orEmpty().contains("Search")) Thread.sleep(200)
        assertTrue("Target screen: " + JarvisAccessibilityService.snapshot(i.targetContext), JarvisAccessibilityService.snapshot(i.targetContext).text.orEmpty().contains("Search"))
        val tap = JarvisAccessibilityService.prepareLive(i.targetContext, LiveRequest.Tap("Search"))
        assertTrue(JarvisAccessibilityService.executeLive(i.targetContext, tap))
        val clickedDeadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < clickedDeadline && !JarvisAccessibilityService.snapshot(i.targetContext).text.orEmpty().contains("Clicked")) Thread.sleep(100)
        assertTrue(JarvisAccessibilityService.snapshot(i.targetContext).text.orEmpty().contains("Clicked"))
        assertTrue(runCatching { JarvisAccessibilityService.executeLive(i.targetContext, tap) }.isFailure)
        val edit = JarvisAccessibilityService.prepareLive(i.targetContext, LiveRequest.Edit("Hello Sofia"))
        assertTrue(JarvisAccessibilityService.executeLive(i.targetContext, edit))
        val editedDeadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < editedDeadline && !JarvisAccessibilityService.snapshot(i.targetContext).text.orEmpty().contains("Hello Sofia")) Thread.sleep(100)
        assertTrue(JarvisAccessibilityService.snapshot(i.targetContext).text.orEmpty().contains("Hello Sofia"))
        assertTrue(runCatching { JarvisAccessibilityService.prepareLive(i.targetContext, LiveRequest.Tap("Send")) }.isFailure)
    }
}
