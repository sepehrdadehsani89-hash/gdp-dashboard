package com.jarvis.assistant.titan

import android.content.Context
import com.jarvis.assistant.core.AppLanguage
import kotlin.math.*

class VoicePreferences(context:Context){
    private val p=context.getSharedPreferences("jarvis_titan_voice_v1",Context.MODE_PRIVATE)
    var sensitivity:String get()=p.getString("sensitivity","BALANCED").orEmpty().takeIf{it in listOf("LOW","BALANCED","HIGH","CUSTOM")} ?: "BALANCED";set(v){require(v in listOf("LOW","BALANCED","HIGH","CUSTOM"));p.edit().putString("sensitivity",v).apply()}
    var customEnergy:Float get()=p.getFloat("energy",.015f).coerceIn(.005f,.08f);set(v){p.edit().putFloat("energy",v.coerceIn(.005f,.08f)).apply()}
    var customConfidence:Float get()=p.getFloat("confidence",.55f).coerceIn(.45f,.9f);set(v){p.edit().putFloat("confidence",v.coerceIn(.45f,.9f)).apply()}
    val energy get()=when(sensitivity){"LOW"->.03f;"HIGH"->.008f;"CUSTOM"->customEnergy;else->.015f}
    val confidence get()=when(sensitivity){"LOW"->.65f;"HIGH"->.5f;"CUSTOM"->customConfidence;else->.55f}
    var noiseReduction:Boolean get()=p.getBoolean("ns",true);set(v){p.edit().putBoolean("ns",v).apply()}
    var echoCancellation:Boolean get()=p.getBoolean("aec",true);set(v){p.edit().putBoolean("aec",v).apply()}
    var gainControl:Boolean get()=p.getBoolean("agc",false);set(v){p.edit().putBoolean("agc",v).apply()}
    var playbackInterruption:Boolean get()=p.getBoolean("playback_interruption",true);set(v){p.edit().putBoolean("playback_interruption",v).apply()}
    var timeAwareGreeting:Boolean get()=p.getBoolean("time_aware_greeting",false);set(v){p.edit().putBoolean("time_aware_greeting",v).apply()}
    var conversationFocus:Boolean get()=p.getBoolean("conversation_focus",true);set(v){p.edit().putBoolean("conversation_focus",v).apply()}
    var customWakePhrase:String get()=p.getString("custom_wake_phrase","").orEmpty();set(v){require(v.isBlank() || com.jarvis.assistant.voice.WakePhraseMatcher.validCustomPhrase(v));p.edit().putString("custom_wake_phrase",v.trim()).apply()}
    var selectedLanguageMode:Boolean get()=p.getBoolean("selected_language_mode",false);set(v){p.edit().putBoolean("selected_language_mode",v).apply()}
    var firstSpeakerModeApplied:Boolean get()=p.getBoolean("first_speaker_mode_applied",false);set(v){check(p.edit().putBoolean("first_speaker_mode_applied",v).commit())}
    var morning:Int get()=p.getInt("morning",5);set(v){p.edit().putInt("morning",v).apply()}
    var afternoon:Int get()=p.getInt("afternoon",12);set(v){p.edit().putInt("afternoon",v).apply()}
    var evening:Int get()=p.getInt("evening",18);set(v){p.edit().putInt("evening",v).apply()}
    var night:Int get()=p.getInt("night",22);set(v){p.edit().putInt("night",v).apply()}
    fun clear(){p.edit().clear().apply()}
}
object GreetingPolicy {
    fun validBounds(morning:Int,afternoon:Int,evening:Int,night:Int)=morning in 0..20 && afternoon in (morning+1)..21 && evening in (afternoon+1)..22 && night in (evening+1)..23
    fun greeting(hour:Int,language:AppLanguage,known:Boolean,owner:Boolean,approvedName:String?,morning:Int=5,afternoon:Int=12,evening:Int=18,night:Int=22):String {
        require(hour in 0..23 && validBounds(morning,afternoon,evening,night))
        if(!known)return if(language==AppLanguage.ES)"Hola. ¿Cómo puedo ayudarte?" else "Hello. How can I help you?"
        val address=if(owner){if(language==AppLanguage.ES)"señor" else "sir"} else approvedName?.take(80)?.takeIf{it.isNotBlank() && it.none(Char::isISOControl)} ?: return greeting(hour,language,false,false,null)
        val slot=when(hour){in morning until afternoon->0;in afternoon until evening->1;in evening until night->2;else->3}
        return if(language==AppLanguage.ES)listOf("Buenos días","Buenas tardes","Buenas noches","Buenas noches")[slot]+", $address. ¿Cómo puedo ayudarte?"
        else listOf("Good morning","Good afternoon","Good evening","Good night")[slot]+", $address. How can I help you?"
    }
}
data class AcousticMeasurement(val rms:Double,val dbfs:Double,val clippingFraction:Double,val seconds:Double)
object VoiceMeasurements {
    fun measure(pcm:ByteArray,rate:Int=16000):AcousticMeasurement {
        require(pcm.isNotEmpty() && pcm.size%2==0 && rate in 8000..48000)
        var square=0.0;var clipped=0
        for(i in 0 until pcm.size/2){val v=((pcm[i*2].toInt() and 255) or (pcm[i*2+1].toInt() shl 8)).toShort()/32768.0;square+=v*v;if(abs(v)>=.98)clipped++}
        val rms=sqrt(square/(pcm.size/2))
        return AcousticMeasurement(rms,20*log10(rms.coerceAtLeast(1e-6)),clipped.toDouble()/(pcm.size/2),pcm.size.toDouble()/(rate*2))
    }
    fun signalNoiseDb(signal:Double,noise:Double)=20*log10(signal.coerceAtLeast(1e-6)/noise.coerceAtLeast(1e-6))
}
