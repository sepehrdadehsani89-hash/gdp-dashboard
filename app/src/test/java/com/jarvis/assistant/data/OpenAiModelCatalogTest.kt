package com.jarvis.assistant.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Catalog eligibility is distinct from inference access. These are simulated provider catalogs. */
class OpenAiModelCatalogTest {
    private fun catalog(vararg ids:String)=JSONObject().put("data",JSONArray().apply{ids.forEach{put(JSONObject().put("id",it))}}).toString()
    @Test fun retainsOnlyIdsActuallyReturned(){assertEquals(listOf("gpt-4.1-mini"),OpenAiModelCatalog.parse(catalog("gpt-4.1-mini")))}
    @Test fun neverInventsDefaultWhenCatalogEmpty(){assertNull(OpenAiModelCatalog.defaultFrom(emptyList()))}
    @Test fun preferredSmallTextModelMustBeListed(){assertEquals("o3",OpenAiModelCatalog.defaultFrom(listOf("o3")))}
    @Test fun preferredModelCanBeSelectedFromActualCatalog(){assertEquals("gpt-4.1-mini",OpenAiModelCatalog.defaultFrom(listOf("gpt-4o-mini","gpt-4.1-mini")))}
    @Test fun preservesDatedModelsExactly(){assertEquals(listOf("gpt-4.1-mini-2025-04-14"),OpenAiModelCatalog.parse(catalog("gpt-4.1-mini-2025-04-14")))}
    @Test fun deduplicatesWithoutChangingProviderOrder(){assertEquals(listOf("gpt-z","gpt-a"),OpenAiModelCatalog.parse(catalog("gpt-z","gpt-z","gpt-a")))}
    @Test fun filtersEmbeddingModels(){assertTrue(OpenAiModelCatalog.parse(catalog("text-embedding-3-small")).isEmpty())}
    @Test fun filtersImageGenerationModels(){assertTrue(OpenAiModelCatalog.parse(catalog("gpt-image-1")).isEmpty())}
    @Test fun filtersSpeechSynthesisModels(){assertTrue(OpenAiModelCatalog.parse(catalog("gpt-4o-mini-tts")).isEmpty())}
    @Test fun filtersTranscriptionModels(){assertTrue(OpenAiModelCatalog.parse(catalog("gpt-4o-transcribe")).isEmpty())}
    @Test fun filtersRealtimeModels(){assertTrue(OpenAiModelCatalog.parse(catalog("gpt-realtime")).isEmpty())}
    @Test fun filtersAudioOnlyModels(){assertTrue(OpenAiModelCatalog.parse(catalog("gpt-4o-audio-preview")).isEmpty())}
    @Test fun filtersSearchSpecificModels(){assertTrue(OpenAiModelCatalog.parse(catalog("gpt-4o-search-preview")).isEmpty())}
    @Test fun rejectsPathInjection(){assertTrue(OpenAiModelCatalog.parse(catalog("gpt-../../secret")).isEmpty())}
    @Test fun rejectsControlCharacterInId(){assertTrue(OpenAiModelCatalog.parse(catalog("gpt-good\r\nX-Header:secret")).isEmpty())}
    @Test fun rejectsNonStringIdsInsteadOfCoercingThem(){assertTrue(OpenAiModelCatalog.parse("""{"data":[{"id":42},{"id":null}]}""").isEmpty())}
    @Test fun planCatalogCannotStandInForApiCatalog(){assertTrue(runCatching{OpenAiModelCatalog.parse("""{"models":[{"slug":"gpt-4.1-mini"}]}""")}.exceptionOrNull() is ProviderException)}
    @Test fun malformedCatalogFailsInsteadOfFabricatingChoices(){assertTrue(runCatching{OpenAiModelCatalog.parse("not JSON")}.exceptionOrNull() is ProviderException)}
    @Test fun legacyGpt4RetainsWorkingChatRoute(){assertTrue(OpenAiModelCatalog.legacyChat("gpt-4-turbo"));assertTrue(OpenAiModelCatalog.legacyChat("gpt-4"))}
    @Test fun gpt41UsesResponsesRoute(){assertFalse(OpenAiModelCatalog.legacyChat("gpt-4.1-mini"))}
    @Test fun gpt4oUsesResponsesRoute(){assertFalse(OpenAiModelCatalog.legacyChat("gpt-4o-mini"))}
    @Test fun reasoningFamiliesAreCandidatesRequiringInference(){assertEquals(listOf("o1","o3-mini","o4-mini"),OpenAiModelCatalog.parse(catalog("o1","o3-mini","o4-mini")))}
    @Test fun fineTunedLegacyModelRetainsLegacyRoute(){assertTrue(OpenAiModelCatalog.legacyChat("ft:gpt-3.5-turbo:org:model:id"))}
    @Test fun fineTunedModernModelRetainsExactId(){val id="ft:gpt-4.1-mini:org:model:id";assertEquals(listOf(id),OpenAiModelCatalog.parse(catalog(id)));assertFalse(OpenAiModelCatalog.legacyChat(id))}
    @Test fun catalogDoesNotClaimInferenceIsVerified(){assertTrue(OpenAiModelCatalog.capability("gpt-4.1-mini").contains("inference unverified"))}
    @Test fun allExcludedCatalogHasNoDefault(){assertNull(OpenAiModelCatalog.defaultFrom(listOf("gpt-image-1","gpt-realtime")))}
    @Test fun unknownProviderModelsAreNotOfferedAsOpenAiChat(){assertTrue(OpenAiModelCatalog.parse(catalog("claude-sonnet-4","gemini-2.5-flash")).isEmpty())}
    @Test fun unreasonablyLongModelIdIsRejected(){assertTrue(runCatching{OpenAiModelCatalog.validateId("gpt-"+"x".repeat(100))}.isFailure)}
}
