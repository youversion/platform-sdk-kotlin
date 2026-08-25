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

    @Test
    fun `test removeExpiredEntries wipes legacy versions once`() =
        runTest {
            val context = RuntimeEnvironment.getApplication()
            val cache = BibleVersionPersistentCache(context)
            cache.addVersion(testVersion)

            cache.removeExpiredEntries()

            assertFalse(cache.versionIsPresent(111))
            assertTrue(File(context.filesDir, ".bible_downloads_only").exists())

            cache.addVersion(testVersion)
            cache.removeExpiredEntries()

            assertTrue(cache.versionIsPresent(111))
        }
}
