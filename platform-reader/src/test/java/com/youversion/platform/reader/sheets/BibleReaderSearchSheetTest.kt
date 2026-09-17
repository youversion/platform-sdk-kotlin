package com.youversion.platform.reader.sheets

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
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
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BibleReaderSearchSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val query = mutableStateOf("")

    /** The text the view model has filled in so far, which a test can add to mid-render as a fetch would. */
    private val resultText = mutableStateOf(emptyMap<String, String?>())

    /** Whether a page is out, which a test can flip mid-render the way an ask for one does. */
    private val loadingNextPage = mutableStateOf(false)

    /** Whether a page would not load, which a test can clear mid-render the way asking again does. */
    private val nextPageLoadError = mutableStateOf(false)

    /** Renders the sheet with its query hoisted into [query], the way the search view model holds it. */
    private fun renderSheet(
        onDismissRequest: () -> Unit = {},
        onSubmit: () -> Unit = {},
        onRequestResultText: (BibleReference) -> Unit = {},
        onLoadNextPage: () -> Unit = {},
        results: List<BibleReference> = emptyList(),
        searchVersion: BibleVersion? = kjv,
        status: SearchStatus = SearchStatus.IDLE,
        nextPageToken: String? = null,
        isLoadingNextPage: Boolean = false,
        hasNextPageLoadError: Boolean = false,
    ) {
        loadingNextPage.value = isLoadingNextPage
        nextPageLoadError.value = hasNextPageLoadError

        composeTestRule.setContent {
            BibleReaderMaterialTheme(readerColorScheme = Cream) {
                BibleReaderSearchSheet(
                    onDismissRequest = onDismissRequest,
                    onQueryChange = { query.value = it },
                    onSubmit = onSubmit,
                    onRequestResultText = onRequestResultText,
                    onLoadNextPage = onLoadNextPage,
                    state =
                        State(
                            query = query.value,
                            status = status,
                            results = results,
                            resultTextByPassageId = resultText.value,
                            searchVersion = searchVersion,
                            nextPageToken = nextPageToken,
                            isLoadingNextPage = loadingNextPage.value,
                            hasNextPageLoadError = nextPageLoadError.value,
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

    /**
     * A row before and after its text arrives. The title moving down when the text lands is what says no space was
     * held open for it beforehand — had the row reserved a gap, the title would already have been where it ends up.
     */
    @Test
    fun `a result shows its title alone until its text arrives above it`() {
        renderSheet(results = listOf(john316), status = SearchStatus.COMPLETED)

        composeTestRule.onNodeWithText(JOHN_3_16_TEXT).assertDoesNotExist()
        val titleTopWithoutText = composeTestRule.onNodeWithText("JOHN 3:16").getUnclippedBoundsInRoot().top

        composeTestRule.runOnIdle { resultText.value = mapOf("JHN.3.16" to JOHN_3_16_TEXT) }

        composeTestRule.onNodeWithText(JOHN_3_16_TEXT).assertIsDisplayed()
        val titleTopWithText = composeTestRule.onNodeWithText("JOHN 3:16").getUnclippedBoundsInRoot().top
        assertTrue(titleTopWithText > titleTopWithoutText)
    }

    /**
     * More results than fit the sheet. Only those near the top ask for their text, and they are the top ones rather
     * than an arbitrary handful — a list that asked eagerly would have asked for all of them.
     */
    @Test
    fun `results ask for their own text as they come into view, not all at once`() {
        val requested = mutableListOf<BibleReference>()
        val results = (1..200).map { BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 3, verse = it) }

        renderSheet(
            onRequestResultText = { requested.add(it) },
            results = results,
            status = SearchStatus.COMPLETED,
        )
        composeTestRule.waitUntil { requested.isNotEmpty() }
        composeTestRule.waitForIdle()

        assertTrue(requested.size < results.size, "asked for all ${results.size} results at once")
        assertEquals(results.take(requested.size), requested)
    }

    /**
     * More results than fit the sheet, so the end of what is loaded is nowhere near the reader. Asking here would
     * be paging the whole set in at once rather than as it is read.
     */
    @Test
    fun `a list whose end is far below the reader does not ask for the next page`() {
        var askCount = 0
        renderSheet(
            onLoadNextPage = { askCount++ },
            results = manyResults,
            status = SearchStatus.COMPLETED,
            nextPageToken = SECOND_PAGE_TOKEN,
        )
        composeTestRule.waitForIdle()

        assertEquals(0, askCount)
    }

    /** Stopped five rows short of the end, which is where the next page is asked for rather than at the bottom. */
    @Test
    fun `scrolling to within five of the end asks for the next page`() {
        var askCount = 0
        renderSheet(
            onLoadNextPage = { askCount++ },
            results = manyResults,
            status = SearchStatus.COMPLETED,
            nextPageToken = SECOND_PAGE_TOKEN,
        )
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(SEARCH_RESULTS_TEST_TAG)
            .performScrollToIndex(manyResults.size - 5)
        composeTestRule.waitForIdle()

        assertTrue(askCount > 0, "the reader reached the last five rows without the next page being asked for")
    }

    @Test
    fun `a page in flight is announced at the foot of the list`() {
        renderSheet(
            results = listOf(john316),
            status = SearchStatus.COMPLETED,
            isLoadingNextPage = true,
        )

        composeTestRule.onNodeWithContentDescription(SEARCHING_LABEL).assertIsDisplayed()
    }

    @Test
    fun `a list with no page coming ends without an indicator`() {
        renderSheet(results = listOf(john316), status = SearchStatus.COMPLETED)

        composeTestRule.onNodeWithContentDescription(SEARCHING_LABEL).assertDoesNotExist()
        composeTestRule.onNodeWithText(FAILURE_MESSAGE).assertDoesNotExist()
    }

    @Test
    fun `a failed page load offers a retry at the foot of the list`() {
        var askCount = 0
        renderSheet(
            onLoadNextPage = { askCount++ },
            results = listOf(john316),
            status = SearchStatus.COMPLETED,
            hasNextPageLoadError = true,
        )

        composeTestRule.onNodeWithText(FAILURE_MESSAGE).performClick()

        assertEquals(1, askCount)
        composeTestRule.onNodeWithText("JOHN 3:16").assertIsDisplayed()
    }

    /**
     * Two taps in quick succession, the view model answering the first as it does in life: a page goes out, which
     * puts the indicator where the retry was. So the second tap finds nothing to land on and the reader has asked
     * once. That a second ask would be dropped even if one did land is proven at the view model's own seam.
     */
    @Test
    fun `a retry tapped twice in quick succession causes one request`() {
        var askCount = 0
        renderSheet(
            onLoadNextPage = {
                askCount++
                loadingNextPage.value = true
                nextPageLoadError.value = false
            },
            results = listOf(john316),
            status = SearchStatus.COMPLETED,
            hasNextPageLoadError = true,
        )

        repeat(2) {
            val retry = composeTestRule.onAllNodesWithText(FAILURE_MESSAGE)
            if (retry.fetchSemanticsNodes().isNotEmpty()) retry.onFirst().performClick()
        }

        assertEquals(1, askCount)
        composeTestRule.onNodeWithContentDescription(SEARCHING_LABEL).assertIsDisplayed()
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
        /** The value of the shared UI module's `close_search_bar` key, the clear button's own label. */
        const val CLEAR_LABEL = "Close the search input"

        /** The value of the reader's `search` key, which the searching indicator is labelled with. */
        const val SEARCHING_LABEL = "Search"

        const val FAILURE_MESSAGE = "Error"
        const val EMPTY_MESSAGE = "We're sorry, there are no Bible results for this search."

        const val JOHN_3_16_TEXT = "For God so loved the world"

        const val SECOND_PAGE_TOKEN = "second-page"

        /** More results than the sheet can show at once, so the end of the list starts out of the reader's reach. */
        val manyResults =
            (1..200).map { BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 3, verse = it) }

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
