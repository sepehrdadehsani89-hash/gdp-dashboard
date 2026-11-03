package com.jarvis.assistant

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.jarvis.assistant.core.*
import com.jarvis.assistant.hud.*
import com.jarvis.assistant.omnisystem.HudModule
import org.junit.*
import org.junit.Assert.*

/** Dedicated emulator only. Does not clear data or download models. */
class OmnisystemFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var runtime:AssistantRuntime
    private lateinit var original:AssistantSettings
    private lateinit var hud:HudSettings
    @Before fun prepare(){compose.activityRule.scenario.onActivity{
        runtime=(it.application as JarvisApplication).runtime;runtime.stopEverything()
        original=runtime.store.settings.value;hud=runtime.hud.state.value.settings
        runtime.hud.save(hud.copy(reducedMotion=true,weatherCity=""))
        runtime.store.updateSettings(AssistantSettings(onboardingComplete=true,speakReplies=false,provider="local"))
        runtime.selectModule(HudModule.ASSISTANT)
    };compose.waitUntil(30_000){runtime.state.value.settings.onboardingComplete}}
    @After fun restore(){compose.activityRule.scenario.onActivity{runtime.stopEverything();runtime.selectModule(HudModule.ASSISTANT);runtime.store.updateSettings(original);runtime.hud.save(hud)}}
    @Test fun weatherPanelShowsReceivedForecastAndKeepsMemoriesAndProvider(){
        val memories=runtime.store.memories.value;val provider=runtime.store.settings.value.provider
        compose.runOnUiThread{
            runtime.hud.acceptWeatherReading(WeatherReading("Fixture city",18.0,17.0,70,8.0,45,"06:00","18:00",3,"2026-11-02T10:00",System.currentTimeMillis(),listOf(WeatherHour("2026-11-02T11:00",19.0,40)),listOf(WeatherDay("2026-11-03",22.0,12.0,30))))
            runtime.selectModule(HudModule.WEATHER)
        }
        compose.onNodeWithText("ATMOSPHERIC INTELLIGENCE").assertExists()
        compose.onAllNodes(hasText("Fixture city · 18.0°C")).assertCountEquals(2)
        compose.onNode(hasText("2026-11-03 · 12.0°C / 22.0°C · 30% rain")).performScrollTo().assertIsDisplayed()
        assertEquals(memories,runtime.store.memories.value);assertEquals(provider,runtime.store.settings.value.provider)
        assertFalse(runtime.state.value.audio.recording)
    }
    @Test fun moduleNavigationDoesNotStartCaptureOrFabricateMusic(){
        compose.runOnUiThread{runtime.selectModule(HudModule.MUSIC)}
        compose.onNode(hasText("AudD recognition uses an explicitly approved ten-second sample",substring=true)).assertExists()
        assertFalse(runtime.state.value.audio.recording)
        compose.runOnUiThread{runtime.selectModule(HudModule.CAMERA)}
        compose.onNodeWithText("Open front camera preview").assertExists()
        assertFalse(runtime.state.value.audio.recording)
        compose.runOnUiThread{runtime.selectModule(HudModule.SYSTEM)}
        compose.onAllNodesWithText("SYSTEM FREE").assertCountEquals(0)
        compose.onAllNodesWithText("APP PSS").assertCountEquals(0)
        compose.onNodeWithText("SYSTEM TELEMETRY").assertExists()
    }
    @Test fun batteryModuleUsesSameActualReadingAsHome(){
        compose.waitUntil(20_000){runtime.state.value.hud.telemetry.battery.percent!=null}
        val percent=runtime.state.value.hud.telemetry.battery.percent!!
        compose.runOnUiThread{runtime.selectModule(HudModule.BATTERY)}
        compose.onNodeWithText("BATTERY DIAGNOSTICS").assertExists()
        compose.onAllNodesWithText("$percent%").fetchSemanticsNodes().let{assertTrue(it.isNotEmpty())}
        compose.onNode(hasText("Capacity-health percentage is not measured.",substring=true)).assertExists()
    }
}
