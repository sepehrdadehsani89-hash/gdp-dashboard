package com.jarvis.assistant.pickup

import android.app.Service
import android.hardware.*
import android.os.SystemClock
import com.jarvis.assistant.JarvisApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Sensor lifetime follows the already user-enabled foreground wake service. No new mic. */
class PickupMonitor(private val service:Service):SensorEventListener,AutoCloseable {
    private val runtime get()=(service.application as JarvisApplication).runtime
    private val sensors=service.getSystemService(SensorManager::class.java)
    private val policy=PickupPolicy(SystemClock::elapsedRealtime)
    private var far=false
    private var registered=false
    fun start(){
        if(!runtime.pickupPreferences.enabled){mutable.value="Pickup greetings are off";return}
        val accelerometer=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val proximity=sensors.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        if(accelerometer==null || proximity==null || proximity.maximumRange<=0){mutable.value="Pickup greeting unavailable: motion and proximity sensors are both required";return}
        registered=sensors.registerListener(this,proximity,SensorManager.SENSOR_DELAY_NORMAL) && sensors.registerListener(this,accelerometer,200_000)
        if(!registered){sensors.unregisterListener(this);mutable.value="Android could not register pickup sensors";return}
        mutable.value="Motion + proximity active while wake listening is enabled; unlocked screen only"
    }
    override fun onSensorChanged(event:SensorEvent){
        if(!registered)return
        if(event.sensor.type==Sensor.TYPE_PROXIMITY){far=event.values.firstOrNull()?.let{it.isFinite() && it>=minOf(5f,event.sensor.maximumRange)}==true;if(!far)policy.reset();return}
        if(event.sensor.type!=Sensor.TYPE_ACCELEROMETER || event.values.size<3)return
        val eligible=runtime.pickupEligible() && !runtime.pickupPreferences.coolingDown(System.currentTimeMillis())
        if(policy.sample(event.values[0],event.values[1],event.values[2],eligible,far))runtime.greetPickup()
    }
    override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){if(accuracy==SensorManager.SENSOR_STATUS_UNRELIABLE){far=false;policy.reset()}}
    override fun close(){registered=false;sensors.unregisterListener(this);far=false;policy.reset();mutable.value="Pickup sensors stopped"}
    companion object{
        private val mutable=MutableStateFlow("Pickup sensors start with user-enabled wake listening")
        val state=mutable.asStateFlow()
    }
}
