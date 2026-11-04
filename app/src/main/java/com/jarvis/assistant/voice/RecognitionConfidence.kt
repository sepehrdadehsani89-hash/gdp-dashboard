package com.jarvis.assistant.voice

import org.json.JSONObject

/** Vosk's final word confidences, when available. No invented multilingual confidence score. */
object RecognitionConfidence {
    fun acceptsFinal(json:JSONObject,threshold:Double=.55):Boolean {
        if(json.optString("text").isBlank())return false
        val words=json.optJSONArray("result") ?: return true
        if(words.length()==0)return true
        var total=0.0
        for(i in 0 until words.length()){
            val value=words.optJSONObject(i)?.optDouble("conf",Double.NaN) ?: return false
            if(!value.isFinite() || value !in 0.0..1.0)return false
            total+=value
        }
        return total/words.length()>=threshold
    }
}
