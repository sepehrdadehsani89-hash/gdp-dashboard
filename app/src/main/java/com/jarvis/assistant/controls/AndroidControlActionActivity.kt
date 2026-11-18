package com.jarvis.assistant.controls

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import com.jarvis.assistant.omnisystem.PrivateModuleActivity

/** One explicit native-command handoff. No exported entry, silent grants, or automatic retry loop. */
class AndroidControlActionActivity:PrivateModuleActivity(){
    private var ticket:AndroidControlManager.Ticket?=null
    private var uiToken:Long?=null
    private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        endHandoff()
        if(granted && active())startFlow() else status="PERMISSION_REQUIRED · Android permission was not granted. No change is claimed."
    }
    private val system=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        endHandoff()
        val current=ticket
        if(current!=null && active())task{val result=runtime.androidControls.afterUserAction(current.command,this@AndroidControlActionActivity);status="${result.status} · ${result.detail}"}
        else status="The control request expired or was canceled. Ask again; no result is assumed."
    }
    private fun active()=ticket?.let(runtime.androidControls::current)==true && !locked()
    private fun endHandoff(){uiToken?.let(runtime::finishAndroidUi);uiToken=null}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState);if(isFinishing)return
        ticket=runtime.androidControls.takeTicket(intent.getStringExtra("control_ticket"))
        screen("ANDROID CONTROL REQUEST"){
            ticket?.command?.let{Text(AndroidControlCatalog.capability(it.id).setup)}
            Text("Only this requested control may execute. Android consent and permissions remain in Android's own screens.")
        }
        if(savedInstanceState!=null || !active()){status="The request is no longer active. Return to Jarvis and ask again.";return}
        startFlow()
    }
    private fun startFlow(){
        val current=ticket ?: return
        if(!active())return
        val required=runtime.androidControls.runtimePermission(current.command)
        if(required!=null && !runtime.androidControls.hasPermission(required)){
            uiToken=runtime.pauseForAndroidUi();runtime.phantomOverlay.hide();permission.launch(required);return
        }
        if(current.command.id==AndroidControlId.RECORD_START){
            startActivity(Intent(this,com.jarvis.assistant.screen.VideoRecordingActivity::class.java));finish();return
        }
        val intent=runtime.androidControls.userIntent(current.command)
        if(intent!=null){
            try{uiToken=runtime.pauseForAndroidUi();runtime.phantomOverlay.hide();system.launch(intent);status="USER_ACTION_REQUIRED · Android is showing the requested controls. No setting change is claimed."}
            catch(_:Exception){endHandoff();status="UNSUPPORTED · This Android screen is not available on this device. Open the matching setting manually."}
        }else task{val result=runtime.androidControls.execute(current.command,allowUi=false,host=this@AndroidControlActionActivity);status="${result.status} · ${result.detail}"}
    }
    override fun onDestroy(){endHandoff();super.onDestroy()}
}
