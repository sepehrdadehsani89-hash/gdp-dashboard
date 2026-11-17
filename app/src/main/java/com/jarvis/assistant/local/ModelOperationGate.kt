package com.jarvis.assistant.local

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job

/** Registers whole operations, including hashing/loading and time spent waiting for a model lock. */
internal class ModelOperationGate {
    private val lock = Any()
    private val active = linkedSetOf<Job>()
    private var closed = false

    suspend fun <T> operation(block: suspend () -> T): T = coroutineScope {
        val job = currentCoroutineContext().job
        synchronized(lock) {
            check(!closed) { "Local Brain management is in progress. Please wait for it to finish." }
            active += job
        }
        try { block() } finally { synchronized(lock) { active -= job } }
    }

    /** Caller serializes management and must reopen admission in finally, after joining these jobs. */
    fun closeAdmission(): List<Job> = synchronized(lock) {
        check(!closed) { "Local Brain management is already in progress." }
        closed = true
        active.toList()
    }

    fun reopen() = synchronized(lock) { closed = false }
}
