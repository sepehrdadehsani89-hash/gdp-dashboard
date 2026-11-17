package com.jarvis.assistant.hud

/** Adapt frame budgets without copying transient/system constraints into saved user preferences. */
object MotionSettingsPolicy {
    fun description(userReduced:Boolean,systemMotion:Boolean,powerSaver:Boolean,memoryRestricted:Boolean):String=buildList {
        add(if(userReduced)"User preference: Reduced Motion" else "User preference: Full Motion")
        if(!systemMotion)add("Android animations are disabled; enable system animations for full motion")
        if(powerSaver)add("Battery Saver reduces the frame budget")
        if(memoryRestricted)add("Temporary memory adaptation reduces visual detail; your motion preference is unchanged")
    }.joinToString(". ")
    fun project(saved:HudState,memoryRestricted:Boolean):HudState=saved
}
