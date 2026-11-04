package com.jarvis.assistant

import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.jarvis.assistant.security.*
import org.junit.*
import org.junit.Assert.*

/** Official system UI on a disposable emulator only; never uses a personal device credential. */
abstract class SentinelSecurityFlowBase {
    @get:Rule val compose=createAndroidComposeRule<SecuritySettingsActivity>()
    protected val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    protected val context get()=instrumentation.targetContext
    protected val device get()=UiDevice.getInstance(instrumentation)
    protected val prefs get()=ScreenSecurityPreferences(context)
    @Before fun prepare(){
        Assume.assumeTrue(Build.HARDWARE in setOf("ranchu","goldfish") && Build.MODEL.contains("SDK",true))
        compose.activityRule.scenario.onActivity{a->val r=(a.application as JarvisApplication).runtime;r.stopEverything();r.store.updateSettings(r.store.settings.value.copy(language=com.jarvis.assistant.core.AppLanguage.EN,speakReplies=false));r.hud.save(r.hud.state.value.settings.copy(reducedMotion=true))}
        prefs.resetVerification();compose.waitForIdle()
    }
    protected fun enable(){compose.onNodeWithText("Enable requests · authenticate once with Android").performScrollTo().performClick();assertTrue(device.wait(Until.hasObject(By.pkg("com.android.settings")),60_000))}
}
class SentinelNoCredentialFlowTest:SentinelSecurityFlowBase(){
    @Before fun noLock(){Assume.assumeFalse(prefs.secure())}
    @Test fun noScreenLockCannotEnableRequests(){compose.onNodeWithText("Enable requests · authenticate once with Android").performScrollTo().assertIsNotEnabled();assertFalse(prefs.enabled);compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)}
    @Test fun configurationOpensActualAndroidSettings(){compose.onNodeWithText("Configure device security in Android").performScrollTo().performClick();assertTrue(device.wait(Until.hasObject(By.pkg("com.android.settings")),60_000));device.pressBack();assertFalse(prefs.enabled)}
}
class SentinelCredentialFlowTest:SentinelSecurityFlowBase(){
    @Before fun requireFixture(){Assume.assumeTrue(prefs.secure())}
    @Test fun oneActualAndroidChallengeEnablesRequests(){
        enable();val entry=device.wait(Until.findObject(By.res("com.android.settings","password_entry")),60_000) ?: error("Android credential input missing")
        entry.setText("2468");device.pressEnter() // public synthetic emulator fixture, never production data
        compose.waitUntil(60_000){prefs.enabled};assertEquals(VerificationMode.ANDROID_SINGLE_CHALLENGE,prefs.mode)
        assertTrue(context.getSharedPreferences("jarvis_screen_security_v1",0).all.keys.all{it in setOf("verified_at","mode","voice_enabled","public_reactor","require_sensitive")})
    }
    @Test fun cancellationNeverEnablesRequests(){enable();device.pressBack();compose.waitForIdle();assertFalse(prefs.enabled)}
}
