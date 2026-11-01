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

class LiveFixtureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(Button(this@LiveFixtureActivity).apply { text = "Search"; setOnClickListener { text = "Clicked" } })
            addView(EditText(this@LiveFixtureActivity).apply { hint = "Draft"; setText("Original draft"); requestFocus() })
            addView(Button(this@LiveFixtureActivity).apply { text = "Send" })
        })
    }
}

@RunWith(AndroidJUnit4::class)
class LiveControlFlowTest {
    @Test fun currentTargetTapDraftAndStaleTargetGuard() {
        val i = InstrumentationRegistry.getInstrumentation()
        i.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).executeShellCommand("settings put secure enabled_accessibility_services com.jarvis.next/com.jarvis.assistant.tools.JarvisAccessibilityService").close()
        i.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).executeShellCommand("settings put secure accessibility_enabled 1").close()
        i.targetContext.startActivity(Intent(i.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Thread.sleep(500)
        val runtime = (i.targetContext.applicationContext as JarvisApplication).runtime
        i.runOnMainSync { runtime.submit("open Jarvis Next Test Controls") }
        val openedDeadline = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < openedDeadline && !runtime.state.value.messages.any { it.text == "Opened Jarvis Next Test Controls." }) Thread.sleep(200)
        assertTrue(runtime.state.value.messages.any { it.text == "Opened Jarvis Next Test Controls." })
        assertNull(runtime.state.value.pendingAction)
        val deadline = System.currentTimeMillis() + 20_000
        while (System.currentTimeMillis() < deadline && !JarvisAccessibilityService.snapshot(i.targetContext).text.orEmpty().contains("Search")) Thread.sleep(200)
        assertTrue(JarvisAccessibilityService.connected)
        val tap = JarvisAccessibilityService.prepareLive(i.targetContext, LiveRequest.Tap("Search"))
        assertTrue(JarvisAccessibilityService.executeLive(i.targetContext, tap))
        Thread.sleep(300)
        assertTrue(runCatching { JarvisAccessibilityService.executeLive(i.targetContext, tap) }.isFailure)
        val edit = JarvisAccessibilityService.prepareLive(i.targetContext, LiveRequest.Edit("Hello Sofia"))
        assertTrue(JarvisAccessibilityService.executeLive(i.targetContext, edit))
        Thread.sleep(300)
        assertTrue(JarvisAccessibilityService.snapshot(i.targetContext).text.orEmpty().contains("Hello Sofia"))
        assertTrue(runCatching { JarvisAccessibilityService.prepareLive(i.targetContext, LiveRequest.Tap("Send")) }.isFailure)
    }
}
