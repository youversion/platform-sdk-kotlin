package com.youversion.platform.core.bibles.domain

import co.touchlab.kermit.Logger
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.data.BibleVersionCache

class BibleChapterRepository(
    private val memoryCache: BibleVersionCache,
    private val temporaryCache: BibleVersionCache,
    private val persistentCache: BibleVersionCache,
) {
    suspend fun chapter(reference: BibleReference): String {
        val logKey = "${reference.versionId}_${reference.chapterUSFM}"
        memoryCache.chapterContent(reference)?.let {
            return it
        }

        temporaryCache.chapterContent(reference)?.let {
            memoryCache.addChapterContents(it, reference)
            return it
        }

        persistentCache.chapterContent(reference)?.let {
            memoryCache.addChapterContents(it, reference)
            return it
        }

        Logger.d { "$logKey Not found in any cache. Fetching from network..." }
        val contents = YouVersionApi.bible.passage(reference).content
        memoryCache.addChapterContents(contents, reference)
        temporaryCache.addChapterContents(contents, reference)
        persistentCache.addChapterContents(contents, reference)

        return contents
    }

    suspend fun removeVersionChapters(versionId: Int) {
        memoryCache.removeVersionChapters(versionId)
        temporaryCache.removeVersionChapters(versionId)
        persistentCache.removeVersionChapters(versionId)
    }
}
