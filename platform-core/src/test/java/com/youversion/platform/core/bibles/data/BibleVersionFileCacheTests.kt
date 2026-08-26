package com.youversion.platform.core.bibles.data

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class TestBibleVersionFileCache(
    override val rootDir: File,
    isExpiring: Boolean = false,
    now: () -> Long = System::currentTimeMillis,
) : BibleVersionFileCache(isExpiring, now)

class BibleVersionFileCacheTests {
    private lateinit var tempDir: File
    private lateinit var cache: TestBibleVersionFileCache

    private val testVersion = BibleVersion(id = 111, abbreviation = "NIV", title = "New International Version")
    private val testReference = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1, verse = 1)

    private var currentTime = 1_000L

    private fun expiringCache() = TestBibleVersionFileCache(tempDir, isExpiring = true, now = { currentTime })

    @BeforeTest
    fun setup() {
        tempDir = createTempDirectory("bible_cache_test").toFile()
        cache = TestBibleVersionFileCache(tempDir)
    }

    @AfterTest
    fun teardown() {
        tempDir.deleteRecursively()
    }

    // ----- Empty state

    @Test
    fun `test empty cache has no stored version ids`() {
        assertTrue(cache.storedVersionIds.isEmpty())
    }

    @Test
    fun `test versionIsPresent returns false for empty cache`() {
        assertFalse(cache.versionIsPresent(111))
    }

    @Test
    fun `test version returns null for empty cache`() =
        runTest {
            assertNull(cache.version(111))
        }

    @Test
    fun `test chapterContent returns null for empty cache`() =
        runTest {
            assertNull(cache.chapterContent(testReference))
        }

    @Test
    fun `test chaptersArePresent returns false for empty cache`() {
        assertFalse(cache.chaptersArePresent(111))
    }

    @Test
    fun `test reads do not create version directories`() =
        runTest {
            cache.version(999)
            cache.chapterContent(testReference)
            cache.versionIsPresent(999)
            cache.chaptersArePresent(999)

            assertTrue(cache.storedVersionIds.isEmpty())
        }

    // ----- Add and retrieve version

    @Test
    fun `test addVersion and retrieve version`() =
        runTest {
            cache.addVersion(testVersion)

            assertTrue(cache.versionIsPresent(111))
            val retrieved = cache.version(111)
            assertEquals(111, retrieved?.value?.id)
            assertEquals("NIV", retrieved?.value?.abbreviation)
            assertEquals("New International Version", retrieved?.value?.title)
        }

    @Test
    fun `test addVersion appears in storedVersionIds`() =
        runTest {
            cache.addVersion(testVersion)
            assertContains(cache.storedVersionIds, 111)
        }

    @Test
    fun `test overwrite version with same id`() =
        runTest {
            cache.addVersion(testVersion)
            val updated = BibleVersion(id = 111, abbreviation = "NIV2", title = "Updated")
            cache.addVersion(updated)

            val retrieved = cache.version(111)
            assertEquals("NIV2", retrieved?.value?.abbreviation)
            assertEquals("Updated", retrieved?.value?.title)
        }

    // ----- Add and retrieve chapter content

    @Test
    fun `test addChapterContents and retrieve chapterContent`() =
        runTest {
            val content = "<html><body>In the beginning...</body></html>"
            cache.addChapterContents(content, testReference)

            val retrieved = cache.chapterContent(testReference)
            assertEquals(content, retrieved?.value)
        }

    @Test
    fun `test chaptersArePresent returns true after adding chapters`() =
        runTest {
            cache.addChapterContents("content", testReference)
            assertTrue(cache.chaptersArePresent(111))
        }

    @Test
    fun `test overwrite chapter content for same reference`() =
        runTest {
            cache.addChapterContents("original", testReference)
            cache.addChapterContents("updated", testReference)

            assertEquals("updated", cache.chapterContent(testReference)?.value)
        }

    @Test
    fun `test empty chapter file returns empty string not null`() =
        runTest {
            cache.addChapterContents("", testReference)

            val retrieved = cache.chapterContent(testReference)
            assertEquals("", retrieved?.value)
        }

    // ----- removeVersion

    @Test
    fun `test removeVersion makes version not present`() =
        runTest {
            cache.addVersion(testVersion)
            cache.removeVersion(111)

            assertFalse(cache.versionIsPresent(111))
            assertNull(cache.version(111))
        }

    @Test
    fun `test removeVersion leaves chapters intact`() =
        runTest {
            cache.addVersion(testVersion)
            cache.addChapterContents("content", testReference)
            cache.removeVersion(111)

            assertFalse(cache.versionIsPresent(111))
            assertTrue(cache.chaptersArePresent(111))
            assertEquals("content", cache.chapterContent(testReference)?.value)
        }

    @Test
    fun `test removeVersion on non-existent version does not throw`() =
        runTest {
            cache.removeVersion(999)
        }

    // ----- removeVersionChapters

    @Test
    fun `test removeVersionChapters clears chapters but keeps metadata`() =
        runTest {
            cache.addVersion(testVersion)
            cache.addChapterContents("content", testReference)

            cache.removeVersionChapters(111)

            assertTrue(cache.versionIsPresent(111))
            assertFalse(cache.chaptersArePresent(111))
            assertNull(cache.chapterContent(testReference))
        }

    @Test
    fun `test removeVersionChapters on non-existent version does not throw`() =
        runTest {
            cache.removeVersionChapters(999)
        }

    @Test
    fun `test chaptersArePresent returns false after removing chapters`() =
        runTest {
            cache.addChapterContents("content", testReference)
            assertTrue(cache.chaptersArePresent(111))

            cache.removeVersionChapters(111)
            assertFalse(cache.chaptersArePresent(111))
        }

    // ----- removeUnpermittedVersions

    @Test
    fun `test removeUnpermittedVersions removes only unpermitted versions`() =
        runTest {
            cache.addVersion(testVersion)
            cache.addVersion(BibleVersion(id = 222, abbreviation = "KJV"))
            cache.addVersion(BibleVersion(id = 333, abbreviation = "ESV"))

            cache.removeUnpermittedVersions(setOf(111, 333))

            assertTrue(cache.versionIsPresent(111))
            assertFalse(cache.versionIsPresent(222))
            assertTrue(cache.versionIsPresent(333))
        }

    @Test
    fun `test removeUnpermittedVersions with empty set removes all`() =
        runTest {
            cache.addVersion(testVersion)
            cache.addVersion(BibleVersion(id = 222, abbreviation = "KJV"))

            cache.removeUnpermittedVersions(emptySet())

            assertFalse(cache.versionIsPresent(111))
            assertFalse(cache.versionIsPresent(222))
        }

    @Test
    fun `test removeUnpermittedVersions with all permitted removes none`() =
        runTest {
            cache.addVersion(testVersion)
            cache.addVersion(BibleVersion(id = 222, abbreviation = "KJV"))

            cache.removeUnpermittedVersions(setOf(111, 222))

            assertTrue(cache.versionIsPresent(111))
            assertTrue(cache.versionIsPresent(222))
        }

    @Test
    fun `test removeUnpermittedVersions with extra permitted ids does not error`() =
        runTest {
            cache.addVersion(testVersion)

            cache.removeUnpermittedVersions(setOf(111, 999, 888))

            assertTrue(cache.versionIsPresent(111))
        }

    @Test
    fun `test removeUnpermittedVersions leaves chapters orphaned`() =
        runTest {
            cache.addVersion(testVersion)
            cache.addChapterContents("content", testReference)

            cache.removeUnpermittedVersions(emptySet())

            assertFalse(cache.versionIsPresent(111))
            assertTrue(cache.chaptersArePresent(111))
        }

    // ----- Multiple versions

    @Test
    fun `test storedVersionIds returns all added version ids`() =
        runTest {
            cache.addVersion(BibleVersion(id = 111))
            cache.addVersion(BibleVersion(id = 222))
            cache.addVersion(BibleVersion(id = 333))

            val ids = cache.storedVersionIds
            assertEquals(3, ids.size)
            assertContains(ids, 111)
            assertContains(ids, 222)
            assertContains(ids, 333)
        }

    // ----- scanForVersionIds edge cases

    @Test
    fun `test scanForVersionIds ignores hidden directories`() =
        runTest {
            cache.addVersion(testVersion)
            File(tempDir, ".bible_999").mkdir()

            val ids = cache.storedVersionIds
            assertContains(ids, 111)
            assertFalse(ids.contains(999))
        }

    @Test
    fun `test scanForVersionIds ignores non-numeric suffix`() {
        File(tempDir, "bible_abc").mkdir()
        assertTrue(cache.storedVersionIds.isEmpty())
    }

    @Test
    fun `test scanForVersionIds ignores empty suffix`() {
        File(tempDir, "bible_").mkdir()
        assertTrue(cache.storedVersionIds.isEmpty())
    }

    @Test
    fun `test scanForVersionIds ignores directories without bible prefix`() {
        File(tempDir, "other_123").mkdir()
        assertTrue(cache.storedVersionIds.isEmpty())
    }

    @Test
    fun `test scanForVersionIds ignores regular files`() {
        File(tempDir, "bible_123").createNewFile()
        assertTrue(cache.storedVersionIds.isEmpty())
    }

    @Test
    fun `test scanForVersionIds includes id zero`() {
        File(tempDir, "bible_0").mkdir()
        assertContains(cache.storedVersionIds, 0)
    }

    @Test
    fun `test scanForVersionIds includes six digit ids`() {
        File(tempDir, "bible_999999").mkdir()
        assertContains(cache.storedVersionIds, 999999)
    }

    @Test
    fun `test scanForVersionIds excludes seven digit ids`() {
        File(tempDir, "bible_1000000").mkdir()
        assertTrue(cache.storedVersionIds.isEmpty())
    }

    // ----- Error handling

    @Test
    fun `test version throws on corrupted metadata json`() =
        runTest {
            val versionDir = File(tempDir, "bible_111")
            versionDir.mkdir()
            File(versionDir, "metadata.json").writeText("not valid json")

            assertFailsWith<SerializationException> {
                cache.version(111)
            }
        }

    // ----- Chapter isolation

    @Test
    fun `test multiple chapters for same version are stored independently`() =
        runTest {
            val gen1 = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1, verse = 1)
            val gen2 = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 2, verse = 1)

            cache.addChapterContents("Genesis 1 content", gen1)
            cache.addChapterContents("Genesis 2 content", gen2)

            assertEquals("Genesis 1 content", cache.chapterContent(gen1)?.value)
            assertEquals("Genesis 2 content", cache.chapterContent(gen2)?.value)
        }

    @Test
    fun `test same chapter across different versions are isolated`() =
        runTest {
            val refV111 = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1, verse = 1)
            val refV222 = BibleReference(versionId = 222, bookUSFM = "GEN", chapter = 1, verse = 1)

            cache.addChapterContents("NIV Genesis 1", refV111)
            cache.addChapterContents("KJV Genesis 1", refV222)

            assertEquals("NIV Genesis 1", cache.chapterContent(refV111)?.value)
            assertEquals("KJV Genesis 1", cache.chapterContent(refV222)?.value)
        }

    @Test
    fun `test addVersion succeeds when directory already exists from chapter writes`() =
        runTest {
            cache.addChapterContents("content", testReference)
            assertTrue(cache.chaptersArePresent(111))

            cache.addVersion(testVersion)

            assertTrue(cache.versionIsPresent(111))
            assertEquals("content", cache.chapterContent(testReference)?.value)
        }

    // ----- chaptersArePresent edge cases

    @Test
    fun `test chaptersArePresent returns false when chapters dir exists but is empty`() =
        runTest {
            cache.addVersion(testVersion)
            File(File(tempDir, "bible_111"), "chapters").mkdir()

            assertFalse(cache.chaptersArePresent(111))
        }

    // ----- Expiration

    @Test
    fun `test expiring cache writes expiration sidecars on add`() =
        runTest {
            val cache = expiringCache()
            cache.addVersion(testVersion, expiresAt = 5_000)
            cache.addChapterContents("content", testReference, expiresAt = 6_000)

            assertEquals("5000", File(tempDir, "bible_111/metadata.json.expiration").readText())
            assertEquals("6000", File(tempDir, "bible_111/chapters/GEN.1.expiration").readText())
        }

    @Test
    fun `test expiring cache returns unexpired entry with its expiration`() =
        runTest {
            val cache = expiringCache()
            cache.addVersion(testVersion, expiresAt = 5_000)

            val retrieved = cache.version(111)
            assertEquals(111, retrieved?.value?.id)
            assertEquals(5_000, retrieved?.expiresAt)
        }

    @Test
    fun `test expiring cache evicts expired entry on read`() =
        runTest {
            val cache = expiringCache()
            cache.addChapterContents("content", testReference, expiresAt = 5_000)

            currentTime = 5_000

            assertNull(cache.chapterContent(testReference))
            assertFalse(File(tempDir, "bible_111/chapters/GEN.1").exists())
            assertFalse(File(tempDir, "bible_111/chapters/GEN.1.expiration").exists())
        }

    @Test
    fun `test expiring cache treats entry without sidecar as expired`() =
        runTest {
            val cache = expiringCache()
            cache.addVersion(testVersion)

            assertNull(cache.version(111))
        }

    @Test
    fun `test removeExpiredEntries evicts expired entries and keeps unexpired ones`() =
        runTest {
            val cache = expiringCache()
            val gen2 = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 2, verse = 1)
            cache.addVersion(testVersion, expiresAt = 2_000)
            cache.addChapterContents("expired chapter", testReference, expiresAt = 2_000)
            cache.addChapterContents("fresh chapter", gen2, expiresAt = 9_000)

            currentTime = 3_000
            cache.removeExpiredEntries()

            assertFalse(cache.versionIsPresent(111))
            assertNull(cache.chapterContent(testReference))
            assertEquals("fresh chapter", cache.chapterContent(gen2)?.value)
        }

    @Test
    fun `test removeExpiredEntries is a no-op for non-expiring cache`() =
        runTest {
            cache.addVersion(testVersion)
            cache.addChapterContents("content", testReference)

            cache.removeExpiredEntries()

            assertEquals(111, cache.version(111)?.value?.id)
            assertNull(cache.version(111)?.expiresAt)
            assertEquals("content", cache.chapterContent(testReference)?.value)
        }
}
