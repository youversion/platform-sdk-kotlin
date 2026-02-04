package com.youversion.platform.core.bibles.domain

import com.youversion.platform.core.bibles.data.BibleVersionCache
import com.youversion.platform.core.bibles.data.BibleVersionMemoryCache
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BibleChapterRepositoryTests : YouVersionPlatformTest {
    lateinit var memoryCache: BibleVersionCache
    lateinit var temporaryCache: BibleVersionCache
    lateinit var persistentCache: BibleVersionCache

    lateinit var repository: BibleVersionRepository

    @BeforeTest
    fun setup() {
        // Recreate caches each test to clear state
        memoryCache = BibleVersionMemoryCache()
        temporaryCache = BibleVersionMemoryCache()
        persistentCache = BibleVersionMemoryCache()

        repository =
            BibleVersionRepository(
                memoryCache = memoryCache,
                temporaryCache = temporaryCache,
                persistentCache = persistentCache,
            )
    }

    @AfterTest
    fun teardown() {
        stopYouVersionPlatformTest()
    }

    // ----- chapter

    @Test
    fun `test chapter returns chapter contents from memory cache first`() =
        runTest {
            val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 1)
            val contents = "GEN.1.1 WEBUS"
            memoryCache.addChapterContents(contents, reference)

            val chapter = repository.chapter(reference)
            assertEquals(contents, chapter)
        }

    @Test
    fun `test chapter returns chapter contents from temporary cache if not in memory`() =
        runTest {
            val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 1)
            val contents = "GEN.1.1 WEBUS"
            temporaryCache.addChapterContents(contents, reference)
            assertNull(memoryCache.chapterContent(reference))

            val chapter = repository.chapter(reference)
            assertEquals(contents, chapter)

            assertEquals(contents, memoryCache.chapterContent(reference))
        }

    @Test
    fun `test chapter returns chapter contents from persistent cache if not in memory or temporary`() =
        runTest {
            val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 1)
            val contents = "GEN.1.1 WEBUS"
            persistentCache.addChapterContents(contents, reference)
            assertNull(memoryCache.chapterContent(reference))

            val chapter = repository.chapter(reference)
            assertEquals(contents, chapter)

            assertEquals(contents, memoryCache.chapterContent(reference))
        }

    @Test
    fun `test chapter fetches chapter contents from the API if not in any cache and then caches it`() =
        runTest {
            MockEngine {
                respondJson(
                    """
                    {
                        "id": "JHN.3.1",
                        "content": "content",
                        "reference": "John 3:1"
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 1)
            val chapter = repository.chapter(reference)
            assertEquals("content", chapter)

            assertEquals("content", memoryCache.chapterContent(reference))
            assertEquals("content", temporaryCache.chapterContent(reference))
            assertEquals("content", persistentCache.chapterContent(reference))
        }

    // ----- removeVersionChapters
    @Test
    fun `test removeVersionChapters removes chapters from all caches`() =
        runTest {
            val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 1)
            val contents = "In the beginning..."

            memoryCache.addChapterContents(contents, reference)
            temporaryCache.addChapterContents(contents, reference)
            persistentCache.addChapterContents(contents, reference)

            assertEquals("In the beginning...", memoryCache.chapterContent(reference))
            assertEquals("In the beginning...", temporaryCache.chapterContent(reference))
            assertEquals("In the beginning...", persistentCache.chapterContent(reference))

            repository.removeVersionChapters(206)

            assertNull(memoryCache.chapterContent(reference))
            assertNull(temporaryCache.chapterContent(reference))
            assertNull(persistentCache.chapterContent(reference))
        }
}
