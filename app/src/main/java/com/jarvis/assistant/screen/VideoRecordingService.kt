package com.jarvis.assistant.screen

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Records only its fresh, consented projection into an app-owned pending MediaStore item. */
class VideoRecordingService:Service(){
    companion object {
        private const val CHANNEL="jarvis_video_recording_v1"
        private const val STOP="com.jarvis.assistant.STOP_OWNED_VIDEO"
        private val mutable=MutableStateFlow(RecordingStatus())
        val state=mutable.asStateFlow()
        internal fun publish(status:RecordingStatus){mutable.value=status}
        fun requestStop(context:Context):Boolean {if(!state.value.active)return false;return runCatching{context.startService(Intent(context,VideoRecordingService::class.java).setAction(STOP));true}.getOrDefault(false)}
    }
    private var owner=""
    private var recorder:MediaRecorder?=null
    private var projection:MediaProjection?=null
    private var display:VirtualDisplay?=null
    private var descriptor:ParcelFileDescriptor?=null
    private var destination:Uri?=null
    private var recording=false
    private var finalized=false
    override fun onBind(intent:Intent?)=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action==STOP){finishRecording(true,"Recording stopped by your request.");return START_NOT_STICKY}
        if(owner.isNotEmpty())return START_NOT_STICKY
        owner=intent?.getStringExtra("request_id").orEmpty()
        if(!VideoRecordingRequests.consume(owner) || !ProjectionLease.owned(owner)){publish(RecordingStatus(RecordingStage.FAILED,"No fresh recording consent request is active."));stopSelf();return START_NOT_STICKY}
        try{
            val manager=getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL,"Visible Jarvis screen recording",NotificationManager.IMPORTANCE_LOW))
            check(manager.areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL).importance!=NotificationManager.IMPORTANCE_NONE)
            val stop=PendingIntent.getService(this,4721,Intent(this,VideoRecordingService::class.java).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification=NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_jarvis).setContentTitle("Jarvis · screen recording").setContentText("Recording video without microphone audio. Tap Stop to finish.").setOngoing(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).addAction(0,"Stop recording",stop).build()
            startForeground(4721,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            publish(RecordingStatus(RecordingStage.STARTING,"Android accepted a new screen-recording session. No saved video is claimed yet."))
            @Suppress("DEPRECATION") val grant=intent?.getParcelableExtra<Intent>("grant") ?: error("Capture consent missing")
            projection=getSystemService(MediaProjectionManager::class.java).getMediaProjection(intent.getIntExtra("result",0),grant)
            projection!!.registerCallback(object:MediaProjection.Callback(){override fun onStop(){finishRecording(true,"Android stopped screen sharing.")}},Handler(Looper.getMainLooper()))
            @Suppress("DEPRECATION") val metrics=android.util.DisplayMetrics().also{getSystemService(WindowManager::class.java).defaultDisplay.getRealMetrics(it)}
            val size=RecordingPolicy.dimensions(metrics.widthPixels,metrics.heightPixels)
            val values=ContentValues().apply{put(MediaStore.Video.Media.DISPLAY_NAME,"Jarvis_${System.currentTimeMillis()}.mp4");put(MediaStore.Video.Media.MIME_TYPE,"video/mp4");put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/Jarvis");put(MediaStore.Video.Media.IS_PENDING,1)}
            destination=contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values) ?: error("Video storage unavailable")
            descriptor=contentResolver.openFileDescriptor(destination!!,"w") ?: error("Video file unavailable")
            @Suppress("DEPRECATION") val created=if(Build.VERSION.SDK_INT>=31)MediaRecorder(this) else MediaRecorder()
            recorder=created
            created.apply{
                setVideoSource(MediaRecorder.VideoSource.SURFACE);setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(size.first,size.second);setVideoFrameRate(24);setVideoEncodingBitRate(3_000_000);setOutputFile(descriptor!!.fileDescriptor)
                setMaxDuration(30*60*1000);setMaxFileSize(512L*1024*1024)
                setOnInfoListener{_,what,_->if(what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)finishRecording(true,"The visible recording reached its duration or size limit.")}
                setOnErrorListener{_,_,_->finishRecording(false,"Android's video recorder reported a failure.")}
                prepare()
            }
            display=projection!!.createVirtualDisplay("JarvisOwnedVideo",size.first,size.second,metrics.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,created.surface,null,Handler(Looper.getMainLooper()))
            check(display!=null)
            created.start();recording=true
            publish(RecordingStatus(RecordingStage.RECORDING,"Screen recording is active. Video only; no microphone audio. Use Stop to finalize the owned recording."))
        }catch(_:Exception){finishRecording(false,"Android could not start or maintain this recording. No successful saved video is claimed.")}
        return START_NOT_STICKY
    }
    private fun finishRecording(wantedSave:Boolean,message:String){
        if(finalized)return;finalized=true
        publish(RecordingStatus(RecordingStage.FINALIZING,"Finalizing only the current owned video recording."))
        val stopped=recording && runCatching{recorder?.stop();true}.getOrDefault(false)
        recording=false
        runCatching{display?.release()};display=null
        runCatching{recorder?.reset();recorder?.release()};recorder=null
        runCatching{projection?.stop()};projection=null
        val bytes=runCatching{descriptor?.statSize ?: 0L}.getOrDefault(0L)
        runCatching{descriptor?.close()};descriptor=null
        val uri=destination
        val duration=if(stopped && uri!=null)runCatching{val reader=MediaMetadataRetriever();try{reader.setDataSource(this,uri);reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L}finally{reader.release()}}.getOrDefault(0L) else 0L
        val valid=wantedSave && RecordingPolicy.saved(stopped,bytes,duration) && uri!=null
        if(valid){
            val saved=runCatching{contentResolver.update(uri!!,ContentValues().apply{put(MediaStore.Video.Media.IS_PENDING,0)},null,null)>0}.getOrDefault(false)
            if(saved)publish(RecordingStatus(RecordingStage.SAVED,"$message Android finalized the video in Movies/Jarvis.",uri.toString(),bytes,duration))
            else{runCatching{contentResolver.delete(uri!!,null,null)};publish(RecordingStatus(RecordingStage.FAILED,"Video finalization could not be confirmed. No saved file is claimed."))}
        }else{uri?.let{runCatching{contentResolver.delete(it,null,null)}};publish(RecordingStatus(RecordingStage.FAILED,"$message The new pending item was discarded because no completed video was verified."))}
        ProjectionLease.release(owner);stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()
    }
    override fun onDestroy(){if(!finalized)finishRecording(true,"The recording service stopped.");ProjectionLease.release(owner);super.onDestroy()}
}
