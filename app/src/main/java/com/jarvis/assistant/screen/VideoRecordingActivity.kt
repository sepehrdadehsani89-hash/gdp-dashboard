package com.jarvis.assistant.screen

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.jarvis.assistant.JarvisApplication
import java.util.UUID

internal object VideoRecordingRequests {
    private var pending:Pair<String,Long>?=null
    @Synchronized fun issue():String? {val id="record-${UUID.randomUUID()}";if(!ProjectionLease.claim(id))return null;pending=id to android.os.SystemClock.elapsedRealtime();return id}
    @Synchronized fun current(id:String?)=pending?.let{it.first==id && RecordingPolicy.validConsent(it.second,android.os.SystemClock.elapsedRealtime()) && ProjectionLease.owned(it.first)}==true
    @Synchronized fun consume(id:String?):Boolean {if(!current(id))return false;pending=null;return true}
    @Synchronized fun cancel(id:String?){if(pending?.first==id){pending=null;id?.let(ProjectionLease::release)}}
}
/** Always foreground, fresh system consent, visible video-only recording. */
class VideoRecordingActivity:ComponentActivity(){
    private val runtime get()=(application as JarvisApplication).runtime
    private var requestId:String?=null
    private var handoff:Long?=null
    private var started=false
    private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){if(it)requestCapture() else cancel("Notification permission was not granted; recording did not start.")}
    private val capture=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        if(result.resultCode==Activity.RESULT_OK && result.data!=null && VideoRecordingRequests.current(requestId) && !getSystemService(KeyguardManager::class.java).isDeviceLocked){
            try{ContextCompat.startForegroundService(this,Intent(this,VideoRecordingService::class.java).putExtra("request_id",requestId).putExtra("grant",result.data).putExtra("result",result.resultCode));started=true}
            catch(_:Exception){cancel("Android could not start the visible recording service.")}
        }else cancel("Screen recording was canceled or its fresh permission expired. No video is claimed.")
        finish()
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        if(getSystemService(KeyguardManager::class.java).isDeviceLocked){finish();return}
        if(savedInstanceState!=null){requestId=savedInstanceState.getString("request");handoff=savedInstanceState.getLong("handoff",-1).takeIf{it>=0};return}
        requestId=VideoRecordingRequests.issue()
        if(requestId==null){cancel("A screen capture or recording already owns Android projection. Stop it first.");finish();return}
        VideoRecordingService.publish(RecordingStatus(RecordingStage.CONSENT_REQUIRED,"Android screen-capture permission is required for a new video recording."))
        handoff=runtime.pauseForAndroidUi();runtime.phantomOverlay.hide()
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)permission.launch(Manifest.permission.POST_NOTIFICATIONS) else requestCapture()
    }
    private fun requestCapture(){
        if(!VideoRecordingRequests.current(requestId)){cancel("The recording request expired. Ask again.");finish();return}
        if(!getSystemService(NotificationManager::class.java).areNotificationsEnabled()){cancel("Enable Jarvis notifications before starting visible screen recording.");finish();return}
        android.app.AlertDialog.Builder(this).setTitle("Record the screen?").setMessage("Android will ask you to choose the screen or app. Video is saved to Movies/Jarvis on this phone, without microphone audio. A visible notification and Android indicator remain active. Recording is limited to 30 minutes or 512MB per session; protected screens may be blank.")
            .setNegativeButton("Cancel"){_,_->cancel("Recording canceled.");finish()}.setPositiveButton("Continue to Android"){_,_->try{capture.launch(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent())}catch(_:Exception){cancel("Android screen-capture consent is unavailable.");finish()}}.setOnCancelListener{cancel("Recording canceled.");finish()}.show()
    }
    private fun cancel(message:String){VideoRecordingRequests.cancel(requestId);if(!VideoRecordingService.state.value.active)VideoRecordingService.publish(RecordingStatus(RecordingStage.CANCELED,message));android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_LONG).show()}
    override fun onSaveInstanceState(out:Bundle){out.putString("request",requestId);handoff?.let{out.putLong("handoff",it)};super.onSaveInstanceState(out)}
    override fun onDestroy(){if(!isChangingConfigurations){if(!started)VideoRecordingRequests.cancel(requestId);handoff?.let(runtime::finishAndroidUi)};super.onDestroy()}
}
