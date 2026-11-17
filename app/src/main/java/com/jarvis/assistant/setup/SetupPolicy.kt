package com.jarvis.assistant.setup

/** Progress records completion of the wizard, never a provider or microphone capability. */
object SetupPolicy {
    const val REVISION=1
    val steps=listOf("Welcome to Jarvis","Choose Language","Microphone Setup","Voice Identity","Select AI Brain","Connect Provider","Test AI Response","Test Voice Output","Device Permissions","Device Control Test","Wallpaper & Appearance","Final Diagnostics","Jarvis Ready")
    fun needed(completed:Boolean,revision:Int)=!completed || revision<REVISION
    fun next(step:Int)=(step+1).coerceIn(0,steps.lastIndex)
    fun back(step:Int)=(step-1).coerceIn(0,steps.lastIndex)
    fun ready(microphone:Boolean,inference:Boolean,playbackCompleted:Boolean,heardVoice:Boolean)=microphone && inference && playbackCompleted && heardVoice
    fun status(microphone:Boolean,inference:Boolean,playbackCompleted:Boolean,heardVoice:Boolean)=if(ready(microphone,inference,playbackCompleted,heardVoice))"Jarvis Ready · the selected AI test and voice check completed." else "Setup can be saved. Microphone, AI inference or audible voice verification remains pending."
}
