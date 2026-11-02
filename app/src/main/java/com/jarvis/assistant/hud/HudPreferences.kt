package com.jarvis.assistant.hud

import android.content.Context

/** Additive preferences: the existing settings, encrypted provider keys and Room schema stay intact. */
class HudPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("jarvis_hud_v2", Context.MODE_PRIVATE)
    fun read() = HudSettings(
        weatherCity=prefs.getString("city", "").orEmpty(), fahrenheit=prefs.getBoolean("fahrenheit",false),
        clockFormat=prefs.getString("clock","system").orEmpty(), animationProfile=prefs.getString("profile","balanced").orEmpty(),
        reducedMotion=prefs.getBoolean("motion",false), autoBatterySaver=prefs.getBoolean("autoSaver",true),
        animationIntensity=prefs.getFloat("intensity",.7f), accent=prefs.getString("accent","cyan").orEmpty(),
        soundEffects=prefs.getBoolean("sounds",true), autoDismissSeconds=prefs.getInt("dismiss",0), codexEnabled=prefs.getBoolean("codex",true),
        voiceEngine=prefs.getString("engine","auto").orEmpty(), voiceId=prefs.getString("voice", "").orEmpty(),
        voicePitch=prefs.getFloat("pitch",.9f), voiceRate=prefs.getFloat("rate",.98f), voiceVolume=prefs.getFloat("volume",.9f))
    fun save(value: HudSettings): HudSettings {
        val s=value.copy(weatherCity=value.weatherCity.trim().take(100), clockFormat=value.clockFormat.takeIf { it in listOf("system","12","24") } ?: "system",
            animationProfile=value.animationProfile.takeIf { it in listOf("cinematic","balanced","saver") } ?: "balanced",
            animationIntensity=value.animationIntensity.coerceIn(0f,1f), autoDismissSeconds=value.autoDismissSeconds.coerceIn(0,600),
            voicePitch=value.voicePitch.coerceIn(.7f,1.3f), voiceRate=value.voiceRate.coerceIn(.7f,1.3f), voiceVolume=value.voiceVolume.coerceIn(0f,1f))
        prefs.edit().putString("city",s.weatherCity).putBoolean("fahrenheit",s.fahrenheit).putString("clock",s.clockFormat)
            .putString("profile",s.animationProfile).putBoolean("motion",s.reducedMotion).putBoolean("autoSaver",s.autoBatterySaver)
            .putFloat("intensity",s.animationIntensity).putString("accent",s.accent).putBoolean("sounds",s.soundEffects)
            .putInt("dismiss",s.autoDismissSeconds).putBoolean("codex",s.codexEnabled).putString("engine",s.voiceEngine)
            .putString("voice",s.voiceId).putFloat("pitch",s.voicePitch).putFloat("rate",s.voiceRate).putFloat("volume",s.voiceVolume).apply()
        return s
    }
}
