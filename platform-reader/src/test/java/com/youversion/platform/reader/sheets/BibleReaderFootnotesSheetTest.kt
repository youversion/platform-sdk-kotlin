package com.youversion.platform.reader.sheets

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.reader.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.views.BibleTextOptions
import com.youversion.platform.ui.views.rendering.BibleVersionRendering
import io.mockk.coEvery
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
                    textOptions = BibleTextOptions(),
                    onDismissRequest = onDismissRequest,
                    version = version,
                    reference = reference,
                    footnotes = footnotes,
                )
            }
        }
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
}
