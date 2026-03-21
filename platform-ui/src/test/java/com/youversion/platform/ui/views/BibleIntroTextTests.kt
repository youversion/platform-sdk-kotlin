package com.youversion.platform.ui.views

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.domain.BibleIntroRepository
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.core.utilities.exceptions.BibleVersionApiException
import com.youversion.platform.ui.views.rendering.BibleTextBlock
import com.youversion.platform.ui.views.rendering.BibleTextCategory
import com.youversion.platform.ui.views.rendering.BibleTextCategoryAttribute
import com.youversion.platform.ui.views.rendering.BibleVersionRendering
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BibleIntroTextTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockVersionRepository = mockk<BibleVersionRepository>()
    private val mockIntroRepository = mockk<BibleIntroRepository>()

    private val ltrVersion = BibleVersion(id = 1, abbreviation = "KJV", textDirection = "ltr")
    private val rtlVersion = BibleVersion(id = 1, abbreviation = "KJV", textDirection = "rtl")

    private fun annotatedBlock(
        text: String,
        marginTop: Dp = 8.dp,
        footnoteMarkerRange: IntRange? = null,
        footnoteImageRange: IntRange? = null,
        footnotes: List<AnnotatedString> = emptyList(),
    ): BibleTextBlock {
        val annotatedText =
            buildAnnotatedString {
                append(text)
                if (footnoteMarkerRange != null) {
                    addStringAnnotation(
                        tag = BibleTextCategoryAttribute.NAME,
                        annotation = BibleTextCategory.FOOTNOTE_MARKER.name,
                        start = footnoteMarkerRange.first,
                        end = footnoteMarkerRange.last + 1,
                    )
                }
                if (footnoteImageRange != null) {
                    addStringAnnotation(
                        tag = BibleTextCategoryAttribute.NAME,
                        annotation = BibleTextCategory.FOOTNOTE_IMAGE.name,
                        start = footnoteImageRange.first,
                        end = footnoteImageRange.last + 1,
                    )
                }
            }
        return BibleTextBlock(
            text = annotatedText,
            chapter = 1,
            headIndent = 0.sp,
            marginTop = marginTop,
            alignment = TextAlign.Start,
            footnotes = footnotes,
        )
    }

    @Before
    fun setup() {
        mockkObject(BibleVersionRendering)
        PlatformKoinGraph.start(
            listOf(
                module {
                    single { mockVersionRepository }
                    single { mockIntroRepository }
                },
            ),
        )
    }

    @After
    fun tearDown() {
        PlatformKoinGraph.stop()
        unmockkObject(BibleVersionRendering)
    }

    // region Loading Phase Transitions

    @Test
    fun `transitions to SUCCESS when introTextBlocks returns non-null`() {
        val phases = mutableListOf<BibleTextLoadingPhase>()
        val introTextBlocksDeferred = CompletableDeferred<List<BibleTextBlock>?>()

        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns "<p>Intro for Genesis</p>"
        coEvery {
            BibleVersionRendering.introTextBlocks(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers { introTextBlocksDeferred.await() }

        composeTestRule.setContent {
            BibleIntroText(1, "GEN", "INTRO", onStateChange = { phases.add(it) })
        }
        introTextBlocksDeferred.complete(listOf(annotatedBlock("This is an intro for Genesis.")))
        composeTestRule.waitForIdle()

        runTest {
            assertEquals(BibleTextLoadingPhase.INACTIVE, phases[0])
            assertEquals(BibleTextLoadingPhase.LOADING, phases[1])
            assertEquals(BibleTextLoadingPhase.SUCCESS, phases[2])
        }
    }

    @Test
    fun `transitions to FAILED when introTextBlocks returns null`() {
        val phases = mutableListOf<BibleTextLoadingPhase>()
        val introTextBlocksDeferred = CompletableDeferred<List<BibleTextBlock>?>()

        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns "<p> Intro Chapter </p>"
        coEvery {
            BibleVersionRendering.introTextBlocks(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers { introTextBlocksDeferred.await() }

        composeTestRule.setContent { BibleIntroText(1, "GEN", "INTRO", onStateChange = { phases.add(it) }) }
        introTextBlocksDeferred.complete(null)
        composeTestRule.waitForIdle()

        runTest {
            assertEquals(BibleTextLoadingPhase.INACTIVE, phases[0])
            assertEquals(BibleTextLoadingPhase.LOADING, phases[1])
            assertEquals(BibleTextLoadingPhase.FAILED, phases[2])
        }
    }

    @Test
    fun `transitions to NOT_PERMITTED when BibleVersionApiException is thrown`() {
        val phases = mutableListOf<BibleTextLoadingPhase>()
        val versionDeferred = CompletableDeferred<BibleVersion>()

        coEvery { mockVersionRepository.version(any()) } coAnswers { versionDeferred.await() }

        composeTestRule.setContent { BibleIntroText(1, "GEN", "INTRO", onStateChange = { phases.add(it) }) }
        versionDeferred.completeExceptionally(
            BibleVersionApiException(BibleVersionApiException.Reason.NOT_PERMITTED),
        )
        composeTestRule.waitForIdle()

        runTest {
            assertEquals(BibleTextLoadingPhase.INACTIVE, phases[0])
            assertEquals(BibleTextLoadingPhase.LOADING, phases[1])
            assertEquals(BibleTextLoadingPhase.NOT_PERMITTED, phases[2])
        }
    }

    @Test
    fun `transitions to FAILED when generic exception is thrown`() {
        val phases = mutableListOf<BibleTextLoadingPhase>()
        val versionDeferred = CompletableDeferred<BibleVersion>()

        coEvery { mockVersionRepository.version(any()) } coAnswers { versionDeferred.await() }

        composeTestRule.setContent { BibleIntroText(1, "GEN", "INTRO", onStateChange = { phases.add(it) }) }
        versionDeferred.completeExceptionally(RuntimeException("Network error"))
        composeTestRule.waitForIdle()

        runTest {
            assertEquals(BibleTextLoadingPhase.INACTIVE, phases[0])
            assertEquals(BibleTextLoadingPhase.LOADING, phases[1])
            assertEquals(BibleTextLoadingPhase.FAILED, phases[2])
        }
    }

    @Test
    fun `transitions to INACTIVE when CancellationException is thrown`() {
        val phases = mutableListOf<BibleTextLoadingPhase>()
        val versionDeferred = CompletableDeferred<BibleVersion>()

        coEvery { mockVersionRepository.version(any()) } coAnswers { versionDeferred.await() }

        composeTestRule.setContent { BibleIntroText(1, "GEN", "INTRO", onStateChange = { phases.add(it) }) }
        versionDeferred.completeExceptionally(CancellationException("cancelled"))
        composeTestRule.waitForIdle()

        runTest {
            assertEquals(BibleTextLoadingPhase.INACTIVE, phases[0])
            assertEquals(BibleTextLoadingPhase.LOADING, phases[1])
            assertEquals(BibleTextLoadingPhase.INACTIVE, phases[2])
        }
    }

    // endregion

    // region Placeholder and Content Visibility

    @Test
    fun `placeholder is shown when loading phase is not SUCCESS`() {
        var lastPhase: BibleTextLoadingPhase? = null

        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns "<p>Intro for Genesis</p>"
        coEvery {
            BibleVersionRendering.introTextBlocks(
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
        } returns
            null

        composeTestRule.setContent { BibleIntroText(1, "GEN", "INTRO", onStateChange = { lastPhase = it }) }
        composeTestRule.waitForIdle()

        assertEquals(BibleTextLoadingPhase.FAILED, lastPhase)
        composeTestRule
            .onNodeWithText(
                "You've lost your internet connection. Re-connect and download a Bible version to proceed offline.",
            ).assertIsDisplayed()
    }

    @Test
    fun `content is shown when loading phase is SUCCESS`() {
        var lastPhase: BibleTextLoadingPhase? = null

        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns "<p> Intro chapters </p>"

        coEvery {
            BibleVersionRendering.introTextBlocks(
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
        } returns
            listOf(annotatedBlock("This is an intro chapter for Genesis."))

        composeTestRule.setContent { BibleIntroText(1, "GEN", "INTRO", onStateChange = { lastPhase = it }) }
        composeTestRule.waitForIdle()

        assertEquals(BibleTextLoadingPhase.SUCCESS, lastPhase)
        composeTestRule.onNodeWithText("This is an intro chapter for Genesis.").assertIsDisplayed()
    }

    // endregion

    // region RTL/LTR Alignment

    @Test
    fun `LTR system with RTL version aligns content to end`() {
        coEvery { mockVersionRepository.version(any()) } returns rtlVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns "<p>Intro</p>"
        coEvery {
            BibleVersionRendering.introTextBlocks(
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
        } returns
            listOf(
                annotatedBlock("Filler text"),
                BibleTextBlock(
                    text = AnnotatedString(""),
                    chapter = 1,
                    rows = listOf(listOf(AnnotatedString("A"))),
                    headIndent = 0.sp,
                    marginTop = 8.dp,
                    alignment = TextAlign.Start,
                    footnotes = emptyList(),
                ),
            )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                BibleIntroText(1, "GEN", "INTRO")
            }
        }
        composeTestRule.waitForIdle()

        val textBounds = composeTestRule.onNodeWithText("Filler text").getBoundsInRoot()
        val cellBounds = composeTestRule.onNodeWithText("A").getBoundsInRoot()
        assertTrue(cellBounds.left > textBounds.left)
    }

    @Test
    fun `LTR system with LTR version aligns content to start`() {
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns "<p>Intro</p>"
        coEvery {
            BibleVersionRendering.introTextBlocks(
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
        } returns
            listOf(
                annotatedBlock("Filler text"),
                BibleTextBlock(
                    text = AnnotatedString(""),
                    chapter = 1,
                    rows = listOf(listOf(AnnotatedString("A"))),
                    headIndent = 0.sp,
                    marginTop = 8.dp,
                    alignment = TextAlign.Start,
                    footnotes = emptyList(),
                ),
            )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                BibleIntroText(1, "GEN", "INTRO")
            }
        }
        composeTestRule.waitForIdle()

        val textBounds = composeTestRule.onNodeWithText("Filler text").getBoundsInRoot()
        val cellBounds = composeTestRule.onNodeWithText("A").getBoundsInRoot()
        assertTrue(cellBounds.left >= textBounds.left)
    }

    @Test
    fun `RTL system with RTL version aligns content to start`() {
        coEvery { mockVersionRepository.version(any()) } returns rtlVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns "<p>Intro</p>"
        coEvery {
            BibleVersionRendering.introTextBlocks(
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
        } returns
            listOf(
                annotatedBlock("Filler text"),
                BibleTextBlock(
                    text = AnnotatedString(""),
                    chapter = 1,
                    rows = listOf(listOf(AnnotatedString("A"))),
                    headIndent = 0.sp,
                    marginTop = 8.dp,
                    alignment = TextAlign.Start,
                    footnotes = emptyList(),
                ),
            )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BibleIntroText(1, "GEN", "INTRO")
            }
        }
        composeTestRule.waitForIdle()

        val textBounds = composeTestRule.onNodeWithText("Filler text").getBoundsInRoot()
        val cellBounds = composeTestRule.onNodeWithText("A").getBoundsInRoot()
        assertTrue(cellBounds.right <= textBounds.right)
    }

    @Test
    fun `RTL system with LTR version aligns content to end`() {
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns "<p>Intro</p>"
        coEvery {
            BibleVersionRendering.introTextBlocks(
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
        } returns
            listOf(
                annotatedBlock("Filler text"),
                BibleTextBlock(
                    text = AnnotatedString(""),
                    chapter = 1,
                    rows = listOf(listOf(AnnotatedString("A"))),
                    headIndent = 0.sp,
                    marginTop = 8.dp,
                    alignment = TextAlign.Start,
                    footnotes = emptyList(),
                ),
            )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BibleIntroText(1, "GEN", "INTRO")
            }
        }
        composeTestRule.waitForIdle()

        val textBounds = composeTestRule.onNodeWithText("Filler text").getBoundsInRoot()
        val cellBounds = composeTestRule.onNodeWithText("A").getBoundsInRoot()
        assertTrue(cellBounds.right < textBounds.right)
    }

    // endregion

    // region Blank Block Filtering

    @Test
    fun `blank blocks are filtered from visible blocks`() {
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns ""

        val blocks =
            listOf(
                annotatedBlock("Visible block"),
                annotatedBlock(""),
                annotatedBlock("   "),
                annotatedBlock("Another visible block"),
            )
        coEvery {
            BibleVersionRendering.introTextBlocks(
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
        } returns blocks

        composeTestRule.setContent { BibleIntroText(1, "GEN", "INTRO") }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Visible block").assertIsDisplayed()
        composeTestRule.onNodeWithText("Another visible block").assertIsDisplayed()
        composeTestRule.onNodeWithText("").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("    ").assertIsNotDisplayed()
    }

    // endregion

    // region Block Margins

    @Test
    fun `first block has zero top margin and subsequent blocks use marginTop`() {
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns ""
        coEvery {
            BibleVersionRendering.introTextBlocks(
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
        } returns
            listOf(
                annotatedBlock("First Block", marginTop = 20.dp),
                annotatedBlock("Second Block", marginTop = 20.dp),
            )

        composeTestRule.setContent { BibleIntroText(1, "GEN", "INTRO") }
        composeTestRule.waitForIdle()

        val firstBlock = composeTestRule.onNodeWithText("First Block").getBoundsInRoot()
        val secondBlock = composeTestRule.onNodeWithText("Second Block").getBoundsInRoot()
        assertEquals(0.dp, firstBlock.top)
        assertTrue(secondBlock.top >= firstBlock.bottom + 20.dp)
    }

    // endregion

    // region Paragraph Spacing Defaults

    @Test
    fun `paragraph spacing defaults to half of the fontSize`() {
        val fontSize = 20.sp
        val expectedSpacing = (fontSize / 2).value.dp

        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns ""
        coEvery {
            BibleVersionRendering.introTextBlocks(
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
        } returns listOf(annotatedBlock("Intro chapter 1"), annotatedBlock("Intro chapter 2", marginTop = 0.dp))

        composeTestRule.setContent {
            BibleIntroText(
                1,
                "GEN",
                "INTRO",
                textOptions = BibleTextOptions(fontSize = fontSize),
            )
        }
        composeTestRule.waitForIdle()

        val firstParagraph = composeTestRule.onNodeWithText("Intro chapter 1").getBoundsInRoot()
        val secondParagraph = composeTestRule.onNodeWithText("Intro chapter 2").getBoundsInRoot()
        val gap = secondParagraph.top - firstParagraph.bottom
        assertEquals(expectedSpacing, gap)
    }

    // endregion

    // region Table Blocks

    @Test
    fun `table blocks render rows and columns`() {
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns ""

        val tableBlock =
            BibleTextBlock(
                text = AnnotatedString(""),
                chapter = 1,
                rows =
                    listOf(
                        listOf(AnnotatedString("Cell 1"), AnnotatedString("Cell 2")),
                        listOf(AnnotatedString("Cell 3"), AnnotatedString("Cell 4")),
                    ),
                headIndent = 0.sp,
                marginTop = 8.dp,
                alignment = TextAlign.Start,
                footnotes = emptyList(),
            )

        coEvery {
            BibleVersionRendering.introTextBlocks(
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
        } returns listOf(tableBlock)

        composeTestRule.setContent { BibleIntroText(1, "GEN", "INTRO") }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Cell 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cell 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cell 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cell 4").assertIsDisplayed()
    }

    // endregion

    // region Footnote Tap

    @Test
    fun `tapping FOOTNOTE_MARKER invokes onFootnoteTap with block footnotes`() {
        var tappedFootnotes: List<AnnotatedString>? = null

        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns ""

        val footnoteText = buildAnnotatedString { append("This is a footnote") }
        val block = annotatedBlock(text = "*", footnoteMarkerRange = 0..0, footnotes = listOf(footnoteText))
        coEvery {
            BibleVersionRendering.introTextBlocks(
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
        } returns listOf(block)

        composeTestRule.setContent {
            BibleIntroText(
                1,
                "GEN",
                "INTRO",
                onFootnoteTap = { footnotes -> tappedFootnotes = footnotes },
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("*").performClick()
        composeTestRule.waitForIdle()

        assertTrue(tappedFootnotes != null)
        assertEquals(1, tappedFootnotes.size)
        assertEquals("This is a footnote", tappedFootnotes.first().text)
    }

    @Test
    fun `tapping FOOTNOTE_IMAGE invokes onFootnoteTap with block footnotes`() {
        var tappedFootnotes: List<AnnotatedString>? = null

        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns ""

        val footnoteText = buildAnnotatedString { append("Image footnote") }
        val block = annotatedBlock(text = "†", footnoteImageRange = 0..0, footnotes = listOf(footnoteText))
        coEvery {
            BibleVersionRendering.introTextBlocks(
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
        } returns listOf(block)

        composeTestRule.setContent {
            BibleIntroText(
                1,
                "GEN",
                "INTRO",
                onFootnoteTap = { footnotes -> tappedFootnotes = footnotes },
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("†").performClick()
        composeTestRule.waitForIdle()

        assertTrue(tappedFootnotes != null)
        assertEquals(1, tappedFootnotes.size)
        assertEquals("Image footnote", tappedFootnotes.first().text)
    }

    // endregion

    // region Reloads on Parameter Change

    @Test
    fun `reloads when versionId, bookUSFM, passageId, or textOptions change`() {
        val versionIdState = mutableIntStateOf(1)
        val bookUSFMState = mutableStateOf("GEN")
        val passageIdState = mutableStateOf("INTRO")
        val textOptionsState = mutableStateOf(BibleTextOptions())

        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
        coEvery { mockIntroRepository.introContent(any(), any()) } returns ""
        coEvery {
            BibleVersionRendering.introTextBlocks(
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
        } returnsMany
            listOf(
                listOf(annotatedBlock("Content 1")),
                listOf(annotatedBlock("Content 2")),
                listOf(annotatedBlock("Content 3")),
                listOf(annotatedBlock("Content 4")),
                listOf(annotatedBlock("Content 5")),
            )

        composeTestRule.setContent {
            val versionId by remember { versionIdState }
            val bookUSFM by remember { bookUSFMState }
            val passageId by remember { passageIdState }
            val textOptions by remember { textOptionsState }
            BibleIntroText(versionId, bookUSFM, passageId, textOptions = textOptions)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Content 1").assertIsDisplayed()

        versionIdState.intValue = 2
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Content 2").assertIsDisplayed()

        bookUSFMState.value = "EXO"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Content 3").assertIsDisplayed()

        passageIdState.value = "GEN:1"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Content 4").assertIsDisplayed()

        textOptionsState.value = BibleTextOptions(fontSize = 24.sp)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Content 5").assertIsDisplayed()
    }

    // endregion
}
