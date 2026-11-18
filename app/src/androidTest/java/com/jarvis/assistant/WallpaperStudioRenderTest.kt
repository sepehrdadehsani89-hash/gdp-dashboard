package com.jarvis.assistant

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jarvis.assistant.hud.HudState
import com.jarvis.assistant.sentinel.*
import com.jarvis.assistant.studio.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Real Android software bitmap fixtures. These do not test home-screen display or microphone hardware. */
@RunWith(AndroidJUnit4::class)
class WallpaperStudioRenderTest {
    private val frame=WallpaperFrame(hud=HudState().let{h->h.copy(settings=h.settings.copy(reducedMotion=true),telemetry=h.telemetry.copy(epochMillis=1700000000000,timezone="UTC"))},visual=SentinelVisualState(mode=SentinelMode.PROCESSING))
    private val core=StudioLayer("core","PART:CORE")
    private fun profile(vararg layers:StudioLayer,privateWidgets:Boolean=false)=StudioProfile(UUID.randomUUID().toString(),"Android bitmap fixture",layers.toList(),privateWidgets=privateWidgets).validated()
    private fun render(p:StudioProfile?,data:Map<WidgetSource,String> = emptyMap(),privateAllowed:Boolean=false):IntArray {
        val b=Bitmap.createBitmap(400,700,Bitmap.Config.ARGB_8888)
        try{StudioWallpaperRenderer().draw(Canvas(b),frame,10000,p,data,privateAllowed);return IntArray(400*700).also{b.getPixels(it,0,400,0,0,400,700)}}finally{b.recycle()}
    }
    private fun original(parts:Set<WallpaperPart>?=null):IntArray{val b=Bitmap.createBitmap(400,700,Bitmap.Config.ARGB_8888);try{val r=WallpaperRenderer();if(parts==null)r.draw(Canvas(b),frame,10000)else r.draw(Canvas(b),frame,10000,parts);return IntArray(400*700).also{b.getPixels(it,0,400,0,0,400,700)}}finally{b.recycle()}}
    @Test fun inactiveStudioUsesOriginalPixels(){assertArrayEquals(original(),render(null))}
    @Test fun allPartsMaskPreservesDefaultPixels(){assertArrayEquals(original(),original(WallpaperPart.entries.toSet()))}
    @Test fun solidBackgroundIsActuallyDrawn(){val pixels=render(profile(StudioLayer("bg","BACKGROUND",color=Color.BLUE)));assertTrue(pixels.all{it==Color.BLUE})}
    @Test fun gradientBackgroundHasDifferentEnds(){val p=render(profile(StudioLayer("bg","BACKGROUND",color=Color.BLUE,gradientEnd=Color.GREEN)));assertNotEquals(p[10],p[p.size-11])}
    @Test fun backgroundOpacityBlendsOverBlack(){val p=render(profile(StudioLayer("bg","BACKGROUND",color=Color.RED,opacity=.5f)));assertTrue(Color.red(p[200])in 126..129);assertEquals(0,Color.green(p[200]))}
    @Test fun hiddenBackgroundDoesNotInventArtwork(){assertTrue(render(profile(StudioLayer("bg","BACKGROUND",visible=false,color=Color.RED))).all{it==Color.BLACK})}
    @Test fun backgroundRotationAffectsActualPixels(){val base=render(profile(StudioLayer("bg","BACKGROUND",color=Color.BLUE)));val moved=render(profile(StudioLayer("bg","BACKGROUND",rotation=45f,color=Color.BLUE)));assertFalse(base.contentEquals(moved))}
    @Test fun hiddenCoreIsNotDrawn(){assertTrue(render(profile(core.copy(visible=false))).all{it==Color.BLACK})}
    @Test fun visibleCoreUsesRealGeometry(){assertTrue(render(profile(core)).any{it!=Color.BLACK})}
    @Test fun corePlacementActuallyMovesPixels(){assertFalse(render(profile(core)).contentEquals(render(profile(core.copy(x=.7f)))))}
    @Test fun coreScaleActuallyChangesPixels(){assertFalse(render(profile(core)).contentEquals(render(profile(core.copy(scale=.6f)))))}
    @Test fun ringRotationActuallyChangesPixels(){val rings=core.copy(kind="PART:RINGS");assertFalse(render(profile(rings)).contentEquals(render(profile(rings.copy(rotation=17f)))))}
    @Test fun solidLayerColorTintsItsOwnedGeometry(){val p=render(profile(core.copy(color=Color.RED)));assertTrue(p.any{Color.red(it)>0});assertTrue(p.all{Color.green(it)==0 && Color.blue(it)==0})}
    @Test fun gradientLayerColorActuallyMixesChannels(){val p=render(profile(core.copy(color=Color.BLUE,gradientEnd=Color.GREEN)));assertTrue(p.any{Color.green(it)>0});assertTrue(p.any{Color.blue(it)>0})}
    @Test fun transparentCoreDoesNotLeavePixels(){assertTrue(render(profile(core.copy(opacity=0f))).all{it==Color.BLACK})}
    @Test fun higherLayerOrderCanCoverEarlierElements(){val bg=StudioLayer("bg","BACKGROUND",color=Color.RED,order=90);assertArrayEquals(render(profile(bg)),render(profile(core,bg)))}
    @Test fun widgetUsesActualSuppliedValue(){val clock=StudioLayer("clock","WIDGET:CLOCK",y=.5f);assertFalse(render(profile(clock),mapOf(WidgetSource.CLOCK to "12:34")).contentEquals(render(profile(clock),mapOf(WidgetSource.CLOCK to "23:59"))))}
    @Test fun hiddenPrivateWidgetCannotRevealDifferentSecrets(){val n=StudioLayer("n","WIDGET:NOTIFICATIONS",y=.5f);assertArrayEquals(render(profile(n),mapOf(WidgetSource.NOTIFICATIONS to "First private message")),render(profile(n),mapOf(WidgetSource.NOTIFICATIONS to "Second private message")))}
    @Test fun authorizedPrivateWidgetUsesItsActualData(){val n=StudioLayer("n","WIDGET:NOTIFICATIONS",y=.5f);assertFalse(render(profile(n,privateWidgets=true),mapOf(WidgetSource.NOTIFICATIONS to "First"),true).contentEquals(render(profile(n,privateWidgets=true),mapOf(WidgetSource.NOTIFICATIONS to "Second"),true)))}
    @Test fun ownerOptOutWinsOverCallerPrivacyFlag(){val n=StudioLayer("n","WIDGET:NOTIFICATIONS",y=.5f);assertArrayEquals(render(profile(n),mapOf(WidgetSource.NOTIFICATIONS to "First private text"),true),render(profile(n),mapOf(WidgetSource.NOTIFICATIONS to "Second private text"),true))}
    @Test fun unavailableWidgetIsExplicitlyLabeled(){val n=StudioLayer("n","WIDGET:WEATHER",y=.5f);assertArrayEquals(render(profile(n)),render(profile(n),mapOf(WidgetSource.WEATHER to "WEATHER · unavailable")))}
    @Test fun subsequentBlankProfileClearsPreviousArtwork(){val b=Bitmap.createBitmap(400,700,Bitmap.Config.ARGB_8888);try{val r=StudioWallpaperRenderer();val c=Canvas(b);r.draw(c,frame,10000,profile(core),emptyMap(),false);r.draw(c,frame,10001,profile(core.copy(visible=false)),emptyMap(),false);val p=IntArray(400*700);b.getPixels(p,0,400,0,0,400,700);assertTrue(p.all{it==Color.BLACK})}finally{b.recycle()}}
    @Test fun textAlignmentActuallyChangesPixels(){val n=StudioLayer("n","WIDGET:CLOCK",y=.5f);val data=mapOf(WidgetSource.CLOCK to "12:34");assertFalse(render(profile(n.copy(alignment="left")),data).contentEquals(render(profile(n.copy(alignment="right")),data)))}
    @Test fun componentPassDoesNotClearAnotherOwnedLayer(){val b=Bitmap.createBitmap(400,700,Bitmap.Config.ARGB_8888);try{val c=Canvas(b);c.drawColor(Color.BLUE);WallpaperRenderer().draw(c,frame,10000,emptySet(),clearBackground=false);val p=IntArray(400*700);b.getPixels(p,0,400,0,0,400,700);assertTrue(p.all{it==Color.BLUE})}finally{b.recycle()}}
}
