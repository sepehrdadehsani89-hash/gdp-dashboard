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
    val infinity=com.jarvis.assistant.infinity.InfinityTools(context)
    val appRegistry=com.jarvis.assistant.neural.AppRegistry(context)
    private var recentNews:Pair<Long,List<NewsArticle>>?=null
    private val pending = ConcurrentHashMap<String, PendingAction>()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS).followRedirects(false).build()

    /** Canonical, local-only requests: no vault, model, network or app scanning. */
    fun fastRoute(canonical:String,language:AppLanguage):AssistantReply?=when(canonical){
        "battery"->{
            val level=context.getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            AssistantReply(if(level in 0..100)language.choose("Battery level is $level percent.","La batería está al $level por ciento.","Battery $level%") else "Android did not report a battery level.")
        }
        "what time is it"->{val time=LocalDateTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.forLanguageTag(language.tag)));AssistantReply(language.choose("It's $time.","Son las $time.",time))}
        else->null
    }

    suspend fun route(text: String, language: AppLanguage): AssistantReply? {
        com.jarvis.assistant.contacts.ContactShortcutPolicy.target(text)?.let{target->
            if(locked())return AssistantReply(unlockMessage(language))
            val contacts=(context.applicationContext as com.jarvis.assistant.JarvisApplication).runtime.contacts
            val c=withContext(Dispatchers.IO){contacts.resolve(target)} ?: return AssistantReply(language.choose("Choose and approve this nickname in Contact shortcuts first.","Elige y aprueba este alias en Atajos de contactos primero.","ابتدا این نام مستعار را در میانبر مخاطبین انتخاب و تأیید کنید."))
            val payload=org.json.JSONObject().put("id",c.id).put("number",c.number).toString()
            return proposal(language.choose("Open Android dialer for ${c.nickname}: ${c.number}? Review and press Call there. No call is placed automatically.","¿Abrir marcador Android para ${c.nickname}: ${c.number}? Revisa y pulsa Llamar allí. No se llama automáticamente.","شماره‌گیر اندروید برای ${c.nickname}: ${c.number} باز شود؟ شماره را بررسی و تماس را آنجا تأیید کنید. تماس خودکار برقرار نمی‌شود."),"dial_contact",payload,confirmedEntity=target)
        }
        if(!locked())infinity.route(text,language,::proposal)?.let{return it}
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
        MediaCommand.parse(text)?.let { command->
            if(locked())return AssistantReply(unlockMessage(language))
            return proposal(language.choose("Send media '$command' to Android's active player?","¿Enviar '$command' al reproductor activo de Android?","درخواست رسانهٔ '$command' به پخش‌کنندهٔ فعال اندروید ارسال شود؟"), "media_control",command)
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
        val headline=Regex("^(?:tell me more about|more about|amplía|mas sobre|بیشتر درباره) (?:the )?(first|second|third|1|2|3|primero|segundo|اول|دوم|سوم)(?: headline| noticia| خبر)?[.!؟?]*$",RegexOption.IGNORE_CASE).matchEntire(text.trim())
        if(headline!=null){val articles=recentNews?.takeIf{System.currentTimeMillis()-it.first in 0..600_000}?.second;val offset=when(headline.groupValues[1].lowercase(Locale.ROOT)){"first","1","primero","اول"->0;"second","2","segundo","دوم"->1;else->2};val article=articles?.getOrNull(offset)
            return if(article==null)AssistantReply("Fetch a fresh headline list first.") else AssistantReply("${article.title}\n${article.publisher} · ${article.publishedAt ?: "publication date unavailable"}\nThis is the source headline, not a full article summary. Tap the source to read it, or ask: research ${article.url}",listOf(SourceLink(article.title,article.url)))
        }
        if (LocalIntentParser.isNews(text)) return news(text, language)
        if (UtilityIntentParser.isWeather(text) && com.jarvis.assistant.sentinel.WeatherQuestion.isRequest(text)) {
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
            val schedule = com.jarvis.assistant.infinity.AlarmDatePolicy.parse(text,java.time.ZonedDateTime.now()) ?: return AssistantReply(language.choose("Specify an unambiguous time and date, for example ‘alarm tomorrow at 07:30’ or ‘alarm every Friday at 06:35’. Unsupported date phrases need clarification.","Indica hora y fecha claras, por ejemplo ‘alarma mañana a las 07:30’.","زمان و تاریخ روشن بگویید، مثلاً «آلارم فردا ۰۷:۳۰»."))
            val value = String.format(Locale.ROOT, "%02d:%02d", schedule.hour,schedule.minute)
            val payload=org.json.JSONObject().put("hour",schedule.hour).put("minute",schedule.minute).put("date",schedule.requestedDate?.toString().orEmpty()).put("days",org.json.JSONArray(schedule.repeatDays)).put("zone",schedule.zone).toString()
            val detail=if(schedule.requestedDate!=null)"Requested date: ${schedule.requestedDate}. Android’s portable clock intent cannot set a one-off date: choose and verify it manually in your Clock app; this is not a repeating alarm." else if(schedule.repeatDays.isNotEmpty())"Explicit repeat days (Android Sunday=1): ${schedule.repeatDays}. Review in Clock." else "Review the next occurrence in Clock."
            return proposal(language.choose("Open Clock for $value (${schedule.zone})?", "¿Abrir Reloj a las $value (${schedule.zone})?", "ساعت برای $value (${schedule.zone}) باز شود؟")+"\n"+detail, "set_alarm", payload)
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
                return try { withLaunch("com.google.android.youtube"); AssistantReply("YouTube opening requested through Android.") }
                catch (_: Exception) { AssistantReply("Android could not open YouTube.") }
            }
            val url = if (youtube.isBlank()) "https://www.youtube.com" else "https://www.youtube.com/results?search_query=${encode(youtube)}"
            return proposal(language.choose(if (youtube.isBlank()) "Open YouTube?" else "Search YouTube for “$youtube”?", if (youtube.isBlank()) "¿Abrir YouTube?" else "¿Buscar «$youtube» en YouTube?", if (youtube.isBlank()) "یوتیوب باز شود؟" else "«$youtube» را در یوتیوب جستجو کنم؟"), "open_url", url)
        }
        val appName = com.jarvis.assistant.neural.AppMatching.commandTarget(text) ?: LocalIntentParser.appName(text)
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
                "dial_contact" -> {
                    val payload=org.json.JSONObject(action.payload)
                    val runtime=(context.applicationContext as com.jarvis.assistant.JarvisApplication).runtime
                    val c=withContext(Dispatchers.IO){runtime.contacts.validateForDial(payload.getString("id"),payload.getString("number"))}
                    if(locked())return@withContext unlockMessage(language)
                    context.startActivity(Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",c.number,null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    runtime.offerContactFollowUp()
                    return@withContext language.choose("Dialer opening requested. Review and press Call in Android. Call outcome is not verified.","Apertura del marcador solicitada. Revisa y pulsa Llamar en Android. Resultado no verificado.","باز شدن شماره‌گیر درخواست شد. شماره را بررسی و تماس را در اندروید تأیید کنید. نتیجهٔ تماس تأیید نشده است.")
                }
                "infinity_controls" -> {
                    context.startActivity(Intent(context,com.jarvis.assistant.infinity.InfinityControlsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return@withContext "Device controls opening requested. Tests, scans and optional permissions require your action there."
                }
                "system_panel" -> {
                    context.startActivity(com.jarvis.assistant.infinity.SystemPanels.intent(action.payload))
                    return@withContext "AWAITING_USER: Android controls opening requested. Review the setting or accessory there; no change or connection has been verified."
                }
                "live_control" -> check(JarvisAccessibilityService.executeLive(context, action.payload)) { "The control did not accept the action." }
                "open_url" -> {
                    check(ToolPolicy.allowedWebUrl(action.payload))
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.payload)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val resolved = context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
                    check(resolved == null || !ToolPolicy.protectedPackage(resolved))
                    context.startActivity(intent)
                    val draft=Uri.parse(action.payload).host=="wa.me"
                    return@withContext if(draft)language.choose("WhatsApp draft opening requested. Review the recipient and send manually; Jarvis did not send a message.","Se solicitó abrir el borrador de WhatsApp. Revisa el destinatario y envía manualmente; Jarvis no envió ningún mensaje.","باز کردن پیش‌نویس واتساپ درخواست شد؛ گیرنده را بررسی و خودتان ارسال کنید. جارویس پیامی نفرستاده است.")
                    else language.choose("Opening request sent to Android. Check the target app; completion is not verified.","Solicitud de apertura enviada a Android. Comprueba la aplicación; no se verificó su finalización.","درخواست باز کردن به اندروید ارسال شد؛ نتیجه را در برنامه بررسی کنید. تکمیل آن تأیید نشده است.")
                }
                "app_alias" -> {
                    val value=org.json.JSONObject(action.payload)
                    appRegistry.saveAlias(value.getString("alias"),value.getString("package"))
                    return@withContext language.choose("App alias saved. Review or delete it in Neural Core settings.","Alias guardado. Puedes editarlo en Neural Core.","نام مستعار ذخیره شد؛ در تنظیمات هستهٔ عصبی قابل تغییر یا حذف است.")
                }
                "open_app" -> {
                    check(!ToolPolicy.protectedPackage(action.payload))
                    val intent = context.packageManager.getLaunchIntentForPackage(action.payload) ?: error("Application unavailable")
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return@withContext language.choose("App launch requested. Check the foreground app; Android controls whether it opens.","Apertura solicitada. Android controla el inicio; comprueba la aplicación.","باز کردن برنامه درخواست شد؛ نمایش آن را در اندروید بررسی کنید.")
                }
                "nav_home", "nav_back" -> {
                    check(JarvisAccessibilityService.navigate(context, action.kind == "nav_home"))
                }
                "media_control" -> {
                    val audio=context.getSystemService(android.media.AudioManager::class.java)
                    val key=MediaCommand.key(action.payload)
                    audio.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,key))
                    audio.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,key))
                    kotlinx.coroutines.delay(350)
                    return@withContext language.choose("Media request sent to Android. ${if(audio.isMusicActive)"Android reports active media audio." else "Android does not report active media audio."} Track changes are not verified here; check the player.",
                        "Solicitud enviada a Android. ${if(audio.isMusicActive)"Android informa audio multimedia activo." else "Android no informa audio multimedia activo."} Comprueba la pista en el reproductor.",
                        "درخواست به اندروید ارسال شد. ${if(audio.isMusicActive)"اندروید پخش رسانهٔ فعال گزارش می‌کند." else "اندروید پخش رسانهٔ فعال گزارش نمی‌کند."} تغییر آهنگ را در پخش‌کننده بررسی کنید.")
                }
                "reminder_draft" -> {
                    context.startActivity(Intent(Intent.ACTION_INSERT,android.provider.CalendarContract.Events.CONTENT_URI)
                        .putExtra(android.provider.CalendarContract.Events.TITLE,action.payload).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return@withContext "Calendar draft opening requested. Choose the date and save it there; no reminder has been saved by Jarvis."
                }
                "set_timer" -> {
                    val seconds = action.payload.toInt().also { check(it in 1..86_400) }
                    context.startActivity(Intent(AlarmClock.ACTION_SET_TIMER)
                        .putExtra(AlarmClock.EXTRA_LENGTH, seconds).putExtra(AlarmClock.EXTRA_MESSAGE, "Jarvis")
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, false).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                "set_alarm" -> {
                    val data=org.json.JSONObject(action.payload);val h=data.getInt("hour");val m=data.getInt("minute")
                    check(h in 0..23 && m in 0..59 && data.getString("zone")==java.time.ZoneId.systemDefault().id){"The timezone changed; request the alarm again."}
                    val date=data.getString("date");if(date.isNotEmpty())check(!java.time.LocalDate.parse(date).isBefore(java.time.LocalDate.now()))
                    val days=data.getJSONArray("days");val repeating=ArrayList<Int>((0 until days.length()).map{days.getInt(it).also{d->check(d in 1..7)}})
                    val intent=Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR,h).putExtra(AlarmClock.EXTRA_MINUTES,m)
                        .putExtra(AlarmClock.EXTRA_MESSAGE,if(date.isBlank())"Jarvis" else "Jarvis requested $date — choose date manually")
                        .putExtra(AlarmClock.EXTRA_SKIP_UI,false).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if(repeating.isNotEmpty())intent.putIntegerArrayListExtra(AlarmClock.EXTRA_DAYS,repeating)
                    context.startActivity(intent)
                }
                else -> return@withContext language.choose("That action is not supported.", "Esa acción no es compatible.", "این کار پشتیبانی نمی‌شود.")
            }
            if(action.kind in listOf("set_alarm","set_timer")) language.choose("Clock opening requested. Review and save the alarm or timer there.","Reloj abierto. Revisa y guarda la alarma o el temporizador allí.","ساعت باز شد. زنگ یا تایمر را آنجا بررسی و ذخیره کنید.")
            else language.choose("Done.", "Listo.", "انجام شد.")
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            language.choose("Android couldn't complete that action. Check the app or optional permission and try again.", "Android no pudo completar la acción. Revisa la aplicación o el permiso opcional e inténtalo de nuevo.", "اندروید نتوانست این کار را انجام دهد. برنامه یا مجوز اختیاری را بررسی کنید و دوباره تلاش کنید.")
        }
    }

    fun cancelPendingActions() { pending.clear() }

    private fun proposal(description: String, kind: String, payload: String,confirmedEntity:String?=null): AssistantReply {
        require(kind in com.jarvis.assistant.neural.ToolRegistry.definitions){"Unregistered action"}
        pending.entries.removeAll { !ToolPolicy.confirmationCurrent(it.value.createdAt, System.currentTimeMillis()) }
        val action = PendingAction(description = description, kind = kind, payload = payload)
        pending[action.id] = action
        return AssistantReply(description, action = action,confirmedEntity=confirmedEntity)
    }

    fun requestApp(name:String,language:AppLanguage,alwaysConfirm:Boolean=false):AssistantReply {
        if(locked())return AssistantReply(unlockMessage(language))
        if(!alwaysConfirm)return app(name,language)
        val matches=com.jarvis.assistant.neural.AppMatching.match(name,appRegistry.apps())
        if(matches.isEmpty() || matches.size>1 && matches[1].score>=matches[0].score-.08)return AssistantReply("Choose an unambiguous available app in Neural Core settings.")
        val target=matches.first().app
        return proposal(language.choose("Open ${target.label}?","¿Abrir ${target.label}?","${target.label} باز شود؟"),"open_app",target.packageId)
    }
    fun requestAlias(alias:String,name:String,language:AppLanguage):AssistantReply {
        if(locked())return AssistantReply(unlockMessage(language))
        val matches=com.jarvis.assistant.neural.AppMatching.match(name,appRegistry.apps())
        if(matches.isEmpty() || matches.size>1 && matches[1].score>=matches[0].score-.08)return AssistantReply("Choose the alias target in Neural Core settings.")
        val payload=org.json.JSONObject().put("alias",alias.take(80)).put("package",matches.first().app.packageId).toString()
        return proposal(language.choose("Save '$alias' as ${matches.first().app.label}?","¿Guardar '$alias' como ${matches.first().app.label}?","«$alias» برای ${matches.first().app.label} ذخیره شود؟"),"app_alias",payload)
    }

    private fun app(name: String, language: AppLanguage): AssistantReply {
        if (locked()) return AssistantReply(unlockMessage(language))
        val matches=com.jarvis.assistant.neural.AppMatching.match(name,appRegistry.apps())
        if(matches.isEmpty())return AssistantReply(language.choose("I couldn't find a matching available app.","No encontré una aplicación disponible que coincida.","برنامهٔ در دسترس مطابقی پیدا نشد."))
        val best=matches.first()
        if(matches.size>1 && matches[1].score>=best.score-.08)return AssistantReply(language.choose("Which app: ","¿Qué aplicación: ","کدام برنامه: ")+matches.take(3).joinToString{it.app.label})
        val packageName=best.app.packageId;val label=best.app.label
        if(!best.exact)return proposal(language.choose("Do you mean $label?","¿Te refieres a $label?","منظورتان $label است؟"),"open_app",packageName)
        return try {
            withLaunch(packageName)
            AssistantReply(language.choose("Opening request sent to Android for $label.","Solicitud de apertura enviada a Android para $label.","درخواست باز کردن $label به اندروید ارسال شد."),confirmedEntity=name)
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

    suspend fun news(text: String, language: AppLanguage): AssistantReply {
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
            val articles = withContext(Dispatchers.Default) { NewsParser.parse(bytes, window) }.distinctBy{it.url}.distinctBy{it.title.lowercase(Locale.ROOT)}.take(5)
            recentNews=System.currentTimeMillis() to articles
            if (articles.isEmpty()) AssistantReply(language.choose("I couldn't verify any matching headlines${if (yesterday) " for yesterday" else ""}. Try another topic.", "No pude verificar titulares coincidentes${if (yesterday) " de ayer" else ""}. Prueba otro tema.", "نتوانستم خبر منطبقی${if (yesterday) " از دیروز" else ""} تأیید کنم. موضوع دیگری را امتحان کنید."))
            else {
                val intro = language.choose(if (yesterday) "Here are yesterday's headlines, dated ${window!!.date}:" else "Here are the latest available headlines:", if (yesterday) "Estos son los titulares de ayer, ${window!!.date}:" else "Estos son los últimos titulares disponibles:", if (yesterday) "تیترهای دیروز، ${window!!.date}:" else "تازه‌ترین تیترهای موجود:")
                AssistantReply(intro + "\n" + articles.mapIndexed { index, article -> "${index + 1}. ${article.title} · ${article.publisher} · ${article.publishedAt ?: "date unavailable"}" }.joinToString("\n") + "\n" + language.choose("These are source headlines, not independently verified reporting. Tap a source for the full story.", "Son titulares de las fuentes, no reportajes verificados de forma independiente. Toca una fuente para leer más.", "این‌ها تیتر منابع هستند و مستقلاً راستی‌آزمایی نشده‌اند. برای متن کامل، منبع را باز کنید."), articles.map { SourceLink(it.title, it.url) })
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
