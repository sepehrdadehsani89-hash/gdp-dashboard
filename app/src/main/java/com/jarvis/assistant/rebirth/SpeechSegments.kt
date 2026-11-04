package com.jarvis.assistant.rebirth

/** Native/tool replies use the same bounded Fish sequence as ordinary replies, never a new voice. */
object SpeechSegments {
    fun split(text:String,max:Int=900):List<String>{
        require(max in 100..3500)
        val remaining=StringBuilder(text.take(16000));val parts=mutableListOf<String>()
        while(remaining.isNotEmpty()){
            if(remaining.length<=max){remaining.toString().trim().takeIf{it.isNotEmpty()}?.let(parts::add);break}
            val cut=remaining.lastIndexOf(' ',max).takeIf{it>=max/3} ?: max
            remaining.substring(0,cut).trim().takeIf{it.isNotEmpty()}?.let(parts::add);remaining.delete(0,cut)
        }
        return parts.take(64)
    }
}
object SpeechLocalePolicy {
    fun select(spanish:Boolean,supported:List<String>):String? {
        val normalized=supported.distinct().filter{it.length<=40}.associateBy{it.replace('_','-').lowercase(java.util.Locale.ROOT)}
        val preferred=if(spanish)listOf("es-ec","es-419","es-es","es") else listOf("en-us","en-gb","en")
        preferred.firstNotNullOfOrNull{normalized[it]}?.let{return it}
        return normalized.entries.firstOrNull{it.key.startsWith(if(spanish)"es-" else "en-")}?.value
    }
}
