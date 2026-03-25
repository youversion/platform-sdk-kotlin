package com.youversion.platform.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.domain.UserSettingsRepository
import com.youversion.platform.reader.theme.ReaderTheme
import com.youversion.platform.reader.theme.ui.BibleReaderTheme
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BibleReaderViewModelActionTests {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userSettingsRepository: UserSettingsRepository
    private lateinit var viewModel: BibleReaderViewModel

    private val defaultReference =
        BibleReference(
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
        )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        val bibleReaderRepository = mockk<BibleReaderRepository>(relaxed = true)
        userSettingsRepository = mockk(relaxed = true)

        every { bibleReaderRepository.produceBibleReference(any()) } returns defaultReference
        every { userSettingsRepository.readerThemeId } returns null
        every { userSettingsRepository.readerFontFamilyName } returns null
        every { userSettingsRepository.readerLineSpacing } returns null
        every { userSettingsRepository.readerFontSize } returns null

        viewModel =
            BibleReaderViewModel(
                bibleReference = null,
                fontDefinitionProvider = null,
                bibleVersionRepository = mockk(relaxed = true),
                bibleReaderRepository = bibleReaderRepository,
                userSettingsRepository = userSettingsRepository,
                bibleChapterRepository = mockk(relaxed = true),
                copyManager = mockk(relaxed = true),
                shareManager = mockk(relaxed = true),
            )
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        BibleReaderTheme.selectedColorScheme.value = null
    }

    // ----- Font Settings

    @Test
    fun `OpenFontSettings sets showingFontList to true`() =
        runTest(testDispatcher) {
            viewModel.onAction(BibleReaderViewModel.Action.OpenFontSettings)

            assertTrue(viewModel.state.value.showingFontList)
        }

    @Test
    fun `CloseFontSettings sets showingFontList to false`() =
        runTest(testDispatcher) {
            viewModel.onAction(BibleReaderViewModel.Action.OpenFontSettings)
            assertTrue(viewModel.state.value.showingFontList)

            viewModel.onAction(BibleReaderViewModel.Action.CloseFontSettings)

            assertFalse(viewModel.state.value.showingFontList)
        }

    // ----- Font Size

    @Test
    fun `DecreaseFontSize reduces font size and persists`() =
        runTest(testDispatcher) {
            val initialSize = viewModel.state.value.fontSize
            val expectedSize = ReaderFontSettings.nextSmallerFontSize(initialSize)

            viewModel.onAction(BibleReaderViewModel.Action.DecreaseFontSize)

            assertEquals(expectedSize, viewModel.state.value.fontSize)
            verify { userSettingsRepository.readerFontSize = expectedSize.value }
        }

    @Test
    fun `IncreaseFontSize increases font size and persists`() =
        runTest(testDispatcher) {
            val initialSize = viewModel.state.value.fontSize
            val expectedSize = ReaderFontSettings.nextLargerFontSize(initialSize)

            viewModel.onAction(BibleReaderViewModel.Action.IncreaseFontSize)

            assertEquals(expectedSize, viewModel.state.value.fontSize)
            verify { userSettingsRepository.readerFontSize = expectedSize.value }
        }

    // ----- Line Spacing

    @Test
    fun `NextLineSpacingMultiplierOption cycles spacing and persists`() =
        runTest(testDispatcher) {
            val initialSpacing = viewModel.state.value.lineSpacingMultiplier
            val expectedSpacing = ReaderFontSettings.nextLineSpacingMultiplier(initialSpacing)

            viewModel.onAction(BibleReaderViewModel.Action.NextLineSpacingMultiplierOption)

            assertEquals(expectedSpacing, viewModel.state.value.lineSpacingMultiplier)
            verify { userSettingsRepository.readerLineSpacing = expectedSpacing }
        }

    // ----- SetFontDefinition

    @Test
    fun `SetFontDefinition persists font family name and updates state`() =
        runTest(testDispatcher) {
            val monoFont = FontDefinition("Monospace", FontFamily.Monospace)

            viewModel.onAction(BibleReaderViewModel.Action.SetFontDefinition(monoFont))

            assertEquals(monoFont, viewModel.state.value.selectedFontDefinition)
            verify { userSettingsRepository.readerFontFamilyName = "Monospace" }
        }

    // ----- Footnotes

    @Test
    fun `OpenFootnotes sets showing state and stores reference and footnotes`() =
        runTest(testDispatcher) {
            val footnoteRef = defaultReference.copy(verseStart = 5, verseEnd = 5)
            val footnotes = listOf(AnnotatedString("footnote text"))

            viewModel.onAction(BibleReaderViewModel.Action.OpenFootnotes(footnoteRef, footnotes))

            val state = viewModel.state.value
            assertTrue(state.showingFootnotes)
            assertEquals(footnoteRef, state.footnotesReference)
            assertEquals(footnotes, state.footnotes)
        }

    @Test
    fun `CloseFootnotes clears showing state and reference and footnotes`() =
        runTest(testDispatcher) {
            val footnoteRef = defaultReference.copy(verseStart = 5, verseEnd = 5)
            val footnotes = listOf(AnnotatedString("note"))
            viewModel.onAction(BibleReaderViewModel.Action.OpenFootnotes(footnoteRef, footnotes))

            assertTrue(viewModel.state.value.showingFootnotes)
            assertEquals(footnoteRef, viewModel.state.value.footnotesReference)
            assertEquals(1, viewModel.state.value.footnotes.size)

            viewModel.onAction(BibleReaderViewModel.Action.CloseFootnotes)

            val state = viewModel.state.value
            assertFalse(state.showingFootnotes)
            assertNull(state.footnotesReference)
            assertEquals(0, state.footnotes.size)
        }

    @Test
    fun `OpenIntroFootnotes sets showing state and stores intro footnotes`() =
        runTest(testDispatcher) {
            val introFootnotes = listOf(AnnotatedString("intro footnote"))

            viewModel.onAction(BibleReaderViewModel.Action.OpenIntroFootnotes(introFootnotes))

            val state = viewModel.state.value
            assertTrue(state.showingIntroFootnotes)
            assertEquals(introFootnotes, state.introFootnotes)
        }

    @Test
    fun `CloseIntroFootnotes clears showing state and intro footnotes`() =
        runTest(testDispatcher) {
            val introFootnotes = listOf(AnnotatedString("intro note"))
            viewModel.onAction(BibleReaderViewModel.Action.OpenIntroFootnotes(introFootnotes))

            assertTrue(viewModel.state.value.showingIntroFootnotes)
            assertEquals(1, viewModel.state.value.introFootnotes.size)

            viewModel.onAction(BibleReaderViewModel.Action.CloseIntroFootnotes)

            val state = viewModel.state.value
            assertFalse(state.showingIntroFootnotes)
            assertEquals(0, state.introFootnotes.size)
        }

    // ----- SetReaderTheme

    @Test
    fun `SetReaderTheme updates color scheme and persists theme id`() =
        runTest(testDispatcher) {
            val charcoalTheme = ReaderTheme.allThemes.first { it.id == 5 }

            viewModel.onAction(BibleReaderViewModel.Action.SetReaderTheme(charcoalTheme))

            assertEquals(charcoalTheme.colorScheme, BibleReaderTheme.selectedColorScheme.value)
            verify { userSettingsRepository.readerThemeId = charcoalTheme.id }
        }
}
