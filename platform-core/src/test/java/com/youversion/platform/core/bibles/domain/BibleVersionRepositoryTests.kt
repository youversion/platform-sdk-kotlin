package com.youversion.platform.core.bibles.domain

import com.youversion.platform.core.bibles.data.BibleVersionCache
import com.youversion.platform.core.bibles.data.BibleVersionMemoryCache
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.helpers.FixtureLoader
import com.youversion.platform.helpers.TestStore
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BibleVersionRepositoryTests : YouVersionPlatformTest {
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
                store = TestStore(),
            )
    }

    @AfterTest
    fun teardown() {
        stopYouVersionPlatformTest()
    }

    // ----- versionIfCached

    @Test
    fun `test versionIfCached returns version from memory cache first`() =
        runTest {
            val memoryVersion = BibleVersion(id = 111, abbreviation = "MEM")
            val tempVersion = BibleVersion(id = 111, abbreviation = "TEMP")
            val persistentVersion = BibleVersion(id = 111, abbreviation = "PERS")

            // Add version to all three caches with different abbreviations
            memoryCache.addVersion(memoryVersion)
            temporaryCache.addVersion(tempVersion)
            persistentCache.addVersion(persistentVersion)

            // Should return from memory cache first
            val cached = repository.versionIfCached(111)
            assertEquals("MEM", cached?.abbreviation)
        }

    @Test
    fun `test versionIfCached returns version from temporary cache if not in memory`() =
        runTest {
            val tempVersion = BibleVersion(id = 111, abbreviation = "TEMP")
            val persistentVersion = BibleVersion(id = 111, abbreviation = "PERS")

            // Add version to temporary and persistent caches only
            temporaryCache.addVersion(tempVersion)
            persistentCache.addVersion(persistentVersion)

            // Should return from temporary cache (memory cache is empty)
            val cached = repository.versionIfCached(111)
            assertEquals("TEMP", cached?.abbreviation)
        }

    @Test
    fun `test versionIfCached returns version from persistent cache if not in memory or temporary`() =
        runTest {
            val persistentVersion = BibleVersion(id = 111, abbreviation = "PERS")

            // Add version to persistent cache only
            persistentCache.addVersion(persistentVersion)

            // Should return from persistent cache (memory and temporary are empty)
            val cached = repository.versionIfCached(111)
            assertEquals("PERS", cached?.abbreviation)
        }

    @Test
    fun `test versionIfCached returns null if version not in any cache`() =
        runTest {
            // Don't add version to any cache

            // Should return null
            val cached = repository.versionIfCached(111)
            assertNull(cached)
        }

    // ----- version
    @Test
    fun `test version returns a cached version`() =
        runTest {
            val persistentVersion = BibleVersion(id = 111, abbreviation = "PERS")

            // Add version to persistent cache only
            persistentCache.addVersion(persistentVersion)

            val version = repository.version(111)
            assertEquals(111, version.id)
            assertFalse(memoryCache.versionIsPresent(111))
            assertFalse(temporaryCache.versionIsPresent(111))
            assertTrue(persistentCache.versionIsPresent(111))
        }

    @Test
    fun `test version fetches a version from the API if not in any cache and then caches it`() =
        runTest {
            MockEngine { request ->
                val bible206Json = FixtureLoader().loadFixtureString("bible_206")
                val bible206IndexJson = FixtureLoader().loadFixtureString("bible_206_index")

                when (request.url.encodedPath) {
                    "/v1/bibles/206" -> respondJson(bible206Json)
                    "/v1/bibles/206/index" -> respondJson(bible206IndexJson)
                    else -> throw IllegalArgumentException("Unexpected request path: ${request.url.encodedPath}")
                }
            }.also { engine -> startYouVersionPlatformTest(engine) }

            val version = repository.version(206)
            assertEquals(206, version.id)
            assertTrue { memoryCache.versionIsPresent(206) }
            assertTrue { temporaryCache.versionIsPresent(206) }
            assertTrue { persistentCache.versionIsPresent(206) }
        }

    // ----- versionIsPresent

    @Test
    fun `test versionIsPresent returns true when version is in persistent cache`() =
        runTest {
            val version = BibleVersion(id = 111, abbreviation = "NIV")
            assertFalse(repository.versionIsPresent(111))

            // Add version to persistent cache
            persistentCache.addVersion(version)
            assertTrue(repository.versionIsPresent(111))
        }

    @Test
    fun `test versionIsPresent returns false when version only in memory or temporary cache`() =
        runTest {
            val version = BibleVersion(id = 111, abbreviation = "NIV")

            // Add version only to memory and temporary cache
            memoryCache.addVersion(version)
            temporaryCache.addVersion(version)

            // Check if version is present (only persistent cache matters)
            assertFalse(repository.versionIsPresent(111))
        }

    // ----- downloadedVersions

    @Test
    fun `test downloadedVersions returns empty list when no versions downloaded`() =
        runTest {
            // Don't add any versions

            // Check downloaded versions
            val downloaded = repository.downloadedVersions
            assertEquals(emptyList(), downloaded)
        }

    @Test
    fun `test downloadedVersions returns list of version ids in persistent cache`() =
        runTest {
            val version1 = BibleVersion(id = 1, abbreviation = "KJV")
            val version2 = BibleVersion(id = 111, abbreviation = "NIV")
            val version3 = BibleVersion(id = 206, abbreviation = "WEBUS")

            // Add versions to persistent cache
            persistentCache.addVersion(version1)
            persistentCache.addVersion(version2)
            persistentCache.addVersion(version3)

            // Check downloaded versions
            val downloaded = repository.downloadedVersions
            assertEquals(3, downloaded.size)
            assertTrue(downloaded.contains(1))
            assertTrue(downloaded.contains(111))
            assertTrue(downloaded.contains(206))
        }

    @Test
    fun `test downloadedVersions only returns persistent cache versions`() =
        runTest {
            val version1 = BibleVersion(id = 1, abbreviation = "KJV")
            val version2 = BibleVersion(id = 111, abbreviation = "NIV")
            val version3 = BibleVersion(id = 206, abbreviation = "WEBUS")

            // Add version to persistent cache
            persistentCache.addVersion(version1)

            // Add versions to memory and temporary caches only
            memoryCache.addVersion(version2)
            temporaryCache.addVersion(version3)

            // Check downloaded versions (only persistent cache matters)
            val downloaded = repository.downloadedVersions
            assertEquals(1, downloaded.size)
            assertTrue(downloaded.contains(1))
            assertFalse(downloaded.contains(111))
            assertFalse(downloaded.contains(206))
        }

    // ----- downloadVersion

    @Test
    fun `test downloadVersion does nothing if version already in persistent cache`() =
        runTest {
            val version = BibleVersion(id = 111, abbreviation = "NIV")

            // Add version to persistent cache
            persistentCache.addVersion(version)

            // Download should be a no-op
            repository.downloadVersion(111)

            // Verify version is still in persistent cache
            assertTrue(persistentCache.versionIsPresent(111))
        }

    @Test
    fun `test downloadVersion moves version from temporary to persistent cache`() =
        runTest {
            val version = BibleVersion(id = 111, abbreviation = "NIV")

            // Add version to temporary cache (simulating it was fetched previously)
            temporaryCache.addVersion(version)
            memoryCache.addVersion(version)

            // Verify version is in temporary cache
            assertTrue(temporaryCache.versionIsPresent(111))
            assertFalse(persistentCache.versionIsPresent(111))

            // Download version
            repository.downloadVersion(111)

            // Verify version is moved to persistent cache
            assertTrue(persistentCache.versionIsPresent(111))
            // Verify version is removed from temporary cache
            assertFalse(temporaryCache.versionIsPresent(111))
            // Verify version remains in memory cache
            assertTrue(memoryCache.versionIsPresent(111))
        }

    // ----- downloadStatus

    @Test
    fun `test downloadStatus returns DOWNLOADED when version is in persistent cache`() =
        runTest {
            val version = BibleVersion(id = 111, abbreviation = "NIV")

            // Add version to persistent cache
            persistentCache.addVersion(version)

            // Check download status
            val status = repository.downloadStatus(111)
            assertEquals(BibleVersionDownloadStatus.DOWNLOADED, status)
        }

    @Test
    fun `test downloadStatus returns NOT_DOWNLOADABLE when version is not in persistent cache`() =
        runTest {
            // Don't add version to any cache

            // Check download status
            val status = repository.downloadStatus(111)
            assertEquals(BibleVersionDownloadStatus.NOT_DOWNLOADABLE, status)
        }

    @Test
    fun `test downloadStatus returns NOT_DOWNLOADABLE when version only in memory cache`() =
        runTest {
            val version = BibleVersion(id = 111, abbreviation = "NIV")

            // Add version only to memory cache
            memoryCache.addVersion(version)

            // Check download status (only persistent cache matters)
            val status = repository.downloadStatus(111)
            assertEquals(BibleVersionDownloadStatus.NOT_DOWNLOADABLE, status)
        }

    @Test
    fun `test downloadStatus returns NOT_DOWNLOADABLE when version only in temporary cache`() =
        runTest {
            val version = BibleVersion(id = 111, abbreviation = "NIV")

            // Add version only to temporary cache
            temporaryCache.addVersion(version)

            // Check download status (only persistent cache matters)
            val status = repository.downloadStatus(111)
            assertEquals(BibleVersionDownloadStatus.NOT_DOWNLOADABLE, status)
        }

    // ----- removeVersion
    @Test
    fun `test removeVersion removes from all caches`() =
        runTest {
            val version = BibleVersion(id = 111, abbreviation = "NIV")

            // Add version to all three caches
            memoryCache.addVersion(version)
            temporaryCache.addVersion(version)
            persistentCache.addVersion(version)

            // Verify version is present in all caches
            assertTrue(memoryCache.versionIsPresent(111))
            assertTrue(temporaryCache.versionIsPresent(111))
            assertTrue(persistentCache.versionIsPresent(111))

            // Remove version
            repository.removeVersion(111)

            // Verify version is removed from all caches
            assertFalse(memoryCache.versionIsPresent(111))
            assertFalse(temporaryCache.versionIsPresent(111))
            assertFalse(persistentCache.versionIsPresent(111))

            // Verify version returns null from all caches
            assertNull(memoryCache.version(111))
            assertNull(temporaryCache.version(111))
            assertNull(persistentCache.version(111))
        }

    // ----- removeUnpermittedVersions

    @Test
    fun `test removeUnpermittedVersions removes unpermitted versions from all caches`() =
        runTest {
            val version1 = BibleVersion(id = 1, abbreviation = "KJV")
            val version2 = BibleVersion(id = 111, abbreviation = "NIV")
            val version3 = BibleVersion(id = 206, abbreviation = "WEBUS")
            val version4 = BibleVersion(id = 999, abbreviation = "ESV")

            // Add all versions to all three caches
            listOf(version1, version2, version3, version4).forEach { version ->
                memoryCache.addVersion(version)
                temporaryCache.addVersion(version)
                persistentCache.addVersion(version)
            }

            // Verify all versions are present
            assertTrue(memoryCache.versionIsPresent(1))
            assertTrue(memoryCache.versionIsPresent(111))
            assertTrue(memoryCache.versionIsPresent(206))
            assertTrue(memoryCache.versionIsPresent(999))

            // Remove unpermitted versions (only keep 111 and 206)
            val permittedIds = setOf(111, 206)
            repository.removeUnpermittedVersions(permittedIds)

            // Verify permitted versions remain in all caches
            assertTrue(memoryCache.versionIsPresent(111))
            assertTrue(temporaryCache.versionIsPresent(111))
            assertTrue(persistentCache.versionIsPresent(111))

            assertTrue(memoryCache.versionIsPresent(206))
            assertTrue(temporaryCache.versionIsPresent(206))
            assertTrue(persistentCache.versionIsPresent(206))

            // Verify unpermitted versions are removed from all caches
            assertFalse(memoryCache.versionIsPresent(1))
            assertFalse(temporaryCache.versionIsPresent(1))
            assertFalse(persistentCache.versionIsPresent(1))

            assertFalse(memoryCache.versionIsPresent(999))
            assertFalse(temporaryCache.versionIsPresent(999))
            assertFalse(persistentCache.versionIsPresent(999))
        }

    @Test
    fun `test removeUnpermittedVersions with empty permitted set removes all versions`() =
        runTest {
            val version1 = BibleVersion(id = 111, abbreviation = "NIV")
            val version2 = BibleVersion(id = 206, abbreviation = "WEBUS")

            // Add versions to all caches
            memoryCache.addVersion(version1)
            memoryCache.addVersion(version2)
            temporaryCache.addVersion(version1)
            temporaryCache.addVersion(version2)
            persistentCache.addVersion(version1)
            persistentCache.addVersion(version2)

            // Remove all unpermitted versions (empty set means nothing is permitted)
            repository.removeUnpermittedVersions(emptySet())

            // Verify all versions are removed
            assertFalse(memoryCache.versionIsPresent(111))
            assertFalse(memoryCache.versionIsPresent(206))
            assertFalse(temporaryCache.versionIsPresent(111))
            assertFalse(temporaryCache.versionIsPresent(206))
            assertFalse(persistentCache.versionIsPresent(111))
            assertFalse(persistentCache.versionIsPresent(206))
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
