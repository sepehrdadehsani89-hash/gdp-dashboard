package com.jarvis.assistant.rebirth

import com.jarvis.assistant.core.AppLanguage

object ConversationCompletionPolicy {
    fun matches(input:String)=com.jarvis.assistant.farewell.FarewellIntent.matches(input)
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
