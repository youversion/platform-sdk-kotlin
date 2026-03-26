package com.youversion.platform.reader

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
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
class BibleReaderViewModelStateTests {
    private val testDispatcher = UnconfinedTestDispatcher()

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

        val bibleReaderRepository = mockk<BibleReaderRepository>(relaxed = true)
        every { bibleReaderRepository.produceBibleReference(any()) } returns defaultReference

        viewModel =
            BibleReaderViewModel(
                bibleReference = null,
                fontDefinitionProvider = null,
                bibleVersionRepository = mockk(relaxed = true),
                bibleReaderRepository = bibleReaderRepository,
                userSettingsRepository =
                    mockk(relaxed = true) {
                        every { readerLineSpacing } returns null
                        every { readerFontSize } returns null
                    },
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

    // ----- isViewingIntro

    @Test
    fun `isViewingIntro is true when both introBookUSFM and introPassageId are non-null`() {
        assertFalse(viewModel.state.value.isViewingIntro)

        viewModel.onIntroSelected("GEN", "GEN.INTRO1")

        assertTrue(viewModel.state.value.isViewingIntro)
    }

    // ----- bookName

    @Test
    fun `bookName returns book name from version when version exists`() {
        viewModel.bibleVersion = multiBookVersion

        assertEquals("Genesis", viewModel.state.value.bookName)
    }

    @Test
    fun `bookName uses introBookUSFM when viewing intro`() {
        viewModel.bibleVersion = multiBookVersion
        viewModel.onIntroSelected("LEV", "LEV.INTRO1")

        assertEquals("Leviticus", viewModel.state.value.bookName)
    }

    @Test
    fun `bookName returns empty string when version is null`() {
        assertEquals("", viewModel.state.value.bookName)
    }

    // ----- bookAndChapter

    @Test
    fun `bookAndChapter returns book name and chapter number`() {
        viewModel.bibleVersion = multiBookVersion

        assertEquals("Genesis 1", viewModel.state.value.bookAndChapter)
    }

    @Test
    fun `bookAndChapter returns book name with Intro when viewing intro`() {
        viewModel.bibleVersion = multiBookVersion
        viewModel.onIntroSelected("LEV", "LEV.INTRO1")

        assertEquals("Leviticus Intro", viewModel.state.value.bookAndChapter)
    }

    @Test
    fun `bookAndChapter returns empty string when bookName is empty`() {
        assertEquals("", viewModel.state.value.bookAndChapter)
    }

    // ----- versionAbbreviation

    @Test
    fun `versionAbbreviation returns localizedAbbreviation when available`() {
        viewModel.bibleVersion = BibleVersion(id = 1, abbreviation = "KJV", localizedAbbreviation = "KJV-Local")

        assertEquals("KJV-Local", viewModel.state.value.versionAbbreviation)
    }

    @Test
    fun `versionAbbreviation falls back to abbreviation`() {
        viewModel.bibleVersion = BibleVersion(id = 1, abbreviation = "KJV")

        assertEquals("KJV", viewModel.state.value.versionAbbreviation)
    }

    @Test
    fun `versionAbbreviation falls back to id when no abbreviation`() {
        viewModel.bibleVersion = BibleVersion(id = 42)

        assertEquals("42", viewModel.state.value.versionAbbreviation)
    }

    @Test
    fun `versionAbbreviation returns empty string when version is null`() {
        assertEquals("", viewModel.state.value.versionAbbreviation)
    }

    // ----- lineSpacingSettingsIndex

    @Test
    fun `lineSpacingSettingsIndex returns correct index for default spacing`() {
        assertEquals(0, viewModel.state.value.lineSpacingSettingsIndex)
    }

    // ----- lineSpacing

    @Test
    fun `lineSpacing returns fontSize multiplied by lineSpacingMultiplier`() {
        assertEquals(27.sp, viewModel.state.value.lineSpacing)
    }

    // ----- fontFamily

    @Test
    fun `fontFamily returns selectedFontDefinition fontFamily`() {
        assertEquals(
            ReaderFontSettings.DEFAULT_FONT_DEFINITION.fontFamily,
            viewModel.state.value.fontFamily,
        )
    }

    @Test
    fun `fontFamily reflects updated selectedFontDefinition`() {
        val monoFont = FontDefinition("Monospace", FontFamily.Monospace)
        viewModel.onAction(BibleReaderViewModel.Action.SetFontDefinition(monoFont))

        assertEquals(FontFamily.Monospace, viewModel.state.value.fontFamily)
    }
}
