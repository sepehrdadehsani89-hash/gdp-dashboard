package com.jarvis.assistant.neural

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NeuralEvent { WAKE_DETECTED,ASSISTANT_LISTENING,COMMAND_RECOGNIZED,LOCAL_MODEL_LOADING,LOCAL_MODEL_GENERATING,TOOL_RUNNING,TOOL_COMPLETED,ASSISTANT_SPEAKING,BRIEFING_READY,AUTHORIZATION_REQUIRED,ERROR,ASSISTANT_IDLE }
data class NeuralEventState(val event:NeuralEvent=NeuralEvent.ASSISTANT_IDLE,val label:String="Idle",val sequence:Long=0)
/** Labels are real state transitions, not estimates of model completion. */
object NeuralEvents {
    private val mutable=MutableStateFlow(NeuralEventState());val state=mutable.asStateFlow()
    @Synchronized fun emit(event:NeuralEvent,label:String){mutable.value=NeuralEventState(event,label.take(100),mutable.value.sequence+1)}
}
