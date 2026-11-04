package com.jarvis.assistant.quantum

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.*
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.jarvis.assistant.core.*
import com.jarvis.assistant.infinity.*
import com.jarvis.assistant.neural.AppMatching
import com.jarvis.assistant.titan.TitanLaunch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

enum class ActionStatus { VERIFIED,AWAITING_USER,UNAVAILABLE,FAILED,TIMED_OUT }
data class ActionResult(val operation:String,val status:ActionStatus,val evidence:String,val requestId:String=UUID.randomUUID().toString(),val observedAt:Long=System.currentTimeMillis())
data class ControlDefinition(val id:String,val command:String,val mechanism:String,val permission:String,val verification:String)
object ControlRegistry {
    val entries=listOf(
        ControlDefinition("bluetooth","turn on Bluetooth","Android ACTION_REQUEST_ENABLE / Bluetooth settings","BLUETOOTH_CONNECT on Android12+","Re-read BluetoothAdapter.isEnabled after Android UI"),
        ControlDefinition("bluetooth_off","turn off Bluetooth","Android Bluetooth settings only","System interaction","Never requests enabling; actual state re-read after UI"),
        ControlDefinition("wifi","turn on Wi-Fi","Android Wi-Fi panel","System interaction","WifiManager.isWifiEnabled after UI"),
        ControlDefinition("cellular","turn on mobile data","Android wireless settings","System interaction","Panel dispatch only; privileged toggling unavailable"),
        ControlDefinition("torch_on","turn on the flashlight","CameraManager torch","CAMERA","TorchCallback observes requested state"),
        ControlDefinition("torch_off","turn off the flashlight","CameraManager torch","CAMERA","TorchCallback observes requested state"),
        ControlDefinition("volume_up","increase the volume","AudioManager media stream","MODIFY_AUDIO_SETTINGS","Read stream volume after adjustment"),
        ControlDefinition("volume_down","lower the volume","AudioManager media stream","MODIFY_AUDIO_SETTINGS","Read stream volume after adjustment"),
        ControlDefinition("mute","mute media","AudioManager media stream","MODIFY_AUDIO_SETTINGS","Read actual media volume"),
        ControlDefinition("display","increase brightness","Android display settings","System interaction","Global write not attempted without special access"),
        ControlDefinition("audio_route","switch audio to my headphones","Android sound controls","System interaction","Available audio devices are not proof of active route"),
        ControlDefinition("location","open location settings","Android location settings","System interaction","LocationManager.isLocationEnabled"),
        ControlDefinition("dnd","open do not disturb settings","Android notification policy settings","System interaction","No silent notification-policy access elevation"),
        ControlDefinition("airplane","turn on airplane mode","Android airplane settings","System interaction","No privileged radio toggle"),
        ControlDefinition("hotspot","open hotspot settings","Android wireless settings","System interaction","No privileged tethering control"),
        ControlDefinition("battery_saver","open battery saver settings","Android battery-saver settings","System interaction","No fake system toggle")
    )
    fun command(input:String):String? {
        val n=AppMatching.normalize(input).removePrefix("hey jarvis ").removePrefix("oye jarvis ").removePrefix("please ")
        entries.firstOrNull{AppMatching.normalize(it.command)==n}?.let{return it.id}
        return when(n){"turn off bluetooth"->"bluetooth_off";"open bluetooth settings"->"bluetooth_off";"turn off wi fi","turn off wifi","open internet settings"->"wifi";"turn off mobile data"->"cellular";"decrease brightness","set brightness to fifty percent"->"display";"use the phone speaker","which device is handling audio","is my bluetooth microphone active"->"audio_route";"enciende la linterna"->"torch_on";"apaga la linterna"->"torch_off";"activa bluetooth"->"bluetooth";else->null}
    }
}
data class DrainSample(val at:Long,val percent:Int,val playingConfirmed:Boolean)
object AccessoryForecast {
    fun minutes(samples:List<DrainSample>,now:Long,connected:Boolean,charging:Boolean):Int? {
        if(!connected || charging || samples.size<3 || samples.any{it.percent !in 1..100 || !it.playingConfirmed})return null
        if(samples.zipWithNext().any{(a,b)->b.at<=a.at || b.percent>a.percent})return null
        val first=samples.first();val last=samples.last();val elapsed=last.at-first.at;val drop=first.percent-last.percent
        if(now-last.at !in 0..300_000 || elapsed<1_800_000 || drop<5)return null
        return (last.percent.toDouble()/drop*elapsed/60_000).toInt().takeIf{it in 1..2880}
    }
}
/** Same control engine powers native voice routing and the visible control screen. */
class DeviceControl(private val context:Context){
    val connectivity=ConnectivityRepository(context)
    private val camera=context.getSystemService(CameraManager::class.java)
    private val torch=MutableStateFlow<Boolean?>(null)
    val torchState=torch.asStateFlow()
    private var torchId:String?=null
    private var selectedTorch=false
    var last:ActionResult?=null;private set
    private val history=mutableMapOf<String,List<DrainSample>>()
    init{runCatching{torchId=camera.cameraIdList.firstOrNull{camera.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)==true};camera.registerTorchCallback(object:CameraManager.TorchCallback(){override fun onTorchModeChanged(id:String,enabled:Boolean){if(id==torchId)torch.value=enabled};override fun onTorchModeUnavailable(id:String){if(id==torchId)torch.value=null}},Handler(Looper.getMainLooper()))}}
    fun snapshot():String {
        val audio=context.getSystemService(AudioManager::class.java)
        val bt=if(Build.VERSION.SDK_INT<31 || ContextCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED)runCatching{context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled}.getOrNull() else null
        val wifi=runCatching{(context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled}.getOrNull()
        val outputs=audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).joinToString{it.productName.toString()}.ifBlank{"unavailable"}
        return "Bluetooth: ${bt ?: "permission/hardware unavailable"}\nWi-Fi enabled: ${wifi ?: "unavailable"}\nFlashlight observed: ${torch.value ?: "unavailable"}\nMedia volume: ${audio.getStreamVolume(AudioManager.STREAM_MUSIC)}/${audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}\nAvailable outputs (not proof of active playback): $outputs"
    }
    suspend fun execute(id:String):ActionResult {
        check(!context.getSystemService(android.app.KeyguardManager::class.java).isDeviceLocked){"Unlock for device controls."}
        val result=try{
            when(id){
                "torch_on","torch_off"->{
                    check(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){"Camera permission required"}
                    val target=torchId ?: error("No camera torch available");val desired=id=="torch_on"
                    camera.setTorchMode(target,desired)
                    val observed=withTimeoutOrNull(2500){torch.filter{it==desired}.first()}
                    selectedTorch=true
                    ActionResult(id,if(observed==desired)ActionStatus.VERIFIED else ActionStatus.TIMED_OUT,if(observed==desired)"Flashlight ${if(desired)"on" else "off"}; Android TorchCallback confirmed." else "Torch change requested but not observed.")
                }
                "volume_up","volume_down","mute"->{val audio=context.getSystemService(AudioManager::class.java);if(id=="mute")audio.setStreamVolume(AudioManager.STREAM_MUSIC,0,0) else audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,if(id=="volume_up")AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,0);ActionResult(id,ActionStatus.VERIFIED,"Android media volume: ${audio.getStreamVolume(AudioManager.STREAM_MUSIC)}/${audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}")}
                else->{val intent=Intent(context,ControlCenterActivity::class.java).putExtra("operation",id);ActionResult(id,ActionStatus.AWAITING_USER,TitanLaunch.open(context,intent,"Control Center"))}
            }
        }catch(e:CancellationException){throw e}catch(_:Exception){ActionResult(id,ActionStatus.FAILED,"Unavailable: check hardware, permission, audio/camera conflicts or Android settings. No change is claimed.")}
        last=result;return result
    }
    suspend fun accessories():List<Accessory> =connectivity.paired()
    suspend fun sample(device:Accessory,continuousPlaybackConfirmed:Boolean):String {
        val battery=connectivity.readBleBattery(device.address) ?: return "Battery Service unavailable. No percentage or runtime invented."
        val now=SystemClock.elapsedRealtime();val old=history[device.address].orEmpty()
        val samples=(old+DrainSample(now,battery,continuousPlaybackConfirmed)).takeLast(20)
        if(continuousPlaybackConfirmed)history[device.address]=samples
        val connectedNow=accessories().any{it.address==device.address && it.connected}
        val estimate=AccessoryForecast.minutes(samples,now,connectedNow,false)
        return "${device.name}: $battery% · BLE reading now. "+(estimate?.let{"Approximate playback remaining: $it minutes, based on user-confirmed continuous playback samples; mode/charging changes invalidate it."} ?: "Reliable playback runtime unavailable; needs at least three fresh, consistent, user-confirmed samples spanning 30 minutes and 5% discharge.")
    }
    fun clearHistory(){history.clear();selectedTorch=false}
    suspend fun route(input:String,lang:AppLanguage):AssistantReply? {
        val n=AppMatching.normalize(input)
        val operation=if(n=="turn it off" && selectedTorch)"torch_off" else ControlRegistry.command(input)
        if(operation!=null)return AssistantReply(execute(operation).evidence)
        if(n in setOf("what devices are connected","what bluetooth devices are connected","which device is connected","show connected bluetooth devices","are my earbuds connected","what s my headset battery","how much longer can my headphones play","find my headphones")){
            if(!connectivity.bluetoothAllowed())return AssistantReply("Nearby devices permission is required. Open Control Center.")
            val devices=accessories();val connected=devices.filter{it.connected}
            return AssistantReply(if(connected.isEmpty())"No connection reported by Android's queried A2DP, headset or GATT profiles. Paired devices are not assumed connected." else connected.joinToString("\n"){"${it.name}: connection reported by Android profiles. Battery/runtime unavailable until an explicit supported BLE read in Control Center."})
        }
        return null
    }
}
