package com.youversion.platform.ui.views

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.turbine.test
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.core.utilities.exceptions.BibleVersionApiException
import com.youversion.platform.ui.views.rendering.BibleReferenceAttribute
import com.youversion.platform.ui.views.rendering.BibleTextBlock
import com.youversion.platform.ui.views.rendering.BibleTextCategory
import com.youversion.platform.ui.views.rendering.BibleTextCategoryAttribute
import com.youversion.platform.ui.views.rendering.BibleVersionRendering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BibleTextTests {
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

    private val ltrVersion = BibleVersion(id = 1, abbreviation = "KJV", textDirection = "ltr")
    private val rtlVersion = BibleVersion(id = 1, abbreviation = "KJV", textDirection = "rtl")

    private fun annotatedBlock(
        text: String,
        referenceAnnotation: String? = null,
        marginTop: Dp = 8.dp,
        footnoteMarkerRange: IntRange? = null,
        footnotes: List<AnnotatedString> = emptyList(),
    ): BibleTextBlock {
        val annotatedText =
            buildAnnotatedString {
                append(text)
                if (referenceAnnotation != null) {
                    addStringAnnotation(
                        tag = BibleReferenceAttribute.NAME,
                        annotation = referenceAnnotation,
                        start = 0,
                        end = text.length,
                    )
                }
                if (footnoteMarkerRange != null) {
                    addStringAnnotation(
                        tag = BibleTextCategoryAttribute.NAME,
                        annotation = BibleTextCategory.FOOTNOTE_MARKER.name,
                        start = footnoteMarkerRange.first,
                        end = footnoteMarkerRange.last + 1,
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

    // region convertToEnumeration

    @Test
    fun `zero converts to a`() {
        assertEquals("a", 0.convertToEnumeration())
    }

    @Test
    fun `one converts to b`() {
        assertEquals("b", 1.convertToEnumeration())
    }

    @Test
    fun `twenty five converts to z`() {
        assertEquals("z", 25.convertToEnumeration())
    }

    @Test
    fun `values greater than twenty five clamp to z`() {
        assertEquals("z", 26.convertToEnumeration())
        assertEquals("z", 100.convertToEnumeration())
    }

    // endregion

    @Before
    fun setUp() {
        mockkObject(BibleVersionRendering)
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

    // region Loading Phase Transitions

    @Test
    fun `transitions to SUCCESS when textBlocks returns non-null`() {
        val phaseFlow = MutableSharedFlow<BibleTextLoadingPhase>(replay = 10)
        val textBlocksDeferred = CompletableDeferred<List<BibleTextBlock>?>()
        composeTestRule.mainClock.autoAdvance = false
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
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
        } coAnswers { textBlocksDeferred.await() }

        composeTestRule.setContent {
            BibleText(
                reference = testReference,
                onStateChange = { phaseFlow.tryEmit(it) },
            )
        }
        composeTestRule.mainClock.advanceTimeBy(1000)
        textBlocksDeferred.complete(listOf(annotatedBlock("In the beginning")))
        composeTestRule.mainClock.advanceTimeBy(1000)

        runBlocking {
            phaseFlow.test {
                assertEquals(BibleTextLoadingPhase.INACTIVE, awaitItem())
                assertEquals(BibleTextLoadingPhase.LOADING, awaitItem())
                assertEquals(BibleTextLoadingPhase.SUCCESS, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `transitions to FAILED when textBlocks returns null`() {
        val phaseFlow = MutableSharedFlow<BibleTextLoadingPhase>(replay = 10)
        val textBlocksDeferred = CompletableDeferred<List<BibleTextBlock>?>()
        composeTestRule.mainClock.autoAdvance = false
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
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
        } coAnswers { textBlocksDeferred.await() }

        composeTestRule.setContent {
            BibleText(
                reference = testReference,
                onStateChange = { phaseFlow.tryEmit(it) },
            )
        }
        composeTestRule.mainClock.advanceTimeBy(1000)
        textBlocksDeferred.complete(null)
        composeTestRule.mainClock.advanceTimeBy(1000)

        runBlocking {
            phaseFlow.test {
                assertEquals(BibleTextLoadingPhase.INACTIVE, awaitItem())
                assertEquals(BibleTextLoadingPhase.LOADING, awaitItem())
                assertEquals(BibleTextLoadingPhase.FAILED, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `transitions to NOT_PERMITTED when BibleVersionApiException is thrown`() {
        val phaseFlow = MutableSharedFlow<BibleTextLoadingPhase>(replay = 10)
        composeTestRule.mainClock.autoAdvance = false
        val versionDeferred = CompletableDeferred<BibleVersion>()
        coEvery { mockVersionRepository.version(any()) } coAnswers { versionDeferred.await() }
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
        } returns null

        composeTestRule.setContent {
            BibleText(
                reference = testReference,
                onStateChange = { phaseFlow.tryEmit(it) },
            )
        }
        composeTestRule.mainClock.advanceTimeBy(1000)
        versionDeferred.completeExceptionally(
            BibleVersionApiException(BibleVersionApiException.Reason.NOT_PERMITTED),
        )
        composeTestRule.mainClock.advanceTimeBy(1000)

        runBlocking {
            phaseFlow.test {
                assertEquals(BibleTextLoadingPhase.INACTIVE, awaitItem())
                assertEquals(BibleTextLoadingPhase.LOADING, awaitItem())
                assertEquals(BibleTextLoadingPhase.NOT_PERMITTED, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `transitions to FAILED when generic exception is thrown`() {
        val phaseFlow = MutableSharedFlow<BibleTextLoadingPhase>(replay = 10)
        composeTestRule.mainClock.autoAdvance = false
        val versionDeferred = CompletableDeferred<BibleVersion>()
        coEvery { mockVersionRepository.version(any()) } coAnswers { versionDeferred.await() }
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
        } returns null

        composeTestRule.setContent {
            BibleText(
                reference = testReference,
                onStateChange = { phaseFlow.tryEmit(it) },
            )
        }
        composeTestRule.mainClock.advanceTimeBy(1000)
        versionDeferred.completeExceptionally(RuntimeException("Network error"))
        composeTestRule.mainClock.advanceTimeBy(1000)

        runBlocking {
            phaseFlow.test {
                assertEquals(BibleTextLoadingPhase.INACTIVE, awaitItem())
                assertEquals(BibleTextLoadingPhase.LOADING, awaitItem())
                assertEquals(BibleTextLoadingPhase.FAILED, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `transitions to INACTIVE when CancellationException is thrown`() {
        val phaseFlow = MutableSharedFlow<BibleTextLoadingPhase>(replay = 10)
        composeTestRule.mainClock.autoAdvance = false
        val versionDeferred = CompletableDeferred<BibleVersion>()
        coEvery { mockVersionRepository.version(any()) } coAnswers { versionDeferred.await() }

        composeTestRule.setContent {
            BibleText(
                reference = testReference,
                onStateChange = { phaseFlow.tryEmit(it) },
            )
        }
        composeTestRule.mainClock.advanceTimeBy(1000)
        versionDeferred.completeExceptionally(CancellationException("cancelled"))
        composeTestRule.mainClock.advanceTimeBy(1000)

        runBlocking {
            phaseFlow.test {
                assertEquals(BibleTextLoadingPhase.INACTIVE, awaitItem())
                assertEquals(BibleTextLoadingPhase.LOADING, awaitItem())
                assertEquals(BibleTextLoadingPhase.INACTIVE, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // endregion

    // region Placeholder and Content Visibility

    @Test
    fun `placeholder is shown when loading phase is not SUCCESS`() {
        var lastPhase: BibleTextLoadingPhase? = null
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
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
        } returns
            null

        composeTestRule.setContent {
            BibleText(
                reference = testReference,
                onStateChange = { lastPhase = it },
            )
        }
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
        } returns
            listOf(annotatedBlock("In the beginning God created"))

        composeTestRule.setContent {
            BibleText(reference = testReference, onStateChange = { lastPhase = it })
        }
        composeTestRule.waitForIdle()

        assertEquals(BibleTextLoadingPhase.SUCCESS, lastPhase)
        composeTestRule.onNodeWithText("In the beginning God created").assertIsDisplayed()
    }

    // endregion

    // region Blank Block Filtering

    @Test
    fun `blank blocks are filtered from visible blocks`() {
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion

        val blocks =
            listOf(
                annotatedBlock("Visible block"),
                annotatedBlock(""),
                annotatedBlock("   "),
                annotatedBlock("Another visible block"),
            )
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
        } returns
            blocks

        composeTestRule.setContent {
            BibleText(reference = testReference)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Visible block").assertIsDisplayed()
        composeTestRule.onNodeWithText("Another visible block").assertIsDisplayed()
        composeTestRule.onNodeWithText("").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("   ").assertIsNotDisplayed()
    }

    // endregion

    // region Reload Behavior

    @Test
    fun `reloads when reference changes`() {
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
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
        } returns
            listOf(annotatedBlock("First load"))

        var currentReference = testReference
        composeTestRule.setContent {
            BibleText(reference = currentReference)
        }
        composeTestRule.waitForIdle()

        val secondReference =
            BibleReference(
                versionId = 1,
                bookUSFM = "GEN",
                chapter = 2,
                verse = 1,
            )

        coEvery {
            BibleVersionRendering.textBlocks(
                any(),
                eq(secondReference),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            listOf(annotatedBlock("Second load"))

        currentReference = secondReference
        composeTestRule.waitForIdle()

        coVerify(atLeast = 1) { mockVersionRepository.version(any()) }
    }

    // endregion

    // region Verse Tap

    @Test
    fun `verse tap invokes onVerseTap with parsed BibleReference`() {
        var tappedReference: BibleReference? = null
        var tappedPosition: Offset? = null
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion

        val block = annotatedBlock("In the beginning", referenceAnnotation = "1:GEN:1:1")
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
        } returns
            listOf(block)

        composeTestRule.setContent {
            BibleText(
                reference = testReference,
                onVerseTap = { ref, pos ->
                    tappedReference = ref
                    tappedPosition = pos
                },
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("In the beginning").performClick()
        composeTestRule.waitForIdle()

        assertTrue(tappedReference != null)
        assertEquals(1, tappedReference.versionId)
        assertEquals("GEN", tappedReference.bookUSFM)
        assertEquals(1, tappedReference.chapter)
        assertEquals(1, tappedReference.verseStart)
        assertEquals(1, tappedReference.verseEnd)
        assertTrue(tappedPosition != null)
    }

    // endregion

    // region Table Blocks

    @Test
    fun `table blocks render rows and columns`() {
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion

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
        } returns
            listOf(tableBlock)

        composeTestRule.setContent {
            BibleText(reference = testReference)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Cell 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cell 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cell 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cell 4").assertIsDisplayed()
    }

    @Test
    fun `table cell tap invokes onVerseTap with parsed BibleReference`() {
        var tappedReference: BibleReference? = null
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion

        val cellText =
            buildAnnotatedString {
                append("Selah")
                addStringAnnotation(
                    tag = BibleReferenceAttribute.NAME,
                    annotation = "1:GEN:1:3",
                    start = 0,
                    end = 5,
                )
            }
        val tableBlock =
            BibleTextBlock(
                text = AnnotatedString(""),
                chapter = 1,
                rows = listOf(listOf(cellText)),
                headIndent = 0.sp,
                marginTop = 8.dp,
                alignment = TextAlign.Start,
                footnotes = emptyList(),
            )

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
        } returns listOf(tableBlock)

        composeTestRule.setContent {
            BibleText(
                reference = testReference,
                onVerseTap = { ref, _ -> tappedReference = ref },
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Selah").performClick()
        composeTestRule.waitForIdle()

        assertTrue(tappedReference != null)
        assertEquals(1, tappedReference.versionId)
        assertEquals("GEN", tappedReference.bookUSFM)
        assertEquals(1, tappedReference.chapter)
        assertEquals(3, tappedReference.verseStart)
        assertEquals(3, tappedReference.verseEnd)
    }

    // endregion

    // region StandardPlaceholder

    @Test
    fun `StandardPlaceholder INACTIVE shows nothing`() {
        composeTestRule.setContent {
            StandardPlaceholder(phase = BibleTextLoadingPhase.INACTIVE)
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(
                "You've lost your internet connection. Re-connect and download a Bible version to proceed offline.",
            ).assertDoesNotExist()
        composeTestRule
            .onNodeWithText(
                "Your previously selected Bible version is unavailable. Another similar Bible version has been selected for you.",
            ).assertDoesNotExist()
    }

    @Test
    fun `StandardPlaceholder LOADING shows CircularProgressIndicator`() {
        composeTestRule.setContent {
            StandardPlaceholder(phase = BibleTextLoadingPhase.LOADING)
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(
                "You've lost your internet connection. Re-connect and download a Bible version to proceed offline.",
            ).assertDoesNotExist()
        composeTestRule
            .onNodeWithText(
                "Your previously selected Bible version is unavailable. Another similar Bible version has been selected for you.",
            ).assertDoesNotExist()
        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertExists()
    }

    @Test
    fun `StandardPlaceholder NOT_PERMITTED shows version unavailable message`() {
        composeTestRule.setContent {
            StandardPlaceholder(phase = BibleTextLoadingPhase.NOT_PERMITTED)
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Version unavailable")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "Your previously selected Bible version is unavailable. Another similar Bible version has been selected for you.",
            ).assertIsDisplayed()
    }

    @Test
    fun `StandardPlaceholder FAILED shows offline message`() {
        composeTestRule.setContent {
            StandardPlaceholder(phase = BibleTextLoadingPhase.FAILED)
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("No Wi-Fi")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "You've lost your internet connection. Re-connect and download a Bible version to proceed offline.",
            ).assertIsDisplayed()
    }

    @Test
    fun `StandardPlaceholder SUCCESS shows nothing`() {
        composeTestRule.setContent {
            StandardPlaceholder(phase = BibleTextLoadingPhase.SUCCESS)
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(
                "You've lost your internet connection. Re-connect and download a Bible version to proceed offline.",
            ).assertDoesNotExist()
        composeTestRule
            .onNodeWithText(
                "Your previously selected Bible version is unavailable. Another similar Bible version has been selected for you.",
            ).assertDoesNotExist()
    }

    // endregion

    // region RTL/LTR Alignment

    @Test
    fun `LTR system with RTL version aligns content to end`() {
        coEvery { mockVersionRepository.version(any()) } returns rtlVersion
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
                BibleText(reference = testReference)
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
                BibleText(reference = testReference)
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
                BibleText(reference = testReference)
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
                BibleText(reference = testReference)
            }
        }
        composeTestRule.waitForIdle()

        val textBounds = composeTestRule.onNodeWithText("Filler text").getBoundsInRoot()
        val cellBounds = composeTestRule.onNodeWithText("A").getBoundsInRoot()
        assertTrue(cellBounds.right <= textBounds.right)
    }

    // endregion

    // region Reload on textOptions Change

    @Test
    fun `reloads when textOptions change`() {
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
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
        } returns listOf(annotatedBlock("Text content"))

        var textOptions by mutableStateOf(BibleTextOptions())
        composeTestRule.setContent {
            BibleText(reference = testReference, textOptions = textOptions)
        }
        composeTestRule.waitForIdle()

        textOptions = BibleTextOptions(fontFamily = FontFamily.Monospace)
        composeTestRule.waitForIdle()

        coVerify(atLeast = 2) {
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
        }
    }

    // endregion

    // region Verse Tap Non-Annotated

    @Test
    fun `tapping non-annotated area does not invoke onVerseTap`() {
        var tappedReference: BibleReference? = null
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
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
        } returns listOf(annotatedBlock("Plain text without annotations"))

        composeTestRule.setContent {
            BibleText(
                reference = testReference,
                onVerseTap = { ref, _ -> tappedReference = ref },
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Plain text without annotations").performClick()
        composeTestRule.waitForIdle()

        assertNull(tappedReference)
    }

    // endregion

    // region Footnote Tap

    @Test
    fun `tapping footnote marker invokes onFootnoteTap with matching footnotes`() {
        var tappedFootnotes: List<AnnotatedString>? = null
        var tappedReference: BibleReference? = null
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion

        val footnoteText =
            buildAnnotatedString {
                append("This is a footnote")
                addStringAnnotation(
                    tag = BibleReferenceAttribute.NAME,
                    annotation = "1:GEN:1:1",
                    start = 0,
                    end = 18,
                )
            }

        val block =
            annotatedBlock(
                text = "※",
                referenceAnnotation = "1:GEN:1:1",
                footnoteMarkerRange = 0..0,
                footnotes = listOf(footnoteText),
            )
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
        } returns listOf(block)

        composeTestRule.setContent {
            BibleText(
                reference = testReference,
                onFootnoteTap = { ref, footnotes ->
                    tappedReference = ref
                    tappedFootnotes = footnotes
                },
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("※").performClick()
        composeTestRule.waitForIdle()

        assertTrue(tappedReference != null)
        assertEquals(1, tappedReference.versionId)
        assertEquals("GEN", tappedReference.bookUSFM)
        assertEquals(1, tappedReference.chapter)
        assertEquals(1, tappedReference.verseStart)
        assertTrue(tappedFootnotes != null)
        assertEquals(1, tappedFootnotes.size)
        assertEquals("This is a footnote", tappedFootnotes.first().text)
    }

    // endregion

    // region Block Margins

    @Test
    fun `first block has zero top margin and subsequent blocks use marginTop`() {
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
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
        } returns
            listOf(
                annotatedBlock("First block", marginTop = 0.dp),
                annotatedBlock("Second block", marginTop = 24.dp),
            )

        composeTestRule.setContent {
            BibleText(reference = testReference)
        }
        composeTestRule.waitForIdle()

        val firstBounds = composeTestRule.onNodeWithText("First block").getBoundsInRoot()
        val secondBounds = composeTestRule.onNodeWithText("Second block").getBoundsInRoot()
        assertTrue(secondBounds.top > firstBounds.bottom)
    }

    // endregion

    // region Paragraph Spacing Defaults

    @Test
    fun `paragraph spacing defaults to half of fontSize`() {
        val fontSize = 20.sp
        val expectedSpacing = (fontSize / 2).value.dp
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
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
        } returns
            listOf(
                annotatedBlock("Block A", marginTop = 0.dp),
                annotatedBlock("Block B", marginTop = 0.dp),
            )

        composeTestRule.setContent {
            BibleText(
                reference = testReference,
                textOptions = BibleTextOptions(fontSize = fontSize),
            )
        }
        composeTestRule.waitForIdle()

        val firstBounds = composeTestRule.onNodeWithText("Block A").getBoundsInRoot()
        val secondBounds = composeTestRule.onNodeWithText("Block B").getBoundsInRoot()
        val gap = secondBounds.top - firstBounds.bottom
        val tolerance = 1.dp
        assertTrue(gap >= expectedSpacing - tolerance && gap <= expectedSpacing + tolerance)
    }

    // endregion

    // region Selection Underlines

    @Test
    fun `selected verses render on text blocks`() {
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion
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
        } returns listOf(annotatedBlock("Genesis verse", referenceAnnotation = "1:GEN:1:1"))

        composeTestRule.setContent {
            BibleText(
                reference = testReference,
                selectedVerses = setOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Genesis verse").assertIsDisplayed()
    }

    @Test
    fun `selected verses render on table cells`() {
        coEvery { mockVersionRepository.version(any()) } returns ltrVersion

        val cellText =
            buildAnnotatedString {
                append("Selah")
                addStringAnnotation(
                    tag = BibleReferenceAttribute.NAME,
                    annotation = "1:GEN:1:1",
                    start = 0,
                    end = 5,
                )
            }
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
        } returns
            listOf(
                BibleTextBlock(
                    text = AnnotatedString(""),
                    chapter = 1,
                    rows = listOf(listOf(cellText)),
                    headIndent = 0.sp,
                    marginTop = 8.dp,
                    alignment = TextAlign.Start,
                    footnotes = emptyList(),
                ),
            )

        composeTestRule.setContent {
            BibleText(
                reference = testReference,
                selectedVerses = setOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Selah").assertIsDisplayed()
    }

    // endregion
}
