package com.jarvis.assistant.sentinel

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import kotlinx.coroutines.*

class WallpaperRepairActivity:PrivateModuleActivity(){
    private var selected by mutableStateOf(false)
    private var frames by mutableStateOf(0)
    private var details by mutableStateOf("")
    private var monitor:Job?=null
    override fun onCreate(state:Bundle?){super.onCreate(state);if(isFinishing)return
        screen("JARVIS · MOVING WALLPAPER"){
            Text(if(selected)"Android has selected the real Jarvis live wallpaper." else "The Jarvis live wallpaper is not currently selected. A saved image or screenshot cannot animate.")
            Text("Actual rendered frames in this app process: $frames\n$details")
            Button(onClick={startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,ComponentName(this@WallpaperRepairActivity,JarvisWallpaperService::class.java)))}){Text("Select the real moving Jarvis wallpaper")}
            Text("Approve the live wallpaper in Android's preview. Return to the home screen to see it move; Android pauses wallpaper hidden behind a full-screen app or while the screen is off.")
            OutlinedButton(onClick={runtime.restoreCoreMotion();status="App motion restored. Android accessibility/reduced-motion settings still take priority."}){Text("Restore app core rotation and glow")}
            Text("Android's animation accessibility setting, heat and power saving are respected. This screen reports actual selection and render counters, not an invented frame rate. No microphone is enabled by applying wallpaper.")
        }
    }
    override fun onResume(){super.onResume();monitor?.cancel();monitor=kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch{while(isActive){
        selected=runCatching{getSystemService(WallpaperManager::class.java).wallpaperInfo?.component==ComponentName(this@WallpaperRepairActivity,JarvisWallpaperService::class.java)}.getOrDefault(false)
        frames=JarvisWallpaperService.renderedFrames
        val s=runtime.hud.state.value
        details=(CoreMotionPolicy.pauseReason(s.settings,s.telemetry.systemMotion) ?: "Motion enabled")+"\nVisible live engines: ${JarvisWallpaperService.visibleEngineCount}\n${JarvisWallpaperService.frameTimingSummary}\nRecovered rendering errors: ${JarvisWallpaperService.recoveredErrors}"
        delay(1000)
    }}}
    override fun onPause(){monitor?.cancel();monitor=null;super.onPause()}
}
