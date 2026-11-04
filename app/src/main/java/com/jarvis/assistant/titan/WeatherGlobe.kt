package com.jarvis.assistant.titan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.jarvis.assistant.hud.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.*

data class GlobePoint(val x:Double,val y:Double,val z:Double)
object GlobeProjection {
    fun project(latitude:Double,longitude:Double,centerLatitude:Double,centerLongitude:Double):GlobePoint {
        require(listOf(latitude,longitude,centerLatitude,centerLongitude).all(Double::isFinite) && latitude in -90.0..90.0 && centerLatitude in -90.0..90.0)
        val lat=Math.toRadians(latitude);val delta=Math.toRadians(longitude-centerLongitude);val center=Math.toRadians(centerLatitude)
        return GlobePoint(cos(lat)*sin(delta),cos(center)*sin(lat)-sin(center)*cos(lat)*cos(delta),sin(center)*sin(lat)+cos(center)*cos(lat)*cos(delta))
    }
}
/** Original native orthographic wireframe. Marker is a provider forecast grid, never claimed GPS/terrain/satellite imagery. */
@Composable fun WeatherGlobe(hud:HudState){
    val reading=hud.weather.reading
    val latitude=reading?.latitude;val longitude=reading?.longitude
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val fps=HudMotion.fps(hud.settings,hud.telemetry.battery,hud.telemetry.powerSaver,hud.telemetry.systemMotion)
    var tick by remember{mutableLongStateOf(0)}
    LaunchedEffect(fps,lifecycle){if(fps>0)lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){while(isActive){tick=android.os.SystemClock.elapsedRealtime();delay(1000L/fps)}}}
    val centerLatitude by animateFloatAsState((latitude ?: 0.0).toFloat(),tween(if(fps==0)0 else 1300),label="Forecast latitude")
    val centerLongitude by animateFloatAsState((longitude ?: 0.0).toFloat(),tween(if(fps==0)0 else 1300),label="Forecast longitude")
    val yaw=if(latitude!=null && longitude!=null)centerLongitude+if(fps>0)sin(tick/15_000.0)*12 else 0.0 else if(fps>0)(tick/150.0)%360-180 else 0.0
    Text(if(latitude!=null && longitude!=null)"Original forecast-grid globe · ${reading.place}\n${latitude}, ${longitude} · not device GPS or satellite imagery" else "Original wireframe globe · forecast coordinates unavailable; no location marker invented")
    Canvas(Modifier.fillMaxWidth().height(250.dp).semantics{contentDescription="Native wireframe weather globe; actual forecast-grid marker only"}){
        val r=minOf(size.width,size.height)*.43f;val c=center
        drawCircle(Brush.radialGradient(listOf(Color(0xFF052F46),Color(0xFF020810)),c,r),r,c)
        drawCircle(Color(0xFF00E5FF),r,c,style=Stroke(1.dp.toPx()))
        fun point(lat:Double,lon:Double)=GlobeProjection.project(lat,lon,centerLatitude.toDouble(),yaw)
        fun offset(p:GlobePoint)=Offset(c.x+(p.x*r).toFloat(),c.y-(p.y*r).toFloat())
        fun line(a:GlobePoint,b:GlobePoint){if(a.z>=0 && b.z>=0)drawLine(Color(0xFF00B8FF).copy(alpha=.24f+.4f*((a.z+b.z)/2).toFloat()),offset(a),offset(b),1.dp.toPx())}
        for(lat in -60..60 step 30){var prior=point(lat.toDouble(),-180.0);for(lon in -165..180 step 15){val next=point(lat.toDouble(),lon.toDouble());line(prior,next);prior=next}}
        for(lon in -180..180 step 30){var prior=point(-90.0,lon.toDouble());for(lat in -80..90 step 10){val next=point(lat.toDouble(),lon.toDouble());line(prior,next);prior=next}}
        if(latitude!=null && longitude!=null){val marker=point(latitude,longitude);if(marker.z>0){drawCircle(Color(0xFF22C55E).copy(alpha=.25f),10.dp.toPx(),offset(marker));drawCircle(Color(0xFF00E5FF),4.dp.toPx(),offset(marker))}}
    }
}
