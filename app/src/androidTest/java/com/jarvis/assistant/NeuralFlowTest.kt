package com.jarvis.assistant

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import androidx.work.WorkInfo
import com.jarvis.assistant.core.*
import com.jarvis.assistant.local.LocalBrain
import com.jarvis.assistant.neural.*
import com.jarvis.assistant.voice.WakeListenerService
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.UUID

class NeuralFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private lateinit var runtime:AssistantRuntime
    private lateinit var settings:NeuralSettings
    private lateinit var original:AssistantSettings
    @Before fun prepare(){compose.activityRule.scenario.onActivity{runtime=(it.application as JarvisApplication).runtime;runtime.stopEverything();settings=runtime.neuralPreferences.state.value;original=runtime.store.settings.value;runtime.store.updateSettings(AssistantSettings(onboardingComplete=true,speakReplies=false));runtime.neuralPreferences.save(NeuralSettings())};compose.waitUntil(30_000){runtime.state.value.settings.onboardingComplete}}
    @After fun restore(){compose.activityRule.scenario.onActivity{runtime.stopEverything();runtime.neuralPreferences.save(settings);runtime.store.updateSettings(original);BriefingScheduler.sync(context)}}
    private fun database(block:(KnowledgeIndex)->Unit){val name="neural-test-${UUID.randomUUID()}.db";val index=KnowledgeIndex(context,name);try{block(index)}finally{index.close();context.deleteDatabase(name)}}
    @Test fun actualFtsSearchesApprovedDocumentsAndPersianThenDeletesCopies(){database{index->
        val keyBefore=runtime.store.getApiKey("gemini")
        val id=index.index("Power notes","Unrelated opening. ".repeat(20)+"Power is energy per second. Joules measure energy. کتاب فارسی درباره انرژی و توان.")
        assertTrue(index.search("joules energy").any{it.document.id==id})
        assertTrue(index.search("كتاب فارسي").any{it.document.id==id})
        assertTrue(index.search("unrelatednonexistentterm").isEmpty())
        val exported=JSONObject(index.export());assertEquals(1,exported.getJSONArray("documents").length());assertFalse(exported.has("credentials"));assertTrue(keyBefore==runtime.store.getApiKey("gemini"))
        index.remove(id);assertTrue(index.search("joules energy").isEmpty());assertTrue(index.list().isEmpty())
    }}
    @Test fun signedPackCatalogIntegrityAndTransactionalInstallAreReal(){database{index->
        val pack=KnowledgePacks.catalog(context).first{it.id=="science"}
        val bytes=instrumentation.context.assets.open("neural/science-v1.json").use{it.readBytes()}
        val json=KnowledgePacks.parse(pack,bytes);val entries=json.getJSONArray("entries")
        index.installPack(pack,(0 until entries.length()).map{val e=entries.getJSONObject(it);e.getString("title") to e.getString("text")})
        assertEquals(entries.length(),index.list().size);assertTrue(index.search("hypotenuse").isNotEmpty());assertTrue(index.list().all{it.license=="CC0-1.0" && it.publication=="2026-11-02"})
        val altered=bytes.clone();altered[altered.lastIndex]=(altered.last()+1).toByte();assertTrue(runCatching{KnowledgePacks.parse(pack,altered)}.isFailure)
    }}
    @Test fun discoveredAppAliasesUseActualAvailablePackagesAndCanBeRevoked(){
        val registry=runtime.appRegistry;val self=registry.apps().first{it.packageId==context.packageName}
        val alias="neural fixture ${UUID.randomUUID().toString().take(6)}"
        try{registry.saveAlias(alias,self.packageId);registry.invalidate();assertEquals(self.packageId,AppMatching.match(alias,registry.apps()).first().app.packageId);assertTrue(runCatching{registry.saveAlias("bad fixture","not.an.installed.package")}.isFailure)}finally{registry.removeAlias(alias)}
        assertFalse(alias in registry.aliases());assertFalse(WakeListenerService.state.value.running)
    }
    @Test fun optInWorkIsActuallyEnqueuedAndDisableCancelsItWithoutMic(){
        val work=WorkManager.getInstance(context)
        runtime.neuralPreferences.save(NeuralSettings(briefing=true,briefingWeather=false,briefingNews=false))
        BriefingScheduler.sync(context)
        compose.waitUntil(30_000){work.getWorkInfosForUniqueWork("jarvis_opt_in_briefing_v4").get().any{it.state==WorkInfo.State.ENQUEUED}}
        assertFalse(WakeListenerService.state.value.running)
        runtime.neuralPreferences.save(NeuralSettings(briefing=false));BriefingScheduler.sync(context)
        compose.waitUntil(30_000){work.getWorkInfosForUniqueWork("jarvis_opt_in_briefing_v4").get().all{it.state.isFinished}}
    }
    @Test fun briefingUsesRealLocalTasksWithNetworkOptionsOff(){
        val text="Neural task fixture ${UUID.randomUUID()}"
        runBlocking(kotlinx.coroutines.Dispatchers.IO){runtime.knowledge.addTask(text)}
        runtime.neuralPreferences.save(NeuralSettings(briefingWeather=false,briefingNews=false,briefingCalendar=false,briefingTasks=true))
        try{val reply=runBlocking{runtime.briefing.compose()};assertTrue(reply.text.contains(text));assertTrue(reply.text.contains(java.time.ZonedDateTime.now().year.toString()));assertTrue(reply.sources.isEmpty());assertFalse(reply.allowCloudSpeech);assertFalse(WakeListenerService.state.value.running)}finally{runBlocking(kotlinx.coroutines.Dispatchers.IO){runtime.knowledge.tasks().filter{it.second==text}.forEach{runtime.knowledge.completeTask(it.first)}}}
    }
    @Test fun neuralSettingsEntryWorksAndDoesNotStartListening(){
        compose.onNodeWithText("Settings",useUnmergedTree=true).performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Neural Core · brain, knowledge & briefings"))
        compose.onNodeWithText("Neural Core · brain, knowledge & briefings").performClick()
        compose.waitUntil(30_000){runCatching{compose.onAllNodesWithText("JARVIS SENTINEL ASCENSION 10.4 · BETA").fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)}
        compose.onNodeWithText("JARVIS SENTINEL ASCENSION 10.4 · BETA").assertIsDisplayed()
        assertFalse(WakeListenerService.state.value.running)
        compose.onNodeWithText("Back to Jarvis").performClick()
    }
    @Test fun spanishAndPersianNeuralControlsNavigateWithoutMicrophone(){
        for(lang in listOf(AppLanguage.ES,AppLanguage.FA)){
            compose.runOnUiThread{runtime.setLanguage(lang);compose.activity.startActivity(Intent(compose.activity,NeuralSettingsActivity::class.java))}
            val back=lang.choose("Back to Jarvis","Volver a Jarvis","بازگشت به جارویس")
            compose.waitUntil(30_000){runCatching{compose.onAllNodesWithText(back).fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)}
            compose.onNodeWithText(NeuralStrings.text(lang,"Search offline")).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(back).performScrollTo().performClick()
            assertFalse(WakeListenerService.state.value.running)
        }
    }
    @Test fun privateBriefingPreviewNeverUsesConfiguredCloudVoice(){
        compose.runOnUiThread{runtime.store.updateSettings(runtime.store.settings.value.copy(useCloudVoice=true,allowLockedChat=true));runtime.speakPrivatePreview("Private briefing fixture. This stays on the device.")}
        assertEquals("on-device",runtime.lastSpeechDestination)
        assertFalse(WakeListenerService.state.value.running)
        compose.runOnUiThread{runtime.stopEverything()}
    }
    @Test fun nativeRuntimeRejectsCorruptWeightsAndReleasesItsHandle(){
        val file=File(context.cacheDir,"invalid-neural-${UUID.randomUUID()}.gguf").apply{writeText("This is a deliberately corrupt GGUF fixture, not model weights.")}
        val brain=LocalBrain(context)
        val cls=LocalBrain::class.java
        val create=cls.getDeclaredMethod("nativeCreate",String::class.java,Int::class.javaPrimitiveType,Int::class.javaPrimitiveType).apply{isAccessible=true}
        val destroy=cls.getDeclaredMethod("nativeDestroy",Long::class.javaPrimitiveType).apply{isAccessible=true}
        val prepare=cls.getDeclaredMethod("nativePrepare",Long::class.javaPrimitiveType).apply{isAccessible=true}
        val generate=cls.getDeclaredMethod("nativeGenerate",Long::class.javaPrimitiveType,ByteArray::class.java,Int::class.javaPrimitiveType,LocalBrain.TokenReceiver::class.java).apply{isAccessible=true}
        val handle=create.invoke(brain,file.absolutePath,1536,1) as Long
        try{
            prepare.invoke(brain,handle)
            val failure=runCatching{generate.invoke(brain,handle,"fixture".toByteArray(),8,object:LocalBrain.TokenReceiver{override fun onToken(bytes:ByteArray){fail("Corrupt model must not emit tokens")}})}.exceptionOrNull()
            assertNotNull(failure);assertTrue(failure?.cause is IllegalStateException)
        }finally{destroy.invoke(brain,handle);file.delete()}
    }
}
