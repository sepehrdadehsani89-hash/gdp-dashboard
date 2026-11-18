package com.jarvis.assistant.farewell

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.neural.AppMatching

object FarewellIntent {
    private val phrases=setOf("goodbye","bye","bye bye","see you later","see you tomorrow","i have to go","i need to go","thank you i m done","thanks i m done","thank you im done","thanks im done","i m done","im done","i am done","okay i m done","ok i m done","okay im done","ok im done","that s it","thats it","that s all","thats all","okay that s enough","ok that s enough","that s enough","enough for now","end this conversation","end conversation","you can rest","stand down","good night","talk tomorrow","stop listening","i m done studying","i am done studying","i m done working","i am done working","i m leaving for work","i have to travel","adios","hasta luego","hasta manana","buenas noches","ya termine","termine","eso es todo","ya es suficiente","terminar conversacion","deja de escuchar","gracias he terminado","خداحافظ","خدا حافظ","فعلا خداحافظ","فعلا","شب بخیر","شب به خیر","دیگه کافیه","دیگه کاری ندارم","گفتگو را تمام کن","گفت و گو را تمام کن","گوش نده","تمام شد")
    fun normalize(raw:String)=AppMatching.normalize(raw).removePrefix("hey jarvis ").removePrefix("jarvis ")
        .removeSuffix(" jarvis").removeSuffix(" جارویس")
        .replace(Regex("^(thank you|thanks|gracias) jarvis "),"$1 ")
        .replace(Regex("^(ممنون|متشکرم) جارویس "),"$1 ")
    fun matches(raw:String):Boolean {
        val text=normalize(raw)
        return text in phrases || listOf("thank you ","thanks ","gracias ","ممنون ","متشکرم ").any{prefix->text.startsWith(prefix) && text.removePrefix(prefix) in phrases}
    }
}
enum class FarewellCategory { GENERAL,FORMAL,SHORT,FRIENDLY,EVENING,BEDTIME,STUDY,WORK,COMPUTER,REMINDER,CALENDAR,CALL,THANKS,RELAXED,FOCUS,TRAVEL,RETURN }
data class FarewellLine(val language:String,val category:FarewellCategory,val text:String)
class FarewellCatalog private constructor(val lines:List<FarewellLine>,private val pick:(Int)->Int){
    private val recent=ArrayDeque<String>()
    @Synchronized fun choose(language:AppLanguage,category:FarewellCategory,courtesy:Boolean):String {
        val candidates=lines.filter{it.language==language.tag.substringBefore('-') && it.category==category}.ifEmpty{lines.filter{it.language==language.tag.substringBefore('-') && it.category==FarewellCategory.GENERAL}}
        val available=candidates.filter{it.text !in recent}.ifEmpty{candidates}
        val selected=available[pick(available.size).mod(available.size)].text
        recent.addLast(selected);while(recent.size>8)recent.removeFirst()
        return (if(courtesy)when(language){AppLanguage.ES->"Por supuesto, señor. ";AppLanguage.FA->"حتماً، قربان. ";else->"Certainly, sir. "}else "")+selected
    }
    companion object {
        fun parse(text:String,pick:(Int)->Int={kotlin.random.Random.nextInt(it)}):FarewellCatalog {
            val rows=text.lineSequence().filter{it.isNotBlank() && !it.startsWith("#")}.map{line->val columns=line.split('|',limit=3);require(columns.size==3);val language=columns[0];require(language in setOf("en","es","fa"));val category=FarewellCategory.valueOf(columns[1]);val words=columns[2].trim();require(words.length in 4..240 && !words.any{it.isISOControl()});FarewellLine(language,category,words)}.toList()
            require(rows.size>=500 && rows.map{AppMatching.normalize(it.text)}.distinct().size==rows.size)
            for(language in setOf("en","es","fa"))for(category in FarewellCategory.entries)require(rows.count{it.language==language && it.category==category}>=10)
            return FarewellCatalog(rows,pick)
        }
        fun category(input:String,hour:Int,reminders:Boolean=false,calendar:Boolean=false,calls:Boolean=false,computer:Boolean=false):FarewellCategory {
            val text=FarewellIntent.normalize(input)
            return when {
                reminders->FarewellCategory.REMINDER;calendar->FarewellCategory.CALENDAR;calls->FarewellCategory.CALL
                text in setOf("good night","buenas noches","شب بخیر","شب به خیر")->FarewellCategory.BEDTIME
                text.contains("studying")->FarewellCategory.STUDY;text.contains("working")->FarewellCategory.WORK
                text.contains("travel") || text=="i m leaving for work"->FarewellCategory.TRAVEL
                text.contains("tomorrow") || text=="hasta manana"->FarewellCategory.RETURN
                text.startsWith("thank") || text=="gracias he terminado"->FarewellCategory.THANKS
                text=="stand down"->FarewellCategory.FORMAL;text=="you can rest"->FarewellCategory.RELAXED
                text in setOf("bye","adios","فعلا")->FarewellCategory.SHORT
                computer->FarewellCategory.COMPUTER;hour in 18..23->FarewellCategory.EVENING
                else->FarewellCategory.GENERAL
            }
        }
    }
}
/** A farewell is a barrier for delayed navigation, TTS and foreground-resume callbacks. */
class ConversationExitGate {
    var blocked=false;private set
    fun finish(){blocked=true}
    fun explicitStart(){blocked=false}
}
