package com.youversion.platform.reader

import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.search.api.SearchApi
import com.youversion.platform.core.search.models.VerseSearchResults
import com.youversion.platform.reader.BibleReaderSearchViewModel.Action
import com.youversion.platform.reader.BibleReaderSearchViewModel.SearchStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BibleReaderSearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val searchApi = mockk<SearchApi>()
    private val viewModel = BibleReaderSearchViewModel()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(YouVersionApi)
        every { YouVersionApi.search } returns searchApi
    }

    @AfterTest
    fun teardown() {
        testDispatcher.scheduler.advanceUntilIdle()
        unmockkObject(YouVersionApi)
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

    /** Types [query] and submits it, then drains whatever search that started. */
    private fun submit(query: String) {
        viewModel.onAction(Action.SetQuery(query))
        viewModel.onAction(Action.Submit)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun stubSearch(references: List<BibleReference>) {
        coEvery { searchApi.verses(query = any(), bibleId = any()) } returns verseSearchResults(references)
    }

    private fun verseSearchResults(references: List<BibleReference>) =
        VerseSearchResults(
            references = references,
            userIntent = null,
            didYouMean = emptyList(),
            searchInsteadFor = null,
            nextPageToken = null,
        )

    private companion object {
        const val SLOW_RESPONSE_MILLIS = 1_000L

        val kjv = BibleVersion(id = 1, abbreviation = "KJV")
        val esv = BibleVersion(id = 2, abbreviation = "ESV")

        val john316 = BibleReference(versionId = 1, bookUSFM = "JHN", chapter = 3, verse = 16)
        val psalm231 = BibleReference(versionId = 1, bookUSFM = "PSA", chapter = 23, verse = 1)
    }
}
