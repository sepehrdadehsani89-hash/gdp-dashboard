package com.jarvis.assistant.core

import android.Manifest
import android.app.KeyguardManager
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.CalendarContract
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Explicit, permission-gated, local-only phone information. This class makes no network calls. */
class PhoneContext(context: Context,private val currentLocation:suspend ()->Location = {com.jarvis.assistant.omnisystem.LocationFix(context).current()}) {
    private val app = context.applicationContext

    fun requiredPermission(text: String): String? = when (PhoneRequestParser.parse(text)) {
        is PhoneRequest.Contacts -> Manifest.permission.READ_CONTACTS
        is PhoneRequest.Calendar -> Manifest.permission.READ_CALENDAR
        PhoneRequest.Location -> Manifest.permission.ACCESS_COARSE_LOCATION
        null -> null
    }

    suspend fun route(text: String, language: AppLanguage): AssistantReply? {
        val request = PhoneRequestParser.parse(text) ?: return null
        return withContext(Dispatchers.IO) {
            if (!isUnlocked()) return@withContext unlockReply(language)
            val permission = requiredPermission(text)
            if (permission != null && !hasPermission(permission)) return@withContext permissionReply(request, language)
            try {
                val reply = when (request) {
                    is PhoneRequest.Contacts -> contacts(request.query, language)
                    is PhoneRequest.Calendar -> calendar(request.days, language)
                    PhoneRequest.Location -> location(language)
                }
                // A query may outlive an unlock or permission grant. Do not return stale authorization.
                when {
                    !isUnlocked() -> unlockReply(language)
                    permission != null && !hasPermission(permission) -> permissionReply(request, language)
                    else -> reply
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                permissionReply(request, language)
            } catch (_: Exception) {
                AssistantReply(language.choose(
                    "I couldn't read that phone information. Check the feature permission and try again.",
                    "No pude leer esa información. Revisa el permiso de la función e inténtalo de nuevo.",
                    "خواندن این اطلاعات ممکن نشد. دسترسی مربوط را بررسی کن و دوباره امتحان کن.",
                ))
            }
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(app, permission) == PackageManager.PERMISSION_GRANTED

    private fun isUnlocked(): Boolean = app.getSystemService(KeyguardManager::class.java)?.isDeviceLocked == false

    private fun unlockReply(language: AppLanguage) = AssistantReply(language.choose(
        "Unlock your phone before I read private phone information.",
        "Desbloquea el teléfono antes de que lea información privada.",
        "برای خواندن اطلاعات خصوصی گوشی، اول قفل گوشی را باز کن.",
    ))

    private fun permissionReply(request: PhoneRequest, language: AppLanguage): AssistantReply {
        val feature = when (request) {
            is PhoneRequest.Contacts -> language.choose("Contacts", "Contactos", "مخاطبین")
            is PhoneRequest.Calendar -> language.choose("Calendar", "Calendario", "تقویم")
            PhoneRequest.Location -> language.choose("Location", "Ubicación", "موقعیت مکانی")
        }
        return AssistantReply(language.choose(
            "To read this, grant the optional $feature permission from Jarvis Settings. I won't change anything or upload this information.",
            "Para leerlo, concede el permiso opcional de $feature desde Ajustes de Jarvis. No modificaré ni subiré esa información.",
            "برای خواندن این اطلاعات، دسترسی اختیاری $feature را از تنظیمات جارویس فعال کن. این اطلاعات را تغییر نمی‌دهم و جایی بارگذاری نمی‌کنم.",
        ))
    }

    private fun contacts(query: String, language: AppLanguage): AssistantReply {
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY, ContactsContract.CommonDataKinds.Phone.NUMBER)
        val uri = if (query.isBlank()) ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            else ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI.buildUpon().appendPath(query).build()
        val contacts = linkedSetOf<String>()
        app.contentResolver.query(
            uri,
            projection, null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { cursor ->
            while (contacts.size < 10 && cursor.moveToNext()) {
                val name = cursor.getString(0).orEmpty().trim().take(120)
                val number = cursor.getString(1).orEmpty().trim().take(80)
                if (number.isNotBlank()) contacts += "${name.ifBlank { language.choose("Unnamed contact", "Contacto sin nombre", "مخاطب بدون نام") }}: $number"
            }
        }
        if (contacts.isEmpty()) return AssistantReply(language.choose(
            "I didn't find any matching phone contacts.", "No encontré contactos con teléfono que coincidan.", "مخاطب دارای شماره‌ای با این مشخصات پیدا نکردم.",
        ))
        val heading = if (query.isBlank()) language.choose("Your contacts (up to 10):", "Tus contactos (hasta 10):", "مخاطبین تو (حداکثر ۱۰ مورد):")
        else language.choose("Matching contacts (up to 10):", "Contactos encontrados (hasta 10):", "مخاطبین پیدا‌شده (حداکثر ۱۰ مورد):")
        return AssistantReply(heading + "\n" + contacts.joinToString("\n"))
    }

    private fun calendar(days: Int, language: AppLanguage): AssistantReply {
        val now = ZonedDateTime.now()
        val end = if (days == 1) LocalDate.now().plusDays(1).atStartOfDay(now.zone) else now.plusDays(7)
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now.toInstant().toEpochMilli())
        ContentUris.appendId(builder, end.toInstant().toEpochMilli())
        val projection = arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.ALL_DAY)
        val locale = Locale.forLanguageTag(language.tag)
        val timed = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale).withZone(now.zone)
        val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        val results = mutableListOf<String>()
        app.contentResolver.query(
            builder.build(), projection,
            "${CalendarContract.Instances.VISIBLE} = 1 AND ${CalendarContract.Instances.END} > ?",
            arrayOf(now.toInstant().toEpochMilli().toString()),
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            while (results.size < 6 && cursor.moveToNext()) {
                val title = cursor.getString(0).orEmpty().trim().take(240).ifBlank { language.choose("Untitled event", "Evento sin título", "رویداد بدون عنوان") }
                val start = Instant.ofEpochMilli(cursor.getLong(1))
                val time = if (cursor.getInt(2) != 0) {
                    date.format(start.atZone(ZoneOffset.UTC).toLocalDate()) + language.choose(" (all day)", " (todo el día)", " (تمام روز)")
                } else timed.format(start)
                results += "$time — $title"
            }
        }
        if (results.isEmpty()) return AssistantReply(if (days == 1) language.choose(
            "I found no remaining events today in your visible calendars.", "No encontré más eventos hoy en tus calendarios visibles.", "در تقویم‌های فعال تو رویداد دیگری برای امروز پیدا نکردم.",
        ) else language.choose(
            "I found no upcoming events in the next seven days in your visible calendars.", "No encontré eventos en los próximos siete días en tus calendarios visibles.", "در تقویم‌های فعال تو رویدادی برای هفت روز آینده پیدا نکردم.",
        ))
        val heading = if (days == 1) language.choose("Today's remaining events", "Eventos pendientes de hoy", "رویدادهای باقی‌ماندهٔ امروز")
        else language.choose("Next seven days (up to 6 events)", "Próximos siete días (hasta 6 eventos)", "هفت روز آینده (حداکثر ۶ رویداد)")
        return AssistantReply("$heading • ${now.zone.id}\n" + results.joinToString("\n"))
    }

    private suspend fun location(language: AppLanguage): AssistantReply {
        val fix=currentLocation()
        val coordinates=String.format(Locale.ROOT, "%.2f, %.2f", fix.latitude, fix.longitude)
        val accuracy=String.format(Locale.ROOT, "%.0f",fix.accuracy)
        return AssistantReply(language.choose(
            "Fresh approximate location: $coordinates. Android-reported accuracy: $accuracy metres. Updated: ${Instant.ofEpochMilli(fix.time)}. No separate location history is recorded; conversation-history settings still apply. Address/map lookup has not been performed.",
            "Ubicación aproximada reciente: $coordinates. Precisión indicada por Android: $accuracy metros. Actualizada: ${Instant.ofEpochMilli(fix.time)}. No se guarda un historial separado; se aplica la configuración del historial de conversación. No se consultaron dirección ni mapa.",
            "موقعیت تقریبی تازه: $coordinates. دقت گزارش‌شدهٔ اندروید: $accuracy متر. زمان: ${Instant.ofEpochMilli(fix.time)}. سابقهٔ جداگانهٔ موقعیت ذخیره نمی‌شود؛ تنظیمات سابقهٔ گفتگو اعمال می‌شود. نشانی یا نقشه دریافت نشده است."
        ))
    }

}
