package com.jarvis.assistant.rebirth

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.jarvis.assistant.R
import com.jarvis.assistant.tools.ToolPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppVisibility:Application.ActivityLifecycleCallbacks {
    private val activities=java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Activity,Boolean>())
    private val mutable=MutableStateFlow(false);val state=mutable.asStateFlow()
    val visible:Boolean get()=mutable.value
    override fun onActivityStarted(a:Activity){activities.add(a);mutable.value=activities.isNotEmpty()}
    override fun onActivityStopped(a:Activity){activities.remove(a);mutable.value=activities.isNotEmpty()}
    override fun onActivityDestroyed(a:Activity){activities.remove(a);mutable.value=activities.isNotEmpty()}
    override fun onActivityCreated(a:Activity,b:android.os.Bundle?){}
    override fun onActivityResumed(a:Activity){}
    override fun onActivityPaused(a:Activity){}
    override fun onActivitySaveInstanceState(a:Activity,b:android.os.Bundle){}
}
enum class LaunchStatus { DISPATCHED, WAITING_FOR_USER, NOT_AVAILABLE_OR_VISIBLE, LAUNCH_INTENT_MISSING, BACKGROUND_RESTRICTED, PROTECTED, FAILED }
data class LaunchResult(val status:LaunchStatus,val detail:String,val packageId:String?=null)

/** Report dispatch/notification evidence. No foreground-app spying or invented launch completion. */
class AppLaunchCoordinator(private val context:Context){
    @Volatile var last:LaunchResult?=null;private set
    fun launchPackage(pkg:String,label:String):LaunchResult {
        if(ToolPolicy.protectedPackage(pkg))return result(LaunchStatus.PROTECTED,"Open this protected app yourself.",pkg)
        val intent=context.packageManager.getLaunchIntentForPackage(pkg)
            ?: return result(LaunchStatus.LAUNCH_INTENT_MISSING,"Android did not expose a launch intent. The app may be unavailable or hidden by package visibility. Check the saved alias.",pkg)
        return open(intent,label,pkg)
    }
    fun open(intent:Intent,label:String,pkg:String?=null):LaunchResult {
        if(context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked!=false)return result(LaunchStatus.PROTECTED,"Unlock with Android before opening private interfaces.",pkg)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val runtime=(context.applicationContext as com.jarvis.assistant.JarvisApplication).runtime
        val visibleOverlay=android.provider.Settings.canDrawOverlays(context) && runtime.phantomOverlay.isShowing
        if(AppVisibility.visible || visibleOverlay)return try{context.startActivity(intent);result(LaunchStatus.DISPATCHED,"Sent a launch request to Android for $label. Foreground opening is controlled by Android and was not independently verified.",pkg)}catch(_:SecurityException){result(LaunchStatus.PROTECTED,"Android denied this launch. Check the app's access settings.",pkg)}catch(_:Exception){result(LaunchStatus.FAILED,"Android could not dispatch $label. Check the saved app and try again.",pkg)}
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return result(LaunchStatus.BACKGROUND_RESTRICTED,"Android restricts background launches. Open Jarvis or allow its task notifications to continue $label.",pkg)
        return try{
            val manager=context.getSystemService(NotificationManager::class.java)
            if(!manager.areNotificationsEnabled())return result(LaunchStatus.BACKGROUND_RESTRICTED,"Task notifications are disabled. Open Jarvis to continue $label.",pkg)
            manager.createNotificationChannel(NotificationChannel("jarvis_rebirth_launch","Jarvis requested apps",NotificationManager.IMPORTANCE_DEFAULT))
            if(manager.getNotificationChannel("jarvis_rebirth_launch").importance==NotificationManager.IMPORTANCE_NONE)return result(LaunchStatus.BACKGROUND_RESTRICTED,"The requested-app notification channel is disabled. Open Jarvis to continue.",pkg)
            val pending=PendingIntent.getActivity(context,7112,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.notify(7112,NotificationCompat.Builder(context,"jarvis_rebirth_launch").setSmallIcon(R.drawable.ic_jarvis).setContentTitle("Jarvis · requested app")
                .setContentText("Tap to continue your requested app. Unlock normally for private information.").setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setContentIntent(pending).setAutoCancel(true).setTimeoutAfter(120_000).build())
            result(LaunchStatus.WAITING_FOR_USER,"Tap the Jarvis requested-app notification to open $label. Android requires your interaction; the app has not been reported as opened.",pkg)
        }catch(_:Exception){result(LaunchStatus.FAILED,"Android could not show the launch request. Open Jarvis and retry.",pkg)}
    }
    private fun result(status:LaunchStatus,text:String,pkg:String?)=LaunchResult(status,text,pkg).also{last=it}
}
