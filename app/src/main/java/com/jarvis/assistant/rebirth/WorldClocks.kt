package com.jarvis.assistant.rebirth

import java.time.*
import java.time.format.DateTimeFormatter
import com.jarvis.assistant.core.*

object WorldClocks {
    val cities=linkedMapOf("Quito" to "America/Guayaquil","Tehran" to "Asia/Tehran","London" to "Europe/London","New York" to "America/New_York","Tokyo" to "Asia/Tokyo","Sydney" to "Australia/Sydney")
    private val request=Regex("^(?:what time is it in|what time in|time in|(?:que|qué) hora es en|hora en) (.+?)[?!.]?$",RegexOption.IGNORE_CASE)
    private fun clean(text:String)=text.trim().removePrefix("¿").replace(Regex("^(?:hey|oye) jarvis[, ]+",RegexOption.IGNORE_CASE),"").replace(Regex("^(?:please )?(?:tell me )?",RegexOption.IGNORE_CASE),"")
    fun isRequest(text:String)=request.matches(clean(text))
    fun time(instant:Instant,zone:String):String=instant.atZone(ZoneId.of(zone)).format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss XXX"))
    fun answer(text:String,instant:Instant=Instant.now()):AssistantReply? {
        val name=request.matchEntire(clean(text))?.groupValues?.get(1) ?: return null
        val normalized=com.jarvis.assistant.neural.AppMatching.normalize(name)
        val known=cities.entries.firstOrNull{com.jarvis.assistant.neural.AppMatching.normalize(it.key)==normalized}?.value
            ?: if(normalized in setOf("teheran","teherán"))"Asia/Tehran" else null
        val zone=known ?: runCatching{ZoneId.of(name).id}.getOrNull()
        return if(zone==null)AssistantReply("Name a supported city (${cities.keys.joinToString()}) or an exact IANA time-zone ID. No time zone was guessed.") else AssistantReply("${time(instant,zone)} · $zone · Android time-zone data.")
    }
}
