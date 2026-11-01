package com.jarvis.assistant

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.assistant.ui.JarvisAppUi
import com.jarvis.assistant.ui.JarvisUiActions
import com.jarvis.assistant.core.choose

class MainActivity : ComponentActivity() {
    private val runtime get() = (application as JarvisApplication).runtime
    private var micAfterGrant = false
    private val microphone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        runtime.refreshCapabilities()
        if (granted && micAfterGrant) runtime.listen()
        micAfterGrant = false
    }
    private val optionalPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { runtime.refreshCapabilities() }
    private val documents = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri -> runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        runtime.addAttachments(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.rgb(8, 15, 25)),
        )
        setContent {
            val state by runtime.state.collectAsStateWithLifecycle()
            JarvisAppUi(state, JarvisUiActions(
                onSend = { runtime.submit(it) },
                onMic = {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) runtime.listen()
                    else { micAfterGrant = true; microphone.launch(Manifest.permission.RECORD_AUDIO) }
                },
                onStop = runtime::stopEverything,
                onLanguage = runtime::setLanguage,
                onSaveSettings = runtime::saveSettings,
                onListGeminiModels = runtime::listGeminiModels,
                onTestProvider = runtime::testProvider,
                onPlayConnectionTest = runtime::playConnectionSample,
                onRememberDelete = runtime::deleteMemory,
                onClearConversation = runtime::clearConversation,
                onClearScreen = runtime::clearScreenContext,
                onWipe = runtime::wipe,
                onStartWake = {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) microphone.launch(Manifest.permission.RECORD_AUDIO)
                    else runtime.startWake()
                },
                onStopWake = runtime::stopWake,
                onDownloadWake = runtime::downloadWake,
                onDownloadBrain = runtime::downloadBrain,
                onDownloadVoice = runtime::downloadVoice,
                onVoiceCheck = runtime::voiceCheck,
                onPickFiles = { documents.launch(arrayOf("text/*", "application/pdf", "application/json")) },
                onPickPhotos = { documents.launch(arrayOf("image/*")) },
                onClearAttachments = runtime::clearAttachments,
                onOpenPermission = ::openPermission,
                onConfirmAction = runtime::confirmAction,
                onCancelAction = runtime::cancelAction,
                onOpenSource = ::openSource,
            ))
        }
    }

    override fun onResume() { super.onResume(); runtime.refreshCapabilities() }

    private fun openPermission(name: String) {
        when (name) {
            "licenses" -> showLicenses()
            "microphone" -> microphone.launch(Manifest.permission.RECORD_AUDIO)
            "notifications" -> if (Build.VERSION.SDK_INT >= 33) optionalPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            "notification_access" -> openSettings(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            "accessibility" -> openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            "battery" -> openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            "location" -> optionalPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            "contacts" -> optionalPermission.launch(Manifest.permission.READ_CONTACTS)
            "calendar" -> optionalPermission.launch(Manifest.permission.READ_CALENDAR)
            "voices" -> openSettings(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
            else -> openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun showLicenses() {
        val language = runtime.state.value.settings.language
        val notices = assets.open("legal/THIRD_PARTY_NOTICES.md").bufferedReader().use { it.readText() }
        val license = assets.open("legal/LICENSE").bufferedReader().use { it.readText() }
        val text = TextView(this).apply {
            this.text = "$notices\n\n$license"
            textSize = 13f
            setPadding(32, 24, 32, 24)
            setTextIsSelectable(true)
        }
        AlertDialog.Builder(this)
            .setTitle(language.choose("Open-source licenses", "Licencias de código abierto", "مجوزهای متن‌باز"))
            .setView(ScrollView(this).apply { addView(text) })
            .setPositiveButton(language.choose("Close", "Cerrar", "بستن"), null)
            .show()
    }

    private fun openSettings(intent: Intent) {
        try { startActivity(intent) }
        catch (_: ActivityNotFoundException) {
            runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
        }
    }

    private fun openSource(url: String) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        if (uri.scheme != "https" || uri.host.isNullOrBlank() || uri.userInfo != null) return
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}
