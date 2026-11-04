package com.jarvis.assistant.phantom

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PhantomSettings(val enabled:Boolean=false,val captions:Boolean=false,val opacity:Float=.94f,val scale:Float=1f,val lines:Int=3,val autoHide:Boolean=true,val editMode:Boolean=false,val x:Float=0f,val y:Float=0f,val glow:Float=.6f,val speed:Float=1f,val captionSp:Float=12f,val cardWidth:Float=210f,val reducedMotion:Boolean=false)
/** Non-secret presentation preferences; conversations never enter this store. */
class PhantomPreferences(context:Context) {
    private val p=context.getSharedPreferences("jarvis_phantom_presentation",Context.MODE_PRIVATE)
    private val mutable=MutableStateFlow(PhantomSettings(p.getBoolean("enabled",false),p.getBoolean("captions",false),p.getFloat("opacity",.94f).coerceIn(.65f,1f),p.getFloat("scale",1f).coerceIn(.8f,1.3f),p.getInt("lines",3).coerceIn(2,4),p.getBoolean("autoHide",true),false,p.getFloat("x",0f).coerceIn(0f,1f),p.getFloat("y",0f).coerceIn(0f,1f),p.getFloat("glow",.6f).coerceIn(0f,1f),p.getFloat("speed",1f).coerceIn(0f,2f),p.getFloat("captionSp",12f).coerceIn(10f,20f),p.getFloat("cardWidth",210f).coerceIn(160f,280f),p.getBoolean("reducedMotion",false)))
    val state=mutable.asStateFlow()
    fun save(s:PhantomSettings){
        val v=s.copy(opacity=s.opacity.coerceIn(.65f,1f),scale=s.scale.coerceIn(.8f,1.3f),lines=s.lines.coerceIn(2,4),x=s.x.coerceIn(0f,1f),y=s.y.coerceIn(0f,1f),glow=s.glow.coerceIn(0f,1f),speed=s.speed.coerceIn(0f,2f),captionSp=s.captionSp.coerceIn(10f,20f),cardWidth=s.cardWidth.coerceIn(160f,280f))
        p.edit().putBoolean("enabled",v.enabled).putBoolean("captions",v.captions).putFloat("opacity",v.opacity).putFloat("scale",v.scale).putInt("lines",v.lines).putBoolean("autoHide",v.autoHide).putFloat("x",v.x).putFloat("y",v.y).putFloat("glow",v.glow).putFloat("speed",v.speed).putFloat("captionSp",v.captionSp).putFloat("cardWidth",v.cardWidth).putBoolean("reducedMotion",v.reducedMotion).apply();mutable.value=v
    }
}
