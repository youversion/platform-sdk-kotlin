package com.youversion.platform.reader.sheets

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleBook
import com.youversion.platform.core.bibles.models.BibleChapter
import com.youversion.platform.core.bibles.models.BibleVerse
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.reader.BibleReaderSearchViewModel.SearchStatus
import com.youversion.platform.reader.BibleReaderSearchViewModel.State
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.theme.Cream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class BibleReaderSearchSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val query = mutableStateOf("")

    /** Renders the sheet with its query hoisted into [query], the way the search view model holds it. */
    private fun renderSheet(
        onDismissRequest: () -> Unit = {},
        onSubmit: () -> Unit = {},
        results: List<BibleReference> = emptyList(),
        searchVersion: BibleVersion? = kjv,
        status: SearchStatus = SearchStatus.IDLE,
    ) {
        composeTestRule.setContent {
            BibleReaderMaterialTheme(readerColorScheme = Cream) {
                BibleReaderSearchSheet(
                    onDismissRequest = onDismissRequest,
                    onQueryChange = { query.value = it },
                    onSubmit = onSubmit,
                    state =
                        State(
                            query = query.value,
                            status = status,
                            results = results,
                            searchVersion = searchVersion,
                        ),
                )
            }
        }
    }

    @Test
    fun `the field is focused and ready to type into`() {
        renderSheet()

        composeTestRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun `typing reports the query`() {
        renderSheet()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Corinthians")

        assertEquals("Corinthians", query.value)
    }

    @Test
    fun `the clear button carries the reader's own label and empties the query`() {
        renderSheet()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Corinthians")

        composeTestRule.onNodeWithContentDescription(CLEAR_LABEL).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(CLEAR_LABEL).performClick()

        assertEquals("", query.value)
    }

    @Test
    fun `the action key offers search`() {
        renderSheet()

        composeTestRule.onNode(hasImeAction(ImeAction.Search)).assertIsDisplayed()
    }

    @Test
    fun `typing past one hundred characters is refused`() {
        renderSheet()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("a".repeat(100))
        assertEquals("a".repeat(100), query.value)

        composeTestRule.onNode(hasSetTextAction()).performTextInput("b")

        assertEquals("a".repeat(100), query.value)
    }

    @Test
    fun `Done dismisses the sheet`() {
        var isDismissed = false
        renderSheet(onDismissRequest = { isDismissed = true })

        composeTestRule.onNodeWithText("Done").performClick()

        composeTestRule.waitUntil { isDismissed }
    }

    @Test
    fun `a search in flight shows an indicator a screen reader can hear`() {
        renderSheet(status = SearchStatus.SEARCHING)

        composeTestRule.onNodeWithContentDescription(SEARCHING_LABEL).assertIsDisplayed()
    }

    @Test
    fun `results are listed as titles of the version the search ran against`() {
        renderSheet(results = listOf(john316, psalm231), status = SearchStatus.COMPLETED)

        composeTestRule.onNodeWithText("JOHN 3:16").assertIsDisplayed()
        composeTestRule.onNodeWithText("PSALMS 23:1").assertIsDisplayed()
    }

    @Test
    fun `a result falls back to its passage id when the searched version is unavailable`() {
        renderSheet(results = listOf(john316), searchVersion = null, status = SearchStatus.COMPLETED)

        composeTestRule.onNodeWithText("JHN.3.16").assertIsDisplayed()
    }

    @Test
    fun `a search that found nothing says so`() {
        renderSheet(status = SearchStatus.COMPLETED)

        composeTestRule.onNodeWithText(EMPTY_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun `a failed search says so and offers nothing to retry with`() {
        renderSheet(status = SearchStatus.FAILED)

        composeTestRule.onNodeWithText(FAILURE_MESSAGE).assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(SEARCH_MESSAGE_TEST_TAG)
            .onChildren()
            .filter(hasClickAction())
            .assertCountEquals(0)
    }

    @Test
    fun `an idle sheet reports neither results nor a failure`() {
        renderSheet(status = SearchStatus.IDLE)

        composeTestRule.onNodeWithText(EMPTY_MESSAGE).assertDoesNotExist()
        composeTestRule.onNodeWithText(FAILURE_MESSAGE).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(SEARCHING_LABEL).assertDoesNotExist()
    }

    private companion object {
        /** The value of the reader's `cancel` key, which the clear button is labelled with. */
        const val CLEAR_LABEL = "Cancel"

        /** The value of the reader's `search` key, which the searching indicator is labelled with. */
        const val SEARCHING_LABEL = "Search"

        const val FAILURE_MESSAGE = "Error"
        const val EMPTY_MESSAGE = "We're sorry, there are no Bible results for this search."

        private fun chapters(count: Int) =
            (1..count).map {
                BibleChapter(
                    id = "ch$it",
                    passageId = "p$it",
                    title = "$it",
                    verses = listOf(BibleVerse(id = "v1", passageId = "p1", title = "1")),
                )
            }

        val kjv =
            BibleVersion(
                id = 1,
                abbreviation = "KJV",
                books =
                    listOf(
                        BibleBook(
                            id = "JHN",
                            title = "John",
                            fullTitle = null,
                            abbreviation = null,
                            canon = "new_testament",
                            chapters = chapters(3),
                        ),
                        BibleBook(
                            id = "PSA",
                            title = "Psalms",
                            fullTitle = null,
                            abbreviation = null,
                            canon = "old_testament",
                            chapters = chapters(23),
                        ),
                    ),
            )

        val john316 = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 3, verse = 16)
        val psalm231 = BibleReference(versionId = 1, bookUSFM = "PSA", chapter = 23, verse = 1)
    }
}
