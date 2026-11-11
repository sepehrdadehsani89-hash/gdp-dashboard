package com.jarvis.assistant.sentinel

/** The state color and independent charging ring are shared by both renderers. */
object ReactorPalette {
    val cyan=0xFF00E5FF.toInt();val purple=0xFFAA7CFF.toInt();val thinking=0xFF6975FF.toInt();val blue=0xFF38BDF8.toInt()
    val green=0xFF38E58C.toInt();val warning=0xFFFFBF55.toInt()
    fun chargingAccent(connected:Boolean?,charging:Boolean,enabled:Boolean)=connected==true && charging && enabled
    fun core(mode:SentinelMode,accent:String="cyan"):Int=when(mode){
        SentinelMode.LISTENING,SentinelMode.ACTIVATING,SentinelMode.AUTHORIZATION_REQUIRED->purple
        SentinelMode.SPEAKING->blue
        SentinelMode.PROCESSING->thinking
        SentinelMode.ERROR->warning
        else->when(accent){"purple"->purple;"blue"->0xFF38BDF8.toInt();else->cyan}
    }
}
