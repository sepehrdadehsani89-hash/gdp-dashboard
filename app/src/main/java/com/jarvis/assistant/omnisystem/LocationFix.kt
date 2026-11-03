package com.jarvis.assistant.omnisystem

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object LocationFixPolicy {
    fun usable(ageNanos:Long,accuracyMeters:Float,latitude:Double,longitude:Double):Boolean =
        ageNanos in 0..120_000_000_000L && accuracyMeters.isFinite() && accuracyMeters in 0f..10_000f &&
            latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0
}
/** One explicit foreground request; no stored coordinates, background tracking, or network geocoding. */
class LocationFix(private val context:Context) {
    @android.annotation.SuppressLint("MissingPermission")
    suspend fun current():Location = withContext(Dispatchers.Main.immediate) {
        check(ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED){"Allow approximate location first."}
        val manager=context.getSystemService(LocationManager::class.java)
        check(manager.isLocationEnabled){"Location is disabled in Android settings."}
        val fine=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
        val providers=listOf(LocationManager.NETWORK_PROVIDER,LocationManager.GPS_PROVIDER).filter{manager.isProviderEnabled(it) && (it!=LocationManager.GPS_PROVIDER || fine)}
        check(providers.isNotEmpty()){"No permitted location provider is available. Approximate network location may require connectivity."}
        var active:LocationListener?=null
        try {
            withTimeoutOrNull(20_000) {
                suspendCancellableCoroutine<Location> { continuation ->
                    val listener=object:LocationListener {
                        override fun onLocationChanged(location:Location) {
                            if(continuation.isActive && location.hasAccuracy() && LocationFixPolicy.usable(SystemClock.elapsedRealtimeNanos()-location.elapsedRealtimeNanos,location.accuracy,location.latitude,location.longitude)) continuation.resume(Location(location))
                        }
                        override fun onProviderEnabled(provider:String) {}
                        override fun onProviderDisabled(provider:String) {}
                        @Deprecated("legacy") override fun onStatusChanged(provider:String?,status:Int,extras:Bundle?) {}
                    }
                    active=listener
                    continuation.invokeOnCancellation{manager.removeUpdates(listener)}
                    try { providers.forEach{manager.requestSingleUpdate(it,listener,Looper.getMainLooper())} }
                    catch(e:Exception){continuation.cancel(e)}
                }
            } ?: error("No fresh location fix within 20 seconds. Check permissions, location services and signal, then retry. No saved fix was substituted.")
        } finally { active?.let{manager.removeUpdates(it)} }
    }
}
