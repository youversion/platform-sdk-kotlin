package com.youversion.platform.reader

import android.content.Context
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.domain.Storage
import com.youversion.platform.core.languages.domain.LanguageRepository
import com.youversion.platform.reader.di.PlatformReaderKoinModule
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.domain.CopyManager
import com.youversion.platform.reader.domain.ShareManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlatformReaderKoinModuleTests {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val defaultReference =
        BibleReference(
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
            verseStart = null,
            verseEnd = null,
        )

    private val bibleReaderRepository =
        mockk<BibleReaderRepository>(relaxed = true) {
            every { produceBibleReference(any()) } returns defaultReference
        }

    private val bibleVersionRepository =
        mockk<BibleVersionRepository>(relaxed = true) {
            coEvery { version(any()) } returns BibleVersion(id = 1, abbreviation = "KJV")
        }

    private val testDependenciesModule =
        module {
            single { mockk<Context>(relaxed = true) }
            single<Storage> { mockk(relaxed = true) }
            single { bibleVersionRepository }
            single { mockk<BibleChapterRepository>(relaxed = true) }
            single { mockk<LanguageRepository>(relaxed = true) }
            single { bibleReaderRepository }
            factory { mockk<CopyManager>(relaxed = true) }
            factory { mockk<ShareManager>(relaxed = true) }
        }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // ----- BibleReaderViewModel

    @Test
    fun `module resolves BibleReaderViewModel with parameters`() {
        val koin =
            koinApplication {
                allowOverride(true)
                modules(PlatformReaderKoinModule, testDependenciesModule)
            }.koin

        koin.get<BibleReaderViewModel> { parametersOf(null, null) }
        koin.close()
    }
}
