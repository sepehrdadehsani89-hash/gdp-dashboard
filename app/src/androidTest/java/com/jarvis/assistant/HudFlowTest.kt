package com.jarvis.assistant

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.assistant.core.*
import com.jarvis.assistant.hud.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Runs only on the dedicated emulator. Battery overrides are always reset in finally/@After. */
@RunWith(AndroidJUnit4::class)
class HudFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private lateinit var runtime:AssistantRuntime
    private var oldHud=HudSettings()
    private val noteText="HUD categorized note "+java.util.UUID.randomUUID().toString().take(8)
    @Before fun prepare(){
        compose.activityRule.scenario.onActivity {
            runtime=(it.application as JarvisApplication).runtime
            runtime.stopEverything();oldHud=runtime.hud.state.value.settings
            runtime.hud.save(HudSettings(reducedMotion=true))
            runtime.store.updateSettings(AssistantSettings(onboardingComplete=true,speakReplies=false,provider="local"))
        }
        compose.waitUntil(20_000){runtime.state.value.settings.onboardingComplete}
    }
    @After fun restore(){
        shell("dumpsys battery reset")
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.activityRule.scenario.onActivity {runtime.stopEverything();runtime.hud.save(oldHud);runtime.store.updateSettings(AssistantSettings())}
    }
    private fun shell(command:String):String = ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)).bufferedReader().use{it.readText()}

    @Test fun overlappingVisibleActivitiesRetainTelemetryUntilLastOwnerStops(){
        val repo=HudRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        val first=Any();val second=Any()
        try {
            repo.startVisible(first);repo.startVisible(second);repo.stopVisible(first)
            shell("dumpsys battery set level 37")
            compose.waitUntil(20_000){repo.state.value.telemetry.battery.percent==37}
            repo.stopVisible(second)
            shell("dumpsys battery set level 38")
            assertEquals(37,repo.state.value.telemetry.battery.percent)
            repo.startVisible(first)
            compose.waitUntil(20_000){repo.state.value.telemetry.battery.percent==38}
        }finally{repo.stopVisible(first);repo.stopVisible(second);shell("dumpsys battery reset")}
    }

    @Test fun startupBatteryIsRealAndHudCanRenderOnSmallScreen(){
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val sticky=context.registerReceiver(null,android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))!!
        val expected=HudRepository.battery(sticky).percent
        assertNotNull(expected)
        compose.waitUntil(20_000){runtime.state.value.hud.telemetry.battery.percent==expected}
        compose.onNodeWithText("Assistant",useUnmergedTree=true).performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("$expected%"))
        compose.onNodeWithText("$expected%").assertIsDisplayed()
        val image=compose.onRoot().captureToImage().asAndroidBitmap()
        java.io.File(context.cacheDir,"jarvis-hud-tested.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
    }

    @Test fun batteryEventsChargingFullAndForegroundReentryUseSystemValues(){
        try {
            shell("dumpsys battery set ac 0");shell("dumpsys battery set wireless 0")
            shell("dumpsys battery set level 42")
            shell("dumpsys battery set status 2")
            shell("dumpsys battery set usb 1")
            compose.waitUntil(20_000){runtime.state.value.hud.telemetry.battery.let {it.percent==42 && it.charging && it.source=="USB"}}
            compose.onNodeWithText("Assistant",useUnmergedTree=true).performClick()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("42%"))
            compose.onNodeWithText("42%").assertIsDisplayed()
            shell("dumpsys battery set level 100");shell("dumpsys battery set status 5")
            compose.waitUntil(20_000){runtime.state.value.hud.telemetry.battery.let{it.percent==100 && it.full}}
            compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            shell("dumpsys battery set level 9")
            assertEquals(100,runtime.state.value.hud.telemetry.battery.percent)
            compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            compose.waitUntil(20_000){runtime.state.value.hud.telemetry.battery.percent==9}
            assertEquals(0,HudMotion.fps(runtime.hud.state.value.settings,runtime.hud.state.value.telemetry.battery,false,true))
        }finally{shell("dumpsys battery reset")}
    }
    @Test fun navigationSearchExplicitMemorySaveAndCategoryPersist(){
        compose.onNodeWithText("Memory",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Work").performClick()
        compose.onNodeWithText("New memory").performTextInput(noteText)
        compose.onNodeWithText("Save memory").performClick()
        compose.waitUntil(20_000){runtime.state.value.memories.any{it.text==noteText}}
        val id=runtime.state.value.memories.first{it.text==noteText}.id
        compose.waitUntil(10_000){runtime.state.value.memoryCategories[id]=="Work"}
        compose.onNodeWithText("Search memory").performTextInput("HUD categorized")
        shell("input keyevent 4")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(noteText))
        compose.onNodeWithText(noteText).assertIsDisplayed()
        compose.onNodeWithText("Settings",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Reduced motion").assertExists()
        compose.onNodeWithText("Assistant",useUnmergedTree=true).performClick()
        compose.onNodeWithText("J A R V I S").assertExists()
    }
    @Test fun codexCancelNeverExecutesProtectedAction(){
        var executions=0
        compose.runOnUiThread{runtime.protect("Test protected action"){executions++}}
        compose.onNodeWithText("Codex · authorization required").assertIsDisplayed()
        compose.onNodeWithText("Cancel",useUnmergedTree=true).performClick()
        compose.waitUntil(10_000){runtime.state.value.authorization?.status==AuthorizationStatus.DENIED}
        assertEquals(0,executions)
        compose.runOnUiThread{runtime.requestIdentity()}
        assertEquals(0,executions)
    }
    @Test fun rtlAndHudPreferencesSurviveActivityRecreation(){
        compose.runOnUiThread{runtime.setLanguage(AppLanguage.FA);runtime.hud.save(runtime.hud.state.value.settings.copy(clockFormat="24",accent="purple",reducedMotion=true))}
        compose.activityRule.scenario.recreate()
        compose.waitUntil(20_000){runtime.state.value.settings.language==AppLanguage.FA}
        val activityContext=InstrumentationRegistry.getInstrumentation().targetContext
        val read=HudPreferences(activityContext).read()
        assertEquals("24",read.clockFormat);assertTrue(read.reducedMotion);assertEquals("purple",read.accent)
        compose.onNodeWithText("دستیار",useUnmergedTree=true).assertIsDisplayed()
        compose.onNodeWithText("تنظیمات",useUnmergedTree=true).performClick()
        compose.onNodeWithText("HUD · VOICE · PRIVACY").assertExists()
    }
    @Test fun batteryAndWeatherPermissionDenialDoNotCreateMeasurements(){
        val battery=HudRepository.battery(android.content.Intent(android.content.Intent.ACTION_BATTERY_CHANGED))
        assertNull(battery.percent);assertNull(battery.temperatureC)
        compose.runOnUiThread{runtime.hud.refreshWeather(true)}
        compose.waitUntil(25_000){!runtime.state.value.hud.weather.loading}
        val granted=androidx.core.content.ContextCompat.checkSelfPermission(InstrumentationRegistry.getInstrumentation().targetContext,android.Manifest.permission.ACCESS_COARSE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED
        if(!granted){assertTrue(runtime.state.value.hud.weather.error.orEmpty().contains("permission denied"));assertNull(runtime.state.value.hud.weather.reading)}
    }
}
