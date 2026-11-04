package com.jarvis.assistant.titan

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.MainActivity
import com.jarvis.assistant.R

object TitanLaunch {
    fun open(context:Context,intent:Intent,label:String):String {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if(com.jarvis.assistant.rebirth.AppVisibility.visible)return try{context.startActivity(intent);"Android opening requested: $label. Foreground/result is controlled by Android."}catch(_:Exception){"No available app could open $label."}
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return "Open Jarvis to continue $label. Android notification permission is needed for a background request."
        return try{
            val manager=context.getSystemService(NotificationManager::class.java)
            if(!manager.areNotificationsEnabled())return "Open Jarvis to continue $label; notifications are disabled."
            manager.createNotificationChannel(NotificationChannel("jarvis_titan_tasks","Jarvis requested tasks",NotificationManager.IMPORTANCE_DEFAULT))
            val open=PendingIntent.getActivity(context,7107,intent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            manager.notify(7107,NotificationCompat.Builder(context,"jarvis_titan_tasks").setSmallIcon(R.drawable.ic_jarvis).setContentTitle("Jarvis · requested task")
                .setContentText("Tap to continue your requested task. Unlock normally for private information.").setContentIntent(open).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setTimeoutAfter(120_000).build())
            "Tap the Jarvis task notification to open $label; Android requires foreground interaction."
        }catch(_:Exception){"Android could not open $label. Open Jarvis and retry."}
    }
}
