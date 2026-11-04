package com.jarvis.assistant.rebirth

import com.jarvis.assistant.core.AppLanguage

object ConversationCompletionPolicy {
    fun matches(input:String):Boolean {
        val t=com.jarvis.assistant.neural.AppMatching.normalize(input).removePrefix("hey jarvis ").removePrefix("jarvis ")
        return t in setOf("i m done","im done","i am done","okay i m done","ok i m done","okay im done","ok im done","that s it","thats it","that s all","thats all","okay that s enough","ok that s enough","that s enough","enough for now","end this conversation","ya termine","termine","eso es todo","ya es suficiente","terminar conversacion")
    }
}

/** Sir is a courtesy preference, not evidence of identity. No stored names enter public speech. */
object ConversationGreetingPolicy {
    fun text(language:AppLanguage,unknownAfterMatch:Boolean=false):String = when {
        unknownAfterMatch && language==AppLanguage.ES->"Hola. No estoy seguro de que seas el propietario. ¿Cómo puedo ayudarte?"
        unknownAfterMatch->"Hello. I'm not sure you're the owner. How can I help you?"
        language==AppLanguage.ES->"Hola, señor. ¿Cómo puedo ayudarte?"
        else->"Hello sir, how can I help you?"
    }
}
