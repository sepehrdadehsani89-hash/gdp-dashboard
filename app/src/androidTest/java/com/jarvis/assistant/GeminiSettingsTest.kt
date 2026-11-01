package com.jarvis.assistant

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.jarvis.assistant.core.AssistantSettings
import com.jarvis.assistant.core.ConnectionCheck
import com.jarvis.assistant.data.ProviderException
import com.jarvis.assistant.ui.JarvisAppUi
import com.jarvis.assistant.ui.JarvisUiActions
import com.jarvis.assistant.ui.JarvisUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.awaitCancellation

/** Settings interaction fixtures: no Google account, network calls, or real keys. */
class GeminiSettingsTest {
    @get:Rule val compose = createComposeRule()
    private val settings = AssistantSettings(provider = "gemini", model = "gemini-unknown-live", onboardingComplete = true)

    @Test fun compatibleModelCanBeSelectedTestedAndSaved() {
        var saved: AssistantSettings? = null
        var testedModel = ""
        compose.setContent {
            JarvisAppUi(JarvisUiState(settings = settings, hasApiKey = true), JarvisUiActions(
                onListGeminiModels = { listOf("gemini-text-fixture") },
                onTestProvider = { selected, _ -> testedModel = selected.model; ConnectionCheck(selected.model, "Hello from the test fixture", 125) },
                onSaveSettings = { selected, _ -> saved = selected },
            ))
        }
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Find Gemini models").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("gemini-text-fixture").performScrollTo().performClick()
        compose.onNodeWithText("Test connection").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Connection verified").assertIsDisplayed()
        compose.onNodeWithText("Connect & save").performClick()
        compose.runOnIdle {
            assertEquals("gemini-text-fixture", testedModel)
            assertEquals("gemini-text-fixture", saved?.model)
        }
    }

    @Test fun failedTestIsShownWithoutClaimingConnectionSuccess() {
        var tested = false
        compose.setContent {
            JarvisAppUi(JarvisUiState(settings = settings, hasApiKey = true), JarvisUiActions(
                onTestProvider = { _, _ -> tested = true; throw ProviderException("Model unavailable (HTTP 404; NOT_FOUND)") },
            ))
        }
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Test connection").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Connection not verified").assertIsDisplayed()
        compose.onNode(hasText("Model unavailable (HTTP 404; NOT_FOUND)") and hasAnyAncestor(isDialog())).assertIsDisplayed()
        compose.onNodeWithText("Connection verified", substring = true).assertDoesNotExist()
        compose.runOnIdle { assertTrue(tested) }
    }

    @Test fun stalledCheckCanBeCancelledWithoutClaimingSuccess() {
        var cancelled = false
        compose.setContent {
            JarvisAppUi(JarvisUiState(settings = settings, hasApiKey = true), JarvisUiActions(
                onTestProvider = { _, _ -> try { awaitCancellation() } finally { cancelled = true } },
            ))
        }
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Test connection").performScrollTo().performClick()
        compose.onNodeWithText("Checking provider…").assertIsDisplayed()
        compose.onNodeWithText("Cancel check").performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(cancelled) }
        compose.onNodeWithText("Connection verified").assertDoesNotExist()
    }
}
