package com.jarvis.assistant.voice

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.choose
import com.jarvis.assistant.data.ProviderException
import com.jarvis.assistant.brain.ProviderFailureKind

/** Used only after the already opted-in Local Brain actually completes an answer. Spoken and shown. */
object AiAnswerProvenance {
    fun localFallback(answer:String,language:AppLanguage):String {
        if(answer.isBlank())throw ProviderException("The local fallback returned no answer.",ProviderFailureKind.INCOMPLETE)
        return language.choose("The cloud AI is unavailable. This answer is from your Local Brain. ","La IA en la nube no está disponible. Esta respuesta es de tu cerebro local. ","هوش مصنوعی ابری در دسترس نیست. این پاسخ از مغز محلی شماست. ")+answer
    }
}
