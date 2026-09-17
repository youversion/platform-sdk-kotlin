package com.youversion.platform.reader

import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.data.BibleVersionMemoryCache
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.search.api.SearchApi
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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

    /** Waits for [reference]'s text, which the real fetch path delivers off the test dispatcher. */
    private suspend fun awaitResultText(reference: BibleReference) {
        viewModel.state.first { it.resultTextByPassageId[reference.asUSFM] != null }
    }

    private fun stubResultText(text: String?) {
        mockkObject(BibleVersionRendering)
        coEvery { BibleVersionRendering.plainTextOf(any(), any()) } returns text
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
