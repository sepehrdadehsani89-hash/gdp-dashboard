package com.jarvis.assistant.voice

/** Stitch engine-final speech segments while allowing a brief natural pause before a conversational turn ends. */
class UtteranceAssembler(private val now:()->Long,private val threshold:Float=.015f){
    private val segments=mutableListOf<String>()
    private var tail=""
    private var lastVoice=now()
    fun reset(){segments.clear();tail="";lastVoice=now()}
    fun partial(text:String){val clean=text.trim().take(8000);if(clean!=tail && clean.isNotBlank()){tail=clean;lastVoice=now()}}
    fun segment(text:String,keepPartial:Boolean=false){val clean=text.trim();if(clean.isNotBlank() && this.text().length+clean.length<=8000){segments+=clean;if(!keepPartial)tail="";lastVoice=now()}}
    fun clearPartial(){tail=""}
    val hasUnfinishedSpeech:Boolean get()=tail.isNotBlank()
    fun energy(level:Float){if((segments.isNotEmpty() || tail.isNotBlank()) && level.isFinite() && level>=threshold)lastVoice=now()}
    fun text()=(segments+listOf(tail).filter{it.isNotBlank()}).joinToString(" ").take(8000)
    fun ready():Boolean {
        if(segments.isEmpty() || tail.isNotBlank())return false
        val incomplete=Regex("(?:\\b(?:and|then|because|with|to|about|y|pero|porque|con|sobre)|[,;:])$",RegexOption.IGNORE_CASE).containsMatchIn(text())
        return now()-lastVoice>=if(incomplete)2500 else 1000
    }
    companion object {
        fun fastCommand(text:String)=com.jarvis.assistant.rebirth.ConversationCompletionPolicy.matches(text) || Regex("^(?:open|abre|call|llama|lock my|unlock my|bloquea|desbloquea|turn (?:on|off) (?:the )?flashlight|start (?:a )?\\d+.+timer|inicia (?:un )?temporizador)\\b",RegexOption.IGNORE_CASE).containsMatchIn(text.trim()) &&
            !Regex("\\b(and|then|y|despues|después)\\b|[,;]",RegexOption.IGNORE_CASE).containsMatchIn(text)
    }
}
