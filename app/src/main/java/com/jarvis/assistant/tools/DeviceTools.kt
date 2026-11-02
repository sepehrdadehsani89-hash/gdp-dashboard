package com.jarvis.assistant.tools

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantReply
import com.jarvis.assistant.core.PendingAction
import com.jarvis.assistant.core.SourceLink
import com.jarvis.assistant.core.choose
import com.jarvis.assistant.data.JarvisStore
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Deterministic, user-requested tools. Model-generated prose cannot invoke these. */
class DeviceTools(private val context: Context, private val store: JarvisStore) {
    private val pending = ConcurrentHashMap<String, PendingAction>()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS).followRedirects(false).build()

    suspend fun route(text: String, language: AppLanguage): AssistantReply? {
        WhatsAppDraft.parse(text)?.let { draft ->
            if (locked()) return AssistantReply(unlockMessage(language))
            return proposal("Open WhatsApp draft for +${draft.number}: ${draft.text}\nReview the recipient and send manually in WhatsApp.",
                "open_url", "https://wa.me/${draft.number}?text=${encode(draft.text)}")
        }
        LiveControlPolicy.parse(text)?.let { request ->
            if (locked()) return AssistantReply(unlockMessage(language))
            return try {
                val payload = withContext(Dispatchers.Main) { JarvisAccessibilityService.prepareLive(context, request) }
                proposal(when (request) {
                    is LiveRequest.Tap -> "Tap “${request.label}” on the current screen? Confirm by voice while that screen stays open."
                    is LiveRequest.Edit -> "Replace the focused draft with: ${request.text}\nThis does not send it. Confirm by voice while that screen stays open."
                }, "live_control", payload)
            } catch (error: Exception) { AssistantReply(error.message ?: "Live control unavailable.") }
        }
        if (Regex("^(calculate|compute|calcula|calcular|محاسبه کن|حساب کن)\\s+",RegexOption.IGNORE_CASE).containsMatchIn(text.trim())) {
            return AssistantReply(runCatching { com.jarvis.assistant.core.Calculation.requested(text) ?: "Enter an arithmetic expression." }.getOrElse { it.message ?: "Invalid calculation." })
        }
        Regex("^(?:remind me|recuérdame|یادآوری کن)\\s+(.+)$",RegexOption.IGNORE_CASE).matchEntire(text.trim())?.let {
            return proposal("Open a calendar draft for: ${it.groupValues[1]}? Choose the date and save it in your calendar.","reminder_draft",it.groupValues[1].take(300))
        }
        val normalized = ToolPolicy.normalize(text)
        val remembered = LocalIntentParser.rememberedText(text)
        if (remembered != null) {
            if (locked()) return AssistantReply(unlockMessage(language))
            store.remember(remembered.take(2_000))
            return AssistantReply(language.choose("I'll remember that. You can view or delete it in Memory.", "Lo recordaré. Puedes verlo o eliminarlo en Memoria.", "این را به خاطر می‌سپارم. در بخش حافظه می‌توانید آن را حذف کنید."))
        }
        if (isMemoryRequest(normalized)) {
            if (locked()) return AssistantReply(unlockMessage(language))
            val facts = store.memories.value.take(20)
            return AssistantReply(if (facts.isEmpty()) language.choose("You haven't asked me to remember anything yet.", "Aún no me has pedido que recuerde nada.", "هنوز چیزی از من نخواسته‌اید که به خاطر بسپارم.") else language.choose("Here is what you asked me to remember:\n", "Esto es lo que me pediste recordar:\n", "این‌ها مواردی هستند که خواسته‌اید به خاطر بسپارم:\n") + facts.joinToString("\n") { it.text })
        }
        if (LocalIntentParser.isNews(text)) return news(text, language)
        if (UtilityIntentParser.isWeather(text)) {
            val city = UtilityIntentParser.weatherCity(text)
                ?: return AssistantReply(language.choose("Which city? For example, “weather in Quito”. I only send the city you request to the weather source.", "¿Qué ciudad? Por ejemplo, «clima en Quito». Solo envío la ciudad que pides a la fuente meteorológica.", "کدام شهر؟ مثلاً «هوای تهران». فقط نام شهری را که درخواست کرده‌اید به منبع هواشناسی می‌فرستم."))
            return WeatherTool().reply(city, language)
        }
        if (LocalIntentParser.isScreen(text)) return screen(language)
        if (LocalIntentParser.isNotifications(text)) return notifications(language)
        if (UtilityIntentParser.isTimer(text)) {
            if (locked()) return AssistantReply(unlockMessage(language))
            val seconds = UtilityIntentParser.timerSeconds(text) ?: return AssistantReply(language.choose("Say a duration, for example “set a timer for 5 minutes”. The maximum is 24 hours.", "Di una duración, por ejemplo «temporizador de 5 minutos». El máximo es 24 horas.", "مدت را بگویید، مثلاً «تایمر ۵ دقیقه». بیشترین مدت ۲۴ ساعت است."))
            val duration = if (seconds % 60 == 0) "${seconds / 60}" else "$seconds"
            val unit = if (seconds % 60 == 0) language.choose("minutes", "minutos", "دقیقه") else language.choose("seconds", "segundos", "ثانیه")
            return proposal(language.choose("Set a timer for $duration $unit?", "¿Poner un temporizador de $duration $unit?", "تایمر برای $duration $unit تنظیم شود؟"), "set_timer", seconds.toString())
        }
        if (UtilityIntentParser.isAlarm(text)) {
            if (locked()) return AssistantReply(unlockMessage(language))
            val time = UtilityIntentParser.alarmTime(text) ?: return AssistantReply(language.choose("Say the alarm time with hour and minutes, for example “alarm at 07:30”.", "Di la hora y los minutos, por ejemplo «alarma a las 07:30».", "ساعت و دقیقه را بگویید، مثلاً «آلارم ۰۷:۳۰»."))
            val value = String.format(Locale.ROOT, "%02d:%02d", time.first, time.second)
            return proposal(language.choose("Set an alarm for $value?", "¿Poner una alarma a las $value?", "آلارم برای ساعت $value تنظیم شود؟"), "set_alarm", value)
        }
        UtilityIntentParser.mapQuery(text)?.let { query ->
            if (locked()) return AssistantReply(unlockMessage(language))
            return proposal(language.choose("Open Maps for “$query”?", "¿Abrir Maps para «$query»?", "نقشه برای «$query» باز شود؟"), "open_url", "https://www.google.com/maps/search/?api=1&query=${encode(query)}")
        }
        if (Regex("\\bbattery\\b|\\bbatería\\b|\\bbateria\\b|باتری").containsMatchIn(normalized)) {
            val level = context.getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            return AssistantReply(if (level in 0..100) language.choose("Battery level is $level percent.", "La batería está al $level por ciento.", "شارژ باتری $level درصد است.") else language.choose("Android did not report a battery level.", "Android no indicó el nivel de batería.", "اندروید میزان باتری را گزارش نکرد."))
        }
        if (listOf("what time", "time is it", "qué hora", "que hora", "ساعت چنده", "ساعت چند", "hora actual").any(normalized::contains)) {
            val time = LocalDateTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.forLanguageTag(language.tag)))
            return AssistantReply(language.choose("It's $time.", "Son las $time.", "ساعت $time است."))
        }
        if (listOf("what date", "today's date", "fecha de hoy", "qué día", "تاریخ امروز", "امروز چندم").any(normalized::contains)) {
            val date = LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.forLanguageTag(language.tag)))
            return AssistantReply(date)
        }
        if (isHome(normalized) || isBack(normalized)) {
            if (locked()) return AssistantReply(unlockMessage(language))
            val home = isHome(normalized)
            return proposal(language.choose(if (home) "Go to the home screen?" else "Go back one screen?", if (home) "¿Ir a la pantalla de inicio?" else "¿Volver a la pantalla anterior?", if (home) "به صفحه اصلی برویم؟" else "به صفحه قبلی برویم؟"), if (home) "nav_home" else "nav_back", "")
        }
        val youtube = LocalIntentParser.youtubeQuery(text)
        if (youtube != null) {
            if (locked()) return AssistantReply(unlockMessage(language))
            if (youtube.isBlank() && context.packageManager.getLaunchIntentForPackage("com.google.android.youtube") != null) {
                return try { withLaunch("com.google.android.youtube"); AssistantReply("Opened YouTube.") }
                catch (_: Exception) { AssistantReply("Android could not open YouTube.") }
            }
            val url = if (youtube.isBlank()) "https://www.youtube.com" else "https://www.youtube.com/results?search_query=${encode(youtube)}"
            return proposal(language.choose(if (youtube.isBlank()) "Open YouTube?" else "Search YouTube for “$youtube”?", if (youtube.isBlank()) "¿Abrir YouTube?" else "¿Buscar «$youtube» en YouTube?", if (youtube.isBlank()) "یوتیوب باز شود؟" else "«$youtube» را در یوتیوب جستجو کنم؟"), "open_url", url)
        }
        val appName = LocalIntentParser.appName(text)
        if (appName != null) return app(appName, language)
        if (listOf("my files", "my photos", "mis archivos", "mis fotos", "فایل های من", "عکس های من", "فایل‌ها", "عکس‌ها").any(normalized::contains)) {
            return AssistantReply(language.choose("Tap Attach to choose files or photos. Android lets you decide exactly what Jarvis can read; I don't have access to all of your private app data.", "Toca Adjuntar para elegir archivos o fotos. Android te permite decidir qué puede leer Jarvis; no tengo acceso a todos los datos privados de tus aplicaciones.", "برای انتخاب فایل یا عکس، دکمه پیوست را بزنید. اندروید به شما اجازه می‌دهد دقیقاً مشخص کنید جارویس چه چیزی را بخواند؛ من به همه اطلاعات خصوصی برنامه‌ها دسترسی ندارم."))
        }
        return null
    }

    /** Single use, action-bound approval. Only proposals created by this instance execute. */
    suspend fun execute(action: PendingAction): String = withContext(Dispatchers.Main) {
        val language = store.settings.value.language
        val registered = pending.remove(action.id)
        if (registered != action || !ToolPolicy.confirmationCurrent(action.createdAt, System.currentTimeMillis())) {
            return@withContext language.choose("This approval expired or changed. Please ask again.", "Esta confirmación caducó o cambió. Pídelo otra vez.", "این تأیید منقضی شده یا تغییر کرده است. لطفاً دوباره درخواست کنید.")
        }
        if (locked()) return@withContext unlockMessage(language)
        try {
            when (action.kind) {
                "live_control" -> check(JarvisAccessibilityService.executeLive(context, action.payload)) { "The control did not accept the action." }
                "open_url" -> {
                    check(ToolPolicy.allowedWebUrl(action.payload))
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.payload)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val resolved = context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
                    check(resolved == null || !ToolPolicy.protectedPackage(resolved))
                    context.startActivity(intent)
                }
                "open_app" -> {
                    check(!ToolPolicy.protectedPackage(action.payload))
                    val intent = context.packageManager.getLaunchIntentForPackage(action.payload) ?: error("Application unavailable")
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                "nav_home", "nav_back" -> {
                    check(JarvisAccessibilityService.navigate(context, action.kind == "nav_home"))
                }
                "reminder_draft" -> {
                    context.startActivity(Intent(Intent.ACTION_INSERT,android.provider.CalendarContract.Events.CONTENT_URI)
                        .putExtra(android.provider.CalendarContract.Events.TITLE,action.payload).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return@withContext "Calendar draft opened. Choose the date and save it there; no reminder has been saved by Jarvis."
                }
                "set_timer" -> {
                    val seconds = action.payload.toInt().also { check(it in 1..86_400) }
                    context.startActivity(Intent(AlarmClock.ACTION_SET_TIMER)
                        .putExtra(AlarmClock.EXTRA_LENGTH, seconds).putExtra(AlarmClock.EXTRA_MESSAGE, "Jarvis")
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, false).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                "set_alarm" -> {
                    val time = action.payload.split(':').map(String::toInt)
                    check(time.size == 2 && time[0] in 0..23 && time[1] in 0..59)
                    context.startActivity(Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_HOUR, time[0]).putExtra(AlarmClock.EXTRA_MINUTES, time[1])
                        .putExtra(AlarmClock.EXTRA_MESSAGE, "Jarvis").putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                else -> return@withContext language.choose("That action is not supported.", "Esa acción no es compatible.", "این کار پشتیبانی نمی‌شود.")
            }
            if(action.kind in listOf("set_alarm","set_timer")) language.choose("Clock opened. Review and save the alarm or timer there.","Reloj abierto. Revisa y guarda la alarma o el temporizador allí.","ساعت باز شد. زنگ یا تایمر را آنجا بررسی و ذخیره کنید.")
            else language.choose("Done.", "Listo.", "انجام شد.")
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            language.choose("Android couldn't complete that action. Check the app or optional permission and try again.", "Android no pudo completar la acción. Revisa la aplicación o el permiso opcional e inténtalo de nuevo.", "اندروید نتوانست این کار را انجام دهد. برنامه یا مجوز اختیاری را بررسی کنید و دوباره تلاش کنید.")
        }
    }

    fun cancelPendingActions() { pending.clear() }

    private fun proposal(description: String, kind: String, payload: String): AssistantReply {
        pending.entries.removeAll { !ToolPolicy.confirmationCurrent(it.value.createdAt, System.currentTimeMillis()) }
        val action = PendingAction(description = description, kind = kind, payload = payload)
        pending[action.id] = action
        return AssistantReply(description, action = action)
    }

    private fun app(name: String, language: AppLanguage): AssistantReply {
        if (locked()) return AssistantReply(unlockMessage(language))
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = context.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.packageName to it.loadLabel(context.packageManager).toString() }.distinctBy { it.first }
        val exact = apps.filter { ToolPolicy.normalize(it.second) == name }
        val matches = exact.ifEmpty { apps.filter { ToolPolicy.normalize(it.second).contains(name) || name.contains(ToolPolicy.normalize(it.second)) } }
        if (matches.isEmpty()) return AssistantReply(language.choose("I couldn't find an installed app named “$name”.", "No encontré una aplicación instalada llamada «$name».", "برنامه نصب‌شده‌ای با نام «$name» پیدا نکردم."))
        if (matches.size > 1) return AssistantReply(language.choose("Which app? ", "¿Qué aplicación? ", "کدام برنامه؟ ") + matches.take(5).joinToString { it.second })
        val (packageName, label) = matches.single()
        if (ToolPolicy.protectedPackage(packageName)) return AssistantReply(language.choose("I don't operate protected banking, payment, password, or security apps. Please open it yourself.", "No manejo aplicaciones protegidas de banca, pagos, contraseñas o seguridad. Ábrela tú.", "من برنامه‌های محافظت‌شده بانکی، پرداخت، رمز عبور یا امنیتی را کنترل نمی‌کنم. لطفاً خودتان آن را باز کنید."))
        return try {
            withLaunch(packageName)
            AssistantReply("Opened $label.")
        } catch (_: Exception) { AssistantReply("Android could not open $label.") }
    }

    private fun withLaunch(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: error("App unavailable")
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun screen(language: AppLanguage): AssistantReply {
        val result = JarvisAccessibilityService.snapshot(context)
        return AssistantReply(when (result.failure) {
            ScreenFailure.LOCKED -> unlockMessage(language)
            ScreenFailure.PERMISSION -> language.choose("Enable optional Screen access in Settings, then open the screen you want me to read and ask again. I skip password fields and protected apps.", "Activa el acceso opcional a la pantalla en Ajustes, abre la pantalla que quieres leer y vuelve a preguntar. Omita campos de contraseña y aplicaciones protegidas.", "دسترسی اختیاری صفحه را در تنظیمات فعال کنید، سپس صفحه موردنظر را باز کنید و دوباره بپرسید. بخش‌های رمز عبور و برنامه‌های محافظت‌شده خوانده نمی‌شوند.")
            ScreenFailure.PROTECTED -> language.choose("This is a protected or sensitive screen. I won't read it.", "Esta pantalla es protegida o sensible. No la leeré.", "این صفحه محافظت‌شده یا حساس است. آن را نمی‌خوانم.")
            ScreenFailure.UNAVAILABLE -> language.choose("This screen does not expose readable text. I can't safely describe it from accessibility data.", "Esta pantalla no ofrece texto legible. No puedo describirla con seguridad mediante accesibilidad.", "این صفحه متن قابل‌خواندن ارائه نمی‌کند. نمی‌توانم از داده‌های دسترس‌پذیری آن را با اطمینان توصیف کنم.")
            null -> language.choose("Here is the visible screen text:\n", "Este es el texto visible de la pantalla:\n", "متن قابل‌مشاهده صفحه:\n") + result.text.orEmpty()
        })
    }

    private fun notifications(language: AppLanguage): AssistantReply {
        if (locked()) return AssistantReply(unlockMessage(language))
        val items = JarvisNotificationService.snapshot(context)
        return AssistantReply(if (items == null) language.choose("Enable optional Notification access in Settings to read current notifications. Protected apps and private codes are excluded.", "Activa el acceso opcional a notificaciones en Ajustes para leerlas. Se excluyen aplicaciones protegidas y códigos privados.", "برای خواندن اعلان‌های فعلی، دسترسی اختیاری اعلان را در تنظیمات فعال کنید. برنامه‌های محافظت‌شده و کدهای خصوصی مستثنا هستند.") else if (items.isEmpty()) language.choose("There are no readable notifications right now.", "No hay notificaciones legibles ahora.", "در حال حاضر اعلان قابل‌خواندنی وجود ندارد.") else language.choose("Your recent notifications:\n", "Tus notificaciones recientes:\n", "اعلان‌های اخیر شما:\n") + items.joinToString("\n"))
    }

    private suspend fun news(text: String, language: AppLanguage): AssistantReply {
        val yesterday = LocalIntentParser.isYesterday(text)
        val window = if (yesterday) ToolPolicy.yesterday() else null
        val region = if (language == AppLanguage.FA) "IR" else "US"
        val hl = when (language) { AppLanguage.EN -> "en-US"; AppLanguage.ES -> "es-419"; AppLanguage.FA -> "fa" }
        val topic = topic(text)
        val query = listOfNotNull(topic.takeIf(String::isNotBlank), window?.let { "after:${it.date} before:${it.date.plusDays(1)}" }).joinToString(" ")
        val url = if (query.isBlank()) "https://news.google.com/rss?hl=$hl&gl=$region&ceid=$region:${language.code}"
            else "https://news.google.com/rss/search?q=${encode(query)}&hl=$hl&gl=$region&ceid=$region:${language.code}"
        return try {
            val bytes = fetch(url)
            val articles = withContext(Dispatchers.Default) { NewsParser.parse(bytes, window) }.take(5)
            if (articles.isEmpty()) AssistantReply(language.choose("I couldn't verify any matching headlines${if (yesterday) " for yesterday" else ""}. Try another topic.", "No pude verificar titulares coincidentes${if (yesterday) " de ayer" else ""}. Prueba otro tema.", "نتوانستم خبر منطبقی${if (yesterday) " از دیروز" else ""} تأیید کنم. موضوع دیگری را امتحان کنید."))
            else {
                val intro = language.choose(if (yesterday) "Here are yesterday's headlines, dated ${window!!.date}:" else "Here are the latest available headlines:", if (yesterday) "Estos son los titulares de ayer, ${window!!.date}:" else "Estos son los últimos titulares disponibles:", if (yesterday) "تیترهای دیروز، ${window!!.date}:" else "تازه‌ترین تیترهای موجود:")
                AssistantReply(intro + "\n" + articles.mapIndexed { index, article -> "${index + 1}. ${article.title}" }.joinToString("\n") + "\n" + language.choose("These are source headlines, not independently verified reporting. Tap a source for the full story.", "Son titulares de las fuentes, no reportajes verificados de forma independiente. Toca una fuente para leer más.", "این‌ها تیتر منابع هستند و مستقلاً راستی‌آزمایی نشده‌اند. برای متن کامل، منبع را باز کنید."), articles.map { SourceLink(it.title, it.url) })
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { AssistantReply(language.choose("I couldn't reach the news feed. Check your connection and try again.", "No pude conectar con las noticias. Comprueba tu conexión e inténtalo de nuevo.", "به منبع خبر دسترسی پیدا نکردم. اتصال اینترنت را بررسی کنید و دوباره تلاش کنید.")) }
    }

    private suspend fun fetch(url: String): ByteArray {
        check(ToolPolicy.allowedWebUrl(url))
        val call = client.newCall(Request.Builder().url(url).header("User-Agent", "JarvisAndroid/0.1").build())
        val handle = currentCoroutineContext().job.invokeOnCompletion { if (it is CancellationException) call.cancel() }
        return try {
            withContext(Dispatchers.IO) {
                call.execute().use { response ->
                    check(response.isSuccessful) { "News source unavailable" }
                    val body = response.body ?: error("Empty news response")
                    check(body.contentLength() <= NewsParser.MAX_FEED_BYTES)
                    body.source().use { source ->
                        source.request(NewsParser.MAX_FEED_BYTES.toLong() + 1)
                        check(source.buffer.size <= NewsParser.MAX_FEED_BYTES)
                        source.buffer.readByteArray()
                    }
                }
            }
        } finally { handle.dispose() }
    }

    private fun topic(text: String): String = LocalIntentParser.newsTopic(text)

    private fun isMemoryRequest(t: String) = listOf("what do you remember", "my memories", "qué recuerdas", "que recuerdas", "mis recuerdos", "چی یادت", "چه چیزهایی یادت", "حافظه من").any(t::contains)
    private fun isHome(t: String) = t in setOf("go home", "home screen", "pantalla de inicio", "ir al inicio", "صفحه اصلی", "برو خانه")
    private fun isBack(t: String) = t in setOf("go back", "back", "volver", "atrás", "atras", "برگرد", "برو عقب")
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
    private fun locked(): Boolean = context.getSystemService(KeyguardManager::class.java).isDeviceLocked
    private fun unlockMessage(language: AppLanguage): String = language.choose("Unlock your phone first so I can safely help with private content or actions.", "Desbloquea el teléfono para acceder de forma segura a contenido privado o realizar acciones.", "ابتدا قفل گوشی را باز کنید تا بتوانم با اطمینان به اطلاعات خصوصی یا انجام کارها کمک کنم.")
}
