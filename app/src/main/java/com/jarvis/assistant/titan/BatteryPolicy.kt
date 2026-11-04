package com.jarvis.assistant.titan

import com.jarvis.assistant.core.AppLanguage

/** Percentage color is independent from listening, speaking and charging state. */
enum class BatteryBand(val argb:Long){ NORMAL(0xFFF8FAFC),MODERATE(0xFFFACC15),LOW(0xFFFB923C),VERY_LOW(0xFFEF4444),CRITICAL(0xFF991B1B),UNKNOWN(0xFF94A3B8) }
object BatteryPolicy {
    fun band(percent:Int?):BatteryBand=when(percent){
        in 70..100->BatteryBand.NORMAL;in 31..69->BatteryBand.MODERATE;in 11..30->BatteryBand.LOW
        in 6..10->BatteryBand.VERY_LOW;in 0..5->BatteryBand.CRITICAL;else->BatteryBand.UNKNOWN
    }
    fun quiet(hour:Int,start:Int,end:Int)=if(start==end)false else if(start<end)hour in start until end else hour>=start || hour<end
    fun phrase(percent:Int,language:AppLanguage):String {
        require(percent in 1..10)
        val en=listOf("zero","one","two","three","four","five","six","seven","eight","nine","ten")[percent]
        val es=listOf("cero","uno","dos","tres","cuatro","cinco","seis","siete","ocho","nueve","diez")[percent]
        return if(language==AppLanguage.ES){if(percent<=5)"Carga tu teléfono inmediatamente. El nivel crítico de batería es del $es por ciento." else "Carga tu teléfono, por favor. La batería está al $es por ciento."}
        else if(percent<=5)"Please charge your phone immediately. Critical battery percentage is at $en percent."
        else "Please charge your phone. Battery percentage is at $en percent."
    }
}
data class BatteryAlertState(val previous:Int?=null,val powered:Boolean=false,val announcedMask:Int=0,val episode:Long=0)
data class BatteryObservation(val state:BatteryAlertState,val candidate:Int?)
object BatteryAlertPolicy {
    fun observe(old:BatteryAlertState,percent:Int?,powered:Boolean):BatteryObservation {
        if(percent==null || percent !in 0..100)return BatteryObservation(old,null)
        val newEpisode=old.powered && !powered || percent>=15 && old.previous!=null && old.previous<15
        val mask=if(newEpisode || percent>=15)0 else old.announcedMask
        val state=BatteryAlertState(percent,powered,mask,old.episode+if(newEpisode)1 else 0)
        val candidate=percent.takeIf{!powered && old.previous!=null && it<old.previous && it in 1..10 && mask and (1 shl it)==0}
        return BatteryObservation(state,candidate)
    }
    /** Reserve before playback for at-most-once scheduling across process death, never evidence of audibility. */
    fun reserve(state:BatteryAlertState,level:Int)=state.copy(announcedMask=state.announcedMask or (1 shl level))
}
