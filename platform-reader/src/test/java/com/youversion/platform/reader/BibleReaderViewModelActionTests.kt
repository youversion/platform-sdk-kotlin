package com.youversion.platform.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleBookIntro
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.domain.UserSettingsRepository
import com.youversion.platform.ui.theme.ReaderTheme
import com.youversion.platform.ui.theme.ui.BibleReaderTheme
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var userSettingsRepository: UserSettingsRepository
    private lateinit var bibleVersionRepository: BibleVersionRepository
    private lateinit var viewModel: BibleReaderViewModel

    private val defaultReference =
        BibleReference(
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
        )

    private val versionWithGenesisIntro =
        BibleVersion(
            id = 1,
            abbreviation = "KJV",
            books =
                listOf(
                    BibleBook(
                        id = "GEN",
                        title = "Genesis",
                        fullTitle = null,
                        abbreviation = null,
                        canon = null,
                        chapters = null,
                        intro =
                            BibleBookIntro(
                                id = "GEN.intro",
                                passageId = "GEN.intro",
                                title = "Introduction",
                            ),
                    ),
                ),
            copyright = "Public Domain",
        )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        val bibleReaderRepository = mockk<BibleReaderRepository>(relaxed = true)
        userSettingsRepository = mockk(relaxed = true)
        bibleVersionRepository = mockk(relaxed = true)

        // Explicit null stubs — relaxed mockk otherwise returns 0f for `Float?` getters, which
        // would poison the ViewModel's default state when it restores from storage on init.
        every { userSettingsRepository.readerLineSpacingFraction } returns null

        every { bibleReaderRepository.produceBibleReference(any()) } returns defaultReference

        viewModel =
            BibleReaderViewModel(
                bibleReference = null,
                fontDefinitionProvider = null,
                bibleVersionRepository = bibleVersionRepository,
                bibleReaderRepository = bibleReaderRepository,
                userSettingsRepository = userSettingsRepository,
                bibleChapterRepository = mockk(relaxed = true),
                languageRepository = mockk(relaxed = true),
                bibleHighlightsRepository = mockk(relaxed = true),
                copyManager = mockk(relaxed = true),
                shareManager = mockk(relaxed = true),
            )
    }

    private fun verseReference(verse: Int) =
        viewModel.state.value.bibleReference
            .copy(verseStart = verse, verseEnd = verse)

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        BibleReaderTheme.selectedColorScheme.value = null
    }

    // ----- Font Settings

    @Test
    fun `OpenFontSettings sets showingFontList to true`() {
        viewModel.onAction(BibleReaderViewModel.Action.OpenFontSettings)

        assertTrue(viewModel.state.value.showingFontList)
    }

    @Test
    fun `CloseFontSettings sets showingFontList to false`() {
        viewModel.onAction(BibleReaderViewModel.Action.OpenFontSettings)
        assertTrue(viewModel.state.value.showingFontList)

        viewModel.onAction(BibleReaderViewModel.Action.CloseFontSettings)

        assertFalse(viewModel.state.value.showingFontList)
    }

    // ----- Search

    @Test
    fun `OpenSearch sets showingSearch to true`() {
        viewModel.onAction(BibleReaderViewModel.Action.OpenSearch)

        assertTrue(viewModel.state.value.showingSearch)
    }

    @Test
    fun `CloseSearch sets showingSearch to false`() {
        viewModel.onAction(BibleReaderViewModel.Action.OpenSearch)
        assertTrue(viewModel.state.value.showingSearch)

        viewModel.onAction(BibleReaderViewModel.Action.CloseSearch)

        assertFalse(viewModel.state.value.showingSearch)
    }

    // ----- Font Size

    @Test
    fun `DecreaseFontSize reduces font size and persists`() {
        val initialSize = viewModel.state.value.fontSize
        val expectedSize = ReaderFontSettings.nextSmallerFontSize(initialSize)

        viewModel.onAction(BibleReaderViewModel.Action.DecreaseFontSize)

        assertEquals(expectedSize, viewModel.state.value.fontSize)
        verify { userSettingsRepository.readerFontSize = expectedSize.value }
    }

    @Test
    fun `IncreaseFontSize increases font size and persists`() {
        val initialSize = viewModel.state.value.fontSize
        val expectedSize = ReaderFontSettings.nextLargerFontSize(initialSize)

        viewModel.onAction(BibleReaderViewModel.Action.IncreaseFontSize)

        assertEquals(expectedSize, viewModel.state.value.fontSize)
        verify { userSettingsRepository.readerFontSize = expectedSize.value }
    }

    // ----- Line Spacing

    @Test
    fun `CycleLineSpacing advances through the available fractions and wraps`() {
        // Default seeded state is 0.4f. Cycling should walk 0.4 -> 0.6 -> 0.3 -> 0.4.
        assertEquals(ReaderFontSettings.DEFAULT_LINE_SPACING_FRACTION, viewModel.state.value.lineSpacingFraction)

        viewModel.onAction(BibleReaderViewModel.Action.CycleLineSpacing)
        assertEquals(0.6f, viewModel.state.value.lineSpacingFraction)
        verify { userSettingsRepository.readerLineSpacingFraction = 0.6f }

        viewModel.onAction(BibleReaderViewModel.Action.CycleLineSpacing)
        assertEquals(0.3f, viewModel.state.value.lineSpacingFraction)
        verify { userSettingsRepository.readerLineSpacingFraction = 0.3f }

        viewModel.onAction(BibleReaderViewModel.Action.CycleLineSpacing)
        assertEquals(0.4f, viewModel.state.value.lineSpacingFraction)
        verify { userSettingsRepository.readerLineSpacingFraction = 0.4f }
    }

    // ----- SetFontDefinition

    @Test
    fun `SetFontDefinition persists font family name and updates state`() {
        val monoFont = FontDefinition("Monospace", FontFamily.Monospace)

        viewModel.onAction(BibleReaderViewModel.Action.SetFontDefinition(monoFont))

        assertEquals(monoFont, viewModel.state.value.selectedFontDefinition)
        verify { userSettingsRepository.readerFontFamilyName = "Monospace" }
    }

    // ----- Footnotes

    @Test
    fun `OpenFootnotes sets showing state and stores reference and footnotes`() {
        val footnoteRef = defaultReference.copy(verseStart = 5, verseEnd = 5)
        val footnotes = listOf(AnnotatedString("footnote text"))

        viewModel.onAction(BibleReaderViewModel.Action.OpenFootnotes(footnoteRef, footnotes))

        val state = viewModel.state.value
        assertTrue(state.showingFootnotes)
        assertEquals(footnoteRef, state.footnotesReference)
        assertEquals(footnotes, state.footnotes)
    }

    @Test
    fun `CloseFootnotes clears showing state and reference and footnotes`() {
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
    fun `OpenIntroFootnotes sets showing state and stores intro footnotes`() {
        val introFootnotes = listOf(AnnotatedString("intro footnote"))

        viewModel.onAction(BibleReaderViewModel.Action.OpenIntroFootnotes(introFootnotes))

        val state = viewModel.state.value
        assertTrue(state.showingIntroFootnotes)
        assertEquals(introFootnotes, state.introFootnotes)
    }

    @Test
    fun `CloseIntroFootnotes clears showing state and intro footnotes`() {
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
    fun `SetReaderTheme updates color scheme and persists theme id`() {
        val charcoalTheme = ReaderTheme.allThemes.first { it.id == 5 }

        viewModel.onAction(BibleReaderViewModel.Action.SetReaderTheme(charcoalTheme))

        assertEquals(charcoalTheme.colorScheme, BibleReaderTheme.selectedColorScheme.value)
        verify { userSettingsRepository.readerThemeId = charcoalTheme.id }
    }

    // ----- Scroll Target

    @Test
    fun `ScrollToReference stages the reference on state`() {
        val verse = verseReference(12)

        viewModel.onAction(BibleReaderViewModel.Action.ScrollToReference(verse))

        assertEquals(verse, viewModel.state.value.scrollTargetReference)
    }

    @Test
    fun `ScrollToReference replaces an unconsumed staged reference`() {
        viewModel.onAction(BibleReaderViewModel.Action.ScrollToReference(verseReference(3)))

        val later = verseReference(9)
        viewModel.onAction(BibleReaderViewModel.Action.ScrollToReference(later))

        assertEquals(later, viewModel.state.value.scrollTargetReference)
    }

    @Test
    fun `ScrollTargetReached clears the staged reference`() {
        viewModel.onAction(BibleReaderViewModel.Action.ScrollToReference(verseReference(12)))

        viewModel.onAction(BibleReaderViewModel.Action.ScrollTargetReached)

        assertNull(viewModel.state.value.scrollTargetReference)
    }

    @Test
    fun `no scroll target is staged by default`() {
        assertNull(viewModel.state.value.scrollTargetReference)
    }

    // ----- Focused Reference

    @Test
    fun `FocusReference points the reader at the verse`() {
        val verse = verseReference(12)

        viewModel.onAction(BibleReaderViewModel.Action.FocusReference(verse))

        assertEquals(verse, viewModel.state.value.focusedReference)
    }

    @Test
    fun `FocusReference ignores a whole-chapter reference`() {
        viewModel.onAction(BibleReaderViewModel.Action.FocusReference(defaultReference))

        assertNull(viewModel.state.value.focusedReference)
    }

    @Test
    fun `FocusReference ignores a verse outside the chapter on display`() {
        val otherChapterVerse = verseReference(5).copy(chapter = viewModel.bibleReference.chapter + 1)

        viewModel.onAction(BibleReaderViewModel.Action.FocusReference(otherChapterVerse))

        assertNull(viewModel.state.value.focusedReference)
    }

    @Test
    fun `ClearFocusedReference lifts the focus`() {
        viewModel.onAction(BibleReaderViewModel.Action.FocusReference(verseReference(12)))

        viewModel.onAction(BibleReaderViewModel.Action.ClearFocusedReference)

        assertNull(viewModel.state.value.focusedReference)
    }

    @Test
    fun `tapping a verse lifts the focus`() {
        viewModel.onAction(BibleReaderViewModel.Action.FocusReference(verseReference(12)))

        viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseReference(3)))

        assertNull(viewModel.state.value.focusedReference)
    }

    @Test
    fun `opening font settings lifts the focus`() {
        viewModel.onAction(BibleReaderViewModel.Action.FocusReference(verseReference(12)))

        viewModel.onAction(BibleReaderViewModel.Action.OpenFontSettings)

        assertNull(viewModel.state.value.focusedReference)
    }

    @Test
    fun `opening footnotes lifts the focus`() {
        val verse = verseReference(12)
        viewModel.onAction(BibleReaderViewModel.Action.FocusReference(verse))

        viewModel.onAction(
            BibleReaderViewModel.Action.OpenFootnotes(
                reference = verse,
                footnotes = listOf(AnnotatedString("a note")),
            ),
        )

        assertNull(viewModel.state.value.focusedReference)
    }

    @Test
    fun `opening intro footnotes lifts the focus`() {
        viewModel.onAction(BibleReaderViewModel.Action.FocusReference(verseReference(12)))

        viewModel.onAction(
            BibleReaderViewModel.Action.OpenIntroFootnotes(footnotes = listOf(AnnotatedString("a note"))),
        )

        assertNull(viewModel.state.value.focusedReference)
    }

    @Test
    fun `navigating to another chapter lifts the focus`() {
        viewModel.onAction(BibleReaderViewModel.Action.FocusReference(verseReference(12)))

        viewModel.bibleReference = verseReference(3).copy(chapter = 2)

        assertNull(viewModel.state.value.focusedReference)
    }

    @Test
    fun `navigating within the chapter that contains the focus keeps it`() {
        val verse = verseReference(12)
        viewModel.onAction(BibleReaderViewModel.Action.FocusReference(verse))

        viewModel.bibleReference = viewModel.bibleReference

        assertEquals(verse, viewModel.state.value.focusedReference)
    }

    @Test
    fun `focusing a verse leaves the reader's own selection alone`() {
        val selected = verseReference(3)
        viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(selected))

        viewModel.onAction(BibleReaderViewModel.Action.FocusReference(verseReference(12)))

        assertEquals(setOf(selected), viewModel.state.value.selectedVerses)
    }

    @Test
    fun `entering a book intro lifts the focus`() {
        viewModel.bibleVersion = versionWithGenesisIntro
        viewModel.onAction(BibleReaderViewModel.Action.FocusReference(verseReference(12)))

        viewModel.onAction(BibleReaderViewModel.Action.GoToPreviousChapter)

        assertTrue(viewModel.state.value.isViewingIntro)
        assertNull(viewModel.state.value.focusedReference)
    }

    // ----- Search Results

    @Test
    fun `GoToSearchResult takes the reader to the chapter containing the result`() {
        val verse = verseReference(12).copy(chapter = 3)

        viewModel.onAction(BibleReaderViewModel.Action.GoToSearchResult(verse))

        assertEquals(verse.copy(verseStart = null, verseEnd = null), viewModel.bibleReference)
    }

    @Test
    fun `GoToSearchResult stages the verse as the scroll target`() {
        val verse = verseReference(12).copy(chapter = 3)

        viewModel.onAction(BibleReaderViewModel.Action.GoToSearchResult(verse))

        assertEquals(verse, viewModel.state.value.scrollTargetReference)
    }

    @Test
    fun `GoToSearchResult closes search`() {
        viewModel.onAction(BibleReaderViewModel.Action.OpenSearch)

        viewModel.onAction(BibleReaderViewModel.Action.GoToSearchResult(verseReference(12)))

        assertFalse(viewModel.state.value.showingSearch)
    }

    @Test
    fun `the result is not focused before its chapter has loaded`() {
        viewModel.onAction(BibleReaderViewModel.Action.GoToSearchResult(verseReference(12).copy(chapter = 3)))

        assertNull(viewModel.state.value.focusedReference)
    }

    @Test
    fun `the staged focus outlives the move and takes once the chapter has loaded`() {
        val verse = verseReference(12).copy(chapter = 3)
        viewModel.onAction(BibleReaderViewModel.Action.GoToSearchResult(verse))

        viewModel.onAction(BibleReaderViewModel.Action.ScrollTargetReached)

        assertEquals(verse, viewModel.state.value.focusedReference)
    }

    @Test
    fun `the staged focus is consumed once`() {
        viewModel.onAction(BibleReaderViewModel.Action.GoToSearchResult(verseReference(12)))
        viewModel.onAction(BibleReaderViewModel.Action.ScrollTargetReached)
        viewModel.onAction(BibleReaderViewModel.Action.ClearFocusedReference)

        viewModel.onAction(BibleReaderViewModel.Action.ScrollTargetReached)

        assertNull(viewModel.state.value.focusedReference)
    }

    @Test
    fun `a scroll the reader started itself focuses nothing when it lands`() {
        viewModel.onAction(BibleReaderViewModel.Action.ScrollToReference(verseReference(12)))

        viewModel.onAction(BibleReaderViewModel.Action.ScrollTargetReached)

        assertNull(viewModel.state.value.focusedReference)
    }

    @Test
    fun `a result from another version arrives in that version, focused`() {
        val otherVersion = BibleVersion(id = 2, abbreviation = "ESV", books = emptyList())
        coEvery { bibleVersionRepository.version(id = 2) } returns otherVersion
        val verse = BibleReference(versionId = 2, bookUSFM = "JHN", chapter = 3, verse = 16)

        viewModel.onAction(BibleReaderViewModel.Action.GoToSearchResult(verse))
        viewModel.onAction(BibleReaderViewModel.Action.ScrollTargetReached)

        assertEquals(BibleReference(versionId = 2, bookUSFM = "JHN", chapter = 3), viewModel.bibleReference)
        assertEquals(otherVersion, viewModel.bibleVersion)
        assertEquals(verse, viewModel.state.value.focusedReference)
    }

    @Test
    fun `opening search lifts the focus`() {
        viewModel.onAction(BibleReaderViewModel.Action.GoToSearchResult(verseReference(12)))
        viewModel.onAction(BibleReaderViewModel.Action.ScrollTargetReached)

        viewModel.onAction(BibleReaderViewModel.Action.OpenSearch)

        assertNull(viewModel.state.value.focusedReference)
    }

    @Test
    fun `a staged focus does not follow the reader into an unrelated chapter`() {
        viewModel.onAction(BibleReaderViewModel.Action.GoToSearchResult(verseReference(12).copy(chapter = 3)))

        viewModel.bibleReference = viewModel.bibleReference.copy(chapter = 9)
        viewModel.onAction(BibleReaderViewModel.Action.ScrollTargetReached)

        assertNull(viewModel.state.value.focusedReference)
    }

    @Test
    fun `closing search without a result leaves the reader alone`() {
        viewModel.onAction(BibleReaderViewModel.Action.OpenSearch)
        val readerReference = viewModel.bibleReference

        viewModel.onAction(BibleReaderViewModel.Action.CloseSearch)

        assertEquals(readerReference, viewModel.bibleReference)
        assertNull(viewModel.state.value.scrollTargetReference)
        assertNull(viewModel.state.value.focusedReference)
    }

    @Test
    fun `no reference is focused by default`() {
        assertNull(viewModel.state.value.focusedReference)
    }
}
