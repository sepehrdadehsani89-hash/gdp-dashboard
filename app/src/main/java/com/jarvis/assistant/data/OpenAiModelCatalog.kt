package com.jarvis.assistant.data

import com.jarvis.assistant.brain.ProviderFailureKind
import org.json.JSONObject

/** Routing registry, not an availability list. Only IDs returned by the credential's catalog are offered.
 * References: OpenAI Models/Responses documentation; unfamiliar explicit IDs still require real inference.
 * Old GPT-3.5/GPT-4 chat routes remain supported. Audio/image/search/embedding IDs are not chat defaults. */
object OpenAiModelCatalog {
    fun validateId(value:String):String {require(value.matches(Regex("[A-Za-z0-9._:-]{1,100}"))){"Use an exact provider model identifier"};return value}
    private fun base(id:String)=if(id.startsWith("ft:"))id.removePrefix("ft:").substringBefore(':')else id
    fun legacyChat(id:String):Boolean=base(id).let{it=="gpt-4" || it.startsWith("gpt-4-") || it.startsWith("gpt-3.5-")}
    fun chatCandidate(id:String):Boolean {
        if(!id.matches(Regex("[A-Za-z0-9._:-]{1,100}")))return false
        val name=base(id).lowercase(java.util.Locale.ROOT)
        if(listOf("audio","realtime","tts","transcrib","embedding","image","moderation","search").any{name.contains(it)})return false
        return name.startsWith("gpt-") || Regex("o[134](?:-|$).*").matches(name) || name in setOf("o1","o3","o4")
    }
    fun parse(raw:String):List<String>{
        val root=try{JSONObject(raw)}catch(_:org.json.JSONException){throw ProviderException("OpenAI returned an invalid API model catalog.",ProviderFailureKind.INCOMPLETE)}
        val data=root.optJSONArray("data") ?: throw ProviderException("OpenAI returned an invalid API model catalog.",ProviderFailureKind.INCOMPLETE)
        return (0 until minOf(data.length(),2000)).mapNotNull{(data.optJSONObject(it)?.opt("id") as? String)?.takeIf(::chatCandidate)}.distinct()
    }
    fun defaultFrom(actualIds:List<String>):String? {
        val candidates=actualIds.filter(::chatCandidate)
        return listOf("gpt-4.1-mini","gpt-4o-mini").firstOrNull{it in candidates} ?: candidates.firstOrNull{!legacyChat(it)} ?: candidates.firstOrNull()
    }
    fun capability(id:String)=if(legacyChat(id))"Text chat · legacy Chat Completions · inference unverified" else "Text candidate · Responses API · inference unverified"
}
