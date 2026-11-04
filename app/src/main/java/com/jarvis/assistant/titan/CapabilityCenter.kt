package com.jarvis.assistant.titan

import android.Manifest
import android.app.*
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.*
import android.os.Build
import androidx.core.content.ContextCompat
import com.jarvis.assistant.AssistantRuntime
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.voice.CustomVoicePolicy

enum class CapabilityStatus { AVAILABLE,NEEDS_PERMISSION,NEEDS_SETUP,EXTERNAL_SERVICE,UNSUPPORTED,TEMPORARILY_UNAVAILABLE }
data class CapabilityReading(val name:String,val status:CapabilityStatus,val evidence:String)
object CapabilityCenter {
    fun read(context:Context,runtime:AssistantRuntime):List<CapabilityReading>{
        fun permission(name:String)=ContextCompat.checkSelfPermission(context,name)==PackageManager.PERMISSION_GRANTED
        val pm=context.packageManager
        val microphone=permission(Manifest.permission.RECORD_AUDIO)
        val camera=pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        val bluetooth=pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        val location=context.getSystemService(LocationManager::class.java)
        val notifications=context.getSystemService(NotificationManager::class.java)
        val network=context.getSystemService(ConnectivityManager::class.java).let{it.getNetworkCapabilities(it.activeNetwork)}
        val english=runtime.customVoice.config(AppLanguage.EN)
        val spanish=runtime.customVoice.config(AppLanguage.ES)
        return listOf(
            CapabilityReading("Microphone",if(microphone)CapabilityStatus.AVAILABLE else CapabilityStatus.NEEDS_PERMISSION,if(microphone)"Permission granted; actual capture/recognition must be measured in Voice Lab." else "Explicit RECORD_AUDIO approval required."),
            CapabilityReading("Offline wake model",if(runtime.installedWakeModelForCalibration()!=null)CapabilityStatus.AVAILABLE else CapabilityStatus.NEEDS_SETUP,"Selected English/Spanish Vosk model completion markers; no inference score fabricated."),
            CapabilityReading("Local AI",if(runtime.brain.modelReady())CapabilityStatus.AVAILABLE else CapabilityStatus.NEEDS_SETUP,runtime.brain.modelStatus()),
            CapabilityReading("English Fish Audio",if(english.enabled)CapabilityStatus.EXTERNAL_SERVICE else CapabilityStatus.NEEDS_SETUP,"Saved enabled profile: ${english.enabled}. Preview/account/connectivity are required; no live call in this check."),
            CapabilityReading("Exclusive Spanish Fish Audio",if(spanish.enabled && spanish.id==CustomVoicePolicy.SPANISH_ID)CapabilityStatus.EXTERNAL_SERVICE else CapabilityStatus.NEEDS_SETUP,"Only ${CustomVoicePolicy.SPANISH_ID}; never silently substitutes another speaker."),
            CapabilityReading("Wi-Fi",if(network?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)==true)CapabilityStatus.AVAILABLE else CapabilityStatus.TEMPORARILY_UNAVAILABLE,"Android active-network transport. A Wi-Fi link is not measured internet throughput."),
            CapabilityReading("Bluetooth",when{!bluetooth->CapabilityStatus.UNSUPPORTED;Build.VERSION.SDK_INT>=31 && !permission(Manifest.permission.BLUETOOTH_CONNECT)->CapabilityStatus.NEEDS_PERMISSION;else->CapabilityStatus.AVAILABLE},"Hardware feature and API-specific CONNECT permission checked; scanning/pairing require their own controls."),
            CapabilityReading("Front camera",when{!camera->CapabilityStatus.UNSUPPORTED;!permission(Manifest.permission.CAMERA)->CapabilityStatus.NEEDS_PERMISSION;else->CapabilityStatus.AVAILABLE},"Android camera feature/permission. Open preview to test the real camera."),
            CapabilityReading("Location",when{!permission(Manifest.permission.ACCESS_COARSE_LOCATION)->CapabilityStatus.NEEDS_PERMISSION;!location.isLocationEnabled->CapabilityStatus.NEEDS_SETUP;else->CapabilityStatus.AVAILABLE},"Location switch ${location.isLocationEnabled}; coarse ${permission(Manifest.permission.ACCESS_COARSE_LOCATION)}; precise ${permission(Manifest.permission.ACCESS_FINE_LOCATION)}. No fresh fix claimed."),
            CapabilityReading("Notifications",if((Build.VERSION.SDK_INT<33 || permission(Manifest.permission.POST_NOTIFICATIONS)) && notifications.areNotificationsEnabled())CapabilityStatus.AVAILABLE else CapabilityStatus.NEEDS_PERMISSION,"Android app-level permission/enabled state; each channel and OEM delivery can still be restricted."),
            CapabilityReading("Wallpaper",if(WallpaperManager.getInstance(context).isWallpaperSupported)CapabilityStatus.AVAILABLE else CapabilityStatus.UNSUPPORTED,"Active wallpaper package: "+(WallpaperManager.getInstance(context).wallpaperInfo?.packageName ?: "static/unknown")+". Rendering quality is not measured by this check."),
            CapabilityReading("Battery voice safety",if(permission(Manifest.permission.READ_PHONE_STATE))CapabilityStatus.AVAILABLE else CapabilityStatus.NEEDS_PERMISSION,"Call-state permission required for spoken alerts; missing permission retains text-only notifications. Quiet hours, DND and audio focus still apply.")
        )
    }
}
