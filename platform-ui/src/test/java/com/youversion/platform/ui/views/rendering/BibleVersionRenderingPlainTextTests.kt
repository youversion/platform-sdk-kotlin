package com.youversion.platform.ui.views.rendering

import com.youversion.platform.core.bibles.data.BibleVersionCache
import com.youversion.platform.core.bibles.data.BibleVersionMemoryCache
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.utilities.exceptions.BibleVersionApiException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BibleVersionRenderingPlainTextTests {
    private val reference =
        BibleReference(
            versionId = 111,
            bookUSFM = "GEN",
            chapter = 1,
        )

    private suspend fun repositoryWithHtml(html: String): BibleChapterRepository {
        val cache = BibleVersionMemoryCache()
        cache.addChapterContents(html, reference)
        return BibleChapterRepository(
            memoryCache = cache,
            temporaryCache = BibleVersionMemoryCache(),
            persistentCache = BibleVersionMemoryCache(),
        )
    }

    private fun throwingRepository(exception: Exception): BibleChapterRepository {
        val throwingCache =
            object : BibleVersionCache {
                override val storedVersionIds: List<Int> = emptyList()

                override suspend fun version(id: Int): BibleVersion? = null

                override suspend fun chapterContent(reference: BibleReference): String? = throw exception

                override suspend fun addVersion(version: BibleVersion) {}

                override suspend fun addChapterContents(
                    content: String,
                    reference: BibleReference,
                ) {}

                override suspend fun removeVersion(versionId: Int) {}

                override suspend fun removeVersionChapters(versionId: Int) {}

                override suspend fun removeUnpermittedVersions(permittedIds: Set<Int>) {}

                override fun versionIsPresent(versionId: Int): Boolean = false

                override fun chaptersArePresent(versionId: Int): Boolean = false
            }
        return BibleChapterRepository(
            memoryCache = throwingCache,
            temporaryCache = BibleVersionMemoryCache(),
            persistentCache = BibleVersionMemoryCache(),
        )
    }

    @Test
    fun `plainTextOf returns joined plain text for a reference`() =
        runTest {
            val html =
                """
                <div>
                    <div class="p">
                        <span class="yv-v" v="1"></span>
                        <span class="yv-vlbl">1</span>
                        In the beginning
                    </div>
                    <div class="p">
                        <span class="yv-v" v="2"></span>
                        <span class="yv-vlbl">2</span>
                        And the earth was
                    </div>
                </div>
                """.trimIndent()

            val result =
                BibleVersionRendering.plainTextOf(
                    bibleChapterRepository = repositoryWithHtml(html),
                    reference = reference,
                )
            assertNotNull(result)
            assertTrue(result.contains("In the beginning"))
            assertTrue(result.contains("And the earth was"))
            assertTrue(result.contains("\n"))
        }

    @Test
    fun `plainTextOf returns null on non-API exception`() =
        runTest {
            val result =
                BibleVersionRendering.plainTextOf(
                    bibleChapterRepository = throwingRepository(RuntimeException("test error")),
                    reference = reference,
                )
            assertNull(result)
        }

    @Test
    fun `plainTextOf rethrows BibleVersionApiException`() =
        runTest {
            val exception =
                BibleVersionApiException(
                    reason = BibleVersionApiException.Reason.CANNOT_DOWNLOAD,
                )
            assertFailsWith<BibleVersionApiException> {
                BibleVersionRendering.plainTextOf(
                    bibleChapterRepository = throwingRepository(exception),
                    reference = reference,
                )
            }
        }
}
