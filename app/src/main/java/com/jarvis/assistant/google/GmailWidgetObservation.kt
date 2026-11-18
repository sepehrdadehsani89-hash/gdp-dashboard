package com.jarvis.assistant.google

data class GmailWidgetObservation(val account:String,val epoch:Long,val unread:Long,val at:Long){override fun toString()="GmailWidgetObservation(private data=redacted)"}
data class GoogleWidgetObservation(val account:String,val epoch:Long,val product:GoogleProduct,val summary:String,val at:Long){override fun toString()="GoogleWidgetObservation(product=$product, private data=redacted)"}
object GoogleWidgetPolicy {
    fun text(value:GoogleWidgetObservation?,product:GoogleProduct,account:String?,epoch:Long,granted:Boolean,now:Long,privateAllowed:Boolean):String {
        if(!privateAllowed)return "$product · private data hidden"
        if(!granted || account==null)return "$product · account/read authorization required"
        if(value==null || value.product!=product || value.account!=account || value.epoch!=epoch || now-value.at !in 0..300_000)return "$product · no current verified read\nOpen Connected Services to read this source"
        return value.summary.take(1200)+"\nSource read: ${java.util.Date(value.at)}"
    }
}
object GmailWidgetPolicy {
    fun text(value:GmailWidgetObservation?,account:String?,epoch:Long,granted:Boolean,now:Long,privateAllowed:Boolean):String {
        if(!privateAllowed)return "Gmail · private data hidden"
        if(!granted || account==null)return "Gmail · account/read authorization required"
        if(value==null || value.account!=account || value.epoch!=epoch || value.unread<0 || now-value.at !in 0..300_000)return "Gmail · no current verified unread count\nAsk Jarvis to check unread mail"
        return "Gmail unread · ${value.unread}\nActual authorized read · ${java.util.Date(value.at)}"
    }
}
