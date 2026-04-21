package com.youversion.platform.reader

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.domain.UserSettingsRepository
import com.youversion.platform.reader.theme.FontDefinitionProvider
import com.youversion.platform.reader.theme.ReaderTheme
import com.youversion.platform.reader.theme.ui.BibleReaderTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BibleReaderViewModelInitTests {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var bibleVersionRepository: BibleVersionRepository
    private lateinit var bibleReaderRepository: BibleReaderRepository
    private lateinit var userSettingsRepository: UserSettingsRepository

    private val defaultReference =
        BibleReference(
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
        )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        bibleVersionRepository = mockk(relaxed = true)
        bibleReaderRepository = mockk(relaxed = true)
        userSettingsRepository = mockk(relaxed = true)

        every { bibleReaderRepository.produceBibleReference(any()) } returns defaultReference
        every { userSettingsRepository.readerThemeId } returns null
        every { userSettingsRepository.readerFontFamilyName } returns null
        every { userSettingsRepository.readerFontSize } returns null
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        BibleReaderTheme.selectedColorScheme.value = null
    }

    private fun createViewModel(
        bibleReference: BibleReference? = null,
        fontDefinitionProvider: FontDefinitionProvider? = null,
    ): BibleReaderViewModel =
        BibleReaderViewModel(
            bibleReference = bibleReference,
            fontDefinitionProvider = fontDefinitionProvider,
            bibleVersionRepository = bibleVersionRepository,
            bibleReaderRepository = bibleReaderRepository,
            userSettingsRepository = userSettingsRepository,
            bibleChapterRepository = mockk(relaxed = true),
            languageRepository = mockk(relaxed = true),
            copyManager = mockk(relaxed = true),
            shareManager = mockk(relaxed = true),
        )

    // ----- Constructor

    @Test
    fun `constructor passes bibleReference to produceBibleReference`() =
        runTest(testDispatcher) {
            val providedReference = BibleReference(versionId = 5, bookUSFM = "PSA", chapter = 23)
            every { bibleReaderRepository.produceBibleReference(providedReference) } returns providedReference

            val vm = createViewModel(bibleReference = providedReference)

            verify { bibleReaderRepository.produceBibleReference(providedReference) }
            assertEquals(providedReference, vm.state.value.bibleReference)
        }

    @Test
    fun `constructor with fontDefinitionProvider includes provided fonts`() =
        runTest(testDispatcher) {
            val customFont = FontDefinition("Custom Font", FontFamily.Cursive)
            val provider =
                mockk<FontDefinitionProvider> {
                    every { fonts() } returns listOf(customFont)
                }

            val vm = createViewModel(fontDefinitionProvider = provider)

            assertEquals(1, vm.state.value.providedFontDefinitions.size)
            assertEquals(
                customFont,
                vm.state.value.providedFontDefinitions
                    .first(),
            )
            assertTrue(
                vm.state.value.allFontDefinitions
                    .contains(customFont),
            )
        }

    @Test
    fun `constructor without fontDefinitionProvider has empty provided fonts`() =
        runTest(testDispatcher) {
            val vm = createViewModel()

            assertEquals(0, vm.state.value.providedFontDefinitions.size)
        }

    // ----- loadUserSettingsFromStorage

    @Test
    fun `init restores saved theme from storage`() =
        runTest(testDispatcher) {
            val sepiaTheme = ReaderTheme.allThemes.first { it.id == 2 }
            every { userSettingsRepository.readerThemeId } returns sepiaTheme.id

            createViewModel()

            assertEquals(sepiaTheme.colorScheme, BibleReaderTheme.selectedColorScheme.value)
        }

    @Test
    fun `init restores default theme when saved theme id is null`() =
        runTest(testDispatcher) {
            every { userSettingsRepository.readerThemeId } returns null

            createViewModel()

            val defaultTheme = ReaderTheme.themeById(null)
            assertEquals(defaultTheme.colorScheme, BibleReaderTheme.selectedColorScheme.value)
        }

    @Test
    fun `init restores saved font family when name matches available font`() =
        runTest(testDispatcher) {
            every { userSettingsRepository.readerFontFamilyName } returns "Serif"

            val vm = createViewModel()

            assertEquals("Serif", vm.state.value.selectedFontDefinition.fontName)
        }

    @Test
    fun `init does not change font when saved name does not match any font`() =
        runTest(testDispatcher) {
            every { userSettingsRepository.readerFontFamilyName } returns "NonExistentFont"

            val vm = createViewModel()

            assertEquals(ReaderFontSettings.DEFAULT_FONT_DEFINITION, vm.state.value.selectedFontDefinition)
        }

    @Test
    fun `init restores saved font size`() =
        runTest(testDispatcher) {
            every { userSettingsRepository.readerFontSize } returns 24f

            val vm = createViewModel()

            assertEquals(24.sp, vm.state.value.fontSize)
        }

    @Test
    fun `init uses defaults when all settings are null`() =
        runTest(testDispatcher) {
            val vm = createViewModel()

            assertEquals(ReaderFontSettings.DEFAULT_FONT_DEFINITION, vm.state.value.selectedFontDefinition)
            assertEquals(ReaderFontSettings.DEFAULT_FONT_SIZE, vm.state.value.fontSize)
        }

    @Test
    fun `init handles exception from version loading`() =
        runTest(testDispatcher) {
            coEvery { bibleVersionRepository.version(id = any()) } throws RuntimeException("Network error")

            val vm = createViewModel()
            testDispatcher.scheduler.advanceUntilIdle()

            assertNull(vm.state.value.bibleVersion)
        }
}
