package com.jarvis.assistant

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jarvis.assistant.hud.BatteryReading
import com.jarvis.assistant.hud.HudSettings
import com.jarvis.assistant.hud.HudState
import com.jarvis.assistant.hud.Telemetry
import com.jarvis.assistant.sentinel.SentinelMode
import com.jarvis.assistant.sentinel.SentinelVisualState
import com.jarvis.assistant.sentinel.WallpaperFrame
import com.jarvis.assistant.sentinel.WallpaperRenderer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android bitmap draws, not handset FPS or WallpaperService lifecycle proof.
 * Runtime execution requires an authorized Android device/emulator. */
@RunWith(AndroidJUnit4::class)
class PhoenixWallpaperFrameTest {
    private fun frame(settings: HudSettings = HudSettings()) = WallpaperFrame(
        hud = HudState(settings = settings, telemetry = Telemetry(
            battery = BatteryReading(percent = 80), epochMillis = 0, timezone = "UTC",
        )),
    )

    private fun pixels(renderer: WallpaperRenderer, frame: WallpaperFrame, at: Long,
                       width: Int = 360, height: Int = 720): IntArray {
        val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return try {
            renderer.draw(Canvas(image), frame, at)
            IntArray(width * height).also { image.getPixels(it, 0, width, 0, 0, width, height) }
        } finally { image.recycle() }
    }

    @Test fun nativeFramesChangeAfterTheRendererResumesFromPause() {
        val renderer = WallpaperRenderer(); val state = frame()
        pixels(renderer, state, 1000); pixels(renderer, state, 1250); renderer.pause()
        val resumed = pixels(renderer, state, 100_000)
        assertFalse(resumed.contentEquals(pixels(renderer, state, 100_250)))
    }

    @Test fun repeatedPauseResumeCyclesDoNotLeaveTheNativeClockFrozen() {
        val renderer = WallpaperRenderer(); val state = frame()
        repeat(4) { cycle ->
            renderer.pause()
            val now = 1000L + cycle * 20_000L
            assertFalse(pixels(renderer, state, now).contentEquals(pixels(renderer, state, now + 250)))
        }
    }

    @Test fun recreatingTheSurfaceSizeKeepsSubsequentFramesMoving() {
        val renderer = WallpaperRenderer(); val state = frame()
        pixels(renderer, state, 1000); renderer.pause()
        val landscape = pixels(renderer, state, 2000, 720, 360)
        assertFalse(landscape.contentEquals(pixels(renderer, state, 2250, 720, 360)))
    }

    @Test fun changingFromListeningToSpeakingDoesNotStopNativeRotation() {
        val renderer = WallpaperRenderer(); val base = frame()
        pixels(renderer, base.copy(visual = SentinelVisualState(mode = SentinelMode.LISTENING)), 1000)
        val speaking = base.copy(visual = SentinelVisualState(mode = SentinelMode.SPEAKING))
        assertFalse(pixels(renderer, speaking, 1250).contentEquals(pixels(renderer, speaking, 1500)))
    }

    @Test fun userReducedMotionStaysStillAndCanBeRestored() {
        val renderer = WallpaperRenderer(); val still = frame(HudSettings(reducedMotion = true))
        assertArrayEquals(pixels(renderer, still, 1000), pixels(renderer, still, 1250))
        val moving = frame()
        assertFalse(pixels(renderer, moving, 1500).contentEquals(pixels(renderer, moving, 1750)))
    }

    @Test fun systemMotionRestorationResumesNativeFrames() {
        val renderer = WallpaperRenderer(); val base = frame()
        val still = base.copy(hud = base.hud.copy(telemetry = base.hud.telemetry.copy(systemMotion = false)))
        assertArrayEquals(pixels(renderer, still, 1000), pixels(renderer, still, 1250))
        assertFalse(pixels(renderer, base, 1500).contentEquals(pixels(renderer, base, 1750)))
    }
}
