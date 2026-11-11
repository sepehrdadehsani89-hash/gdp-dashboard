package com.jarvis.assistant.core

import java.util.Locale

object LanguageRules {
    fun confirmation(text: String): Boolean? = when (text.trim().lowercase(Locale.ROOT).trim('.', '!', '?', '؟')) {
        "yes", "confirm", "sí", "si", "confirmar", "بله", "آره", "تأیید", "تایید" -> true
        "no", "cancel", "cancela", "نه", "خیر", "لغو کن" -> false
        else -> null
    }
    fun detect(text: String, fallback: AppLanguage): AppLanguage {
        if (text.any { it in '\u0600'..'\u06ff' }) return if(fallback in AppLanguage.supported)fallback else AppLanguage.EN
        val lowered = text.lowercase(Locale.ROOT)
        if (Regex("\\b(hola|noticias|ayer|español|habla|recuerda|abre|qué|quiero|buenos|gracias|dime|por favor)\\b").containsMatchIn(lowered)) return AppLanguage.ES
        if (Regex("\\b(hey|hello|news|yesterday|remember|open|what|please|tell|english|speak)\\b").containsMatchIn(lowered)) return AppLanguage.EN
        return if(fallback in AppLanguage.supported)fallback else AppLanguage.EN
    }
    fun replyLanguage(text:String,selected:AppLanguage,selectedMode:Boolean)=if(selectedMode)selected else detect(text,selected)
    fun languageRequest(text: String): AppLanguage? {
        val normalized=com.jarvis.assistant.neural.AppMatching.normalize(text)
            .removePrefix("hey jarvis ").removePrefix("oye jarvis ").removePrefix("jarvis ")
            .removePrefix("please ").removePrefix("por favor ").removeSuffix(" please").removeSuffix(" por favor")
            .replace(Regex("^(?:(?:can|could|would|will) you(?: please)?|puedes(?: por favor)?|podrias(?: por favor)?) "),"")
        val spanish=setOf("spanish", "espanol")
        val english=setOf("english", "ingles")
        val pattern=Regex("^(?:change to|switch to|change language to|switch language to|change the language to|switch the language to|speak|speak in|speak to me in|answer in|answer me in|respond in|talk in|talk to me in|cambia a|cambia al|cambiar a|cambia el idioma a|habla|habla en|hablar en|responde en) (spanish|espanol|english|ingles)(?: mode)?$")
        val target=pattern.matchEntire(normalized)?.groupValues?.get(1)
            ?: Regex("^(spanish|espanol|english|ingles) (?:mode|modo)$").matchEntire(normalized)?.groupValues?.get(1)
            ?: Regex("^modo (espanol|ingles)$").matchEntire(normalized)?.groupValues?.get(1)
        return when {
            target in spanish->AppLanguage.ES
            target in english->AppLanguage.EN
            normalized in setOf("به اسپانیایی صحبت کن","اسپانیایی صحبت کن","به زبان اسپانیایی صحبت کن")->AppLanguage.ES
            normalized in setOf("به انگلیسی صحبت کن","انگلیسی صحبت کن","به زبان انگلیسی صحبت کن")->AppLanguage.EN
            else->null
        }
    }

    fun isStop(text: String): Boolean = text.trim().lowercase(Locale.ROOT).trim('.', '!', '?', '؟') in setOf(
        "stop", "stop everything", "jarvis stop", "cancel", "para", "detente", "cancela", "بس کن", "توقف", "لغو کن", "همه چیز را متوقف کن",
    )
}
