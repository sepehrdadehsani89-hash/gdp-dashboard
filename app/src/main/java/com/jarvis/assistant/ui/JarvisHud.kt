package com.jarvis.assistant.ui

import android.animation.ValueAnimator
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jarvis.assistant.core.*
import com.jarvis.assistant.hud.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.*

private val Cyan=Color(0xFF00E5FF)
private val Purple=Color(0xFFAA7CFF)
private val Ice=Color(0xFFDDF8FF)
private val Dim=Color(0xFF8CAFB8)
private val Back=Color(0xFF050C14)

@Composable
internal fun HudPanel(title:String, modifier:Modifier=Modifier, content:@Composable ColumnScope.()->Unit) {
    Column(modifier.background(Brush.verticalGradient(listOf(Back,Color.Black)),CutCornerShape(10.dp)).border(.7.dp,Cyan.copy(alpha=.48f),CutCornerShape(10.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Text(title.uppercase(),color=Ice,fontSize=10.sp,letterSpacing=2.sp)
        HorizontalDivider(color=Cyan.copy(alpha=.25f))
        content()
    }
}
@Composable private fun Reading(label:String,value:String) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(label,color=Dim,fontSize=11.sp,modifier=Modifier.weight(1f))
        Text(value,color=Ice,fontSize=11.sp,textAlign=TextAlign.End,modifier=Modifier.weight(1f))
    }
}
@Composable internal fun HudDashboard(state:JarvisUiState,actions:JarvisUiActions,onSettings:()->Unit) {
    val h=state.hud; val t=h.telemetry; val s=h.settings; val l=state.settings.language
    val locale=remember(l){Locale.forLanguageTag(l.tag)}
    val color=if(s.accent=="purple")Purple else Cyan
    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)) {
                Text("J A R V I S",color=color,fontSize=24.sp,letterSpacing=5.sp,fontWeight=FontWeight.Light)
                Text("PERSONAL INTELLIGENCE SYSTEM",color=Dim,fontSize=8.sp,letterSpacing=2.sp)
            }
            IconButton(onClick=onSettings){Icon(Icons.Rounded.Settings,"HUD settings",tint=color)}
            IconButton(onClick=actions.onDismiss){Icon(Icons.Rounded.Close,"Dismiss Jarvis",tint=Dim)}
        }
        Text(ClockDisplay.time(t.epochMillis,t.timezone,locale,s.clockFormat,t.system24Hour),color=Ice,fontSize=38.sp,fontWeight=FontWeight.Light)
        Text(ClockDisplay.date(t.epochMillis,t.timezone,locale),color=Dim,fontSize=12.sp,textAlign=TextAlign.Center)
        Text(t.timezone,color=Dim,fontSize=10.sp)
        val hour=java.time.Instant.ofEpochMilli(t.epochMillis).atZone(java.time.ZoneId.of(t.timezone)).hour
        Text(when(hour){in 5..11->l.choose("Good morning","Buenos días","صبح بخیر");in 12..17->l.choose("Good afternoon","Buenas tardes","عصر بخیر");else->l.choose("Good evening","Buenas noches","شب بخیر")}+state.settings.name.takeIf(String::isNotBlank)?.let {", $it"}.orEmpty(),color=Dim,fontSize=11.sp)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide=maxWidth>=650.dp
            if(wide) Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                SystemPanel(state,Modifier.weight(1f)); HudCore(state,Modifier.weight(2f)); TelemetryPanel(state,Modifier.weight(1f))
            } else Column {
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){SystemPanel(state,Modifier.weight(1f));TelemetryPanel(state,Modifier.weight(1f))}
                HudCore(state,Modifier.fillMaxWidth())
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            WeatherPanel(state,actions,Modifier.weight(1f))
            HudPanel(l.choose("Quick info","Información","اطلاعات"),Modifier.weight(1f)) {
                Reading("MEMORIES",state.memories.size.toString())
                Reading("FILES",state.attachedFiles.size.toString())
                Reading("PROVIDER",state.settings.provider.uppercase())
                Text(if(state.settings.provider=="local")state.brainStatus else if(state.hasApiKey)"Key saved · use Test connection to verify" else "API key missing",color=Dim,fontSize=10.sp)
                Text(state.voiceStatus,color=Dim,fontSize=10.sp)
            }
        }
        if(state.authorization!=null) AuthorizationPanel(state,actions)
        HudPanel(l.choose("Live transcript","Transcripción","رونویسی زنده"),Modifier.fillMaxWidth()) {
            Text(state.transcript.ifBlank { l.choose("Tap the microphone or use your enabled wake phrase.","Toca el micrófono o usa la frase de activación.","میکروفن را لمس کنید یا عبارت بیدارباش فعال را بگویید.") },color=Ice,fontSize=15.sp)
        }
        state.messages.lastOrNull { it.role==MessageRole.ASSISTANT }?.let { message ->
            HudPanel("JARVIS",Modifier.fillMaxWidth()){Text(l.choose("Response: ","Respuesta: ","پاسخ: ")+message.text,color=Ice,fontSize=15.sp)}
        }
    }
}
@Composable private fun SystemPanel(state:JarvisUiState,modifier:Modifier) {
    val t=state.hud.telemetry
    HudPanel("System link",modifier){
        Reading("NETWORK",t.network)
        Reading("INTERNET",if(t.internetValidated)"Validated" else "Not verified")
        Reading("AUDIO IN",if(state.audio.recording)"Recording" else "Inactive")
        Reading("WAKE WORD",state.settings.language.wakePhrase)
        Text(state.wakeStatus.ifBlank {"Wake listening off"},color=Dim,fontSize=10.sp)
    }
}
@Composable private fun TelemetryPanel(state:JarvisUiState,modifier:Modifier) {
    val t=state.hud.telemetry;val b=t.battery
    fun mib(bytes:Long?)=bytes?.let { "${it/(1024*1024)} MB" } ?: "Unavailable"
    HudPanel("Telemetry",modifier) {
        Reading("SYSTEM FREE",mib(t.systemAvailableBytes))
        Reading("SYSTEM TOTAL",mib(t.systemTotalBytes))
        Reading("APP PSS",t.appPssKiB?.let {"${it/1024} MB"} ?: "Unavailable")
        Reading("BATTERY °C",b.temperatureC?.let {String.format(Locale.ROOT,"%.1f",it)} ?: "Unavailable")
        Reading("HEALTH",b.health)
    }
}
@Composable internal fun HudCore(state:JarvisUiState,modifier:Modifier=Modifier) {
    val t=state.hud.telemetry; val s=state.hud.settings; val b=t.battery
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var active by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle){val observer=LifecycleEventObserver{_,_->active=lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)};lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer)}}
    val fps=HudMotion.fps(s,b,t.powerSaver,ValueAnimator.areAnimatorsEnabled())
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(active,fps,s.animationIntensity,state.phase,state.authorization?.status) {
        if(active && fps>0) { var last=SystemClock.elapsedRealtime();while(true){delay(1000L/fps);val now=SystemClock.elapsedRealtime();val speed=if(state.authorization?.status in listOf(AuthorizationStatus.AWAITING_CODEX,AuthorizationStatus.IDENTITY_CONFIRMATION))1f else when(state.phase){AssistantPhase.READY->2.5f;AssistantPhase.THINKING->12f;AssistantPhase.SPEAKING->6f;AssistantPhase.ERROR->1f;else->9f};angle=(angle+(now-last).coerceAtMost(100)*.001f*speed*s.animationIntensity)%360;last=now} }
    }
    val auth=state.authorization?.status
    val authorizing=auth in listOf(AuthorizationStatus.AUTHORIZATION_REQUIRED,AuthorizationStatus.AWAITING_CODEX,AuthorizationStatus.IDENTITY_CONFIRMATION)
    val color=when { authorizing->Purple; auth==AuthorizationStatus.DENIED->Color(0xFFFF5368); (b.percent ?: 100)<10->Color(0xFFFF5368);(b.percent ?: 100)<20->Color(0xFFFFBF55);s.accent=="purple"->Purple;else->Cyan }
    val ticks=remember { List(120){ val rad=it*PI*2/120;Offset(cos(rad).toFloat(),sin(rad).toFloat()) } }
    Box(modifier.aspectRatio(1f).sizeIn(maxWidth=450.dp,maxHeight=450.dp),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize().semantics { contentDescription="Battery reactor ${b.percent?.toString() ?: "unavailable"} percent" }) {
            val r=size.minDimension*.455f; val center=Offset(size.width/2,size.height/2)
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha=.15f),color.copy(alpha=.02f),Color.Transparent),center,r),r,center)
            ticks.forEachIndexed { i,v -> val outer=center+v*r;val inner=center+v*(r-if(i%5==0)8.dp.toPx() else 3.dp.toPx());drawLine(color.copy(alpha=if(i%5==0).8f else .4f),inner,outer,if(i%5==0)1.2.dp.toPx() else .6.dp.toPx()) }
            val energy=if(state.audio.recording)state.audio.level else 0f
            drawCircle(color.copy(alpha=.09f+energy*.22f),r*.52f,center,style=Stroke((5+energy*4).dp.toPx()))
            for(i in 0..11){val rad=(i*30f+angle*.35f)*PI/180;val rr=r*(.58f+(i%3)*.08f);drawCircle(color.copy(alpha=.35f),.8.dp.toPx(),center+Offset(cos(rad).toFloat()*rr,sin(rad).toFloat()*rr))}
            for(ring in 0..4){val rr=r*(.44f+ring*.12f);drawCircle(color.copy(alpha=.25f),rr,center,style=Stroke(.6.dp.toPx()))}
            for(ring in 0..2){val rr=r*(.95f-ring*.15f);val spin=angle*(if(ring%2==0)1 else -1)
                rotate(spin,center){for(i in 0..2){ drawArc(if(i==1)Purple.copy(alpha=.85f) else color.copy(alpha=.8f),i*120f+12,if(ring==0)40f else 64f,false,Offset(center.x-rr,center.y-rr),Size(rr*2,rr*2),style=Stroke(if(ring==0)5.dp.toPx() else 2.dp.toPx()))}}
            }
            val core=r*.49f
            drawCircle(Brush.radialGradient(listOf(Back,color.copy(alpha=.18f)),center,core),core,center)
            drawCircle(color,core,center,style=Stroke(1.4.dp.toPx()))
            val pointer=Path().apply{moveTo(center.x,center.y-r*.73f);lineTo(center.x-4.dp.toPx(),center.y-r*.80f);lineTo(center.x+4.dp.toPx(),center.y-r*.80f);close()}
            drawPath(pointer,color)
            // Mesh is decorative geometry. It never represents a sensor reading.
            for(i in -3..3){val y=center.y+i*core/4;val dx=sqrt(max(0f,core*core-(y-center.y).pow(2)));drawLine(color.copy(alpha=.10f),Offset(center.x-dx,y),Offset(center.x+dx,y),.6f)}
            if(state.audio.recording){val values=state.audio.history; val step=core*1.45f/32
                values.forEachIndexed { i,value -> val x=center.x-core*.725f+i*step;val height=value*core*.28f;drawLine(Purple,Offset(x,center.y+core*.55f-height),Offset(x,center.y+core*.55f+height),1.4.dp.toPx()) }
            }
            for(i in 0..3){val rad=i*PI/2;val v=Offset(cos(rad).toFloat(),sin(rad).toFloat());drawLine(color,center+v*(r*.97f),center+v*(r*1.06f),1.3.dp.toPx())}
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth(.48f)) {
            if(authorizing) Icon(Icons.Rounded.Lock,"Authorization required",Modifier.size(16.dp),tint=Purple)
            Text(if(authorizing)"CODEX" else when(state.phase){AssistantPhase.LISTENING->"LISTENING";AssistantPhase.SPEAKING->"SPEAKING";AssistantPhase.THINKING->"THINKING";AssistantPhase.ERROR->"ATTENTION";else->"READY"},color=Ice,fontSize=10.sp,letterSpacing=2.sp)
            Text(b.percent?.let {"$it%"} ?: "—",color=Ice,fontSize=44.sp,fontWeight=FontWeight.Light)
            Text(if(b.full)"FULL" else if(b.charging)"CHARGING · ${b.source}" else "BATTERY",color=color,fontSize=9.sp,textAlign=TextAlign.Center)
        }
        Text(if(state.audio.recording)"LIVE MIC · ${state.audio.source}" else if(state.phase==AssistantPhase.SPEAKING)"SPEAKING · DECORATIVE MOTION" else "${s.animationProfile.uppercase()} · ${if(fps==0)"STILL" else "$fps FPS CAP"}",color=Dim,fontSize=8.sp,modifier=Modifier.align(Alignment.BottomCenter))
    }
}
@Composable private fun WeatherPanel(state:JarvisUiState,actions:JarvisUiActions,modifier:Modifier) {
    val w=state.hud.weather;val r=w.reading;val f=state.hud.settings.fahrenheit
    fun temp(v:Double?)=v?.let {"${if(f)(it*9/5+32).roundToInt() else it.roundToInt()}°${if(f)"F" else "C"}"} ?: "Unavailable"
    HudPanel("Local conditions",modifier) {
        if(r==null)Text(if(w.loading)"Fetching weather…" else "Set a city in Settings or choose approximate location.",color=Dim,fontSize=11.sp)
        else {
            Text(r.place,color=Ice,fontSize=12.sp)
            Text(when(r.code){0->"Clear";1,2->"Partly cloudy";3->"Overcast";45,48->"Fog";51,53,55,56,57->"Drizzle";61,63,65,66,67,80,81,82->"Rain";71,73,75,77,85,86->"Snow";95,96,99->"Thunderstorm";else->"Conditions unavailable"},color=Ice,fontSize=11.sp)
            Reading("TEMP / FEELS", "${temp(r.temperatureC)} / ${temp(r.feelsLikeC)}")
            Reading("HUMIDITY",r.humidity?.let {"$it%"} ?: "Unavailable")
            Reading("WIND",r.windKmh?.let {"$it km/h"} ?: "Unavailable")
            Reading("RAIN CHANCE",r.precipitationProbability?.let {"$it%"} ?: "Unavailable")
            Reading("SUNRISE",r.sunrise?.substringAfter('T') ?: "Unavailable")
            Reading("SUNSET",r.sunset?.substringAfter('T') ?: "Unavailable")
            Text("${if(w.stale)"STALE · " else ""}${r.observationTime.replace('T',' ')} · place local time",color=Dim,fontSize=9.sp)
        }
        w.error?.let {Text(it,color=Color(0xFFFFBF55),fontSize=10.sp)}
        if(w.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(state.hud.settings.weatherCity.isNotBlank())TextButton(onClick=actions.onWeatherRefresh){Text("Refresh",fontSize=11.sp)}
        Text("Open-Meteo · weather data",color=Dim,fontSize=9.sp)
    }
}
@Composable internal fun AuthorizationPanel(state:JarvisUiState,actions:JarvisUiActions) {
    state.authorization?.let { request ->
        HudPanel("CODEX · ${request.status.name.replace('_',' ')}",Modifier.fillMaxWidth()) {
            Text(request.action,color=Ice)
            Text("Codex opens Android identity confirmation. Only your device credential or enrolled biometric can authorize this action.",color=Dim,fontSize=12.sp)
            if(request.status in listOf(AuthorizationStatus.AUTHORIZATION_REQUIRED,AuthorizationStatus.AWAITING_CODEX))Button(onClick=actions.onCodex){Text("Codex · verify identity")}
            if(request.status !in listOf(AuthorizationStatus.DENIED,AuthorizationStatus.EXPIRED,AuthorizationStatus.AUTHORIZED))TextButton(onClick=actions.onCancelAuthorization){Text("Cancel authorization")}
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun HudSettingsCard(state:JarvisUiState,actions:JarvisUiActions) {
    val s=state.hud.settings
    var city by rememberSaveable(s.weatherCity){mutableStateOf(s.weatherCity)}
    var pitch by remember(s.voicePitch){mutableFloatStateOf(s.voicePitch)}
    var rate by remember(s.voiceRate){mutableFloatStateOf(s.voiceRate)}
    var volume by remember(s.voiceVolume){mutableFloatStateOf(s.voiceVolume)}
    var intensity by remember(s.animationIntensity){mutableFloatStateOf(s.animationIntensity)}
    HudPanel("HUD · voice · privacy",Modifier.fillMaxWidth()) {
        Text("Jarvis 2.0 · separate from your original Jarvis",color=Ice)
        Text("Clock format",color=Dim)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){ listOf("system","12","24").forEach { option->FilterChip(s.clockFormat==option,{actions.onSaveHud(s.copy(clockFormat=option))},label={Text(option)}) } }
        OutlinedTextField(city,{city=it},label={Text("Weather city")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Row {TextButton(onClick={actions.onSaveHud(s.copy(weatherCity=city))}){Text("Save city")};TextButton(onClick=actions.onWeatherRefresh){Text("Retry")}}
        TextButton(onClick=actions.onApproximateWeather){Text("Use approximate location once")}
        Text("City or approximate coordinates go to Open-Meteo. Location is optional and is not stored.",color=Dim,fontSize=11.sp)
        state.hud.weather.locationNotice?.let {Text(it,color=Dim,fontSize=11.sp)}
        HudToggle("Fahrenheit",s.fahrenheit){actions.onSaveHud(s.copy(fahrenheit=it))}
        Text("Animation profile",color=Dim)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("cinematic","balanced","saver").forEach {p->FilterChip(s.animationProfile==p,{actions.onSaveHud(s.copy(animationProfile=p))},label={Text(p)})}}
        HudToggle("Reduced motion",s.reducedMotion){actions.onSaveHud(s.copy(reducedMotion=it))}
        HudToggle("Automatic low-battery saver",s.autoBatterySaver){actions.onSaveHud(s.copy(autoBatterySaver=it))}
        Text("Motion intensity ${(intensity*100).roundToInt()}%",color=Dim)
        Slider(intensity,{intensity=it},onValueChangeFinished={actions.onSaveHud(s.copy(animationIntensity=intensity))})
        Row {FilterChip(s.accent=="cyan",{actions.onSaveHud(s.copy(accent="cyan"))},label={Text("Cyan")});Spacer(Modifier.width(8.dp));FilterChip(s.accent=="purple",{actions.onSaveHud(s.copy(accent="purple"))},label={Text("Purple")})}
        HudToggle("Wake feedback sound",s.soundEffects){actions.onSaveHud(s.copy(soundEffects=it))}
        Text("Auto-dismiss when idle",color=Dim)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(0,30,60,120).forEach {n->FilterChip(s.autoDismissSeconds==n,{actions.onSaveHud(s.copy(autoDismissSeconds=n))},label={Text(if(n==0)"Off" else "${n}s")})}}
        HudToggle("Codex protection",s.codexEnabled){actions.onSaveHud(s.copy(codexEnabled=it))}
        Text("Requires an Android screen lock. Changing Codex protection uses device authentication. Spoken Codex alone grants nothing.",color=Dim,fontSize=11.sp)
        Text("Voice engine",color=Dim)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("auto","system","neural").forEach {p->FilterChip(s.voiceEngine==p,{actions.onSaveHud(s.copy(voiceEngine=p))},label={Text(p)})}}
        Text("System voice pitch ${"%.2f".format(pitch)} · default 0.90",color=Dim)
        Slider(pitch,{pitch=it},valueRange=.7f..1.3f,onValueChangeFinished={actions.onSaveHud(s.copy(voicePitch=pitch))})
        Text("Voice rate ${"%.2f".format(rate)}",color=Dim)
        Slider(rate,{rate=it},valueRange=.7f..1.3f,onValueChangeFinished={actions.onSaveHud(s.copy(voiceRate=rate))})
        Text("Reply volume ${(volume*100).roundToInt()}%",color=Dim)
        Slider(volume,{volume=it},onValueChangeFinished={actions.onSaveHud(s.copy(voiceVolume=volume))})
        Text("Installed offline system voices for ${state.settings.language.label}",color=Dim)
        if(state.voices.isEmpty())Text("No suitable installed system voice. Optional neural download remains available below.",color=Dim,fontSize=12.sp)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){state.voices.forEach {v->FilterChip(s.voiceId==v,{actions.onSaveHud(s.copy(voiceId=v))},label={Text(v,fontSize=10.sp)})}}
        Text("Neural voices support rate and volume; pitch is controlled by the chosen voice model. System and cloud playback animations are decorative, not an output audio meter.",color=Dim,fontSize=11.sp)
        TextButton(onClick=actions.onVoiceCheck){Text("Test voice")}
    }
}
@Composable private fun HudToggle(label:String,checked:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f),color=Ice);Switch(checked,onChange)}}
