package com.jarvis.assistant.brain

import com.jarvis.assistant.core.ConnectionCheck
import com.jarvis.assistant.data.OpenAiModelCatalog
import com.jarvis.assistant.data.ProviderException

/** Explicit user action: a catalog alone can never change the active brain. No fallback on failure. */
internal object OpenAiDefaultSelection {
    suspend fun connect(
        catalog:suspend()->List<String>,
        configure:suspend(ProviderConfig)->Unit,
        verify:suspend(ProviderConfig)->ConnectionCheck,
        select:suspend(ProviderConfig)->Unit,
    ):Pair<ProviderConfig,ConnectionCheck> {
        val actual=catalog()
        val model=OpenAiModelCatalog.defaultFrom(actual) ?: throw ProviderException("OpenAI listed no suitable text model for this key. No default was selected.",ProviderFailureKind.MODEL_UNAVAILABLE)
        val config=ProviderConfig("openai",model).validated()
        configure(config)
        val result=verify(config)
        if(result.model!=model || result.sample.isBlank())throw ProviderException("OpenAI did not verify the selected default. Your active brain was preserved.",ProviderFailureKind.INCOMPLETE)
        select(config)
        return config to result
    }
}
