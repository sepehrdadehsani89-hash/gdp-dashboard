package com.jarvis.assistant.rebirth

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionNotePreferences(context:Context){
    private val p=context.getSharedPreferences("jarvis_rebirth_session_notes",Context.MODE_PRIVATE)
    // Requested explicitly by this release's owner; disclosed next to the live conversation.
    private val mutable=MutableStateFlow(p.getBoolean("enabled",true))
    val enabled=mutable.asStateFlow()
    fun setEnabled(value:Boolean){check(p.edit().putBoolean("enabled",value).commit());mutable.value=value}
}

/** Extractive, bounded ordinary-dialogue notes. No model call, raw chat archive or private tool text. */
class SessionNoteDraft {
    private val turns=ArrayDeque<String>()
    @Synchronized fun record(question:String,answer:String,ordinary:Boolean){
        if(!ordinary || !safe(question) || !safe(answer))return
        fun compact(s:String,n:Int)=s.replace(Regex("\\s+")," ").trim().take(n)
        turns.addLast("Topic: ${compact(question,180)}; assistant excerpt (unverified): ${compact(answer,220)}")
        while(turns.size>6)turns.removeFirst()
    }
    @Synchronized fun takeSummary():String? {
        if(turns.isEmpty())return null
        val note="Local session summary · user-approved ordinary conversation excerpts, not verified facts:\n"+turns.joinToString("\n").takeLast(1700)
        turns.clear();return note
    }
    @Synchronized fun clear(){turns.clear()}
    companion object {
        private val credentials=Regex("(?i)password|passphrase|passcode|contrase[nñ]a|\\b(?:pin|token|secret|credential|bearer|clave|codigo|código)\\b|api[ _-]?key|private[ _-]?key|-----BEGIN|\\b(?:sk-|gh[pousr]_|AIza)|[A-Za-z0-9_-]{32,}|(?:\\d[ -]?){13,19}|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}|(?:content|file)://")
        fun safe(text:String)=text.isNotBlank() && !credentials.containsMatchIn(text)
    }
}
