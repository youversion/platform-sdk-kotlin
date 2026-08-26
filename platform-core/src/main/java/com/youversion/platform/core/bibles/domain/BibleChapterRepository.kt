package com.youversion.platform.core.bibles.domain

import co.touchlab.kermit.Logger
import com.youversion.platform.core.bibles.api.BiblesEndpoints
import com.youversion.platform.core.bibles.data.BibleVersionCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BibleChapterRepository(
    private val memoryCache: BibleVersionCache,
    private val temporaryCache: BibleVersionCache,
    private val persistentCache: BibleVersionCache,
) {
    private val inFlightTasks = mutableMapOf<String, Deferred<String>>()
    private val inFlightTasksMutex = Mutex()

    suspend fun chapter(reference: BibleReference): String {
        memoryCache.chapterContent(reference)?.let {
            return it.value
        }

        temporaryCache.chapterContent(reference)?.let {
            memoryCache.addChapterContents(it.value, reference, it.expiresAt)
            return it.value
        }

        persistentCache.chapterContent(reference)?.let {
            memoryCache.addChapterContents(it.value, reference)
            return it.value
        }

        val cacheKey = cacheKey(reference)
        Logger.d { "$cacheKey Not found in any cache. Fetching from network..." }

        // If a fetch is already in-flight, await its result
        inFlightTasksMutex
            .withLock { inFlightTasks[cacheKey] }
            ?.let { task ->
                if (task.isActive) return task.await()
            }

        // Otherwise, create a new fetch task
        val deferred = CompletableDeferred<String>()
        inFlightTasksMutex.withLock { inFlightTasks[cacheKey] = deferred }

        return try {
            val response = BiblesEndpoints.passageResponse(reference)
            val contents = response.value.content
            memoryCache.addChapterContents(contents, reference, response.expiresAt)
            if (response.isCacheable) {
                temporaryCache.addChapterContents(contents, reference, response.expiresAt)
            }
            deferred.complete(contents)
            contents
        } catch (e: Exception) {
            deferred.completeExceptionally(e)
            throw e
        } finally {
            inFlightTasksMutex.withLock {
                inFlightTasks.remove(cacheKey)
            }
        }
    }

    private fun cacheKey(reference: BibleReference): String = "${reference.versionId}_${reference.chapterUSFM}"

    suspend fun removeVersionChapters(versionId: Int) {
        memoryCache.removeVersionChapters(versionId)
        temporaryCache.removeVersionChapters(versionId)
        persistentCache.removeVersionChapters(versionId)
    }
}
