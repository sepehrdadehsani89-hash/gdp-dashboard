package com.jarvis.assistant.notifications

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import com.jarvis.assistant.tools.JarvisNotificationService

class NotificationAwarenessActivity:PrivateModuleActivity(){
    private var revision by mutableIntStateOf(0)
    override fun onResume(){super.onResume();revision++}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
        val preferences=NotificationSourcePreferences(this)
        screen("JARVIS · NOTIFICATION AWARENESS"){
            val allowed=remember(revision){preferences.allowed()}
            val permission=getSystemService(android.app.NotificationManager::class.java).isNotificationListenerAccessGranted(ComponentName(this@NotificationAwarenessActivity,JarvisNotificationService::class.java))
            Text("Android access: $permission · Listener connected: ${JarvisNotificationService.connected}")
            Text("Choose each source explicitly. Jarvis reads only active Android notification previews on request, never private app databases or complete chat history. No previews are automatically sent to an AI provider. Lock-screen content, protected apps and private codes are excluded.")
            Button(onClick={startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))}){Text("Open official Android Notification access")}
            NotificationAwarenessPolicy.sources.forEach{(label,packages)->
                val installed=packages.filter{runCatching{packageManager.getApplicationInfo(it,0)}.isSuccess}
                Text(label+if(installed.isEmpty())" · not visible / not installed" else "")
                packages.forEach{pkg->
                    val enabled=pkg in allowed
                    OutlinedButton(enabled=pkg in installed || enabled,onClick={authorize{preferences.set(pkg,!enabled);revision++;status=if(enabled)"Source removed. Future requests cannot read its previews." else "Source permitted locally. Android Notification access is still required; no message was read."}}){Text((if(enabled)"Disable " else "Allow ")+pkg)}
                }
            }
            OutlinedButton(onClick={authorize{preferences.clear();revision++;status="All source consents removed. No notification content is retained by this feature."}}){Text("Stop reading all sources")}
            Button(enabled=!working,onClick={task{status=JarvisNotificationService.read(this@NotificationAwarenessActivity).message}}){Text("Test real active notification query")}
        }
    }
}
