package com.jarvis.assistant.voice

import com.jarvis.assistant.core.AppLanguage

/** Native public knowledge; TIME/DATE keep their existing device-format-aware handler. */
object LocalKnowledge {
    fun answer(text:String,language:AppLanguage):String? = when(val query=LocalVoiceQueries.parse(text)){
        LocalVoiceQuery.IDENTITY,LocalVoiceQuery.TIME_ZONE->LocalVoiceQueries.answer(query,language)
        else->LocalArithmetic.answer(text,language)
    }
}
