package com.jarvis.assistant.sentinel

import com.jarvis.assistant.hud.BatteryReading
import com.jarvis.assistant.hud.HudSettings

/** Targets, not fabricated measurements. Actual Android motion/power/thermal/memory limits win. */
object CoreMotionPolicy {
    fun pauseReason(settings:HudSettings,systemMotion:Boolean):String?=when {
        settings.reducedMotion->"Reduced motion is enabled in Jarvis"
        !systemMotion->"Android animations are disabled"
        settings.sentinel.animationSpeed<=0f->"Core animation speed is zero"
        !settings.animationIntensity.isFinite() || settings.animationIntensity<=0f->"Core motion intensity is zero"
        else->null
    }
    fun fps(settings:HudSettings,battery:BatteryReading,powerSaver:Boolean,systemMotion:Boolean,mode:SentinelMode,restricted:Boolean=false,thermal:Int?=null,refreshRate:Float=60f):Int {
        if(pauseReason(settings,systemMotion)!=null)return 0
        val display=if(refreshRate.isFinite() && refreshRate>=1)kotlin.math.round(refreshRate).toInt().coerceIn(1,60)else 30
        val target=when {
            powerSaver || settings.animationProfile=="saver"->8
            (thermal ?: 0)>=4->8
            (thermal ?: 0)>=3->20
            settings.autoBatterySaver && (battery.percent ?: 100)<10->if(battery.charging)16 else 8
            (battery.percent ?: 100)<20->20
            restricted->if(mode==SentinelMode.DORMANT)20 else 30
            mode==SentinelMode.DORMANT->30
            else->60
        }
        return minOf(display,target)
    }
    fun visualLevel(raw:Float):Float=if(raw.isFinite())(kotlin.math.sqrt(raw.coerceIn(0f,1f))*2.2f).coerceIn(0f,1f)else 0f
    fun breathing(now:Long,mode:SentinelMode,motion:Boolean):Float=if(motion && mode==SentinelMode.DORMANT)(1+.045*kotlin.math.sin(now/1400.0)).toFloat()else 1f
    fun errorVisible(at:Long,now:Long)=at>0 && now-at in 0..650L
}
