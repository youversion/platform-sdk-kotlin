package com.youversion.platform.reader.di

import android.content.ClipboardManager
import android.content.Context
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.domain.Storage
import com.youversion.platform.core.languages.domain.LanguageRepository
import com.youversion.platform.reader.BibleReaderViewModel
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.domain.CopyManager
import com.youversion.platform.reader.domain.ShareManager
import com.youversion.platform.reader.domain.UserSettingsRepository
import com.youversion.platform.reader.screens.languages.LanguagesViewModel
import com.youversion.platform.reader.screens.versions.VersionsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.Koin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class PlatformReaderKoinModuleTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val mockContext =
        mockk<Context>(relaxed = true) {
            every { getSystemService(ClipboardManager::class.java) } returns mockk(relaxed = true)
        }

    private val mockStorage =
        mockk<Storage>(relaxed = true) {
            every { getStringOrNull(any()) } returns null
        }

    private val upstreamDependencies =
        module {
            single<Storage> { mockStorage }
            single<BibleVersionRepository> { mockk(relaxed = true) }
            single<BibleChapterRepository> { mockk(relaxed = true) }
            single<LanguageRepository> { mockk(relaxed = true) }
            single<Context> { mockContext }
        }

    private lateinit var koin: Koin

    private fun createKoin(): Koin {
        koin =
            koinApplication {
                modules(upstreamDependencies, PlatformReaderKoinModule)
            }.koin
        return koin
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun teardown() {
        if (::koin.isInitialized) koin.close()
        Dispatchers.resetMain()
    }

    // ----- Singleton Bindings

    @Test
    fun `BibleReaderRepository resolves as singleton`() {
        val koin = createKoin()

        val first = koin.get<BibleReaderRepository>()
        val second = koin.get<BibleReaderRepository>()

        assertSame(first, second)
    }

    // ----- Factory Bindings

    @Test
    fun `UserSettingsRepository resolves as factory with new instance each time`() {
        val koin = createKoin()

        val first = koin.get<UserSettingsRepository>()
        val second = koin.get<UserSettingsRepository>()

        assertNotSame(first, second)
    }

    @Test
    fun `CopyManager resolves as factory with new instance each time`() {
        val koin = createKoin()

        val first = koin.get<CopyManager>()
        val second = koin.get<CopyManager>()

        assertNotSame(first, second)
    }

    @Test
    fun `ShareManager resolves as factory with new instance each time`() {
        val koin = createKoin()

        val first = koin.get<ShareManager>()
        val second = koin.get<ShareManager>()

        assertNotSame(first, second)
    }

    // ----- ViewModel Factory Bindings

    @Test
    fun `BibleReaderViewModel resolves with parameter injection`() {
        val koin = createKoin()
        val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)

        val viewModel = koin.get<BibleReaderViewModel> { parametersOf(reference, null) }

        assertEquals("GEN", viewModel.state.value.bibleReference.bookUSFM)
    }

    @Test
    fun `BibleReaderViewModel resolves with null parameters`() {
        val koin = createKoin()

        koin.get<BibleReaderViewModel> { parametersOf(null, null) }
    }

    @Test
    fun `VersionsViewModel resolves with BibleReaderRepository injected`() {
        val koin = createKoin()

        koin.get<VersionsViewModel>()
    }

    @Test
    fun `LanguagesViewModel resolves with parameter and BibleReaderRepository injected`() {
        val koin = createKoin()
        val bibleVersion = BibleVersion(id = 1, abbreviation = "KJV")

        koin.get<LanguagesViewModel> { parametersOf(bibleVersion) }
    }

    @Test
    fun `LanguagesViewModel resolves with null BibleVersion parameter`() {
        val koin = createKoin()

        koin.get<LanguagesViewModel> { parametersOf(null) }
    }
}
