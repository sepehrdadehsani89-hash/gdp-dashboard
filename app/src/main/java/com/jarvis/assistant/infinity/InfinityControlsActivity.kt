package com.jarvis.assistant.infinity

import android.Manifest
import android.app.AlertDialog
import android.app.KeyguardManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.core.choose
import com.jarvis.assistant.neural.WebResearch
import kotlinx.coroutines.*

/** Foreground-only device diagnostics. Starting a scan, GATT read or transfer always requires a tap. */
class InfinityControlsActivity:ComponentActivity(){
    private val repository by lazy{ConnectivityRepository(this)}
    private val preferences by lazy{InfinityPreferences(this)}
    private var devices by mutableStateOf(emptyList<Accessory>())
    private var nearby by mutableStateOf(emptyList<Accessory>())
    private var status by mutableStateOf("")
    private var busy by mutableStateOf(false)
    private var revision by mutableIntStateOf(0)
    private var operation:Job?=null
    private val bluetooth=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){grants->status=if(grants.values.all{it})"Bluetooth permission allowed. Press Refresh or Discover to continue." else "Bluetooth permission denied. Android settings remain available."}
    private val location=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){status="Location permission updated. Press Read Wi-Fi status; Android may still redact the SSID if location services are off."}
    private fun runOperation(block:suspend ()->Unit){if(busy)return;busy=true;operation=lifecycleScope.launch{try{block()}catch(e:CancellationException){status="Cancelled. No complete result is claimed.";throw e}catch(e:Exception){status=e.message ?: "Unavailable."}finally{repository.close();busy=false;revision++}}}
    private fun panel(id:String){runCatching{startActivity(SystemPanels.intent(id));status="AWAITING_USER: Android controls opened; no setting change or connection has been verified."}.onFailure{status="UNSUPPORTED: Android has no handler for this control."}}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if(getSystemService(KeyguardManager::class.java).isDeviceLocked){finish();return}
        setContent{MaterialTheme(colorScheme=darkColorScheme()){
            val lang=(application as JarvisApplication).runtime.store.settings.collectAsState().value.language
            var sites by remember{mutableStateOf(preferences.websites())}
            var aliases by remember{mutableStateOf(preferences.aliases())}
            var siteName by remember{mutableStateOf("")};var siteUrl by remember{mutableStateOf("")};var description by remember{mutableStateOf("")}
            var alias by remember{mutableStateOf("")}
            LaunchedEffect(revision){sites=preferences.websites();aliases=preferences.aliases()}
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides if(lang==com.jarvis.assistant.core.AppLanguage.FA)androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr){Surface(Modifier.fillMaxSize()){
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                    TextButton(onClick={finish()}){Text(lang.choose("Back to Jarvis","Volver a Jarvis","بازگشت به جارویس"))}
                    Text(lang.choose("INFINITY DEVICE CONTROLS","CONTROLES INFINITY","کنترل‌های دستگاه اینفینیتی"),style=MaterialTheme.typography.headlineSmall)
                    Text(lang.choose("On-demand readings from Android. Permission denial and missing hardware are reported, never replaced with example data.","Lecturas reales bajo demanda. Los datos o permisos ausentes se indican sin inventarlos.","خوانش واقعی و درخواستی از اندروید؛ نبود مجوز یا سخت‌افزار با دادهٔ ساختگی جایگزین نمی‌شود."))
                    if(status.isNotBlank())Text(status,color=MaterialTheme.colorScheme.primary)
                    if(busy)OutlinedButton(onClick={operation?.cancel()}){Text(lang.choose("Cancel operation","Cancelar operación","لغو عملیات"))}
                    Text("Wi-Fi & battery",style=MaterialTheme.typography.titleLarge)
                    OutlinedButton(enabled=!busy,onClick={location.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION))}){Text("Allow optional Wi-Fi name access")}
                    Button(enabled=!busy,onClick={runOperation{status=withContext(Dispatchers.IO){repository.wifi()}}}){Text("Read Wi-Fi status")}
                    Button(enabled=!busy,onClick={runOperation{status=withContext(Dispatchers.IO){repository.battery()}}}){Text("Read battery health & charging")}
                    Text("Real internet speed test",style=MaterialTheme.typography.titleLarge)
                    Text("Cloudflare receives about 1 MiB generated random upload data; about 4 MiB is downloaded, plus HTTP overhead. No documents, microphone audio or memories are uploaded. This is a short observed HTTP transfer, not your ISP’s guaranteed capacity. Latency includes HTTP/TLS/server overhead; packet loss is not measured. Mobile data charges may apply.")
                    Button(enabled=!busy,onClick={AlertDialog.Builder(this@InfinityControlsActivity).setTitle("Start a real network transfer?").setMessage("Download 4 MiB and upload 1 MiB to speed.cloudflare.com, plus overhead. Use Wi-Fi if you want to avoid mobile data charges. You can cancel; leaving this screen cancels the test.").setNegativeButton("Cancel",null).setPositiveButton("Start test"){_,_->runOperation{
                        val result=InternetSpeedTest().run{update->lifecycleScope.launch(Dispatchers.Main){status=update}}
                        status=java.lang.String.format(java.util.Locale.ROOT,"Cloudflare HTTP test · %s\nDownload %.2f Mbps (%d bytes)\nUpload %.2f Mbps (%d bytes sent, HTTP success)\nMean HTTP response time %.1f ms\nSuccessive HTTP latency variation %.1f ms\nNo packet-loss measurement. Results describe this transfer only.",java.time.ZonedDateTime.now(),result.downMbps,result.downloaded,result.upMbps,result.uploaded,result.httpLatencyMs,result.httpJitterMs)
                    }}.show()}){Text("Start speed test · about 5 MiB")}
                    Text("Bluetooth",style=MaterialTheme.typography.titleLarge)
                    OutlinedButton(enabled=!busy,onClick={bluetooth.launch(repository.scanPermissions())}){Text("Allow optional Nearby devices access")}
                    Button(enabled=!busy,onClick={runOperation{devices=repository.paired();status="${devices.size} paired accessories reported. A2DP/headset/GATT connection results are queried afresh. A missing result does not prove disconnection for every possible profile."}}){Text("Refresh paired devices & connection state")}
                    Button(enabled=!busy,onClick={runOperation{nearby=emptyList();status="Discovering for up to 12 seconds…";try{repository.startScan{device->nearby=(nearby.filterNot{it.address==device.address}+device).take(100)};delay(12_000);status="Discovery complete: ${nearby.size} devices reported during this scan. RSSI is measured at discovery time."}finally{repository.close()}}}){Text("Discover nearby devices · 12 seconds")}
                    OutlinedTextField(alias,{alias=it.take(60)},label={Text("Optional Bluetooth nickname")},modifier=Modifier.fillMaxWidth())
                    devices.forEachIndexed{index,d->
                        Text("${index+1}. ${d.name} · ${d.type}\n${if(d.connected)"Connected profile reported by Android" else "No connection reported by queried profiles"}\nAccessory battery: unavailable until explicitly read")
                        OutlinedButton(enabled=!busy,onClick={AlertDialog.Builder(this@InfinityControlsActivity).setTitle("Read ${d.name} BLE battery?").setMessage("This temporarily opens a BLE GATT connection to a paired device and reads the standard Battery Service, then disconnects. Many accessories, earbuds and cases do not expose it. No hidden API is used.").setNegativeButton("Cancel",null).setPositiveButton("Read battery"){_,_->runOperation{val percent=repository.readBleBattery(d.address);status="${d.name}: ${percent?.let{"$it% reported by BLE Battery Service at ${java.time.LocalTime.now()}"} ?: "Battery Service unavailable, read failed or timed out. No percentage is inferred."}"}}.show()}){Text("Read BLE battery: ${d.name}")}
                        TextButton(enabled=!busy && alias.isNotBlank(),onClick={runCatching{preferences.alias(alias,d.address,devices);alias="";revision++}.onFailure{status=it.message.orEmpty()}}){Text("Save nickname for ${d.name}")}
                    }
                    nearby.forEach{d->Text("Nearby: ${d.name} · ${d.type} · RSSI ${d.rssi?.let{"$it dBm"} ?: "unavailable"} · ${if(d.paired)"paired" else "not paired"}. Discovery does not mean connected.")}
                    aliases.forEach{(name,address)->TextButton(onClick={preferences.removeAlias(name);revision++}){Text("Delete nickname: $name → ${devices.firstOrNull{it.address==address}?.name ?: "not in current list"}")}}
                    Text("Supported Android controls",style=MaterialTheme.typography.titleLarge)
                    Text("These open Android’s documented controls. You make protected changes there. Returning to Jarvis does not itself verify a change; refresh the relevant reading.")
                    SystemPanels.actions.keys.forEach{id->OutlinedButton(enabled=!busy,onClick={panel(id)}){Text("Open Android $id controls")}}
                    Text("Saved HTTPS websites · ${sites.size}/10",style=MaterialTheme.typography.titleLarge)
                    Text("Save explicitly approved sites. Say ‘open NAME’ or ‘read NAME’. Reading retrieves an attributed excerpt; site content cannot authorize tools. Private exports and documents are never sent to these sites.")
                    OutlinedTextField(siteName,{siteName=it.take(60)},label={Text("Website name")},modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(siteUrl,{siteUrl=it.take(2048)},label={Text("HTTPS URL")},modifier=Modifier.fillMaxWidth())
                    OutlinedTextField(description,{description=it.take(160)},label={Text("Description")},modifier=Modifier.fillMaxWidth())
                    Button(enabled=!busy,onClick={runCatching{preferences.saveWebsite(SavedWebsite(siteName,siteUrl,description));siteName="";siteUrl="";description="";revision++;status="Website saved. No network request was made."}.onFailure{status=it.message.orEmpty()}}){Text("Save website · replaces same name")}
                    sites.forEach{site->Text("${site.name}\n${site.url}\n${site.description}");Row{
                        TextButton(enabled=!busy,onClick={siteName=site.name;siteUrl=site.url;description=site.description}){Text("Edit")}
                        TextButton(enabled=!busy,onClick={runOperation{status=WebResearch().read(site.url,lang).text}}){Text("Read source")}
                        TextButton(enabled=!busy,onClick={preferences.removeWebsite(site.name);revision++}){Text("Delete")}
                    }}
                    Text("Music matching and speaker identification need separately licensed and tested recognition engines. This build does not claim song identification or voice identity, and voice never bypasses Android authentication.",style=MaterialTheme.typography.bodySmall)
                }
            }}
        }}
    }
    override fun onStop(){operation?.cancel();repository.close();super.onStop();if(getSystemService(KeyguardManager::class.java).isDeviceLocked){devices=emptyList();nearby=emptyList();status="";finish()}}
    override fun onDestroy(){repository.close();super.onDestroy()}
}
