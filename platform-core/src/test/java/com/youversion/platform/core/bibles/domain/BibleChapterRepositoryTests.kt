package com.youversion.platform.core.bibles.domain

import com.youversion.platform.core.bibles.data.BibleVersionCache
import com.youversion.platform.core.bibles.data.BibleVersionMemoryCache
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BibleChapterRepositoryTests : YouVersionPlatformTest {
    lateinit var memoryCache: BibleVersionCache
    lateinit var temporaryCache: BibleVersionCache
    lateinit var persistentCache: BibleVersionCache

    lateinit var repository: BibleChapterRepository

    @BeforeTest
    fun setup() {
        // Recreate caches each test to clear state
        memoryCache = BibleVersionMemoryCache()
        temporaryCache = BibleVersionMemoryCache()
        persistentCache = BibleVersionMemoryCache()

        repository =
            BibleChapterRepository(
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

            assertEquals(contents, memoryCache.chapterContent(reference)?.value)
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

            assertEquals(contents, memoryCache.chapterContent(reference)?.value)
        }

    @Test
    fun `test chapter promotion from temporary cache carries expiration into memory`() =
        runTest {
            val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 1)
            val expiresAt = System.currentTimeMillis() + 60_000
            temporaryCache.addChapterContents("contents", reference, expiresAt)

            repository.chapter(reference)

            assertEquals(expiresAt, memoryCache.chapterContent(reference)?.expiresAt)
        }

    @Test
    fun `test chapter promotion from persistent cache stores no expiration in memory`() =
        runTest {
            val reference = BibleReference(versionId = 206, bookUSFM = "GEN", chapter = 1)
            persistentCache.addChapterContents("contents", reference, System.currentTimeMillis() + 60_000)

            repository.chapter(reference)

            val promoted = memoryCache.chapterContent(reference)
            assertEquals("contents", promoted?.value)
            assertNull(promoted?.expiresAt)
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

            assertEquals("content", memoryCache.chapterContent(reference)?.value)
            assertEquals("content", temporaryCache.chapterContent(reference)?.value)
            assertNull(persistentCache.chapterContent(reference))
        }

    @OptIn(ExperimentalAtomicApi::class, ExperimentalCoroutinesApi::class)
    @Test
    fun `test concurrent calls only ever trigger a single in-flight task`() =
        runTest {
            val count = AtomicInt(0)

            MockEngine {
                count.incrementAndFetch()
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

            joinAll(
                launch { repository.chapter(reference) },
                launch { repository.chapter(reference) },
                launch { repository.chapter(reference) },
                launch { repository.chapter(reference) },
                launch { repository.chapter(reference) },
                launch { repository.chapter(reference) },
            )

            // Assert all other tasks waited on the first one to complete
            assertEquals(1, count.load())

            repository.removeVersionChapters(206)

            // Assert another call would trigger a new request, asserting the task was removed
            repository.chapter(reference)
            assertEquals(2, count.load())
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

            assertEquals("In the beginning...", memoryCache.chapterContent(reference)?.value)
            assertEquals("In the beginning...", temporaryCache.chapterContent(reference)?.value)
            assertEquals("In the beginning...", persistentCache.chapterContent(reference)?.value)

            repository.removeVersionChapters(206)

            assertNull(memoryCache.chapterContent(reference))
            assertNull(temporaryCache.chapterContent(reference))
            assertNull(persistentCache.chapterContent(reference))
        }
}
