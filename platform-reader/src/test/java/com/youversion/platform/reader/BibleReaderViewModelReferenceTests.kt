package com.youversion.platform.reader

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.reader.domain.BibleReaderRepository
import com.youversion.platform.reader.theme.ui.BibleReaderTheme
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
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
class BibleReaderViewModelReferenceTests {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var bibleVersionRepository: BibleVersionRepository
    private lateinit var bibleReaderRepository: BibleReaderRepository
    private lateinit var viewModel: BibleReaderViewModel

    private val defaultReference =
        BibleReference(
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
        )

    private val testBibleVersion =
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
                    ),
                ),
        )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        bibleVersionRepository = mockk(relaxed = true)
        bibleReaderRepository = mockk(relaxed = true)

        every { bibleReaderRepository.produceBibleReference(any()) } returns defaultReference

        viewModel =
            BibleReaderViewModel(
                bibleReference = null,
                fontDefinitionProvider = null,
                bibleVersionRepository = bibleVersionRepository,
                bibleReaderRepository = bibleReaderRepository,
                userSettingsRepository = mockk(relaxed = true),
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

    // ----- Property Setters

    @Test
    fun `bibleReference setter persists to repository and updates state`() {
        val newReference = BibleReference(versionId = 2, bookUSFM = "EXO", chapter = 3)

        viewModel.bibleReference = newReference

        verify { bibleReaderRepository.lastBibleReference = newReference }
        assertEquals(newReference, viewModel.state.value.bibleReference)
    }

    @Test
    fun `bibleVersion setter updates state`() {
        viewModel.bibleVersion = testBibleVersion

        assertEquals(testBibleVersion, viewModel.state.value.bibleVersion)
    }

    // ----- switchToVersion

    @Test
    fun `switchToVersion creates new reference and triggers header selection change`() {
        val newVersion = BibleVersion(id = 99, abbreviation = "NIV")
        coEvery { bibleVersionRepository.version(id = 99) } returns newVersion

        viewModel.switchToVersion(99)

        assertEquals(99, viewModel.state.value.bibleReference.versionId)
        assertEquals(
            99,
            viewModel.state.value.bibleVersion
                ?.id,
        )
        assertNull(viewModel.state.value.introBookUSFM)
        assertNull(viewModel.state.value.introPassageId)
        coVerify { bibleVersionRepository.version(id = 99) }
    }

    // ----- onIntroSelected

    @Test
    fun `onIntroSelected updates intro state and bible reference`() {
        viewModel.onIntroSelected(bookUSFM = "LEV", passageId = "LEV.INTRO1")

        val state = viewModel.state.value
        assertEquals("LEV", state.introBookUSFM)
        assertEquals("LEV.INTRO1", state.introPassageId)
        assertEquals("LEV", state.bibleReference.bookUSFM)
        assertEquals(1, state.bibleReference.chapter)
    }

    // ----- onHeaderSelectionChange

    @Test
    fun `onHeaderSelectionChange loads new version when version id changed`() {
        viewModel.bibleVersion = testBibleVersion
        val newVersion = BibleVersion(id = 42, abbreviation = "NIV")
        coEvery { bibleVersionRepository.version(id = 42) } returns newVersion
        val newRef = BibleReference(versionId = 42, bookUSFM = "GEN", chapter = 1)

        viewModel.onHeaderSelectionChange(newRef)

        assertEquals(newVersion, viewModel.state.value.bibleVersion)
        assertEquals(newRef, viewModel.state.value.bibleReference)
    }

    @Test
    fun `onHeaderSelectionChange does not reload version when version id unchanged`() {
        viewModel.bibleVersion = testBibleVersion
        clearMocks(bibleVersionRepository, answers = false)
        val newRef = BibleReference(versionId = 1, bookUSFM = "EXO", chapter = 5)

        viewModel.onHeaderSelectionChange(newRef)

        coVerify(exactly = 0) { bibleVersionRepository.version(id = any()) }
        assertEquals(testBibleVersion, viewModel.state.value.bibleVersion)
        assertEquals(newRef, viewModel.state.value.bibleReference)
    }

    @Test
    fun `onHeaderSelectionChange clears intro state`() {
        viewModel.bibleVersion = testBibleVersion
        viewModel.onIntroSelected("GEN", "GEN.INTRO1")
        assertTrue(viewModel.state.value.isViewingIntro)

        val newRef = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 5)
        viewModel.onHeaderSelectionChange(newRef)

        assertNull(viewModel.state.value.introBookUSFM)
        assertNull(viewModel.state.value.introPassageId)
        assertFalse(viewModel.state.value.isViewingIntro)
    }

    @Test
    fun `onHeaderSelectionChange clears verse selection`() {
        viewModel.bibleVersion = testBibleVersion
        viewModel.onAction(
            BibleReaderViewModel.Action.OnVerseTap(defaultReference.copy(verseStart = 1, verseEnd = 1)),
        )
        assertEquals(1, viewModel.state.value.selectedVerses.size)

        viewModel.onHeaderSelectionChange(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2))

        assertEquals(0, viewModel.state.value.selectedVerses.size)
    }
}
