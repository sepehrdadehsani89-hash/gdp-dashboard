package com.jarvis.assistant.screen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.JarvisApplication
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import java.io.ByteArrayOutputStream

/** One frame under Android's screen-capture grant. Stops immediately after capture or failure. */
class ScreenCaptureService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var images: ImageReader? = null
    private val runtime get() = (application as JarvisApplication).runtime
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("jarvis_screen", "One-shot screen capture", NotificationManager.IMPORTANCE_LOW))
        val notification = NotificationCompat.Builder(this, "jarvis_screen").setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Jarvis · screen capture").setContentText("Capturing one frame, then stopping").setOngoing(true).build()
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(3102, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            else startForeground(3102, notification)
        } catch (_: Exception) { runtime.screenCaptureFailed("Android did not allow screen capture."); stopSelf(); return START_NOT_STICKY }
        scope.launch {
            try {
                check(runtime.screenCapturePending())
                @Suppress("DEPRECATION") val grant = intent?.getParcelableExtra<Intent>("grant") ?: error("No capture grant")
                val code = intent.getIntExtra("result", 0)
                projection = getSystemService(MediaProjectionManager::class.java).getMediaProjection(code, grant)
                projection?.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() { scope.cancel() }
                }, Handler(Looper.getMainLooper()))
                // Let the consent activity and system dialog disappear before taking the frame.
                delay(450)
                val bitmap = withTimeout(8_000) { capture() }
                val jpeg = try { withContext(Dispatchers.Default) {
                    ByteArrayOutputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output); output.toByteArray() }
                } } finally { bitmap.recycle() }
                require(jpeg.size <= 1_500_000)
                runtime.acceptScreenCapture(jpeg)
            } catch (_: CancellationException) {
                runtime.screenCaptureFailed("Screen capture stopped or timed out. No image was shared.")
            } catch (_: Exception) {
                runtime.screenCaptureFailed("Could not capture this screen. Unlock the phone, open a supported screen and try again.")
            } finally { stopSelf() }
        }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private suspend fun capture(): Bitmap = suspendCancellableCoroutine { continuation ->
        val metrics = DisplayMetrics()
        getSystemService(WindowManager::class.java).defaultDisplay.getRealMetrics(metrics)
        val scale = minOf(1.0, 1280.0 / maxOf(metrics.widthPixels, metrics.heightPixels))
        val width = (metrics.widthPixels * scale).toInt().coerceAtLeast(1)
        val height = (metrics.heightPixels * scale).toInt().coerceAtLeast(1)
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        images = reader
        reader.setOnImageAvailableListener({ source ->
            val image = runCatching { source.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
            image.use {
                if (!continuation.isActive) return@setOnImageAvailableListener
                try {
                    val plane = image.planes[0]
                    val paddedWidth = plane.rowStride / plane.pixelStride
                    val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
                    padded.copyPixelsFromBuffer(plane.buffer)
                    val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
                    if (cropped !== padded) padded.recycle()
                    reader.setOnImageAvailableListener(null, null)
                    continuation.resume(cropped, onCancellation = { _, value, _ -> value.recycle() })
                } catch (_: Exception) { continuation.cancel() }
            }
        }, Handler(Looper.getMainLooper()))
        display = projection?.createVirtualDisplay("JarvisOneFrame", width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.surface, null, null)
        if (display == null) continuation.cancel()
    }
    override fun onDestroy() {
        scope.cancel()
        display?.release(); display = null
        images?.close(); images = null
        projection?.stop(); projection = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
