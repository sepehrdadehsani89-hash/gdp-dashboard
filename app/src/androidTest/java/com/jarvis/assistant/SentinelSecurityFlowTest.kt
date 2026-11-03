package com.jarvis.assistant

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.security.*
import org.junit.*
import org.junit.Assert.*

/** Disposable emulator only. The known test credential is never used by production code or a personal device. */
abstract class SentinelSecurityFlowBase {
    @get:Rule val compose=createAndroidComposeRule<SecuritySettingsActivity>()
    protected val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    protected val context get()=instrumentation.targetContext
    protected val device get()=UiDevice.getInstance(instrumentation)
    protected val preferences get()=ScreenSecurityPreferences(context)
    @Before fun prepare(){
        Assume.assumeTrue("Only a disposable Android SDK emulator",Build.HARDWARE in setOf("ranchu","goldfish") && Build.MODEL.contains("SDK",true))
        compose.activityRule.scenario.onActivity{activity->
            val runtime=(activity.application as JarvisApplication).runtime
            runtime.stopEverything();runtime.hud.save(runtime.hud.state.value.settings.copy(reducedMotion=true,weatherCity=""))
            runtime.store.updateSettings(runtime.store.settings.value.copy(language=AppLanguage.EN,speakReplies=false))
        }
        compose.waitForIdle()
    }
    @After fun stop(){instrumentation.runOnMainSync{(context.applicationContext as JarvisApplication).runtime.stopEverything()}}
    protected fun visible(text:String)=runCatching{compose.onAllNodesWithText(text,substring=true).fetchSemanticsNodes().isNotEmpty()}.getOrDefault(false)
    protected fun waitText(text:String){compose.waitUntil(60_000){visible(text)}}
    protected fun click(text:String){compose.onNodeWithText(text,substring=true).performScrollTo().performClick()}
    protected fun start(){click("Configure voice unlock · start verification");waitText("STEP 1 OF 3")}
    protected fun challenge(){click("Verify with Android");assertTrue("Android-owned credential UI must open",device.wait(Until.hasObject(By.pkg("com.android.settings")),60_000))}
    protected fun enterFixture(pin:String=TEST_EMULATOR_PIN){
        val input=device.wait(Until.findObject(By.res("com.android.settings","password_entry")),60_000)
            ?: device.findObject(By.res("com.android.systemui","pinEntry"))
        if(input!=null){input.click();input.setText(pin);device.pressEnter()}
        else{pin.forEach{device.pressKeyCode(KeyEvent.KEYCODE_0+(it-'0'))};device.pressEnter()}
    }
    protected fun passStage(next:String){challenge();enterFixture();waitText(next)}
    protected fun cancelAndroid(){repeat(3){if(device.currentPackageName!=context.packageName){device.pressBack();device.waitForIdle(1000)}}}
    companion object {
        // Public synthetic fixture for the isolated emulator; never a user's credential.
        const val TEST_EMULATOR_PIN="2468"
    }
}
class SentinelNoCredentialFlowTest:SentinelSecurityFlowBase(){
    @Before fun noCredential(){Assume.assumeFalse("Run before the host configures the synthetic emulator credential",preferences.secure())}
    @Test fun absentSecureLockDoesNotPretendVerificationSucceeded(){
        waitText("NOT CONFIGURED")
        compose.onNodeWithText("Configure voice unlock · start verification").performScrollTo().assertIsNotEnabled()
        assertFalse(preferences.enabled);assertEquals(0L,preferences.verifiedAt)
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        assertFalse((context.applicationContext as JarvisApplication).runtime.state.value.audio.recording)
    }
    @Test fun configureButtonUsesActualAndroidSecuritySettings(){
        click("Configure device security in Android")
        assertTrue(device.wait(Until.hasObject(By.pkg("com.android.settings")),60_000));device.pressBack()
        waitText("JARVIS SCREEN CONTROL");assertFalse(preferences.enabled)
    }
}
class SentinelCredentialFlowTest:SentinelSecurityFlowBase(){
    @Before fun requireFixture(){Assume.assumeTrue("Host must configure only the synthetic emulator credential",preferences.secure());preferences.resetVerification()}
    @Test fun threeRealSystemChallengesPersistOnlyNonSecretSetupMetadata(){
        start();passStage("STEP 2 OF 3");passStage("STEP 3 OF 3");passStage("Setup state: VERIFIED")
        assertTrue(preferences.enabled);assertTrue(preferences.verifiedAt>0);assertEquals(VerificationMode.THREE_ANDROID_CHALLENGES,preferences.mode)
        val data=context.getSharedPreferences("jarvis_screen_security_v1",0).all
        assertTrue(data.keys.all{it in setOf("verified_at","mode","voice_enabled","public_reactor","require_sensitive")})
        assertFalse(data.values.any{it.toString()==SentinelSecurityFlowBase.TEST_EMULATOR_PIN})
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        val manager=context.getSystemService(KeyguardManager::class.java);assertFalse(manager.isDeviceLocked)
        assertTrue(ScreenSecurityController(context).requestUnlock().contains("already unlocked"))
    }
    @Test fun cancelSecondSystemChallengeDisablesSetup(){
        start();passStage("STEP 2 OF 3");challenge();cancelAndroid();waitText("Verification not completed")
        assertFalse(preferences.enabled);assertEquals(0L,preferences.verifiedAt)
    }
    @Test fun cancelThirdSystemChallengeDisablesSetup(){
        start();passStage("STEP 2 OF 3");passStage("STEP 3 OF 3");challenge();cancelAndroid();waitText("Verification not completed")
        assertFalse(preferences.enabled);assertEquals(0L,preferences.verifiedAt)
    }
    @Test fun incorrectCredentialNeverAdvancesTheApp(){
        start();challenge();enterFixture("0000")
        assertTrue(device.wait(Until.hasObject(By.textContains("Wrong")),30_000))
        assertFalse(preferences.enabled);assertEquals(0L,preferences.verifiedAt)
        cancelAndroid();waitText("Verification not completed")
    }
    @Test fun stopInvalidatesEvenALaterSuccessfulAndroidCallback(){
        start();challenge()
        instrumentation.runOnMainSync{(context.applicationContext as JarvisApplication).runtime.stopEverything()}
        enterFixture();waitText("Verification not completed")
        assertFalse(preferences.enabled);assertEquals(0L,preferences.verifiedAt)
    }
    @Test fun fallbackLabelsTwoConfirmationsWithoutInventingAndroidResults(){
        compose.onAllNodes(isToggleable()).onFirst().performScrollTo().performClick()
        start();passStage("STEP 2 OF 3")
        assertFalse(preferences.enabled)
        click("Confirm setup understanding · not another authentication");waitText("STEP 3 OF 3")
        click("Confirm setup understanding · not another authentication");waitText("Setup state: VERIFIED")
        assertTrue(preferences.enabled);assertEquals(VerificationMode.ANDROID_AND_TWO_CONFIRMATIONS,preferences.mode)
    }
}
