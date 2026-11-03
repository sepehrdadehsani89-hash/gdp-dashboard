package com.jarvis.assistant.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantPhase
import com.jarvis.assistant.core.AssistantSettings
import com.jarvis.assistant.core.ChatMessage
import com.jarvis.assistant.core.MessageRole
import com.jarvis.assistant.core.choose
import com.jarvis.assistant.core.ConnectionCheck
import com.jarvis.assistant.data.ProviderException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Ink = Color(0xFF000000)
private val Panel = Color(0xFF050C14)
private val PanelLight = Color(0xFF081923)
private val Mint = Color(0xFF00E5FF)
private val Muted = Color(0xFF91A7BB)
private val Lavender = Color(0xFFAA7CFF)
private val Warm = Color(0xFFF0C8A9)
private val Line = Color(0xFF263A50)

@Composable
fun JarvisAppUi(state: JarvisUiState, actions: JarvisUiActions, settingsRequest:Int=0, sentinelRequest:Int=0) {
    val language = state.settings.language
    CompositionLocalProvider(LocalLayoutDirection provides if (language == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = when(state.hud.settings.accent){"purple"->Lavender;"blue"->Color(0xFF38BDF8);else->Mint}, onPrimary = Ink, secondary = Lavender, background = Ink,
                surface = Panel, onSurface = Color(0xFFF2F5FA), onSurfaceVariant = Muted,
                surfaceVariant = PanelLight, outline = Line, error = Color(0xFFFFB4B4),
            ),
            typography = Typography(),
        ) {
            Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
                if (!state.settings.onboardingComplete) Onboarding(state, actions)
                else MainShell(state, actions, settingsRequest, sentinelRequest)
                state.authorization?.takeIf { it.status in listOf(com.jarvis.assistant.hud.AuthorizationStatus.AUTHORIZATION_REQUIRED,com.jarvis.assistant.hud.AuthorizationStatus.AWAITING_CODEX) }?.let { request ->
                    AlertDialog(onDismissRequest=actions.onCancelAuthorization,
                        title={Text("Codex · authorization required")},
                        text={Text("${request.action}\n\nVerify using Android's device credential or biometric. This approval is for one action and expires after 90 seconds.")},
                        confirmButton={Button(onClick=actions.onCodex){Text("Verify identity")}},
                        dismissButton={TextButton(onClick=actions.onCancelAuthorization){Text("Cancel")}})
                }
                state.pendingAction?.let { action ->
                    AlertDialog(
                        onDismissRequest = actions.onCancelAction,
                        icon = { Icon(Icons.Rounded.Shield, contentDescription = null, tint = Mint) },
                        title = { Text(language.choose("Your approval", "Tu autorización", "تأیید شما")) },
                        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(action.description)
                            Text(language.choose("Review the details. Approval is for this action only.", "Revisa los detalles. La autorización es solo para esta acción.", "جزئیات را بررسی کنید. این تأیید فقط برای همین کار است."), color = Muted)
                        } },
                        confirmButton = { Button(onClick = { actions.onConfirmAction(action.id) }) { Text(language.choose("Confirm", "Confirmar", "تأیید")) } },
                        dismissButton = { TextButton(onClick = actions.onCancelAction) { Text(language.choose("Cancel", "Cancelar", "لغو")) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun MainShell(state: JarvisUiState, actions: JarvisUiActions, settingsRequest:Int, sentinelRequest:Int) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(settingsRequest){if(settingsRequest>0)tab=2}
    LaunchedEffect(state.moduleSequence){if(state.moduleSequence>0)tab=0}
    val l = state.settings.language
    val labels = listOf(l.choose("Assistant", "Asistente", "دستیار"), l.choose("Memory", "Memoria", "حافظه"), l.choose("Settings", "Ajustes", "تنظیمات"))
    val icons = listOf(Icons.Rounded.Forum, Icons.Rounded.Memory, Icons.Rounded.Settings)
    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Ink, tonalElevation = 0.dp) {
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(selected = tab == index, onClick = { actions.onInteraction(com.jarvis.assistant.sentinel.SentinelSound.TAB); tab = index },
                        icon = { Icon(icons[index], contentDescription = null) }, label = { Text(label) })
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 820.dp).fillMaxSize()) {
                when (tab) {
                    0 -> Column {
                        if(state.contactFollowUp)Column(Modifier.padding(12.dp)){
                            Text(l.choose("After the dialer request: call outcome is unknown. Would you like to record a new personal memo?","Después de solicitar el marcador: resultado desconocido. ¿Quieres grabar una nota personal nueva?","پس از درخواست شماره‌گیر، نتیجهٔ تماس نامشخص است. پیام شخصی جدیدی ضبط می‌کنید؟"))
                            Row{TextButton(onClick=actions.onOpenMemo){Text(l.choose("Review personal memo","Revisar nota personal","بررسی پیام شخصی"))};TextButton(onClick=actions.onDismissContactFollowUp){Text(l.choose("No thanks","No gracias","نه ممنون"))}}
                        }
                        com.jarvis.assistant.omnisystem.ModuleTabs(state.module,l) { module -> actions.onModule(module) }
                        if(state.module==com.jarvis.assistant.omnisystem.HudModule.ASSISTANT)state.speakerProfileName?.let{name->Text(state.settings.language.choose("Possible voice profile: $name · not authentication","Perfil de voz posible: $name · no autentica","نمایهٔ صوتی احتمالی: $name · نه احراز هویت"),Modifier.padding(horizontal=16.dp))}
                        androidx.compose.animation.Crossfade(targetState=state.module,
                            animationSpec=androidx.compose.animation.core.tween(if(state.hud.settings.reducedMotion)0 else 160),label="module") { module ->
                            if(module==com.jarvis.assistant.omnisystem.HudModule.ASSISTANT) Home(state, actions) { tab = 2 }
                            else com.jarvis.assistant.omnisystem.ModuleHud(state.copy(module=module), actions)
                        }
                    }
                    1 -> MemoryScreen(state, actions)
                    else -> SettingsScreen(state, actions, sentinelRequest)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Home(state: JarvisUiState, actions: JarvisUiActions, onOpenSettings: () -> Unit) {
    val l = state.settings.language
    var input by rememberSaveable { mutableStateOf("") }
    var previewScreen by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val sendInput = {
        if (input.isNotBlank()) {
            val question = input.trim()
            input = ""
            keyboard?.hide()
            focus.clearFocus()
            actions.onSend(question)
        }
    }
    val scroll = rememberLazyListState()
    val busy = state.phase in setOf(AssistantPhase.LISTENING, AssistantPhase.THINKING, AssistantPhase.SPEAKING)
    LaunchedEffect(state.messages.size, state.phase, state.transcript.isNotBlank(),state.voiceInteraction) {
        if (state.messages.isNotEmpty() && !state.voiceInteraction) {
            val target = state.messages.size + if (state.transcript.isNotBlank() && busy) 1 else 0
            scroll.animateScrollToItem(target)
        }
    }
    LaunchedEffect(state.sentinelVisual.activationId){if(state.sentinelVisual.activationId>0)scroll.animateScrollToItem(0)}
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("JARVIS · HUD",color=Mint,fontSize=9.sp,letterSpacing=2.sp)
            Spacer(Modifier.weight(1f))
            Surface(shape = CircleShape, color = PanelLight) {
                Text(l.label, Modifier.padding(horizontal = 13.dp, vertical = 8.dp), color = Muted, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = actions.onStop) { Icon(Icons.Rounded.Stop, l.choose("Stop everything", "Detener todo", "توقف همه‌چیز"), tint = Warm) }
        }
        if (state.screenReady) Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(l.choose("Screen snapshot ready · expires in 2 min", "Captura lista · caduca en 2 min", "تصویر صفحه آماده است · اعتبار ۲ دقیقه"), modifier = Modifier.weight(1f), color = Mint, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { previewScreen = true }) { Text(l.choose("View", "Ver", "دیدن")) }
            TextButton(onClick = actions.onClearScreen) { Text(l.choose("Clear screen", "Borrar captura", "پاک کردن تصویر")) }
        }
        LazyColumn(state = scroll, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                HudDashboard(state, actions, onOpenSettings)
                Text(if (state.settings.name.isBlank()) l.choose("At your service.","A tu servicio.","در خدمت شما هستم.") else "${state.settings.name} · JARVIS", color=Muted)

            }
            if (state.messages.isEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(l.choose("TRY A LITTLE MAGIC", "PRUEBA ALGO NUEVO", "از اینجا شروع کنید"), color = Muted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp)
                    Suggestion(l.choose("Set up your voice", "Configura tu voz", "صدای خود را آماده کنید"), l.choose("Download speech models and test the voice", "Descarga modelos y prueba la voz", "مدل گفتار را دانلود و صدا را آزمایش کنید"), Icons.Rounded.Headphones, onOpenSettings)
                    Suggestion(l.choose("Yesterday’s headlines", "Las noticias de ayer", "خبرهای دیروز"), l.choose("Catch up with the world", "Ponte al día", "از جهان باخبر شوید"), Icons.Rounded.NotificationsNone) {
                        actions.onSend(l.choose("Give me yesterday's news", "Dame las noticias de ayer", "خبرهای دیروز را بگو"))
                    }
                    Suggestion(l.choose("Open YouTube", "Abrir YouTube", "یوتیوب را باز کن"), l.choose("Find something worth watching", "Encuentra algo para ver", "چیزی برای تماشا پیدا کنید"), Icons.Rounded.GraphicEq) {
                        actions.onSend(l.choose("Open YouTube", "Abre YouTube", "یوتیوب را باز کن"))
                    }
                    Suggestion(l.choose("Remember something", "Recordar algo", "چیزی را به خاطر بسپار"), l.choose("Your notes, close at hand", "Tus notas, siempre a mano", "یادداشت‌های شما، همیشه در دسترس"), Icons.Rounded.Memory) {
                        input = l.choose("Remember that ", "Recuerda que ", "یادت بماند که ")
                    }
                }
            }
            items(state.messages, key = { it.id }) { message -> MessageCard(message, l, actions) }
            if (state.transcript.isNotBlank()) item {
                Text(state.transcript, modifier = Modifier.fillMaxWidth().padding(12.dp), color = Mint, style = MaterialTheme.typography.bodyLarge)
            }
            state.error?.takeIf { it.isNotBlank() }?.let { error -> item {
                Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF351F2C)) {
                    Text(error, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                }
            } }
            item {
                Text("Jarvis · separate installation", color = Mint, style = MaterialTheme.typography.labelSmall)
                Text("Live controls: enable Screen access, start wake listening, then say open [app], tap [exact label], or type [draft]. Confirm taps/drafts by voice on the same screen. Send actions stay manual.", color = Muted, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = actions.onUseLocal) { Text("Use free on-device AI") }
                Text(l.choose("Choose Lite or the optional near-1 GB brain in Settings → Neural Core. Exact size is shown before download. Local AI does not reset Google quota.","Elige Lite o el cerebro opcional de casi 1 GB en Ajustes → Neural Core. Verás el tamaño exacto antes de descargar. La IA local no reinicia la cuota de Google.","در تنظیمات ← هستهٔ عصبی، مدل سبک یا مغز اختیاری نزدیک ۱ گیگابایت را انتخاب کنید. اندازهٔ دقیق پیش از دانلود نمایش داده می‌شود. هوش محلی سهمیهٔ گوگل را بازنشانی نمی‌کند."), color = Muted, style = MaterialTheme.typography.bodySmall)
                if (state.attachedFiles.isNotEmpty()) TextButton(onClick = actions.onSharePhotos) { Text("Share selected photos…") }
            }
            if (state.attachedFiles.isNotEmpty()) item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.attachedFiles.forEach { name -> AssistChip(onClick = {}, label = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) }, leadingIcon = { Icon(Icons.Rounded.AttachFile, null, Modifier.size(16.dp)) }) }
                    AssistChip(onClick = actions.onClearAttachments, label = { Text(l.choose("Clear", "Quitar", "حذف")) }, leadingIcon = { Icon(Icons.Rounded.Close, null, Modifier.size(16.dp)) })
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        Column(Modifier.fillMaxWidth().background(Ink).padding(horizontal = 18.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = actions.onFolders) { Text("Folders") }
                IconButton(onClick = actions.onPickFiles) { Icon(Icons.Rounded.AttachFile, l.choose("Choose a document", "Elegir un archivo", "انتخاب فایل"), tint = Muted) }
                IconButton(onClick = actions.onPickPhotos) { Icon(Icons.Rounded.Image, l.choose("Choose a photo", "Elegir una foto", "انتخاب عکس"), tint = Muted) }
                Spacer(Modifier.weight(1f))
                Text(l.choose("YOUR CONTROL. ALWAYS.", "SIEMPRE BAJO TU CONTROL.", "همیشه در اختیار شما"), color = Muted, style = MaterialTheme.typography.labelSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = input, onValueChange = { input = it },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp),
                    placeholder = { Text(l.choose("Ask Jarvis…", "Pregúntale a Jarvis…", "از جارویس بپرسید…")) },
                    maxLines = 4, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendInput() }),
                    trailingIcon = { if (input.isNotBlank()) IconButton(onClick = sendInput) { Icon(Icons.AutoMirrored.Rounded.Send, l.choose("Send", "Enviar", "ارسال"), tint = Mint) } },
                )
                Button(onClick = actions.onMic,
                    shape = CircleShape, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (busy) Warm else Mint)) {
                    Icon(if (state.phase==AssistantPhase.LISTENING) Icons.Rounded.Stop else Icons.Rounded.Mic, l.choose(if (state.phase==AssistantPhase.SPEAKING) "Interrupt and speak" else if (busy) "Stop" else "Speak", if (state.phase==AssistantPhase.SPEAKING) "Interrumpir y hablar" else if (busy) "Detener" else "Hablar", if (state.phase==AssistantPhase.SPEAKING) "قطع پاسخ و صحبت" else if (busy) "توقف" else "صحبت کنید"), Modifier.size(25.dp))
                }
            }
        }
    }
    if (previewScreen && state.screenImage != null) {
        val bytes = state.screenImage
        val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
        DisposableEffect(bitmap) { onDispose { bitmap?.recycle() } }
        AlertDialog(onDismissRequest = { previewScreen = false }, title = { Text(l.choose("Captured screen", "Pantalla capturada", "تصویر گرفته‌شده")) },
            text = { if (bitmap != null) Image(bitmap.asImageBitmap(), l.choose("Your captured screen", "Tu captura", "تصویر صفحهٔ شما"), Modifier.fillMaxWidth().height(320.dp)) },
            confirmButton = { TextButton(onClick = { previewScreen = false }) { Text(l.choose("Close", "Cerrar", "بستن")) } })
    }
}

@Composable
private fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(29.dp).border(1.dp, Mint.copy(alpha = .6f), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.GraphicEq, null, Modifier.size(18.dp), tint = Mint)
        }
        Text("JARVIS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp)
    }
}


@Composable
private fun Suggestion(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            Icon(icon, null, Modifier.size(23.dp), tint = Mint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp), tint = Muted)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageCard(message: ChatMessage, l: AppLanguage, actions: JarvisUiActions) {
    val fromUser = message.role == MessageRole.USER
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start) {
        Text(if (fromUser) l.choose("YOU", "TÚ", "شما") else "JARVIS", color = if (fromUser) Muted else Mint, modifier = Modifier.padding(horizontal = 4.dp, vertical = 7.dp), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp)
        Surface(shape = RoundedCornerShape(20.dp), color = if (fromUser) PanelLight else Panel, modifier = Modifier.widthIn(max = 640.dp)) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(message.text, style = MaterialTheme.typography.bodyLarge, lineHeight = 25.sp)
                if (message.sources.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        message.sources.forEach { source ->
                            AssistChip(onClick = { actions.onOpenSource(source.url) },
                                label = { Text(source.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 220.dp)) },
                                trailingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, l.choose("Open source", "Abrir fuente", "باز کردن منبع"), Modifier.size(14.dp)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryScreen(state: JarvisUiState, actions: JarvisUiActions) {
    val l = state.settings.language
    var deleteId by remember { mutableStateOf<Long?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    val visible = state.memories.filter { it.text.contains(query,true) && (category=="All" || state.memoryCategories[it.id]==category) }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageTitle(l.choose("A place for what matters.", "Lo importante, contigo.", "جایی برای چیزهای مهم."), l.choose("MEMORY", "MEMORIA", "حافظه")) }
        item { Text(l.choose("Say “remember that…” to save a note. Your memories stay on this phone. You can remove them whenever you like.", "Di “recuerda que…” para guardar una nota. Tus recuerdos se guardan en este teléfono y puedes borrarlos cuando quieras.", "برای ذخیرهٔ یادداشت بگویید «یادت بماند که…». یادداشت‌ها روی همین گوشی می‌مانند و هر زمان بخواهید می‌توانید حذفشان کنید."), color = Muted, lineHeight = 23.sp) }
        item { OutlinedTextField(query,{query=it},label={Text(l.choose("Search memory","Buscar memoria","جست‌وجوی حافظه"))},modifier=Modifier.fillMaxWidth()) }
        item { Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("All","Notes","Personal","Work","Tasks").forEach { c->FilterChip(category==c,{category=c},label={Text(c)}) } } }
        item { SectionCard {
            OutlinedTextField(note,{note=it},label={Text(l.choose("New memory","Nueva nota","یادداشت جدید"))},modifier=Modifier.fillMaxWidth())
            Button(onClick={actions.onSaveMemory(note,if(category=="All")"Notes" else category);note=""},enabled=note.isNotBlank()){Text(l.choose("Save memory","Guardar nota","ذخیرهٔ یادداشت"))}
            Text("Stored on this phone. Cloud sharing follows your explicit memory-sharing setting.",color=Muted,style=MaterialTheme.typography.bodySmall)
        } }
        item { if(state.authorization!=null)AuthorizationPanel(state,actions) }
        if (state.memories.isEmpty()) item {
            SectionCard { Icon(Icons.Rounded.Memory, null, Modifier.size(32.dp), tint = Mint); Text(l.choose("Room for your first thought.", "Un lugar para tu primera idea.", "منتظر اولین یادداشت شما هستم."), style = MaterialTheme.typography.titleMedium); Text(l.choose("Nothing is saved here without your request.", "Nada se guarda sin que tú lo pidas.", "بدون درخواست شما چیزی ذخیره نمی‌شود."), color = Muted) }
        }
        items(visible, key = { it.id }) { memory ->
            SectionCard {
                Text(state.memoryCategories[memory.id] ?: "Notes",color=Mint,style=MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(memory.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { val options=listOf("Notes","Personal","Work","Tasks");val current=options.indexOf(state.memoryCategories[memory.id]);actions.onCategory(memory.id,options[(current+1)%options.size]) }) { Text("Category",fontSize=10.sp) }
                    IconButton(onClick = { deleteId = memory.id }) { Icon(Icons.Rounded.DeleteOutline, l.choose("Delete memory", "Borrar recuerdo", "حذف یادداشت"), tint = Muted) }
                }
            }
        }
    }
    deleteId?.let { id ->
        AlertDialog(onDismissRequest = { deleteId = null },
            title = { Text(l.choose("Delete this memory?", "¿Borrar este recuerdo?", "این یادداشت حذف شود؟")) },
            text = { Text(state.memories.firstOrNull { it.id == id }?.text.orEmpty()) },
            confirmButton = { TextButton(onClick = { actions.onRememberDelete(id); deleteId = null }) { Text(l.choose("Delete", "Borrar", "حذف")) } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text(l.choose("Cancel", "Cancelar", "لغو")) } })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Onboarding(state: JarvisUiState, actions: JarvisUiActions) {
    val l = state.settings.language
    var name by rememberSaveable { mutableStateOf(state.settings.name) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            BrandMark()
            HudCore(state, Modifier.size(220.dp).align(Alignment.CenterHorizontally))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(l.choose("Meet your\nquiet advantage.", "Tu nueva\nventaja personal.", "دستیار شما،\nهمیشه در کنارتان."), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Light, lineHeight = 42.sp)
                Text(l.choose("A voice for your questions. A home for your thoughts. Built around your control.", "Una voz para tus preguntas. Un lugar para tus ideas. Tú tienes el control.", "صدایی برای پرسش‌هایتان، جایی برای یادداشت‌هایتان؛ با اختیار کامل شما."), color = Muted, lineHeight = 24.sp)
            }
            SectionCard {
                Text(l.choose("Make yourself at home", "Hazlo tuyo", "از همین‌جا شروع کنیم"), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AppLanguage.entries.forEach { lang -> FilterChip(selected = lang == l, onClick = { actions.onLanguage(lang) }, label = { Text(lang.label) }) } }
                OutlinedTextField(name, { name = it }, label = { Text(l.choose("Your name (optional)", "Tu nombre (opcional)", "نام شما (اختیاری)")) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp))
            }
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Mic, null, tint = Mint)
                    Text(l.choose("Start with your voice", "Empieza con tu voz", "با صدای خود شروع کنید"), style = MaterialTheme.typography.titleMedium)
                }
                Text(l.choose("Microphone access lets Jarvis hear a request when you tap the mic. Offline recognition also needs the speech model below. Background listening is optional.", "El micrófono permite escuchar cuando lo tocas. El reconocimiento local también requiere el modelo de abajo. Escuchar en segundo plano es opcional.", "دسترسی به میکروفن برای شنیدن درخواست پس از لمس دکمه است. تشخیص گفتار آفلاین به مدل زیر هم نیاز دارد. شنیدن در پس‌زمینه اختیاری است."), color = Muted)
                OutlinedButton(onClick = { actions.onOpenPermission("microphone") }) { Icon(if (state.hasMicrophone) Icons.Rounded.Check else Icons.Rounded.Mic, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(l.choose(if (state.hasMicrophone) "Microphone ready" else "Allow microphone", if (state.hasMicrophone) "Micrófono listo" else "Permitir micrófono", if (state.hasMicrophone) "میکروفن آماده است" else "اجازهٔ میکروفن")) }
            }
            SectionCard {
                Text(l.choose("Prepare offline speech", "Prepara la voz local", "آماده‌سازی گفتار آفلاین"), style = MaterialTheme.typography.titleMedium)
                Text(l.choose("Use Wi-Fi to download a speech model for this language, about 40–55 MB. Then test the voice; if the phone has no suitable voice, download the optional offline voice. You can also do this later in Settings and start with typing now.", "Con Wi-Fi, descarga el modelo del idioma (40–55 MB). Después prueba la voz; si falta una adecuada, descarga la voz local opcional. También puedes hacerlo luego en Ajustes y empezar escribiendo.", "با وای‌فای، مدل گفتار این زبان را با حجم حدود ۴۰ تا ۵۵ مگابایت دانلود کنید. سپس صدا را آزمایش کنید؛ اگر گوشی صدای مناسب ندارد، صدای آفلاین اختیاری را دانلود کنید. می‌توانید فعلاً تایپ کنید و بعداً از تنظیمات این کار را انجام دهید."), color = Muted, style = MaterialTheme.typography.bodySmall)
                if (state.wakeStatus.isNotBlank()) Text(state.wakeStatus, color = Warm)
                state.modelProgress?.let { LinearProgressIndicator(progress = { it.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth()) }
                OutlinedButton(onClick = actions.onDownloadWake, enabled = state.modelProgress == null, modifier = Modifier.fillMaxWidth()) { Text(l.choose("Download ${l.label} speech model", "Descargar modelo: ${l.label}", "دانلود مدل گفتار ${l.label}")) }
                if (state.voiceStatus.isNotBlank()) Text(state.voiceStatus, color = Warm)
                state.voiceProgress?.let { LinearProgressIndicator(progress = { it.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth()) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = actions.onVoiceCheck, modifier = Modifier.weight(1f)) { Text(l.choose("Test voice", "Probar voz", "آزمایش صدا")) }
                    OutlinedButton(onClick = actions.onDownloadVoice, enabled = state.voiceProgress == null, modifier = Modifier.weight(1f)) { Text(l.choose("Get offline voice", "Descargar voz", "دانلود صدا")) }
                }
            }
            state.error?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp), tint = Muted)
                Text(l.choose("Start without an API key: news, app shortcuts, and memories are ready. An optional on-device model adds private conversations after a download. You choose which files and photos to share.", "Empieza sin clave API: noticias, atajos y recuerdos están listos. Un modelo opcional añade conversaciones locales tras descargarlo. Tú eliges qué archivos y fotos compartir.", "بدون کلید API شروع کنید: خبرها، میان‌بر برنامه‌ها و یادداشت‌ها آماده‌اند. با دانلود مدل اختیاری روی گوشی، گفت‌وگوی خصوصی هم اضافه می‌شود. انتخاب فایل و عکس با شماست."), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { actions.onSaveSettings(state.settings.copy(name = name.trim(), onboardingComplete = true), "") }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
                Text(l.choose("Meet Jarvis", "Conocer a Jarvis", "شروع با جارویس"), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(12.dp)); Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PageTitle(title: String, eyebrow: String) {
    Column(Modifier.padding(top = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(eyebrow, color = Mint, style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp), content = content)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }
        Switch(checked, onChecked)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(state: JarvisUiState, actions: JarvisUiActions, sentinelRequest:Int=0) {
    var sentinel by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(sentinelRequest){if(sentinelRequest>0)sentinel=true}
    androidx.activity.compose.BackHandler(enabled=sentinel){sentinel=false}
    if(sentinel){SentinelSettingsUi(state,actions){sentinel=false};return}
    val l = state.settings.language
    var draft by remember(state.settings) { mutableStateOf(state.settings) }
    var key by remember { mutableStateOf("") }
    var showKey by rememberSaveable { mutableStateOf(false) }
    var showWipe by rememberSaveable { mutableStateOf(false) }
    var showClearConversation by rememberSaveable { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val connectionScope = rememberCoroutineScope()
    var connectionJob by remember { mutableStateOf<Job?>(null) }
    var checking by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("") }
    var availableModels by remember { mutableStateOf(emptyList<String>()) }
    var connectionGeneration by remember { mutableIntStateOf(0) }
    var connectionDialog by remember { mutableStateOf<String?>(null) }
    var verifiedConnection by remember { mutableStateOf<ConnectionCheck?>(null) }
    var connectionElapsed by remember { mutableIntStateOf(0) }
    fun clearConnectionCheck(clearModels: Boolean = true) {
        connectionGeneration++
        connectionJob?.cancel()
        checking = false
        connectionStatus = ""
        connectionDialog = null
        verifiedConnection = null
        if (clearModels) availableModels = emptyList()
    }
    fun checkConnection(listModels: Boolean) {
        clearConnectionCheck(clearModels = listModels)
        val selected = draft
        val enteredKey = key
        val generation = connectionGeneration
        checking = true
        connectionJob = connectionScope.launch {
            try {
                if (listModels) {
                    availableModels = withTimeout(15_000) { actions.onListGeminiModels(enteredKey) }
                    connectionStatus = l.choose("Choose a model below, then test the connection. Live-only models use a different API and are not listed.", "Elige un modelo y prueba la conexión. Los modelos solo Live usan otra API y no aparecen aquí.", "از فهرست مدلی انتخاب و اتصال را آزمایش کنید. مدل‌های مخصوص Live از API دیگری استفاده می‌کنند و اینجا فهرست نمی‌شوند.")
                } else {
                    val result = withTimeout(16_000) { actions.onTestProvider(selected, enteredKey) }
                    verifiedConnection = result
                    connectionStatus = "${result.model} · ${result.elapsedMs / 1000.0}s\n${result.sample}"
                    connectionDialog = connectionStatus
                    actions.onPlayConnectionTest(result.sample)
                }
            } catch (_: TimeoutCancellationException) {
                connectionDialog = l.choose("The request timed out. Nothing was verified. Check your connection, quota and model; try a listed Flash or Flash-Lite text model.", "La solicitud agotó el tiempo. No se verificó la conexión. Revisa internet, cuota y modelo; prueba Flash o Flash-Lite.", "مهلت درخواست تمام شد. اتصال تأیید نشده است. اینترنت، سهمیه و مدل را بررسی کنید؛ Flash یا Flash-Lite را امتحان کنید.")
                connectionStatus = connectionDialog.orEmpty()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                connectionStatus = if (error is ProviderException) error.message.orEmpty() else l.choose("Check the API key and model ID, then try again.", "Revisa la clave API y el ID del modelo e inténtalo otra vez.", "کلید API و شناسهٔ مدل را بررسی و دوباره امتحان کنید.")
                connectionDialog = connectionStatus
            } finally {
                if (generation == connectionGeneration) checking = false
            }
        }
    }
    LaunchedEffect(state.settings.provider, state.settings.model, state.settings.language) { clearConnectionCheck() }
    LaunchedEffect(checking) { connectionElapsed = 0; while (checking) { delay(1000); connectionElapsed++ } }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Button(onClick=actions.onOpenNeural,modifier=Modifier.fillMaxWidth()){Text(l.choose("Neural Core · brain, knowledge & briefings","Neural Core · cerebro y resúmenes","هستهٔ عصبی · دانش و گزارش روزانه"))} }
        item { Button(onClick={actions.onInteraction(com.jarvis.assistant.sentinel.SentinelSound.PANEL_OPEN);sentinel=true},modifier=Modifier.fillMaxWidth()){Text(l.choose("Sentinel · wallpaper & sound","Sentinel · fondo y sonido","سنتینل · پس‌زمینه و صدا"))} }
        item { HudSettingsCard(state, actions) }
        item { if(state.authorization!=null) AuthorizationPanel(state,actions) }
        item { PageTitle(l.choose("Made to feel like yours.", "Hecho a tu manera.", "همان‌طور که شما می‌خواهید."), l.choose("SETTINGS", "AJUSTES", "تنظیمات")) }
        item { SectionCard {
            Text(l.choose("Identity & language", "Identidad e idioma", "نام و زبان"), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(draft.name, { draft = draft.copy(name = it); saved = false }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(l.choose("Your name", "Tu nombre", "نام شما")) }, shape = RoundedCornerShape(14.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AppLanguage.entries.forEach { lang -> FilterChip(selected = lang == l, onClick = { actions.onLanguage(lang) }, label = { Text(lang.label) }) } }
            Text(l.choose("Your selected language controls voice recognition, replies, and the interface.", "El idioma elegido se usa para el reconocimiento, las respuestas y la interfaz.", "زبان انتخابی برای تشخیص گفتار، پاسخ‌ها و رابط برنامه استفاده می‌شود."), color = Muted, style = MaterialTheme.typography.bodySmall)
        } }
        item { SectionCard {
            Text(l.choose("Intelligence, your way", "Inteligencia a tu manera", "هوش مصنوعی، به انتخاب شما"), style = MaterialTheme.typography.titleMedium)
            Text(l.choose("Use a small on-device model without an API key, or optionally connect your own cloud provider. News, app shortcuts, and memory work without either.", "Usa un modelo local sin clave API o conecta tu proveedor en la nube. Noticias, atajos y memoria funcionan sin ambos.", "از مدل کوچک روی گوشی و بدون کلید API استفاده کنید، یا در صورت تمایل سرویس آنلاین خود را وصل کنید. خبرها، میان‌برها و یادداشت‌ها به هیچ‌کدام نیاز ندارند."), color = Muted, style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("local" to l.choose("On-device · no key", "Local · sin clave", "روی گوشی · بدون کلید"), "gemini" to "Gemini", "openai" to "OpenAI", "anthropic" to "Claude").forEach { (id, title) -> FilterChip(selected = draft.provider == id, onClick = {
                    clearConnectionCheck()
                    draft = draft.copy(provider = id, model = when(id) { "gemini" -> "gemini-2.5-flash"; "openai" -> "gpt-4o-mini"; "anthropic" -> "claude-sonnet-4-20250514"; else -> "qwen2.5-0.5b" }); key = ""; saved = false
                }, label = { Text(title) }) }
            }
            if (draft.provider == "local") {
                Text(l.choose("Settings → Neural Core offers Lite (~400 MB) and an optional near-1 GB Qwen2.5 1.5B brain. Review exact bytes and license before download, then activate separately. RAM-based refusals are off. Phone performance and language quality still need on-device evaluation.","Neural Core ofrece Lite (~400 MB) y un cerebro Qwen2.5 1.5B opcional de casi 1 GB. Revisa tamaño y licencia antes de descargar; actívalo por separado. El bloqueo por RAM está desactivado. Aún se debe evaluar rendimiento e idiomas en el teléfono.","هستهٔ عصبی مدل سبک حدود ۴۰۰ مگابایت و مغز اختیاری Qwen2.5 1.5B نزدیک ۱ گیگابایت دارد. اندازه و مجوز را بررسی و دانلود کنید؛ فعال‌سازی جداست. محدودیت رم حذف شده است. عملکرد و کیفیت زبان‌ها هنوز باید روی گوشی ارزیابی شوند."), color = Muted, style = MaterialTheme.typography.bodySmall)
                if (state.brainStatus.isNotBlank()) Text(state.brainStatus, color = Warm)
                if(state.neuralStage!="Idle")Text(state.neuralStage,color=Warm)
                state.brainProgress?.let { LinearProgressIndicator(progress = { it.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth()) }
                OutlinedButton(onClick = actions.onDownloadBrain, enabled = state.brainProgress == null, modifier = Modifier.fillMaxWidth()) { Text(l.choose("Download on-device model", "Descargar modelo local", "دانلود مدل روی گوشی")) }
            } else {
            if(draft.provider!="local")OutlinedTextField(draft.model, { clearConnectionCheck(false); draft = draft.copy(model = it); saved = false }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(l.choose("Model ID", "ID del modelo", "شناسهٔ مدل")) }, shape = RoundedCornerShape(14.dp))
            OutlinedTextField(key, { clearConnectionCheck(); key = it; saved = false }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(l.choose("API key", "Clave API", "کلید API")) },
                placeholder = { Text(l.choose("Leave blank to keep the saved key", "Vacío conserva la clave guardada", "برای حفظ کلید قبلی خالی بگذارید")) },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = { IconButton(onClick = { showKey = !showKey }) { Icon(if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, l.choose("Toggle key visibility", "Mostrar u ocultar clave", "نمایش یا پنهان کردن کلید")) } }, shape = RoundedCornerShape(14.dp))
            Text(if (draft.provider == state.settings.provider && state.hasApiKey) l.choose("A key is saved for this provider. Saving a key does not verify the connection.", "Hay una clave guardada. Guardarla no verifica la conexión.", "کلید این سرویس ذخیره شده است. ذخیرهٔ کلید به معنی تأیید اتصال نیست.") else l.choose("Enter a key for this provider, or leave blank to use its previously saved key.", "Introduce una clave o deja el campo vacío para usar la guardada para este proveedor.", "کلید این سرویس را وارد کنید یا برای استفاده از کلید قبلی آن، خالی بگذارید."), color = Muted, style = MaterialTheme.typography.bodySmall)
            if (draft.provider == "gemini") {
                OutlinedButton(onClick = { checkConnection(true) }, enabled = !checking, modifier = Modifier.fillMaxWidth()) { Text(l.choose("Find Gemini models", "Buscar modelos Gemini", "پیدا کردن مدل‌های Gemini")) }
                if (availableModels.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableModels.forEach { model -> FilterChip(selected = draft.model == model, onClick = { clearConnectionCheck(false); draft = draft.copy(model = model); saved = false }, label = { Text(model) }) }
                }
            }
            OutlinedButton(onClick = { checkConnection(false) }, enabled = !checking, modifier = Modifier.fillMaxWidth()) { Text(l.choose("Test connection", "Probar conexión", "آزمایش اتصال")) }
            Text(l.choose("The test sends only a short sample prompt, without your conversations or phone data. Provider quota or charges may apply. Save settings after a successful test.", "La prueba envía solo un mensaje breve, sin conversaciones ni datos del teléfono. Puede consumir cuota o tener coste. Guarda los ajustes después.", "آزمایش فقط یک پیام نمونهٔ کوتاه می‌فرستد، بدون گفت‌وگوها یا داده‌های گوشی. ممکن است سهمیه یا هزینهٔ سرویس مصرف شود. پس از موفقیت، تنظیمات را ذخیره کنید."), color = Muted, style = MaterialTheme.typography.bodySmall)
            if (checking) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (connectionStatus.isNotBlank()) Text(connectionStatus, color = Warm)
            }
        } }
        item { SectionCard {
            Text(l.choose("A voice that feels natural", "Una voz natural", "صدایی طبیعی"), style = MaterialTheme.typography.titleMedium)
            ToggleRow(l.choose("Speak replies", "Leer respuestas", "خواندن پاسخ‌ها"), l.choose("Read answers aloud when a voice is available.", "Leer en voz alta si hay una voz disponible.", "در صورت وجود صدای مناسب، پاسخ‌ها بلند خوانده می‌شوند."), draft.speakReplies) { draft = draft.copy(speakReplies = it); saved = false }
            ToggleRow(l.choose("Allow network speech", "Permitir voz en línea", "اجازهٔ پردازش گفتار آنلاین"), l.choose("If on-device recognition is unavailable, allow the phone's speech provider to process your command online after you tap the mic.", "Si no hay reconocimiento local, permite que el servicio del teléfono procese la solicitud en línea al tocar el micrófono.", "اگر تشخیص گفتار روی گوشی موجود نباشد، سرویس گفتار گوشی می‌تواند پس از لمس میکروفن، درخواست را آنلاین پردازش کند."), draft.allowNetworkSpeech) { draft = draft.copy(allowNetworkSpeech = it); saved = false }
            ToggleRow(l.choose("OpenAI cloud voice", "Voz de OpenAI", "صدای آنلاین OpenAI"), l.choose("Optional for general AI chat. Requires an OpenAI key and sends its reply text for speech. Phone tools, memories, screens and files use on-device voices only.", "Opcional para conversación general. Requiere clave OpenAI y envía la respuesta para generar voz. Datos del teléfono, memoria, pantalla y archivos usan solo voces locales.", "برای گفت‌وگوی عمومی اختیاری است؛ به کلید OpenAI نیاز دارد و متن پاسخ را برای تولید صدا می‌فرستد. اطلاعات گوشی، حافظه، صفحه و فایل‌ها فقط با صدای روی دستگاه خوانده می‌شوند."), draft.useCloudVoice) { draft = draft.copy(useCloudVoice = it); saved = false }
            Text(l.choose("No system voice? Download an offline voice for this language, roughly 60–70 MB. It works without an API key after installation.", "¿Falta una voz del sistema? Descarga una voz local para este idioma, de unos 60–70 MB. Funciona sin clave API tras instalarla.", "صدای مناسب روی گوشی ندارید؟ صدای آفلاین این زبان را با حجم حدود ۶۰ تا ۷۰ مگابایت دانلود کنید. پس از نصب به کلید API نیاز ندارد."), color = Muted, style = MaterialTheme.typography.bodySmall)
            if (state.voiceStatus.isNotBlank()) Text(state.voiceStatus, color = Warm)
            state.voiceProgress?.let { LinearProgressIndicator(progress = { it.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth()) }
            OutlinedButton(onClick = actions.onDownloadVoice, enabled = state.voiceProgress == null, modifier = Modifier.fillMaxWidth()) { Text(l.choose("Download offline ${l.label} voice", "Descargar voz local: ${l.label}", "دانلود صدای آفلاین ${l.label}")) }
            OutlinedButton(onClick = actions.onVoiceCheck) { Icon(Icons.Rounded.Headphones, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(l.choose("Check this language's voice", "Probar la voz de este idioma", "آزمایش صدای این زبان")) }
            TextButton(onClick = { actions.onOpenPermission("voices") }) { Text(l.choose("System voice settings", "Ajustes de voz del sistema", "تنظیمات صدای گوشی")) }
        } }
        item { SectionCard {
            Text(l.choose("Screen Security & Voice Unlock","Seguridad y desbloqueo por voz","امنیت صفحه و درخواست بازکردن قفل با صدا"),style=MaterialTheme.typography.titleMedium)
            Text(l.choose("Verify with Android. Jarvis never collects your system credential.","Verifica con Android. Jarvis no recoge tu credencial.","با اندروید تأیید کنید. جارویس رمز دستگاه را جمع‌آوری نمی‌کند."))
            OutlinedButton(onClick=actions.onOpenContacts){Text(l.choose("Fifteen contact shortcuts & personal memos","Quince atajos de contactos y notas personales","پانزده میانبر مخاطب و پیام شخصی"))}
            Button(onClick=actions.onOpenOutputVoice){Text(l.choose("Output voice · editable ID","Voz de salida · ID editable","صدای خروجی · شناسهٔ قابل ویرایش"))}
            Button(onClick=actions.onOpenScreenSecurity){Text(l.choose("Open screen security","Abrir seguridad de pantalla","باز کردن امنیت صفحه"))}
        } }
        item { SectionCard {
            Text(l.choose("Memory & locked-phone replies", "Memoria y respuestas con bloqueo", "حافظه و پاسخ هنگام قفل"), style = MaterialTheme.typography.titleMedium)
            ToggleRow(l.choose("Use saved memories with cloud AI", "Usar recuerdos con la IA en la nube", "استفاده از یادداشت‌ها در هوش مصنوعی ابری"), l.choose("Sends up to 20 facts you explicitly saved with your questions. Phone tools and screenshots never enter general conversation history. Off by default.", "Envía hasta 20 hechos guardados explícitamente con tus preguntas. Herramientas y capturas no entran al historial general. Desactivado por defecto.", "حداکثر ۲۰ موردی که صریحاً ذخیره کرده‌اید همراه پرسش ارسال می‌شود. داده‌های ابزار گوشی و تصویر صفحه وارد سابقهٔ عمومی نمی‌شوند. پیش‌فرض خاموش است."), draft.shareSavedMemories) { draft = draft.copy(shareSavedMemories = it); saved = false }
            ToggleRow(l.choose("General chat while locked", "Conversar con el teléfono bloqueado", "گفت‌وگوی عمومی هنگام قفل"), l.choose("Allows general answers aloud after your wake phrase while the screen is off or locked. Anyone nearby may ask. Private data and phone actions still require unlock. The phone must be powered on.", "Permite respuestas generales tras la frase de activación con pantalla apagada o bloqueo. Cualquiera cerca puede preguntar. Datos privados y acciones requieren desbloqueo. El teléfono debe estar encendido.", "پس از عبارت فعال‌سازی، هنگام خاموش بودن صفحه یا قفل، پاسخ عمومی بلند پخش می‌شود. افراد نزدیک هم می‌توانند بپرسند. اطلاعات خصوصی و فرمان‌های گوشی همچنان به باز کردن قفل نیاز دارند. گوشی باید روشن باشد."), draft.allowLockedChat) { draft = draft.copy(allowLockedChat = it); saved = false }
        } }
        item { SectionCard {
            Text(l.choose("Hands-free, on your terms", "Manos libres, tú decides", "بدون لمس، با اختیار شما"), style = MaterialTheme.typography.titleMedium)
            Text(l.choose("Download a local Vosk model for the selected language. Wake detection stays on this phone; a persistent microphone notification remains visible. Android may pause listening.", "Descarga un modelo Vosk local para el idioma elegido. La activación se detecta en el teléfono y se muestra una notificación del micrófono. Android puede pausar la escucha.", "مدل محلی Vosk را برای زبان انتخابی دانلود کنید. تشخیص عبارت فعال‌سازی روی گوشی انجام می‌شود و اعلان میکروفن نمایش داده می‌شود. اندروید ممکن است شنیدن را متوقف کند."), color = Muted, style = MaterialTheme.typography.bodySmall)
            Text("“${l.wakePhrase}”", color = Mint, style = MaterialTheme.typography.titleLarge)
            Text(l.choose("Active listening keeps the CPU awake and uses battery with the screen off. Android or MIUI can still stop it. After reboot or Force stop, open Jarvis and start listening again.", "La escucha activa mantiene la CPU despierta y consume batería con pantalla apagada. Android o MIUI pueden detenerla. Tras reiniciar o forzar detención, abre Jarvis e inicia la escucha.", "شنیدن فعال، پردازنده را بیدار نگه می‌دارد و هنگام خاموش بودن صفحه باتری مصرف می‌کند. اندروید یا MIUI ممکن است آن را متوقف کند. پس از راه‌اندازی مجدد یا توقف اجباری، جارویس را باز و شنیدن را شروع کنید."), color = Muted, style = MaterialTheme.typography.bodySmall)
            if (state.wakeStatus.isNotBlank()) Text(state.wakeStatus, color = Warm, style = MaterialTheme.typography.bodyMedium)
            state.modelProgress?.let { progress -> LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth()) }
            OutlinedButton(onClick = actions.onDownloadWake, enabled = state.modelProgress == null, modifier = Modifier.fillMaxWidth()) { Text(l.choose("Download language model · ~40–55 MB", "Descargar modelo · ~40–55 MB", "دانلود مدل زبان · حدود ۴۰ تا ۵۵ مگابایت")) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = actions.onStartWake, modifier = Modifier.weight(1f)) { Text(l.choose("Start listening", "Escuchar", "شروع شنیدن")) }
                OutlinedButton(onClick = actions.onStopWake, modifier = Modifier.weight(1f)) { Text(l.choose("Pause", "Pausar", "توقف")) }
            }
        } }
        item { SectionCard {
            Text(l.choose("Connections you choose", "Conexiones que tú eliges", "دسترسی‌های انتخابی شما"), style = MaterialTheme.typography.titleMedium)
            Text(l.choose("Each feature asks for its own access. Jarvis cannot silently access every app, photo, or file. You can revoke access in Android Settings.", "Cada función pide su acceso. Jarvis no puede acceder en secreto a todas tus aplicaciones, fotos o archivos. Puedes revocar los permisos en Ajustes de Android.", "هر قابلیت دسترسی جداگانه می‌خواهد. جارویس نمی‌تواند پنهانی به همهٔ برنامه‌ها، عکس‌ها یا فایل‌ها دسترسی داشته باشد. دسترسی‌ها از تنظیمات اندروید قابل لغو هستند."), color = Muted, style = MaterialTheme.typography.bodySmall)
            PermissionButton(l.choose("Microphone", "Micrófono", "میکروفن"), "microphone", actions)
            PermissionButton(l.choose("Screen reading · Accessibility", "Lectura de pantalla · Accesibilidad", "خواندن صفحه · دسترس‌پذیری"), "accessibility", actions)
            PermissionButton(l.choose("Listening notifications", "Avisos de escucha", "اعلان وضعیت شنیدن"), "notifications", actions)
            PermissionButton(l.choose("Read notification summaries", "Leer resúmenes de notificaciones", "خواندن خلاصهٔ اعلان‌ها"), "notification_access", actions)
            PermissionButton(l.choose("Battery & background settings", "Batería y segundo plano", "باتری و پس‌زمینه"), "battery", actions)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = actions.onPickFiles, modifier = Modifier.weight(1f)) { Text(l.choose("Choose files", "Archivos", "انتخاب فایل")) }
                OutlinedButton(onClick = actions.onPickPhotos, modifier = Modifier.weight(1f)) { Text(l.choose("Choose photos", "Fotos", "انتخاب عکس")) }
            }
        } }
        item {
            Button(onClick = { actions.onSaveSettings(draft.copy(name = draft.name.trim(), model = draft.model.trim()), key.trim()); key = ""; saved = true }, enabled = !checking, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp)) {
                Icon(if (saved) Icons.Rounded.Check else Icons.Rounded.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (saved) l.choose("Saved", "Guardado", "ذخیره شد") else l.choose("Save settings", "Guardar ajustes", "ذخیرهٔ تنظیمات"))
            }
        }
        item { SectionCard {
            Text(l.choose("Your data, your decision", "Tus datos, tu decisión", "داده‌های شما، تصمیم شما"), style = MaterialTheme.typography.titleMedium)
            Text(l.choose("Conversation and memories are stored on this phone. The on-device model keeps inference local; news still needs the internet. Choosing a cloud provider shares conversation context with that provider. Stop everything closes active listening and cancels pending work.", "Las conversaciones y recuerdos se guardan en este teléfono. El modelo local procesa aquí; las noticias necesitan internet. Elegir la nube comparte el contexto con el proveedor. Detener todo cierra la escucha y cancela tareas.", "گفت‌وگوها و یادداشت‌ها روی این گوشی ذخیره می‌شوند. مدل روی گوشی، پردازش را محلی انجام می‌دهد؛ خبرها همچنان اینترنت می‌خواهند. با انتخاب سرویس آنلاین، متن مرتبط گفت‌وگو به همان سرویس فرستاده می‌شود. توقف همه‌چیز، شنیدن و کارهای در انتظار را متوقف می‌کند."), color = Muted, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { showClearConversation = true }) { Text(l.choose("Clear conversation", "Borrar conversación", "پاک کردن گفت‌وگو")) }
            TextButton(onClick = { showWipe = true }) { Text(l.choose("Erase Jarvis data & keys", "Borrar datos y claves de Jarvis", "حذف داده‌ها و کلیدهای جارویس"), color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = { actions.onOpenPermission("licenses") }) { Text(l.choose("Open-source licenses", "Licencias de código abierto", "مجوزهای متن‌باز")) }
            Text("JARVIS ${com.jarvis.assistant.BuildConfig.VERSION_NAME} · SEPARATE APP", color = Muted, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp)
        } }
        item { Spacer(Modifier.height(12.dp)) }
    }
    if (checking) AlertDialog(onDismissRequest = {}, title = { Text(l.choose("Checking provider…", "Comprobando proveedor…", "در حال بررسی سرویس…")) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(l.choose("${connectionElapsed}s · up to 15 seconds. Waiting for an actual API response.", "${connectionElapsed}s · hasta 15 segundos. Esperando una respuesta real de la API.", "${connectionElapsed} ثانیه · حداکثر ۱۵ ثانیه. در انتظار پاسخ واقعی API.")) } },
        confirmButton = { TextButton(onClick = { clearConnectionCheck(); connectionStatus = l.choose("Check cancelled.", "Prueba cancelada.", "بررسی لغو شد.") }) { Text(l.choose("Cancel check", "Cancelar prueba", "لغو بررسی")) } })
    connectionDialog?.let { message -> AlertDialog(onDismissRequest = { connectionDialog = null },
        title = { Text(if (verifiedConnection != null) l.choose("Connection verified", "Conexión verificada", "اتصال تأیید شد") else l.choose("Connection not verified", "Conexión no verificada", "اتصال تأیید نشد")) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = {
            if (verifiedConnection != null) { actions.onSaveSettings(draft.copy(name = draft.name.trim(), model = verifiedConnection!!.model), key.trim()); key = ""; saved = true }
            connectionDialog = null
        }) { Text(if (verifiedConnection != null) l.choose("Connect & save", "Conectar y guardar", "اتصال و ذخیره") else l.choose("OK", "Aceptar", "باشه")) } },
        dismissButton = { if (verifiedConnection != null) TextButton(onClick = { connectionDialog = null }) { Text(l.choose("Close", "Cerrar", "بستن")) } }) }
    if (showWipe) AlertDialog(onDismissRequest = { showWipe = false },
        title = { Text(l.choose("Erase Jarvis data?", "¿Borrar los datos de Jarvis?", "داده‌های جارویس حذف شوند؟")) },
        text = { Text(l.choose("This removes saved conversations, memories, settings, and provider keys. It cannot be undone.", "Se borrarán conversaciones, recuerdos, ajustes y claves. No se puede deshacer.", "گفت‌وگوها، یادداشت‌ها، تنظیمات و کلیدهای سرویس حذف می‌شوند. این کار برگشت‌پذیر نیست.")) },
        confirmButton = { TextButton(onClick = { showWipe = false; actions.onWipe() }) { Text(l.choose("Erase", "Borrar", "حذف"), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { showWipe = false }) { Text(l.choose("Cancel", "Cancelar", "لغو")) } })
    if (showClearConversation) AlertDialog(onDismissRequest = { showClearConversation = false },
        title = { Text(l.choose("Clear this conversation?", "¿Borrar esta conversación?", "این گفت‌وگو پاک شود؟")) },
        text = { Text(l.choose("Saved memories and provider keys stay. Conversation messages will be deleted.", "Se conservarán los recuerdos y claves. Se borrarán los mensajes de la conversación.", "یادداشت‌ها و کلیدهای سرویس حفظ می‌شوند. پیام‌های گفت‌وگو حذف خواهند شد.")) },
        confirmButton = { TextButton(onClick = { showClearConversation = false; actions.onClearConversation() }) { Text(l.choose("Clear", "Borrar", "پاک کردن")) } },
        dismissButton = { TextButton(onClick = { showClearConversation = false }) { Text(l.choose("Cancel", "Cancelar", "لغو")) } })
}

@Composable
private fun PermissionButton(title: String, key: String, actions: JarvisUiActions) {
    OutlinedButton(onClick = { actions.onOpenPermission(key) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
        Text(title, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(16.dp))
    }
}
