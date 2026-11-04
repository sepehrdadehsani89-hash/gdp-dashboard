package com.jarvis.assistant.titan

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.voice.CustomVoicePolicy

enum class SpeechRoute { LOCAL,FISH,OPENAI,TEXT_ONLY }
object SpeechRoutePolicy {
    fun route(language:AppLanguage,enabled:Boolean,id:String,allowCloud:Boolean,privateTurn:Boolean,legacyCloud:Boolean,privateConsent:Boolean=false):SpeechRoute {
        if(language !in AppLanguage.supported)return SpeechRoute.TEXT_ONLY
        val eligible=enabled && (allowCloud && !privateTurn || privateTurn && privateConsent)
        if(language==AppLanguage.ES)return if(eligible && id==CustomVoicePolicy.SPANISH_ID)SpeechRoute.FISH else SpeechRoute.TEXT_ONLY
        return if(eligible && CustomVoicePolicy.validId(id))SpeechRoute.FISH else SpeechRoute.TEXT_ONLY
    }
}
