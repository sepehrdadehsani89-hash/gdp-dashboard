package com.jarvis.assistant.omnisystem

import android.app.KeyguardManager
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.SystemClock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.*

data class GeoPoint(val latitude:Double,val longitude:Double,val accuracy:Float,val observedAt:Long,val elapsedNanos:Long)
data class LocationSnapshot(val point:GeoPoint?=null,val address:String?=null)
data class SavedDestination(val id:String,val name:String,val latitude:Double,val longitude:Double,val savedAt:Long)
object LocationMapPolicy {
    fun coordinates(latitude:Double,longitude:Double)=latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0
    fun embed(latitude:Double,longitude:Double,accuracy:Float=0f):String {
        require(coordinates(latitude,longitude) && abs(latitude)<=85 && accuracy.isFinite() && accuracy>=0)
        val span=(accuracy/111_320.0*2).coerceIn(.02,1.0)
        val x=span/cos(Math.toRadians(latitude)).coerceAtLeast(.1)
        val west=(longitude-x).coerceAtLeast(-180.0);val east=(longitude+x).coerceAtMost(180.0)
        val south=(latitude-span).coerceAtLeast(-85.0);val north=(latitude+span).coerceAtMost(85.0)
        return String.format(Locale.ROOT,"https://www.openstreetmap.org/export/embed.html?bbox=%.6f,%.6f,%.6f,%.6f&layer=mapnik&marker=%.6f,%.6f",west,south,east,north,latitude,longitude)
    }
    fun permittedResource(value:String):Boolean=runCatching{
        val uri=java.net.URI(value)
        uri.scheme=="https" && uri.userInfo==null && uri.port in listOf(-1,443) && uri.host in setOf("www.openstreetmap.org","tile.openstreetmap.org","a.tile.openstreetmap.org","b.tile.openstreetmap.org","c.tile.openstreetmap.org")
    }.getOrDefault(false)
}
/** One process-owned current fix, explicit address lookup and separately approved destinations. */
class LocationIntelligence(private val context:Context,private val vault:FeatureVault){
    private val mutex=Mutex()
    private val generation=AtomicLong()
    private val mutable=MutableStateFlow(LocationSnapshot())
    val state=mutable.asStateFlow()
    private val addressBusy=AtomicBoolean(false)
    private val addressExecutor=Executors.newSingleThreadExecutor{r->Thread(r,"Jarvis-address").apply{isDaemon=true}}
    private fun unlocked()=context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked==false
    fun clearTransient(){generation.incrementAndGet();mutable.value=LocationSnapshot()}
    suspend fun current():Location=mutex.withLock {
        check(unlocked()){"Unlock before requesting private location."}
        val epoch=generation.get();val fix=LocationFix(context).current()
        check(unlocked() && epoch==generation.get()){"Location request expired."}
        mutable.value=LocationSnapshot(GeoPoint(fix.latitude,fix.longitude,fix.accuracy,fix.time,fix.elapsedRealtimeNanos))
        Location(fix)
    }
    fun usablePoint():GeoPoint {
        check(unlocked())
        check(androidx.core.content.ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_COARSE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED){"Location permission was revoked."}
        val point=state.value.point ?: error("Request a current fix first.")
        require(LocationFixPolicy.usable(SystemClock.elapsedRealtimeNanos()-point.elapsedNanos,point.accuracy,point.latitude,point.longitude)){"Refresh the stale location first."}
        return point
    }
    @Suppress("DEPRECATION")
    suspend fun address(locale:Locale):String {
        val point=usablePoint();val epoch=generation.get()
        check(Geocoder.isPresent()){"Android address lookup is unavailable."}
        check(addressBusy.compareAndSet(false,true)){"Address provider is still busy."}
        val future=addressExecutor.submit<String?>{
            try {Geocoder(context,locale).getFromLocation(point.latitude,point.longitude,1)?.firstOrNull()?.let{a->
                (0..minOf(a.maxAddressLineIndex,3)).mapNotNull{a.getAddressLine(it)}.joinToString(", ").take(600).takeIf{it.isNotBlank()}
            }} finally {addressBusy.set(false)}
        }
        val result=withContext(Dispatchers.IO){try{future.get(8,TimeUnit.SECONDS)}catch(e:Exception){future.cancel(true);throw e}}
            ?: error("No address returned by Android.")
        check(unlocked() && epoch==generation.get() && state.value.point==point){"Address request expired."}
        mutable.value=state.value.copy(address=result)
        return result
    }
    @Synchronized fun destinations():List<SavedDestination>{
        check(unlocked())
        val text=vault.read("saved_destinations") ?: run{check(!vault.contains("saved_destinations")){"Encrypted destinations are unavailable."};return emptyList()}
        val array=JSONArray(text);require(array.length()<=20)
        return (0 until array.length()).map{i->val d=array.getJSONObject(i)
            val value=SavedDestination(d.getString("id"),d.getString("name"),d.getDouble("latitude"),d.getDouble("longitude"),d.getLong("saved_at"))
            require(value.name.length in 1..80 && LocationMapPolicy.coordinates(value.latitude,value.longitude));value
        }
    }
    @Synchronized fun save(name:String){
        val point=usablePoint();val clean=name.trim()
        require(clean.length in 1..80 && clean.none(Char::isISOControl))
        val existing=destinations();require(existing.size<20 && existing.none{it.name.equals(clean,true)}){"Choose a new name or remove a destination."}
        persist(existing+SavedDestination(UUID.randomUUID().toString(),clean,point.latitude,point.longitude,System.currentTimeMillis()))
    }
    @Synchronized fun remove(id:String){persist(destinations().filterNot{it.id==id})}
    @Synchronized fun removeAll(){check(unlocked());vault.remove("saved_destinations")}
    private fun persist(items:List<SavedDestination>){
        vault.write("saved_destinations",JSONArray().apply{items.forEach{d->put(JSONObject().put("id",d.id).put("name",d.name).put("latitude",d.latitude).put("longitude",d.longitude).put("saved_at",d.savedAt))}}.toString())
    }
}
