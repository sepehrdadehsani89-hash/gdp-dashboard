package com.jarvis.assistant.notifications

import com.jarvis.assistant.neural.AppMatching
import com.jarvis.assistant.tools.ToolPolicy

enum class NotificationReadStatus { AVAILABLE,LOCKED,PERMISSION_REQUIRED,SOURCE_CONSENT_REQUIRED,DISCONNECTED,READ_FAILED }
data class NotificationPreview(val packageId:String,val title:String,val text:String,val at:Long,val secret:Boolean=false,val groupSummary:Boolean=false)
class NotificationSnapshot(val status:NotificationReadStatus,val previews:List<String>,val message:String){override fun toString()="NotificationSnapshot(status=$status, content=redacted)"}
object NotificationAwarenessPolicy {
    val sources=linkedMapOf("WhatsApp" to setOf("com.whatsapp","com.whatsapp.w4b"),"Instagram" to setOf("com.instagram.android"),"Gmail" to setOf("com.google.android.gm"),"SMS" to setOf("com.google.android.apps.messaging","com.android.mms"),"Messenger" to setOf("com.facebook.orca"),"Signal" to setOf("org.thoughtcrime.securesms"),"Telegram" to setOf("org.telegram.messenger"))
    fun source(raw:String):String? {val text=AppMatching.normalize(raw).removePrefix("hey jarvis ").removePrefix("jarvis ")
        return sources.keys.firstOrNull{label->val name=label.lowercase();text in setOf("catch me up on $name","catch me up on my $name","read my $name notifications","summarize my $name notifications","show my $name notifications")}}
    fun request(raw:String)=source(raw)!=null || AppMatching.normalize(raw).removePrefix("hey jarvis ").removePrefix("jarvis ") in setOf("catch me up on notifications","catch me up on my notifications","catch me up on my messages","catch me up on messages","summarize my messages","read my messages","resume mis mensajes","lee mis mensajes","پیام های من را خلاصه کن") || com.jarvis.assistant.tools.LocalIntentParser.isNotifications(raw)
    fun allowed(packageId:String,self:String,consented:Set<String>)=packageId in consented && packageId!=self && !ToolPolicy.protectedPackage(packageId)
    fun previews(rows:List<NotificationPreview>,self:String,consented:Set<String>,target:Set<String>?=null):List<NotificationPreview> = rows.asSequence()
        .filter{allowed(it.packageId,self,consented) && (target==null || it.packageId in target) && !it.secret && !it.groupSummary}
        .filter{it.title.isNotBlank() || it.text.isNotBlank()}.sortedByDescending{it.at}.take(8)
        .map{it.copy(title=ToolPolicy.sensitiveText(it.title).take(120),text=ToolPolicy.sensitiveText(it.text).take(220))}.toList()
    fun result(status:NotificationReadStatus,items:List<String> = emptyList()):NotificationSnapshot {
        val message=when(status){
            NotificationReadStatus.LOCKED->"Unlock with Android before reading notification previews."
            NotificationReadStatus.PERMISSION_REQUIRED->"Enable optional Android Notification access and choose permitted apps in Notification Awareness first."
            NotificationReadStatus.SOURCE_CONSENT_REQUIRED->"Choose this source in Notification Awareness. Android listener permission alone does not authorize Jarvis to read every app."
            NotificationReadStatus.DISCONNECTED->"Android's notification listener is not connected. No notification query succeeded; this does not mean you have no messages."
            NotificationReadStatus.READ_FAILED->"Current notifications could not be read. No empty inbox or unread count is claimed."
            NotificationReadStatus.AVAILABLE->if(items.isEmpty())"No readable active notification previews were returned for your permitted sources. This is not an unread message count or full message history."
                else "${items.size} readable active notification previews (bounded to8; not a total unread count or full message history):\n"+items.joinToString("\n")
        };return NotificationSnapshot(status,items,message)
    }
}
