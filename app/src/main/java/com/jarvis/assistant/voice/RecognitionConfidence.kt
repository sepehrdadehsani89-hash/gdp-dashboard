package com.jarvis.assistant.voice

import org.json.JSONObject

enum class FinalRecognitionKind { EMPTY, ACCEPTED, UNCERTAIN }

/** Vosk's final word confidences, when available. No invented multilingual confidence score. */
object RecognitionConfidence {
    fun kind(json:JSONObject,threshold:Double=.55)=when {
        json.optString("text").isBlank()->FinalRecognitionKind.EMPTY
        acceptsFinal(json,threshold)->FinalRecognitionKind.ACCEPTED
        else->FinalRecognitionKind.UNCERTAIN
    }
    /** Exact public UI controls only. A call still requires explicit pickup; contact calls,
     * compounds, device controls and owner verification keep their existing gates. */
    fun publicControl(text:String):Boolean =
        com.jarvis.assistant.call.AiCallPolicy.command(text)==com.jarvis.assistant.call.AiCallCommand.START ||
        com.jarvis.assistant.core.LanguageRules.languageRequest(text)!=null ||
        com.jarvis.assistant.core.AppLanguage.supported.any{WakePhraseMatcher.extractCommand(text,it)==""}
    fun acceptsFinal(json:JSONObject,threshold:Double=.55):Boolean {
        if(json.optString("text").isBlank())return false
        val words=json.optJSONArray("result") ?: return true
        if(words.length()==0)return true
        var total=0.0
        var supplied=0
        for(i in 0 until words.length()){
            val word=words.optJSONObject(i) ?: return false
            // Vosk NbestResult emits word/start/end without conf; its whole-hypothesis
            // likelihood is not a probability. Missing scores are not failed recognition.
            if(!word.has("conf")){
                if(word.optString("word").isBlank())return false
                continue
            }
            val value=word.optDouble("conf",Double.NaN)
            if(!value.isFinite() || value !in 0.0..1.0)return false
            total+=value;supplied++
        }
        if(supplied==0)return true // Valid N-best final: intent/owner/confirmation gates still run.
        if(supplied!=words.length())return false // Reject malformed mixed score schemas.
        // Reading public device facts is harmless even when one short function word has
        // low acoustic confidence. This does not relax app/action or speaker-owner checks.
        return total/words.length()>=threshold || LocalVoiceQueries.readOnly(json.optString("text")) || LocalVoiceQueries.ordinaryQuestion(json.optString("text")) || publicControl(json.optString("text"))
    }
}
