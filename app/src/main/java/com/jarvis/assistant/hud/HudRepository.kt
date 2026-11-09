package com.jarvis.assistant.hud

import android.Manifest
import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.*
import android.os.*
import android.provider.Settings
import android.text.format.DateFormat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.ZoneId
import kotlin.coroutines.resume
import kotlin.math.round

/** Receivers, network observation and refresh jobs exist only while the activity is visible. */
class HudRepository(private val context:Context) {
    private val prefs=HudPreferences(context)
    private val weather=WeatherRepository(context)
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val mutable=MutableStateFlow(HudState(settings=prefs.read()))
    val state=mutable.asStateFlow()
    val powerSource=com.jarvis.assistant.power.BatteryStateRepository(context)
    init{scope.launch{powerSource.state.collect{reading->mutable.update{it.copy(telemetry=it.telemetry.copy(battery=reading.battery),power=reading)}}}}
    private var visible=false
    private val visibleOwners=mutableSetOf<Any>()
    private val weatherOwners=mutableSetOf<Any>()
    private var job:Job?=null
    private var weatherJob:Job?=null
    private var generation=0L
    private val cm=context.getSystemService(ConnectivityManager::class.java)
    private val handler=Handler(Looper.getMainLooper())
    private val receiver=object:BroadcastReceiver(){ override fun onReceive(c:Context?,i:Intent?){
        if(!visible)return
        refresh()
    } }
    private val observer=object:ContentObserver(handler){ override fun onChange(selfChange:Boolean){ refresh() } }
    private val callback=object:ConnectivityManager.NetworkCallback(){
        override fun onCapabilitiesChanged(n:Network,c:NetworkCapabilities){ refresh() }
        override fun onLost(n:Network){ refresh() }
    }
    fun startVisible(owner:Any=this,includeWeather:Boolean=true){
        val addedWeather=includeWeather && weatherOwners.add(owner)
        if(!visibleOwners.add(owner))return
        if(visible){
            if(addedWeather && state.value.weather.stale && !state.value.weather.loading && state.value.settings.weatherCity.isNotBlank())refreshWeather()
            return
        }
        visible=true
        val sticky=ContextCompat.registerReceiver(context,receiver,IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK); addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED);addAction(Intent.ACTION_DATE_CHANGED);addAction(Intent.ACTION_LOCALE_CHANGED);addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        },ContextCompat.RECEIVER_EXPORTED)
        // All filter actions are Android protected broadcasts. Some Android/OEM senders
        // use a privileged process outside this UID; exporting this system-only receiver
        // allows those deliveries. Read the sticky result synchronously on re-entry too.
        powerSource.start();powerSource.refresh()
        context.contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.TIME_12_24),false,observer)
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),false,observer)
        runCatching { cm.registerDefaultNetworkCallback(callback,handler) }
        refresh(); memory()
        val s=state.value.settings
        val cached=weather.cached(s.weatherCity)
        mutable.update { it.copy(weather=it.weather.copy(reading=cached,stale=WeatherRepository.stale(cached,System.currentTimeMillis()))) }
        if(weatherOwners.isNotEmpty() && s.weatherCity.isNotBlank() && state.value.weather.stale) refreshWeather()
        job=scope.launch { var ticks=0; while(isActive){ delay(30_000); refresh(); memory(); ticks++; if(ticks%30==0 && weatherOwners.isNotEmpty() && state.value.settings.weatherCity.isNotBlank()) refreshWeather() } }
    }
    fun stopVisible(owner:Any=this){
        visibleOwners.remove(owner)
        weatherOwners.remove(owner)
        if(weatherOwners.isEmpty())cancelPassiveWeather()
        if(visibleOwners.isNotEmpty() || !visible)return
        visible=false; job?.cancel(); weatherJob?.cancel(); generation++
        runCatching{ context.unregisterReceiver(receiver) }; context.contentResolver.unregisterContentObserver(observer)
        runCatching{cm.unregisterNetworkCallback(callback)}
        mutable.update { it.copy(weather=it.weather.copy(loading=false)) }
    }
    fun weatherInterest(owner:Any,enabled:Boolean){
        if(owner !in visibleOwners)return
        val changed=if(enabled)weatherOwners.add(owner) else weatherOwners.remove(owner)
        if(changed && enabled && state.value.weather.stale && !state.value.weather.loading && state.value.settings.weatherCity.isNotBlank())refreshWeather()
        if(changed && !enabled && weatherOwners.isEmpty())cancelPassiveWeather()
    }
    private fun cancelPassiveWeather(){
        if(weatherJob?.isActive==true){weatherJob?.cancel();generation++;mutable.update{it.copy(weather=it.weather.copy(loading=false))}}
    }
    fun acceptBattery(intent:Intent){powerSource.observe(intent)}
    fun clear(){
        generation++;weatherJob?.cancel();weather.clear();prefs.save(HudSettings())
        mutable.update { HudState(settings=HudSettings(),telemetry=it.telemetry) }
    }
    fun save(s:HudSettings){
        val old=state.value.settings
        val value=prefs.save(s); mutable.update { it.copy(settings=value) }
        if(old.weatherCity!=value.weatherCity){ generation++; weatherJob?.cancel(); mutable.update { it.copy(weather=WeatherState()) }; if(value.weatherCity.isNotBlank())refreshWeather() }
    }
    private fun refresh(){
        val caps=cm.getNetworkCapabilities(cm.activeNetwork)
        val network=when { caps==null->"Offline"; caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)->"VPN"; caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->"Wi-Fi";caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)->"Mobile";caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)->"Ethernet";else->"Network" }
        mutable.update { it.copy(telemetry=it.telemetry.copy(storageAvailableBytes=runCatching{StatFs(context.filesDir.path).availableBytes}.getOrNull(),thermalStatus=runCatching{context.getSystemService(PowerManager::class.java).currentThermalStatus}.getOrNull(),epochMillis=System.currentTimeMillis(),timezone=ZoneId.systemDefault().id,system24Hour=DateFormat.is24HourFormat(context),systemMotion=android.animation.ValueAnimator.areAnimatorsEnabled(),network=network,internetValidated=caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true,powerSaver=context.getSystemService(PowerManager::class.java).isPowerSaveMode),weather=it.weather.copy(stale=WeatherRepository.stale(it.weather.reading,System.currentTimeMillis()))) }
    }
    private fun memory(){
        val info=ActivityManager.MemoryInfo(); context.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        mutable.update { it.copy(telemetry=it.telemetry.copy(systemAvailableBytes=info.availMem,systemTotalBytes=info.totalMem,appPssKiB=Debug.getPss().coerceAtMost(Int.MAX_VALUE.toLong()).toInt())) }
    }
    fun refreshWeather(approximate:Boolean=false){
        weatherJob?.cancel(); val token=++generation
        if(!approximate && state.value.settings.weatherCity.isBlank())return
        mutable.update { it.copy(weather=it.weather.copy(loading=true,error=null)) }
        weatherJob=scope.launch {
            try {
                val location=if(approximate) approximateLocation() else null
                val result=weather.fetch(state.value.settings.weatherCity,location?.first,location?.second)
                if(token==generation)mutable.update { it.copy(weather=WeatherState(reading=result,stale=WeatherRepository.stale(result,System.currentTimeMillis()),locationNotice=if(approximate)"Approximate coordinates sent to Open-Meteo for this request; not saved." else null)) }
            }catch(e:CancellationException){throw e}catch(e:Exception){ if(token==generation)mutable.update { it.copy(weather=it.weather.copy(loading=false,error=e.message ?: "Weather unavailable",stale=true)) } }
        }
    }
    fun acceptWeatherReading(reading:WeatherReading){mutable.update{it.copy(weather=WeatherState(reading=reading,stale=WeatherRepository.stale(reading,System.currentTimeMillis())))}}
    suspend fun weatherForQuestion(city:String?=null):WeatherReading {
        weatherJob?.join()
        val current=state.value.weather.reading
        val chosen=city ?: state.value.settings.weatherCity
        if(city==null && !WeatherRepository.stale(current,System.currentTimeMillis()))return current!!
        check(chosen.isNotBlank()){ "Set a weather city in Settings or ask for weather in a named city. Approximate location is optional and must be requested with its button." }
        val token=++generation
        mutable.update{it.copy(weather=it.weather.copy(loading=true,error=null))}
        try{
            val result=weather.fetch(chosen)
            if(token==generation)mutable.update{it.copy(weather=WeatherState(reading=result,stale=WeatherRepository.stale(result,System.currentTimeMillis())))}
            return result
        }catch(e:CancellationException){if(token==generation)mutable.update{it.copy(weather=it.weather.copy(loading=false))};throw e}
        catch(e:Exception){if(token==generation)mutable.update{it.copy(weather=it.weather.copy(loading=false,error=e.message,stale=true))};throw e}
    }
    private suspend fun approximateLocation():Pair<Double,Double> {
        check(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED) { "Approximate location permission denied. Enter a city instead." }
        val manager=context.getSystemService(LocationManager::class.java)
        val location=withTimeoutOrNull(15_000){ suspendCancellableCoroutine<Location>{ continuation->
            val listener=object:LocationListener { override fun onLocationChanged(location:Location){manager.removeUpdates(this);if(continuation.isActive)continuation.resume(location)}; override fun onProviderEnabled(p:String){};override fun onProviderDisabled(p:String){};@Deprecated("legacy") override fun onStatusChanged(p:String?,s:Int,b:Bundle?){} }
            continuation.invokeOnCancellation{ manager.removeUpdates(listener) }
            try { manager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER,listener,Looper.getMainLooper()) }catch(e:Exception){continuation.cancel(e)}
        } } ?: error("Location unavailable. Turn on location or enter a city.")
        return round(location.latitude*100)/100 to round(location.longitude*100)/100
    }
    companion object {
        fun battery(i:Intent):BatteryReading {
            val status=i.getIntExtra(BatteryManager.EXTRA_STATUS,-1); val plug=i.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)
            return BatteryReading(BatteryReading.percentage(i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),i.getIntExtra(BatteryManager.EXTRA_SCALE,-1)),status==BatteryManager.BATTERY_STATUS_CHARGING,status==BatteryManager.BATTERY_STATUS_FULL,
                listOfNotNull("AC".takeIf{plug and BatteryManager.BATTERY_PLUGGED_AC!=0},"USB".takeIf{plug and BatteryManager.BATTERY_PLUGGED_USB!=0},"Wireless".takeIf{plug and BatteryManager.BATTERY_PLUGGED_WIRELESS!=0}).joinToString(" + ").ifBlank { if(plug==0)"Battery" else "External power" },
                if(i.hasExtra(BatteryManager.EXTRA_TEMPERATURE))i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10f else null,
                when(i.getIntExtra(BatteryManager.EXTRA_HEALTH,0)){BatteryManager.BATTERY_HEALTH_GOOD->"Good";BatteryManager.BATTERY_HEALTH_OVERHEAT->"Overheated";BatteryManager.BATTERY_HEALTH_COLD->"Cold";BatteryManager.BATTERY_HEALTH_DEAD->"Degraded";BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE->"Overvoltage";else->"Unavailable"})
        }
    }
}
