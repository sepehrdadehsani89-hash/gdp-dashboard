package com.jarvis.assistant

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jarvis.assistant.hud.BatteryReading
import com.jarvis.assistant.hud.HudSettings
import com.jarvis.assistant.hud.HudState
import com.jarvis.assistant.hud.Telemetry
import com.jarvis.assistant.power.PowerCondition
import com.jarvis.assistant.power.PowerReading
import com.jarvis.assistant.sentinel.SentinelSettings
import com.jarvis.assistant.sentinel.WallpaperFrame
import com.jarvis.assistant.sentinel.WallpaperRenderer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Android bitmap evidence only when executed; no physical charger/FPS claim. */
@RunWith(AndroidJUnit4::class)
class PhoenixChargingParticleRenderTest {
    private fun greenLowerPixels(condition: PowerCondition, percent: Int, connected: Boolean,
                                 reducedMotion: Boolean = false): Int {
        val battery = BatteryReading(percent, charging = condition == PowerCondition.CHARGING)
        val frame = WallpaperFrame(hud = HudState(
            settings = HudSettings(reducedMotion = reducedMotion,
                sentinel = SentinelSettings(wallpaperClock = false, wallpaperBattery = false)),
            telemetry = Telemetry(battery = battery, epochMillis = 0, timezone = "UTC"),
            power = PowerReading(battery, condition, connected),
        ))
        val image = Bitmap.createBitmap(360, 720, Bitmap.Config.ARGB_8888)
        return try {
            val renderer = WallpaperRenderer(); val canvas = Canvas(image)
            renderer.draw(canvas, frame, 1000); renderer.draw(canvas, frame, 1250)
            // Below the old rings, battery strip and clock: transport particles only.
            val pixels = IntArray(360 * 120); image.getPixels(pixels, 0, 360, 0, 600, 360, 120)
            pixels.count { Color.green(it) >= 20 && Color.green(it) > Color.red(it) * 1.3f && Color.green(it) > Color.blue(it) * 1.15f }
        } finally { image.recycle() }
    }

    @Test fun actualChargingStateDrawsGreenTransportBelowTheCore() {
        assertTrue(greenLowerPixels(PowerCondition.CHARGING, 64, true) > 0)
    }
    @Test fun unpluggedStateHasNoIncomingEnergyParticles() {
        assertEquals(0, greenLowerPixels(PowerCondition.DISCHARGING, 64, false))
    }
    @Test fun verifiedFullStateHasNoIncomingEnergyParticles() {
        assertEquals(0, greenLowerPixels(PowerCondition.FULL, 100, true))
    }
    @Test fun reducedMotionKeepsOnlyTheExistingStaticChargingIndicator() {
        assertEquals(0, greenLowerPixels(PowerCondition.CHARGING, 64, true, reducedMotion = true))
    }
}
