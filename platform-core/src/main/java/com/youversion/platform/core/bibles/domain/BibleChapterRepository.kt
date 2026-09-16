package com.youversion.platform.core.bibles.domain

import co.touchlab.kermit.Logger
import com.youversion.platform.core.bibles.api.BiblesEndpoints
import com.youversion.platform.core.bibles.data.BibleVersionCache
import com.youversion.platform.core.utilities.InFlightTasks

class BibleChapterRepository(
    private val memoryCache: BibleVersionCache,
    private val temporaryCache: BibleVersionCache,
    private val persistentCache: BibleVersionCache,
) {
    private val inFlightTasks = InFlightTasks<String, String>()

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

        return inFlightTasks.result(cacheKey) {
            val response = BiblesEndpoints.passageResponse(reference)
            val contents = response.value.content
            memoryCache.addChapterContents(contents, reference, response.expiresAt)
            if (response.isCacheable) {
                temporaryCache.addChapterContents(contents, reference, response.expiresAt)
            }
            contents
        }
    }

    private fun cacheKey(reference: BibleReference): String = "${reference.versionId}_${reference.chapterUSFM}"

    suspend fun removeVersionChapters(versionId: Int) {
        memoryCache.removeVersionChapters(versionId)
        temporaryCache.removeVersionChapters(versionId)
        persistentCache.removeVersionChapters(versionId)
    }
}
