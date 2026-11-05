package com.jarvis.assistant.security

import com.jarvis.assistant.core.AppLanguage
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

object LockCountdown {
    fun announcement(language:AppLanguage)=when(language){
        AppLanguage.ES->"Bloqueo iniciado, bloqueando tu pantalla en 3, 2, 1."
        AppLanguage.FA->"قفل آغاز شد. صفحه در ۳، ۲، ۱ قفل می‌شود."
        else->"Locking initiated, locking your screen in 3, 2, 1."
    }
    /** Playback completion is authoritative; failed speech uses a real three-second visual timer. */
    suspend fun run(current:()->Boolean,announce:suspend ()->Boolean,tick:(Int)->Unit,lock:()->Unit):Boolean {
        if(!current())return false
        val spoken=announce()
        currentCoroutineContext().ensureActive()
        if(!spoken)for(value in 3 downTo 1){
            if(!current())return false
            tick(value);delay(1000)
        }
        currentCoroutineContext().ensureActive()
        if(!current())return false
        lock();return true
    }
}
