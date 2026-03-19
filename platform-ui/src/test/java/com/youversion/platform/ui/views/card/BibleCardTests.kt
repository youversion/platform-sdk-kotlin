package com.youversion.platform.ui.views.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleChapter
import com.youversion.platform.core.bibles.models.BibleVerse
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformKoinGraph
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
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class BibleCardTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockVersionRepository = mockk<BibleVersionRepository>()
    private val mockChapterRepository = mockk<BibleChapterRepository>()

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
                },
            ),
        )
    }

    @After
    fun tearDown() {
        PlatformKoinGraph.stop()
        unmockkObject(BibleVersionRendering)
    }

    private fun setBibleCardContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    Box(Modifier.height(400.dp)) {
                        content()
                    }
                }
            }
        }
    }

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

        setBibleCardContent {
            BibleCard(
                reference = testReference,
                version = testBibleVersion,
                fontSize = 20.sp,
            )
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

        setBibleCardContent {
            BibleCard(
                reference = testReference,
                version = null,
                fontSize = 20.sp,
            )
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
        setBibleCardContent {
            BibleCard(
                reference = testReference,
                version = testBibleVersion,
                fontSize = testFontSize,
            )
        }
        composeTestRule.waitForIdle()

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

        setBibleCardContent {
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
        composeTestRule.waitForIdle()

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

        setBibleCardContent {
            BibleCard(
                reference = testReference,
                textOptions = BibleTextOptions(),
                version = testBibleVersion,
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("© Test").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("KJV Copyright").assertIsDisplayed()
        composeTestRule.onNodeWithText("Promo content for testing").assertIsDisplayed()
    }
}
