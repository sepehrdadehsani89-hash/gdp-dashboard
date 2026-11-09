package com.jarvis.assistant.voice

import kotlin.math.sqrt

enum class OwnerVoiceResult { MATCH, DIFFERENT_VOICE, AMBIGUOUS, UNAVAILABLE }
data class OwnerVoiceDecision(val result:OwnerVoiceResult,val similarity:Float?=null)

/** Development speaker-similarity thresholds, not a security credential or Android authentication. */
object OwnerVoicePolicy {
    fun calibratedThreshold(score:Float):Float {
        require(score.isFinite() && score in .72f..1f)
        return (score-.04f).coerceIn(.72f,.90f)
    }
    fun decide(query:FloatArray,owner:FloatArray,guests:List<FloatArray> = emptyList(),threshold:Float=.90f,margin:Float=.08f):OwnerVoiceDecision {
        require(threshold.isFinite() && threshold in 0.72f..0.95f && margin in 0f..1f)
        fun cosine(a:FloatArray,b:FloatArray):Float? {
            if(a.size!=b.size || a.size !in 16..1024 || a.any{!it.isFinite()} || b.any{!it.isFinite()})return null
            val norm=sqrt(a.sumOf{it.toDouble()*it}*b.sumOf{it.toDouble()*it})
            if(norm<=1e-10)return null
            return (a.indices.sumOf{a[it].toDouble()*b[it]}/norm).toFloat().coerceIn(-1f,1f)
        }
        val score=cosine(query,owner) ?: return OwnerVoiceDecision(OwnerVoiceResult.UNAVAILABLE)
        if(score<threshold)return OwnerVoiceDecision(OwnerVoiceResult.DIFFERENT_VOICE,score)
        val other=guests.mapNotNull{cosine(query,it)}.maxOrNull()
        if(other!=null && score-other<margin)return OwnerVoiceDecision(OwnerVoiceResult.AMBIGUOUS,score)
        return OwnerVoiceDecision(OwnerVoiceResult.MATCH,score)
    }
}

/** In-process, short-lived proof for this voice request, revoked by a new session or template change. */
class OwnerVoicePermit private constructor(private val generation:Long,private val revision:Long,private val expires:Long){
    fun valid(currentGeneration:Long,currentRevision:Long,now:Long)=generation==currentGeneration && revision==currentRevision && now in (expires-30_000)..expires
    internal fun afterDeviceAuthentication(nextGeneration:Long,currentRevision:Long,now:Long):OwnerVoicePermit?=
        if(revision==currentRevision && now in (expires-30_000)..expires)OwnerVoicePermit(nextGeneration,revision,expires)else null
    companion object {internal fun issued(generation:Long,revision:Long,now:Long)=OwnerVoicePermit(generation,revision,now+30_000)}
}
