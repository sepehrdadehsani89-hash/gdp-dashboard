package com.jarvis.assistant.screen

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.jarvis.assistant.JarvisApplication
import com.jarvis.assistant.core.choose

/** Visible, per-capture consent. A projection token is never persisted or reused. */
class ScreenCaptureActivity : ComponentActivity() {
    private val runtime get() = (application as JarvisApplication).runtime
    private val permission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null && runtime.screenCapturePending()) {
            try {
                ContextCompat.startForegroundService(this, Intent(this, ScreenCaptureService::class.java)
                    .putExtra("grant", data).putExtra("result", result.resultCode))
            } catch (_: Exception) { runtime.screenCaptureFailed("Android could not start screen capture. Open Jarvis and try again.") }
        } else runtime.screenCaptureFailed(runtime.state.value.settings.language.choose("Screen capture cancelled. Nothing was shared.", "Captura cancelada. No se compartió nada.", "گرفتن تصویر لغو شد. چیزی به اشتراک گذاشته نشد."))
        finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!runtime.screenCapturePending()) { finish(); return }
        if (savedInstanceState != null) return
        val l = runtime.state.value.settings.language
        val cloud = runtime.state.value.settings.provider != "local"
        AlertDialog.Builder(this)
            .setTitle(l.choose("Capture this screen?", "¿Capturar esta pantalla?", "از این صفحه تصویر بگیریم؟"))
            .setMessage(l.choose(
                "Jarvis captures one image, not a recording. It stays in memory for up to two minutes. " + if (cloud) "Your follow-up screen questions send the captured image to Gemini, or visible text to another selected AI provider. Avoid screens containing passwords or private codes." else "With the local provider, only readable screen text is analyzed on the phone. Visual analysis needs Gemini.",
                "Jarvis captura una imagen, no un vídeo, y la conserva en memoria hasta dos minutos. " + if (cloud) "Tus preguntas sobre la pantalla envían la imagen a Gemini o el texto visible a otro proveedor elegido. Evita contraseñas y códigos privados." else "El proveedor local analiza solo texto visible. El análisis visual requiere Gemini.",
                "جارویس یک تصویر می‌گیرد، نه فیلم؛ حداکثر دو دقیقه در حافظه می‌ماند. " + if (cloud) "پرسش‌های بعدی دربارهٔ صفحه، تصویر را به Gemini یا متن قابل‌مشاهده را به سرویس انتخابی دیگر می‌فرستند. صفحه‌های دارای رمز یا کد خصوصی را انتخاب نکنید." else "مدل محلی فقط متن قابل‌خواندن صفحه را بررسی می‌کند. تحلیل تصویری به Gemini نیاز دارد.",
            ))
            .setPositiveButton(l.choose("Capture", "Capturar", "گرفتن تصویر")) { _, _ ->
                runtime.authorizeScreenCloud(cloud)
                permission.launch(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent())
            }
            .setNegativeButton(l.choose("Cancel", "Cancelar", "لغو")) { _, _ -> runtime.screenCaptureFailed(l.choose("Screen capture cancelled.", "Captura cancelada.", "گرفتن تصویر لغو شد.")); finish() }
            .setOnCancelListener { runtime.screenCaptureFailed(l.choose("Screen capture cancelled.", "Captura cancelada.", "گرفتن تصویر لغو شد.")); finish() }
            .show()
    }
}
