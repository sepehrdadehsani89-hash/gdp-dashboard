package com.jarvis.assistant.companion

import com.jarvis.assistant.neural.AppMatching

/** Unimplemented computer actions must not fall through to invented AI execution or telemetry. */
object DesktopUnsupportedActions {
    fun matches(raw:String):Boolean {
        if(CompanionPolicy.parse(raw)!=null)return false
        val text=AppMatching.normalize(raw).removePrefix("hey jarvis ").removePrefix("jarvis ").removePrefix("please ")
        val target=Regex("\\b(?:computer|pc|desktop|windows)\\b").containsMatchIn(text)
        if(!target)return false
        return Regex("^(?:shut down|shutdown|restart|reboot|sleep|lock|unlock|open|close|mute|unmute|pause|play|stop|adjust|increase|decrease|read|show|access|transfer|copy|delete) .+$").matches(text) ||
            Regex("^(?:what is|what s|whats|tell me) (?:my|the) (?:paired )?(?:computer|pc|desktop)(?: s)? (?:status|cpu|ram|memory|battery|telemetry|volume).*$").matches(text) ||
            Regex("^how (?:much|many) .+ does my (?:computer|pc|desktop) have.*$").matches(text)
    }
    const val MESSAGE="UNSUPPORTED · This companion currently supports reviewed Chrome opening and search only. Native computer power, media, file, app and telemetry actions are not implemented. No computer action or status read was performed. Open Paired Computers for actual authenticated capability checks."
}
