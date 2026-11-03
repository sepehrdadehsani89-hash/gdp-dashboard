package com.jarvis.assistant

import android.content.ContentValues
import android.content.Intent
import android.provider.MediaStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.jarvis.assistant.neural.NeuralSettingsActivity
import com.jarvis.assistant.neural.PrivateMemoryBackup
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class PrivateMemoryImportUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun realFilePickerPreviewCancellationImportAndRetry(){
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val device=UiDevice.getInstance(instrumentation)
        val runtime=(context.applicationContext as JarvisApplication).runtime
        val original=runtime.store.settings.value
        val token=UUID.randomUUID().toString()
        val name="Jarvis-restore-fixture-$token.json"
        val payload=JSONObject().put("format","jarvis-neural-export-v1")
            .put("documents",JSONArray().put(JSONObject().put("id",token).put("title","UI restore fixture").put("kind","USER_DOCUMENT").put("text","Synthetic import interface fixture.").put("source","UI test").put("version","1").put("publication","2026").put("license","Private fixture")))
            .put("tasks",JSONArray()).put("approved_memories",JSONArray()).put("app_aliases",JSONObject()).toString()
        val id=PrivateMemoryBackup.parse(payload).documents.single().id
        val uri=context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,ContentValues().apply{
            put(MediaStore.MediaColumns.DISPLAY_NAME,name);put(MediaStore.MediaColumns.MIME_TYPE,"application/json");put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/")
        })!!
        context.contentResolver.openOutputStream(uri)!!.use{it.write(payload.toByteArray())}
        fun importButton(){
            compose.onNodeWithText("Import memories & approved documents").performScrollTo().assertIsDisplayed().performClick()
        }
        fun chooseFile(){
            if(!device.wait(Until.hasObject(By.text(name)),5_000)){
                val rootSelector=By.res("android:id/title").text("Downloads")
                if(!device.hasObject(rootSelector)){
                    val roots=device.wait(Until.findObject(By.desc("Show roots")),60_000) ?: device.findObject(By.desc("Open navigation drawer"))
                    assertNotNull("Android file picker navigation must be available",roots);roots!!.click()
                }
                val downloads=device.wait(Until.findObject(rootSelector),60_000)
                assertNotNull("Downloads root must be available",downloads);downloads!!.click()
            }
            val selected=device.wait(Until.findObject(By.text(name)),60_000)
            assertNotNull("Synthetic backup must be listed in Downloads",selected);selected!!.click()
            assertTrue("Consent preview must appear",device.wait(Until.hasObject(By.text("Import private backup?")),30_000))
        }
        try{
            runtime.stopEverything()
            runtime.store.updateSettings(original.copy(language=com.jarvis.assistant.core.AppLanguage.EN,onboardingComplete=true,speakReplies=false))
            compose.activityRule.scenario.onActivity{it.startActivity(Intent(it,NeuralSettingsActivity::class.java))}
            assertTrue(device.wait(Until.hasObject(By.text("JARVIS OMNISYSTEM 10.0 · BETA")),30_000))
            importButton();device.pressBack()
            assertFalse(runtime.knowledge.list().any{it.id==id})
            importButton();chooseFile()
            device.findObject(By.res("android:id/button2"))!!.click()
            assertFalse(runtime.knowledge.list().any{it.id==id})
            repeat(2){attempt->
                importButton();chooseFile();device.findObject(By.res("android:id/button1"))!!.click()
                val expected="Imported documents / memories / tasks / aliases: ${if(attempt==0)1 else 0} / 0 / 0 / 0. Aliases skipped: 0."
                compose.waitUntil(30_000){runCatching{compose.onAllNodesWithText(expected).fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)}
                compose.onNodeWithText(expected).performScrollTo().assertIsDisplayed()
                assertEquals(1,runtime.knowledge.list().count{it.id==id})
            }
            device.pressBack()
        } finally {
            runtime.knowledge.remove(id)
            runtime.store.updateSettings(original)
            context.contentResolver.delete(uri,null,null)
        }
    }
}
