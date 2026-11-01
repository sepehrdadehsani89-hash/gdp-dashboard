package com.jarvis.assistant

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantSettings
import com.jarvis.assistant.core.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Real UI → runtime → Room/tool checks, with no cloud keys or fake provider.
 * Run only on a dedicated test installation: each test resets its app data.
 * No test approves an external action, requests a sensitive permission, or
 * downloads a model. Speech output is disabled in seeded conversation tests.
 */
@RunWith(AndroidJUnit4::class)
class JarvisFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var runtime: AssistantRuntime

    @Before fun resetTestInstallation() {
        compose.activityRule.scenario.onActivity { activity ->
            runtime = (activity.application as JarvisApplication).runtime
            runtime.stopEverything()
        }
        runBlocking(Dispatchers.IO) { runtime.store.clearAll() }
        compose.waitUntil(timeoutMillis = 20_000) {
            !runtime.state.value.settings.onboardingComplete &&
                runtime.state.value.messages.isEmpty() && runtime.state.value.memories.isEmpty()
        }
        compose.waitForIdle()
    }

    @After fun stopAppOwnedWork() {
        if (::runtime.isInitialized) compose.activityRule.scenario.onActivity { runtime.stopEverything() }
    }

    @Test fun firstLaunchCanFinishWithoutAKeyOrOptionalPermission() {
        compose.onNodeWithText("Meet your\nquiet advantage.").assertIsDisplayed()
        compose.onNodeWithText("Meet Jarvis Next").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 15_000) { runtime.state.value.settings.onboardingComplete }
        compose.onNodeWithText("At your service.").assertIsDisplayed()
        assertEquals("local", runtime.state.value.settings.provider)
        assertFalse(runtime.state.value.hasApiKey)
        assertFalse(runtime.state.value.settings.allowNetworkSpeech)
        assertFalse(runtime.state.value.settings.useCloudVoice)
    }

    @Test fun typedTimeQuestionUsesTheActualKeylessTool() {
        seedReady()
        send("What time is it?")
        compose.waitUntil(timeoutMillis = 20_000) {
            runtime.state.value.messages.any { it.role == MessageRole.ASSISTANT && it.text.startsWith("It's ") }
        }
        val answer = runtime.state.value.messages.last { it.role == MessageRole.ASSISTANT }
        assertTrue(answer.text.endsWith("."))
        assertTrue(answer.sources.isEmpty())
        try {
            compose.waitUntil(timeoutMillis = 15_000) { compose.onNodeWithText(answer.text).isDisplayed() }
            compose.onNodeWithText(answer.text).assertIsDisplayed()
        } finally {
            val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            screenshot?.let { bitmap -> File(compose.activity.cacheDir, "time-reply.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle() }
        }
        assertFalse(runtime.state.value.hasApiKey)
    }

    @Test fun explicitMemoryRequestIsPersistedAndVisible() {
        seedReady()
        send("Remember that my favorite color is teal.")
        compose.waitUntil(timeoutMillis = 20_000) {
            runtime.state.value.memories.any { it.text == "my favorite color is teal." }
        }
        compose.onNodeWithText("Memory").performClick()
        compose.onNodeWithText("my favorite color is teal.").assertIsDisplayed()
        assertEquals("my favorite color is teal.", runtime.store.memories.value.single().text)
    }

    @Test fun youtubeProposalWaitsForApprovalAndCanBeCancelled() {
        seedReady()
        send("Open YouTube")
        compose.waitUntil(timeoutMillis = 20_000) { runtime.state.value.pendingAction != null }
        val proposal = runtime.state.value.pendingAction
        assertNotNull(proposal)
        assertEquals("open_url", proposal!!.kind)
        assertEquals("https://www.youtube.com", proposal.payload)
        compose.onNodeWithText("Your approval").assertIsDisplayed()
        compose.onNode(hasText("Open YouTube?") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithText("Confirm").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.waitUntil(timeoutMillis = 10_000) { runtime.state.value.pendingAction == null }
        compose.onNodeWithText("Your approval").assertDoesNotExist()
        // We deliberately never press Confirm: no YouTube/browser launch occurs.
    }

    @Test fun persianSelectionLocalizesHomeAndMemoryNavigation() {
        seedReady(AppLanguage.FA)
        compose.onNodeWithText("در خدمت شما هستم.").assertIsDisplayed()
        compose.onNodeWithText("حافظه").performClick()
        compose.onNodeWithText("جایی برای چیزهای مهم.").assertIsDisplayed()
        assertEquals(AppLanguage.FA, runtime.state.value.settings.language)
    }

    @Test fun privateMemorySpeechStaysOnDeviceWhenCloudVoiceIsEnabled() {
        seedReady()
        val note = "Synthetic private test note, never for a speech provider."
        runBlocking(Dispatchers.IO) { runtime.store.remember(note) }
        compose.runOnUiThread {
            runtime.store.updateSettings(runtime.store.settings.value.copy(speakReplies = true, useCloudVoice = true))
        }
        compose.waitUntil(timeoutMillis = 15_000) {
            runtime.store.memories.value.any { it.text == note } && runtime.state.value.settings.useCloudVoice
        }
        assertTrue(runtime.store.getApiKey("openai").isBlank())
        send("What do you remember?")
        compose.waitUntil(timeoutMillis = 20_000) {
            runtime.state.value.messages.any { it.role == MessageRole.ASSISTANT && it.text.contains(note) } &&
                runtime.lastSpeechDestination != "none"
        }
        assertEquals("on-device", runtime.lastSpeechDestination)
        assertFalse(runtime.state.value.error.orEmpty().contains("Cloud voice"))
    }

    @Test fun explicitLocalChoiceKeepsQuotaSeparateAndDisablesCloudVoice() {
        seedReady()
        compose.runOnUiThread { runtime.store.updateSettings(runtime.store.settings.value.copy(provider = "gemini", model = "gemini-fixture", useCloudVoice = true)) }
        compose.waitUntil(15_000) { runtime.state.value.settings.provider == "gemini" }
        compose.onNodeWithText("Use free on-device AI").performScrollTo().performClick()
        compose.waitUntil(15_000) { runtime.state.value.settings.provider == "local" }
        assertFalse(runtime.state.value.settings.useCloudVoice)
        assertEquals("qwen2.5-0.5b", runtime.state.value.settings.model)
        assertTrue(runtime.state.value.messages.isEmpty())
        assertTrue(runtime.state.value.brainProgress == null)
    }

    private fun seedReady(language: AppLanguage = AppLanguage.EN) {
        compose.runOnUiThread {
            runtime.store.updateSettings(AssistantSettings(
                language = language, provider = "local", speakReplies = false,
                allowNetworkSpeech = false, useCloudVoice = false, onboardingComplete = true,
            ))
        }
        compose.waitUntil(timeoutMillis = 15_000) {
            runtime.state.value.settings.onboardingComplete && runtime.state.value.settings.language == language
        }
        compose.waitForIdle()
    }

    private fun send(text: String) {
        compose.onNode(hasSetTextAction()).performTextInput(text)
        compose.onNode(hasSetTextAction()).performImeAction()
    }
}
