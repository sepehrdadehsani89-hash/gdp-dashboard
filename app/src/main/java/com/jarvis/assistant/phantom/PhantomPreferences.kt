package com.jarvis.assistant.phantom

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PhantomSettings(val enabled:Boolean=false,val captions:Boolean=false,val opacity:Float=.94f,val scale:Float=1f,val lines:Int=3,val autoHide:Boolean=true,val editMode:Boolean=false,val x:Float=0f,val y:Float=0f)
/** Non-secret presentation preferences; conversations never enter this store. */
class PhantomPreferences(context:Context) {
    private val p=context.getSharedPreferences("jarvis_phantom_presentation",Context.MODE_PRIVATE)
    private val mutable=MutableStateFlow(PhantomSettings(p.getBoolean("enabled",false),p.getBoolean("captions",false),p.getFloat("opacity",.94f).coerceIn(.65f,1f),p.getFloat("scale",1f).coerceIn(.8f,1.3f),p.getInt("lines",3).coerceIn(2,4),p.getBoolean("autoHide",true),false,p.getFloat("x",0f).coerceIn(0f,1f),p.getFloat("y",0f).coerceIn(0f,1f)))
    val state=mutable.asStateFlow()
    fun save(s:PhantomSettings){
        val v=s.copy(opacity=s.opacity.coerceIn(.65f,1f),scale=s.scale.coerceIn(.8f,1.3f),lines=s.lines.coerceIn(2,4),x=s.x.coerceIn(0f,1f),y=s.y.coerceIn(0f,1f))
        p.edit().putBoolean("enabled",v.enabled).putBoolean("captions",v.captions).putFloat("opacity",v.opacity).putFloat("scale",v.scale).putInt("lines",v.lines).putBoolean("autoHide",v.autoHide).putFloat("x",v.x).putFloat("y",v.y).apply();mutable.value=v
    }
}
