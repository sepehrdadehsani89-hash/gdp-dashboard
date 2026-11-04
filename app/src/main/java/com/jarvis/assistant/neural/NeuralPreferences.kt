package com.jarvis.assistant.neural

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NeuralSettings(val profile:String="lite",val eco:Boolean=true,val saveHistory:Boolean=false,val retentionDays:Int=0,val cloudKnowledge:Boolean=false,val cloudFallbackLocal:Boolean=false,val briefing:Boolean=false,val briefingHour:Int=8,val briefingMinute:Int=0,val briefingWeather:Boolean=true,val briefingNews:Boolean=false,val briefingCalendar:Boolean=false,val briefingTasks:Boolean=true,val newsTopic:String="technology",val quietStart:Int=22,val quietEnd:Int=7)
class NeuralPreferences(context:Context){
    private val prefs=context.getSharedPreferences("jarvis_neural_v4",Context.MODE_PRIVATE)
    private val mutable=MutableStateFlow(read());val state=mutable.asStateFlow()
    private fun read()=NeuralSettings(profile=prefs.getString("profile","lite").orEmpty(),eco=prefs.getBoolean("eco",true),saveHistory=false,retentionDays=prefs.getInt("retention",0),cloudKnowledge=prefs.getBoolean("cloudKnowledge",false),cloudFallbackLocal=prefs.getBoolean("fallback",false),briefing=prefs.getBoolean("briefing",false),briefingHour=prefs.getInt("hour",8),briefingMinute=prefs.getInt("minute",0),briefingWeather=prefs.getBoolean("weather",true),briefingNews=prefs.getBoolean("news",false),briefingCalendar=prefs.getBoolean("calendar",false),briefingTasks=prefs.getBoolean("tasks",true),newsTopic=prefs.getString("topic","technology").orEmpty(),quietStart=prefs.getInt("quietStart",22),quietEnd=prefs.getInt("quietEnd",7))
    fun save(value:NeuralSettings){
        val s=value.copy(saveHistory=false,profile=BrainProfiles.get(value.profile).id,retentionDays=value.retentionDays.takeIf{it in setOf(0,1,7,30)} ?: 0,briefingHour=value.briefingHour.coerceIn(0,23),briefingMinute=value.briefingMinute.coerceIn(0,59),quietStart=value.quietStart.coerceIn(0,23),quietEnd=value.quietEnd.coerceIn(0,23),newsTopic=value.newsTopic.trim().take(80))
        prefs.edit().putString("profile",s.profile).putBoolean("eco",s.eco).putBoolean("history",s.saveHistory).putInt("retention",s.retentionDays).putBoolean("cloudKnowledge",s.cloudKnowledge).putBoolean("fallback",s.cloudFallbackLocal).putBoolean("briefing",s.briefing).putInt("hour",s.briefingHour).putInt("minute",s.briefingMinute).putBoolean("weather",s.briefingWeather).putBoolean("news",s.briefingNews).putBoolean("calendar",s.briefingCalendar).putBoolean("tasks",s.briefingTasks).putString("topic",s.newsTopic).putInt("quietStart",s.quietStart).putInt("quietEnd",s.quietEnd).apply();mutable.value=s
    }
    fun clear(){prefs.edit().clear().apply();mutable.value=NeuralSettings()}
}
