package com.jarvis.assistant.widgets

object WidgetHostPolicy {
    const val HOST_ID=11203
    const val MAX_WIDGETS=16
    fun currentResult(expectedCode:Int,resultCode:Int,issuedSession:Long,currentSession:Long)=
        expectedCode>=0 && expectedCode==resultCode && issuedSession>=0 && issuedSession==currentSession
    /** IDs from another launcher/host, missing providers and malformed saved IDs are not access. */
    fun restored(saved:Set<String>,owned:Set<Int>,bound:(Int)->Boolean):List<Int> =
        saved.mapNotNull(String::toIntOrNull).filter{it>0 && it in owned && bound(it)}.distinct().sorted().take(MAX_WIDGETS)
}
