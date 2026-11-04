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
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.jarvis.assistant.core.ConnectedFolders
import androidx.documentfile.provider.DocumentFile
import com.jarvis.assistant.ui.JarvisAppUi
import com.jarvis.assistant.ui.JarvisUiActions
import com.jarvis.assistant.core.choose

class MainActivity : ComponentActivity() {
    private val runtime get() = (application as JarvisApplication).runtime
    private var sentinelRequest by androidx.compose.runtime.mutableIntStateOf(0)
    private var wakeOpened=false
    private var touchedAfterWake=false
    private var settingsRequest by androidx.compose.runtime.mutableIntStateOf(0)
    private var micAfterGrant = false
    private var authentication: android.os.CancellationSignal? = null
    private var authenticatedRequest: String? = null
    private var lastInteraction = android.os.SystemClock.elapsedRealtime()
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted) runtime.hud.refreshWeather(true)
        else android.widget.Toast.makeText(this,"Location permission denied. Enter a weather city instead.",android.widget.Toast.LENGTH_LONG).show()
    }
    private fun approximateWeather() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED)runtime.hud.refreshWeather(true)
        else locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    private fun authenticate(request:com.jarvis.assistant.hud.AuthorizationRequest) {
        if(authenticatedRequest==request.id)return
        authenticatedRequest=request.id
        if(!getSystemService(android.app.KeyguardManager::class.java).isDeviceSecure){
            runtime.completeIdentity(request.id,false)
            AlertDialog.Builder(this).setTitle("Set a device screen lock")
                .setMessage("Codex requires Android's secure device credential or an enrolled biometric.")
                .setPositiveButton("Security settings"){_,_->openSettings(Intent(Settings.ACTION_SECURITY_SETTINGS))}.setNegativeButton("Cancel",null).show()
            return
        }
        if(runCatching{com.jarvis.assistant.security.SystemCredentialPrivacy.begin(this)}.isFailure){runtime.completeIdentity(request.id,false);return}
        val signal=android.os.CancellationSignal(); authentication=signal
        android.hardware.biometrics.BiometricPrompt.Builder(this).setTitle("Jarvis · Codex")
            .setSubtitle(request.action).setDescription("Authorize this action only. Your credential stays with Android.")
            .setDeviceCredentialAllowed(true).build().authenticate(signal,mainExecutor,object:android.hardware.biometrics.BiometricPrompt.AuthenticationCallback(){
                override fun onAuthenticationSucceeded(result:android.hardware.biometrics.BiometricPrompt.AuthenticationResult){authentication=null;runCatching{com.jarvis.assistant.security.SystemCredentialPrivacy.end(this@MainActivity)};runtime.completeIdentity(request.id,true)}
                override fun onAuthenticationError(code:Int,message:CharSequence){authentication=null;runCatching{com.jarvis.assistant.security.SystemCredentialPrivacy.end(this@MainActivity)};runtime.completeIdentity(request.id,false)}
            })
    }
    override fun onUserInteraction(){super.onUserInteraction();lastInteraction=android.os.SystemClock.elapsedRealtime();if(wakeOpened)touchedAfterWake=true}
    override fun onStart(){super.onStart();updateImmersive();visibleActivities.incrementAndGet(); runtime.hud.startVisible(this)}
    override fun onStop(){androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).show(androidx.core.view.WindowInsetsCompat.Type.systemBars());runtime.hud.stopVisible(this);visibleActivities.decrementAndGet();super.onStop()}
    override fun onDestroy(){authentication?.cancel();super.onDestroy()}
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

    private val folders by lazy { ConnectedFolders(this) }
    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) runCatching { folders.connect(uri); browseFolder(folders.openRoot(), emptyList()) }
            .onFailure { folderError(it) }
    }

    private fun folderError(error: Throwable) {
        AlertDialog.Builder(this).setTitle("Connected folder").setMessage(error.message ?: "Folder unavailable")
            .setPositiveButton("Close", null).show()
    }

    private fun folderMenu() {
        runtime.effects.play(com.jarvis.assistant.sentinel.SentinelSound.PANEL_OPEN)
        val choices = if (folders.root == null) arrayOf("Connect folder (read only)") else arrayOf("Browse connected folder", "Connect another folder", "Disconnect folder")
        AlertDialog.Builder(this).setTitle("Connected folder").setItems(choices) { _, index ->
            when (choices[index]) {
                "Browse connected folder" -> runCatching { browseFolder(folders.openRoot(), emptyList()) }.onFailure(::folderError)
                "Disconnect folder" -> { folders.disconnect(); runtime.clearAttachments() }
                else -> folderPicker.launch(null)
            }
        }.setNegativeButton("Close", null).show()
    }

    private fun browseFolder(directory: DocumentFile, parents: List<DocumentFile>) {
        lifecycleScope.launch {
            runCatching { withContext(Dispatchers.IO) { folders.children(directory) } }.onSuccess { children ->
                val labels = children.map { (if (it.isDirectory) "[Folder] " else "") + it.name.orEmpty() }.toTypedArray()
                AlertDialog.Builder(this@MainActivity).setTitle(directory.name ?: "Folder (first 100 entries)")
                    .setItems(labels) { _, index ->
                        val child = children[index]
                        if (child.isDirectory) browseFolder(child, parents + directory)
                        else runtime.addAttachments(listOf(child.uri))
                    }
                    .setNeutralButton(if (parents.isEmpty()) "Close" else "Up") { _, _ ->
                        if (parents.isNotEmpty()) browseFolder(parents.last(), parents.dropLast(1))
                    }.show()
            }.onFailure(::folderError)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(intent.getBooleanExtra("open_settings",false))settingsRequest++
        if(intent.getBooleanExtra("sentinel_settings",false))sentinelRequest++
        wakeOpened=intent.getBooleanExtra("sentinel_wake",false)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.rgb(8, 15, 25)),
        )
        onBackPressedDispatcher.addCallback(this,object:androidx.activity.OnBackPressedCallback(true){override fun handleOnBackPressed(){dismissHud()}})
        setContent {
            val state by runtime.state.collectAsStateWithLifecycle()
            androidx.compose.runtime.LaunchedEffect(state.module){updateImmersive()}
            androidx.compose.runtime.LaunchedEffect(state.authorization) {
                state.authorization?.let {
                    if(it.status==com.jarvis.assistant.hud.AuthorizationStatus.IDENTITY_CONFIRMATION)authenticate(it)
                    else { authentication?.cancel();authentication=null }
                }
            }
            androidx.compose.runtime.LaunchedEffect(state.hud.settings.autoDismissSeconds,state.phase,state.authorization) {
                val seconds=state.hud.settings.autoDismissSeconds
                if(seconds>0 && state.phase==com.jarvis.assistant.core.AssistantPhase.READY && state.pendingAction==null && state.authorization?.status !in listOf(com.jarvis.assistant.hud.AuthorizationStatus.AWAITING_CODEX,com.jarvis.assistant.hud.AuthorizationStatus.IDENTITY_CONFIRMATION)) {
                    lastInteraction=android.os.SystemClock.elapsedRealtime()
                    while(true){kotlinx.coroutines.delay(1000);if(lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) && android.os.SystemClock.elapsedRealtime()-lastInteraction>seconds*1000){finish();break}}
                }
            }
            androidx.compose.runtime.LaunchedEffect(state.sentinelVisual.mode,state.hud.settings.sentinel.returnOnTimeout){
                if(state.sentinelVisual.mode==com.jarvis.assistant.sentinel.SentinelMode.RETURNING_TO_WALLPAPER && wakeOpened && !touchedAfterWake && state.hud.settings.sentinel.returnOnTimeout){
                    kotlinx.coroutines.delay(if(state.hud.settings.reducedMotion)0 else 600)
                    if(!touchedAfterWake && runtime.state.value.phase==com.jarvis.assistant.core.AssistantPhase.READY)finish()
                }
            }
            JarvisAppUi(state, JarvisUiActions(
                onOpenPhantom={startActivity(Intent(this,com.jarvis.assistant.phantom.PhantomSettingsActivity::class.java))},
                onOpenTitan={page->startActivity(Intent(this,com.jarvis.assistant.titan.TitanCenterActivity::class.java).putExtra("page",page))},
                onOpenMemoryCards={startActivity(Intent(this,com.jarvis.assistant.titan.MemoryCardsActivity::class.java))},
                onOpenCamera={startActivity(Intent(this,com.jarvis.assistant.omnisystem.CameraHudActivity::class.java))},
                onOpenVision={startActivity(Intent(this,com.jarvis.assistant.omnisystem.VisionHudActivity::class.java))},
                onModule=runtime::selectModule,
                onOpenDeviceControls={
                    if(state.module==com.jarvis.assistant.omnisystem.HudModule.MUSIC)startActivity(Intent(this,com.jarvis.assistant.omnisystem.MusicHudActivity::class.java))
                    else if(state.module==com.jarvis.assistant.omnisystem.HudModule.LOCATION)startActivity(Intent(this,com.jarvis.assistant.omnisystem.LocationHudActivity::class.java))
                    else if(state.module==com.jarvis.assistant.omnisystem.HudModule.CLOCK)startActivity(Intent(this,com.jarvis.assistant.omnisystem.ClockHudActivity::class.java).putExtra("request",state.transcript))
                    else startActivity(Intent(this,com.jarvis.assistant.infinity.InfinityControlsActivity::class.java).putExtra("module",state.module.name))
                },
                onOpenMemo={runtime.dismissContactFollowUp();startActivity(Intent(this,com.jarvis.assistant.contacts.PersonalMemoActivity::class.java))},
                onDismissContactFollowUp=runtime::dismissContactFollowUp,
                onOpenContacts={startActivity(Intent(this,com.jarvis.assistant.contacts.ContactShortcutsActivity::class.java))},
                onOpenOutputVoice={startActivity(Intent(this,com.jarvis.assistant.voice.VoiceSettingsActivity::class.java))},
                onOpenScreenSecurity={startActivity(Intent(this,com.jarvis.assistant.security.SecuritySettingsActivity::class.java))},
                onOpenNeural={startActivity(Intent(this,com.jarvis.assistant.neural.NeuralSettingsActivity::class.java))},
                onInteraction = runtime.effects::userPlay,
                onPreviewWallpaper = ::previewWallpaper,
                onSaveHud = {runtime.effects.play(com.jarvis.assistant.sentinel.SentinelSound.BUTTON);runtime.saveHud(it)},
                onWeatherRefresh = { runtime.hud.refreshWeather() },
                onApproximateWeather = ::approximateWeather,
                onCodex = runtime::requestIdentity,
                onCancelAuthorization = { authentication?.cancel(); runtime.cancelAuthorization() },
                onDismiss = ::dismissHud,
                onSaveMemory = runtime::saveMemory,
                onCategory = runtime::setCategory,
                onSend = { runtime.effects.userPlay(com.jarvis.assistant.sentinel.SentinelSound.BUTTON);runtime.submit(it) },
                onMic = {
                    if(runtime.state.value.phase==com.jarvis.assistant.core.AssistantPhase.LISTENING)runtime.endListening()
                    else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) runtime.listen()
                    else { micAfterGrant = true; microphone.launch(Manifest.permission.RECORD_AUDIO) }
                },
                onStop = runtime::stopEverything,
                onLanguage = {runtime.effects.userPlay(com.jarvis.assistant.sentinel.SentinelSound.BUTTON);runtime.setLanguage(it)},
                onSaveSettings = { value, key ->
                    runtime.effects.play(com.jarvis.assistant.sentinel.SentinelSound.BUTTON)
                    val old=runtime.state.value.settings
                    if(old.onboardingComplete && (key.isNotBlank() || value.allowLockedChat!=old.allowLockedChat || value.shareSavedMemories!=old.shareSavedMemories || value.useCloudVoice!=old.useCloudVoice))runtime.protect("Change privacy or provider credentials") { runtime.saveSettings(value,key) }
                    else runtime.saveSettings(value,key)
                },
                onListGeminiModels = {key->runtime.effects.userPlay(com.jarvis.assistant.sentinel.SentinelSound.PROCESSING);runtime.listGeminiModels(key)},
                onTestProvider = {settings,key->runtime.effects.userPlay(com.jarvis.assistant.sentinel.SentinelSound.PROCESSING);runtime.testProvider(settings,key)},
                onPlayConnectionTest = runtime::playConnectionSample,
                onRememberDelete = { id -> runtime.protect("Delete saved memory") { runtime.deleteMemory(id) } },
                onClearConversation = { runtime.protect("Delete conversation history") { runtime.clearConversation() } },
                onClearScreen = runtime::clearScreenContext,
                onWipe = { runtime.protect("Delete all Jarvis data and models") { runtime.wipe() } },
                onStartWake = {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) microphone.launch(Manifest.permission.RECORD_AUDIO)
                    else runtime.startWake()
                },
                onStopWake = runtime::stopWake,
                onDownloadWake = runtime::downloadWake,
                onDownloadBrain = {startActivity(Intent(this,com.jarvis.assistant.neural.NeuralSettingsActivity::class.java))},
                onDownloadVoice = runtime::downloadVoice,
                onVoiceCheck = runtime::voiceCheck,
                onFolders = ::folderMenu,
                onSharePhotos = {runtime.effects.userPlay(com.jarvis.assistant.sentinel.SentinelSound.PANEL_OPEN);runtime.sharePhotos()},
                onUseLocal = {runtime.effects.userPlay(com.jarvis.assistant.sentinel.SentinelSound.BUTTON);runtime.useLocal()},
                onPickFiles = { runtime.effects.userPlay(com.jarvis.assistant.sentinel.SentinelSound.PANEL_OPEN);documents.launch(arrayOf("text/*", "application/pdf", "application/json")) },
                onPickPhotos = { runtime.effects.userPlay(com.jarvis.assistant.sentinel.SentinelSound.PANEL_OPEN);documents.launch(arrayOf("image/*")) },
                onClearAttachments = runtime::clearAttachments,
                onOpenPermission = ::openPermission,
                onConfirmAction = runtime::confirmAction,
                onCancelAction = runtime::cancelAction,
                onOpenSource = ::openSource,
            ), settingsRequest, sentinelRequest)
        }
    }

    override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent)
        if(intent.getBooleanExtra("open_settings",false)){settingsRequest++;wakeOpened=false;touchedAfterWake=true}
        if(intent.getBooleanExtra("sentinel_settings",false))sentinelRequest++
        if(intent.getBooleanExtra("sentinel_wake",false)){wakeOpened=true;touchedAfterWake=false}
        updateImmersive()
    }
    private fun updateImmersive(){
        val controller=androidx.core.view.WindowCompat.getInsetsController(window,window.decorView)
        if(wakeOpened || runtime.state.value.module!=com.jarvis.assistant.omnisystem.HudModule.ASSISTANT){controller.systemBarsBehavior=androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE;controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())}
        else controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
    }
    private fun dismissHud(){runtime.dismissHud();finish()}
    private fun previewWallpaper(){
        runtime.effects.play(com.jarvis.assistant.sentinel.SentinelSound.PANEL_OPEN)
        val component=android.content.ComponentName(this,com.jarvis.assistant.sentinel.JarvisWallpaperService::class.java)
        val preview=Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,component)
        try{startActivity(preview)}catch(_:ActivityNotFoundException){
            runCatching{startActivity(Intent(android.app.WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))}.onFailure{
                AlertDialog.Builder(this).setTitle("Wallpaper preview unavailable").setMessage("This Android build has no compatible live-wallpaper picker. You can still use the Jarvis assistant normally.").setPositiveButton("Close",null).show()
            }
        }
    }

    override fun onResume() { super.onResume(); runtime.refreshCapabilities() }

    private fun openPermission(name: String) {
        runtime.effects.play(if(name=="licenses")com.jarvis.assistant.sentinel.SentinelSound.PANEL_OPEN else com.jarvis.assistant.sentinel.SentinelSound.PERMISSION)
        when (name) {
            "licenses" -> showLicenses()
            "network" -> openSettings(Intent(Settings.ACTION_WIRELESS_SETTINGS))
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
            this.text = "$notices\n\n$license\n\n" + assets.open("legal/SENTINEL_SOUND_LICENSE.txt").bufferedReader().use{it.readText()} + "\n\n" + assets.open("legal/omnisystem-notices.txt").bufferedReader().use{it.readText()}
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
    }    companion object {
        private val visibleActivities=java.util.concurrent.atomic.AtomicInteger()
        val isVisible:Boolean get()=visibleActivities.get()>0
    }

}
