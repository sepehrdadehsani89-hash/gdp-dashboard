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
        private val schedule=Runnable {
            renderHandler.removeCallbacks(draw)
            if(visible && surfaceReady && interactive && !destroyed){
                val s=frame
                val fps=WallpaperRenderPolicy.fps(visible,surfaceReady,interactive,s.hud.settings,s.hud.telemetry.battery,s.hud.telemetry.powerSaver,s.hud.telemetry.systemMotion,s.visual.mode)
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
                    if(canvas!=null){renderer.draw(canvas,frame,started);drawn=true}
                }catch(_:IllegalArgumentException){/* Surface is being replaced by Android. */}
                catch(_:IllegalStateException){/* A hidden/destroyed surface is not a render target. */}
                finally{canvas?.let{if(runCatching{surfaceHolder.unlockCanvasAndPost(it)}.isSuccess && drawn)frames.incrementAndGet()}}
            }
            val snapshot=frame
            val fps=WallpaperRenderPolicy.fps(visible,surfaceReady,interactive,snapshot.hud.settings,snapshot.hud.telemetry.battery,snapshot.hud.telemetry.powerSaver,snapshot.hud.telemetry.systemMotion,snapshot.visual.mode)
            if(fps>0 && !destroyed && visible)renderHandler.postDelayed(this,(1000L/fps-(SystemClock.elapsedRealtime()-started)).coerceAtLeast(1))
        }}
        override fun onCreate(holder:SurfaceHolder){super.onCreate(holder);setTouchEventsEnabled(false);setOffsetNotificationsEnabled(false);engines.incrementAndGet()}
        override fun onSurfaceCreated(holder:SurfaceHolder){super.onSurfaceCreated(holder);synchronized(surfaceLock){surfaceReady=true};requestFrame()}
        override fun onSurfaceChanged(holder:SurfaceHolder,format:Int,width:Int,height:Int){super.onSurfaceChanged(holder,format,width,height);synchronized(surfaceLock){surfaceReady=true};requestFrame()}
        override fun onSurfaceDestroyed(holder:SurfaceHolder){synchronized(surfaceLock){surfaceReady=false};renderHandler.removeCallbacks(draw);renderHandler.removeCallbacks(schedule);super.onSurfaceDestroyed(holder)}
        override fun onVisibilityChanged(value:Boolean){
            super.onVisibilityChanged(value)
            if(visible==value || destroyed)return
            visible=value
            if(value){visibleEngines.incrementAndGet();if(!isPreview)visibleHomeEngines.incrementAndGet();startObservation()}else{visibleEngines.decrementAndGet();if(!isPreview)visibleHomeEngines.decrementAndGet();stopObservation()}
        }
        private fun startObservation(){
            interactive=getSystemService(PowerManager::class.java).isInteractive
            runtime.hud.startVisible(this,includeWeather=runtime.hud.state.value.settings.sentinel.wallpaperWeather)
            if(!registered){ContextCompat.registerReceiver(this@JarvisWallpaperService,screenReceiver,IntentFilter().apply{addAction(Intent.ACTION_SCREEN_OFF);addAction(Intent.ACTION_SCREEN_ON)},ContextCompat.RECEIVER_EXPORTED);registered=true}
            observation?.cancel()
            observation=scope.launch{runtime.state.collect{state->
                frame=WallpaperFrame(state.hud,state.audio,state.sentinelVisual,state.settings.language,state.playback,state.followUpArming,state.voiceWindowDeadline)
                runtime.hud.weatherInterest(this@SentinelEngine,state.hud.settings.sentinel.wallpaperWeather)
                requestFrame()
            }}
            requestFrame()
        }
        private fun stopObservation(){
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
