package com.youversion.platform.reader

import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.data.BibleVersionMemoryCache
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.search.api.SearchApi
import com.youversion.platform.core.search.models.SearchQuery
import com.youversion.platform.core.search.models.VerseSearchResults
import com.youversion.platform.reader.BibleReaderSearchViewModel.Action
import com.youversion.platform.reader.BibleReaderSearchViewModel.SearchStatus
import com.youversion.platform.ui.views.rendering.BibleVersionRendering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BibleReaderSearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val searchApi = mockk<SearchApi>()
    private val viewModel = BibleReaderSearchViewModel()

    /**
     * The only cache holding the chapter, so every read of it is a chapter the repository had to go and get rather
     * than one it already had in memory.
     */
    private val chapterSource = spyk(BibleVersionMemoryCache())

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(YouVersionApi)
        every { YouVersionApi.search } returns searchApi

        // Suggestions are offered on every open and on every keystroke, so a test that is not about
        // them still needs them answered.
        coEvery { searchApi.trendingQueries(languageRanges = any()) } returns emptyList()
        coEvery { searchApi.suggestedQueries(query = any(), languageRanges = any()) } returns emptyList()

        val chapterRepository =
            BibleChapterRepository(
                memoryCache = BibleVersionMemoryCache(),
                temporaryCache = BibleVersionMemoryCache(),
                persistentCache = chapterSource,
            )
        viewModel.bibleChapterRepository = { chapterRepository }
    }

    @AfterTest
    fun teardown() {
        testDispatcher.scheduler.advanceUntilIdle()
        unmockkObject(YouVersionApi)
        unmockkObject(BibleVersionRendering)
        Dispatchers.resetMain()
    }

    @Test
    fun `the query starts empty`() {
        assertEquals("", viewModel.state.value.query)
    }

    @Test
    fun `SetQuery records what was typed`() {
        viewModel.onAction(Action.SetQuery("Corinthians"))

        assertEquals("Corinthians", viewModel.state.value.query)
    }

    @Test
    fun `OpenSearch empties the query left behind by the last one`() {
        viewModel.onAction(Action.SetQuery("Corinthians"))

        viewModel.onAction(Action.OpenSearch(kjv))

        assertEquals("", viewModel.state.value.query)
    }

    @Test
    fun `OpenSearch records the version to search over and clears the last run's results`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            viewModel.onAction(Action.OpenSearch(esv))

            assertEquals(esv, viewModel.state.value.searchVersion)
            assertEquals(emptyList(), viewModel.state.value.results)
            assertEquals(SearchStatus.IDLE, viewModel.state.value.status)
        }

    @Test
    fun `Submit searches the version the reader is on and lists what came back`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316, psalm231))
            viewModel.onAction(Action.OpenSearch(kjv))

            submit("love")

            assertEquals(listOf(john316, psalm231), viewModel.state.value.results)
            assertEquals(SearchStatus.COMPLETED, viewModel.state.value.status)
            coVerify(exactly = 1) { searchApi.verses(query = "love", bibleId = kjv.id) }
        }

    @Test
    fun `Submit trims the query before searching`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))

            submit("  love  ")

            coVerify(exactly = 1) { searchApi.verses(query = "love", bibleId = kjv.id) }
        }

    @Test
    fun `a whitespace-only query does nothing and reports no error`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            submit("   ")

            assertEquals(SearchStatus.IDLE, viewModel.state.value.status)
            assertEquals(emptyList(), viewModel.state.value.results)
            coVerify(exactly = 1) { searchApi.verses(query = any(), bibleId = any()) }
        }

    @Test
    fun `a failed search reaches the failed status and leaves the query in the field`() =
        runTest(testDispatcher) {
            coEvery { searchApi.verses(query = any(), bibleId = any()) } throws RuntimeException("offline")
            viewModel.onAction(Action.OpenSearch(kjv))

            submit("love")

            assertEquals(SearchStatus.FAILED, viewModel.state.value.status)
            assertEquals("love", viewModel.state.value.query)
            assertEquals(emptyList(), viewModel.state.value.results)
        }

    @Test
    fun `the same query against the same version is not run a second time`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            viewModel.onAction(Action.Submit)
            advanceUntilIdle()

            coVerify(exactly = 1) { searchApi.verses(query = "love", bibleId = kjv.id) }
            assertEquals(listOf(john316), viewModel.state.value.results)
        }

    @Test
    fun `the same query runs again once the sheet has been reopened`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            assertEquals(listOf(john316), viewModel.state.value.results)
            assertEquals(SearchStatus.COMPLETED, viewModel.state.value.status)
            coVerify(exactly = 2) { searchApi.verses(query = "love", bibleId = kjv.id) }
        }

    @Test
    fun `a query typed back after an amendment runs again rather than leaving the sheet empty`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")
            viewModel.onAction(Action.SetQuery("loves"))

            submit("love")

            assertEquals(listOf(john316), viewModel.state.value.results)
            assertEquals(SearchStatus.COMPLETED, viewModel.state.value.status)
        }

    @Test
    fun `the same query runs again once the version has changed`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            viewModel.onAction(Action.OpenSearch(esv))
            submit("love")

            coVerify(exactly = 1) { searchApi.verses(query = "love", bibleId = kjv.id) }
            coVerify(exactly = 1) { searchApi.verses(query = "love", bibleId = esv.id) }
        }

    @Test
    fun `submitting again abandons the search in flight and its response never lands`() =
        runTest(testDispatcher) {
            coEvery { searchApi.verses(query = "lov", bibleId = kjv.id) } coAnswers {
                delay(SLOW_RESPONSE_MILLIS)
                verseSearchResults(listOf(psalm231))
            }
            coEvery { searchApi.verses(query = "love", bibleId = kjv.id) } returns
                verseSearchResults(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))

            viewModel.onAction(Action.SetQuery("lov"))
            viewModel.onAction(Action.Submit)
            advanceTimeBy(SLOW_RESPONSE_MILLIS / 2)
            submit("love")

            assertEquals(listOf(john316), viewModel.state.value.results)
            assertEquals(SearchStatus.COMPLETED, viewModel.state.value.status)
        }

    @Test
    fun `a search abandoned by typing is left idle rather than failed`() =
        runTest(testDispatcher) {
            coEvery { searchApi.verses(query = any(), bibleId = any()) } coAnswers {
                delay(SLOW_RESPONSE_MILLIS)
                verseSearchResults(listOf(john316))
            }
            viewModel.onAction(Action.OpenSearch(kjv))

            viewModel.onAction(Action.SetQuery("love"))
            viewModel.onAction(Action.Submit)
            advanceTimeBy(SLOW_RESPONSE_MILLIS / 2)
            assertEquals(SearchStatus.SEARCHING, viewModel.state.value.status)

            viewModel.onAction(Action.SetQuery("loves"))
            advanceUntilIdle()

            assertEquals(SearchStatus.IDLE, viewModel.state.value.status)
        }

    @Test
    fun `typing clears the results already on screen`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")
            assertTrue(
                viewModel.state.value.results
                    .isNotEmpty(),
            )

            viewModel.onAction(Action.SetQuery("loves"))

            assertEquals(emptyList(), viewModel.state.value.results)
        }

    @Test
    fun `a result's own verse text lands under its passage id`() =
        runTest(testDispatcher) {
            stubResultText("For God so loved the world")
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            loadResultText(john316)

            assertEquals(
                mapOf("JHN.3.16" to "For God so loved the world"),
                viewModel.state.value.resultTextByPassageId,
            )
        }

    @Test
    fun `result text is trimmed of the whitespace around it`() =
        runTest(testDispatcher) {
            stubResultText("\n  For God so loved the world  \n")
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            loadResultText(john316)

            assertEquals(
                "For God so loved the world",
                viewModel.state.value.resultTextByPassageId["JHN.3.16"],
            )
        }

    @Test
    fun `text arriving for a superseded result set never appears under the current one`() =
        runTest(testDispatcher) {
            mockkObject(BibleVersionRendering)
            coEvery { BibleVersionRendering.plainTextOf(any(), any()) } coAnswers {
                delay(SLOW_RESPONSE_MILLIS)
                "For God so loved the world"
            }
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            viewModel.onAction(Action.LoadResultText(john316))
            advanceTimeBy(SLOW_RESPONSE_MILLIS / 2)
            submit("loved")

            advanceUntilIdle()

            assertEquals(listOf(john316), viewModel.state.value.results)
            assertEquals(emptyMap(), viewModel.state.value.resultTextByPassageId)
        }

    @Test
    fun `a result whose text cannot be read keeps its title and reports no error`() =
        runTest(testDispatcher) {
            stubResultText(null)
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            loadResultText(john316)

            assertNull(viewModel.state.value.resultTextByPassageId["JHN.3.16"])
            assertEquals(listOf(john316), viewModel.state.value.results)
            assertEquals(SearchStatus.COMPLETED, viewModel.state.value.status)
        }

    /**
     * A row that fails leaves and re-enters the viewport, asking again each time it does. The chapter is read once
     * across all of it: a chapter that cannot be read is not read over and over as the reader scrolls past it.
     */
    @Test
    fun `a result whose text could not be read is not asked for again as its row comes back into view`() =
        runTest(testDispatcher) {
            stubResultText(null)
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            loadResultText(john316)
            loadResultText(john316)
            loadResultText(john316)

            coVerify(exactly = 1) { BibleVersionRendering.plainTextOf(any(), any()) }
        }

    @Test
    fun `text is not asked for before there is a result set to fetch it against`() =
        runTest(testDispatcher) {
            stubResultText("For God so loved the world")
            viewModel.onAction(Action.OpenSearch(kjv))

            loadResultText(john316)

            coVerify(exactly = 0) { BibleVersionRendering.plainTextOf(any(), any()) }
        }

    /**
     * The second ask arrives while the first is still out, as a row scrolled out and straight back would send it.
     */
    @Test
    fun `a result already asked for is not asked for a second time while its text is still coming`() =
        runTest(testDispatcher) {
            stubResultText("For God so loved the world")
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            viewModel.onAction(Action.LoadResultText(john316))
            viewModel.onAction(Action.LoadResultText(john316))
            advanceUntilIdle()

            coVerify(exactly = 1) { BibleVersionRendering.plainTextOf(any(), any()) }
        }

    /**
     * Two results in one chapter, asked for in turn the way rows coming into view ask. The second finds the chapter
     * the first left in the repository's memory cache, so the chapter is gone and got once between them.
     */
    @Test
    fun `two results in the same chapter cost one chapter fetch between them`() =
        runTest(testDispatcher) {
            chapterSource.addChapterContents(john3Html, john3)
            stubSearch(listOf(john31, john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            viewModel.onAction(Action.LoadResultText(john31))
            awaitResultText(john31)
            viewModel.onAction(Action.LoadResultText(john316))
            awaitResultText(john316)

            assertEquals(
                mapOf(
                    "JHN.3.1" to "There was a man of the Pharisees",
                    "JHN.3.16" to "For God so loved the world",
                ),
                viewModel.state.value.resultTextByPassageId,
            )
            coVerify(exactly = 1) { chapterSource.chapterContent(any()) }
        }

    @Test
    fun `a search records the token its next page is asked for with`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316), nextPageToken = SECOND_PAGE_TOKEN)
            viewModel.onAction(Action.OpenSearch(kjv))

            submit("love")

            assertEquals(SECOND_PAGE_TOKEN, viewModel.state.value.nextPageToken)
        }

    @Test
    fun `the next page is added to the results already listed`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316), nextPageToken = SECOND_PAGE_TOKEN)
            stubNextPage(listOf(psalm231))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            loadNextPage()

            assertEquals(listOf(john316, psalm231), viewModel.state.value.results)
            coVerify(exactly = 1) {
                searchApi.verses(query = "love", bibleId = kjv.id, pageToken = SECOND_PAGE_TOKEN)
            }
        }

    /**
     * The platform can hand back a result on two pages in a row. The list is keyed by passage id, so a repeat is
     * both a row the reader sees twice and a key collision.
     */
    @Test
    fun `a result returned on two pages is listed once`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316, psalm231), nextPageToken = SECOND_PAGE_TOKEN)
            stubNextPage(listOf(psalm231, john31))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            loadNextPage()

            assertEquals(listOf(john316, psalm231, john31), viewModel.state.value.results)
        }

    @Test
    fun `paging stops once the platform has no further results to give`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316), nextPageToken = SECOND_PAGE_TOKEN)
            stubNextPage(listOf(psalm231), nextPageToken = null)
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")
            loadNextPage()

            loadNextPage()

            assertNull(viewModel.state.value.nextPageToken)
            assertFalse(viewModel.state.value.isLoadingNextPage)
            coVerify(exactly = 2) { searchApi.verses(query = any(), bibleId = any(), pageToken = any()) }
        }

    /**
     * Three asks while the first page is still out, as a reader scrolling back and forth across the threshold sends
     * them. The one already running will deliver the same page, so the rest are dropped rather than queued.
     */
    @Test
    fun `a page already on its way is not asked for a second time`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316), nextPageToken = SECOND_PAGE_TOKEN)
            stubNextPage(listOf(psalm231), delayMillis = SLOW_RESPONSE_MILLIS)
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            viewModel.onAction(Action.LoadNextPage)
            viewModel.onAction(Action.LoadNextPage)
            viewModel.onAction(Action.LoadNextPage)
            advanceUntilIdle()

            assertEquals(listOf(john316, psalm231), viewModel.state.value.results)
            coVerify(exactly = 1) {
                searchApi.verses(query = any(), bibleId = any(), pageToken = SECOND_PAGE_TOKEN)
            }
        }

    @Test
    fun `a page in flight is announced until it lands`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316), nextPageToken = SECOND_PAGE_TOKEN)
            stubNextPage(listOf(psalm231), delayMillis = SLOW_RESPONSE_MILLIS)
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            viewModel.onAction(Action.LoadNextPage)
            advanceTimeBy(SLOW_RESPONSE_MILLIS / 2)
            assertTrue(viewModel.state.value.isLoadingNextPage)

            advanceUntilIdle()

            assertFalse(viewModel.state.value.isLoadingNextPage)
        }

    @Test
    fun `a failed page load offers a retry and leaves the results already listed alone`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316), nextPageToken = SECOND_PAGE_TOKEN)
            coEvery {
                searchApi.verses(query = any(), bibleId = any(), pageToken = SECOND_PAGE_TOKEN)
            } throws RuntimeException("offline")
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            loadNextPage()

            assertTrue(viewModel.state.value.hasNextPageLoadError)
            assertFalse(viewModel.state.value.isLoadingNextPage)
            assertEquals(listOf(john316), viewModel.state.value.results)
            assertEquals(SearchStatus.COMPLETED, viewModel.state.value.status)
        }

    @Test
    fun `asking again after a failed page load takes the retry away and lands the page`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316), nextPageToken = SECOND_PAGE_TOKEN)
            coEvery {
                searchApi.verses(query = any(), bibleId = any(), pageToken = SECOND_PAGE_TOKEN)
            } throws RuntimeException("offline") andThen verseSearchResults(listOf(psalm231))
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")
            loadNextPage()

            loadNextPage()

            assertEquals(listOf(john316, psalm231), viewModel.state.value.results)
            assertFalse(viewModel.state.value.hasNextPageLoadError)
        }

    @Test
    fun `the next page is not asked for before a search has finished`() =
        runTest(testDispatcher) {
            viewModel.onAction(Action.OpenSearch(kjv))
            viewModel.onAction(Action.SetQuery("love"))

            loadNextPage()

            coVerify(exactly = 0) { searchApi.verses(query = any(), bibleId = any(), pageToken = any()) }
        }

    @Test
    fun `a page arriving after the reader has typed on never lands under the new query`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316), nextPageToken = SECOND_PAGE_TOKEN)
            stubNextPage(listOf(psalm231), delayMillis = SLOW_RESPONSE_MILLIS)
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")

            viewModel.onAction(Action.LoadNextPage)
            advanceTimeBy(SLOW_RESPONSE_MILLIS / 2)
            viewModel.onAction(Action.SetQuery("loves"))
            advanceUntilIdle()

            assertEquals(emptyList(), viewModel.state.value.results)
            assertFalse(viewModel.state.value.isLoadingNextPage)
        }

    @Test
    fun `a new search leaves no page token, no retry and no rows behind`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316), nextPageToken = SECOND_PAGE_TOKEN)
            coEvery {
                searchApi.verses(query = any(), bibleId = any(), pageToken = SECOND_PAGE_TOKEN)
            } throws RuntimeException("offline")
            viewModel.onAction(Action.OpenSearch(kjv))
            submit("love")
            loadNextPage()

            viewModel.onAction(Action.SetQuery("loves"))

            assertNull(viewModel.state.value.nextPageToken)
            assertFalse(viewModel.state.value.hasNextPageLoadError)
            assertFalse(viewModel.state.value.isLoadingNextPage)
            assertEquals(emptyList(), viewModel.state.value.results)
        }

    @Test
    fun `opening the sheet offers what other readers are searching without waiting`() =
        runTest(testDispatcher) {
            stubTrending(listOf(love, peace))

            viewModel.onAction(Action.OpenSearch(kjv))
            runCurrent()

            assertEquals(listOf(love, peace), viewModel.state.value.suggestedQueries)
        }

    @Test
    fun `a typed query is not asked about until the reader has stopped typing`() =
        runTest(testDispatcher) {
            stubSuggested(listOf(love))
            viewModel.onAction(Action.OpenSearch(kjv))
            runCurrent()

            viewModel.onAction(Action.SetQuery("lov"))
            advanceTimeBy(SUGGESTION_DEBOUNCE_MILLIS - 1)

            coVerify(exactly = 0) { searchApi.suggestedQueries(query = any(), languageRanges = any()) }

            advanceTimeBy(1)
            runCurrent()

            assertEquals(listOf(love), viewModel.state.value.suggestedQueries)
        }

    @Test
    fun `each keystroke drops the ask the one before it started`() =
        runTest(testDispatcher) {
            stubSuggested(listOf(love))
            viewModel.onAction(Action.OpenSearch(kjv))
            runCurrent()

            viewModel.onAction(Action.SetQuery("l"))
            advanceTimeBy(SUGGESTION_DEBOUNCE_MILLIS / 3)
            viewModel.onAction(Action.SetQuery("lo"))
            advanceTimeBy(SUGGESTION_DEBOUNCE_MILLIS / 3)
            viewModel.onAction(Action.SetQuery("lov"))
            advanceUntilIdle()

            coVerify(exactly = 1) { searchApi.suggestedQueries(query = any(), languageRanges = any()) }
            coVerify(exactly = 1) { searchApi.suggestedQueries(query = "lov", languageRanges = any()) }
        }

    @Test
    fun `suggestions are asked for in the language of the version being read`() =
        runTest(testDispatcher) {
            viewModel.onAction(Action.OpenSearch(rvr))
            runCurrent()

            coVerify { searchApi.trendingQueries(languageRanges = listOf("es")) }
        }

    @Test
    fun `a version that declares no language of its own takes suggestions in any language`() =
        runTest(testDispatcher) {
            viewModel.onAction(Action.OpenSearch(kjv))
            runCurrent()

            coVerify { searchApi.trendingQueries(languageRanges = listOf("*")) }
        }

    @Test
    fun `suggestions are announced while they are being fetched`() =
        runTest(testDispatcher) {
            coEvery { searchApi.trendingQueries(languageRanges = any()) } coAnswers {
                delay(SLOW_RESPONSE_MILLIS)
                listOf(love)
            }

            viewModel.onAction(Action.OpenSearch(kjv))
            runCurrent()

            assertTrue(viewModel.state.value.isLoadingSuggestedQueries)

            advanceUntilIdle()

            assertFalse(viewModel.state.value.isLoadingSuggestedQueries)
        }

    @Test
    fun `a failed ask for suggestions leaves the reader nothing and reports no error`() =
        runTest(testDispatcher) {
            coEvery { searchApi.trendingQueries(languageRanges = any()) } throws RuntimeException("offline")

            viewModel.onAction(Action.OpenSearch(kjv))
            advanceUntilIdle()

            assertEquals(emptyList(), viewModel.state.value.suggestedQueries)
            assertFalse(viewModel.state.value.isLoadingSuggestedQueries)
            assertEquals(SearchStatus.IDLE, viewModel.state.value.status)
        }

    @Test
    fun `an ask for suggestions abandoned mid-flight is not reported as a failure`() =
        runTest(testDispatcher) {
            stubSearch(listOf(john316))
            coEvery { searchApi.suggestedQueries(query = any(), languageRanges = any()) } coAnswers {
                delay(SLOW_RESPONSE_MILLIS)
                listOf(love)
            }
            viewModel.onAction(Action.SetQuery("love"))
            advanceTimeBy(SUGGESTION_DEBOUNCE_MILLIS + SLOW_RESPONSE_MILLIS / 2)

            viewModel.onAction(Action.OpenSearch(kjv))
            advanceUntilIdle()

            assertEquals(emptyList(), viewModel.state.value.suggestedQueries)
            assertFalse(viewModel.state.value.isLoadingSuggestedQueries)
            assertEquals(SearchStatus.IDLE, viewModel.state.value.status)
        }

    @Test
    fun `suggestions are let go once a search is run`() =
        runTest(testDispatcher) {
            stubTrending(listOf(love, peace))
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            runCurrent()

            submit("love")

            assertEquals(emptyList(), viewModel.state.value.suggestedQueries)
        }

    @Test
    fun `a suggestion the reader taps is searched for`() =
        runTest(testDispatcher) {
            stubTrending(listOf(love, peace))
            stubSearch(listOf(john316))
            viewModel.onAction(Action.OpenSearch(kjv))
            runCurrent()

            viewModel.onAction(Action.SelectSuggestedQuery(love))
            advanceUntilIdle()

            assertEquals("love", viewModel.state.value.query)
            assertEquals(listOf(john316), viewModel.state.value.results)
            assertEquals(SearchStatus.COMPLETED, viewModel.state.value.status)
        }

    /** Types [query] and submits it, then drains whatever search that started. */
    private fun submit(query: String) {
        viewModel.onAction(Action.SetQuery(query))
        viewModel.onAction(Action.Submit)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    /** Asks for [reference]'s text, then drains the load that started. */
    private fun loadResultText(reference: BibleReference) {
        viewModel.onAction(Action.LoadResultText(reference))
        testDispatcher.scheduler.advanceUntilIdle()
    }

    /** Asks for the next page, then drains the load that started. */
    private fun loadNextPage() {
        viewModel.onAction(Action.LoadNextPage)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    /** Waits for [reference]'s text, which the real fetch path delivers off the test dispatcher. */
    private suspend fun awaitResultText(reference: BibleReference) {
        viewModel.state.first { it.resultTextByPassageId[reference.asUSFM] != null }
    }

    private fun stubResultText(text: String?) {
        mockkObject(BibleVersionRendering)
        coEvery { BibleVersionRendering.plainTextOf(any(), any()) } returns text
    }

    private fun stubTrending(queries: List<SearchQuery>) {
        coEvery { searchApi.trendingQueries(languageRanges = any()) } returns queries
    }

    private fun stubSuggested(queries: List<SearchQuery>) {
        coEvery { searchApi.suggestedQueries(query = any(), languageRanges = any()) } returns queries
    }

    private fun stubSearch(
        references: List<BibleReference>,
        nextPageToken: String? = null,
    ) {
        coEvery { searchApi.verses(query = any(), bibleId = any(), pageToken = null) } returns
            verseSearchResults(references, nextPageToken)
    }

    private fun stubNextPage(
        references: List<BibleReference>,
        nextPageToken: String? = null,
        delayMillis: Long = 0,
    ) {
        coEvery { searchApi.verses(query = any(), bibleId = any(), pageToken = SECOND_PAGE_TOKEN) } coAnswers {
            delay(delayMillis)
            verseSearchResults(references, nextPageToken)
        }
    }

    private fun verseSearchResults(
        references: List<BibleReference>,
        nextPageToken: String? = null,
    ) = VerseSearchResults(
        references = references,
        userIntent = null,
        didYouMean = emptyList(),
        searchInsteadFor = null,
        nextPageToken = nextPageToken,
    )

    private companion object {
        const val SLOW_RESPONSE_MILLIS = 1_000L

        /** The token a first page comes back carrying, which the second page is then asked for with. */
        const val SECOND_PAGE_TOKEN = "second-page"

        /** How long after the reader stops typing what they have entered is asked about. */
        const val SUGGESTION_DEBOUNCE_MILLIS = 300L

        val kjv = BibleVersion(id = 1, abbreviation = "KJV")
        val esv = BibleVersion(id = 2, abbreviation = "ESV")
        val rvr = BibleVersion(id = 3, abbreviation = "RVR", languageTag = "es")

        val love = SearchQuery(text = "love", source = null)
        val peace = SearchQuery(text = "peace", source = null)

        val john3 = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 3)
        val john31 = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 3, verse = 1)
        val john316 = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 3, verse = 16)
        val psalm231 = BibleReference(versionId = 1, bookUSFM = "PSA", chapter = 23, verse = 1)

        val john3Html =
            """
            <div>
                <div class="p">
                    <span class="yv-v" v="1"></span>
                    <span class="yv-vlbl">1</span>
                    There was a man of the Pharisees
                </div>
                <div class="p">
                    <span class="yv-v" v="16"></span>
                    <span class="yv-vlbl">16</span>
                    For God so loved the world
                </div>
            </div>
            """.trimIndent()
    }
}
