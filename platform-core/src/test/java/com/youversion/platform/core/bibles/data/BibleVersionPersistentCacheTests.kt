package com.youversion.platform.core.bibles.data

import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BibleVersionPersistentCacheTests {
    private val testVersion = BibleVersion(id = 111, abbreviation = "NIV", title = "New International Version")

    private fun createPreviouslyCachedEntry(
        filesDir: File,
        id: Int,
    ): File {
        val dir = File(filesDir, "bible_$id")
        File(dir, "chapters").mkdirs()
        File(dir, "metadata.json").writeText("{}")
        return dir
    }

    @Test
    fun `test removeExpiredEntries deletes previously cached entries and keeps downloaded versions`() =
        runTest {
            val context = RuntimeEnvironment.getApplication()
            val previouslyCachedDir = createPreviouslyCachedEntry(context.filesDir, 999)
            val cache = BibleVersionPersistentCache(context)
            cache.addVersion(testVersion)

            cache.removeExpiredEntries()

            assertFalse(previouslyCachedDir.exists())
            assertTrue(cache.versionIsPresent(111))
        }

    @Test
    fun `test previously cached entries are not reported as downloaded`() {
        val context = RuntimeEnvironment.getApplication()
        createPreviouslyCachedEntry(context.filesDir, 999)
        val cache = BibleVersionPersistentCache(context)

        assertTrue(cache.storedVersionIds.isEmpty())
        assertFalse(cache.versionIsPresent(999))
    }
}
