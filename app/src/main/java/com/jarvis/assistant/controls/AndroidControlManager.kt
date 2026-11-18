package com.jarvis.assistant.controls

import android.Manifest
import android.app.*
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.*
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantReply
import com.jarvis.assistant.quantum.ActionStatus
import com.jarvis.assistant.quantum.DeviceControl
import com.jarvis.assistant.rebirth.AppLaunchCoordinator
import com.jarvis.assistant.rebirth.LaunchStatus
import com.jarvis.assistant.security.JarvisLockAdminReceiver
import com.jarvis.assistant.controls.AndroidControlId.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/** Typed, user-authorized native actions. No model tools, shell, private app data or hidden toggles. */
class AndroidControlManager(private val context:Context,private val device:DeviceControl,private val launches:AppLaunchCoordinator){
    private val mutable=MutableStateFlow<Map<AndroidControlId,ControlOutcome>>(emptyMap())
    val results=mutable.asStateFlow()
    private val journal=MutableStateFlow<List<ControlTrace>>(emptyList())
    val traces=journal.asStateFlow()
    private var epoch=0L
    data class Ticket(val id:String,val command:AndroidControlCommand,val epoch:Long,val issued:Long)
    private val pending=mutableMapOf<String,Ticket>()
    private val preferences=context.getSharedPreferences("jarvis_android_controls_v1",Context.MODE_PRIVATE)
    private val audio get()=context.getSystemService(AudioManager::class.java)
    private val notifications get()=context.getSystemService(NotificationManager::class.java)
    private fun unlocked()=context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked==false
    fun hasPermission(permission:String)=ContextCompat.checkSelfPermission(context,permission)==PackageManager.PERMISSION_GRANTED
    fun runtimePermission(command:AndroidControlCommand):String?=when(command.id){
        TORCH_ON,TORCH_OFF->Manifest.permission.CAMERA
        BLUETOOTH_ON,BLUETOOTH_OFF,BLUETOOTH_STATUS->if(Build.VERSION.SDK_INT>=31)Manifest.permission.BLUETOOTH_CONNECT else null
        else->null
    }
    @Synchronized fun takeTicket(id:String?):Ticket?=pending.remove(id)?.takeIf(::current)
    @Synchronized fun current(ticket:Ticket)=ticket.epoch==epoch && SystemClock.elapsedRealtime()-ticket.issued in 0..120_000 && unlocked()
    @Synchronized fun cancelPending(){epoch++;pending.clear()}
    private fun publish(result:ControlOutcome,input:String=result.command.id.name,origin:String="BUTTON",operation:String=result.command.id.name):ControlOutcome {
        mutable.value=mutable.value+(result.command.id to result)
        journal.value=(journal.value+ControlTrace(input.take(500),origin,result.command,AndroidControlCatalog.capability(result.command.id).permission,operation,result)).takeLast(30)
        return result
    }
    fun clearDiagnostics(){journal.value=emptyList();mutable.value=emptyMap()}
    private fun result(c:AndroidControlCommand,status:ControlResultStatus,text:String,value:String?=null)=ControlOutcome(c,status,text,value)
    private fun dispatch(c:AndroidControlCommand):ControlOutcome {
        val ticket=Ticket(UUID.randomUUID().toString(),c,epoch,SystemClock.elapsedRealtime())
        synchronized(this){pending.entries.removeAll{!current(it.value)};if(pending.size>=8)return result(c,ControlResultStatus.FAILED,"Finish or cancel the pending Android request first.");pending[ticket.id]=ticket}
        val launch=launches.open(Intent(context,AndroidControlActionActivity::class.java).putExtra("control_ticket",ticket.id),"Android ${AndroidControlCatalog.capability(c.id).name} controls")
        return if(launch.status in setOf(LaunchStatus.DISPATCHED,LaunchStatus.WAITING_FOR_USER))result(c,ControlResultStatus.USER_ACTION_REQUIRED,"Android interaction required. ${launch.detail}")
            else {synchronized(this){pending.remove(ticket.id)};result(c,if(launch.status==LaunchStatus.PROTECTED)ControlResultStatus.PERMISSION_REQUIRED else ControlResultStatus.FAILED,launch.detail)}
    }
    /** The activity uses this exact allowlist; arbitrary Intent actions are never accepted. */
    fun userIntent(c:AndroidControlCommand):Intent? {
        if(c.id==LOCK && !context.getSystemService(DevicePolicyManager::class.java).isAdminActive(ComponentName(context,JarvisLockAdminReceiver::class.java)))return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,ComponentName(context,JarvisLockAdminReceiver::class.java)).putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,"Jarvis requests only force-lock. It cannot reset your PIN, erase data or read passwords.")
        if(c.id in setOf(DND_ON,DND_OFF,RINGER_SILENT) && !notifications.isNotificationPolicyAccessGranted)return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        val action=when(c.id){
            WIFI_ON,WIFI_OFF,WIFI_SETTINGS->Settings.Panel.ACTION_WIFI
            BLUETOOTH_ON->if(runCatching{context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled==false}.getOrDefault(false))android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE else Settings.ACTION_BLUETOOTH_SETTINGS
            BLUETOOTH_OFF,BLUETOOTH_SETTINGS->Settings.ACTION_BLUETOOTH_SETTINGS
            HOTSPOT->Settings.ACTION_WIRELESS_SETTINGS
            AIRPLANE->Settings.ACTION_AIRPLANE_MODE_SETTINGS
            POWER_SAVER->Settings.ACTION_BATTERY_SAVER_SETTINGS
            DISPLAY,ROTATION,DARK_MODE,READING_MODE->Settings.ACTION_DISPLAY_SETTINGS
            SOUND->Settings.ACTION_SOUND_SETTINGS
            CAST->Settings.ACTION_CAST_SETTINGS
            FOCUS,BEDTIME->Settings.ACTION_SETTINGS
            WALLET->Settings.ACTION_NFC_PAYMENT_SETTINGS
            else->null
        }
        return action?.let{Intent(it)}
    }
    suspend fun execute(command:AndroidControlCommand,input:String=command.id.name,origin:String="BUTTON",allowUi:Boolean=true,host:Activity?=null):ControlOutcome {
        command.validated()
        val c=command
        if(c.id==UNLOCK){if(unlocked())return publish(result(c,ControlResultStatus.SUCCESS,"Your phone is already unlocked."),input,origin);val message=com.jarvis.assistant.security.ScreenSecurityController(host ?: context).requestUnlock(forceNotification=host==null);return publish(result(c,ControlResultStatus.USER_ACTION_REQUIRED,message),input,origin)}
        if(!unlocked() && c.id!=BATTERY)return publish(result(c,ControlResultStatus.PERMISSION_REQUIRED,"Unlock with Android before using this control."),input,origin)
        val permission=runtimePermission(c)
        if(permission!=null && !hasPermission(permission))return publish(if(allowUi)dispatch(c) else result(c,ControlResultStatus.PERMISSION_REQUIRED,"Grant the requested Android permission before this action can run. No change is claimed."),input,origin)
        if(userIntent(c)!=null)return publish(if(allowUi)dispatch(c) else result(c,ControlResultStatus.USER_ACTION_REQUIRED,"Use the official Android control screen. No global setting change has been verified."),input,origin)
        val outcome=try{when(c.id){
            TORCH_ON,TORCH_OFF->{val r=device.execute(if(c.id==TORCH_ON)"torch_on" else "torch_off");result(c,when(r.status){ActionStatus.VERIFIED->ControlResultStatus.SUCCESS;ActionStatus.TIMED_OUT->ControlResultStatus.TIMEOUT;ActionStatus.UNAVAILABLE->ControlResultStatus.UNSUPPORTED;ActionStatus.AWAITING_USER->ControlResultStatus.USER_ACTION_REQUIRED;else->ControlResultStatus.FAILED},r.evidence,device.torchState.value?.toString())}
            VOLUME_UP,VOLUME_DOWN,VOLUME_SET,MEDIA_MUTE,MEDIA_UNMUTE->changeVolume(c)
            VOLUME_STATUS->result(c,ControlResultStatus.SUCCESS,"Android media volume is ${audio.getStreamVolume(AudioManager.STREAM_MUSIC)} of ${audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}.",audio.getStreamVolume(AudioManager.STREAM_MUSIC).toString())
            RINGER_SILENT,RINGER_NORMAL,VIBRATE->{val desired=when(c.id){RINGER_SILENT->AudioManager.RINGER_MODE_SILENT;VIBRATE->AudioManager.RINGER_MODE_VIBRATE;else->AudioManager.RINGER_MODE_NORMAL};audio.ringerMode=desired;val value=audio.ringerMode;result(c,if(value==desired)ControlResultStatus.SUCCESS else ControlResultStatus.FAILED,"Android reports ringer mode ${when(value){0->"silent";1->"vibrate";else->"normal"}}. This does not change media or alarm volume.",value.toString())}
            DND_ON,DND_OFF->{val desired=if(c.id==DND_ON)NotificationManager.INTERRUPTION_FILTER_PRIORITY else NotificationManager.INTERRUPTION_FILTER_ALL;notifications.setInterruptionFilter(desired);val actual=notifications.currentInterruptionFilter;result(c,if(actual==desired)ControlResultStatus.SUCCESS else ControlResultStatus.FAILED,"Android interruption filter is $actual. ${if(actual==desired)"The requested filter was observed." else "The requested global filter was not observed; another policy may apply."}",actual.toString())}
            DND_STATUS->result(c,ControlResultStatus.SUCCESS,"Android interruption filter: ${notifications.currentInterruptionFilter}. Policy access granted: ${notifications.isNotificationPolicyAccessGranted}.",notifications.currentInterruptionFilter.toString())
            WIFI_STATUS->wifiResult(c)
            BLUETOOTH_STATUS->bluetoothResult(c)
            LOCK->{context.getSystemService(DevicePolicyManager::class.java).lockNow();val locked=context.getSystemService(KeyguardManager::class.java).isDeviceLocked;result(c,if(locked)ControlResultStatus.SUCCESS else ControlResultStatus.USER_ACTION_REQUIRED,if(locked)"Android confirms the phone is locked." else "Android accepted the force-lock request; locked state has not yet been observed.")}
            BATTERY->battery(c)
            PORTRAIT,LANDSCAPE->{if(host==null){if(allowUi)dispatch(c) else result(c,ControlResultStatus.USER_ACTION_REQUIRED,"Open the Jarvis control activity to change its own orientation.")}else{val portrait=c.id==PORTRAIT;host.requestedOrientation=if(portrait)android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;val observed=host.resources.configuration.orientation==(if(portrait)android.content.res.Configuration.ORIENTATION_PORTRAIT else android.content.res.Configuration.ORIENTATION_LANDSCAPE);result(c,if(observed)ControlResultStatus.SUCCESS else ControlResultStatus.USER_ACTION_REQUIRED,"Jarvis control activity requested ${if(portrait)"portrait" else "landscape"}. ${if(observed)"Its configuration confirms this orientation." else "Android has not yet reported the new configuration."} System-wide rotation is unchanged.")}}
            RECORD_START->{if(allowUi)dispatch(c) else result(c,ControlResultStatus.USER_ACTION_REQUIRED,"Screen recording requires fresh Android capture consent. It records video without microphone audio.")}
            RECORD_STOP->{val requested=com.jarvis.assistant.screen.VideoRecordingService.requestStop(context);result(c,if(requested)ControlResultStatus.USER_ACTION_REQUIRED else ControlResultStatus.SUCCESS,if(requested)"The recording stop request was sent. Check the recording result; no saved video is claimed until finalization." else "No owned video recording is active.")}
            SHARE,QUICK_SHARE,MI_SHARE->{val launch=launches.open(Intent(context,com.jarvis.assistant.rebirth.PersonalContentActivity::class.java),"authorized file selection and sharing");result(c,ControlResultStatus.USER_ACTION_REQUIRED,"Select a file you authorize, then use Android's Sharesheet. Quick Share and Mi Share depend on installed services. ${launch.detail}")}
            TV_REMOTE->result(c,ControlResultStatus.UNSUPPORTED,"No TV is paired to an authorized remote integration. Open your television's supported remote app and pair it there; no TV command was sent.")
            else->result(c,ControlResultStatus.UNSUPPORTED,"This operation requires a supported device API or system screen. No unsupported change was attempted.")
        }}catch(e:CancellationException){throw e}catch(_:SecurityException){result(c,ControlResultStatus.PERMISSION_REQUIRED,"Android denied the operation. Review this control's specific permission; no change is claimed.")}
        catch(_:Exception){result(c,ControlResultStatus.FAILED,"Android could not finish this operation. Check its hardware, active apps and system restrictions; no success is claimed.")}
        return publish(outcome,input,origin)
    }
    private fun changeVolume(c:AndroidControlCommand):ControlOutcome {
        if(audio.isVolumeFixed)return result(c,ControlResultStatus.UNSUPPORTED,"Android reports fixed media volume on this output.")
        val stream=AudioManager.STREAM_MUSIC;val maximum=audio.getStreamMaxVolume(stream);val before=audio.getStreamVolume(stream)
        val desired=when(c.id){VOLUME_UP->(before+1).coerceAtMost(maximum);VOLUME_DOWN->(before-1).coerceAtLeast(0);VOLUME_SET->((c.percent!!*maximum+50)/100).coerceIn(0,maximum);MEDIA_MUTE->0;else->preferences.getInt("media_before_mute",1).coerceIn(1,maximum.coerceAtLeast(1))}
        audio.setStreamVolume(stream,desired,0);val actual=audio.getStreamVolume(stream)
        if(c.id==MEDIA_MUTE && before>0 && actual==0)preferences.edit().putInt("media_before_mute",before).apply()
        return result(c,if(actual==desired)ControlResultStatus.SUCCESS else ControlResultStatus.FAILED,"Android media volume: $actual of $maximum. ${if(actual==desired)"Requested level observed." else "Requested level was not observed."}",actual.toString())
    }
    private fun wifiResult(c:AndroidControlCommand):ControlOutcome {val actual=(context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled;val expected=when(c.id){WIFI_ON->true;WIFI_OFF->false;else->null};return result(c,if(expected==null || expected==actual)ControlResultStatus.SUCCESS else ControlResultStatus.USER_ACTION_REQUIRED,"Android reports Wi-Fi ${if(actual)"on" else "off"}. ${if(expected!=null && expected!=actual)"The requested state is not active; change it in Android's panel." else "This is a state reading, not a claim of silent toggling."}",actual.toString())}
    private fun bluetoothResult(c:AndroidControlCommand):ControlOutcome {if(Build.VERSION.SDK_INT>=31 && ContextCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)return result(c,ControlResultStatus.PERMISSION_REQUIRED,"Allow Android Nearby devices access first.");val adapter=context.getSystemService(BluetoothManager::class.java)?.adapter ?: return result(c,ControlResultStatus.UNSUPPORTED,"No Bluetooth adapter is available.");val actual=adapter.isEnabled;val expected=when(c.id){BLUETOOTH_ON->true;BLUETOOTH_OFF->false;else->null};return result(c,if(expected==null || expected==actual)ControlResultStatus.SUCCESS else ControlResultStatus.USER_ACTION_REQUIRED,"Android reports Bluetooth ${if(actual)"on" else "off"}. ${if(expected!=null && expected!=actual)"Use Android's controls to reach the requested state." else "Adapter state does not mean a specific accessory is connected."}",actual.toString())}
    private fun battery(c:AndroidControlCommand):ControlOutcome {
        val battery=context.getSystemService(BatteryManager::class.java);val percent=battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if(percent !in 0..100)return result(c,ControlResultStatus.UNSUPPORTED,"Android did not report a usable battery percentage.")
        val reading=context.registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED));val status=reading?.getIntExtra(BatteryManager.EXTRA_STATUS,-1)
        val charging=status in setOf(BatteryManager.BATTERY_STATUS_CHARGING,BatteryManager.BATTERY_STATUS_FULL)
        val health=when(reading?.getIntExtra(BatteryManager.EXTRA_HEALTH,-1)){BatteryManager.BATTERY_HEALTH_GOOD->"good";BatteryManager.BATTERY_HEALTH_OVERHEAT->"overheated";BatteryManager.BATTERY_HEALTH_DEAD->"dead";BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE->"over voltage";BatteryManager.BATTERY_HEALTH_COLD->"cold";else->"unavailable"}
        return result(c,ControlResultStatus.SUCCESS,"Battery is $percent percent. ${if(status==null || status==-1)"Charging state unavailable." else if(charging)"Charging or full." else "Not charging."} Android battery health: $health. Battery Saver: ${context.getSystemService(PowerManager::class.java).isPowerSaveMode}.",percent.toString())
    }
    suspend fun afterUserAction(c:AndroidControlCommand,host:Activity):ControlOutcome=when(c.id){WIFI_ON,WIFI_OFF,WIFI_SETTINGS->publish(wifiResult(c));BLUETOOTH_ON,BLUETOOTH_OFF,BLUETOOTH_SETTINGS->publish(bluetoothResult(c));LOCK,DND_ON,DND_OFF,RINGER_SILENT->execute(c,allowUi=false,host=host);else->publish(result(c,ControlResultStatus.USER_ACTION_REQUIRED,"Returned from Android settings. No setting change or device connection was independently verified."))}
    suspend fun route(input:String,language:AppLanguage,voiced:Boolean):AssistantReply? {
        val command=AndroidControlParser.parse(input) ?: return null
        val result=execute(command,input,if(voiced)"SPEECH_RECOGNITION" else "TYPED_COMMAND")
        return AssistantReply(result.detail,allowCloudSpeech=true)
    }
    fun observeRecording(status:com.jarvis.assistant.screen.RecordingStatus){
        val command=AndroidControlCommand(if(status.stage in setOf(com.jarvis.assistant.screen.RecordingStage.FINALIZING,com.jarvis.assistant.screen.RecordingStage.SAVED))RECORD_STOP else RECORD_START)
        val statusValue=when(status.stage){com.jarvis.assistant.screen.RecordingStage.RECORDING,com.jarvis.assistant.screen.RecordingStage.SAVED->ControlResultStatus.SUCCESS;com.jarvis.assistant.screen.RecordingStage.FAILED->ControlResultStatus.FAILED;else->ControlResultStatus.USER_ACTION_REQUIRED}
        publish(result(command,statusValue,status.message),origin="ANDROID_RECORDING_CALLBACK",operation=status.stage.name)
    }
}
