package com.jarvis.assistant.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Volatile metadata only: no transcript, PCM, credentials or persistent content log. */
object RecognitionDiagnostics {
    private val mutable=MutableStateFlow("No final recognition result yet")
    val state=mutable.asStateFlow()
    fun record(json:JSONObject,threshold:Double,source:String){
        val words=json.optJSONArray("result")
        val values=(0 until (words?.length() ?: 0)).map{words?.optJSONObject(it)?.optDouble("conf",Double.NaN) ?: Double.NaN}
        val supplied=(0 until (words?.length() ?: 0)).count{words?.optJSONObject(it)?.has("conf")==true}
        val score=if(words!=null && words.length()>0 && supplied==0)"not supplied by Vosk (N-best)" else if(values.isNotEmpty() && values.all{it.isFinite() && it in 0.0..1.0}) "${(values.average()*100).toInt()}%" else if(values.isEmpty())"not supplied" else "invalid / incomplete"
        mutable.value="$source · ${RecognitionConfidence.kind(json,threshold)} · ${json.optString("text").trim().split(Regex("\\s+")).count{it.isNotBlank()}} words · mean $score · threshold ${(threshold*100).toInt()}%"
    }
}
