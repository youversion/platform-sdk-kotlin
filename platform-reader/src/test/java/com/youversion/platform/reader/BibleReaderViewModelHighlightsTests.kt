package com.youversion.platform.reader

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.highlights.domain.BibleHighlightsRepository
import com.youversion.platform.core.highlights.models.BibleHighlight
import com.youversion.platform.reader.domain.BibleReaderRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BibleReaderViewModelHighlightsTests {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var bibleHighlightsRepository: BibleHighlightsRepository
    private lateinit var viewModel: BibleReaderViewModel

    private val defaultReference =
        BibleReference(
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
        )
    private val verseOne = defaultReference.copy(verseStart = 1, verseEnd = 1)
    private val verseTwo = defaultReference.copy(verseStart = 2, verseEnd = 2)

    private val yellow = "#ffff00"
    private val blue = "#0000ff"

    private val highlightsByReference = mutableMapOf<BibleReference, List<BibleHighlight>>()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        val bibleReaderRepository = mockk<BibleReaderRepository>(relaxed = true)
        every { bibleReaderRepository.produceBibleReference(any()) } returns defaultReference

        bibleHighlightsRepository = mockk(relaxed = true)
        every { bibleHighlightsRepository.highlights(overlapping = any()) } answers {
            highlightsByReference[firstArg<BibleReference>()] ?: emptyList()
        }

        viewModel =
            BibleReaderViewModel(
                bibleReference = null,
                fontDefinitionProvider = null,
                bibleVersionRepository = mockk(relaxed = true),
                bibleReaderRepository = bibleReaderRepository,
                userSettingsRepository = mockk(relaxed = true),
                bibleChapterRepository = mockk(relaxed = true),
                languageRepository = mockk(relaxed = true),
                bibleHighlightsRepository = bibleHighlightsRepository,
                copyManager = mockk(relaxed = true),
                shareManager = mockk(relaxed = true),
            )
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        highlightsByReference.clear()
    }

    private fun selectVerses(vararg references: BibleReference) {
        references.forEach { viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(it)) }
    }

    private fun highlight(
        reference: BibleReference,
        hexColor: String,
    ) {
        highlightsByReference[reference] = listOf(BibleHighlight(bibleReference = reference, hexColor = hexColor))
    }

    // ----- AddHighlight

    @Test
    fun `AddHighlight highlights the selected verses and clears the selection`() {
        selectVerses(verseOne, verseTwo)

        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        verify { bibleHighlightsRepository.addHighlights(match { it.toSet() == setOf(verseOne, verseTwo) }, yellow) }
        assertTrue(
            viewModel.state.value.selectedVerses
                .isEmpty(),
        )
        assertFalse(viewModel.state.value.showVerseActionSheet)
    }

    @Test
    fun `AddHighlight with no selection does not call the repository`() {
        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        verify(exactly = 0) { bibleHighlightsRepository.addHighlights(any(), any()) }
    }

    // ----- RemoveHighlight

    @Test
    fun `RemoveHighlight forwards the selection and color to the repository and clears the selection`() {
        selectVerses(verseOne, verseTwo)

        viewModel.onAction(BibleReaderViewModel.Action.RemoveHighlight(yellow))

        verify {
            bibleHighlightsRepository.removeHighlights(
                match { it.toSet() == setOf(verseOne, verseTwo) },
                matchingColor = yellow,
            )
        }
        assertTrue(
            viewModel.state.value.selectedVerses
                .isEmpty(),
        )
        assertFalse(viewModel.state.value.showVerseActionSheet)
    }

    @Test
    fun `RemoveHighlight with no selection does not call the repository`() {
        viewModel.onAction(BibleReaderViewModel.Action.RemoveHighlight(yellow))

        verify(exactly = 0) { bibleHighlightsRepository.removeHighlights(any(), any<String>()) }
    }

    // ----- Color presence helpers

    @Test
    fun `isColorPresentOnAnySelectedVerses is true when a single selected verse carries the color`() {
        highlight(verseOne, yellow)
        selectVerses(verseOne, verseTwo)

        assertTrue(viewModel.isColorPresentOnAnySelectedVerses(yellow))
        assertFalse(viewModel.isColorPresentOnAllSelectedVerses(yellow))
    }

    @Test
    fun `isColorPresentOnAllSelectedVerses is true only when every selected verse carries the color`() {
        highlight(verseOne, yellow)
        highlight(verseTwo, yellow)
        selectVerses(verseOne, verseTwo)

        assertTrue(viewModel.isColorPresentOnAllSelectedVerses(yellow))
        assertTrue(viewModel.isColorPresentOnAnySelectedVerses(yellow))
    }

    @Test
    fun `color presence helpers are false when nothing is selected`() {
        assertFalse(viewModel.isColorPresentOnAnySelectedVerses(yellow))
        assertFalse(viewModel.isColorPresentOnAllSelectedVerses(yellow))
    }

    @Test
    fun `color comparison ignores the hash prefix and case`() {
        highlight(verseOne, "#FFFF00")
        selectVerses(verseOne)

        assertTrue(viewModel.isColorPresentOnAnySelectedVerses("ffff00"))
        assertTrue(viewModel.isColorPresentOnAllSelectedVerses("ffff00"))
    }
}
