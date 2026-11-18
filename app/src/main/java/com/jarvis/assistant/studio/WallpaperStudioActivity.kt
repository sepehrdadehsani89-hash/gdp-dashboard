package com.jarvis.assistant.studio

import android.app.KeyguardManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.jarvis.assistant.omnisystem.PrivateModuleActivity
import com.jarvis.assistant.sentinel.WallpaperFrame
import com.jarvis.assistant.sentinel.JarvisWallpaperService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.UUID

class WallpaperStudioActivity:PrivateModuleActivity(){
    private var draft by mutableStateOf(StudioPolicy.original())
    private var selection by mutableStateOf("CORE")
    private fun update(transform:(StudioLayer)->StudioLayer){val old=draft.layers.firstOrNull{it.id==selection} ?: return;runCatching{transform(old).validated()}.onSuccess{next->draft=draft.copy(layers=draft.layers.map{if(it.id==selection)next else it})}.onFailure{status="Invalid layer value; the previous draft was preserved."}}
    override fun onCreate(state:Bundle?){super.onCreate(state);if(isFinishing)return
        draft=runtime.wallpaperStudio.state.value.selected() ?: StudioPolicy.original()
        selection=draft.layers.firstOrNull{it.kind=="PART:CORE"}?.id ?: draft.layers.first().id
        screen("JARVIS · WALLPAPER & COLOR STUDIO"){
            val library by runtime.wallpaperStudio.state.collectAsStateWithLifecycle()
            val actual by runtime.state.collectAsStateWithLifecycle()
            val mode by runtime.systemModes.state.collectAsStateWithLifecycle()
            val renderer=remember{StudioWallpaperRenderer()}
            var tick by remember{mutableLongStateOf(android.os.SystemClock.elapsedRealtime())}
            LaunchedEffect(Unit){lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){while(isActive){withFrameNanos{tick=it/1_000_000};delay(50)}}}
            DisposableEffect(Unit){onDispose{renderer.pause()}}
            val privateAllowed=StudioPolicy.visiblePrivate(draft.privateWidgets,getSystemService(KeyguardManager::class.java)?.isDeviceLocked==false,!com.jarvis.assistant.modes.SystemModePolicy.privatePreviewsAllowed(mode))
            val wanted=draft.layers.filter{it.visible && it.kind.startsWith("WIDGET:")}.map{it.kind}.sorted().joinToString()
            val values=remember(actual.hud.telemetry.epochMillis/1000,wanted,privateAllowed,mode,actual.settings.provider,actual.settings.model){WallpaperWidgetValues.sample(this@WallpaperStudioActivity,runtime,draft,privateAllowed)}
            val frame=WallpaperFrame(com.jarvis.assistant.modes.ModePresentation.hud(actual.hud,mode,draft.quality),actual.audio,actual.sentinelVisual,actual.settings.language,actual.playback,actual.followUpArming,actual.voiceWindowDeadline,if(privateAllowed)actual.spokenCaption else com.jarvis.assistant.phantom.LiveCaption())
            Text("The original wallpaper stays active until you save/apply a profile. This preview uses the real rendering code and available device readings; missing sources are labeled unavailable. Private data is hidden by default and while locked or quiet.")
            runtime.wallpaperStudio.storageError?.let{Text(it)}
            Canvas(Modifier.fillMaxWidth().height(350.dp).pointerInput(selection,draft.snap){detectDragGestures{change,drag->change.consume();val layer=draft.layers.firstOrNull{it.id==selection};if(layer!=null && size.width>0 && size.height>0)update{StudioPolicy.moved(it,it.x+drag.x/size.width,it.y+drag.y/size.height,draft.snap)}}}){
                drawIntoCanvas{renderer.draw(it.nativeCanvas,frame,tick,draft,values,privateAllowed)}
                val selected=draft.layers.firstOrNull{it.id==selection}
                if(selected!=null)drawCircle(Color.White,5.dp.toPx(),androidx.compose.ui.geometry.Offset(selected.x*size.width,selected.y*size.height))
            }
            Text("Select a layer, then drag this preview. Native group X/Y are offsets around their original geometry (0.5 = unchanged); data widgets use absolute normalized positions. The white dot is an editor handle.")
            OutlinedTextField(draft.name,{draft=draft.copy(name=it)},label={Text("Profile name · up to 60 characters")})
            Row{Text("Snap to 5% grid");Switch(draft.snap,{draft=draft.copy(snap=it)})}
            Row{Text("Allow private information widgets");Switch(draft.privateWidgets,{draft=draft.copy(privateWidgets=it)})}
            Text("Private previews still require each Android/Google source's actual permission. Enabling this flag does not grant access or connect an account.")
            Text("Quality · ${draft.quality}");Row{listOf("saver","balanced","high").forEach{q->TextButton(onClick={draft=draft.copy(quality=q)}){Text(q)}}}
            var themes by remember{mutableStateOf(false)}
            OutlinedButton(onClick={themes=!themes}){Text("Premium palettes / themes")}
            if(themes)StudioPolicy.themes.forEach{name->TextButton(onClick={draft=StudioPolicy.theme(name);selection=draft.layers.first().id;themes=false}){Text(name)}}
            Text("Layers · ${draft.layers.size}/24")
            draft.layers.sortedBy{it.order}.forEach{l->OutlinedButton(onClick={selection=l.id}){Text("${if(l.id==selection)"● "else""}${l.kind} · z${l.order} · ${if(l.visible)"visible"else"hidden"}")}}
            val layer=draft.layers.firstOrNull{it.id==selection}
            if(layer!=null){
                Text("Editing ${layer.kind}")
                Row{Text("Visible");Switch(layer.visible,{v->update{it.copy(visible=v)}})}
                valueSlider("X",layer.x,0f..1f){v->update{StudioPolicy.moved(it,v,it.y,draft.snap)}}
                valueSlider("Y",layer.y,0f..1f){v->update{StudioPolicy.moved(it,it.x,v,draft.snap)}}
                valueSlider("Rotation",layer.rotation,-180f..180f){v->update{it.copy(rotation=v)}}
                valueSlider("Scale",layer.scale,.25f..2f){v->update{it.copy(scale=v)}}
                valueSlider("Opacity",layer.opacity,0f..1f){v->update{it.copy(opacity=v)}}
                valueSlider("Layer order",layer.order.toFloat(),0f..100f){v->update{it.copy(order=v.toInt())}}
                if(layer.kind=="PART:GLOW" || layer.kind.startsWith("WIDGET:"))valueSlider("Glow intensity",layer.glow,0f..1f){v->update{it.copy(glow=v)}}
                if(layer.kind.startsWith("WIDGET:") || layer.kind in setOf("PART:CORE","PART:BATTERY","PART:POWER","PART:CAPTION"))valueSlider("Text scale",layer.fontScale,.5f..2f){v->update{it.copy(fontScale=v)}}
                if(layer.kind.startsWith("WIDGET:"))Row{listOf("left","center","right").forEach{a->TextButton(onClick={update{it.copy(alignment=a)}}){Text(a)}}}
                colorEditor(layer){color,end->update{it.copy(color=color,gradientEnd=end)}}
                TextButton(onClick={if(draft.layers.size>1){draft=draft.copy(layers=draft.layers.filterNot{it.id==selection});selection=draft.layers.first().id}}){Text("Remove this layer from the draft only")}
            }
            var libraryOpen by remember{mutableStateOf(false)}
            OutlinedButton(onClick={libraryOpen=!libraryOpen}){Text("Add an information widget")}
            if(libraryOpen)WidgetSource.entries.forEach{source->TextButton(onClick={if(draft.layers.size>=24)status="24-layer limit reached; remove one draft layer first."else{val l=StudioLayer(UUID.randomUUID().toString(),"WIDGET:${source.name}",x=.5f,y=.7f,order=30);draft=draft.copy(layers=draft.layers+l);selection=l.id;libraryOpen=false}}){Text(source.name)}}
            Button(onClick={val reviewed=draft;authorize{runtime.wallpaperStudio.apply(reviewed);status="Profile saved and selected for Jarvis's wallpaper renderer. Android must separately select this WallpaperService; saving is not a physical display test."}}){Text("Save layout and select this profile")}
            OutlinedButton(onClick={draft=draft.copy(id=UUID.randomUUID().toString(),name=(draft.name.take(50)+" copy"));status="Draft copied. Save explicitly to retain it."}){Text("Create another profile from this draft")}
            library.profiles.forEach{p->
                Text("Saved: ${p.name}${if(library.active==p.id)" · selected"else""}")
                TextButton(onClick={draft=p;selection=p.layers.first().id}){Text("Load saved layout into editor")}
                TextButton(onClick={authorize{runtime.wallpaperStudio.select(p.id);status="Saved profile selected; no account or permission was changed."}}){Text("Select saved profile")}
                TextButton(onClick={authorize{runtime.wallpaperStudio.delete(p.id);status="Only this saved Studio profile was removed. Original geometry, assets and other profiles remain."}}){Text("Delete this saved profile only")}
            }
            OutlinedButton(onClick={authorize{runtime.wallpaperStudio.select(null);status="Original wallpaper composition restored. Saved Studio profiles and all original appearance preferences remain."}}){Text("Restore original wallpaper composition")}
            Button(onClick={startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,ComponentName(this@WallpaperStudioActivity,JarvisWallpaperService::class.java)))}){Text("Preview / select the real live wallpaper in Android")}
            Text("Actual Android-selected service: ${runCatching{WallpaperManager.getInstance(this@WallpaperStudioActivity).wallpaperInfo?.component?.flattenToShortString()}.getOrNull() ?: "none / unavailable"}")
            Text("Gmail unread values come only from an actual authorized Gmail UNREAD-label request and expire. Notifications are separate, permission-filtered current Android previews. Calendar/Tasks widgets show only a recent authorized read, otherwise a setup/unverified status. No Gmail Pub/Sub backend, private Maps history or fabricated telemetry is provided.")
        }
    }
    @Composable private fun valueSlider(label:String,value:Float,range:ClosedFloatingPointRange<Float>,changed:(Float)->Unit){Text("$label · ${"%.2f".format(java.util.Locale.ROOT,value)}");Slider(value,changed,valueRange=range)}
    @Composable private fun colorEditor(layer:StudioLayer,changed:(Int?,Int?)->Unit){
        var hex by remember(layer.id,layer.color){mutableStateOf(StudioColor.hex(layer.color ?: 0xff00e5ff.toInt()))}
        var end by remember(layer.id,layer.gradientEnd){mutableStateOf(StudioColor.hex(layer.gradientEnd ?: 0xffaa7cff.toInt()))}
        var r by remember(layer.id){mutableStateOf("0")};var g by remember(layer.id){mutableStateOf("229")};var b by remember(layer.id){mutableStateOf("255")}
        var hue by remember(layer.id){mutableStateOf("180")};var sat by remember(layer.id){mutableStateOf("100")};var level by remember(layer.id){mutableStateOf("100")}
        fun apply(block:()->Int){runCatching(block).onSuccess{changed(it,null)}.onFailure{status="Invalid color. Use full HEX, RGB 0–255, hue 0–360 and saturation/value/lightness 0–100."}}
        OutlinedTextField(hex,{hex=it},label={Text("Full-spectrum HEX · #RRGGBB or #AARRGGBB")})
        OutlinedButton(onClick={apply{StudioColor.hex(hex)}}){Text("Apply HEX color")}
        Row{OutlinedTextField(r,{r=it},Modifier.weight(1f),label={Text("R")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));OutlinedTextField(g,{g=it},Modifier.weight(1f),label={Text("G")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));OutlinedTextField(b,{b=it},Modifier.weight(1f),label={Text("B")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))}
        OutlinedButton(onClick={apply{StudioColor.rgb(r.toInt(),g.toInt(),b.toInt())}}){Text("Apply RGB")}
        Row{OutlinedTextField(hue,{hue=it},Modifier.weight(1f),label={Text("Hue")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal));OutlinedTextField(sat,{sat=it},Modifier.weight(1f),label={Text("S %")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal));OutlinedTextField(level,{level=it},Modifier.weight(1f),label={Text("V / L %")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal))}
        Row{TextButton(onClick={apply{StudioColor.hsv(hue.toFloat(),sat.toFloat()/100,level.toFloat()/100)}}){Text("Apply HSV")};TextButton(onClick={apply{StudioColor.hsl(hue.toFloat(),sat.toFloat()/100,level.toFloat()/100)}}){Text("Apply HSL")}}
        OutlinedTextField(end,{end=it},label={Text("Gradient second color · HEX")})
        OutlinedButton(onClick={runCatching{StudioColor.hex(hex) to StudioColor.hex(end)}.onSuccess{changed(it.first,it.second)}.onFailure{status="Both gradient stops require valid full HEX colors."}}){Text("Apply two-color gradient")}
        TextButton(onClick={changed(null,null)}){Text("Restore this layer's original colors")}
    }
}
