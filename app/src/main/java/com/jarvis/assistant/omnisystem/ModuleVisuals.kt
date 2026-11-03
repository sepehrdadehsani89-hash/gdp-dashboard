package com.jarvis.assistant.omnisystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.jarvis.assistant.hud.HudMotion
import com.jarvis.assistant.ui.JarvisUiState
import kotlinx.coroutines.delay

/** Decorative atmosphere is selected only from an actual received WMO condition code.
 * It is not a measurement, progress estimate, or a generated forecast. */
@Composable fun WeatherAtmosphere(state:JarvisUiState) {
    val code=state.hud.weather.reading?.code ?: return
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val fps=HudMotion.fps(state.hud.settings,state.hud.telemetry.battery,state.hud.telemetry.powerSaver,state.hud.telemetry.systemMotion).coerceAtMost(20)
    var phase by remember{mutableFloatStateOf(0f)}
    LaunchedEffect(lifecycle,fps,code){if(fps>0)lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){while(true){delay(1000L/fps);phase=(phase+1f/fps)%10f}}}
    val rain=code in 51..67 || code in 80..82 || code in 95..99
    val snow=code in 71..77 || code in 85..86
    val cloud=code in 1..3 || code in 45..48
    val color=MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(75.dp)) {
        drawLine(color.copy(alpha=.25f),Offset(0f,size.height-1),Offset(size.width,size.height-1),1f)
        when {
            rain || snow -> repeat(16){i->val x=size.width*(i+.5f)/16;val y=((i*.173f+phase*.28f)%1f)*size.height
                if(snow)drawCircle(Color.White.copy(alpha=.6f),2f,Offset(x,y)) else drawLine(color.copy(alpha=.6f),Offset(x,y),Offset(x-3,y+9),2f)
            }
            cloud -> repeat(5){i->drawOval(color.copy(alpha=.12f),Offset(size.width*(i*.19f+phase*.006f)%size.width,15f+i%2*12f),androidx.compose.ui.geometry.Size(size.width*.3f,30f))}
            code==0 -> {val centre=Offset(size.width/2,size.height/2);drawCircle(Color(0xFFFFBF55).copy(alpha=.7f),18f,centre,style=Stroke(2f));drawCircle(color.copy(alpha=.35f),28f+phase%2,centre,style=Stroke(1f))}
            else -> drawCircle(color.copy(alpha=.35f),20f,Offset(size.width/2,size.height/2),style=Stroke(1f))
        }
    }
    Text("WMO condition $code · ${if(state.hud.weather.stale)"cached/stale" else "received weather"}",style=MaterialTheme.typography.labelSmall)
}

/** Values and time coordinates are measured samples; no interpolation beyond connecting samples. */
@Composable fun DownloadChart(samples:List<com.jarvis.assistant.infinity.InternetSpeedTest.Sample>) {
    if(samples.isEmpty())return
    Text("Download · observed cumulative Mbps (includes HTTP setup time)",style=MaterialTheme.typography.labelSmall)
    val color=MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(100.dp)) {
        drawLine(color.copy(alpha=.3f),Offset(0f,size.height),Offset(size.width,size.height),1f)
        val peak=samples.maxOf{it.mbps}.coerceAtLeast(.001)
        val duration=samples.last().elapsedMillis.coerceAtLeast(1)
        val path=Path()
        samples.forEachIndexed{i,s->val p=Offset(size.width*s.elapsedMillis/duration.toFloat(),(size.height*(1-s.mbps/peak)).toFloat());if(i==0)path.moveTo(p.x,p.y) else path.lineTo(p.x,p.y)}
        drawPath(path,color,style=Stroke(2f))
    }
    val last=samples.last()
    Text(java.lang.String.format(java.util.Locale.ROOT,"%.2f Mbps · %d bytes · %.2f s. Partial samples do not prove a completed test.",last.mbps,last.bytes,last.elapsedMillis/1000.0),style=MaterialTheme.typography.bodySmall)
}

@Composable fun ClockDial(epochMillis:Long,zone:String) {
    val time=java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.of(zone))
    val color=MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val centre=Offset(size.width/2,size.height/2);val radius=size.height*.43f
        drawCircle(color.copy(alpha=.3f),radius,centre,style=Stroke(1f))
        fun point(turn:Double,length:Float):Offset {val angle=turn*2*Math.PI-Math.PI/2;return centre+Offset((kotlin.math.cos(angle)*length).toFloat(),(kotlin.math.sin(angle)*length).toFloat())}
        repeat(60){i->drawLine(color.copy(alpha=if(i%5==0).8f else .25f),point(i/60.0,radius),point(i/60.0,radius-if(i%5==0)8f else 3f),1f)}
        drawLine(color,centre,point(time.minute/60.0,radius*.8f),3f)
        drawLine(Color.White,centre,point((time.hour%12+time.minute/60.0)/12,radius*.5f),4f)
    }
}

@Composable fun AccessorySignals(devices:List<com.jarvis.assistant.infinity.Accessory>) {
    if(devices.isEmpty())return
    Text("Discovery RSSI · not distance or direction",style=MaterialTheme.typography.labelSmall)
    devices.take(12).forEach{device->
        val signal=device.rssi?.takeIf{it in -126..-1}
        Text("${device.name} · ${signal?.let{"$it dBm"} ?: "Unavailable"}")
        if(signal!=null){val color=MaterialTheme.colorScheme.primary;Canvas(Modifier.fillMaxWidth().height(6.dp)){drawLine(color.copy(alpha=.15f),Offset.Zero,Offset(size.width,0f),6f);drawLine(color,Offset.Zero,Offset(size.width*(signal+126)/125f,0f),6f)}}
    }
}
