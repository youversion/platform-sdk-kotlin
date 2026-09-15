package com.youversion.platform.core.utilities

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coalesces work sharing a key onto one task, so callers that want the same thing at the same time fetch it
 * once between them.
 */
internal class InFlightTasks<K, V> {
    private val tasks = mutableMapOf<K, Deferred<V>>()
    private val mutex = Mutex()

    /**
     * The result of the task for [key], running [fetch] to produce it when no task is already in flight.
     *
     * Claiming a key and joining the task already holding it happen in one critical section, so callers
     * arriving together cannot each start a task for the same key. A caller cancelled in its own right sees
     * that cancellation; one left waiting because the caller that owned the task was cancelled takes the task
     * over, or joins whichever waiter took it over first.
     *
     * @param key Identifies the work, so callers sharing a key share a task.
     * @param fetch Produces the value, run by at most one caller at a time for [key]. Anything a waiting
     *     caller must see, such as a cache write, belongs inside it rather than after it.
     * @return The value [fetch] produced, whether this caller ran it or awaited another caller's task.
     */
    suspend fun result(
        key: K,
        fetch: suspend () -> V,
    ): V {
        while (true) {
            val claim = CompletableDeferred<V>()
            val task =
                mutex.withLock {
                    tasks[key]?.takeIf { it.isActive } ?: claim.also { tasks[key] = it }
                }

            if (task === claim) return claimedResult(key, claim, fetch)

            try {
                return task.await()
            } catch (_: CancellationException) {
                currentCoroutineContext().ensureActive()
            }
        }
    }

    private suspend fun claimedResult(
        key: K,
        claim: CompletableDeferred<V>,
        fetch: suspend () -> V,
    ): V =
        try {
            fetch().also { claim.complete(it) }
        } catch (e: Exception) {
            claim.completeExceptionally(e)
            throw e
        } finally {
            mutex.withLock {
                if (tasks[key] === claim) tasks.remove(key)
            }
        }
}
