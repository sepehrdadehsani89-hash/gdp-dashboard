package com.jarvis.assistant.voice

/** Stitch engine-final speech segments while allowing a brief natural pause before a conversational turn ends. */
class UtteranceAssembler(private val now:()->Long,private val threshold:Float=.015f,pauseMs:Long=1000L){
    private val segments=mutableListOf<String>()
    private var tail=""
    private var lastVoice=now()
    private var lastText=lastVoice
    private var lastGrowth=lastVoice
    private var largestPartialWords=0
    private val pause=pauseMs.coerceIn(650L,1800L)
    private var finalizedEndpoint=false
    fun reset(){segments.clear();tail="";lastVoice=now();lastText=lastVoice;lastGrowth=lastVoice;largestPartialWords=0;finalizedEndpoint=false}
    fun partial(text:String){val clean=text.trim().take(8000);if(clean!=tail && clean.isNotBlank()){
        val words=Regex("[\\p{L}\\p{N}]+").findAll(clean).count()
        if(words>largestPartialWords){largestPartialWords=words;lastGrowth=now()}
        tail=clean;lastVoice=now();lastText=lastVoice;finalizedEndpoint=false
    }}
    fun segment(text:String,keepPartial:Boolean=false){val clean=text.trim();if(clean.isNotBlank() && this.text().length+clean.length<=8000){segments+=clean;if(!keepPartial)tail="";lastVoice=now();lastText=lastVoice;lastGrowth=lastVoice;largestPartialWords=0;finalizedEndpoint=false}}
    fun clearPartial(){tail=""}
    val hasUnfinishedSpeech:Boolean get()=tail.isNotBlank()
    fun energy(level:Float){if((segments.isNotEmpty() || tail.isNotBlank()) && level.isFinite() && level>=threshold)lastVoice=now()}
    fun text()=(segments+listOf(tail).filter{it.isNotBlank()}).joinToString(" ").take(8000)
    fun acceptedText()=segments.joinToString(" ").take(8000)
    /** An empty decoder boundary cannot authorize dropping a newly heard compound tail. */
    fun emptyFinal(endpoint:Boolean):Boolean{
        if(hasUnfinishedSpeech && segments.isNotEmpty())return false
        clearPartial();if(endpoint && acceptedText().isNotBlank())finalizedEndpoint()
        return true
    }
    /** A partial is never a command. Ask the decoder for a confidence-checked final instead.
     * Constant room noise must not postpone this forever when the words have stopped changing. */
    fun needsFinalization():Boolean = tail.isNotBlank() &&
        (now()-lastVoice>=(if(incomplete())(pause*2).coerceIn(1200L,1800L) else pause) ||
            now()-lastText>=(if(incomplete())4000 else 2500) ||
            now()-lastGrowth>=(if(incomplete())4000 else 2500))
    fun finalizedEndpoint(){finalizedEndpoint=true}
    private fun incomplete()=Regex("(?:\\b(?:and|then|because|with|to|about|y|pero|porque|con|sobre)|[,;:])$",RegexOption.IGNORE_CASE).containsMatchIn(text())
    fun ready():Boolean {
        if(segments.isEmpty() || tail.isNotBlank())return false
        if(finalizedEndpoint)return true
        val incomplete=incomplete()
        return now()-lastVoice>=(if(incomplete)(pause*2).coerceIn(1200L,1800L) else pause) ||
            now()-lastText>=(if(incomplete)4000 else 1800)
    }
    companion object {
        fun fastCommand(text:String)=com.jarvis.assistant.core.LanguageRules.languageRequest(text)!=null || LocalVoiceQueries.readOnly(text) || com.jarvis.assistant.rebirth.ConversationCompletionPolicy.matches(text) || com.jarvis.assistant.security.ScreenCommandPolicy.parse(text)!=null || Regex("^(?:open|abre|call|llama|lock my|unlock my|bloquea|desbloquea|turn (?:on|off) (?:the )?flashlight|start (?:a )?\\d+.+timer|inicia (?:un )?temporizador)\\b",RegexOption.IGNORE_CASE).containsMatchIn(text.trim()) &&
            !Regex("\\b(and|then|y|despues|después)\\b|[,;]",RegexOption.IGNORE_CASE).containsMatchIn(text)
    }
}
