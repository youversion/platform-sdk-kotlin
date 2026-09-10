package com.youversion.platform.reader.sheets

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.core.highlights.domain.BibleHighlightsRepository
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.views.rendering.BibleReferenceAttribute
import com.youversion.platform.ui.views.rendering.BibleTextBlock
import com.youversion.platform.ui.views.rendering.BibleVersionRendering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BibleReaderFootnotesSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockVersionRepository =
        mockk<BibleVersionRepository> {
            coEvery { version(any()) } returns BibleVersion(id = 1)
        }

    private val testVersion =
        BibleVersion(
            id = 1,
            localizedAbbreviation = "NIV",
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

    private val testReference =
        BibleReference(
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
        )

    private val testVerseReference =
        BibleReference(
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
            verse = 3,
        )

    @Before
    fun setUp() {
        mockkObject(BibleVersionRendering)
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

        PlatformKoinGraph.start(
            listOf(
                module {
                    single<BibleVersionRepository> { mockVersionRepository }
                    single<BibleChapterRepository> { mockk(relaxed = true) }
                    single { BibleHighlightsRepository(api = mockk(relaxed = true)) }
                },
            ),
        )
    }

    @After
    fun tearDown() {
        PlatformKoinGraph.stop()
        unmockkObject(BibleVersionRendering)
    }

    private fun renderSheet(
        version: BibleVersion? = testVersion,
        reference: BibleReference? = testReference,
        footnotes: List<AnnotatedString> = emptyList(),
        onDismissRequest: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                BibleReaderFootnotesSheet(
                    onDismissRequest = onDismissRequest,
                    version = version,
                    reference = reference,
                    footnotes = footnotes,
                )
            }
        }
    }

    private fun stubRenderedFootnotes(footnotes: List<AnnotatedString>) {
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
                    firstLineHeadIndent = 0,
                    headIndent = 0,
                    marginTop = 0.dp,
                    marginBottom = 0.dp,
                    alignment = TextAlign.Start,
                    footnotes = footnotes,
                ),
            )
    }

    private fun footnoteFor(
        reference: BibleReference,
        text: String,
    ) = buildAnnotatedString {
        append(text)
        addStringAnnotation(
            tag = BibleReferenceAttribute.NAME,
            annotation = "${reference.versionId}:${reference.bookUSFM}:${reference.chapter}:${reference.verseStart}",
            start = 0,
            end = text.length,
        )
    }

    private fun renderFootnotes(footnotes: List<AnnotatedString>) {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                Footnotes(footnotes = footnotes)
            }
        }
    }

    // ----- Header

    @Test
    fun `displays version title and reference when both are non-null`() {
        renderSheet(
            version = testVersion,
            reference = testReference,
        )

        composeTestRule
            .onNodeWithText("Genesis 1 NIV", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `hides header when version is null`() {
        renderSheet(
            version = null,
            reference = testReference,
        )

        composeTestRule
            .onNodeWithText("NIV", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun `hides header when reference is null`() {
        renderSheet(
            version = testVersion,
            reference = null,
        )

        composeTestRule
            .onNodeWithText("Genesis 1 NIV", substring = true)
            .assertDoesNotExist()
    }

    // ----- Footnotes

    @Test
    fun `renders lettered enumeration for each footnote`() {
        renderFootnotes(
            footnotes =
                listOf(
                    AnnotatedString("First footnote"),
                    AnnotatedString("Second footnote"),
                    AnnotatedString("Third footnote"),
                ),
        )

        composeTestRule.onNodeWithText("a.").assertIsDisplayed()
        composeTestRule.onNodeWithText("b.").assertIsDisplayed()
        composeTestRule.onNodeWithText("c.").assertIsDisplayed()
    }

    @Test
    fun `renders dividers separating footnotes`() {
        renderFootnotes(
            footnotes =
                listOf(
                    AnnotatedString("First footnote"),
                    AnnotatedString("Second footnote"),
                    AnnotatedString("Third footnote"),
                ),
        )

        composeTestRule
            .onAllNodesWithTag("footnote_divider")
            .assertCountEquals(4)
    }

    @Test
    fun `renders all footnote texts`() {
        renderFootnotes(
            footnotes =
                listOf(
                    AnnotatedString("First footnote"),
                    AnnotatedString("Second footnote"),
                    AnnotatedString("Third footnote"),
                ),
        )

        composeTestRule.onNodeWithText("First footnote").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second footnote").assertIsDisplayed()
        composeTestRule.onNodeWithText("Third footnote").assertIsDisplayed()
    }

    // ----- Re-rendering

    @Test
    fun `shows re-rendered footnotes when an annotation matches the reference`() {
        stubRenderedFootnotes(listOf(footnoteFor(testVerseReference, "Re-rendered footnote")))

        renderSheet(
            reference = testVerseReference,
            footnotes = listOf(AnnotatedString("Passed-in footnote")),
        )

        composeTestRule.onNodeWithText("Re-rendered footnote").assertIsDisplayed()
        composeTestRule.onNodeWithText("Passed-in footnote").assertDoesNotExist()
    }

    @Test
    fun `keeps passed-in footnotes when no annotation matches the reference`() {
        val otherVerse = testVerseReference.copy(verseStart = 9, verseEnd = 9)
        stubRenderedFootnotes(listOf(footnoteFor(otherVerse, "Other verse footnote")))

        renderSheet(
            reference = testVerseReference,
            footnotes = listOf(AnnotatedString("Passed-in footnote")),
        )

        composeTestRule.onNodeWithText("Passed-in footnote").assertIsDisplayed()
        composeTestRule.onNodeWithText("Other verse footnote").assertDoesNotExist()
    }

    @Test
    fun `renders the chapter once for both the passage and its footnotes`() {
        stubRenderedFootnotes(listOf(footnoteFor(testVerseReference, "Re-rendered footnote")))

        renderSheet(reference = testVerseReference)

        coVerify(exactly = 1) {
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
}
