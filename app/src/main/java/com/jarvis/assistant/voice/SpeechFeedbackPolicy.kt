package com.jarvis.assistant.voice

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.choose

/** Fixed acknowledgement for text-only/private results. No tool output is sent to TTS. */
object SpeechFeedbackPolicy {
    fun publicAcknowledgement(language:AppLanguage,confirmationRequired:Boolean)=language.choose(
        if(confirmationRequired)"Please review the confirmation on your screen, sir." else "Sir, the result is ready on your screen.",
        if(confirmationRequired)"Revisa la confirmación en tu pantalla, señor." else "Señor, el resultado está listo en tu pantalla.",
        if(confirmationRequired)"لطفاً تأیید روی صفحه را بررسی کنید." else "نتیجه روی صفحه آماده است.",
    )
    fun needsPublicAcknowledgement(contentRoute:com.jarvis.assistant.titan.SpeechRoute,publicRoute:com.jarvis.assistant.titan.SpeechRoute)=
        contentRoute==com.jarvis.assistant.titan.SpeechRoute.TEXT_ONLY && publicRoute==com.jarvis.assistant.titan.SpeechRoute.FISH
}
