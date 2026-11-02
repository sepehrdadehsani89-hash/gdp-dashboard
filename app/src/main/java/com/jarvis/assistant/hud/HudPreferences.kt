package com.jarvis.assistant.hud

import android.content.Context

/** Additive preferences: the existing settings, encrypted provider keys and Room schema stay intact. */
class HudPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("jarvis_hud_v2", Context.MODE_PRIVATE)
    fun read() = HudSettings(
        sentinel=com.jarvis.assistant.sentinel.SentinelSettings(
            reactorScale=prefs.getFloat("sentinel.scale",1f),animationSpeed=prefs.getFloat("sentinel.speed",1f),particleDensity=prefs.getFloat("sentinel.particles",.45f),
            detail=prefs.getString("sentinel.detail","balanced").orEmpty(),brightness=prefs.getFloat("sentinel.brightness",.65f),ambientGlow=prefs.getFloat("sentinel.glow",.45f),
            cinematicWallpaper=prefs.getBoolean("sentinel.cinematic",false),wallpaperClock=prefs.getBoolean("sentinel.clock",true),wallpaperBattery=prefs.getBoolean("sentinel.battery",true),
            wallpaperWeather=prefs.getBoolean("sentinel.weather",false),chargingEffects=prefs.getBoolean("sentinel.charging",true),effectsVolume=prefs.getFloat("sentinel.volume",.35f),
            activationVolume=prefs.getFloat("sentinel.activationVolume",prefs.getFloat("sentinel.volume",.35f)),activationSound=prefs.getString("sentinel.activation","chime").orEmpty(),shutdownSound=prefs.getString("sentinel.shutdown","sweep").orEmpty(),conciseReplies=prefs.getBoolean("sentinel.concise",true),returnOnTimeout=prefs.getBoolean("sentinel.return",true)).sanitized(),
        weatherCity=prefs.getString("city", "").orEmpty(), fahrenheit=prefs.getBoolean("fahrenheit",false),
        clockFormat=prefs.getString("clock","system").orEmpty(), animationProfile=prefs.getString("profile","balanced").orEmpty(),
        reducedMotion=prefs.getBoolean("motion",false), autoBatterySaver=prefs.getBoolean("autoSaver",true),
        animationIntensity=prefs.getFloat("intensity",.7f), accent=prefs.getString("accent","cyan").orEmpty(),
        soundEffects=prefs.getBoolean("sounds",true), autoDismissSeconds=prefs.getInt("dismiss",0), codexEnabled=prefs.getBoolean("codex",true),
        voiceEngine=prefs.getString("engine","auto").orEmpty(), voiceId=prefs.getString("voice", "").orEmpty(),
        voicePitch=prefs.getFloat("pitch",.9f), voiceRate=prefs.getFloat("rate",.98f), voiceVolume=prefs.getFloat("volume",.9f))
    fun save(value: HudSettings): HudSettings {
        val s=value.copy(sentinel=value.sentinel.sanitized(), accent=value.accent.takeIf{it in listOf("cyan","blue","purple")} ?: "cyan", weatherCity=value.weatherCity.trim().take(100), clockFormat=value.clockFormat.takeIf { it in listOf("system","12","24") } ?: "system",
            animationProfile=value.animationProfile.takeIf { it in listOf("cinematic","balanced","saver") } ?: "balanced",
            animationIntensity=value.animationIntensity.coerceIn(0f,1f), autoDismissSeconds=value.autoDismissSeconds.coerceIn(0,600),
            voicePitch=value.voicePitch.coerceIn(.7f,1.3f), voiceRate=value.voiceRate.coerceIn(.7f,1.3f), voiceVolume=value.voiceVolume.coerceIn(0f,1f))
        val w=s.sentinel
        prefs.edit().putFloat("sentinel.scale",w.reactorScale).putFloat("sentinel.speed",w.animationSpeed).putFloat("sentinel.particles",w.particleDensity)
            .putString("sentinel.detail",w.detail).putFloat("sentinel.brightness",w.brightness).putFloat("sentinel.glow",w.ambientGlow).putBoolean("sentinel.cinematic",w.cinematicWallpaper)
            .putBoolean("sentinel.clock",w.wallpaperClock).putBoolean("sentinel.battery",w.wallpaperBattery).putBoolean("sentinel.weather",w.wallpaperWeather).putBoolean("sentinel.charging",w.chargingEffects)
            .putFloat("sentinel.volume",w.effectsVolume).putFloat("sentinel.activationVolume",w.activationVolume).putString("sentinel.activation",w.activationSound).putString("sentinel.shutdown",w.shutdownSound).putBoolean("sentinel.concise",w.conciseReplies).putBoolean("sentinel.return",w.returnOnTimeout)
            .putString("city",s.weatherCity).putBoolean("fahrenheit",s.fahrenheit).putString("clock",s.clockFormat)
            .putString("profile",s.animationProfile).putBoolean("motion",s.reducedMotion).putBoolean("autoSaver",s.autoBatterySaver)
            .putFloat("intensity",s.animationIntensity).putString("accent",s.accent).putBoolean("sounds",s.soundEffects)
            .putInt("dismiss",s.autoDismissSeconds).putBoolean("codex",s.codexEnabled).putString("engine",s.voiceEngine)
            .putString("voice",s.voiceId).putFloat("pitch",s.voicePitch).putFloat("rate",s.voiceRate).putFloat("volume",s.voiceVolume).apply()
        return s
    }
}
