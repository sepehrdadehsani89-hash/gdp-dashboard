package com.jarvis.assistant.settings

import com.jarvis.assistant.neural.AppMatching

enum class SpokenValueType { BOOLEAN, FRACTION, NUMBER, TEXT, CHOICE }
data class SpokenSetting(val id:String,val label:String,val type:SpokenValueType,val aliases:List<String>,val min:Double=0.0,val max:Double=1.0,val choices:List<String> = emptyList(),val confirmation:Boolean=false)
sealed interface SpokenSettingRequest {
    data class Edit(val setting:SpokenSetting,val value:String):SpokenSettingRequest
    data class Query(val setting:SpokenSetting):SpokenSettingRequest
    data object Help:SpokenSettingRequest
    data object PrivateCredential:SpokenSettingRequest
}
/** A bounded allowlist, never reflection, arbitrary preference paths or LLM-generated actions. */
object SpokenSettings {
    private fun bool(id:String,label:String,vararg aliases:String,confirm:Boolean=false)=SpokenSetting(id,label,SpokenValueType.BOOLEAN,aliases.toList()+label,confirmation=confirm)
    private fun fraction(id:String,label:String,min:Double=0.0,max:Double=1.0,vararg aliases:String)=SpokenSetting(id,label,SpokenValueType.FRACTION,aliases.toList()+label,min,max)
    val all=listOf(
        bool("reduced_motion","reduced motion","reduce motion","movimiento reducido","کاهش حرکت"),
        bool("sound_effects","sound effects","system sounds","efectos de sonido"),
        bool("charging_effects","charging effects","charging particles"),
        bool("wallpaper_animation","wallpaper animation","animated wallpaper"),
        bool("wallpaper_clock","wallpaper clock"),bool("wallpaper_battery","wallpaper battery"),bool("wallpaper_weather","wallpaper weather"),
        bool("battery_adaptation","battery adaptation","automatic battery saver"),
        bool("captions","speech captions","captions","subtitles","subtitulos"),
        bool("continuous","continuous conversation","follow up listening"),
        bool("noise_reduction","noise reduction"),bool("echo_cancellation","echo cancellation"),bool("automatic_gain","automatic gain"),
        bool("spoken_replies","spoken replies","speak replies","voice replies"),
        bool("locked_chat","locked screen chat","chat while locked",confirm=true),
        bool("private_voice","private voice replies","private voice output",confirm=true),
        bool("wake_listening","wake listening","microphone","always on microphone",confirm=true),
        fraction("intensity","animation intensity",aliases=arrayOf("motion intensity")),
        fraction("particles","particle density"),fraction("glow","ambient glow"),fraction("core_brightness","core brightness",.15,1.0),
        fraction("voice_volume","voice volume"),fraction("voice_rate","voice speed",.7,1.3,"speech rate"),fraction("voice_pitch","voice pitch",.7,1.3),
        SpokenSetting("core_speed","core animation speed",SpokenValueType.NUMBER,listOf("core speed","ring speed","core animation speed"),0.0,2.0),
        SpokenSetting("cooldown","speech cooldown",SpokenValueType.NUMBER,listOf("speech cooldown","audio cooldown"),250.0,2000.0),
        SpokenSetting("endpoint","speech endpoint pause",SpokenValueType.NUMBER,listOf("speech endpoint pause","end of speech pause"),650.0,1800.0),
        SpokenSetting("caption_linger","caption duration",SpokenValueType.NUMBER,listOf("caption duration","caption linger"),0.0,5000.0),
        SpokenSetting("language","language",SpokenValueType.CHOICE,listOf("language","idioma","زبان"),choices=listOf("english","spanish","en","es")),
        SpokenSetting("accent","accent color",SpokenValueType.CHOICE,listOf("accent color","theme color"),choices=listOf("cyan","blue","purple")),
        SpokenSetting("profile","animation profile",SpokenValueType.CHOICE,listOf("animation profile"),choices=listOf("balanced","cinematic","saver")),
        SpokenSetting("clock","clock format",SpokenValueType.CHOICE,listOf("clock format"),choices=listOf("12","24","system","12 hour","24 hour")),
        SpokenSetting("sensitivity","microphone sensitivity",SpokenValueType.CHOICE,listOf("microphone sensitivity","voice sensitivity"),choices=listOf("low","balanced","high")),
        SpokenSetting("brain","ai brain",SpokenValueType.CHOICE,listOf("ai brain","brain","provider"),choices=listOf("chatgpt","openai api","claude","gemini","local"),confirmation=true),
        SpokenSetting("wake_phrase","wake phrase",SpokenValueType.TEXT,listOf("wake phrase","wake word")),
        SpokenSetting("name","my name",SpokenValueType.TEXT,listOf("my name","user name","name")),
        SpokenSetting("weather_city","weather city",SpokenValueType.TEXT,listOf("weather city"))
    )
    fun find(label:String)=all.firstOrNull{spec->spec.aliases.any{AppMatching.normalize(it)==AppMatching.normalize(label)}}
    fun validate(spec:SpokenSetting,raw:String):String {
        require(all.any{it==spec}){"Unsupported setting"}
        val value=raw.trim();require(value.length in 1..100 && value.none(Char::isISOControl)){"Use a short setting value"}
        val n=AppMatching.normalize(value)
        return when(spec.type){
            SpokenValueType.BOOLEAN->when(n){"on","true","enabled","enable","activado","encendido","فعال"->"true";"off","false","disabled","disable","desactivado","apagado","غیرفعال"->"false";else->error("Say on or off")}
            SpokenValueType.CHOICE->spec.choices.firstOrNull{AppMatching.normalize(it)==n} ?: error("Choose one of: ${spec.choices.joinToString()}")
            SpokenValueType.TEXT->{if(spec.id=="wake_phrase")require(com.jarvis.assistant.voice.WakePhraseMatcher.validCustomPhrase(value)){"Use a distinctive valid wake phrase"};value}
            SpokenValueType.NUMBER,SpokenValueType.FRACTION->{
                val percent=value.contains('%') || n.endsWith(" percent") || n.endsWith(" por ciento")
                val cleaned=value.lowercase(java.util.Locale.ROOT).replace(Regex("%| percent| por ciento| milliseconds| ms"),"").trim()
                val number=cleaned.toDoubleOrNull() ?: numberWords(cleaned) ?: error("Use a number or percentage")
                val adjusted=if(spec.type==SpokenValueType.FRACTION && (percent || number>spec.max))number/100 else number
                require(adjusted.isFinite() && adjusted in spec.min..spec.max){"Value outside ${spec.min}–${spec.max}"}
                if(spec.type==SpokenValueType.NUMBER)require(adjusted%1.0==0.0 || spec.id=="core_speed"){"Use whole milliseconds"}
                adjusted.toString()
            }
        }
    }
    private fun numberWords(value:String):Double? {
        val small=listOf("zero","one","two","three","four","five","six","seven","eight","nine","ten","eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen")
        val tens=mapOf("twenty" to 20,"thirty" to 30,"forty" to 40,"fifty" to 50,"sixty" to 60,"seventy" to 70,"eighty" to 80,"ninety" to 90)
        val words=value.replace('-',' ').split(' ').filter{it.isNotBlank() && it!="and"};if(words.isEmpty() || words.size>5)return null
        var total=0
        for(w in words){val v=small.indexOf(w);when{v>=0->total+=v;w in tens->total+=tens.getValue(w);w=="hundred" && total in 1..9->total*=100;w=="thousand" && total in 1..2->total*=1000;else->return null}}
        return total.toDouble()
    }
    fun credentialRequest(input:String):Boolean=input.length<=8000 && Regex("""^(?:set|save|store|remember|change) .*\b(?:api key|password|access token|refresh token|client secret|secret key)\b""").containsMatchIn(AppMatching.normalize(input).removePrefix("hey jarvis ").removePrefix("jarvis "))
    fun parse(input:String):SpokenSettingRequest? {
        if(input.length !in 1..400 || input.any(Char::isISOControl))return null
        val raw=input.trim().replace(Regex("^(?:hey jarvis|jarvis)[, ]+",RegexOption.IGNORE_CASE),"")
        val n=AppMatching.normalize(raw)
        if(credentialRequest(raw))return SpokenSettingRequest.PrivateCredential
        if(n in setOf("what settings can i change by voice","voice settings help","list voice settings"))return SpokenSettingRequest.Help
        Regex("^(?:what is|what s|show|read) (?:my |the )?(.+?)(?: setting)?$").matchEntire(n)?.let{match->find(match.groupValues[1])?.let{return SpokenSettingRequest.Query(it)}}
        val pair=Regex("^(?:set|change|adjust|cambia|ajusta) (.+?) (?:to|a|en) (.+)$",RegexOption.IGNORE_CASE).matchEntire(raw)?.let{it.groupValues[1] to it.groupValues[2]}
            ?: Regex("^(?:turn|switch) (on|off) (.+)$").matchEntire(n)?.let{it.groupValues[2] to it.groupValues[1]}
            ?: Regex("^(?:turn|switch) (.+) (on|off)$").matchEntire(n)?.let{it.groupValues[1] to it.groupValues[2]}
            ?: Regex("^(enable|disable) (.+)$").matchEntire(n)?.let{it.groupValues[2] to if(it.groupValues[1]=="enable")"on" else "off"}
            ?: Regex("^use (chatgpt|claude|gemini|local|openai api)(?: as (?:my )?(?:ai )?brain)?$").matchEntire(n)?.let{"brain" to it.groupValues[1]}
            ?: return null
        val spec=find(pair.first) ?: return null
        return SpokenSettingRequest.Edit(spec,validate(spec,pair.second))
    }
}
