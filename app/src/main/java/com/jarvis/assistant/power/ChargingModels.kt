package com.jarvis.assistant.power

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.hud.BatteryReading
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ceil

enum class PowerCondition { UNKNOWN, DISCHARGING, CHARGING, FULL, PLUGGED_NOT_CHARGING }

data class ChargingEstimate(val remainingMs:Long,val measuredAt:Long) {
    fun remaining(now:Long):Long? = (now-measuredAt).takeIf{it in 0..150_000L}?.let{(remainingMs-it).takeIf{left->left>0}}
}

data class PowerReading(
    val battery:BatteryReading=BatteryReading(),val condition:PowerCondition=PowerCondition.UNKNOWN,
    val connected:Boolean?=null,val estimate:ChargingEstimate?=null,val observedAt:Long=0,
    val voltageMv:Int?=null,val currentMicroAmps:Int?=null,
) {
    val verifiedFull get()=connected==true && battery.percent==100 && condition in setOf(PowerCondition.FULL,PowerCondition.CHARGING)
    val charging get()=condition==PowerCondition.CHARGING
    val stripVisible get()=connected==true
    companion object {
        fun condition(status:Int,plugged:Int?):PowerCondition=when {
            plugged==null || plugged<0 -> PowerCondition.UNKNOWN
            plugged==0 && status==3 -> PowerCondition.DISCHARGING
            plugged==0 -> PowerCondition.UNKNOWN
            status==2 -> PowerCondition.CHARGING
            status==5 -> PowerCondition.FULL
            status==3 || status==4 -> PowerCondition.PLUGGED_NOT_CHARGING
            else -> PowerCondition.UNKNOWN
        }
    }
}

/** Platform estimates only. An isolated large jump needs confirmation; unavailable clears stale data. */
class ChargingEstimateFilter {
    private var accepted:ChargingEstimate?=null
    private var pending:Long?=null
    fun update(raw:Long?,charging:Boolean,now:Long):ChargingEstimate? {
        if(!charging || raw==null || raw !in 1..604_800_000L){accepted=null;pending=null;return null}
        val previous=accepted?.remaining(now)
        if(previous!=null && abs(raw-previous)>maxOf(1_800_000L,previous/2)){
            if(pending?.let{abs(raw-it)<=maxOf(60_000L,it/10)}!=true){pending=raw;return accepted}
        }
        pending=null
        return ChargingEstimate(raw,now).also{accepted=it}
    }
}

object ChargingText {
    private val english=listOf("zero","one","two","three","four","five","six","seven","eight","nine","ten","eleven","twelve","thirteen","fourteen","fifteen","sixteen","seventeen","eighteen","nineteen")
    fun number(n:Int):String=when {
        n in 0..19 -> english[n]
        n in 20..99 -> listOf("","","twenty","thirty","forty","fifty","sixty","seventy","eighty","ninety")[n/10]+if(n%10==0)"" else "-"+english[n%10]
        else -> n.toString()
    }
    fun duration(ms:Long,spanish:Boolean=false):String {
        val minutes=ceil(ms.coerceAtLeast(0)/60_000.0).toInt().coerceAtLeast(1)
        val h=minutes/60;val m=minutes%60
        return if(spanish)listOfNotNull(if(h>0)"$h ${if(h==1)"hora" else "horas"}" else null,if(m>0)"$m ${if(m==1)"minuto" else "minutos"}" else null).joinToString(" y ")
        else listOfNotNull(if(h>0)"${number(h)} ${if(h==1)"hour" else "hours"}" else null,if(m>0)"${number(m)} ${if(m==1)"minute" else "minutes"}" else null).joinToString(" and ")
    }
    fun completion(remaining:Long,epoch:Long,zone:String):String {
        val z=runCatching{ZoneId.of(zone)}.getOrDefault(ZoneId.systemDefault())
        val today=Instant.ofEpochMilli(epoch).atZone(z);val end=Instant.ofEpochMilli(epoch+remaining).atZone(z)
        return DateTimeFormatter.ofPattern(if(end.toLocalDate()!=today.toLocalDate())"EEE d MMM · HH:mm" else "HH:mm").format(end)
    }
    fun status(r:PowerReading,spanish:Boolean)=when(r.condition){
        PowerCondition.CHARGING->if(spanish)"CARGANDO" else "SYSTEM CHARGING"
        PowerCondition.FULL->if(r.verifiedFull){if(spanish)"CARGA COMPLETA" else "FULLY CHARGED"}else if(spanish)"ANDROID INFORMA COMPLETA" else "ANDROID REPORTS FULL"
        PowerCondition.DISCHARGING->if(spanish)"DESCARGANDO" else "DISCHARGING"
        PowerCondition.PLUGGED_NOT_CHARGING->if(spanish)"CONECTADO · SIN CARGAR" else "PLUGGED IN · NOT CHARGING"
        PowerCondition.UNKNOWN->if(spanish)"ESTADO NO DISPONIBLE" else "BATTERY STATUS UNAVAILABLE"
    }
    fun strip(r:PowerReading,now:Long,spanish:Boolean=false):String {
        val level=r.battery.percent?.let{"$it%"} ?: "—"
        if(r.verifiedFull)return "${if(spanish)"CARGA COMPLETA" else "FULLY CHARGED"} · $level"
        val left=r.estimate?.remaining(now)
        val eta=if(r.charging && left!=null){val min=ceil(left/60_000.0).toInt().coerceAtLeast(1);"~${if(min>=60)"${min/60}H " else ""}${min%60}M"}
            else if(spanish)"ESTIMACIÓN NO DISPONIBLE" else "ESTIMATE UNAVAILABLE"
        return "${status(r,spanish)} · $level · $eta"
    }
    fun answer(r:PowerReading,now:Long,epoch:Long,zone:String,language:AppLanguage):String {
        val es=language==AppLanguage.ES
        val level=r.battery.percent?.let{if(es)"Batería al $it por ciento." else "Battery percentage is at ${number(it)} percent."} ?: if(es)"Porcentaje no disponible." else "Battery percentage is unavailable."
        if(r.verifiedFull)return if(es)"Señor, la batería está completamente cargada. Está al cien por ciento y lista para usar." else "Sir, your battery is fully charged. You're at one hundred percent and ready to go."
        val left=r.estimate?.remaining(now)
        val eta=if(r.charging && left!=null){if(es)"Carga completa en aproximadamente ${duration(left,true)}. Hora estimada: ${completion(left,epoch,zone)}." else "Estimated full charge in approximately ${duration(left)}. Estimated finish: ${completion(left,epoch,zone)}."}
            else if(r.charging){if(es)"Todavía no hay una estimación fiable del tiempo restante." else "A reliable full-charge estimate isn't available yet."}else ""
        val status=if(es)when(r.condition){PowerCondition.CHARGING->"Sistema cargando, señor.";PowerCondition.DISCHARGING->"El teléfono se está descargando.";PowerCondition.PLUGGED_NOT_CHARGING->"Conectado a la corriente, pero no está cargando.";else->"El estado de carga no está disponible."}
            else when(r.condition){PowerCondition.CHARGING->"System charging, sir.";PowerCondition.DISCHARGING->"Your phone is discharging.";PowerCondition.PLUGGED_NOT_CHARGING->"Power is connected, but Android reports that the battery is not charging.";else->"Charging status is unavailable."}
        return listOf(status,level,eta).filter{it.isNotBlank()}.joinToString(" ")
    }
}
