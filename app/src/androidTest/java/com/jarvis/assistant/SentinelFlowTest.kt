package com.jarvis.assistant

import android.app.UiAutomation
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.assistant.core.AssistantSettings
import com.jarvis.assistant.hud.*
import com.jarvis.assistant.sentinel.*
import com.jarvis.assistant.voice.WakeListenerService
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Dedicated emulator only. Wallpaper selection uses Android's real confirmation UI. */
class SentinelFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private lateinit var runtime:AssistantRuntime
    private var originalHud=HudSettings()
    private var originalSettings=AssistantSettings()
    private var wallpaperChanged=false
    @Before fun prepare(){
        compose.activityRule.scenario.onActivity {
            runtime=(it.application as JarvisApplication).runtime;runtime.stopEverything()
            originalHud=runtime.hud.state.value.settings;originalSettings=runtime.store.settings.value
            runtime.hud.save(HudSettings(reducedMotion=true,soundEffects=false))
            runtime.store.updateSettings(AssistantSettings(onboardingComplete=true,speakReplies=false))
        }
        compose.waitUntil(30_000){runtime.state.value.settings.onboardingComplete}
    }
    @After fun restore(){
        shell("dumpsys battery reset");shell("input keyevent KEYCODE_WAKEUP");shell("wm dismiss-keyguard")
        if(wallpaperChanged){
            val automation=instrumentation.uiAutomation
            automation.adoptShellPermissionIdentity(android.Manifest.permission.SET_WALLPAPER)
            try{WallpaperManager.getInstance(context).clear(WallpaperManager.FLAG_SYSTEM)}finally{automation.dropShellPermissionIdentity()}
        }
        compose.runOnUiThread{runtime.stopEverything();runtime.hud.save(originalHud);runtime.store.updateSettings(originalSettings)}
        PlaybackLevels.state.value.let{assertFalse("No leaked playback meter",it.active)}
    }
    private fun shell(command:String):String=ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use{it.readText()}
    private fun clickSystem(label:String,required:Boolean=true):Boolean {
        val until=SystemClock.elapsedRealtime()+if(required)25_000 else 2500
        while(SystemClock.elapsedRealtime()<until){
            var root=instrumentation.uiAutomation.rootInActiveWindow
            // Software emulation can stall System UI during cold DEX verification.
            // Dismiss only that explicitly identified system dialog, never a Jarvis failure.
            if(root?.findAccessibilityNodeInfosByText("System UI isn't responding")?.isNotEmpty()==true || root?.findAccessibilityNodeInfosByText("Quickstep isn't responding")?.isNotEmpty()==true){
                val close=root.findAccessibilityNodeInfosByText("Close app").firstOrNull{it.text?.toString()?.equals("Close app",true)==true}
                (if(close?.isClickable==true)close else close?.parent)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Thread.sleep(250);root=instrumentation.uiAutomation.rootInActiveWindow
            }
            val node=root?.findAccessibilityNodeInfosByText(label)?.firstOrNull{it.text?.toString()?.equals(label,true)==true || it.contentDescription?.toString()?.equals(label,true)==true}
            var target=node
            repeat(5){if(target!=null && target?.isClickable!=true)target=target?.parent}
            if(target?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true)return true
            Thread.sleep(100)
        }
        if(required)fail("Android system control not found: $label")
        return false
    }
    @Test fun realWallpaperPreviewSelectionBatteryAndHiddenEngineLifecycle(){
        val manager=WallpaperManager.getInstance(context)
        val expected=ComponentName(context,JarvisWallpaperService::class.java)
        val original=manager.wallpaperInfo?.component
        val launcher=context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        compose.onNodeWithText("Settings",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Sentinel · wallpaper & sound").performClick()
        compose.onNodeWithText("Preview / apply Jarvis wallpaper").performClick()
        compose.waitUntil(30_000){JarvisWallpaperService.activeEngineCount>0 && JarvisWallpaperService.renderedFrames>0}
        assertFalse("Preview never starts microphone service",WakeListenerService.state.value.running)
        assertEquals("Preview is not silent wallpaper installation",original,manager.wallpaperInfo?.component)
        clickSystem("Set wallpaper")
        clickSystem("Home screen",required=false)
        wallpaperChanged=true
        compose.waitUntil(30_000){manager.wallpaperInfo?.component==expected}
        shell("input keyevent KEYCODE_HOME")
        compose.waitUntil(30_000){JarvisWallpaperService.visibleEngineCount>0}
        val before=JarvisWallpaperService.renderedFrames
        shell("dumpsys battery set ac 0");shell("dumpsys battery set usb 0");shell("dumpsys battery set level 37");shell("dumpsys battery set status 3")
        compose.waitUntil(30_000){runtime.state.value.hud.telemetry.battery.percent==37 && JarvisWallpaperService.renderedFrames>before}
        assertFalse(WakeListenerService.state.value.running)
        val sampleStart=SystemClock.elapsedRealtime();val staticFrames=JarvisWallpaperService.renderedFrames
        Thread.sleep(1000)
        val staticDelta=JarvisWallpaperService.renderedFrames-staticFrames
        assertTrue("Reduced-motion wallpaper must not run a continuous renderer",staticDelta<=2)
        File(context.cacheDir,"sentinel-emulator-performance.json").writeText(org.json.JSONObject().put("sample_ms",SystemClock.elapsedRealtime()-sampleStart).put("submitted_static_frames",staticDelta).put("app_pss_kib",android.os.Debug.getPss()).put("device","API 29 software emulator; reduced motion").toString())
        assertEquals(launcher,context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName)
        instrumentation.uiAutomation.takeScreenshot()?.let{image->File(context.cacheDir,"sentinel-home-tested.png").outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()}
        shell("input keyevent KEYCODE_SLEEP")
        Thread.sleep(1200)
        val asleep=JarvisWallpaperService.renderedFrames
        Thread.sleep(800)
        assertTrue("Display-off surface must not keep drawing",JarvisWallpaperService.renderedFrames-asleep<=1)
        shell("input keyevent KEYCODE_WAKEUP");shell("wm dismiss-keyguard")
        shell("am start -n com.jarvis.next/com.jarvis.assistant.MainActivity")
        compose.waitUntil(30_000){JarvisWallpaperService.visibleEngineCount==0}
        val hidden=JarvisWallpaperService.renderedFrames
        Thread.sleep(800)
        assertTrue("Covered wallpaper must stop drawing",JarvisWallpaperService.renderedFrames-hidden<=1)
    }
    @Test fun sentinelPreferencesDoNotOverwriteExistingMemoryOrProviderKeys(){
        val keyBefore=runtime.store.getApiKey("gemini")
        val note="Sentinel additive settings fixture"
        val id=runBlocking{runtime.store.remember(note)}
        compose.runOnUiThread{runtime.hud.save(runtime.hud.state.value.settings.copy(accent="blue",sentinel=SentinelSettings(effectsVolume=.2f,animationSpeed=.7f,wallpaperClock=false,conciseReplies=false)))}
        val read=HudPreferences(context).read();assertEquals("blue",read.accent);assertEquals(.2f,read.sentinel.effectsVolume,0f);assertFalse(read.sentinel.wallpaperClock);assertFalse(read.sentinel.conciseReplies)
        compose.waitUntil(20_000){runtime.store.memories.value.any{it.id==id && it.text==note}}
        assertTrue("HUD preferences must preserve provider credentials",keyBefore==runtime.store.getApiKey("gemini"))
        runBlocking{runtime.store.forget(id)}
    }
    @Test fun allOriginalSoundAssetsDecodeAndPlaybackQueuesWithoutMic(){
        compose.runOnUiThread{runtime.hud.save(runtime.hud.state.value.settings.copy(soundEffects=true,sentinel=SentinelSettings(effectsVolume=.1f)));runtime.effects.warm()}
        compose.waitUntil(30_000){runtime.effects.loadedCount==26}
        val audio=context.getSystemService(android.media.AudioManager::class.java)
        val notification=context.getSystemService(android.app.NotificationManager::class.java)
        Assume.assumeTrue("Sound playback policy intentionally blocks quiet mode",audio.ringerMode==android.media.AudioManager.RINGER_MODE_NORMAL && notification.currentInterruptionFilter==android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
        compose.runOnUiThread{runtime.effects.userPlay(SentinelSound.SUCCESS)}
        compose.waitUntil(10_000){runtime.effects.lastQueued==SentinelSound.SUCCESS}
        assertFalse(WakeListenerService.state.value.running)
    }
    @Test fun neuralPlaybackMeterFollowsRealAudioTrackCursor(){
        val minimum=AudioTrack.getMinBufferSize(24000,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_FLOAT)
        val track=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(24000).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_FLOAT).build())
            .setBufferSizeInBytes(maxOf(minimum*2,8192)).setTransferMode(AudioTrack.MODE_STREAM).build()
        val meter=PlaybackCursorMeter(track);val done=AtomicBoolean(false)
        val written=java.util.concurrent.atomic.AtomicLong()
        val writeFailure=java.util.concurrent.atomic.AtomicReference<Throwable?>()
        val thread=Thread{
            try{
                val samples=FloatArray(48000){i->(kotlin.math.sin(i*2*Math.PI*440/24000)*.3).toFloat()}
                var offset=0
                while(offset<samples.size){val n=track.write(samples,offset,minOf(1024,samples.size-offset),AudioTrack.WRITE_BLOCKING);check(n>0);meter.written(samples,offset,n,offset.toLong());offset+=n;written.set(offset.toLong())}
            }catch(error:Throwable){writeFailure.set(error)}finally{done.set(true)}
        }
        try{
            track.setVolume(.001f) // Test digital PCM levels, not acoustic speaker loudness.
            track.play();thread.start()
            try{compose.waitUntil(20_000){meter.observedPeak>.01f}}
            catch(error:Exception){throw AssertionError("No measured PCM playback: cursor=${meter.lastCursor}, polls=${meter.samplesTaken}, peak=${meter.observedPeak}, written=${written.get()}, done=${done.get()}, writeFailure=${writeFailure.get()}",error)}
            assertTrue(track.playbackHeadPosition>0)
            thread.join(10_000);assertTrue(done.get());assertNull(writeFailure.get())
        }finally{meter.close();runCatching{track.stop()};track.release()}
        assertFalse(PlaybackLevels.state.value.active)
    }
    @Test fun landscapeAndPersianArtworkRenderWithoutReplacingTheLauncher(){
        for((width,height) in listOf(800 to 360,360 to 800)){
            val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
            val renderer=WallpaperRenderer()
            val frame=WallpaperFrame(hud=HudState(settings=HudSettings(reducedMotion=true),telemetry=Telemetry(battery=BatteryReading(64),timezone="Asia/Tehran")),language=com.jarvis.assistant.core.AppLanguage.FA)
            renderer.draw(Canvas(bitmap),frame,1000)
            val pixels=IntArray(width*height);bitmap.getPixels(pixels,0,width,0,0,width,height)
            assertTrue("Native shaped artwork must contain visible drawing",pixels.count{it and 0x00ffffff!=0}>100)
            File(context.cacheDir,"sentinel-rtl-${width}x${height}.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
    @Test fun nativeRendererDrawsFixtureBatteryAsTextRatherThanACannedImage(){
        val bitmap=Bitmap.createBitmap(1080,1920,Bitmap.Config.ARGB_8888)
        val frame=WallpaperFrame(hud=HudState(settings=HudSettings(reducedMotion=true),telemetry=Telemetry(battery=BatteryReading(37),epochMillis=1735689600000,timezone="UTC")))
        WallpaperRenderer().draw(Canvas(bitmap),frame,1000)
        val directory=File(context.cacheDir,"sentinel-render-ocr").apply{File(this,"tessdata").mkdirs()}
        context.assets.open("ocr/eng.traineddata").use{input->File(directory,"tessdata/eng.traineddata").outputStream().use(input::copyTo)}
        val ocr=TessBaseAPI()
        try{
            assertTrue(ocr.init(directory.absolutePath,"eng",TessBaseAPI.OEM_LSTM_ONLY))
            ocr.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);ocr.setImage(bitmap);ocr.setRectangle(280,790,520,180)
            val text=ocr.getUTF8Text().orEmpty();assertTrue("Expected native battery text 37: $text",text.contains("37"))
            File(context.cacheDir,"sentinel-renderer-fixture.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        }finally{ocr.recycle();bitmap.recycle();directory.deleteRecursively()}
    }
}
