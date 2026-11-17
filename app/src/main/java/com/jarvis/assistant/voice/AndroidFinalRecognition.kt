package com.jarvis.assistant.voice

/** The platform may omit confidence entirely (-1). Never invent a score from RMS or text. */
internal object AndroidFinalRecognition {
    fun kind(text: String, confidence: Float?, threshold: Float): FinalRecognitionKind = when {
        text.isBlank() -> FinalRecognitionKind.EMPTY
        !threshold.isFinite() || threshold !in 0f..1f -> FinalRecognitionKind.UNCERTAIN
        confidence == null || confidence == -1f -> FinalRecognitionKind.ACCEPTED
        !confidence.isFinite() || confidence !in 0f..1f -> FinalRecognitionKind.UNCERTAIN
        confidence < threshold -> FinalRecognitionKind.UNCERTAIN
        else -> FinalRecognitionKind.ACCEPTED
    }
    fun metadata(text: String, confidence: Float?, threshold: Float): String {
        val score = when {
            confidence == null || confidence == -1f -> "not supplied by Android"
            confidence.isFinite() && confidence in 0f..1f -> "${(confidence * 100).toInt()}%"
            else -> "invalid"
        }
        return "Android final · ${kind(text, confidence, threshold)} · confidence $score · threshold ${(threshold * 100).toInt()}%"
    }
}
