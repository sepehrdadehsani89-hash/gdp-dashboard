package com.jarvis.assistant.omnisystem

import android.Manifest
import android.app.KeyguardManager
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.jarvis.assistant.JarvisApplication
import java.io.File

/** Visible still capture only. Old camera/capture callbacks cannot replace the current result. */
class CameraHudActivity : ComponentActivity() {
    private val runtime get() = (application as JarvisApplication).runtime
    private val handler = Handler(Looper.getMainLooper())
    private var allowed by mutableStateOf(false)
    private var status by mutableStateOf("Camera permission is required for a live preview.")
    private var ready by mutableStateOf(false)
    private var capturing by mutableStateOf(false)
    private var retry by mutableStateOf(false)
    private var front by mutableStateOf(true)
    private var revision by mutableIntStateOf(0)
    private var captured by mutableStateOf<File?>(null)
    private var image by mutableStateOf<Bitmap?>(null)
    private var provider: ProcessCameraProvider? = null
    private var boundPreview: PreviewView? = null
    private var capture: ImageCapture? = null
    private var previewGeneration = 0L
    private var captureGeneration = 0L
    private var timeout: Runnable? = null
    private val files = mutableListOf<File>()
    private var retained: File? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted
        status = if (granted) "Preparing camera…" else "Camera permission denied. Nothing was captured."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (locked() || !runtime.beginCamera(this)) { finish(); return }
        allowed = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("J.A.R.V.I.S. · CAMERA VISION", style = MaterialTheme.typography.titleLarge)
                        Text(status)
                        if (allowed) key(front, revision) {
                            AndroidView(factory = { context -> PreviewView(context).also(::bind) }, modifier = Modifier.fillMaxWidth().weight(1f))
                        } else Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) { Text("Allow visible camera preview") }
                        image?.let { Image(it.asImageBitmap(), "Explicitly captured still", Modifier.fillMaxWidth().height(140.dp)) }
                        if (captured != null) Button(onClick = ::usePhoto) { Text("Use this photo in Assistant") }
                        if (captured != null) OutlinedButton(onClick = ::analyzePhoto) { Text("Describe this photo · review sharing") }
                        if (retry && !capturing) OutlinedButton(onClick = ::retryPreview) { Text("Retake / retry preview") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = ::takePhoto, enabled = ready && !capturing) { Text("Capture still") }
                            OutlinedButton(onClick = { discardReview(); front = !front; ready = false }, enabled = !capturing) { Text("Switch camera") }
                            TextButton(onClick = { finish() }) { Text("Close") }
                        }
                        Text("Captures are private and temporary. Closing deletes unused captures. Use this photo explicitly attaches it to Assistant; your selected provider and existing privacy controls apply. Nothing is uploaded automatically. Previously enabled wake listening resumes on close. Retained captures older than 24 hours are cleaned on the next capture.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    private fun locked() = getSystemService(KeyguardManager::class.java).isDeviceLocked
    private fun bind(view: PreviewView) {
        val generation = ++previewGeneration
        ready = false; retry = false
        boundPreview?.previewStreamState?.removeObservers(this)
        boundPreview = view
        status = "Preparing ${if (front) "front" else "rear"} camera…"
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            if (generation != previewGeneration || isFinishing || isDestroyed) return@addListener
            try {
                check(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { "Camera permission was revoked." }
                val camera = future.get(); provider = camera; camera.unbindAll()
                val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                check(camera.hasCamera(selector)) { "Requested camera is unavailable. Try switching cameras." }
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                val still = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                camera.bindToLifecycle(this, selector, preview, still); capture = still
                view.previewStreamState.observe(this) { stream ->
                    if (generation == previewGeneration && !capturing && captured == null && !isFinishing) {
                        ready = stream == PreviewView.StreamState.STREAMING
                        status = if (ready) "${if (front) "Front" else "Rear"} camera active · preview only" else "Waiting for a camera frame…"
                    }
                }
            } catch (e: Exception) {
                ready = false; retry = true; status = e.message ?: "Camera unavailable or in use by another app."
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopPreview() {
        previewGeneration++
        boundPreview?.previewStreamState?.removeObservers(this)
        provider?.unbindAll(); ready = false
    }
    private fun clearTimeout() { timeout?.let(handler::removeCallbacks); timeout = null }
    private fun discardReview() { captured?.delete(); captured = null; image = null; retry = false }
    private fun retryPreview() { discardReview(); ready = false; revision++ }
    private fun takePhoto() {
        val useCase = capture ?: return
        if (!ready || capturing || locked()) return
        val directory = File(cacheDir, "jarvis-camera").apply { mkdirs() }
        directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000 }?.forEach { it.delete() }
        val file = runCatching { File.createTempFile("jarvis-camera-", ".jpg", directory) }.getOrElse {
            status = "Camera storage is unavailable. Free space or retry."; return
        }
        files += file
        val generation = ++captureGeneration
        captured = null; image = null; capturing = true; retry = false
        status = "Capturing a still… Close cancels this camera session."
        timeout = Runnable {
            if (generation == captureGeneration && capturing) {
                captureGeneration++; capturing = false; stopPreview(); retry = true; file.delete()
                status = "Capture timed out. No photo was accepted. Retry preview or close."
            }
        }.also { handler.postDelayed(it, 90_000) }
        try {
            useCase.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    if (generation != captureGeneration || isFinishing || isDestroyed) { file.delete(); return }
                    clearTimeout(); capturing = false; stopPreview(); retry = true
                    try {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.path, options)
                        check(file.length() > 0 && options.outWidth > 0 && options.outHeight > 0) { "Captured image header could not be decoded." }
                        val sample = BitmapFactory.Options().apply { inSampleSize = maxOf(1, maxOf(options.outWidth, options.outHeight) / 640) }
                        val bitmap = BitmapFactory.decodeFile(file.path, sample) ?: error("Captured image pixels could not be decoded.")
                        image = bitmap; captured = file
                        status = "Captured JPEG: ${options.outWidth} × ${options.outHeight}, ${file.length()} bytes. Temporary, not uploaded."
                    } catch (_: OutOfMemoryError) {
                        file.delete(); status = "Android could not allocate the image preview. Close other apps or retry."
                    } catch (e: Exception) {
                        file.delete(); status = e.message ?: "Capture could not be decoded; no success claimed."
                    }
                }
                override fun onError(error: ImageCaptureException) {
                    if (generation != captureGeneration) { file.delete(); return }
                    clearTimeout(); capturing = false; stopPreview(); retry = true; file.delete()
                    status = "Capture failed: ${error.message}"
                }
            })
        } catch (e: Exception) {
            clearTimeout(); captureGeneration++; capturing = false; stopPreview(); retry = true; file.delete()
            status = "Capture unavailable: ${e.message}"
        }
    }

    private fun usePhoto() {
        if (locked()) return
        val file = captured ?: return
        runCatching {
            val uri = FileProvider.getUriForFile(this, "$packageName.camera-files", file)
            runtime.addAttachments(listOf(uri)); runtime.selectModule(HudModule.ASSISTANT)
            retained = file
            finish()
        }.onFailure { status = "Could not attach this photo. No upload was made." }
    }
    override fun onResume() { super.onResume(); if (locked()) { finish(); return }; revision++ }
    private fun analyzePhoto(){
        val file=captured ?: return
        try {
            val uri=FileProvider.getUriForFile(this,"$packageName.camera-files",file)
            startActivity(android.content.Intent(this,VisionHudActivity::class.java).setData(uri).addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION))
            retained=file
        } catch(_:Exception){status="Image analysis is unavailable. Your captured still has not been uploaded."}
    }
    override fun onStop() { clearTimeout(); captureGeneration++; capturing = false; stopPreview(); super.onStop(); finish() }
    override fun onDestroy() { clearTimeout(); stopPreview(); files.filter { it != retained }.forEach { it.delete() }; runtime.endCamera(this); super.onDestroy() }
}
