package com.jarvis.assistant.companion

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.client.android.Intents
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Actual CameraX preview + bundled on-device ML Kit QR decoding. No model/image upload. */
class PairingCaptureActivity:ComponentActivity(){
    private lateinit var view:PreviewView
    private lateinit var message:TextView
    private lateinit var torch:Button
    private var detector:BarcodeScanner?=null
    private var provider:ProcessCameraProvider?=null
    private var preview:Preview?=null
    private var analysis:ImageAnalysis?=null
    private var camera:Camera?=null
    private val executor=Executors.newSingleThreadExecutor()
    private val inFlight=AtomicBoolean(false)
    private val completed=AtomicBoolean(false)
    private val paused=AtomicBoolean(false)
    private var failures=0
    private var bound=false
    override fun onCreate(state:Bundle?){
        super.onCreate(state);window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){failure("CAMERA_PERMISSION_DENIED");return}
        val root=FrameLayout(this).apply{setBackgroundColor(Color.BLACK)}
        view=PreviewView(this).apply{implementationMode=PreviewView.ImplementationMode.COMPATIBLE;scaleType=PreviewView.ScaleType.FIT_CENTER}
        root.addView(view,FrameLayout.LayoutParams(-1,-1))
        root.addView(object:View(this){val stroke=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.CYAN;style=Paint.Style.STROKE;strokeWidth=4f};override fun onDraw(canvas:Canvas){val side=minOf(width,height)*.68f;canvas.drawRoundRect((width-side)/2,(height-side)/2,(width+side)/2,(height+side)/2,18f,18f,stroke)}},FrameLayout.LayoutParams(-1,-1))
        message=TextView(this).apply{setTextColor(Color.WHITE);textSize=17f;setBackgroundColor(0xbb000000.toInt());setPadding(20,24,20,24);text="Initializing the real camera and bundled QR decoder…"}
        root.addView(message,FrameLayout.LayoutParams(-1,-2,Gravity.TOP))
        val controls=LinearLayout(this).apply{gravity=Gravity.CENTER}
        controls.addView(Button(this).apply{text="Cancel";setOnClickListener{completed.set(true);setResult(Activity.RESULT_CANCELED);finish()}})
        controls.addView(Button(this).apply{text="Retry camera";setOnClickListener{paused.set(false);failures=0;stopCamera();message.text="Retrying the real camera…";if(provider==null)prepareCameraProvider()else bindCamera()}})
        torch=Button(this).apply{text="Flashlight";isEnabled=false;setOnClickListener{val c=camera ?: return@setOnClickListener;val enable=c.cameraInfo.torchState.value!=TorchState.ON;val task=c.cameraControl.enableTorch(enable);task.addListener({runCatching{task.get()}.onFailure{message.text="Flashlight could not be changed. Continue scanning or retry."}},ContextCompat.getMainExecutor(this@PairingCaptureActivity))}}
        controls.addView(torch);root.addView(controls,FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));setContentView(root)
        try{
            detector=BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
            prepareCameraProvider()
        }catch(_:Exception){failure("SCANNER_FAILED")}catch(_:LinkageError){failure("SCANNER_UNAVAILABLE")}
    }
    private fun prepareCameraProvider(){
        try{val future=ProcessCameraProvider.getInstance(this)
            future.addListener({if(!isFinishing && !isDestroyed)try{provider=future.get();if(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))bindCamera()}catch(_:Exception){message.text="CAMERA_UNAVAILABLE · Android could not initialize the camera. Retry or Cancel and use gallery / paste."}},ContextCompat.getMainExecutor(this))
        }catch(_:Exception){message.text="CAMERA_UNAVAILABLE · Android could not initialize the camera. Retry or use gallery / paste."}
    }
    private fun bindCamera(){
        val p=provider ?: return
        if(bound || isFinishing || isDestroyed || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))return
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){failure("CAMERA_PERMISSION_DENIED");return}
        try{
            val selector=when{p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)->CameraSelector.DEFAULT_BACK_CAMERA;p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)->CameraSelector.DEFAULT_FRONT_CAMERA;else->{failure("CAMERA_UNAVAILABLE");return}}
            val pv=Preview.Builder().build().also{it.setSurfaceProvider(view.surfaceProvider)}
            val ai=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            ai.setAnalyzer(executor,::analyze);preview=pv;analysis=ai
            camera=p.bindToLifecycle(this,selector,pv,ai);bound=true
            camera?.cameraInfo?.cameraState?.observe(this){s->if(s.error!=null)message.text="CAMERA_UNAVAILABLE · Camera is busy or Android rejected it. Retry or Cancel and use gallery / paste."}
            view.previewStreamState.observe(this){stream->if(stream==PreviewView.StreamState.STREAMING && !paused.get())message.text="Real camera active · fit the whole private computer QR inside the frame. Decoding stays on this phone."}
            torch.isEnabled=camera?.cameraInfo?.hasFlashUnit()==true
            camera?.cameraInfo?.torchState?.observe(this){state->torch.text=if(state==TorchState.ON)"Flashlight off"else"Flashlight on"}
            if(!torch.isEnabled)torch.text="No flashlight"
        }catch(_:Exception){bound=false;message.text="CAMERA_UNAVAILABLE · Camera initialization failed. Retry or Cancel and use gallery / paste."}
    }
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyze(proxy:ImageProxy){
        if(completed.get() || paused.get() || !inFlight.compareAndSet(false,true)){proxy.close();return}
        try{
            val image=proxy.image
            if(image==null){proxy.close();inFlight.set(false);return}
            val scanner=detector ?: run{proxy.close();inFlight.set(false);return}
            scanner.process(InputImage.fromMediaImage(image,proxy.imageInfo.rotationDegrees))
                .addOnSuccessListener(ContextCompat.getMainExecutor(this)){codes->
                    if(!isFinishing && !isDestroyed && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)){
                        val value=codes.firstOrNull{it.format==Barcode.FORMAT_QR_CODE && !it.rawValue.isNullOrEmpty()}?.rawValue
                        if(value!=null && completed.compareAndSet(false,true)){setResult(Activity.RESULT_OK,Intent().putExtra(Intents.Scan.RESULT,value).putExtra(Intents.Scan.RESULT_FORMAT,"QR_CODE"));finish()}
                    }
                    failures=0
                }
                .addOnFailureListener(ContextCompat.getMainExecutor(this)){decodeFailure()}
                .addOnCompleteListener{proxy.close();inFlight.set(false)}
        }catch(_:Exception){proxy.close();inFlight.set(false);runOnUiThread{decodeFailure()}}
        catch(_:LinkageError){proxy.close();inFlight.set(false);runOnUiThread{failure("SCANNER_UNAVAILABLE")}}
    }
    private fun decodeFailure(){if(isFinishing || isDestroyed)return;failures++;if(failures>=3){paused.set(true);message.text="SCANNER_FAILED · On-device decoding failed. Retry camera or Cancel and import a clear QR image / paste its full invitation."}}
    private fun failure(code:String){completed.set(true);setResult(Activity.RESULT_CANCELED,Intent().putExtra("jarvis_scan_error",code));finish()}
    private fun stopCamera(){analysis?.clearAnalyzer();camera?.cameraInfo?.cameraState?.removeObservers(this);camera?.cameraInfo?.torchState?.removeObservers(this);if(::view.isInitialized)view.previewStreamState.removeObservers(this);val owned=listOfNotNull(preview,analysis).toTypedArray();runCatching{if(owned.isNotEmpty())provider?.unbind(*owned)};preview=null;analysis=null;camera=null;bound=false}
    override fun onStart(){super.onStart();if(provider!=null)bindCamera()}
    override fun onStop(){stopCamera();super.onStop()}
    override fun onDestroy(){completed.set(true);detector?.close();detector=null;executor.shutdown();super.onDestroy()}
}
