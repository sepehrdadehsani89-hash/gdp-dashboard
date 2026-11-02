package com.jarvis.assistant.hud

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.sqrt

/** Values are absent until Android/the selected provider supplies them. */
data class BatteryReading(val percent: Int? = null, val charging: Boolean = false, val full: Boolean = false,
    val source: String = "Battery", val temperatureC: Float? = null, val health: String = "Unavailable") {
    companion object {
        fun percentage(level: Int, scale: Int): Int? = if (scale > 0 && level in 0..scale) (level.toLong() * 100 / scale).toInt().coerceIn(0,100) else null
    }
}
data class Telemetry(val battery: BatteryReading = BatteryReading(), val epochMillis: Long = System.currentTimeMillis(),
    val timezone: String = ZoneId.systemDefault().id, val system24Hour: Boolean = false, val systemMotion:Boolean=true,
    val network: String = "Unavailable", val internetValidated: Boolean = false, val powerSaver: Boolean = false,
    val systemAvailableBytes: Long? = null, val systemTotalBytes: Long? = null, val appPssKiB: Int? = null)

data class HudSettings(val sentinel:com.jarvis.assistant.sentinel.SentinelSettings = com.jarvis.assistant.sentinel.SentinelSettings(), val weatherCity: String = "", val fahrenheit: Boolean = false, val clockFormat: String = "system",
    val animationProfile: String = "balanced", val reducedMotion: Boolean = false, val autoBatterySaver: Boolean = true,
    val animationIntensity: Float = .7f, val accent: String = "cyan", val soundEffects: Boolean = true,
    val autoDismissSeconds: Int = 0, val codexEnabled: Boolean = true,
    val voiceEngine: String = "auto", val voiceId: String = "", val voicePitch: Float = .9f,
    val voiceRate: Float = .98f, val voiceVolume: Float = .9f)

data class WeatherReading(val place: String, val temperatureC: Double?, val feelsLikeC: Double?, val humidity: Int?,
    val windKmh: Double?, val precipitationProbability: Int?, val sunrise: String?, val sunset: String?,
    val code: Int?, val observationTime: String, val fetchedAt: Long)
data class WeatherState(val reading: WeatherReading? = null, val loading: Boolean = false, val error: String? = null,
    val stale: Boolean = true, val locationNotice: String? = null)
data class HudState(val settings: HudSettings = HudSettings(), val telemetry: Telemetry = Telemetry(), val weather: WeatherState = WeatherState())

object ClockDisplay {
    fun time(epoch: Long, zone: String, locale: Locale, format: String, system24: Boolean): String =
        DateTimeFormatter.ofPattern(if (format == "24" || format == "system" && system24) "HH:mm" else "h:mm a", locale).format(Instant.ofEpochMilli(epoch).atZone(ZoneId.of(zone)))
    fun date(epoch: Long, zone: String, locale: Locale): String = DateTimeFormatter.ofPattern("EEEE · d MMMM yyyy", locale).format(Instant.ofEpochMilli(epoch).atZone(ZoneId.of(zone)))
}
object HudMotion {
    fun fps(settings: HudSettings, battery: BatteryReading, powerSaver: Boolean, systemMotion: Boolean): Int = when {
        settings.reducedMotion || !systemMotion || settings.sentinel.animationSpeed==0f -> 0
        powerSaver || settings.animationProfile == "saver" || settings.autoBatterySaver && (battery.percent ?: 100) < 10 -> 8
        (battery.percent ?: 100) < 20 -> 12
        settings.animationProfile == "cinematic" -> 30
        else -> 20
    }
}
object PcmLevel {
    fun pcm16(bytes: ByteArray, count: Int): Float {
        val n = minOf(count.coerceAtLeast(0), bytes.size) / 2
        if (n == 0) return 0f
        var sum = 0.0
        for (i in 0 until n) { val sample = ((bytes[i*2].toInt() and 255) or (bytes[i*2+1].toInt() shl 8)).toShort().toDouble() / 32768; sum += sample*sample }
        return sqrt(sum/n).toFloat().coerceIn(0f,1f)
    }
    fun floats(samples: FloatArray, start: Int = 0, count: Int = samples.size): Float {
        val end = minOf(samples.size, start+count); if (start < 0 || end <= start) return 0f
        var sum = 0.0; for (i in start until end) { val v=samples[i].takeIf(Float::isFinite)?.coerceIn(-1f,1f) ?: 0f; sum+=v*v }
        return sqrt(sum/(end-start)).toFloat().coerceIn(0f,1f)
    }
}
