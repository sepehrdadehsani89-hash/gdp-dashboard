package com.jarvis.assistant.ui

import android.animation.ValueAnimator
import android.os.SystemClock
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import com.jarvis.assistant.sentinel.*
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
import androidx.lifecycle.repeatOnLifecycle
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
    Column(modifier.background(Brush.verticalGradient(listOf(Back,Color.Black)),CutCornerShape(10.dp)).border(.7.dp,MaterialTheme.colorScheme.primary.copy(alpha=.48f),CutCornerShape(10.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
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
    var conversation by remember{mutableStateOf(false)}
    var presentationNow by remember{mutableLongStateOf(SystemClock.elapsedRealtime())}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state.sentinelVisual.activationId,lifecycle){
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
            while(state.sentinelVisual.activationId>0 && SystemClock.elapsedRealtime()-state.sentinelVisual.activatedAt<2100){presentationNow=SystemClock.elapsedRealtime();delay(50)}
            presentationNow=SystemClock.elapsedRealtime()
        }
    }
    val reveal=if(state.sentinelVisual.mode==SentinelMode.ACTIVATING && !s.reducedMotion && t.systemMotion)ReactorGeometry.awakening(presentationNow-state.sentinelVisual.activatedAt).panels else 1f
    val panelMotion=Modifier.graphicsLayer{alpha=reveal}.drawBehind{drawLine(color=Color(0xFF00E5FF),start=Offset.Zero,end=Offset(size.width*reveal,0f),strokeWidth=1.dp.toPx())}
    val color=when(s.accent){"purple"->Purple;"blue"->Color(0xFF38BDF8);else->Cyan}
    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Column(Modifier.weight(1f)) {
                Text("J A R V I S",color=color,fontSize=24.sp,letterSpacing=5.sp,fontWeight=FontWeight.Light)
                Text("PERSONAL INTELLIGENCE SYSTEM",color=Dim,fontSize=8.sp,letterSpacing=2.sp)
            }
            IconButton(onClick={actions.onInteraction(SentinelSound.PANEL_OPEN);onSettings()}){Icon(Icons.Rounded.Settings,"HUD settings",tint=color)}
            IconButton(onClick=actions.onDismiss){Icon(Icons.Rounded.Close,"Dismiss Jarvis",tint=Dim)}
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
            Column(Modifier.weight(1f)){
                Text(ClockDisplay.time(t.epochMillis,t.timezone,locale,s.clockFormat,t.system24Hour),color=Ice,fontSize=30.sp,fontWeight=FontWeight.Light)
                Text(ClockDisplay.date(t.epochMillis,t.timezone,locale),color=Dim,fontSize=11.sp)
                Text(t.timezone,color=Dim,fontSize=9.sp)
            }
            WeatherPanel(state,actions,Modifier.weight(1f),compact=true)
        }
        CommandCountdown(state)
        state.futureSuggestion?.let{suggestion->
            HudPanel("FUTURE ACTION · ${suggestion.source.name}",Modifier.fillMaxWidth()){
                Text(suggestion.title,color=Ice,maxLines=2)
                Text(suggestion.explanation,color=Dim,fontSize=11.sp,maxLines=3)
                Row{
                    TextButton(onClick={actions.onSuggestion(suggestion.id,"review")}){Text("Review")}
                    TextButton(onClick={actions.onSuggestion(suggestion.id,"snooze")}){Text("Snooze")}
                    TextButton(onClick={actions.onSuggestion(suggestion.id,"dismiss")}){Text("Not now")}
                    TextButton(onClick={actions.onSuggestion(suggestion.id,"never")}){Text("Never")}
                }
            }
        }
        if(state.aiCall.active){
            HudPanel("JARVIS AI CALL · ${state.aiCall.phase.name}",Modifier.fillMaxWidth()){
                Text(if(state.aiCall.phase==com.jarvis.assistant.call.AiCallPhase.RINGING)"Jarvis is calling. Pick up to talk." else "Your AI conversation is active.",color=Ice)
                Row{
                    if(state.aiCall.phase==com.jarvis.assistant.call.AiCallPhase.RINGING)TextButton(onClick={actions.onSend("answer Jarvis call")}){Text("Answer")}
                    TextButton(onClick={actions.onSend("end Jarvis call")}){Text(if(state.aiCall.phase==com.jarvis.assistant.call.AiCallPhase.RINGING)"Decline" else "End call")}
                }
            }
        }
        val hour=java.time.Instant.ofEpochMilli(t.epochMillis).atZone(java.time.ZoneId.of(t.timezone)).hour
        Text(when(hour){in 5..11->l.choose("Good morning","Buenos días","صبح بخیر");in 12..17->l.choose("Good afternoon","Buenas tardes","عصر بخیر");else->l.choose("Good evening","Buenas noches","شب بخیر")}+state.settings.name.takeIf(String::isNotBlank)?.let {", $it"}.orEmpty(),color=Dim,fontSize=11.sp)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide=maxWidth>=650.dp
            if(wide) Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                SystemPanel(state,actions,Modifier.weight(1f).then(panelMotion)); HudCore(state,Modifier.weight(2f),actions); TelemetryPanel(state,Modifier.weight(1f).then(panelMotion))
            } else Column {
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){SystemPanel(state,actions,Modifier.weight(1f).then(panelMotion));TelemetryPanel(state,Modifier.weight(1f).then(panelMotion))}
                HudCore(state,Modifier.fillMaxWidth(),actions)
            }
        }
        if(h.power.stripVisible)HudPanel(l.choose("Power Core · charging","Núcleo de energía · carga","شارژ"),Modifier.fillMaxWidth().border(.7.dp,Color(ReactorPalette.green),CutCornerShape(10.dp))){
            val remaining=h.power.estimate?.remaining(SystemClock.elapsedRealtime())
            Text(com.jarvis.assistant.power.ChargingText.strip(h.power,SystemClock.elapsedRealtime(),l==AppLanguage.ES),color=Ice,fontSize=12.sp)
            if(h.power.charging && remaining!=null)Text("~ "+com.jarvis.assistant.power.ChargingText.completion(remaining,System.currentTimeMillis(),t.timezone),color=Cyan,fontSize=11.sp)
            TextButton(onClick={actions.onSend("open power core")}){Text(l.choose("Charging details & reminders","Detalles de carga y avisos","جزئیات شارژ"))}
        }
        val userCaption=state.transcript.ifBlank{state.messages.lastOrNull{it.role==MessageRole.USER}?.text.orEmpty()}
        val assistantCaption=state.liveReply.ifBlank{state.messages.lastOrNull{it.role==MessageRole.ASSISTANT}?.text.orEmpty()}
        val captions=rememberScrollState()
        LaunchedEffect(userCaption,assistantCaption,captions.maxValue){if(s.reducedMotion || !t.systemMotion)captions.scrollTo(captions.maxValue) else captions.animateScrollTo(captions.maxValue)}
        HudPanel(l.choose("Live captions · tap to expand","Subtítulos · toca para ampliar","زیرنویس زنده · لمس برای نمایش کامل"),Modifier.fillMaxWidth().clickable(role=Role.Button){actions.onInteraction(SentinelSound.PANEL_OPEN);conversation=true}){
            Column(Modifier.fillMaxWidth().heightIn(max=112.dp).verticalScroll(captions),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Text(confirmedCaption(l.choose("YOU: ","TÚ: ","شما: ")+userCaption.ifBlank{l.choose("Tap the microphone or use your enabled wake phrase.","Toca el micrófono o usa la frase de activación.","میکروفن یا عبارت بیدارباش را فعال کنید.")},state.confirmedEntity),color=Purple,fontSize=13.sp)
                if(assistantCaption.isNotBlank())Text("JARVIS: $assistantCaption",color=Ice,fontSize=13.sp)
            }
            Text(if(state.automaticSessionNotes)l.choose("Temporary chat · local session summaries ON","Chat temporal · resúmenes locales ACTIVADOS","گفت‌وگوی موقت · یادداشت محلی روشن") else l.choose("Temporary chat · no automatic summaries","Chat temporal · sin resúmenes automáticos","گفت‌وگوی موقت · بدون یادداشت خودکار"),color=Dim,fontSize=10.sp)
            state.error?.takeIf{it.isNotBlank()}?.let{Text(it,color=Color(0xFFFFB3B3),fontSize=12.sp)}
            Row {
                if(state.lockCountdownPending)TextButton(onClick={actions.onSend("cancel lock countdown")}){Text(l.choose("Cancel lock","Cancelar bloqueo","لغو قفل"))}
                TextButton(onClick={actions.onSend("I'm done")}){Text(l.choose("End & clear chat","Terminar y borrar chat","پایان و پاک کردن"))}
                TextButton(onClick=actions.onOpenOutputVoice){Text(l.choose("Output voice","Voz de salida","صدای خروجی"))}
            }
        }
        HudPanel(l.choose("Quick info","Información","اطلاعات"),Modifier.fillMaxWidth().then(panelMotion)) {
                Reading("MEMORIES",state.memories.size.toString())
                Reading("FILES",state.attachedFiles.size.toString())
                Reading("PROVIDER",state.settings.provider.uppercase())
                Text(if(state.settings.provider=="local")state.brainStatus else if(state.hasApiKey)"Key saved · use Test connection to verify" else "API key missing",color=Dim,fontSize=10.sp)
                Text(state.voiceStatus,color=Dim,fontSize=10.sp)
        }
        if(state.authorization!=null) AuthorizationPanel(state,actions)
    }
    if(conversation)AlertDialog(onDismissRequest={conversation=false},title={Text(l.choose("Conversation","Conversación","گفت‌وگو"))},text={
        androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max=420.dp)){
            state.messages.takeLast(30).forEach{message->item{Text("${message.role}: ${message.text}",Modifier.padding(vertical=8.dp))}}
            item{Text(state.transcript.ifBlank{"No active transcript"})}
            if(state.liveReply.isNotBlank())item{Text("JARVIS: ${state.liveReply}")}
        }
    },confirmButton={TextButton(onClick={conversation=false;actions.onInteraction(SentinelSound.PANEL_CLOSE)}){Text("Close")}},dismissButton={TextButton(onClick={conversation=false;actions.onSend("I'm done")}){Text(l.choose("End & clear","Terminar y borrar","پایان و پاک کردن"))}})
}
@Composable private fun SystemPanel(state:JarvisUiState,actions:JarvisUiActions,modifier:Modifier) {
    val t=state.hud.telemetry
    var details by remember{mutableStateOf(false)}
    HudPanel("System link",modifier.clickable(role=Role.Button){actions.onInteraction(SentinelSound.PANEL_OPEN);details=true}){
        Reading("NETWORK",t.network)
        Reading("INTERNET",if(t.internetValidated)"Validated" else "Not verified")
        Reading("AUDIO IN",if(state.audio.recording)"Recording" else "Inactive")
        Reading("WAKE WORD",state.settings.language.wakePhrase)
        Text(state.wakeStatus.ifBlank {"Wake listening off"},color=Dim,fontSize=10.sp)
    }
    if(details)AlertDialog(onDismissRequest={details=false},title={Text("Connection status")},text={Text("Transport: ${t.network}\nAndroid validated internet: ${t.internetValidated}\nProvider: ${state.settings.provider}\n${if(state.hasApiKey)"A key is saved; Test connection verifies it." else "No provider key saved."}\n${state.brainStatus}",Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()))},
        confirmButton={TextButton(onClick={actions.onOpenPermission("network")}){Text("Network settings")}},dismissButton={TextButton(onClick={details=false}){Text("Close")}})
}
@Composable private fun TelemetryPanel(state:JarvisUiState,modifier:Modifier) {
    val t=state.hud.telemetry;val b=t.battery
    HudPanel("Telemetry",modifier) {
        Reading("NETWORK",t.network)
        Reading("AI ENGINE",state.settings.provider.toString())
        Reading("MICROPHONE",if(state.audio.recording) "Recording" else "Inactive")
        Reading("BATTERY °C",b.temperatureC?.let {String.format(Locale.ROOT,"%.1f",it)} ?: "Unavailable")
        Reading("HEALTH",b.health)
    }
}
@Composable internal fun HudCore(state:JarvisUiState,modifier:Modifier=Modifier,actions:JarvisUiActions?=null) {
    val t=state.hud.telemetry; val s=state.hud.settings; val b=t.battery
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var frameNow by remember{mutableLongStateOf(SystemClock.elapsedRealtime())}
    val auth=state.authorization?.status
    val authorizing=auth in listOf(AuthorizationStatus.AUTHORIZATION_REQUIRED,AuthorizationStatus.AWAITING_CODEX,AuthorizationStatus.IDENTITY_CONFIRMATION)
    val rawMode=if(authorizing)SentinelMode.AUTHORIZATION_REQUIRED else when(state.phase){AssistantPhase.LISTENING->SentinelMode.LISTENING;AssistantPhase.SPEAKING->SentinelMode.SPEAKING;AssistantPhase.THINKING->SentinelMode.PROCESSING;AssistantPhase.ERROR->SentinelMode.ERROR;else->state.sentinelVisual.mode}
    val mode=when{CoreMotionPolicy.errorVisible(state.voiceErrorAt,frameNow)->SentinelMode.ERROR;rawMode==SentinelMode.ERROR && state.voiceErrorAt>0->SentinelMode.DORMANT;else->rawMode}
    val context=androidx.compose.ui.platform.LocalContext.current
    val lowRam=remember(context){context.getSystemService(android.app.ActivityManager::class.java).isLowRamDevice}
    val refresh=androidx.compose.ui.platform.LocalView.current.display?.refreshRate ?: 60f
    val fps=CoreMotionPolicy.fps(s,b,t.powerSaver,ValueAnimator.areAnimatorsEnabled(),mode,lowRam || state.performanceRestricted,t.thermalStatus,refresh)
    val frameClock=remember{ReactorFrameClock()}
    val chargingClock=remember{com.jarvis.assistant.power.ChargingVisualClock()}
    var chargingFrame by remember{mutableStateOf(com.jarvis.assistant.power.ChargingVisual())}
    var angle by remember{mutableFloatStateOf(0f)}
    var batteryDetails by remember{mutableStateOf(false)}
    val boot=if(state.sentinelVisual.mode==SentinelMode.ACTIVATING && fps>0)ReactorGeometry.awakening(frameNow-state.sentinelVisual.activatedAt) else Awakening(1f,1f,1f,0f,true)
    val speed by rememberUpdatedState(ReactorGeometry.speed(s,mode,state.execution==com.jarvis.assistant.phantom.ExecutionState.EXECUTING))
    val actualCharging=ReactorPalette.chargingAccent(state.hud.power.connected,state.hud.power.charging,s.sentinel.chargingEffects)
    val chargeActive by rememberUpdatedState(actualCharging)
    val chargeSpeed by rememberUpdatedState(s.sentinel.animationSpeed)
    val green=if(fps>0)chargingFrame else com.jarvis.assistant.power.ChargingVisual(chargingFrame.angle,if(actualCharging)1f else 0f)
    LaunchedEffect(lifecycle,fps) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
            frameClock.pause();chargingClock.pause()
            try{
                if(fps>0){
                    var previousFrame=0L
                    while(true)withFrameNanos{nanos->
                        if(previousFrame==0L || nanos-previousFrame>=1_000_000_000L/fps){
                            previousFrame=nanos;frameNow=SystemClock.elapsedRealtime();angle=frameClock.frame(frameNow,speed)
                            chargingFrame=chargingClock.frame(frameNow,chargeActive,true,chargeSpeed)
                        }
                    }
                }
            }finally{frameClock.pause();chargingClock.pause()}
        }
    }
    val color=Color(ReactorPalette.core(mode,s.accent))
    val ticks=remember { ReactorGeometry.ticks.map{Offset(it.x,it.y)} }
    Box(modifier.aspectRatio(1f).sizeIn(maxWidth=450.dp,maxHeight=450.dp).clickable(enabled=actions!=null,role=Role.Button,onClickLabel="Activate voice"){actions?.onMic?.invoke()},contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize().semantics { contentDescription="Battery reactor ${b.percent?.toString() ?: "unavailable"} percent" }) {
            val r=size.minDimension*.42f*s.sentinel.reactorScale; val center=Offset(size.width/2,size.height/2)
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha=(.24f*s.sentinel.ambientGlow*boot.core*CoreMotionPolicy.breathing(frameNow,mode,fps>0)).coerceIn(0f,1f)),color.copy(alpha=.02f),Color.Transparent),center,r),r,center)
            ticks.forEachIndexed { i,v -> if(s.sentinel.detail=="minimal" && i%3!=0)return@forEachIndexed; val outer=center+v*r;val inner=center+v*(r-if(i%5==0)8.dp.toPx() else 3.dp.toPx());drawLine(color.copy(alpha=if(i%5==0).8f else .4f),inner,outer,if(i%5==0)1.2.dp.toPx() else .6.dp.toPx()) }
            val energy=CoreMotionPolicy.visualLevel(if(state.playback.active)state.playback.level else if(state.audio.recording && state.phase==AssistantPhase.LISTENING)state.audio.level else 0f)
            drawCircle(color.copy(alpha=.09f+energy*.22f),r*.52f,center,style=Stroke((5+energy*4).dp.toPx()))
            for(i in 0 until (s.sentinel.particleDensity*(if(s.sentinel.detail=="minimal")8 else 24)).toInt()){val rad=(i*30f+angle*.35f)*PI/180;val rr=r*(.58f+(i%3)*.08f);drawCircle(color.copy(alpha=.35f),.8.dp.toPx(),center+Offset(cos(rad).toFloat()*rr,sin(rad).toFloat()*rr))}
            for(ring in 0..4){val rr=r*ReactorGeometry.ringScales[ring];drawCircle(color.copy(alpha=.25f),rr,center,style=Stroke(.6.dp.toPx()))}
            for(ring in 0..2){val rr=r*ReactorGeometry.arcScales[ring];val spin=angle*(if(ring%2==0)1 else -1)
                rotate(spin,center){for(i in 0..2){ drawArc(if(i==1)Purple.copy(alpha=.85f) else color.copy(alpha=.8f),i*120f+12,(if(ring==0)40f else 64f)*boot.rings,false,Offset(center.x-rr,center.y-rr),Size(rr*2,rr*2),style=Stroke(if(ring==0)5.dp.toPx() else 2.dp.toPx()))}}
            }
            if(boot.pulse>0)drawCircle(color.copy(alpha=boot.pulse*.5f),r*(.5f+boot.pulse*.6f),center,style=Stroke(1.5.dp.toPx()))
            b.percent?.let{percent->
                val tone=Color(com.jarvis.assistant.titan.BatteryPolicy.band(percent).argb)
                val rr=r*.55f
                drawArc(tone.copy(alpha=.7f),-90f,percent*3.6f,false,Offset(center.x-rr,center.y-rr),Size(rr*2,rr*2),style=Stroke(2.dp.toPx()))
            }
            if(green.alpha>0){val rr=r*1.025f;drawArc(Color(ReactorPalette.green).copy(alpha=.9f*green.alpha),green.angle,36f,false,Offset(center.x-rr,center.y-rr),Size(rr*2,rr*2),style=Stroke(2.dp.toPx()))
                drawArc(Color(ReactorPalette.green).copy(alpha=.35f*green.alpha),green.angle-30,22f,false,Offset(center.x-rr,center.y-rr),Size(rr*2,rr*2),style=Stroke(.8.dp.toPx()))}
            val core=r*.49f*(if(state.playback.active)1f+energy*.035f else CoreMotionPolicy.breathing(frameNow,mode,fps>0))
            drawCircle(Brush.radialGradient(listOf(Back,color.copy(alpha=.18f)),center,core),core,center)
            drawCircle(color,core,center,style=Stroke(1.4.dp.toPx()))
            val pointer=Path().apply{moveTo(center.x,center.y-r*.73f);lineTo(center.x-4.dp.toPx(),center.y-r*.80f);lineTo(center.x+4.dp.toPx(),center.y-r*.80f);close()}
            drawPath(pointer,color)
            // Mesh is decorative geometry. It never represents a sensor reading.
            for(i in -3..3){val y=center.y+i*core/4;val dx=sqrt(max(0f,core*core-(y-center.y).pow(2)));drawLine(color.copy(alpha=.10f),Offset(center.x-dx,y),Offset(center.x+dx,y),.6f)}
            if(state.audio.recording && state.phase==AssistantPhase.LISTENING || state.playback.active){val values=if(state.playback.active)state.playback.history else state.audio.history; val step=core*1.45f/32
                values.forEachIndexed { i,value -> val x=center.x-core*.725f+i*step;val height=CoreMotionPolicy.visualLevel(value)*core*.28f;drawLine(Purple,Offset(x,center.y+core*.55f-height),Offset(x,center.y+core*.55f+height),1.4.dp.toPx()) }
            }
            for(i in 0..3){val rad=i*PI/2;val v=Offset(cos(rad).toFloat(),sin(rad).toFloat());drawLine(color,center+v*(r*.97f),center+v*(r*1.06f),1.3.dp.toPx())}
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth(.48f)) {
            if(authorizing) Icon(Icons.Rounded.Lock,"Authorization required",Modifier.size(16.dp),tint=Purple)
            Text(if(authorizing)"CODEX" else when(state.phase){AssistantPhase.LISTENING->"LISTENING";AssistantPhase.SPEAKING->"SPEAKING";AssistantPhase.THINKING->"THINKING";AssistantPhase.ERROR->"ATTENTION";else->"READY"},color=Ice,fontSize=10.sp,letterSpacing=2.sp)
            if(state.phase==AssistantPhase.THINKING && state.neuralStage!="Idle")Text(state.neuralStage,color=Ice,fontSize=9.sp)
            Text(b.percent?.let {"$it%"} ?: "—",modifier=Modifier.clickable(enabled=actions!=null,role=Role.Button,onClickLabel="Battery details"){actions?.onInteraction?.invoke(SentinelSound.PANEL_OPEN);batteryDetails=true},color=Color(com.jarvis.assistant.titan.BatteryPolicy.band(b.percent).argb),fontSize=44.sp,fontWeight=FontWeight.Light)
            Text(if(b.full)"FULL" else if(b.charging)"CHARGING · ${b.source}" else "BATTERY",color=color,fontSize=9.sp,textAlign=TextAlign.Center)
        }
        Text(if(state.playback.active)"LIVE AUDIO · ${state.playback.source.uppercase()}" else if(state.audio.recording)"LIVE MIC · ${state.audio.source}" else if(state.phase==AssistantPhase.SPEAKING)"SPEAKING · DECORATIVE MOTION" else "${s.animationProfile.uppercase()} · ${if(fps==0)"STILL" else "$fps FPS CAP"}",color=Dim,fontSize=8.sp,modifier=Modifier.align(Alignment.BottomCenter))
    }
    CoreMotionPolicy.pauseReason(s,ValueAnimator.areAnimatorsEnabled())?.let{reason->
        Text(reason,color=Dim,fontSize=11.sp)
        if(actions!=null && ValueAnimator.areAnimatorsEnabled())TextButton(onClick={actions.onSaveHud(s.copy(reducedMotion=false,animationIntensity=.7f,sentinel=s.sentinel.copy(animationSpeed=1f)))}){Text("Restore core motion")}
    }
    if(batteryDetails)AlertDialog(onDismissRequest={batteryDetails=false},title={Text("Battery details")},text={Text("Charge: ${b.percent?.let{"$it%"} ?: "Unavailable"}\nCharging: ${b.charging}\nFull: ${b.full}\nSource: ${b.source}\nTemperature: ${b.temperatureC?.let{"$it °C"} ?: "Unavailable"}\nHealth: ${b.health}\nAndroid power saver: ${t.powerSaver}",Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()))},confirmButton={TextButton(onClick={batteryDetails=false}){Text("Close")}})
}
@Composable private fun WeatherPanel(state:JarvisUiState,actions:JarvisUiActions,modifier:Modifier,compact:Boolean=false) {
    val w=state.hud.weather;val r=w.reading;val f=state.hud.settings.fahrenheit
    var details by remember{mutableStateOf(false)}
    fun temp(v:Double?)=v?.let {"${if(f)(it*9/5+32).roundToInt() else it.roundToInt()}°${if(f)"F" else "C"}"} ?: "Unavailable"
    HudPanel("Local conditions",modifier.clickable(role=Role.Button){actions.onInteraction(SentinelSound.PANEL_OPEN);details=true}) {
        if(r==null)Text(if(w.loading)"Fetching weather…" else "Set a city in Settings or choose approximate location.",color=Dim,fontSize=11.sp)
        else {
            Text(r.place,color=Ice,fontSize=12.sp)
            Text(when(r.code){0->"Clear";1,2->"Partly cloudy";3->"Overcast";45,48->"Fog";51,53,55,56,57->"Drizzle";61,63,65,66,67,80,81,82->"Rain";71,73,75,77,85,86->"Snow";95,96,99->"Thunderstorm";else->"Conditions unavailable"},color=Ice,fontSize=11.sp)
            if(compact)Text(temp(r.temperatureC),color=Ice,fontSize=18.sp)
            if(!compact){
            Reading("TEMP / FEELS", "${temp(r.temperatureC)} / ${temp(r.feelsLikeC)}")
            Reading("HUMIDITY",r.humidity?.let {"$it%"} ?: "Unavailable")
            Reading("WIND",r.windKmh?.let {"$it km/h"} ?: "Unavailable")
            Reading("RAIN CHANCE",r.precipitationProbability?.let {"$it%"} ?: "Unavailable")
            Reading("SUNRISE",r.sunrise?.substringAfter('T') ?: "Unavailable")
            Reading("SUNSET",r.sunset?.substringAfter('T') ?: "Unavailable")
            }
            Text("${if(w.stale)"STALE · " else ""}${r.observationTime.replace('T',' ')} · place local time",color=Dim,fontSize=9.sp)
        }
        w.error?.let {Text(it,color=Color(0xFFFFBF55),fontSize=10.sp)}
        if(w.loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(state.hud.settings.weatherCity.isNotBlank())TextButton(onClick=actions.onWeatherRefresh){Text("Refresh",fontSize=11.sp)}
        TextButton(onClick={actions.onOpenSource("https://open-meteo.com/")}){Text("Open-Meteo · CC BY 4.0",color=Dim,fontSize=9.sp)}
    }
    if(details)AlertDialog(onDismissRequest={details=false},title={Text("Weather details")},text={Text(if(r==null)w.error ?: "Choose a city in Settings or request approximate location once." else WeatherQuestion.describe(r,state.settings.language,f)+"\nSunrise: ${r.sunrise ?: "Unavailable"}\nSunset: ${r.sunset ?: "Unavailable"}\n${if(w.stale)"Cached / stale" else "Current cached reading"}",Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()))},
        confirmButton={TextButton(onClick=actions.onWeatherRefresh,enabled=state.hud.settings.weatherCity.isNotBlank()){Text("Refresh")}},dismissButton={TextButton(onClick={details=false}){Text("Close")}})
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
        Text("JARVIS · "+com.jarvis.assistant.BuildConfig.VERSION_NAME,color=Ice)
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
        Text("Voice motion follows actual decoded speech or neural playback PCM when available. Decorative motion is labeled when audio analysis is unavailable. It is not a measurement of speaker loudness.",color=Dim,fontSize=11.sp)
        TextButton(onClick=actions.onVoiceCheck){Text("Test voice")}
    }
}
@Composable private fun HudToggle(label:String,checked:Boolean,onChange:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f),color=Ice);Switch(checked,onChange)}}

@Composable private fun CommandCountdown(state:JarvisUiState){
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var seconds by remember(state.voiceWindowDeadline){mutableIntStateOf(((state.voiceWindowDeadline-SystemClock.elapsedRealtime()+999)/1000).toInt().coerceAtLeast(0))}
    LaunchedEffect(state.voiceWindowDeadline,lifecycle){lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
        while(state.voiceWindowDeadline>SystemClock.elapsedRealtime()){
            seconds=((state.voiceWindowDeadline-SystemClock.elapsedRealtime()+999)/1000).toInt().coerceAtLeast(0);delay(200)
        };seconds=0
    }}
    Text(if(seconds>0)"INACTIVITY · ${seconds}s" else if(state.followUpArming)"MIC REARMING · FOLLOW-UP NEXT" else if(state.phase==AssistantPhase.LISTENING)"LISTENING · ADAPTIVE END OF TURN" else if(state.wakeEnabled)"WAKE WORD ONLY · ${state.settings.language.wakePhrase}" else "WAKE OFF · TAP MIC",color=if(seconds>0)Cyan else Dim,fontSize=10.sp)
}
