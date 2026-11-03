package com.jarvis.assistant.voice
import com.jarvis.assistant.neural.AppMatching
/** Recognized control text only. Does not turn on an echo-sensitive recorder during TTS. */
object ConversationInterruptPolicy {
    fun matches(text:String)=AppMatching.normalize(text) in setOf("wait wait","hold on","stop talking","one second","pause","espera espera","un segundo","deja de hablar","صبر کن","یک لحظه","حرف نزن")
}
