package com.jarvis.assistant.hud

import android.content.Context
import org.json.JSONObject

/** Open-Meteo is contacted only after an explicit city or approximate-location choice. */
class WeatherRepository(context: Context) {
    private val cache=context.getSharedPreferences("jarvis_weather_v2",Context.MODE_PRIVATE)
    private val client=WeatherClient()
    fun clear(){cache.edit().clear().apply()}
    fun cached(key:String): WeatherReading? = if(cache.getString("key",null)==key) runCatching {
        parse(JSONObject(cache.getString("json",null)!!),cache.getString("place","").orEmpty(),cache.getLong("at",0))
    }.getOrNull() else null
    suspend fun fetch(city:String, latitude:Double?=null, longitude:Double?=null): WeatherReading {
        val payload=client.fetch(city,latitude,longitude,8);val now=System.currentTimeMillis()
        val reading=parse(payload.json,payload.place,now)
        // Approximate coordinates are deliberately not persisted.
        if(latitude==null)cache.edit().putString("key",city).putString("place",payload.place).putString("json",payload.json.toString()).putLong("at",now).apply()
        return reading
    }
    companion object {
        const val MAX_AGE=30*60*1000L
        fun stale(reading:WeatherReading?, now:Long):Boolean {
            if(reading==null || now<reading.fetchedAt || now-reading.fetchedAt>=MAX_AGE)return true
            val observed=runCatching{java.time.LocalDateTime.parse(reading.observationTime).atZone(java.time.ZoneId.of(reading.sourceTimezone)).toInstant().toEpochMilli()}.getOrNull()
            return observed!=null && (now-observed>60*60*1000L || observed-now>15*60*1000L)
        }
        fun parse(json:JSONObject,place:String,now:Long):WeatherReading {
            check(!json.optBoolean("error",false)) { json.optString("reason","Weather unavailable") }
            val c=json.optJSONObject("current") ?: error("Current weather is unavailable")
            val time=c.optString("time"); check(runCatching{java.time.LocalDateTime.parse(time)}.isSuccess) { "Weather timestamp missing or invalid" }
            fun number(key:String)=if(c.has(key)&& !c.isNull(key)) c.optDouble(key).takeIf(Double::isFinite) else null
            val daily=json.optJSONObject("daily"); val hourly=json.optJSONObject("hourly")
            val times=hourly?.optJSONArray("time"); var index=-1
            for(i in 0 until (times?.length() ?: 0)) if(times?.optString(i)?.take(13)==time.take(13)) { index=i; break }
            val rain=hourly?.optJSONArray("precipitation_probability")?.let { if(index>=0 && !it.isNull(index)) it.optInt(index).takeIf { n->n in 0..100 } else null }
            fun first(key:String)=daily?.optJSONArray(key)?.optString(0)?.takeIf { it.isNotBlank() && it!="null" }
            fun arrayNumber(section:JSONObject?,key:String,i:Int):Double? {
                val a=section?.optJSONArray(key) ?: return null
                return if(i !in 0 until a.length() || a.isNull(i)) null else a.optDouble(i).takeIf(Double::isFinite)
            }
            fun probability(value:Double?)=value?.takeIf{it in 0.0..100.0}?.toInt()
            val hours=(0 until (times?.length() ?: 0)).mapNotNull { i ->
                val stamp=times?.optString(i).orEmpty()
                if(stamp<time || runCatching{java.time.LocalDateTime.parse(stamp)}.isFailure) null
                else WeatherHour(stamp,arrayNumber(hourly,"temperature_2m",i),probability(arrayNumber(hourly,"precipitation_probability",i)))
            }.take(12)
            val dates=daily?.optJSONArray("time")
            val days=(0 until minOf(dates?.length() ?: 0,8)).mapNotNull { i ->
                val stamp=dates?.optString(i).orEmpty()
                if(runCatching{java.time.LocalDate.parse(stamp)}.isFailure) null
                else WeatherDay(stamp,arrayNumber(daily,"temperature_2m_max",i),arrayNumber(daily,"temperature_2m_min",i),probability(arrayNumber(daily,"precipitation_probability_max",i)))
            }
            return WeatherReading(place,number("temperature_2m"),number("apparent_temperature"),number("relative_humidity_2m")?.toInt(),number("wind_speed_10m"),rain,first("sunrise"),first("sunset"),number("weather_code")?.toInt(),time,now,hours,days,json.optString("timezone").ifBlank{"Unavailable"})
        }
    }
}
