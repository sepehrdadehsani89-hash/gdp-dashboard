package com.jarvis.assistant.neural

/** Explicit per-request local-only wording overrides saved cloud preferences. */
object ContextPrivacyPolicy {
    fun localOnly(input:String)=Regex("\\b(offline only|on device only|on-device only|keep this local|do not use cloud|don't use cloud|sin nube|solo sin conexion|solo sin conexión)\\b|فقط آفلاین|فقط روی گوشی|ابر استفاده نکن",RegexOption.IGNORE_CASE).containsMatchIn(input)
}
