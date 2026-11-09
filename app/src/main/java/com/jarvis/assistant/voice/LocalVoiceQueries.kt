package com.jarvis.assistant.voice

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.neural.AppMatching
import java.time.Clock
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

enum class LocalVoiceQuery { TIME, DATE }
object LocalVoiceQueries {
    fun parse(text:String):LocalVoiceQuery?=when(AppMatching.normalize(text).removePrefix("hey jarvis ").removePrefix("oye jarvis ").removePrefix("jarvis ").removePrefix("please ")){
        "what time is it","what is the time","tell me the time","what s the time","time","what time","que hora es","dime la hora","hora actual"->LocalVoiceQuery.TIME
        "what date is it","what is the date","what is today s date","today s date","what day is it","what day is today","tell me the date","fecha de hoy","que dia es hoy","cual es la fecha"->LocalVoiceQuery.DATE
        else->null
    }
    fun readOnly(text:String)=parse(text)!=null || com.jarvis.assistant.power.PowerCommands.parse(text) in setOf(com.jarvis.assistant.power.PowerCommand.STATUS,com.jarvis.assistant.power.PowerCommand.TEMPERATURE)
    fun ordinaryQuestion(text:String):Boolean {
        val n=AppMatching.normalize(text)
        if(n.length !in 8..4000)return false
        if(!Regex("^(?:what|why|how|when|where|who|which|tell me|explain|que|por que|como|cuando|donde|quien|cual|explica|dime)\\b").containsMatchIn(n))return false
        // Confidence is advisory for ordinary questions, but never relax it for a
        // possible action, credential, recipient or numeric instruction.
        return !Regex("\\b(?:open|close|call|send|delete|erase|transfer|pay|buy|tap|type|lock|unlock|start|stop|set|remind|turn|switch|dial|message|publish|share|run|execute|password|passcode|pin|token|secret|api key|abre|cierra|llama|llamar|envia|enviar|borra|elimina|compra|paga|escribe|toca|bloquea|desbloquea|inicia|activa|apaga|enciende|contrasena)\\b|\\d").containsMatchIn(n)
    }
    fun answer(query:LocalVoiceQuery,language:AppLanguage,clock:Clock=Clock.systemDefaultZone(),use24Hour:Boolean=false):String {
        val now=ZonedDateTime.now(clock);val locale=Locale.forLanguageTag(language.tag)
        val value=if(query==LocalVoiceQuery.TIME)now.format(DateTimeFormatter.ofPattern(if(use24Hour)"HH:mm" else "h:mm a",locale))
            else now.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
        return if(language==AppLanguage.ES){if(query==LocalVoiceQuery.TIME)"Señor, son las $value." else "Señor, hoy es $value."}
            else if(query==LocalVoiceQuery.TIME)"Sir, it's $value." else "Sir, today is $value."
    }
}
