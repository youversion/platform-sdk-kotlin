package com.youversion.platform.core.bibles.domain

import co.touchlab.kermit.Logger
import com.youversion.platform.core.api.YouVersionApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Fetches and caches intro chapter HTML content.
 *
 * Intro chapters cannot be represented by [BibleReference] (which requires
 * a numeric chapter >= 1), so this repository fetches passages by their
 * string passage ID (e.g., "GEN.INTRO") directly.
 */
class BibleIntroRepository {
    private val introCache = mutableMapOf<String, String>()
    private val inFlightTasks = mutableMapOf<String, Deferred<String>>()
    private val inFlightTasksMutex = Mutex()

    /**
     * The HTML content for an intro passage.
     *
     * @param versionId The Bible version ID.
     * @param passageId The passage ID string (e.g., "GEN.INTRO").
     * @return The HTML content of the intro passage.
     */
    suspend fun introContent(
        versionId: Int,
        passageId: String,
    ): String {
        val cacheKey = "${versionId}_$passageId"

        introCache[cacheKey]?.let { return it }

        Logger.d { "$cacheKey Not found in cache. Fetching from network..." }

        inFlightTasksMutex
            .withLock { inFlightTasks[cacheKey] }
            ?.let { task ->
                if (task.isActive) return task.await()
            }

        val deferred = CompletableDeferred<String>()
        inFlightTasksMutex.withLock { inFlightTasks[cacheKey] = deferred }

        return try {
            val contents = YouVersionApi.bible.passage(versionId, passageId).content
            introCache[cacheKey] = contents
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
}
