package com.jarvis.assistant.brain

/** Diagnostic metadata only; callers must never supply credentials or conversation text. */
object ProviderDiagnostics {
    fun describe(config:ProviderConfig,connection:ProviderConnection?,credentialSaved:Boolean,internet:String,now:Long):String {
        val row=connection?.takeIf{it.config==config}
        val auth=if(!credentialSaved)"Not configured" else if(row?.failure==ProviderFailureKind.AUTHENTICATION)"Rejected" else "Configured (inference is a separate check)"
        return listOf(
            "Provider: ${config.provider}","Authentication: $auth",
            "Internet: $internet (Android network observation; provider reachability is separate)",
            "Backend: Not required · direct personal API access",
            "Requested model: ${config.model}","Resolved model: ${row?.resolvedModel ?: "Not recorded"}",
            "Connection: ${row?.diagnosticState ?: AiConnectionState.NOT_VERIFIED}",
            "Last verified: ${row?.verifiedAt?.let{java.util.Date(it).toString()} ?: "Never"}",
            "Last test duration: ${row?.elapsedMs?.let{"$it ms"} ?: "Not recorded"}",
            "Last error category: ${row?.failure ?: "None recorded"}",
            "HTTP status: ${row?.httpStatus ?: "Not recorded"}",
            "Retry: ${if(row?.retryAllowed(now)!=false)"Available" else "Wait until "+java.util.Date(row?.retryAt ?: now)}",
            "This screen does not perform a live request. Test Connection uses a small amount of API usage."
        ).joinToString("\n")
    }
}
