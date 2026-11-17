package com.jarvis.assistant.settings

import android.app.KeyguardManager
import android.content.Context
import com.jarvis.assistant.AssistantRuntime
import com.jarvis.assistant.core.*
import com.jarvis.assistant.titan.VoicePreferences
import com.jarvis.assistant.voice.CustomVoice
import org.json.JSONObject

/** Writes the same stores used by visible controls; cloud models never execute setting names. */
class SpokenSettingsController(private val context:Context,private val runtime:AssistantRuntime,private val output:CustomVoice){
    private val voice get()=VoicePreferences(context)
    private fun unlocked(){check(context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked==false){"Unlock Android before changing Jarvis settings."}}
    fun read(id:String):String {
        val h=runtime.hud.state.value.settings;val w=h.sentinel;val v=voice;val s=runtime.store.settings.value
        return when(id){
            "reduced_motion"->h.reducedMotion;"sound_effects"->h.soundEffects;"charging_effects"->w.chargingEffects;"wallpaper_animation"->w.cinematicWallpaper
            "wallpaper_clock"->w.wallpaperClock;"wallpaper_battery"->w.wallpaperBattery;"wallpaper_weather"->w.wallpaperWeather;"battery_adaptation"->h.autoBatterySaver
            "captions"->runtime.phantomPreferences.state.value.captions;"continuous"->v.continuousConversation;"noise_reduction"->v.noiseReduction;"echo_cancellation"->v.echoCancellation;"automatic_gain"->v.gainControl
            "spoken_replies"->s.speakReplies;"locked_chat"->s.allowLockedChat;"private_voice"->output.config(s.language).privateAllowed
            "wake_listening"->com.jarvis.assistant.voice.WakeListenerService.state.value.running
            "intensity"->h.animationIntensity;"particles"->w.particleDensity;"glow"->w.ambientGlow;"core_brightness"->w.brightness;"core_speed"->w.animationSpeed
            "voice_volume"->h.voiceVolume;"voice_rate"->h.voiceRate;"voice_pitch"->h.voicePitch
            "cooldown"->v.speechCooldownMs;"endpoint"->v.endpointPauseMs;"caption_linger"->v.captionLingerMs
            "language"->s.language.code;"accent"->h.accent;"profile"->h.animationProfile;"clock"->h.clockFormat;"sensitivity"->v.sensitivity.lowercase(java.util.Locale.ROOT)
            "brain"->runtime.brainManager.currentConfig().let{"${it.provider}, ${it.mode}, ${it.model}"};"wake_phrase"->v.customWakePhrase;"name"->s.name;"weather_city"->h.weatherCity
            else->error("Unsupported setting")
        }.toString()
    }
    fun route(text:String):AssistantReply? {
        val request=try{SpokenSettings.parse(text)}catch(_:IllegalArgumentException){return AssistantReply("That setting value is outside its supported range. No setting changed.",allowCloudSpeech=true)}catch(_:IllegalStateException){return AssistantReply("That setting value is unsupported. No setting changed. Ask for voice settings help.",allowCloudSpeech=true)} ?: return null
        unlocked()
        return when(request){
            SpokenSettingRequest.PrivateCredential->AssistantReply("Enter provider credentials in the private AI Brain Manager. No credential was saved or sent to an AI model.",allowCloudSpeech=true)
            SpokenSettingRequest.Help->AssistantReply("You can change motion, sound, captions, voice volume and speed, microphone sensitivity, language, and other listed Jarvis settings by voice. Say set reduced motion to off, set voice volume to fifty percent, or use ChatGPT. Privacy changes require confirmation and provider changes require verified inference.",allowCloudSpeech=true)
            is SpokenSettingRequest.Query->{val value=read(request.setting.id);AssistantReply("${request.setting.label}: $value.",allowCloudSpeech=request.setting.type!=SpokenValueType.TEXT)}
            is SpokenSettingRequest.Edit->{
                if(request.setting.confirmation){
                    val explanation=when(request.setting.id){"private_voice"->if(request.value=="false")"Disable private replies through Fish Audio? Public replies may still use the configured voice. Say confirm or cancel." else "Allow the configured Fish Audio provider to receive private spoken replies? This can include contact names, memories and selected private content. Say confirm or cancel."
                        "locked_chat"->"Change general conversation while Android is locked? Private actions still require Android authentication. Say confirm or cancel."
                        "wake_listening"->"Change wake listening? Enabling it uses a visible microphone foreground notification and requires Android microphone permission. It will pause during replies. Say confirm or cancel."
                        else->"Change the primary AI brain? Its exact model and account must already have a verified inference test. Say confirm or cancel."}
                    val payload=JSONObject().put("setting",request.setting.id).put("value",request.value).put("before",read(request.setting.id)).toString()
                    AssistantReply(explanation,action=PendingAction(description=explanation,kind="spoken_setting",payload=payload),allowCloudSpeech=true)
                }else apply(request)
            }
        }
    }
    fun requiresIdentity(action:PendingAction)=JSONObject(action.payload).getString("setting") in setOf("private_voice","locked_chat","brain")
    fun applyProposal(action:PendingAction):AssistantReply {
        unlocked();require(action.kind=="spoken_setting" && System.currentTimeMillis()-action.createdAt in 0..120000){"The confirmation expired. Ask again."}
        val data=JSONObject(action.payload);val spec=SpokenSettings.all.single{it.id==data.getString("setting")}
        require(data.getString("before")==read(spec.id)){"This setting changed while waiting. Ask again."}
        return apply(SpokenSettingRequest.Edit(spec,SpokenSettings.validate(spec,data.getString("value"))))
    }
    private fun apply(edit:SpokenSettingRequest.Edit):AssistantReply {
        unlocked();val spec=edit.setting;val value=SpokenSettings.validate(spec,edit.value)
        val h=runtime.hud.state.value.settings;val w=h.sentinel;val s=runtime.store.settings.value;val v=voice
        val on=value=="true";val n=value.toFloatOrNull() ?: 0f
        when(spec.id){
            "reduced_motion"->runtime.saveHud(h.copy(reducedMotion=on));"sound_effects"->runtime.saveHud(h.copy(soundEffects=on));"charging_effects"->runtime.saveHud(h.copy(sentinel=w.copy(chargingEffects=on)))
            "wallpaper_animation"->runtime.saveHud(h.copy(sentinel=w.copy(cinematicWallpaper=on)));"wallpaper_clock"->runtime.saveHud(h.copy(sentinel=w.copy(wallpaperClock=on)))
            "wallpaper_battery"->runtime.saveHud(h.copy(sentinel=w.copy(wallpaperBattery=on)));"wallpaper_weather"->runtime.saveHud(h.copy(sentinel=w.copy(wallpaperWeather=on)))
            "battery_adaptation"->runtime.saveHud(h.copy(autoBatterySaver=on));"captions"->runtime.phantomPreferences.save(runtime.phantomPreferences.state.value.copy(captions=on))
            "continuous"->v.continuousConversation=on;"noise_reduction"->v.noiseReduction=on;"echo_cancellation"->v.echoCancellation=on;"automatic_gain"->v.gainControl=on
            "spoken_replies"->runtime.store.updateSettings(s.copy(speakReplies=on));"locked_chat"->runtime.store.updateSettings(s.copy(allowLockedChat=on))
            "private_voice"->{val c=output.config(s.language);require(c.enabled && c.key.isNotBlank()){"Configure and preview your output voice first."};output.save(com.jarvis.assistant.voice.CustomVoiceConfig(c.id,c.model,c.key,c.enabled,on),s.language)}
            "wake_listening"->{val detail=if(on)runtime.requestWakeFromVoice() else {runtime.stopWake();"Wake listening stop requested."};return AssistantReply(detail,allowCloudSpeech=true)}
            "intensity"->runtime.saveHud(h.copy(animationIntensity=n));"particles"->runtime.saveHud(h.copy(sentinel=w.copy(particleDensity=n)));"glow"->runtime.saveHud(h.copy(sentinel=w.copy(ambientGlow=n)))
            "core_brightness"->runtime.saveHud(h.copy(sentinel=w.copy(brightness=n)));"core_speed"->runtime.saveHud(h.copy(sentinel=w.copy(animationSpeed=n)))
            "voice_volume"->runtime.saveHud(h.copy(voiceVolume=n));"voice_rate"->runtime.saveHud(h.copy(voiceRate=n));"voice_pitch"->runtime.saveHud(h.copy(voicePitch=n))
            "cooldown"->v.speechCooldownMs=value.toDouble().toLong();"endpoint"->v.endpointPauseMs=value.toDouble().toLong();"caption_linger"->v.captionLingerMs=value.toDouble().toLong()
            "language"->runtime.setLanguage(AppLanguage.fromCode(when(value){"english"->"en";"spanish"->"es";"persian"->"fa";else->value}))
            "accent"->runtime.saveHud(h.copy(accent=value));"profile"->runtime.saveHud(h.copy(animationProfile=value));"clock"->runtime.saveHud(h.copy(clockFormat=value.removeSuffix(" hour")))
            "sensitivity"->v.sensitivity=value.uppercase(java.util.Locale.ROOT);"wake_phrase"->v.customWakePhrase=value
            "name"->runtime.store.updateSettings(s.copy(name=value.take(80)));"weather_city"->runtime.saveHud(h.copy(weatherCity=value))
            "brain"->{runtime.switchBrainFromVoice(value);return AssistantReply("Checking the saved verified connection for your requested brain. No unverified account or model will be selected.",allowCloudSpeech=true)}
            else->error("Unsupported setting")
        }
        val actual=read(spec.id)
        val detail=if(spec.id=="reduced_motion")com.jarvis.assistant.hud.MotionSettingsPolicy.description(on,runtime.hud.state.value.telemetry.systemMotion,runtime.hud.state.value.telemetry.powerSaver,runtime.state.value.performanceRestricted) else if(spec.type==SpokenValueType.TEXT)"${spec.label} saved." else "${spec.label}: $actual."
        return AssistantReply(detail,allowCloudSpeech=true)
    }
}
