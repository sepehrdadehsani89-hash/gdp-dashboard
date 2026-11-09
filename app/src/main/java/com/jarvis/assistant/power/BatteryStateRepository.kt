package com.jarvis.assistant.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.jarvis.assistant.hud.HudRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The application's only battery broadcast observer. No microphone or new foreground service. */
class BatteryStateRepository(private val context:Context) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val manager=context.getSystemService(BatteryManager::class.java)
    private val mutable=MutableStateFlow(PowerReading())
    val state=mutable.asStateFlow()
    private var started=false
    private var serial=0L
    private val estimates=ChargingEstimateFilter()
    private var measurement:Job?=null
    private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){if(i?.action==Intent.ACTION_BATTERY_CHANGED)observe(i)}}
    fun start(){
        if(started)return
        started=true
        ContextCompat.registerReceiver(context,receiver,IntentFilter(Intent.ACTION_BATTERY_CHANGED),ContextCompat.RECEIVER_EXPORTED)?.let(::observe)
        scope.launch{while(isActive){delay(60_000);refresh()}}
    }
    fun refresh(){
        val sticky=runCatching{context.registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))}.getOrNull()
        if(sticky!=null)observe(sticky)else{serial++;measurement?.cancel();estimates.update(null,false,SystemClock.elapsedRealtime());mutable.value=PowerReading(observedAt=SystemClock.elapsedRealtime())}
    }
    fun observe(intent:Intent){
        val expected=++serial
        measurement?.cancel()
        val battery=HudRepository.battery(intent)
        val plugged=intent.takeIf{it.hasExtra(BatteryManager.EXTRA_PLUGGED)}?.getIntExtra(BatteryManager.EXTRA_PLUGGED,-1)?.takeIf{it>=0}
        val condition=PowerReading.condition(intent.getIntExtra(BatteryManager.EXTRA_STATUS,-1),plugged)
        val now=SystemClock.elapsedRealtime()
        val prior=mutable.value
        val compatible=condition==PowerCondition.CHARGING && prior.condition==condition && prior.connected==true
        val initial=PowerReading(battery,condition,plugged?.let{it>0},if(compatible)prior.estimate else null,now,
            intent.takeIf{it.hasExtra(BatteryManager.EXTRA_VOLTAGE)}?.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0)?.takeIf{it in 1000..6000})
        mutable.value=initial
        measurement=scope.launch{
            val (remaining,current)=withContext(Dispatchers.IO){
                val eta=if(condition==PowerCondition.CHARGING)runCatching{manager.computeChargeTimeRemaining()}.getOrNull()else null
                val amps=runCatching{manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)}.getOrNull()?.takeUnless{it==Int.MIN_VALUE || it==Int.MAX_VALUE}
                eta to amps
            }
            if(expected!=serial)return@launch
            val estimate=estimates.update(remaining,condition==PowerCondition.CHARGING,SystemClock.elapsedRealtime())
            mutable.value=initial.copy(estimate=estimate,currentMicroAmps=current)
        }
    }
}
