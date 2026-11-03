package com.jarvis.assistant

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.jarvis.assistant.voice.CustomVoicePolicy
import com.jarvis.assistant.voice.VoiceSettingsActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class SentinelVoiceSettingsTest {
    @get:Rule val compose=createAndroidComposeRule<VoiceSettingsActivity>()
    @Before fun prepare(){
        compose.activityRule.scenario.onActivity{activity->
            val runtime=(activity.application as JarvisApplication).runtime
            runtime.stopEverything()
            runtime.hud.save(runtime.hud.state.value.settings.copy(reducedMotion=true,weatherCity=""))
            runtime.store.updateSettings(runtime.store.settings.value.copy(language=com.jarvis.assistant.core.AppLanguage.EN,speakReplies=false))
        }
        compose.waitForIdle()
    }
    @Test fun requestedReferenceIsEditableButCannotEnableWithoutRealPreview(){
        compose.onNodeWithText(CustomVoicePolicy.REQUESTED_ID).performScrollTo().assertExists()
        compose.onNodeWithText("Use this voice for replies").performScrollTo().assertIsNotEnabled()
        compose.onAllNodes(hasSetTextAction()).onFirst().performScrollTo().performTextReplacement("another_voice_reference")
        compose.onNodeWithText("another_voice_reference").assertExists()
        compose.onNodeWithText("Use this voice for replies").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Consent and play service preview").performScrollTo().assertIsNotEnabled()
        val runtime=(compose.activity.application as JarvisApplication).runtime
        assertFalse(runtime.customVoice.config().enabled)
        assertFalse(runtime.state.value.audio.recording)
    }
}
