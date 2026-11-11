package com.jarvis.assistant.pickup

import com.jarvis.assistant.core.AppLanguage
import kotlin.math.*

/** Conservative lift gesture, not evidence of identity, attention or being awake. */
class PickupPolicy(private val now:()->Long){
    private var settledAt:Long?=null
    private var armed=false
    private var rest:FloatArray?=null
    private var movementAt:Long?=null
    private var previous:FloatArray?=null
    fun reset(){settledAt=null;armed=false;rest=null;movementAt=null;previous=null}
    fun sample(x:Float,y:Float,z:Float,eligible:Boolean,far:Boolean):Boolean {
        if(!eligible || !far || !listOf(x,y,z).all(Float::isFinite)){reset();return false}
        val current=floatArrayOf(x,y,z);val prior=previous;previous=current
        if(prior==null)return false
        val magnitude=sqrt(x*x+y*y+z*z)
        val delta=sqrt(current.indices.sumOf{(current[it]-prior[it]).toDouble().pow(2)}).toFloat()
        val steady=abs(magnitude-9.81f)<.65f && delta<.35f
        if(!armed){
            if(steady){val at=settledAt ?: now().also{settledAt=it};if(now()-at>=1500){armed=true;rest=current.copyOf()}}
            else settledAt=null
            return false
        }
        if(movementAt==null){
            if(delta>1.1f || abs(magnitude-9.81f)>1.4f)movementAt=now()
            return false
        }
        val elapsed=now()-requireNotNull(movementAt)
        if(elapsed !in 0..2000){reset();return false}
        val base=rest ?: return false
        val baseNorm=sqrt(base.sumOf{it.toDouble()*it})
        val cosine=current.indices.sumOf{current[it].toDouble()*base[it]}/(magnitude*baseNorm).coerceAtLeast(.001)
        if(elapsed>=120 && steady && cosine<.94){reset();return true}
        return false
    }
}
object PickupGreetings {
    private val en=listOf("Hello sir. Ready when you are. How can I help?","Welcome back, sir. What would you like to do?","At your service, sir. Is there anything you need?","Hello sir. I'm here if you'd like a hand.","Good to have you here, sir. What can I help with?","Ready to help, sir. What's on your mind?")
    private val es=listOf("Hola, señor. Estoy listo. ¿En qué puedo ayudarte?","Bienvenido, señor. ¿Qué te gustaría hacer?","A tu servicio, señor. ¿Necesitas algo?","Hola, señor. Estoy aquí si necesitas ayuda.","Qué gusto saludarte, señor. ¿En qué te ayudo?","Listo para ayudarte, señor. ¿Qué tienes en mente?")
    fun text(index:Int,language:AppLanguage)=(if(language==AppLanguage.ES)es else en)[Math.floorMod(index,6)]
}
class PickupPreferences(context:android.content.Context){
    private val p=context.getSharedPreferences("jarvis_pickup_v1",android.content.Context.MODE_PRIVATE)
    var enabled:Boolean get()=p.getBoolean("enabled",true);set(v){p.edit().putBoolean("enabled",v).apply()}
    var next:Int get()=p.getInt("next",0).coerceIn(0,5);set(v){p.edit().putInt("next",Math.floorMod(v,6)).apply()}
    var lastAt:Long get()=p.getLong("last_at",0);set(v){p.edit().putLong("last_at",v).apply()}
    fun coolingDown(now:Long)=lastAt>0 && (now<lastAt || now-lastAt<300_000L)
    fun clear(){p.edit().clear().apply()}
}
