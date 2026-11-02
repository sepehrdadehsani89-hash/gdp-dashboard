package com.jarvis.assistant.neural

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.MainActivity
import com.jarvis.assistant.hud.HudState
import com.jarvis.assistant.sentinel.*
import kotlinx.coroutines.launch

/** Public-only lock-screen presentation, entered by a notification tap. Never dismisses keyguard itself. */
class SafeActivationActivity:ComponentActivity(){
    private val runtime get()=(application as JarvisApplication).runtime
    override fun onStart(){super.onStart();runtime.hud.startVisible(this,includeWeather=false)}
    override fun onStop(){runtime.hud.stopVisible(this);super.onStop()}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
        setShowWhenLocked(true);setTurnScreenOn(true)
        setContent{MaterialTheme(colorScheme=darkColorScheme()){
            val hud by runtime.hud.state.collectAsState()
            Column(Modifier.fillMaxSize().padding(20.dp)){
                Text("JARVIS",style=MaterialTheme.typography.headlineMedium)
                Text("Public display · Android lock remains in place")
                AndroidView(factory={c->object:android.view.View(c){var reading=HudState();private val renderer=WallpaperRenderer();override fun onDraw(canvas:android.graphics.Canvas){renderer.draw(canvas,WallpaperFrame(hud=reading.copy(settings=reading.settings.copy(reducedMotion=true)),visual=SentinelVisualState(SentinelMode.DORMANT)),android.os.SystemClock.elapsedRealtime())}}},update={view->view.reading=hud;view.invalidate()},modifier=Modifier.weight(1f).fillMaxWidth())
                Button(onClick={
                    val manager=getSystemService(KeyguardManager::class.java)
                    if(!manager.isKeyguardLocked)openAssistant()
                    else manager.requestDismissKeyguard(this@SafeActivationActivity,object:KeyguardManager.KeyguardDismissCallback(){override fun onDismissSucceeded(){openAssistant()}})
                },modifier=Modifier.fillMaxWidth()){Text("Unlock with Android to open assistant")}
                TextButton(onClick={finish()}){Text("Dismiss")}
            }
        }}
    }
    private fun openAssistant(){
        if(getSystemService(KeyguardManager::class.java).isDeviceLocked)return
        startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("sentinel_wake",true));finish()
    }
}
