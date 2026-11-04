package com.jarvis.assistant.rebirth

/** Explicit unavailable result, never a fabricated inbox answer from a general AI. */
object RemovedMailIntegration {
    fun matches(text:String):Boolean {
        val t=com.jarvis.assistant.neural.AppMatching.normalize(text)
        return t in setOf("open account center","open my account settings","open account settings","show my email","open my email","read my latest email","read my last email","check my last email","what email did i just get","what s my newest gmail","whats my newest gmail","who emailed me","show my unread messages","show unread emails","do i have any emails","check my email","summarize the last email","summarize my latest email","lee mi ultimo correo","revisa mi correo","muestra correos sin leer","resume el ultimo correo") ||
            Regex("^(?:show|read)(?: me)? (?:the )?(?:email|message)s? from .+$").matches(t)
    }
}
