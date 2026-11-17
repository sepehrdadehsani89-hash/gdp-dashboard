package com.jarvis.assistant.ui

import android.animation.ValueAnimator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jarvis.assistant.core.AssistantPhase
import com.jarvis.assistant.hud.BatteryReading
import com.jarvis.assistant.hud.HudSettings
import com.jarvis.assistant.hud.HudState
import com.jarvis.assistant.hud.Telemetry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Actual Compose/Android pixel and lifecycle fixtures. Requires a real Android render surface.
 * These tests do not verify microphone input, real battery telemetry, or physical-device FPS. */
class HudCoreRenderingTest {
    @get:Rule val compose = createComposeRule()
    private class VisibleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
        init { registry.currentState = Lifecycle.State.RESUMED }
    }
    private var state by mutableStateOf(JarvisUiState(
        hud = HudState(telemetry = Telemetry(battery = BatteryReading(58))),
    ))
    private lateinit var owner: VisibleOwner

    private fun show(phase: AssistantPhase = AssistantPhase.READY, settings: HudSettings = HudSettings()) {
        state = state.copy(phase = phase, hud = state.hud.copy(settings = settings))
        compose.mainClock.autoAdvance = false
        compose.setContent {
            owner = remember { VisibleOwner() }
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                HudCore(state, Modifier.size(320.dp).testTag("core-render-fixture"))
            }
        }
        compose.mainClock.advanceTimeByFrame()
    }
    private fun pixels(): IntArray {
        val bitmap = compose.onNodeWithTag("core-render-fixture").captureToImage().asAndroidBitmap()
        return IntArray(bitmap.width * bitmap.height).also {
            bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        }
    }
    private fun advanceVisibleTime() {
        // The production geometry uses Android's real elapsed clock. Do not fake
        // that passage merely by advancing Compose's virtual frame timestamps.
        Thread.sleep(200)
        compose.mainClock.advanceTimeBy(250)
    }
    private fun assertMoving() {
        val before = pixels(); advanceVisibleTime()
        assertFalse("Consecutive rendered core frames must differ", before.contentEquals(pixels()))
    }
    private fun assertStill() {
        val before = pixels(); advanceVisibleTime()
        assertArrayEquals("Paused motion must preserve the frame", before, pixels())
    }
    private fun requireSystemMotion() {
        assumeTrue("Android animations are disabled; movement case is blocked by the device preference", ValueAnimator.areAnimatorsEnabled())
    }

    @Test fun idleCoreChangesActualRenderedPixels() { requireSystemMotion(); show(); assertMoving() }
    @Test fun listeningCoreChangesActualRenderedPixels() { requireSystemMotion(); show(AssistantPhase.LISTENING); assertMoving() }
    @Test fun processingCoreChangesActualRenderedPixels() { requireSystemMotion(); show(AssistantPhase.THINKING); assertMoving() }
    @Test fun speakingCoreChangesActualRenderedPixels() { requireSystemMotion(); show(AssistantPhase.SPEAKING); assertMoving() }
    @Test fun reducedMotionKeepsTheExistingArtworkStill() { show(settings = HudSettings(reducedMotion = true)); assertStill() }
    @Test fun zeroIntensityKeepsTheExistingArtworkStill() { show(settings = HudSettings(animationIntensity = 0f)); assertStill() }
    @Test fun zeroSpeedKeepsTheExistingArtworkStill() {
        val settings = HudSettings(); show(settings = settings.copy(sentinel = settings.sentinel.copy(animationSpeed = 0f))); assertStill()
    }
    @Test fun lifecycleStopPausesAndResumeRestartsChangingFrames() {
        requireSystemMotion(); show(); assertMoving()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.mainClock.advanceTimeByFrame(); assertStill()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeByFrame(); assertMoving()
    }
    @Test fun restoringTheMotionSettingRestartsTheExistingCore() {
        requireSystemMotion(); show(settings = HudSettings(reducedMotion = true)); assertStill()
        compose.runOnIdle { state = state.copy(hud = state.hud.copy(settings = HudSettings())) }
        compose.mainClock.advanceTimeByFrame(); assertMoving()
    }
    @Test fun speechPhaseTransitionsDoNotLeaveTheCoreStatic() {
        requireSystemMotion(); show(AssistantPhase.LISTENING); assertMoving()
        for (phase in listOf(AssistantPhase.THINKING, AssistantPhase.SPEAKING, AssistantPhase.READY)) {
            compose.runOnIdle { state = state.copy(phase = phase) }
            compose.mainClock.advanceTimeByFrame(); assertMoving()
        }
    }
}
