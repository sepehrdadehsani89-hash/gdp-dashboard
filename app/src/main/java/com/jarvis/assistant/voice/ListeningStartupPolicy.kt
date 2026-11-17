package com.jarvis.assistant.voice

enum class ListeningStartupAction { NONE, START_WAKE, MODEL_REQUIRED }
object ListeningStartupPolicy {
    fun decide(enabled:Boolean,permission:Boolean,model:Boolean,visible:Boolean,locked:Boolean,running:Boolean,audioBusy:Boolean):ListeningStartupAction=when {
        !enabled || !permission || !visible || locked || running || audioBusy->ListeningStartupAction.NONE
        !model->ListeningStartupAction.MODEL_REQUIRED
        else->ListeningStartupAction.START_WAKE
    }
}
