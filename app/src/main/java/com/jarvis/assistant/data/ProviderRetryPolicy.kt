package com.jarvis.assistant.data

import com.jarvis.assistant.brain.ProviderFailureKind
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Only idempotent catalog GETs may retry. Inference POSTs are never automatically replayed. */
object ProviderRetryPolicy {
    fun retryAfter(value:String?,now:Long=System.currentTimeMillis()):Long? {
        if(value.isNullOrBlank() || value.length>100)return null
        val numeric=value.trim().toLongOrNull()
        if(numeric!=null)return numeric.takeIf{it>=0}?.coerceAtMost(86400)?.times(1000)
        return runCatching{(ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()-now).coerceIn(0L,86400000L)}.getOrNull()
    }
    fun delayForSafeRead(attempt:Int,error:ProviderException,jitter:Long=0L):Long? {
        if(attempt !in 0..1 || error.kind in setOf(ProviderFailureKind.CREDITS,ProviderFailureKind.AUTHENTICATION,ProviderFailureKind.AUTHORIZATION,ProviderFailureKind.MODEL_UNAVAILABLE))return null
        if(error.httpStatus !in setOf(429,500,502,503,504))return null
        val requested=error.retryAfterMs
        if(requested!=null && requested>2000)return null
        return maxOf(requested ?: 0L,(300L shl attempt)+jitter.coerceIn(0,200))
    }
}
