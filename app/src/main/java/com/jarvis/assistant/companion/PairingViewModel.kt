package com.jarvis.assistant.companion

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*

class PairingViewModel(client:CompanionClient,track:(Job)->Unit):ViewModel(){
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    val coordinator=PairingCoordinator(client,scope,track=track)
    var handoff=false
    override fun onCleared(){coordinator.cancel();scope.cancel()}
}
