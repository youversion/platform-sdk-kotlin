package com.youversion.platform.reader.screens.references

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleBookIntro
import com.youversion.platform.core.bibles.models.BibleChapter
import com.youversion.platform.core.bibles.models.BibleVerse
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ReferencesScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val defaultReference =
        BibleReference(
            versionId = 1,
            bookUSFM = "GEN",
            chapter = 1,
        )

    private fun createChapters(count: Int): List<BibleChapter> =
        (1..count).map {
            BibleChapter(
                id = "ch$it",
                passageId = "p$it",
                title = "$it",
                verses = listOf(BibleVerse(id = "v1", passageId = "p1", title = "1")),
            )
        }

    private val genesisBook =
        BibleBook(
            id = "GEN",
            title = "Genesis",
            fullTitle = null,
            abbreviation = null,
            canon = "old_testament",
            chapters = createChapters(3),
        )

    private val exodusBook =
        BibleBook(
            id = "EXO",
            title = "Exodus",
            fullTitle = null,
            abbreviation = null,
            canon = "old_testament",
            chapters = createChapters(2),
        )

    private val leviticusBook =
        BibleBook(
            id = "LEV",
            title = "Leviticus",
            fullTitle = null,
            abbreviation = null,
            canon = "old_testament",
            chapters = createChapters(2),
            intro =
                BibleBookIntro(
                    id = "LEV_INTRO",
                    passageId = "LEV.INTRO1",
                    title = "Introduction to Leviticus",
                ),
        )

    private val testVersion =
        BibleVersion(
            id = 1,
            abbreviation = "KJV",
            bookCodes = listOf("GEN", "EXO", "LEV"),
            books = listOf(genesisBook, exodusBook, leviticusBook),
        )

    private fun setScreenContent(
        bibleVersion: BibleVersion = testVersion,
        bibleReference: BibleReference = defaultReference,
        onSelectionClick: (Int, String, String) -> Unit = { _, _, _ -> },
        onBackClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            BibleReaderMaterialTheme {
                ReferencesScreen(
                    bibleVersion = bibleVersion,
                    bibleReference = bibleReference,
                    onSelectionClick = onSelectionClick,
                    onBackClick = onBackClick,
                )
            }
        }
    }

    // ----- ReferencesScreen

    @Test
    fun `displays top app bar with Books title`() {
        setScreenContent()

        composeTestRule.onNodeWithText("Books").assertIsDisplayed()
    }

    @Test
    fun `displays search bar with placeholder`() {
        setScreenContent()

        composeTestRule.onNodeWithText("Search").assertIsDisplayed()
    }

    @Test
    fun `displays all book names`() {
        setScreenContent()

        composeTestRule.onNodeWithText("Genesis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exodus").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leviticus").assertIsDisplayed()
    }

    @Test
    fun `displays chapters for initially expanded book`() {
        setScreenContent()

        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("2").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun `clicking a different book header collapses the previously expanded book`() {
        setScreenContent()

        composeTestRule.onNodeWithText("3").assertIsDisplayed()

        composeTestRule.onNodeWithText("Exodus").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("3").assertDoesNotExist()
    }

    @Test
    fun `clicking a chapter invokes onSelectionClick`() {
        val clicks = mutableListOf<Triple<Int, String, String>>()
        setScreenContent(onSelectionClick = { versionId, bookCode, chapter ->
            clicks.add(Triple(versionId, bookCode, chapter))
        })

        assertEquals(0, clicks.size)

        composeTestRule.onNodeWithText("1").performClick()

        assertEquals(1, clicks.size)
        assertEquals(Triple(1, "GEN", "1"), clicks[0])
    }

    // ----- BookSearchBar

    @Test
    fun `typing in search bar filters books`() {
        setScreenContent()

        composeTestRule.onNodeWithText("Search").performTextInput("Exo")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Exodus").assertIsDisplayed()
        composeTestRule.onNodeWithText("Genesis").assertDoesNotExist()
        composeTestRule.onNodeWithText("Leviticus").assertDoesNotExist()
    }

    @Test
    fun `clearing search shows all books again`() {
        setScreenContent()

        composeTestRule.onNodeWithText("Search").performTextInput("Exo")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Exo").performTextReplacement("")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Genesis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exodus").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leviticus").assertIsDisplayed()
    }

    // ----- ChaptersGrid + IntroCell

    @Test
    fun `book with intro shows intro cell when expanded`() {
        setScreenContent(
            bibleReference = BibleReference(versionId = 1, bookUSFM = "LEV", chapter = 1),
        )

        composeTestRule.onNodeWithContentDescription("Intro").assertIsDisplayed()
    }

    @Test
    fun `clicking intro cell invokes onSelectionClick with introPassageId`() {
        val clicks = mutableListOf<Triple<Int, String, String>>()
        setScreenContent(
            bibleReference = BibleReference(versionId = 1, bookUSFM = "LEV", chapter = 1),
            onSelectionClick = { versionId, bookCode, chapter ->
                clicks.add(Triple(versionId, bookCode, chapter))
            },
        )

        composeTestRule.onNodeWithContentDescription("Intro").performClick()

        assertEquals(1, clicks.size)
        assertEquals(Triple(1, "LEV", "LEV.INTRO1"), clicks[0])
    }
}
