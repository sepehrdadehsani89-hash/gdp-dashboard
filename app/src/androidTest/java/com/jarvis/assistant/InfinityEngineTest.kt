package com.jarvis.assistant

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import com.jarvis.assistant.hud.*
import com.jarvis.assistant.local.LocalBrain
import com.jarvis.assistant.sentinel.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.util.UUID

class InfinityEngineTest {
    @Test fun idleCleanupCannotDestroyAnInUseHandleAndReleasesAnIdleOne(){
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val brain=LocalBrain(context);val cls=LocalBrain::class.java
        val file=File(context.cacheDir,"idle-test-${UUID.randomUUID()}.gguf").apply{writeText("Invalid test fixture; never used as AI weights.")}
        val create=cls.getDeclaredMethod("nativeCreate",String::class.java,Int::class.javaPrimitiveType,Int::class.javaPrimitiveType).apply{isAccessible=true}
        val prepare=cls.getDeclaredMethod("nativePrepare",Long::class.javaPrimitiveType).apply{isAccessible=true}
        val handleField=cls.getDeclaredField("handle").apply{isAccessible=true}
        val mutexField=cls.getDeclaredField("inferenceMutex").apply{isAccessible=true}
        val mutex=mutexField.get(brain) as Mutex
        val id=create.invoke(brain,file.absolutePath,1536,1) as Long;handleField.setLong(brain,id)
        try{runBlocking{
            mutex.lock()
            try{brain.releaseIdle();assertEquals(id,handleField.getLong(brain));prepare.invoke(brain,id)}finally{mutex.unlock()}
            brain.releaseIdle();assertEquals(0L,handleField.getLong(brain))
            assertTrue(runCatching{prepare.invoke(brain,id)}.exceptionOrNull()?.cause is IllegalStateException)
        }}finally{runBlocking{brain.release()};file.delete()}
    }
    @Test fun actualCanvasKeepsChargingGreenWhileSpeakingCyanOrListeningPurple(){
        // Explicit renderer fixtures, not claimed physical device measurements.
        val hud=HudState(settings=HudSettings(reducedMotion=true),telemetry=Telemetry(battery=BatteryReading(percent=63,charging=true,source="USB")))
        for(mode in listOf(SentinelMode.SPEAKING,SentinelMode.LISTENING)){
            val bitmap=Bitmap.createBitmap(480,800,Bitmap.Config.ARGB_8888)
            try{
                WallpaperRenderer().draw(Canvas(bitmap),WallpaperFrame(hud=hud,visual=SentinelVisualState(mode=mode)),50_000)
                val pixels=IntArray(480*800);bitmap.getPixels(pixels,0,480,0,0,480,800)
                val green=pixels.count{val r=Color.red(it);val g=Color.green(it);val b=Color.blue(it);g>100 && g>r*1.3 && g>b*1.2}
                val state=pixels.count{val r=Color.red(it);val g=Color.green(it);val b=Color.blue(it);if(mode==SentinelMode.SPEAKING)b>80 && g>80 && r<minOf(g,b)*.7 else b>130 && r>75 && g<b*.75}
                assertTrue("Independent charging ring must render",green>30)
                assertTrue("The core state must render simultaneously: $mode",state>30)
            }finally{bitmap.recycle()}
        }
    }
}
