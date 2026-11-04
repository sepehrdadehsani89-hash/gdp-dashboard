package com.jarvis.assistant.rebirth

import org.json.JSONObject

/** Vosk whole-hypothesis scores are not probabilities. Only word confidences use that scale. */
data class RecognitionPayload(val primary:JSONObject,val alternatives:List<RecognitionHypothesis>) {
    companion object {
        fun vosk(json:JSONObject):RecognitionPayload {
            val choices=json.optJSONArray("alternatives")
            val best=choices?.optJSONObject(0) ?: json
            val alternatives=if(choices==null)emptyList() else (0 until minOf(5,choices.length())).mapNotNull{i->choices.optJSONObject(i)?.optString("text")?.takeIf{it.isNotBlank()}?.let{RecognitionHypothesis(it.take(4000))}}
            return RecognitionPayload(best,alternatives)
        }
    }
}
