package com.jarvis.assistant.sentinel

/** A known foreign foreground target wins over wallpaper visibility. Otherwise use notification. */
object ActivationPolicy {
    fun mayOpen(locked:Boolean,assistantVisible:Boolean,homeWallpaperVisible:Boolean,screenAccess:Boolean,activePackage:String?,launcherPackage:String?):Boolean {
        if(locked || assistantVisible)return false
        return if(screenAccess)launcherPackage!=null && activePackage==launcherPackage else homeWallpaperVisible
    }
}
