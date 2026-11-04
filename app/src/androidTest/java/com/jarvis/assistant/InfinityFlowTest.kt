package com.jarvis.assistant

import android.content.Intent
import android.os.BatteryManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.assistant.core.*
import com.jarvis.assistant.infinity.*
import com.jarvis.assistant.neural.*
import com.jarvis.assistant.voice.WakeListenerService
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.UUID

class InfinityFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var runtime:AssistantRuntime
    private lateinit var original:AssistantSettings
    private lateinit var neural:NeuralSettings
    @Before fun prepare(){compose.activityRule.scenario.onActivity{runtime=(it.application as JarvisApplication).runtime;runtime.stopEverything();original=runtime.store.settings.value;neural=runtime.neuralPreferences.state.value;runtime.store.updateSettings(AssistantSettings(onboardingComplete=true,speakReplies=false));runtime.setLanguage(AppLanguage.EN)};compose.waitUntil(30_000){runtime.state.value.settings.onboardingComplete}}
    @After fun restore(){compose.activityRule.scenario.onActivity{runtime.stopEverything();runtime.store.updateSettings(original);runtime.neuralPreferences.save(neural)}}
    private fun launch(cls:Class<*>){compose.runOnUiThread{compose.activity.startActivity(Intent(compose.activity,cls))}}
    private fun modelFiles()=File(context.filesDir,"local-brain").walkTopDown().filter{it.isFile}.associate{it.relativeTo(context.filesDir).path to it.length()}
    @Test fun optionalOneGigabyteBrainIsVisibleWithoutDownloadingOrSwitchingProviders(){
        val before=modelFiles();val profile=runtime.neuralPreferences.state.value.profile;val provider=runtime.store.settings.value.provider
        launch(NeuralSettingsActivity::class.java)
        compose.waitUntil(30_000){runCatching{compose.onAllNodesWithText("JARVIS QUANTUM CORE 10.9 · BETA").fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)}
        compose.onNodeWithText("Install optional near-1 GB brain").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Inspect actual size & license before download").performScrollTo().assertIsDisplayed()
        assertEquals(before,modelFiles());assertEquals(profile,runtime.neuralPreferences.state.value.profile);assertEquals(provider,runtime.store.settings.value.provider);assertFalse(WakeListenerService.state.value.running)
        compose.onNodeWithText("Back to Jarvis").performScrollTo().performClick()
    }
    @Test fun unavailableBrainCannotDisplaceTheSelectedProfileOrMemories(){
        val before=modelFiles();val profile=runtime.neuralPreferences.state.value.profile;val memories=runtime.store.memories.value
        val failure=runCatching{runBlocking{runtime.brain.select("balanced")}}.exceptionOrNull()
        assertTrue(failure is IllegalStateException);assertEquals(profile,runtime.neuralPreferences.state.value.profile);assertEquals(memories,runtime.store.memories.value);assertEquals(before,modelFiles());assertFalse(WakeListenerService.state.value.running)
    }
    @Test fun deviceControlsReadActualBatteryAndNetworkWithoutStartingTheMicrophone(){
        launch(InfinityControlsActivity::class.java)
        compose.waitUntil(30_000){runCatching{compose.onAllNodesWithText("INFINITY DEVICE CONTROLS").fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)}
        compose.onNodeWithText("Read battery health & charging").performScrollTo().performClick()
        val actual=context.getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        compose.waitUntil(15_000){runCatching{compose.onAllNodes(hasText("Android health classification:",substring=true)).fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)}
        if(actual in 0..100)compose.onNode(hasText("Battery: $actual%",substring=true)).assertExists()
        compose.onNodeWithText("Read Wi-Fi status").performScrollTo().performClick()
        compose.waitUntil(15_000){runCatching{compose.onAllNodes(hasText("Active transport:",substring=true) or hasText("No active Android network",substring=true)).fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)}
        assertFalse(WakeListenerService.state.value.running)
        compose.onNodeWithText("Back to Jarvis").performScrollTo().performClick()
    }
    @Test fun savedSitesAreExplicitEditableAndOnlyPrepareReviewedOpening(){
        val prefs=InfinityPreferences(context);val name="fixture ${UUID.randomUUID().toString().take(8)}";val tools=InfinityTools(context)
        try{
            prefs.saveWebsite(SavedWebsite(name,"https://example.com/approved","User-approved test reference"))
            prefs.saveWebsite(SavedWebsite(name,"https://example.com/updated","Edited"));assertEquals(1,prefs.websites().count{it.name==name})
            var action="";var target=""
            val reply=runBlocking{tools.route("open $name",AppLanguage.EN){description,kind,payload->action=kind;target=payload;AssistantReply(description)}}
            assertNotNull(reply);assertEquals("open_url",action);assertEquals("https://example.com/updated",target)
            assertTrue(runCatching{prefs.saveWebsite(SavedWebsite("unsafe","http://example.com",""))}.isFailure)
            assertTrue(runCatching{prefs.saveWebsite(SavedWebsite("unsafe","https://user:secret@example.com",""))}.isFailure)
            assertFalse(WakeListenerService.state.value.running)
        }finally{prefs.removeWebsite(name);prefs.removeWebsite("unsafe");tools.clearContext()}
        assertFalse(prefs.websites().any{it.name==name})
    }
}
