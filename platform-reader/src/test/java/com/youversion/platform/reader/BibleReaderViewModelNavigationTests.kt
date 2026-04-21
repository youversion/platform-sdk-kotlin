package com.youversion.platform.reader

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleBookIntro
import com.youversion.platform.core.bibles.models.BibleChapter
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.theme.ui.BibleReaderTheme
import io.mockk.every
import io.mockk.mockk
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BibleReaderViewModelNavigationTests {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var bibleReaderRepository: BibleReaderRepository
    private lateinit var viewModel: BibleReaderViewModel

    private val defaultReference =
        BibleReference(
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
        )

    private fun createChapters(count: Int): List<BibleChapter> =
        (1..count).map { BibleChapter(id = "ch$it", passageId = "p$it", title = "Chapter $it", verses = null) }

    private val multiBookVersion =
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
                        canon = "old_testament",
                        chapters = createChapters(50),
                    ),
                    BibleBook(
                        id = "EXO",
                        title = "Exodus",
                        fullTitle = null,
                        abbreviation = null,
                        canon = "old_testament",
                        chapters = createChapters(40),
                    ),
                    BibleBook(
                        id = "LEV",
                        title = "Leviticus",
                        fullTitle = null,
                        abbreviation = null,
                        canon = "old_testament",
                        chapters = createChapters(27),
                        intro =
                            BibleBookIntro(
                                id = "LEV_INTRO",
                                passageId = "LEV.INTRO1",
                                title = "Introduction to Leviticus",
                            ),
                    ),
                ),
        )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        bibleReaderRepository = mockk(relaxed = true)

        every { bibleReaderRepository.produceBibleReference(any()) } returns defaultReference

        viewModel =
            BibleReaderViewModel(
                bibleReference = null,
                fontDefinitionProvider = null,
                bibleVersionRepository = mockk(relaxed = true),
                bibleReaderRepository = bibleReaderRepository,
                userSettingsRepository = mockk(relaxed = true),
                bibleChapterRepository = mockk(relaxed = true),
                languageRepository = mockk(relaxed = true),
                copyManager = mockk(relaxed = true),
                shareManager = mockk(relaxed = true),
            )
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
        BibleReaderTheme.selectedColorScheme.value = null
    }

    // ----- GoToNextChapter

    @Test
    fun `GoToNextChapter when viewing intro navigates to chapter 1 of intro book`() {
        viewModel.bibleVersion = multiBookVersion
        viewModel.onIntroSelected("LEV", "LEV.INTRO1")

        assertTrue(viewModel.state.value.isViewingIntro)

        viewModel.onAction(BibleReaderViewModel.Action.GoToNextChapter)

        assertFalse(viewModel.state.value.isViewingIntro)
        assertEquals("LEV", viewModel.state.value.bibleReference.bookUSFM)
        assertEquals(1, viewModel.state.value.bibleReference.chapter)
    }

    @Test
    fun `GoToNextChapter increments chapter when next chapter exists in same book`() {
        viewModel.bibleVersion = multiBookVersion
        viewModel.bibleReference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 5)

        val nextRef = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 6)
        every { bibleReaderRepository.nextChapter(multiBookVersion, any()) } returns nextRef

        viewModel.onAction(BibleReaderViewModel.Action.GoToNextChapter)

        assertEquals("GEN", viewModel.state.value.bibleReference.bookUSFM)
        assertEquals(6, viewModel.state.value.bibleReference.chapter)
    }

    @Test
    fun `GoToNextChapter when next book has intro sets intro state instead of navigating`() {
        viewModel.bibleVersion = multiBookVersion
        viewModel.bibleReference = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 40)

        val nextRef = BibleReference(versionId = 1, bookUSFM = "LEV", chapter = 1)
        every { bibleReaderRepository.nextChapter(multiBookVersion, any()) } returns nextRef

        viewModel.onAction(BibleReaderViewModel.Action.GoToNextChapter)

        assertTrue(viewModel.state.value.isViewingIntro)
        assertEquals("LEV", viewModel.state.value.introBookUSFM)
        assertEquals("LEV.INTRO1", viewModel.state.value.introPassageId)
        assertEquals("EXO", viewModel.state.value.bibleReference.bookUSFM)
        assertEquals(40, viewModel.state.value.bibleReference.chapter)
    }

    @Test
    fun `GoToNextChapter when next book has no intro navigates to next book chapter 1`() {
        viewModel.bibleVersion = multiBookVersion
        viewModel.bibleReference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 50)

        val nextRef = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 1)
        every { bibleReaderRepository.nextChapter(multiBookVersion, any()) } returns nextRef

        viewModel.onAction(BibleReaderViewModel.Action.GoToNextChapter)

        assertFalse(viewModel.state.value.isViewingIntro)
        assertEquals("EXO", viewModel.state.value.bibleReference.bookUSFM)
        assertEquals(1, viewModel.state.value.bibleReference.chapter)
    }

    @Test
    fun `GoToNextChapter at end of last book does not navigate`() {
        viewModel.bibleVersion = multiBookVersion
        viewModel.bibleReference = BibleReference(versionId = 1, bookUSFM = "LEV", chapter = 27)

        every { bibleReaderRepository.nextChapter(multiBookVersion, any()) } returns null

        viewModel.onAction(BibleReaderViewModel.Action.GoToNextChapter)

        assertEquals("LEV", viewModel.state.value.bibleReference.bookUSFM)
        assertEquals(27, viewModel.state.value.bibleReference.chapter)
    }

    @Test
    fun `GoToNextChapter clears verse selection when navigation succeeds`() {
        viewModel.bibleVersion = multiBookVersion
        viewModel.bibleReference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 5)

        val verseRef = defaultReference.copy(verseStart = 1, verseEnd = 1)
        viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseRef))
        assertEquals(1, viewModel.state.value.selectedVerses.size)

        val nextRef = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 6)
        every { bibleReaderRepository.nextChapter(multiBookVersion, any()) } returns nextRef

        viewModel.onAction(BibleReaderViewModel.Action.GoToNextChapter)

        assertEquals(0, viewModel.state.value.selectedVerses.size)
        assertEquals(6, viewModel.state.value.bibleReference.chapter)
    }

    // ----- GoToPreviousChapter

    @Test
    fun `GoToPreviousChapter when viewing intro navigates to last chapter of previous book`() {
        viewModel.bibleVersion = multiBookVersion
        viewModel.bibleReference = BibleReference(versionId = 1, bookUSFM = "LEV", chapter = 1)
        viewModel.onIntroSelected("LEV", "LEV.INTRO1")

        assertTrue(viewModel.state.value.isViewingIntro)

        viewModel.onAction(BibleReaderViewModel.Action.GoToPreviousChapter)

        assertFalse(viewModel.state.value.isViewingIntro)
        assertEquals("EXO", viewModel.state.value.bibleReference.bookUSFM)
        assertEquals(40, viewModel.state.value.bibleReference.chapter)
    }

    @Test
    fun `GoToPreviousChapter when viewing intro of first book does nothing`() {
        val versionWithIntroOnFirst =
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
                            canon = "old_testament",
                            chapters = createChapters(50),
                            intro =
                                BibleBookIntro(
                                    id = "GEN_INTRO",
                                    passageId = "GEN.INTRO1",
                                    title = "Introduction to Genesis",
                                ),
                        ),
                    ),
            )
        viewModel.bibleVersion = versionWithIntroOnFirst
        viewModel.onIntroSelected("GEN", "GEN.INTRO1")
        assertTrue(viewModel.state.value.isViewingIntro)

        viewModel.onAction(BibleReaderViewModel.Action.GoToPreviousChapter)

        assertTrue(viewModel.state.value.isViewingIntro)
        assertEquals("GEN", viewModel.state.value.introBookUSFM)
        assertEquals("GEN.INTRO1", viewModel.state.value.introPassageId)
    }

    @Test
    fun `GoToPreviousChapter at chapter 1 when current book has intro sets intro state`() {
        viewModel.bibleVersion = multiBookVersion
        viewModel.bibleReference = BibleReference(versionId = 1, bookUSFM = "LEV", chapter = 1)

        viewModel.onAction(BibleReaderViewModel.Action.GoToPreviousChapter)

        assertTrue(viewModel.state.value.isViewingIntro)
        assertEquals("LEV", viewModel.state.value.introBookUSFM)
        assertEquals("LEV.INTRO1", viewModel.state.value.introPassageId)
    }

    @Test
    fun `GoToPreviousChapter at chapter 1 without intro delegates to previousChapter`() {
        viewModel.bibleVersion = multiBookVersion
        viewModel.bibleReference = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 1)

        val prevRef = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 50)
        every { bibleReaderRepository.previousChapter(multiBookVersion, any()) } returns prevRef

        viewModel.onAction(BibleReaderViewModel.Action.GoToPreviousChapter)

        assertEquals("GEN", viewModel.state.value.bibleReference.bookUSFM)
        assertEquals(50, viewModel.state.value.bibleReference.chapter)
    }

    @Test
    fun `GoToPreviousChapter at chapter 1 of first book without intro does not navigate`() {
        viewModel.bibleVersion = multiBookVersion
        viewModel.bibleReference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)

        every { bibleReaderRepository.previousChapter(multiBookVersion, any()) } returns null

        viewModel.onAction(BibleReaderViewModel.Action.GoToPreviousChapter)

        assertEquals("GEN", viewModel.state.value.bibleReference.bookUSFM)
        assertEquals(1, viewModel.state.value.bibleReference.chapter)
    }

    @Test
    fun `GoToPreviousChapter clears verse selection when navigation succeeds`() {
        viewModel.bibleVersion = multiBookVersion
        viewModel.bibleReference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 10)

        val verseRef = defaultReference.copy(verseStart = 1, verseEnd = 1)
        viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(verseRef))
        assertEquals(1, viewModel.state.value.selectedVerses.size)

        val prevRef = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 9)
        every { bibleReaderRepository.previousChapter(multiBookVersion, any()) } returns prevRef

        viewModel.onAction(BibleReaderViewModel.Action.GoToPreviousChapter)

        assertEquals(0, viewModel.state.value.selectedVerses.size)
        assertEquals(9, viewModel.state.value.bibleReference.chapter)
    }
}
