package com.jarvis.assistant.voice

/** Harmless playback controls only. Never unlocks, dispatches a tool or stores a transcript. */
object PlaybackInterruptPolicy {
    fun command(recognized:String,spoken:String):String? {
        val normalize=com.jarvis.assistant.neural.AppMatching::normalize
        val words=normalize(recognized).removePrefix("hey jarvis ").removePrefix("oye jarvis ").removePrefix("jarvis ")
        val control=when(words){
            "wait wait","espera espera"->"wait wait"
            "stop talking","stop speaking","deja de hablar"->"stop talking"
            else->return null
        }
        val output=" "+normalize(spoken)+" "
        if(output.contains(" $words "))return null
        return control
    }
    fun mayArm(wakeRunning:Boolean,enabled:Boolean,voiceConversation:Boolean,privacyPending:Boolean)=wakeRunning && enabled && voiceConversation && !privacyPending
    fun current(expected:Long,actual:Long,speaking:Boolean)=expected==actual && speaking
}
