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
    fun `selecting verse sets showVerseActionSheet to true`() =
        runTest {
            val verseRef = defaultReference.copy(verseStart = 1, verseEnd = 1)

            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseRef))

            assertTrue(viewModel.state.value.showVerseActionSheet)
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
    fun `deselecting last verse sets showVerseActionSheet to false`() =
        runTest {
            val verseRef = defaultReference.copy(verseStart = 1, verseEnd = 1)

            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseRef))
            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseRef))

            assertFalse(viewModel.state.value.showVerseActionSheet)
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
}
