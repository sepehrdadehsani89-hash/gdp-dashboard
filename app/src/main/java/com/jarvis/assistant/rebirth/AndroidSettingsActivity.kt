package com.jarvis.assistant.rebirth

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import com.jarvis.assistant.omnisystem.PrivateModuleActivity

/** User-requested official settings. Pause overlays/listening until this specific handoff returns. */
class AndroidSettingsActivity:PrivateModuleActivity(){
    private var token:Long?=null
    private val settings=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        token?.let(runtime::finishAndroidUi);token=null;finish()
    }
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);if(isFinishing)return
        if(savedInstanceState!=null){token=savedInstanceState.getLong("handoff",-1).takeIf{it>=0};return}
        token=runtime.pauseForAndroidUi();runtime.phantomOverlay.hide()
        runCatching{settings.launch(Intent(Settings.ACTION_SETTINGS))}.onFailure{token?.let(runtime::finishAndroidUi);token=null;finish()}
    }
    override fun onSaveInstanceState(out:Bundle){token?.let{out.putLong("handoff",it)};super.onSaveInstanceState(out)}
}
