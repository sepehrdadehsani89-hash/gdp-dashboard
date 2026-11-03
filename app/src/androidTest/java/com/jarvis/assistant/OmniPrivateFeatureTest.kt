package com.jarvis.assistant

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jarvis.assistant.omnisystem.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class OmniPrivateFeatureTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    @Test fun featureVaultEncryptsAndBindsNamesAndCanBeClearedWithoutOldStoreChanges(){
        val alias="jarvis_omni_test_"+UUID.randomUUID().toString()
        val vault=FeatureVault(context,alias)
        val prefs=context.getSharedPreferences(alias,Context.MODE_PRIVATE)
        val fixture="fixture-value-not-a-real-credential"
        try{
            vault.write("test_token",fixture)
            assertEquals(fixture,vault.read("test_token"))
            val raw=prefs.getString("test_token",null)!!
            assertFalse(raw.contains(fixture))
            prefs.edit().putString("other_token",raw).commit()
            assertNull(vault.read("other_token")) // GCM AAD prevents moving ciphertext under another field.
            vault.remove("test_token");assertNull(vault.read("test_token"))
            vault.write("test_token",fixture);vault.clear();assertFalse(vault.contains("test_token"))
            vault.write("test_token","new-fixture");assertEquals("new-fixture",vault.read("test_token"))
        }finally{vault.clear();context.deleteSharedPreferences(alias)}
    }
    @Test fun stopDuringCaptureCannotResumeListeningOrResetPausedState(){
        val app=context.applicationContext as JarvisApplication
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync{
            val owner=Any();assertTrue(app.runtime.beginExclusiveCapture(owner))
            app.runtime.stopEverything();app.runtime.endExclusiveCapture(owner)
            assertEquals(com.jarvis.assistant.core.AssistantPhase.PAUSED,app.runtime.state.value.phase)
            assertFalse(app.runtime.state.value.wakeEnabled)
        }
    }
    @Test fun systemCredentialPrivacyPauseBlocksNewCaptureAndPersistsOnlyANonSecretFlag(){
        val runtime=(context.applicationContext as JarvisApplication).runtime
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync{
            val owner=Any()
            try{
                com.jarvis.assistant.security.SystemCredentialPrivacy.begin(context)
                assertTrue(context.getSharedPreferences("jarvis_system_credential_privacy_v1",0).getBoolean("pending",false))
                assertFalse(runtime.beginExclusiveCapture(owner));runtime.listen();assertFalse(runtime.state.value.audio.recording)
                com.jarvis.assistant.security.SystemCredentialPrivacy.end(context)
                assertTrue(runtime.beginExclusiveCapture(owner));assertTrue(runtime.captureIsCurrent(owner))
                runtime.stopEverything();assertFalse(runtime.captureIsCurrent(owner));runtime.endExclusiveCapture(owner)
                assertEquals(com.jarvis.assistant.core.AssistantPhase.PAUSED,runtime.state.value.phase)
            }finally{com.jarvis.assistant.security.SystemCredentialPrivacy.end(context);runtime.abandonExclusiveCapture(owner)}
        }
    }
    @Test fun genuineOptionalSpeakerModelRunsThroughAndroidNativeApi()=runBlocking {
        val model=File(context.filesDir,"speaker-profiles/${SpeakerProfiles.FILE}")
        assumeTrue("Optional verified model must be provisioned explicitly on the test emulator",model.isFile)
        val alias="jarvis_omni_test_"+UUID.randomUUID().toString();val vault=FeatureVault(context,alias)
        val profiles=SpeakerProfiles(context,vault)
        val bytes=ByteArray(16000*3*2)
        for(i in 0 until bytes.size/2){val value=(6000*sin(2*Math.PI*170*i/16000)+2000*sin(2*Math.PI*340*i/16000)).toInt();bytes[i*2]=value.toByte();bytes[i*2+1]=(value shr 8).toByte()}
        try{
            val vector=profiles.embedding(CapturedVoice(bytes,16000))
            assertEquals(256,vector.size);assertTrue(vector.all{it.isFinite()});assertTrue(vector.any{it!=0f})
            vector.fill(0f)
        }finally{bytes.fill(0);profiles.release();vault.clear();context.deleteSharedPreferences(alias)}
    }
}
