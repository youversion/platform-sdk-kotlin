package com.youversion.platform.ui.views.card

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleChapter
import com.youversion.platform.core.bibles.models.BibleVerse
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.core.languages.domain.LanguageRepository
import com.youversion.platform.ui.views.BibleTextFonts
import com.youversion.platform.ui.views.BibleTextOptions
import com.youversion.platform.ui.views.rendering.BibleVersionRendering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class BibleCardTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockVersionRepository = mockk<BibleVersionRepository>(relaxed = true)
    private val mockChapterRepository = mockk<BibleChapterRepository>()
    private val mockLanguageRepository = mockk<LanguageRepository>(relaxed = true)

    private val testReference =
        BibleReference(
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
            verse = 1,
        )

    private val genesisChapter1 =
        BibleChapter(
            id = "1",
            passageId = null,
            title = null,
            verses = listOf(BibleVerse(id = "1", passageId = null, title = null)),
        )

    private val genesisChapter2 =
        BibleChapter(
            id = "2",
            passageId = null,
            title = null,
            verses = listOf(BibleVerse(id = "1", passageId = null, title = null)),
        )

    private val genesisBook =
        BibleBook(
            id = "GEN",
            title = "Genesis",
            fullTitle = null,
            abbreviation = "Gen",
            canon = "old_testament",
            chapters = listOf(genesisChapter1, genesisChapter2),
        )

    private val testBibleVersion =
        BibleVersion(
            id = 1,
            abbreviation = "KJV",
            localizedAbbreviation = "KJV",
            localizedTitle = "KJV Copyright",
            copyright = "© Test",
            promotionalContent = "Promo content for testing",
            books = listOf(genesisBook),
        )

    @Before
    fun setUp() {
        mockkObject(BibleVersionRendering)
        coEvery { mockVersionRepository.version(any()) } returns testBibleVersion
        coEvery { mockChapterRepository.chapter(any()) } returns "<div></div>"
        coEvery { mockChapterRepository.removeVersionChapters(any()) } returns Unit

        PlatformKoinGraph.start(
            listOf(
                module {
                    single { mockVersionRepository }
                    single { mockChapterRepository }
                    single { mockLanguageRepository }
                },
            ),
        )
    }

    @After
    fun tearDown() {
        PlatformKoinGraph.stop()
        unmockkObject(BibleVersionRendering)
    }

    /**
     * [BibleCardViewModel] skips loading when a version is provided; that contract is covered by
     * [BibleCardViewModelTests.`does not load version from repository when initialized with version`].
     * This UI test does not assert [BibleVersionRepository.version] is never called: [BibleText]
     * still invokes it for [com.youversion.platform.core.bibles.models.BibleVersion.isRightToLeft].
     */
    @Test
    fun `simple overload displays header and copyright`() {
        coEvery {
            BibleVersionRendering.textBlocks(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns emptyList()

        composeTestRule.setContent {
            MaterialTheme {
                BibleCard(
                    reference = testReference,
                    version = testBibleVersion,
                    fontSize = 20.sp,
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Genesis 1:1 KJV").assertIsDisplayed()
        composeTestRule.onNodeWithText("© Test").assertIsDisplayed()
    }

    @Test
    fun `displays header and copyright after async version load when version is null`() {
        coEvery {
            BibleVersionRendering.textBlocks(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns emptyList()

        composeTestRule.setContent {
            MaterialTheme {
                BibleCard(
                    reference = testReference,
                    version = null,
                    fontSize = 20.sp,
                )
            }
        }

        composeTestRule.waitUntil(
            conditionDescription = "Header and copyright appear after async version load",
            timeoutMillis = 5_000,
        ) {
            composeTestRule.onAllNodesWithText("Genesis 1:1 KJV").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText("© Test").fetchSemanticsNodes().isNotEmpty()
        }

        coVerify(atLeast = 1) { mockVersionRepository.version(any()) }
        composeTestRule.onNodeWithText("Genesis 1:1 KJV").assertIsDisplayed()
        composeTestRule.onNodeWithText("© Test").assertIsDisplayed()
    }

    /**
     * [BibleCardViewModel] emits [BibleCardViewModel.Event.OnErrorLoadingBibleVersion] when async
     * version loading fails; that contract is covered by
     * [BibleCardViewModelTests.`emits error event when version loading fails`].
     * This UI test asserts the toast shown when that event is handled in [BibleCard].
     */
    @Test
    fun `shows error toast when async version load fails`() {
        coEvery { mockVersionRepository.version(any()) } throws RuntimeException("Network error")
        coEvery {
            BibleVersionRendering.textBlocks(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns emptyList()

        composeTestRule.setContent {
            MaterialTheme {
                BibleCard(
                    reference = testReference,
                    version = null,
                    fontSize = 20.sp,
                )
            }
        }

        composeTestRule.waitUntil(
            conditionDescription = "Error toast is shown after version load fails",
            timeoutMillis = 5_000,
        ) {
            ShadowToast.getLatestToast() != null
        }

        assertNotNull(ShadowToast.getLatestToast())
        assertEquals("Error loading Bible version", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `simple overload passes fontSize to BibleText via fonts baseSize`() {
        val fontsSlot = slot<BibleTextFonts>()
        coEvery {
            BibleVersionRendering.textBlocks(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                capture(fontsSlot),
            )
        } returns emptyList()

        val testFontSize = 20.sp
        composeTestRule.setContent {
            MaterialTheme {
                BibleCard(
                    reference = testReference,
                    version = testBibleVersion,
                    fontSize = testFontSize,
                )
            }
        }
        composeTestRule.waitUntil(
            conditionDescription = "BibleText LaunchedEffect invokes textBlocks and captures fonts",
            timeoutMillis = 5_000,
        ) {
            fontsSlot.isCaptured
        }

        assertEquals(testFontSize, fontsSlot.captured.baseSize)
    }

    @Test
    fun `full overload forwards renderVerseNumbers and renderHeadlines to textBlocks`() {
        val renderVerseNumbersSlot = slot<Boolean>()
        val renderHeadlinesSlot = slot<Boolean>()
        coEvery {
            BibleVersionRendering.textBlocks(
                any(),
                any(),
                capture(renderVerseNumbersSlot),
                capture(renderHeadlinesSlot),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns emptyList()

        composeTestRule.setContent {
            MaterialTheme {
                BibleCard(
                    reference = testReference,
                    textOptions =
                        BibleTextOptions(
                            renderVerseNumbers = false,
                            renderHeadlines = false,
                        ),
                    version = testBibleVersion,
                )
            }
        }
        composeTestRule.waitUntil(
            conditionDescription =
                "BibleText LaunchedEffect invokes textBlocks and captures renderVerseNumbers and renderHeadlines",
            timeoutMillis = 5_000,
        ) {
            renderVerseNumbersSlot.isCaptured && renderHeadlinesSlot.isCaptured
        }

        assertEquals(false, renderVerseNumbersSlot.captured)
        assertEquals(false, renderHeadlinesSlot.captured)
    }

    @Test
    fun `full overload copyright sheet displays localizedTitle and promotionalContent when copyright clicked`() {
        coEvery {
            BibleVersionRendering.textBlocks(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns emptyList()

        composeTestRule.setContent {
            MaterialTheme {
                BibleCard(
                    reference = testReference,
                    textOptions = BibleTextOptions(),
                    version = testBibleVersion,
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("© Test").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("KJV Copyright").assertIsDisplayed()
        composeTestRule.onNodeWithText("Promo content for testing").assertIsDisplayed()
    }

    @Test
    fun `simple overload displays version picker button when showVersionPicker is true`() {
        coEvery {
            BibleVersionRendering.textBlocks(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns emptyList()

        composeTestRule.setContent {
            MaterialTheme {
                BibleCard(
                    reference = testReference,
                    version = testBibleVersion,
                    fontSize = 20.sp,
                    showVersionPicker = true,
                )
            }
        }

        composeTestRule.onNodeWithTag("bible_card_version_picker").assertIsDisplayed()
    }

    @Test
    fun `displays unavailable message when reference does not exist in version`() {
        val versionWithoutGenesis =
            BibleVersion(
                id = 2,
                abbreviation = "NIV",
                localizedAbbreviation = "NIV",
                books = emptyList(),
            )

        composeTestRule.setContent {
            MaterialTheme {
                BibleCard(
                    reference = testReference,
                    version = versionWithoutGenesis,
                    fontSize = 20.sp,
                )
            }
        }

        composeTestRule
            .onNodeWithText("This passage is unavailable in the selected Bible version.")
            .assertIsDisplayed()
    }
}
