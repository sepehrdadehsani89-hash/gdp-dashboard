package com.jarvis.assistant.contacts

import com.jarvis.assistant.neural.AppMatching

data class SpokenContactAlias(val alias:String,val target:String)
data class PhoneContactCandidate(val id:Long,val name:String,val number:String,val reference:String)
/** Explicit local nickname requests only; never infer family relationships or select fuzzy matches. */
object SpokenContactPolicy {
    fun spokenKey(value:String):String {
        val n=AppMatching.normalize(value)
        val parts=n.split(' ')
        return if(parts.size in 2..8 && parts.all{it.length==1 && it[0] in 'a'..'z'})parts.joinToString("") else n
    }
    fun aliasRequest(input:String):SpokenContactAlias? {
        if(input.length !in 1..250 || input.any(Char::isISOControl))return null
        val text=input.trim().replace(Regex("^(?:hey jarvis|jarvis)[, ]+",RegexOption.IGNORE_CASE),"")
        val pair=Regex("^(?:remember(?: that)?|set contact (?:alias|nickname)) (.{1,80}?) (?:means|is|to) (?:contact )?(.{1,80})$",RegexOption.IGNORE_CASE).matchEntire(text)?.let{it.groupValues[1] to it.groupValues[2]}
            ?: Regex("^save (?:contact )?(.{1,80}) as (.{1,80})$",RegexOption.IGNORE_CASE).matchEntire(text)?.let{it.groupValues[2] to it.groupValues[1]}
            ?: return null
        val alias=spokenKey(pair.first);val target=pair.second.trim()
        if(alias.isBlank() || alias.length>80 || target.isBlank())return null
        return SpokenContactAlias(alias,target)
    }
    fun targetName(value:String)=value.replace(Regex("""\s+ending\s+[0-9]{2,4}$""",RegexOption.IGNORE_CASE),"").trim()
    fun ending(value:String)=Regex("""\s+ending\s+([0-9]{2,4})$""",RegexOption.IGNORE_CASE).find(value)?.groupValues?.get(1)
    fun unique(target:String,candidates:List<PhoneContactCandidate>):PhoneContactCandidate? {
        val suffix=ending(target)
        val exact=candidates.filter{AppMatching.normalize(it.name)==AppMatching.normalize(targetName(target)) && (suffix==null || it.number.filter(Char::isDigit).endsWith(suffix))}
        val safe=exact.mapNotNull{c->runCatching{c.copy(number=ContactShortcutPolicy.number(c.number))}.getOrNull()}.distinctBy{it.number}
        return safe.singleOrNull()
    }
    fun savedMatches(target:String,entries:List<ContactShortcut>):List<ContactShortcut> = entries.filter{entry->ContactShortcutPolicy.keys(entry).any{spokenKey(it)==spokenKey(target)}}.distinctBy{it.id}
    fun variants(alias:String)=if(alias.matches(Regex("[a-z]{2,8}")))listOf(alias.toCharArray().joinToString(" ")) else emptyList()
}
