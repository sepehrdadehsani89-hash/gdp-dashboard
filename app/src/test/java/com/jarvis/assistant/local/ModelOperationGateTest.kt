package com.jarvis.assistant.local

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

/** Exercises real coroutine cancellation/admission; it does not simulate native model performance. */
class ModelOperationGateTest {
    @Test fun completedOperationsDoNotLeaveStaleJobs() = runBlocking {
        val gate = ModelOperationGate()
        assertEquals(7, gate.operation { 7 })
        assertTrue(gate.closeAdmission().isEmpty())
    }

    @Test fun failedOperationsAreRetiredBeforeManagement() = runBlocking {
        val gate = ModelOperationGate()
        assertTrue(runCatching { gate.operation { error("fixture failure") } }.isFailure)
        assertTrue(gate.closeAdmission().isEmpty())
    }

    @Test fun managementRejectsNewWorkUntilReopened() = runBlocking {
        val gate = ModelOperationGate(); gate.closeAdmission()
        var entered = false
        val failure = runCatching { gate.operation { entered = true } }.exceptionOrNull()
        assertTrue(failure is IllegalStateException); assertFalse(entered)
        gate.reopen(); gate.operation { entered = true }; assertTrue(entered)
    }

    @Test fun allActiveJobsAreIncludedInTheManagementSnapshot() = runBlocking {
        val gate = ModelOperationGate()
        val first = launch(start = CoroutineStart.UNDISPATCHED) { gate.operation { awaitCancellation() } }
        val second = launch(start = CoroutineStart.UNDISPATCHED) { gate.operation { awaitCancellation() } }
        val jobs = gate.closeAdmission(); assertEquals(2, jobs.size)
        jobs.forEach { it.cancel() }; jobs.joinAll(); first.join(); second.join()
        assertTrue(first.isCancelled && second.isCancelled)
    }

    @Test fun cancellationDuringHashPreparationCannotReachGeneration() = runBlocking {
        val gate = ModelOperationGate(); val hashed = CompletableDeferred<Unit>()
        var generated = false
        val answer = launch(start = CoroutineStart.UNDISPATCHED) {
            gate.operation { hashed.await(); generated = true }
        }
        val jobs = gate.closeAdmission(); jobs.forEach { it.cancel() }; jobs.joinAll()
        hashed.complete(Unit); answer.join(); assertFalse(generated)
    }

    @Test fun queuedWorkIsRegisteredEvenBeforeItsModelLockIsAvailable() = runBlocking {
        val gate = ModelOperationGate(); val modelLock = kotlinx.coroutines.sync.Mutex(true)
        var generated = false
        val answer = launch(start = CoroutineStart.UNDISPATCHED) {
            gate.operation {
                modelLock.lock()
                try { generated = true } finally { modelLock.unlock() }
            }
        }
        val jobs = gate.closeAdmission(); assertEquals(1, jobs.size)
        jobs.forEach { it.cancel() }; jobs.joinAll(); modelLock.unlock(); answer.join()
        assertFalse(generated)
    }

    @Test fun joiningCancelledWorkWaitsForItsResourceFinalizer() = runBlocking {
        val gate = ModelOperationGate(); val finalizerEntered = CompletableDeferred<Unit>()
        val releaseResource = CompletableDeferred<Unit>(); var resourceClosed = false
        val answer = launch(start = CoroutineStart.UNDISPATCHED) {
            gate.operation {
                try { awaitCancellation() } finally {
                    withContext(NonCancellable) {
                        finalizerEntered.complete(Unit); releaseResource.await(); resourceClosed = true
                    }
                }
            }
        }
        val jobs = gate.closeAdmission(); jobs.forEach { it.cancel() }; finalizerEntered.await()
        assertFalse(resourceClosed); assertFalse(jobs.all { it.isCompleted })
        releaseResource.complete(Unit); jobs.joinAll(); answer.join(); assertTrue(resourceClosed)
    }

    @Test fun retiredCancellationCannotCancelAReinstalledBrainsNewOperation() = runBlocking {
        val gate = ModelOperationGate()
        val old = launch(start = CoroutineStart.UNDISPATCHED) { gate.operation { awaitCancellation() } }
        val jobs = gate.closeAdmission(); jobs.forEach { it.cancel() }; jobs.joinAll(); old.join()
        gate.reopen(); assertEquals("new brain", gate.operation { "new brain" })
        assertTrue(gate.closeAdmission().isEmpty())
    }

    @Test fun duplicateManagementCloseIsRejected() {
        val gate = ModelOperationGate(); gate.closeAdmission()
        assertTrue(runCatching { gate.closeAdmission() }.exceptionOrNull() is IllegalStateException)
    }

    @Test fun repeatedManagementWindowsDoNotRetainCompletedOperations() = runBlocking {
        val gate = ModelOperationGate()
        repeat(20) {
            assertEquals(it, gate.operation { it })
            assertTrue(gate.closeAdmission().isEmpty()); gate.reopen()
        }
    }
}
