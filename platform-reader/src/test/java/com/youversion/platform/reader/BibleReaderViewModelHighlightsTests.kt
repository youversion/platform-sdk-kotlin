package com.youversion.platform.reader

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.highlights.domain.BibleHighlightsRepository
import com.youversion.platform.core.highlights.models.BibleHighlight
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.domain.PendingHighlight
import com.youversion.platform.reader.sheets.HighlightColor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

    private val pendingHighlightState = MutableStateFlow<PendingHighlight?>(null)

    // Injected sign-in and permission state. Both default to the signed-in-and-granted case so the add/remove tests
    // exercise the immediate-apply path; the permission-flow and sign-in tests flip them before acting.
    private var isUserSignedIn = true
    private var isHighlightsPermissionGranted = true

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        val bibleReaderRepository = mockk<BibleReaderRepository>(relaxed = true)
        every { bibleReaderRepository.produceBibleReference(any()) } returns defaultReference
        every { bibleReaderRepository.pendingHighlightChanges } returns pendingHighlightState
        every { bibleReaderRepository.pendingHighlight } answers { pendingHighlightState.value }
        every { bibleReaderRepository.pendingHighlight = any() } answers {
            pendingHighlightState.value = firstArg()
        }

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
                isSignedIn = { isUserSignedIn },
                hasHighlightsPermission = { isHighlightsPermissionGranted },
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

    // ----- Highlights permission flow

    @Test
    fun `AddHighlight applies immediately and shows no prompt when permission is granted`() {
        isHighlightsPermissionGranted = true
        selectVerses(verseOne)

        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        verify { bibleHighlightsRepository.addHighlights(any(), yellow) }
        assertFalse(viewModel.state.value.showDataExchangeConfirmation)
    }

    @Test
    fun `AddHighlight without permission shows the confirmation and applies nothing`() {
        isHighlightsPermissionGranted = false
        selectVerses(verseOne, verseTwo)

        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        assertTrue(viewModel.state.value.showDataExchangeConfirmation)
        verify(exactly = 0) { bibleHighlightsRepository.addHighlights(any(), any()) }
    }

    @Test
    fun `AddHighlight without permission keeps the verse selection`() {
        isHighlightsPermissionGranted = false
        selectVerses(verseOne, verseTwo)

        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        assertEquals(setOf(verseOne, verseTwo), viewModel.state.value.selectedVerses)
    }

    @Test
    fun `RemoveHighlight without permission shows the confirmation and applies nothing`() {
        isHighlightsPermissionGranted = false
        selectVerses(verseOne)

        viewModel.onAction(BibleReaderViewModel.Action.RemoveHighlight(yellow))

        assertTrue(viewModel.state.value.showDataExchangeConfirmation)
        verify(exactly = 0) { bibleHighlightsRepository.removeHighlights(any(), any<String>()) }
    }

    @Test
    fun `confirming the prompt dismisses it and starts the grant flow`() {
        isHighlightsPermissionGranted = false
        selectVerses(verseOne)
        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        viewModel.onAction(BibleReaderViewModel.Action.ConfirmDataExchange)

        assertFalse(viewModel.state.value.showDataExchangeConfirmation)
        assertTrue(viewModel.state.value.shouldStartDataExchangeFlow)
    }

    @Test
    fun `a granted flow applies the pending highlight and clears the selection`() {
        isHighlightsPermissionGranted = false
        selectVerses(verseOne, verseTwo)
        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))
        viewModel.onAction(BibleReaderViewModel.Action.ConfirmDataExchange)

        viewModel.onAction(BibleReaderViewModel.Action.DataExchangeCompleted(isHighlightsGranted = true))

        verify { bibleHighlightsRepository.addHighlights(match { it.toSet() == setOf(verseOne, verseTwo) }, yellow) }
        assertTrue(
            viewModel.state.value.selectedVerses
                .isEmpty(),
        )
        assertFalse(viewModel.state.value.shouldStartDataExchangeFlow)
    }

    @Test
    fun `a non-granted flow applies nothing and keeps the selection`() {
        isHighlightsPermissionGranted = false
        selectVerses(verseOne)
        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))
        viewModel.onAction(BibleReaderViewModel.Action.ConfirmDataExchange)

        viewModel.onAction(BibleReaderViewModel.Action.DataExchangeCompleted(isHighlightsGranted = false))

        verify(exactly = 0) { bibleHighlightsRepository.addHighlights(any(), any()) }
        assertEquals(setOf(verseOne), viewModel.state.value.selectedVerses)
        assertFalse(viewModel.state.value.shouldStartDataExchangeFlow)
    }

    @Test
    fun `cancelling the prompt clears the pending highlight so a later grant applies nothing`() {
        isHighlightsPermissionGranted = false
        selectVerses(verseOne)
        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        viewModel.onAction(BibleReaderViewModel.Action.CancelDataExchange)
        viewModel.onAction(BibleReaderViewModel.Action.DataExchangeCompleted(isHighlightsGranted = true))

        assertFalse(viewModel.state.value.showDataExchangeConfirmation)
        verify(exactly = 0) { bibleHighlightsRepository.addHighlights(any(), any()) }
    }

    @Test
    fun `cancelling the prompt keeps the verse selection`() {
        isHighlightsPermissionGranted = false
        selectVerses(verseOne, verseTwo)
        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        viewModel.onAction(BibleReaderViewModel.Action.CancelDataExchange)

        assertEquals(setOf(verseOne, verseTwo), viewModel.state.value.selectedVerses)
    }

    @Test
    fun `changing the verse selection clears a pending highlight so a later grant applies nothing`() {
        isHighlightsPermissionGranted = false
        selectVerses(verseOne)
        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        selectVerses(verseTwo)
        viewModel.onAction(BibleReaderViewModel.Action.DataExchangeCompleted(isHighlightsGranted = true))

        verify(exactly = 0) { bibleHighlightsRepository.addHighlights(any(), any()) }
    }

    @Test
    fun `a held highlight applies once permission is granted`() {
        isHighlightsPermissionGranted = false
        selectVerses(verseOne)
        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        isHighlightsPermissionGranted = true
        viewModel.applyPendingHighlightIfPermitted()

        verify { bibleHighlightsRepository.addHighlights(match { it.toSet() == setOf(verseOne) }, yellow) }
    }

    @Test
    fun `a held highlight is kept while permission is still absent`() {
        isHighlightsPermissionGranted = false
        selectVerses(verseOne)
        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        viewModel.applyPendingHighlightIfPermitted()

        verify(exactly = 0) { bibleHighlightsRepository.addHighlights(any(), any()) }
        assertTrue(viewModel.state.value.hasPendingHighlight)
    }

    @Test
    fun `tapping a color again after declining prompts again`() {
        isHighlightsPermissionGranted = false
        selectVerses(verseOne)
        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))
        viewModel.onAction(BibleReaderViewModel.Action.CancelDataExchange)

        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(blue))

        assertTrue(viewModel.state.value.showDataExchangeConfirmation)
    }

    @Test
    fun `recoloring an existing highlight without permission is gated too`() {
        isHighlightsPermissionGranted = false
        highlight(verseOne, yellow)
        selectVerses(verseOne)

        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(blue))

        assertTrue(viewModel.state.value.showDataExchangeConfirmation)
        verify(exactly = 0) { bibleHighlightsRepository.addHighlights(any(), any()) }
    }

    // ----- Signed-out continuation

    @Test
    fun `a signed-out tap starts sign-in and shows no confirmation yet`() {
        isUserSignedIn = false
        selectVerses(verseOne)

        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        assertTrue(viewModel.state.value.shouldStartSignIn)
        assertFalse(viewModel.state.value.showDataExchangeConfirmation)
        verify(exactly = 0) { bibleHighlightsRepository.addHighlights(any(), any()) }
    }

    @Test
    fun `signing in without permission continues into the confirmation`() {
        isUserSignedIn = false
        selectVerses(verseOne)
        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        isUserSignedIn = true
        isHighlightsPermissionGranted = false
        viewModel.onAction(BibleReaderViewModel.Action.SignInCompleted)

        assertFalse(viewModel.state.value.shouldStartSignIn)
        assertTrue(viewModel.state.value.showDataExchangeConfirmation)
        verify(exactly = 0) { bibleHighlightsRepository.addHighlights(any(), any()) }
    }

    @Test
    fun `signing in with permission applies the highlight immediately`() {
        isUserSignedIn = false
        selectVerses(verseOne, verseTwo)
        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        isUserSignedIn = true
        isHighlightsPermissionGranted = true
        viewModel.onAction(BibleReaderViewModel.Action.SignInCompleted)

        verify { bibleHighlightsRepository.addHighlights(match { it.toSet() == setOf(verseOne, verseTwo) }, yellow) }
        assertFalse(viewModel.state.value.showDataExchangeConfirmation)
        assertTrue(
            viewModel.state.value.selectedVerses
                .isEmpty(),
        )
    }

    @Test
    fun `the highlight requested before sign-in carries its original verses and color through the grant`() {
        isUserSignedIn = false
        selectVerses(verseOne, verseTwo)
        viewModel.onAction(BibleReaderViewModel.Action.RemoveHighlight(yellow))

        isUserSignedIn = true
        isHighlightsPermissionGranted = false
        viewModel.onAction(BibleReaderViewModel.Action.SignInCompleted)
        viewModel.onAction(BibleReaderViewModel.Action.ConfirmDataExchange)
        viewModel.onAction(BibleReaderViewModel.Action.DataExchangeCompleted(isHighlightsGranted = true))

        verify {
            bibleHighlightsRepository.removeHighlights(
                match { it.toSet() == setOf(verseOne, verseTwo) },
                matchingColor = yellow,
            )
        }
    }

    @Test
    fun `abandoning sign-in applies nothing and keeps the verse selection`() {
        isUserSignedIn = false
        selectVerses(verseOne, verseTwo)
        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        viewModel.onAction(BibleReaderViewModel.Action.CancelSignIn)

        assertFalse(viewModel.state.value.shouldStartSignIn)
        assertEquals(setOf(verseOne, verseTwo), viewModel.state.value.selectedVerses)
        verify(exactly = 0) { bibleHighlightsRepository.addHighlights(any(), any()) }
    }

    @Test
    fun `sign-in completing before it settles keeps the held highlight for a later grant`() {
        isUserSignedIn = false
        selectVerses(verseOne)
        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        // Sign-in settles asynchronously, so the reader can still read as signed out when the prompt closes. The
        // held highlight must not be dropped here — the grant may still be in flight.
        viewModel.onAction(BibleReaderViewModel.Action.SignInCompleted)

        assertFalse(viewModel.state.value.shouldStartSignIn)
        assertFalse(viewModel.state.value.showDataExchangeConfirmation)
        assertTrue(viewModel.state.value.hasPendingHighlight)
        verify(exactly = 0) { bibleHighlightsRepository.addHighlights(any(), any()) }

        // Once the grant lands the held highlight applies, as the reader originally asked.
        isUserSignedIn = true
        isHighlightsPermissionGranted = true
        viewModel.applyPendingHighlightIfPermitted()

        verify { bibleHighlightsRepository.addHighlights(match { it.toSet() == setOf(verseOne) }, yellow) }
    }

    @Test
    fun `clearing the verse selection keeps a held highlight so the grant still applies it`() {
        isHighlightsPermissionGranted = false
        selectVerses(verseOne)
        viewModel.onAction(BibleReaderViewModel.Action.AddHighlight(yellow))

        // The selection is cleared for reasons other than the reader changing their mind — a reader recreated during
        // the browser grant restores its version and clears the selection. That must not discard the held highlight.
        viewModel.onAction(BibleReaderViewModel.Action.ClearVerseSelection)

        assertTrue(viewModel.state.value.hasPendingHighlight)

        isHighlightsPermissionGranted = true
        viewModel.applyPendingHighlightIfPermitted()

        verify { bibleHighlightsRepository.addHighlights(match { it.toSet() == setOf(verseOne) }, yellow) }
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

    @Test
    fun `different colors across selected verses are each removable and all remain recolorable`() {
        highlight(verseOne, HighlightColor.Yellow.hexColor)
        highlight(verseTwo, HighlightColor.Cyan.hexColor)
        selectVerses(verseOne, verseTwo)

        assertTrue(viewModel.isColorPresentOnAnySelectedVerses(HighlightColor.Yellow.hexColor))
        assertTrue(viewModel.isColorPresentOnAnySelectedVerses(HighlightColor.Cyan.hexColor))

        assertFalse(viewModel.isColorPresentOnAllSelectedVerses(HighlightColor.Yellow.hexColor))
        assertFalse(viewModel.isColorPresentOnAllSelectedVerses(HighlightColor.Cyan.hexColor))

        assertFalse(viewModel.isColorPresentOnAnySelectedVerses(HighlightColor.Pink.hexColor))
    }
}
