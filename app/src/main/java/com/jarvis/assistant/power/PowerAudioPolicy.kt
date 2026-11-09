package com.jarvis.assistant.power

object PowerAudioPolicy {
    fun blocked(quiet:Boolean,hour:Int,start:Int,end:Int,callPermission:Boolean,callIdle:Boolean,normalMode:Boolean,silent:Boolean,dnd:Boolean,ready:Boolean):String?=when {
        quiet && com.jarvis.assistant.titan.BatteryPolicy.quiet(hour,start,end)->"Suppressed by quiet hours"
        !callPermission->"Call-state permission needed for safe spoken alerts"
        !callIdle || !normalMode->"Deferred during a call or communication audio"
        silent || dnd->"Suppressed by Silent or Do Not Disturb"
        !ready->"Waiting for the current interaction to finish"
        else->null
    }
}
