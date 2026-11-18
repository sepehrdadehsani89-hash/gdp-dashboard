package com.jarvis.assistant.notifications

import android.content.Context
import com.jarvis.assistant.tools.ToolPolicy

class NotificationSourcePreferences(context:Context){
    private val preferences=context.applicationContext.getSharedPreferences("jarvis_notification_sources",Context.MODE_PRIVATE)
    fun allowed():Set<String> = preferences.getStringSet("allowed",emptySet()).orEmpty().filterNot{ToolPolicy.protectedPackage(it)}.toSet()
    fun set(packageId:String,enabled:Boolean){require(packageId.matches(Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+")) && !ToolPolicy.protectedPackage(packageId));val next=allowed().toMutableSet().apply{if(enabled)add(packageId)else remove(packageId)};check(preferences.edit().putStringSet("allowed",next).commit());com.jarvis.assistant.tools.JarvisNotificationService.changed()}
    fun clear(){check(preferences.edit().clear().commit());com.jarvis.assistant.tools.JarvisNotificationService.changed()}
}
