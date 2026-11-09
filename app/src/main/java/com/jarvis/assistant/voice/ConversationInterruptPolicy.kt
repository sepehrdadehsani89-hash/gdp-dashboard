package com.jarvis.assistant.voice
import com.jarvis.assistant.neural.AppMatching
/** Normal recognized session controls; playback controls use a separate narrow, echo-checked gate. */
object ConversationInterruptPolicy {
    fun replacement(text:String):String? {
        val t=AppMatching.normalize(text)
        return Regex("^(?:wait |actually |espera )+(open .+|abre .+)$").matchEntire(t)?.groupValues?.get(1)?.removeSuffix(" instead")?.removeSuffix(" en su lugar")
    }
    fun matches(text:String)=AppMatching.normalize(text) in setOf("wait wait","hold on","stop talking","one second","pause","espera espera","un segundo","deja de hablar","صبر کن","یک لحظه","حرف نزن")
}
