package com.jarvis.assistant.rebirth

import com.jarvis.assistant.core.ChatMessage

enum class MicrophoneOwner { WAKE, FOREGROUND_RECOGNIZER, PRIVATE_CAPTURE }
data class MicrophoneLease(val id:Long,val owner:MicrophoneOwner)
/** A lease is released by the recorder's finally block, never by its result callback. */
class MicrophoneSessionManager {
    private var next=0L
    private var current:MicrophoneLease?=null
    @Synchronized fun acquire(owner:MicrophoneOwner):MicrophoneLease? {
        if(current!=null)return null
        return MicrophoneLease(++next,owner).also{current=it}
    }
    @Synchronized fun release(lease:MicrophoneLease){if(current==lease)current=null}
    @Synchronized fun isCurrent(lease:MicrophoneLease)=current==lease
    @Synchronized fun snapshot()=current
}
object MicrophoneSessions { val manager=MicrophoneSessionManager() }

/** No private payloads in recovery diagnostics. A healthy interval replenishes a small retry budget. */
class RecoveryBudget(private val now:()->Long,private val maxAttempts:Int=2) {
    private var failures=0
    private var healthySince:Long?=null
    fun healthy(){val start=healthySince ?: now().also{healthySince=it};if(now()-start>=5000)failures=0}
    fun retryDelay():Long? {healthySince=null;if(failures>=maxAttempts)return null;return listOf(400L,1200L,2500L)[(failures++).coerceAtMost(2)]}
}

object LiveConversationPolicy {
    const val MAX_MESSAGES=24
    const val MAX_CHARACTERS=24_000
    const val IDLE_EXPIRY_MS=45_000L
    fun bounded(items:List<ChatMessage>):List<ChatMessage>{
        val queue=ArrayDeque(items.takeLast(MAX_MESSAGES).map{it.copy(text=it.text.take(8000),sources=it.sources.take(8).map{source->source.copy(title=source.title.take(500),url=source.url.take(2048))})})
        while(queue.sumOf{it.text.length}>MAX_CHARACTERS && queue.isNotEmpty())queue.removeFirst()
        return queue.toList()
    }
}

enum class RecoveryState { HEALTHY, DEGRADED, RECOVERING, USER_ACTION_REQUIRED, UNAVAILABLE }
