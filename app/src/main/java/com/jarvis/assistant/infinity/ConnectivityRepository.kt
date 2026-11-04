package com.jarvis.assistant.infinity

import android.Manifest
import android.bluetooth.*
import android.content.*
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.WifiManager
import android.os.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import java.io.Closeable

/** On-demand, public Android APIs only. No background scans, hidden battery APIs or inferred connections. */
@android.annotation.SuppressLint("MissingPermission") // Checked at each public operation; revocation errors reach the caller.
class ConnectivityRepository(private val context:Context):Closeable {
    private val adapter get()=context.getSystemService(BluetoothManager::class.java)?.adapter
    private var receiver:BroadcastReceiver?=null
    fun allowed(permission:String)=ContextCompat.checkSelfPermission(context,permission)==PackageManager.PERMISSION_GRANTED
    fun bluetoothAllowed()=Build.VERSION.SDK_INT<31 || allowed(Manifest.permission.BLUETOOTH_CONNECT)
    fun scanPermissions():Array<String> = if(Build.VERSION.SDK_INT>=31)arrayOf(Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    @Suppress("MissingPermission")
    suspend fun paired():List<Accessory> {
        check(bluetoothAllowed()){"Bluetooth permission is required. Open Infinity device controls in Settings."}
        val a=adapter ?: error("Bluetooth hardware is unavailable on this device.")
        check(a.isEnabled){"Bluetooth is off. Turn it on in Android settings."}
        val connected=mutableSetOf<String>()
        val observedDevices=mutableMapOf<String,BluetoothDevice>()
        for(profile in listOf(BluetoothProfile.A2DP,BluetoothProfile.HEADSET)) {
            connected+=withTimeoutOrNull(1500){withContext(Dispatchers.Main){suspendCancellableCoroutine<Set<String>>{continuation->
                val listener=object:BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(kind:Int,proxy:BluetoothProfile){
                        val result=runCatching{proxy.connectedDevices.onEach{observedDevices[it.address]=it}.map{it.address}.toSet()}.getOrDefault(emptySet())
                        runCatching{a.closeProfileProxy(kind,proxy)}
                        if(continuation.isActive)continuation.resume(result)
                    }
                    override fun onServiceDisconnected(kind:Int){if(continuation.isActive)continuation.resume(emptySet())}
                }
                if(!a.getProfileProxy(context,listener,profile) && continuation.isActive)continuation.resume(emptySet())
            }}} ?: emptySet()
        }
        connected+=runCatching{context.getSystemService(BluetoothManager::class.java).getConnectedDevices(BluetoothProfile.GATT).onEach{observedDevices[it.address]=it}.map{it.address}}.getOrDefault(emptyList())
        val bonded=a.bondedDevices
        return (bonded+observedDevices.values).distinctBy{it.address}.map{device->accessory(device,bonded.any{it.address==device.address}).copy(connected=device.address in connected)}.sortedWith(compareBy<Accessory>{it.name.lowercase()}.thenBy{it.address})
    }
    @Suppress("MissingPermission")
    private fun accessory(device:BluetoothDevice,paired:Boolean,rssi:Int?=null)=Accessory(device.address,device.name?.takeIf{it.isNotBlank()} ?: "Unnamed accessory",when(device.type){BluetoothDevice.DEVICE_TYPE_LE->"BLE";BluetoothDevice.DEVICE_TYPE_CLASSIC->"Classic";BluetoothDevice.DEVICE_TYPE_DUAL->"Dual";else->"Unknown type"},paired,rssi=rssi)
    @Suppress("MissingPermission","DEPRECATION")
    fun startScan(found:(Accessory)->Unit){
        close();check(scanPermissions().all(::allowed)){"Bluetooth discovery permission is required."}
        val a=adapter ?: error("Bluetooth hardware unavailable.");check(a.isEnabled){"Enable Bluetooth in Android settings first."}
        receiver=object:BroadcastReceiver(){override fun onReceive(c:Context,intent:Intent){
            if(intent.action!=BluetoothDevice.ACTION_FOUND)return
            val device=if(Build.VERSION.SDK_INT>=33)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE,BluetoothDevice::class.java) else intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            if(device!=null && bluetoothAllowed())runCatching{found(accessory(device,device.bondState==BluetoothDevice.BOND_BONDED,if(intent.hasExtra(BluetoothDevice.EXTRA_RSSI))intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,0).toInt() else null))}
        }}
        ContextCompat.registerReceiver(context,receiver,IntentFilter(BluetoothDevice.ACTION_FOUND),ContextCompat.RECEIVER_EXPORTED)
        check(a.startDiscovery()){"Android could not start discovery. Location services may be needed on older Android versions."}
    }
    @Suppress("MissingPermission")
    override fun close(){receiver?.let{runCatching{context.unregisterReceiver(it)}};receiver=null;if(scanPermissions().all(::allowed))runCatching{adapter?.cancelDiscovery()}}
    @Suppress("DEPRECATION","MissingPermission")
    suspend fun readBleBattery(address:String):Int?=withContext(Dispatchers.IO){
        check(bluetoothAllowed()){"Bluetooth permission required."}
        val device=adapter?.bondedDevices?.firstOrNull{it.address==address} ?: error("Choose a currently paired device.")
        require(device.type==BluetoothDevice.DEVICE_TYPE_LE || device.type==BluetoothDevice.DEVICE_TYPE_DUAL){"This Classic accessory does not expose a public BLE battery service here."}
        var connection:BluetoothGatt?=null
        try{withTimeoutOrNull(12_000){suspendCancellableCoroutine<Int?>{cont->
            val completed=java.util.concurrent.atomic.AtomicBoolean(false)
            fun finish(value:Int?){if(completed.compareAndSet(false,true) && cont.isActive)cont.resume(value)}
            val callback=object:BluetoothGattCallback(){
                override fun onConnectionStateChange(gatt:BluetoothGatt,status:Int,newState:Int){if(!cont.isActive)return;if(status==BluetoothGatt.GATT_SUCCESS && newState==BluetoothProfile.STATE_CONNECTED){if(!runCatching{gatt.discoverServices()}.getOrDefault(false))finish(null)} else if(status!=BluetoothGatt.GATT_SUCCESS || newState==BluetoothProfile.STATE_DISCONNECTED)finish(null)}
                override fun onServicesDiscovered(gatt:BluetoothGatt,status:Int){if(!cont.isActive)return;val requested=runCatching{val characteristic=gatt.getService(java.util.UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb"))?.getCharacteristic(java.util.UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"));status==BluetoothGatt.GATT_SUCCESS && characteristic!=null && gatt.readCharacteristic(characteristic)}.getOrDefault(false);if(!requested)finish(null)}
                override fun onCharacteristicRead(gatt:BluetoothGatt,c:BluetoothGattCharacteristic,status:Int){if(Build.VERSION.SDK_INT<33)finish(if(status==BluetoothGatt.GATT_SUCCESS)c.value?.firstOrNull()?.toInt()?.and(255)?.takeIf{it in 0..100} else null)}
                override fun onCharacteristicRead(gatt:BluetoothGatt,c:BluetoothGattCharacteristic,value:ByteArray,status:Int){finish(if(status==BluetoothGatt.GATT_SUCCESS)value.firstOrNull()?.toInt()?.and(255)?.takeIf{it in 0..100} else null)}
            }
            connection=device.connectGatt(context,false,callback,BluetoothDevice.TRANSPORT_LE)
            if(connection==null)finish(null)
            cont.invokeOnCancellation{completed.set(true);runCatching{connection?.disconnect()}}
        }}}finally{connection?.let{runCatching{it.disconnect()};runCatching{it.close()}}}
    }
    @Suppress("DEPRECATION")
    fun wifi():String {
        val manager=context.getSystemService(ConnectivityManager::class.java)
        val network=manager.activeNetwork ?: return "No active Android network."
        val caps=manager.getNetworkCapabilities(network) ?: return "Network capabilities unavailable."
        val wifi=caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val validated=caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val captive=caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
        val info=if(wifi)(context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo else null
        val ssid=DevicePolicy.usableSsid(info?.ssid)
        val links=manager.getLinkProperties(network)
        return buildString {
            appendLine("Active transport: ${if(wifi)"Wi-Fi" else if(caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))"Cellular" else "Other"}")
            appendLine("Android internet validation: $validated; captive portal: $captive")
            if(wifi){appendLine("SSID: ${ssid ?: "Redacted/unavailable; location permission and location services may be required"}");appendLine("RSSI: ${DevicePolicy.wifiRssi(info?.rssi)?.let{"$it dBm"} ?: "Unavailable"}");appendLine("Frequency: ${info?.frequency?.takeIf{it>0}?.let{"$it MHz"} ?: "Unavailable"}");appendLine("Wi-Fi link rate: ${info?.linkSpeed?.takeIf{it>0}?.let{"$it Mbps"} ?: "Unavailable"} (not measured internet speed)")}
            appendLine("IP: ${links?.linkAddresses?.joinToString{it.toString()}?.ifBlank{"Unavailable"} ?: "Unavailable"}")
            appendLine("DNS: ${links?.dnsServers?.joinToString{it.hostAddress.orEmpty()}?.ifBlank{"Unavailable"} ?: "Unavailable"}")
            append(if(captive)"Android reports a sign-in portal. Open Internet settings and sign in." else if(!validated)"Android has not validated internet access. Check the router, sign-in and mobile/Wi-Fi settings; the cause is not determined." else "Android reports validated internet. A low link rate or weak RSSI alone does not prove an ISP fault.")
        }
    }
    fun battery():String {
        val data=context.registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val manager=context.getSystemService(BatteryManager::class.java)
        fun property(id:Int)=DevicePolicy.knownProperty(manager.getLongProperty(id))
        val level=manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf{it in 0..100}
        val health=when(data?.getIntExtra(BatteryManager.EXTRA_HEALTH,0)){BatteryManager.BATTERY_HEALTH_GOOD->"Good";BatteryManager.BATTERY_HEALTH_OVERHEAT->"Overheating";BatteryManager.BATTERY_HEALTH_DEAD->"Dead";BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE->"Over voltage";BatteryManager.BATTERY_HEALTH_COLD->"Cold";BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE->"Unspecified failure";else->"Unavailable"}
        val temp=data?.takeIf{it.hasExtra(BatteryManager.EXTRA_TEMPERATURE)}?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)?.div(10.0)
        val voltage=data?.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0)?.takeIf{it>0}
        return "Battery: ${level?.let{"$it%"} ?: "Unavailable"}\nCharging: ${manager.isCharging}\nAndroid health classification: $health\nTemperature: ${temp?.let{"$it °C"} ?: "Unavailable"}\nVoltage: ${voltage?.let{"$it mV"} ?: "Unavailable"}\nCurrent: ${property(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.let{"$it µA (device sign convention)"} ?: "Unavailable"}\nCharge counter: ${property(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)?.takeIf{it>=0}?.let{"$it µAh"} ?: "Unavailable"}\nRemaining capacity health percentage and charging completion time are not measured here. Android health classification is not remaining capacity."
    }
}
