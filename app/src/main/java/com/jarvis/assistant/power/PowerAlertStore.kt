package com.jarvis.assistant.power

import org.json.JSONObject

/** Minimal alert/session metadata only; no audio, conversation, files or secrets. */
object PowerAlertCodec {
    fun encode(s:PowerAlertState):String=JSONObject().apply{
        put("percent",s.percent ?: JSONObject.NULL);put("connected",s.connected ?: JSONObject.NULL)
        put("episode",s.episode);put("mask",s.mask);put("session",s.session)
        put("start_done",s.startDone);put("full_done",s.fullDone);put("estimate_done",s.estimateDone)
        put("dismissed",s.dismissed);put("snooze",s.snoozeUntil);put("reminder",s.reminderAt)
        put("critical",s.criticalPending ?: JSONObject.NULL);put("disconnect",s.disconnectPending)
        put("progress",s.progressAt)
        put("reliable",s.reliable)
        put("twenty_pending",s.twentyPending);put("twenty_done",s.twentyDone)
        put("attempts",JSONObject().apply{s.attempts.entries.take(16).forEach{(key,a)->put(key,JSONObject().put("count",a.count).put("retry",a.retryAt))}})
    }.toString()
    fun decode(raw:String):PowerAlertState?=runCatching{
        require(raw.length<=16_000)
        val j=JSONObject(raw);val attempts=j.optJSONObject("attempts")
        PowerAlertState(j.optInt("percent",-1).takeIf{it in 0..100},j.takeUnless{it.isNull("connected")}?.optBoolean("connected"),
            j.optLong("episode").coerceAtLeast(0),j.optInt("mask") and 2046,j.optLong("session").coerceAtLeast(0),
            j.optBoolean("start_done"),j.optBoolean("full_done"),j.optBoolean("estimate_done"),j.optBoolean("dismissed"),
            j.optLong("snooze").coerceAtLeast(0),j.optLong("reminder").coerceAtLeast(0),j.optInt("critical",-1).takeIf{it in 1..10},j.optBoolean("disconnect"),
            attempts?.keys()?.asSequence()?.take(16)?.filter{Regex("(?:(?:critical|charge):[0-9]+:[a-z0-9:]+|low20:[0-9]+)").matches(it)}?.mapNotNull{key->attempts.optJSONObject(key)?.let{key to PowerAttempt(it.optInt("count").coerceIn(0,3),it.optLong("retry").coerceAtLeast(0))}}?.toMap().orEmpty(),j.optLong("progress").coerceAtLeast(0),j.optBoolean("reliable"),j.optBoolean("twenty_pending"),j.optBoolean("twenty_done"))
    }.getOrNull()
}
