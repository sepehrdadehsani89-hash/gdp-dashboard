package com.jarvis.assistant.sentinel

import android.app.WallpaperManager
import android.content.*
import android.os.*
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import java.util.concurrent.atomic.AtomicInteger

/** Wallpaper selection never starts a microphone service or asks for microphone access. */
class JarvisWallpaperService:WallpaperService() {
    override fun onCreateEngine():Engine = SentinelEngine()
    inner class SentinelEngine:Engine(){
        private val runtime get()=(application as JarvisApplication).runtime
        private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
        private val worker=HandlerThread("JarvisWallpaper").apply{start()}
        private val renderHandler=Handler(worker.looper)
        private val renderer=WallpaperRenderer()
        private val surfaceLock=Any()
        @Volatile private var visible=false
        @Volatile private var surfaceReady=false
        @Volatile private var interactive=true
        @Volatile private var destroyed=false
        @Volatile private var frame=WallpaperFrame()
        private var observation:Job?=null
        private var registered=false
        private var lastFrameAt=0L
        @Volatile private var lastMeasuredFrame=0L
        private val schedule=Runnable {
            renderHandler.removeCallbacks(draw)
            if(visible && surfaceReady && interactive && !destroyed){
                val s=frame
                val fps=WallpaperRenderPolicy.fps(visible,surfaceReady,interactive,s.hud.settings,s.hud.telemetry.battery,s.hud.telemetry.powerSaver,s.hud.telemetry.systemMotion,s.visual.mode,s.restricted,s.hud.telemetry.thermalStatus,s.refreshRate)
                renderHandler.postDelayed(draw,(lastFrameAt+(if(fps>0)1000L/fps else 80L)-SystemClock.elapsedRealtime()).coerceAtLeast(0))
            }
        }
        private val screenReceiver=object:BroadcastReceiver(){override fun onReceive(context:Context?,intent:Intent?){
            interactive=intent?.action!=Intent.ACTION_SCREEN_OFF && getSystemService(PowerManager::class.java).isInteractive
            requestFrame()
        }}
        private val draw=object:Runnable { override fun run(){
            if(destroyed || !visible || !surfaceReady || !interactive)return
            val started=SystemClock.elapsedRealtime();lastFrameAt=started
            synchronized(surfaceLock){
                if(destroyed || !surfaceReady || !surfaceHolder.surface.isValid)return
                var canvas:android.graphics.Canvas?=null
                var drawn=false
                try {
                    canvas=try{surfaceHolder.lockHardwareCanvas()}catch(_:Exception){surfaceHolder.lockCanvas()}
                    if(canvas!=null){
                        val safe=runtime.phantomPreferences.state.value.captions && getSystemService(android.app.KeyguardManager::class.java)?.isDeviceLocked==false && !com.jarvis.assistant.security.SystemCredentialPrivacy.pending(this@JarvisWallpaperService)
                        renderer.draw(canvas,if(safe)frame else frame.copy(caption=com.jarvis.assistant.phantom.LiveCaption()),started);drawn=true
                    }
                }catch(_:IllegalArgumentException){/* Surface is being replaced by Android. */}
                catch(_:IllegalStateException){/* A hidden/destroyed surface is not a render target. */}
                finally{canvas?.let{if(runCatching{surfaceHolder.unlockCanvasAndPost(it)}.isSuccess && drawn){frames.incrementAndGet();timings.record(if(lastMeasuredFrame>0)started-lastMeasuredFrame else 0,SystemClock.elapsedRealtime()-started);lastMeasuredFrame=started}}}
            }
            val snapshot=frame
            val fps=WallpaperRenderPolicy.fps(visible,surfaceReady,interactive,snapshot.hud.settings,snapshot.hud.telemetry.battery,snapshot.hud.telemetry.powerSaver,snapshot.hud.telemetry.systemMotion,snapshot.visual.mode,snapshot.restricted,snapshot.hud.telemetry.thermalStatus,snapshot.refreshRate)
            if(fps>0 && !destroyed && visible)renderHandler.postDelayed(this,(1000L/fps-(SystemClock.elapsedRealtime()-started)).coerceAtLeast(1))
            else if(snapshot.caption.visible(started) && snapshot.caption.expiresAt>0 && !destroyed && visible)renderHandler.postDelayed(this,if(snapshot.caption.expiresAt-started<1000 && !snapshot.hud.settings.reducedMotion)80L else 1000L)
        }}
        override fun onCreate(holder:SurfaceHolder){super.onCreate(holder);setTouchEventsEnabled(false);setOffsetNotificationsEnabled(false);engines.incrementAndGet()}
        override fun onSurfaceCreated(holder:SurfaceHolder){super.onSurfaceCreated(holder);synchronized(surfaceLock){surfaceReady=true};requestFrame()}
        override fun onSurfaceChanged(holder:SurfaceHolder,format:Int,width:Int,height:Int){super.onSurfaceChanged(holder,format,width,height);synchronized(surfaceLock){surfaceReady=true};requestFrame()}
        override fun onSurfaceDestroyed(holder:SurfaceHolder){synchronized(surfaceLock){surfaceReady=false};renderHandler.removeCallbacks(draw);renderHandler.removeCallbacks(schedule);renderHandler.post{renderer.pause();lastMeasuredFrame=0L};super.onSurfaceDestroyed(holder)}
        override fun onVisibilityChanged(value:Boolean){
            super.onVisibilityChanged(value)
            if(visible==value || destroyed)return
            visible=value
            if(!value)lastMeasuredFrame=0L
            if(value){visibleEngines.incrementAndGet();if(!isPreview)visibleHomeEngines.incrementAndGet();startObservation()}else{visibleEngines.decrementAndGet();if(!isPreview)visibleHomeEngines.decrementAndGet();stopObservation();renderHandler.post{renderer.pause()}}
        }
        private fun startObservation(){
            interactive=getSystemService(PowerManager::class.java).isInteractive
            runtime.hud.startVisible(this,includeWeather=runtime.hud.state.value.settings.sentinel.wallpaperWeather)
            if(!registered){ContextCompat.registerReceiver(this@JarvisWallpaperService,screenReceiver,IntentFilter().apply{addAction(Intent.ACTION_SCREEN_OFF);addAction(Intent.ACTION_SCREEN_ON);addAction(Intent.ACTION_USER_PRESENT)},ContextCompat.RECEIVER_EXPORTED);registered=true}
            observation?.cancel()
            observation=scope.launch{combine(runtime.state,runtime.phantom.state,runtime.phantomPreferences.state){state,phantom,prefs->Triple(state,phantom,prefs)}.collect{(state,phantom,prefs)->
                val hud=state.hud
                // Observe only visible artwork. Memory/network/PCM wake-only updates and
                // sub-minute timestamps must not wake a static wallpaper renderer.
                val telemetry=hud.telemetry.copy(epochMillis=hud.telemetry.epochMillis/60_000*60_000,
                    network="Unavailable",internetValidated=false,systemAvailableBytes=null,systemTotalBytes=null,appPssKiB=null)
                val mode=state.sentinelVisual.mode
                val audio=if(mode in listOf(SentinelMode.LISTENING,SentinelMode.ACTIVATING))state.audio else com.jarvis.assistant.hud.AudioReading()
                val playback=if(mode==SentinelMode.SPEAKING)state.playback else PlaybackReading()
                val safe=prefs.captions && getSystemService(android.app.KeyguardManager::class.java)?.isDeviceLocked==false && !com.jarvis.assistant.security.SystemCredentialPrivacy.pending(this@JarvisWallpaperService)
                val next=WallpaperFrame(hud.copy(telemetry=telemetry,weather=if(hud.settings.sentinel.wallpaperWeather)hud.weather else com.jarvis.assistant.hud.WeatherState()),audio,state.sentinelVisual,state.settings.language,playback,state.followUpArming,state.voiceWindowDeadline,if(safe)phantom.caption else com.jarvis.assistant.phantom.LiveCaption(),prefs.lines,state.performanceRestricted || getSystemService(android.app.ActivityManager::class.java).isLowRamDevice,getSystemService(android.hardware.display.DisplayManager::class.java).getDisplay(android.view.Display.DEFAULT_DISPLAY)?.refreshRate ?: 60f,state.voiceErrorAt,state.execution)
                runtime.hud.weatherInterest(this@SentinelEngine,hud.settings.sentinel.wallpaperWeather)
                if(next!=frame){frame=next;requestFrame()}
            }}
            requestFrame()
        }
        private fun stopObservation(){
            frame=frame.copy(caption=com.jarvis.assistant.phantom.LiveCaption())
            renderHandler.post{renderer.clearCaption()}
            renderHandler.removeCallbacks(draw)
            renderHandler.removeCallbacks(schedule)
            observation?.cancel();observation=null
            if(registered){runCatching{unregisterReceiver(screenReceiver)};registered=false}
            runtime.hud.stopVisible(this)
        }
        private fun requestFrame(){renderHandler.removeCallbacks(schedule);if(visible && surfaceReady && interactive && !destroyed)renderHandler.post(schedule)}
        override fun onDestroy(){
            if(visible){visible=false;visibleEngines.decrementAndGet();if(!isPreview)visibleHomeEngines.decrementAndGet()}
            destroyed=true;stopObservation();scope.cancel()
            synchronized(surfaceLock){surfaceReady=false}
            renderHandler.removeCallbacksAndMessages(null);worker.quitSafely();engines.decrementAndGet()
            super.onDestroy()
        }
    }
    companion object {
        private val visibleEngines=AtomicInteger()
        private val visibleHomeEngines=AtomicInteger()
        private val engines=AtomicInteger()
        private val frames=AtomicInteger()
        private val timings=com.jarvis.assistant.power.RenderMeasurements()
        val frameTimingSummary:String get()=timings.summary()
        val homeVisible:Boolean get()=visibleHomeEngines.get()>0
        val anyVisible:Boolean get()=visibleEngines.get()>0
        val activeEngineCount:Int get()=engines.get()
        val visibleEngineCount:Int get()=visibleEngines.get()
        val renderedFrames:Int get()=frames.get()
    }
}

/** Android's wallpaper settings shortcut opens the ordinary app settings, not a special privileged UI. */
class SentinelWallpaperSettingsActivity:android.app.Activity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
        startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("open_settings",true).putExtra("sentinel_settings",true));finish()
    }
}
