package com.jarvis.assistant.hud

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class WeatherPayload(val json:JSONObject,val place:String)
/** Cancellable, bounded HTTP, separable from Android storage for real transport tests. */
internal class WeatherClient(
    private val geocodingBase:String="https://geocoding-api.open-meteo.com/v1/search",
    private val forecastBase:String="https://api.open-meteo.com/v1/forecast",
    private val client:OkHttpClient=OkHttpClient.Builder().connectTimeout(12,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).callTimeout(25,TimeUnit.SECONDS).followRedirects(false).build()
) {
    suspend fun fetch(city:String,latitude:Double?,longitude:Double?,forecastDays:Int=1):WeatherPayload {
        require(forecastDays in 1..8)
        val coordinates=if(latitude!=null && longitude!=null){
            require(latitude in -90.0..90.0 && longitude in -180.0..180.0)
            Triple(latitude,longitude,"Approximate location")
        }else{
            val url=geocodingBase.toHttpUrl().newBuilder().addQueryParameter("name",city).addQueryParameter("count","1").build()
            val result=get(url.toString()).optJSONArray("results")?.optJSONObject(0) ?: error("City not found. Try city and country.")
            val lat=result.getDouble("latitude");val lon=result.getDouble("longitude")
            check(lat in -90.0..90.0 && lon in -180.0..180.0){"Weather coordinates invalid"}
            Triple(lat,lon,listOf(result.getString("name"),result.optString("country")).filter(String::isNotBlank).joinToString(", "))
        }
        val url=forecastBase.toHttpUrl().newBuilder().addQueryParameter("latitude",coordinates.first.toString()).addQueryParameter("longitude",coordinates.second.toString())
            .addQueryParameter("current","temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m")
            .addQueryParameter("hourly","temperature_2m,precipitation_probability").addQueryParameter("daily",if(forecastDays==1)"sunrise,sunset" else "sunrise,sunset,temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code")
            .addQueryParameter("forecast_days",forecastDays.toString()).addQueryParameter("timezone","auto").build()
        return WeatherPayload(get(url.toString()),coordinates.third)
    }
    private suspend fun get(url:String):JSONObject=suspendCancellableCoroutine { continuation ->
        val call=client.newCall(Request.Builder().url(url).build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object:Callback {
            override fun onFailure(call:Call,e:IOException){if(continuation.isActive)continuation.resumeWithException(e)}
            override fun onResponse(call:Call,response:Response){
                val result=runCatching { response.use {
                    check(it.isSuccessful){"Weather service returned HTTP ${it.code}. Retry later."}
                    val body=it.body ?: error("Empty weather response")
                    check(body.contentLength()<=262144){"Weather response too large"}
                    val bytes=body.byteStream().use { stream ->
                        val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(4096)
                        while(true){val n=stream.read(buffer);if(n<0)break;check(out.size()+n<=262144){"Weather response too large"};out.write(buffer,0,n)};out.toByteArray()
                    }
                    JSONObject(String(bytes,Charsets.UTF_8))
                } }
                if(continuation.isActive)result.fold({continuation.resume(it)},{continuation.resumeWithException(it)})
            }
        })
    }
}
