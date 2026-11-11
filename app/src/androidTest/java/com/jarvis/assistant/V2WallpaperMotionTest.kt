package com.jarvis.assistant

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jarvis.assistant.hud.*
import com.jarvis.assistant.sentinel.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Android Canvas tests, for a dedicated emulator. Compilation is not runtime execution. */
@RunWith(AndroidJUnit4::class)
class V2WallpaperMotionTest {
    private fun pixels(renderer:WallpaperRenderer,frame:WallpaperFrame,at:Long):IntArray {
        val b=Bitmap.createBitmap(360,720,Bitmap.Config.ARGB_8888)
        return try{renderer.draw(Canvas(b),frame,at);IntArray(360*720).also{b.getPixels(it,0,360,0,0,360,720)}}finally{b.recycle()}
    }
    @Test fun theNativeIdleCoreActuallyChangesRenderedPixels(){val renderer=WallpaperRenderer();val frame=WallpaperFrame();val a=pixels(renderer,frame,1000);val b=pixels(renderer,frame,1500);assertFalse("Native orbit/rotation should move",a.contentEquals(b))}
    @Test fun reducedMotionKeepsTheSameNativeArtworkStill(){val renderer=WallpaperRenderer();val frame=WallpaperFrame(hud=HudState(settings=HudSettings(reducedMotion=true)));val a=pixels(renderer,frame,1000);val b=pixels(renderer,frame,1500);assertArrayEquals(a,b)}
}
