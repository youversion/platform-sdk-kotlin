package com.youversion.platform.reader

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.domain.UserSettingsRepository
import io.mockk.every
import io.mockk.mockk
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BibleReaderViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var bibleVersionRepository: BibleVersionRepository
    private lateinit var bibleReaderRepository: BibleReaderRepository
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

        bibleVersionRepository = mockk(relaxed = true)
        bibleReaderRepository = mockk(relaxed = true)
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
                bibleVersionRepository = bibleVersionRepository,
                bibleReaderRepository = bibleReaderRepository,
                userSettingsRepository = userSettingsRepository,
            )
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selecting verse adds it to selectedVerses`() =
        runTest {
            val verseRef = defaultReference.copy(verseStart = 1, verseEnd = 1)

            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseRef))

            assertTrue(
                viewModel.state.value.selectedVerses
                    .contains(verseRef),
            )
        }

    @Test
    fun `selecting verse sets isShowingVerseActionSheet to true`() =
        runTest {
            val verseRef = defaultReference.copy(verseStart = 1, verseEnd = 1)

            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseRef))

            assertTrue(viewModel.state.value.isShowingVerseActionSheet)
        }

    @Test
    fun `selecting already selected verse removes it`() =
        runTest {
            val verseRef = defaultReference.copy(verseStart = 1, verseEnd = 1)

            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseRef))
            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseRef))

            assertFalse(
                viewModel.state.value.selectedVerses
                    .contains(verseRef),
            )
        }

    @Test
    fun `deselecting last verse sets isShowingVerseActionSheet to false`() =
        runTest {
            val verseRef = defaultReference.copy(verseStart = 1, verseEnd = 1)

            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseRef))
            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseRef))

            assertFalse(viewModel.state.value.isShowingVerseActionSheet)
        }

    @Test
    fun `can select multiple verses`() =
        runTest {
            val verse1 = defaultReference.copy(verseStart = 1, verseEnd = 1)
            val verse2 = defaultReference.copy(verseStart = 2, verseEnd = 2)
            val verse3 = defaultReference.copy(verseStart = 3, verseEnd = 3)

            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verse1))
            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verse2))
            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verse3))

            assertEquals(3, viewModel.state.value.selectedVerses.size)
            assertTrue(
                viewModel.state.value.selectedVerses
                    .containsAll(setOf(verse1, verse2, verse3)),
            )
        }

    @Test
    fun `ClearVerseSelection clears all selected verses`() =
        runTest {
            val verse1 = defaultReference.copy(verseStart = 1, verseEnd = 1)
            val verse2 = defaultReference.copy(verseStart = 2, verseEnd = 2)
            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verse1))
            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verse2))

            viewModel.onAction(BibleReaderViewModel.Action.ClearVerseSelection)

            assertTrue(
                viewModel.state.value.selectedVerses
                    .isEmpty(),
            )
        }

    @Test
    fun `ClearVerseSelection sets isShowingVerseActionSheet to false`() =
        runTest {
            val verseRef = defaultReference.copy(verseStart = 1, verseEnd = 1)
            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseRef))

            viewModel.onAction(BibleReaderViewModel.Action.ClearVerseSelection)

            assertFalse(viewModel.state.value.isShowingVerseActionSheet)
        }

    @Test
    fun `GoToNextChapter clears verse selection`() =
        runTest {
            val verseRef = defaultReference.copy(verseStart = 1, verseEnd = 1)
            val nextChapterRef = defaultReference.copy(chapter = 2)
            every { bibleReaderRepository.nextChapter(any(), any()) } returns nextChapterRef
            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseRef))

            viewModel.onAction(BibleReaderViewModel.Action.GoToNextChapter)

            assertTrue(
                viewModel.state.value.selectedVerses
                    .isEmpty(),
            )
            assertFalse(viewModel.state.value.isShowingVerseActionSheet)
        }

    @Test
    fun `GoToPreviousChapter clears verse selection`() =
        runTest {
            val ref = defaultReference.copy(chapter = 2)
            every { bibleReaderRepository.produceBibleReference(any()) } returns ref
            val prevChapterRef = defaultReference.copy(chapter = 1)
            every { bibleReaderRepository.previousChapter(any(), any()) } returns prevChapterRef

            viewModel =
                BibleReaderViewModel(
                    bibleReference = null,
                    fontDefinitionProvider = null,
                    bibleVersionRepository = bibleVersionRepository,
                    bibleReaderRepository = bibleReaderRepository,
                    userSettingsRepository = userSettingsRepository,
                )

            val verseRef = ref.copy(verseStart = 1, verseEnd = 1)
            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseRef))

            viewModel.onAction(BibleReaderViewModel.Action.GoToPreviousChapter)

            assertTrue(
                viewModel.state.value.selectedVerses
                    .isEmpty(),
            )
            assertFalse(viewModel.state.value.isShowingVerseActionSheet)
        }

    @Test
    fun `onHeaderSelectionChange clears verse selection`() =
        runTest {
            val verseRef = defaultReference.copy(verseStart = 1, verseEnd = 1)
            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseRef))
            val newReference = BibleReference(versionId = 2, bookUSFM = "EXO", chapter = 1)

            viewModel.onHeaderSelectionChange(newReference)

            assertTrue(
                viewModel.state.value.selectedVerses
                    .isEmpty(),
            )
            assertFalse(viewModel.state.value.isShowingVerseActionSheet)
        }
}
