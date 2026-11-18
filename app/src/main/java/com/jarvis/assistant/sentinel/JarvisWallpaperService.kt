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
        private val renderer=com.jarvis.assistant.studio.StudioWallpaperRenderer()
        private var widgetStamp=""
        private var widgetValues:Map<com.jarvis.assistant.studio.WidgetSource,String> = emptyMap()
        private val surfaceRetry=WallpaperSurfaceRetry()
        private val surfaceLock=Any()
        @Volatile private var visible=false
        @Volatile private var surfaceReady=false
        @Volatile private var interactive=true
        @Volatile private var destroyed=false
        @Volatile private var frame=WallpaperFrame()
        private var observation:Job?=null
        private var registered=false
        private var lastFrameAt=0L
        private var recoverAfter=0L
        @Volatile private var lastMeasuredFrame=0L
        private val schedule=Runnable {
            renderHandler.removeCallbacks(draw)
            if(visible && surfaceReady && interactive && !destroyed){
                // A lifecycle/state request gets a fresh bounded recovery window.
                surfaceRetry.reset()
                val s=frame
                val fps=WallpaperRenderPolicy.fps(visible,surfaceReady,interactive,s.hud.settings,s.hud.telemetry.battery,s.hud.telemetry.powerSaver,s.hud.telemetry.systemMotion,s.visual.mode,s.restricted,s.hud.telemetry.thermalStatus,s.refreshRate)
                renderHandler.postDelayed(draw,(maxOf(recoverAfter,lastFrameAt+(if(fps>0)1000L/fps else 80L))-SystemClock.elapsedRealtime()).coerceAtLeast(0))
            }
        }
        private val screenReceiver=object:BroadcastReceiver(){override fun onReceive(context:Context?,intent:Intent?){
            interactive=intent?.action!=Intent.ACTION_SCREEN_OFF && getSystemService(PowerManager::class.java).isInteractive
            requestFrame()
        }}
        private val draw=object:Runnable { override fun run(){
            if(destroyed || !visible || !surfaceReady || !interactive)return
            val started=SystemClock.elapsedRealtime();lastFrameAt=started
            var posted=false
            synchronized(surfaceLock){
                if(destroyed || !surfaceReady)return
                if(surfaceHolder.surface.isValid){
                    var canvas:android.graphics.Canvas?=null
                    var drawn=false
                    try {
                        canvas=try{surfaceHolder.lockHardwareCanvas()}catch(_:Exception){surfaceHolder.lockCanvas()}
                        if(canvas!=null){
                            val safe=runtime.phantomPreferences.state.value.captions && getSystemService(android.app.KeyguardManager::class.java)?.isDeviceLocked==false && !com.jarvis.assistant.security.SystemCredentialPrivacy.pending(this@JarvisWallpaperService)
                            val shown=if(safe && com.jarvis.assistant.modes.SystemModePolicy.privatePreviewsAllowed(runtime.systemModes.state.value))frame else frame.copy(caption=com.jarvis.assistant.phantom.LiveCaption())
                            val privateAllowed=shown.studio?.privateWidgets==true && getSystemService(android.app.KeyguardManager::class.java)?.isDeviceLocked==false && !com.jarvis.assistant.security.SystemCredentialPrivacy.pending(this@JarvisWallpaperService) && com.jarvis.assistant.modes.SystemModePolicy.privatePreviewsAllowed(runtime.systemModes.state.value)
                            var widgetData=shown.widgetValues
                            if(com.jarvis.assistant.studio.WidgetSource.GMAIL_UNREAD in widgetData)widgetData=widgetData+(com.jarvis.assistant.studio.WidgetSource.GMAIL_UNREAD to runtime.gmailWidgetText())
                            if(com.jarvis.assistant.studio.WidgetSource.COMPUTER in widgetData)widgetData=widgetData+(com.jarvis.assistant.studio.WidgetSource.COMPUTER to com.jarvis.assistant.companion.DesktopConnectionProofPolicy.text(runtime.companion.connectionProof.value,runtime.companion.paired(),System.currentTimeMillis()))
                            if(com.jarvis.assistant.studio.WidgetSource.CALENDAR in widgetData)widgetData=widgetData+(com.jarvis.assistant.studio.WidgetSource.CALENDAR to runtime.googleReadWidgetText(com.jarvis.assistant.google.GoogleProduct.CALENDAR))
                            if(com.jarvis.assistant.studio.WidgetSource.TASKS in widgetData)widgetData=widgetData+(com.jarvis.assistant.studio.WidgetSource.TASKS to runtime.googleReadWidgetText(com.jarvis.assistant.google.GoogleProduct.TASKS))
                            if(com.jarvis.assistant.studio.WidgetSource.NOTIFICATIONS in widgetData){
                                val consent=com.jarvis.assistant.notifications.NotificationSourcePreferences(this@JarvisWallpaperService).allowed().sorted().joinToString("|")
                                val granted=getSystemService(android.app.NotificationManager::class.java).isNotificationListenerAccessGranted(ComponentName(this@JarvisWallpaperService,com.jarvis.assistant.tools.JarvisNotificationService::class.java))
                                if(consent!=shown.notificationConsentKey || !granted || !com.jarvis.assistant.tools.JarvisNotificationService.connected)widgetData=widgetData+(com.jarvis.assistant.studio.WidgetSource.NOTIFICATIONS to "Notification access changed · previews hidden")
                            }
                            renderer.draw(canvas,shown,started,shown.studio,widgetData,privateAllowed);drawn=true
                        }
                    }catch(_:IllegalArgumentException){/* Surface is being replaced by Android. */}
                    catch(_:IllegalStateException){/* A hidden/destroyed surface is not a render target. */}
                    catch(_:RuntimeException){recoveryErrors.incrementAndGet();recoverAfter=SystemClock.elapsedRealtime()+1000;renderer.pause()}
                    catch(_:OutOfMemoryError){recoveryErrors.incrementAndGet();recoverAfter=SystemClock.elapsedRealtime()+2000;renderer.pause()}
                    finally{canvas?.let{if(runCatching{surfaceHolder.unlockCanvasAndPost(it)}.isSuccess && drawn){posted=true;frames.incrementAndGet();timings.record(if(lastMeasuredFrame>0)started-lastMeasuredFrame else 0,SystemClock.elapsedRealtime()-started);lastMeasuredFrame=started}}}
                }
            }
            val retryDelay=surfaceRetry.afterPost(posted,visible,surfaceReady,interactive,destroyed)
            if(!posted){
                // Null Canvas, transient lock exceptions and failed posts need the same
                // bounded recovery as an invalid Surface, including when motion is off.
                renderer.pause()
                retryDelay?.let{renderHandler.postDelayed(this,maxOf(it,recoverAfter-SystemClock.elapsedRealtime()))}
                return
            }
            val snapshot=frame
            val fps=WallpaperRenderPolicy.fps(visible,surfaceReady,interactive,snapshot.hud.settings,snapshot.hud.telemetry.battery,snapshot.hud.telemetry.powerSaver,snapshot.hud.telemetry.systemMotion,snapshot.visual.mode,snapshot.restricted,snapshot.hud.telemetry.thermalStatus,snapshot.refreshRate)
            if(fps>0 && !destroyed && visible)renderHandler.postDelayed(this,maxOf(recoverAfter-SystemClock.elapsedRealtime(),1000L/fps-(SystemClock.elapsedRealtime()-started),1L))
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
            runtime.hud.startVisible(this,includeWeather=!runtime.systemModes.state.value.deep && runtime.hud.state.value.settings.sentinel.wallpaperWeather)
            if(!registered){ContextCompat.registerReceiver(this@JarvisWallpaperService,screenReceiver,IntentFilter().apply{addAction(Intent.ACTION_SCREEN_OFF);addAction(Intent.ACTION_SCREEN_ON);addAction(Intent.ACTION_USER_PRESENT)},ContextCompat.RECEIVER_EXPORTED);registered=true}
            observation?.cancel()
            observation=scope.launch{combine(runtime.state,runtime.phantom.state,runtime.phantomPreferences.state){state,phantom,prefs->Triple(state,phantom,prefs)}
                .combine(runtime.wallpaperStudio.state){triple,library->triple to library}
                .combine(runtime.gmailWidget){observed,_->observed}
                .combine(runtime.googleWidgetReadings){observed,_->observed}
                .combine(runtime.companion.connectionProof){observed,_->observed}
                .combine(runtime.companion.registry.changes){observed,_->observed}
                .combine(com.jarvis.assistant.tools.JarvisNotificationService.changes){observed,_->observed}
                .collect{(observed,library)->
                val (state,phantom,prefs)=observed
                val profile=library.selected()
                val hud=com.jarvis.assistant.modes.ModePresentation.hud(state.hud,state.systemMode,profile?.quality)
                // Observe only visible artwork. Memory/network/PCM wake-only updates and
                // sub-minute timestamps must not wake a static wallpaper renderer.
                val telemetry=hud.telemetry.copy(epochMillis=hud.telemetry.epochMillis/60_000*60_000,
                    network="Unavailable",internetValidated=false,systemAvailableBytes=null,systemTotalBytes=null,appPssKiB=null)
                val mode=state.sentinelVisual.mode
                val privateAllowed=profile?.privateWidgets==true && getSystemService(android.app.KeyguardManager::class.java)?.isDeviceLocked==false && com.jarvis.assistant.modes.SystemModePolicy.privatePreviewsAllowed(state.systemMode)
                val consentKey=if(profile?.layers?.any{it.visible && it.kind=="WIDGET:NOTIFICATIONS"}==true)com.jarvis.assistant.notifications.NotificationSourcePreferences(this@JarvisWallpaperService).allowed().sorted().joinToString("|")else""
                val stamp=if(profile==null)""else"${state.hud.telemetry.epochMillis/1000}|${profile.hashCode()}|${state.systemMode}|${state.settings.provider}|${state.selectedBrainVerified}|${runtime.gmailWidget.value?.at}|${runtime.googleWidgetReadings.value.hashCode()}|${runtime.companion.connectionProof.value?.at}|${runtime.companion.registry.changes.value}|${runtime.googleConnections.epoch}|${com.jarvis.assistant.tools.JarvisNotificationService.changes.value}|$privateAllowed|$consentKey"
                if(profile==null)widgetValues=emptyMap() else if(stamp!=widgetStamp){widgetStamp=stamp;widgetValues=withContext(Dispatchers.IO){com.jarvis.assistant.studio.WallpaperWidgetValues.sample(this@JarvisWallpaperService,runtime,profile,privateAllowed)}}
                val audio=if(mode in listOf(SentinelMode.LISTENING,SentinelMode.ACTIVATING))state.audio else com.jarvis.assistant.hud.AudioReading()
                val playback=if(mode==SentinelMode.SPEAKING)state.playback else PlaybackReading()
                val safe=prefs.captions && getSystemService(android.app.KeyguardManager::class.java)?.isDeviceLocked==false && !com.jarvis.assistant.security.SystemCredentialPrivacy.pending(this@JarvisWallpaperService)
                val next=WallpaperFrame(hud.copy(telemetry=telemetry,weather=if(hud.settings.sentinel.wallpaperWeather)hud.weather else com.jarvis.assistant.hud.WeatherState()),audio,state.sentinelVisual,state.settings.language,playback,state.followUpArming,state.voiceWindowDeadline,if(safe)phantom.caption else com.jarvis.assistant.phantom.LiveCaption(),prefs.lines,state.performanceRestricted || getSystemService(android.app.ActivityManager::class.java).isLowRamDevice,getSystemService(android.hardware.display.DisplayManager::class.java).getDisplay(android.view.Display.DEFAULT_DISPLAY)?.refreshRate ?: 60f,state.voiceErrorAt,state.execution).copy(studio=profile,widgetValues=widgetValues,awakeningDurationMillis=com.jarvis.assistant.modes.SystemModePolicy.activationMillis(state.systemMode),notificationConsentKey=consentKey,visual=if(state.systemMode.deep)state.sentinelVisual.copy(mode=SentinelMode.DORMANT)else state.sentinelVisual)
                runtime.hud.weatherInterest(this@SentinelEngine,!state.systemMode.deep && (hud.settings.sentinel.wallpaperWeather || profile?.layers?.any{it.visible && it.kind in setOf("WIDGET:WEATHER","WIDGET:CITY","WIDGET:TEMPERATURE","WIDGET:FORECAST","WIDGET:SUNRISE","WIDGET:SUNSET")}==true))
                if(next!=frame){frame=next;requestFrame()}
            }}
            requestFrame()
        }
        private fun stopObservation(){
            widgetValues=emptyMap();widgetStamp="";frame=frame.copy(caption=com.jarvis.assistant.phantom.LiveCaption(),widgetValues=emptyMap())
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
        private val recoveryErrors=AtomicInteger()
        private val timings=com.jarvis.assistant.power.RenderMeasurements()
        val frameTimingSummary:String get()=timings.summary()
        val homeVisible:Boolean get()=visibleHomeEngines.get()>0
        val anyVisible:Boolean get()=visibleEngines.get()>0
        val activeEngineCount:Int get()=engines.get()
        val visibleEngineCount:Int get()=visibleEngines.get()
        val renderedFrames:Int get()=frames.get()
        val recoveredErrors:Int get()=recoveryErrors.get()
    }
}

/** Android's wallpaper settings shortcut opens the ordinary app settings, not a special privileged UI. */
class SentinelWallpaperSettingsActivity:android.app.Activity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
        startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("open_settings",true).putExtra("sentinel_settings",true));finish()
    }
}
