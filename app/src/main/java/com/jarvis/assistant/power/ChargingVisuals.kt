package com.jarvis.assistant.power

import kotlin.math.min

data class ChargingVisual(val angle:Float=0f,val alpha:Float=0f,val pulse:Float=0f)
/** Independent charging layer; listening and speaking changes cannot reset its phase. */
class ChargingVisualClock {
    private var previous:Long?=null
    private var connected=false
    private var startedAt=0L
    private var angle=0f
    private var alpha=0f
    private var velocity=0f
    fun pause(){previous=null}
    fun frame(now:Long,charging:Boolean,motion:Boolean=true,speed:Float=1f):ChargingVisual {
        val dt=previous?.let{(now-it).coerceIn(0,250).toFloat()/1000} ?: 0f
        previous=now
        if(charging && !connected)startedAt=now
        connected=charging
        if(!motion){alpha=if(charging)1f else 0f;velocity=0f;return ChargingVisual(angle,alpha)}
        alpha=(alpha+if(charging)dt/0.35f else -dt/0.35f).coerceIn(0f,1f)
        val target=if(charging)18f*speed.takeIf{it.isFinite()}?.coerceIn(0f,2f).orZero() else 0f
        velocity+=(target-velocity)*(dt*3f).coerceIn(0f,1f)
        angle=(angle+velocity*dt)%360f
        val pulse=if(charging)(1f-(now-startedAt)/1000f).coerceIn(0f,1f) else 0f
        return ChargingVisual(angle,alpha,pulse)
    }
    private fun Float?.orZero()=this ?: 0f
}

data class PowerWallpaperLayout(val stripTop:Float,val stripHeight:Float,val clockY:Float,val dateY:Float,val captionTop:Float,val captionHeight:Float)
object PowerWallpaperGeometry {
    fun radius(width:Float,height:Float,base:Float,centerY:Float,strip:Boolean):Float {
        if(!strip || width>height)return base
        return min(base,((height*.95f-centerY-min(width,height)*.30f)/1.03f).coerceAtLeast(base*.55f))
    }
    fun layout(width:Float,height:Float,centerY:Float,radius:Float,strip:Boolean,clock:Boolean):PowerWallpaperLayout {
        val unit=min(width,height);val landscape=width>height
        val top=centerY+radius*1.06f
        val stripHeight=if(strip)unit*.09f else 0f
        val clockY=if(landscape)height*.30f else if(strip)top+stripHeight+unit*.065f else centerY-radius*1.32f
        val dateY=if(landscape)clockY+unit*.055f else if(strip)clockY+unit*.035f else centerY-radius*1.16f
        val captionTop=if(strip){if(!landscape && clock)dateY+unit*.035f else top+stripHeight+unit*.025f}else centerY+radius*1.12f
        return PowerWallpaperLayout(top,stripHeight,clockY,dateY,captionTop,(height-unit*.045f-captionTop).coerceAtLeast(0f))
    }
}
