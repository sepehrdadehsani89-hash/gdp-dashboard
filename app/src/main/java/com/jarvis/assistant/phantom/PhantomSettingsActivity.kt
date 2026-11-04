package com.jarvis.assistant.phantom

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.result.contract.ActivityResultContracts
import com.jarvis.assistant.omnisystem.PrivateModuleActivity

class PhantomSettingsActivity:PrivateModuleActivity(){
    private var permission by mutableStateOf(false)
    private var settingsToken:Long?=null
    private val overlayPermission=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        settingsToken?.let(runtime::finishAndroidUi);settingsToken=null
        permission=Settings.canDrawOverlays(this)
    }
    private fun openOverlayPermission(){
        settingsToken=runtime.pauseForAndroidUi();runtime.phantomOverlay.hide()
        runCatching{overlayPermission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))}.onFailure{
            settingsToken?.let(runtime::finishAndroidUi);settingsToken=null
            status="Open Android Settings → Apps → Special app access → Display over other apps → Jarvis."
        }
    }
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
        screen("PHANTOM · FLOATING HUD & CAPTIONS"){
            val p by runtime.phantomPreferences.state.collectAsStateWithLifecycle()
            val current by runtime.phantom.state.collectAsStateWithLifecycle()
            Text("Show the current conversation above ordinary apps or beneath the actual live-wallpaper reactor. Captions are private: enable only if you are comfortable with others seeing your unlocked screen. Hidden during screen lock and Android authentication. No second microphone is opened.")
            Text("Android display-over-other-apps permission: ${if(permission)"granted" else "not granted"}")
            OutlinedButton(onClick=::openOverlayPermission){Text("Open Android overlay permission")}
            Switch(p.enabled,{runtime.phantomPreferences.save(p.copy(enabled=it))});Text("Enable floating HUD (requires Android permission)")
            Switch(p.captions,{runtime.phantomPreferences.save(p.copy(captions=it));if(!it)runtime.phantom.clearCaption()});Text("Allow live captions on the selected Jarvis wallpaper")
            Switch(p.autoHide,{runtime.phantomPreferences.save(p.copy(autoHide=it))});Text("Hide floating HUD after the current conversation")
            Switch(p.editMode,{runtime.phantomPreferences.save(p.copy(editMode=it))});Text("Edit position: drag the capsule; tap it to minimize / restore its card")
            Text("Size ${(p.scale*100).toInt()}%");Slider(p.scale,{runtime.phantomPreferences.save(p.copy(scale=it))},valueRange=.8f..1.3f)
            Text("Opacity ${(p.opacity*100).toInt()}%");Slider(p.opacity,{runtime.phantomPreferences.save(p.copy(opacity=it))},valueRange=.65f..1f)
            Text("Caption lines ${p.lines}");Slider(p.lines.toFloat(),{runtime.phantomPreferences.save(p.copy(lines=it.toInt()))},valueRange=2f..4f,steps=1)
            OutlinedButton(onClick={runtime.phantomPreferences.save(p.copy(x=0f,y=0f,scale=1f))}){Text("Reset position and size")}
            Text("The wallpaper must be selected in Android wallpaper settings. Sensitive apps and Android may hide overlays. This HUD runs with the existing Jarvis process; no promise of survival after Android kills it. Reduced motion follows Jarvis Settings.")
            OutlinedButton(onClick={startActivity(Intent(this@PhantomSettingsActivity,com.jarvis.assistant.omnisystem.SpeakerProfilesActivity::class.java))}){Text("Manage six consenting speaker profiles")}
            var focus by remember{mutableStateOf(runtime.speakers.focusEnabled)}
            Switch(focus,{enabled->task{if(enabled){check(runtime.speakers.autoEnabled);runtime.speakers.prepare()};runtime.speakers.setFocus(enabled);focus=enabled}})
            Text("Experimental voice focus on wake-command segments. Enable optional profile hints in Speaker profiles first. After a confident match, different or uncertain segments are rejected for up to 30 seconds. One microphone cannot reliably separate simultaneous voices; this is not authentication. Short or unmatched initial speech remains neutral. Disable if it rejects your voice.")
            Text("REQUEST DIAGNOSTICS · ${current.execution}")
            Text("Only stage timings are retained in RAM (last 24 requests). No words, microphone recordings, keys or speaker samples appear below. Times are measured on this device; absent stages were not measured. Tap Refresh to view completed traces.")
            var traces by remember{mutableStateOf(runtime.phantom.traces())}
            TextButton(onClick={traces=runtime.phantom.traces()}){Text("Refresh measured timings")}
            traces.asReversed().forEach{trace->Text("Request #${trace.id}\n"+trace.events.entries.joinToString("\n"){(event,time)->"${event.name}: ${time-trace.started} ms"})}
        }
    }
    override fun onResume(){super.onResume();permission=Settings.canDrawOverlays(this)}
}
