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
) : BibleVersionFileCache()

class BibleVersionFileCacheTests {
    private lateinit var tempDir: File
    private lateinit var cache: TestBibleVersionFileCache

    private val testVersion = BibleVersion(id = 111, abbreviation = "NIV", title = "New International Version")
    private val testReference = BibleReference(versionId = 111, bookUSFM = "GEN", chapter = 1, verse = 1)

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

    // ----- Add and retrieve version

    @Test
    fun `test addVersion and retrieve version`() =
        runTest {
            cache.addVersion(testVersion)

            assertTrue(cache.versionIsPresent(111))
            val retrieved = cache.version(111)
            assertEquals(111, retrieved?.id)
            assertEquals("NIV", retrieved?.abbreviation)
            assertEquals("New International Version", retrieved?.title)
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
            assertEquals("NIV2", retrieved?.abbreviation)
            assertEquals("Updated", retrieved?.title)
        }

    // ----- Add and retrieve chapter content

    @Test
    fun `test addChapterContents and retrieve chapterContent`() =
        runTest {
            val content = "<html><body>In the beginning...</body></html>"
            cache.addChapterContents(content, testReference)

            val retrieved = cache.chapterContent(testReference)
            assertEquals(content, retrieved)
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

            assertEquals("updated", cache.chapterContent(testReference))
        }

    @Test
    fun `test empty chapter file returns empty string not null`() =
        runTest {
            cache.addChapterContents("", testReference)

            val retrieved = cache.chapterContent(testReference)
            assertEquals("", retrieved)
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
            assertEquals("content", cache.chapterContent(testReference))
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

    // ----- chaptersArePresent edge cases

    @Test
    fun `test chaptersArePresent returns false when chapters dir exists but is empty`() =
        runTest {
            cache.addVersion(testVersion)
            File(File(tempDir, "bible_111"), "chapters").mkdir()

            assertFalse(cache.chaptersArePresent(111))
        }
}
