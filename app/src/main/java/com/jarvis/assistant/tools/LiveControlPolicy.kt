package com.jarvis.assistant.tools

sealed class LiveRequest {
    data class Tap(val label: String) : LiveRequest()
    data class Edit(val text: String) : LiveRequest()
}

/** Only explicit user commands enter this path. Model replies never call it. */
object LiveControlPolicy {
    fun parse(input: String): LiveRequest? {
        val text = input.trim()
        val edit = Regex("^(?:type|replace draft with|escribe|بنویس) (.+)$", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).matchEntire(text)
        if (edit != null) return edit.groupValues[1].takeIf { it.length <= 2000 }?.let(LiveRequest::Edit)
        return Regex("^(?:tap|click|toca|pulsa|بزن روی) (.{1,100})$", RegexOption.IGNORE_CASE).matchEntire(text)
            ?.groupValues?.get(1)?.let(LiveRequest::Tap)
    }
    fun allowedLabel(label: String): Boolean {
        val value = ToolPolicy.normalize(label)
        return value.isNotBlank() && !Regex("send|submit|post|publish|pay|purchase|buy|order|delete|remove|confirm|approve|transfer|install|allow|grant|enviar|publicar|pagar|comprar|borrar|eliminar|aceptar|permitir|ارسال|بفرست|پرداخت|خرید|حذف|تأیید|تایید|اجازه").containsMatchIn(value)
    }
}
