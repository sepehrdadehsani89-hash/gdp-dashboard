package com.jarvis.assistant.tools

import com.jarvis.assistant.core.AppLanguage
import com.jarvis.assistant.core.AssistantReply
import com.jarvis.assistant.core.SourceLink
import com.jarvis.assistant.core.choose
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/** Explicit-city forecasts; no location permission, device coordinates, or provider key needed. */
class WeatherTool {
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).followRedirects(false).build()

    suspend fun reply(city: String, language: AppLanguage): AssistantReply {
        return try {
            val geocode = HttpUrl.Builder().scheme("https").host("geocoding-api.open-meteo.com").addPathSegments("v1/search")
                .addQueryParameter("name", city).addQueryParameter("count", "1")
                .addQueryParameter("language", language.code).addQueryParameter("format", "json").build()
            val locations = JSONObject(fetch(geocode)).optJSONArray("results")
            val location = locations?.optJSONObject(0) ?: return AssistantReply(language.choose("I couldn't find that city. Try its name and country.", "No encontré esa ciudad. Prueba su nombre y país.", "آن شهر را پیدا نکردم. نام شهر و کشور را بگویید."))
            val latitude = location.getDouble("latitude")
            val longitude = location.getDouble("longitude")
            check(latitude in -90.0..90.0 && longitude in -180.0..180.0)
            val forecast = HttpUrl.Builder().scheme("https").host("api.open-meteo.com").addPathSegments("v1/forecast")
                .addQueryParameter("latitude", latitude.toString()).addQueryParameter("longitude", longitude.toString())
                .addQueryParameter("current", "temperature_2m,apparent_temperature,weather_code,wind_speed_10m")
                .addQueryParameter("temperature_unit", "celsius").addQueryParameter("wind_speed_unit", "kmh")
                .addQueryParameter("timezone", "auto").build()
            val data = JSONObject(fetch(forecast))
            val place = listOf(location.getString("name"), location.optString("admin1"), location.optString("country")).filter(String::isNotBlank).distinct().joinToString(", ")
            AssistantReply(WeatherSummary.describe(place, data, language), listOf(SourceLink("Open-Meteo weather data", "https://open-meteo.com/")))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { AssistantReply(language.choose("I couldn't retrieve the weather. Check your connection and try again.", "No pude consultar el clima. Comprueba tu conexión e inténtalo de nuevo.", "اطلاعات هوا را دریافت نکردم. اتصال اینترنت را بررسی کنید و دوباره تلاش کنید.")) }
    }

    private suspend fun fetch(url: HttpUrl): String {
        require(url.host in setOf("geocoding-api.open-meteo.com", "api.open-meteo.com") && url.scheme == "https")
        val call = client.newCall(Request.Builder().url(url).build())
        val handle = currentCoroutineContext().job.invokeOnCompletion { if (it is CancellationException) call.cancel() }
        return try {
            withContext(Dispatchers.IO) {
                call.execute().use { response ->
                    check(response.isSuccessful)
                    val body = response.body ?: error("Empty weather response")
                    check(body.contentLength() <= 200_000)
                    body.source().use { source ->
                        source.request(200_001)
                        check(source.buffer.size <= 200_000)
                        source.buffer.readUtf8()
                    }
                }
            }
        } finally { handle.dispose() }
    }
}

object WeatherSummary {
    fun describe(place: String, data: JSONObject, language: AppLanguage): String {
        val current = data.getJSONObject("current")
        val temperature = current.getDouble("temperature_2m")
        val feelsLike = current.getDouble("apparent_temperature")
        val wind = current.getDouble("wind_speed_10m")
        require(temperature.isFinite() && feelsLike.isFinite() && wind.isFinite())
        val condition = condition(current.getInt("weather_code"), language)
        val time = current.optString("time").replace('T', ' ')
        val temp = String.format(java.util.Locale.ROOT, "%.0f", temperature)
        val feels = String.format(java.util.Locale.ROOT, "%.0f", feelsLike)
        val speed = String.format(java.util.Locale.ROOT, "%.0f", wind)
        return language.choose(
            "$place: $condition, $temp degrees Celsius, feels like $feels. Wind $speed kilometers per hour. Forecast update: $time. Source: Open-Meteo.",
            "$place: $condition, $temp grados Celsius, sensación de $feels. Viento de $speed kilómetros por hora. Actualización: $time. Fuente: Open-Meteo.",
            "$place: $condition، دما $temp درجه سانتی‌گراد و دمای محسوس $feels درجه است. سرعت باد $speed کیلومتر در ساعت. زمان به‌روزرسانی: $time. منبع: اوپن متئو.",
        )
    }

    private fun condition(code: Int, language: AppLanguage): String = when (code) {
        0 -> language.choose("clear", "despejado", "صاف")
        1, 2 -> language.choose("partly cloudy", "parcialmente nublado", "کمی ابری")
        3 -> language.choose("overcast", "nublado", "ابری")
        45, 48 -> language.choose("foggy", "con niebla", "مه‌آلود")
        51, 53, 55, 56, 57 -> language.choose("drizzle", "llovizna", "نم‌نم باران")
        61, 63, 65, 66, 67, 80, 81, 82 -> language.choose("rain", "lluvia", "بارانی")
        71, 73, 75, 77, 85, 86 -> language.choose("snow", "nieve", "برفی")
        95, 96, 99 -> language.choose("thunderstorms", "tormentas", "رعد و برق")
        else -> language.choose("conditions unavailable", "condiciones no disponibles", "وضعیت نامشخص")
    }
}
