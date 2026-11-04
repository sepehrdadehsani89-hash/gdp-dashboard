package com.jarvis.assistant.quantum

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import com.jarvis.assistant.infinity.Accessory

class ControlCenterActivity:PrivateModuleActivity(){
    private var observing=false
    private val observer=object:android.database.ContentObserver(Handler(Looper.getMainLooper())){override fun onChange(selfChange:Boolean){refresh()}}
    private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){refresh()}}
    private var reading by mutableStateOf("")
    private var devices by mutableStateOf(emptyList<Accessory>())
    private var playbackConsent by mutableStateOf(false)
    private val permission=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){refresh()}
    private val system=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){refresh();status="Returned from Android controls. Actual states re-read below; cancellation is not success."}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return;refresh()
        screen("JARVIS · QUANTUM CONTROL CENTER"){
            val torch by runtime.deviceControl.torchState.collectAsStateWithLifecycle()
            LaunchedEffect(torch){refresh()}
            Text(reading)
            OutlinedButton(onClick={permission.launch(if(android.os.Build.VERSION.SDK_INT>=31)arrayOf(Manifest.permission.CAMERA,Manifest.permission.BLUETOOTH_CONNECT) else arrayOf(Manifest.permission.CAMERA))}){Text("Optional camera / Bluetooth permissions")}
            ControlRegistry.entries.forEach{control->
                Text("${control.command}\n${control.mechanism} · ${control.verification}")
                OutlinedButton(enabled=!working,onClick={if(control.id in listOf("torch_on","torch_off","volume_up","volume_down","mute"))task{status=runtime.deviceControl.execute(control.id).evidence;refresh()} else openSystem(control.id)}){Text(control.command)}
            }
            Button(enabled=!working,onClick={task{devices=runtime.deviceControl.accessories();status="Android profile query completed. No spatial direction/distance is inferred."}}){Text("Query actual paired / connected accessories")}
            Checkbox(playbackConsent,{playbackConsent=it});Text("Opt in to temporary drain samples: I confirm continuous playback on the selected accessory since earlier samples, with no charging or mode change. Clear samples otherwise. Samples stay in this app process only.")
            devices.forEach{d->Text("${d.name} · ${d.type} · paired ${d.paired} · connection reported ${d.connected} · RSSI ${d.rssi ?: "unavailable"}")
                OutlinedButton(enabled=!working,onClick={task{status=runtime.deviceControl.sample(d,playbackConsent)}}){Text("Explicit BLE battery read / optional confirmed-playback sample")}
            }
            TextButton(onClick={runtime.deviceControl.clearHistory();status="Temporary accessory samples and contextual selection cleared"}){Text("Clear temporary accessory history")}
            TextButton(onClick=::refresh){Text("Refresh actual control states")}
            runtime.deviceControl.last?.let{Text("Last operation: ${it.operation} · ${it.status}\n${it.evidence}")}
        }
        intent.getStringExtra("operation")?.let{operation->intent.removeExtra("operation");if(savedInstanceState==null && ControlRegistry.entries.any{it.id==operation})openSystem(operation)}
    }
    private fun refresh(){reading=runtime.deviceControl.snapshot()}
    private fun openSystem(id:String){
        val action=when(id){"wifi"->Settings.Panel.ACTION_WIFI;"bluetooth","bluetooth_off"->Settings.ACTION_BLUETOOTH_SETTINGS;"cellular","hotspot"->Settings.ACTION_WIRELESS_SETTINGS;"display"->Settings.ACTION_DISPLAY_SETTINGS;"audio_route"->Settings.ACTION_SOUND_SETTINGS;"location"->Settings.ACTION_LOCATION_SOURCE_SETTINGS;"dnd"->Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS;"airplane"->Settings.ACTION_AIRPLANE_MODE_SETTINGS;"battery_saver"->Settings.ACTION_BATTERY_SAVER_SETTINGS;else->Settings.ACTION_SETTINGS}
        try{
            if(id=="bluetooth" && (android.os.Build.VERSION.SDK_INT<31 || ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT)==android.content.pm.PackageManager.PERMISSION_GRANTED) && getSystemService(android.bluetooth.BluetoothManager::class.java)?.adapter?.isEnabled==false)system.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else system.launch(Intent(action))
            status="WAITING FOR USER · Android controls. No completed change claimed."
        }catch(_:Exception){status="Android control unavailable. Open the phone's Settings manually."}
    }
    override fun onStart(){super.onStart();if(isFinishing || observing)return
        ContextCompat.registerReceiver(this,receiver,IntentFilter().apply{addAction(BluetoothAdapter.ACTION_STATE_CHANGED);addAction(android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION)},ContextCompat.RECEIVER_EXPORTED)
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI,true,observer);observing=true
    }
    override fun onStop(){if(observing){runCatching{unregisterReceiver(receiver)};contentResolver.unregisterContentObserver(observer);observing=false};super.onStop()}
    override fun onResume(){super.onResume();if(!isFinishing)refresh()}
}
