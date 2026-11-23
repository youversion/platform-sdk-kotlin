package com.youversion.platform.ui.content

import androidx.lifecycle.SavedStateHandle
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformInternalApi
import com.youversion.platform.core.verseimages.models.VerseImage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, PlatformInternalApi::class)
class ContentImagesViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val detailCalls = mutableMapOf<String, Int>()
    private val searchCalls = mutableListOf<Pair<String, String?>>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun image(id: String) =
        VerseImage(
            id = id,
            imageUrl = "https://example.com/$id.png",
            passageId = "JHN.3.16",
            attribution = "Art",
            languageTag = "en",
            width = 100,
            height = 100,
            aspectRatio = 1.0,
        )

    private fun viewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        browsePage: suspend (String?) -> Pair<List<VerseImage>, String?> = { emptyList<VerseImage>() to null },
        searchPage: suspend (String, String?) -> Pair<List<String>, String?> = { _, _ -> emptyList<String>() to null },
        failingDetailIds: Set<String> = emptySet(),
        bibleVersion: suspend () -> BibleVersion = { awaitCancellation() },
    ) = ContentImagesViewModel(
        savedStateHandle = savedStateHandle,
        browsePage = browsePage,
        searchPage = { query, pageToken ->
            searchCalls += query to pageToken
            searchPage(query, pageToken)
        },
        imageDetail = { id ->
            val count = (detailCalls[id] ?: 0) + 1
            detailCalls[id] = count
            if (id in failingDetailIds && count == 1) error("$id failed")
            image(id)
        },
        bibleVersion = bibleVersion,
    )

    private fun ContentImagesViewModel.imageIds() = state.value.images.map { it.id }

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest(testDispatcher) { block() }

    @Test
    fun `a repeated image on a later page is shown once, in its first position`() =
        runViewModelTest {
            val viewModel =
                viewModel(browsePage = { token ->
                    if (token ==
                        null
                    ) {
                        listOf(image("A"), image("B"), image("C")) to "p2"
                    } else {
                        listOf(image("C"), image("D")) to
                            null
                    }
                })
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()

            assertEquals(listOf("A", "B", "C", "D"), viewModel.imageIds())
            assertFalse(viewModel.state.value.hasMorePages)
        }

    @Test
    fun `search hits keep server order and retry fetches only the hits that failed`() =
        runViewModelTest {
            val viewModel =
                viewModel(
                    searchPage = { _, _ -> listOf("J", "A", "X", "B", "A") to null },
                    failingDetailIds = setOf("X"),
                )
            advanceUntilIdle()
            viewModel.search("love")
            advanceUntilIdle()

            assertEquals(listOf("J", "A", "B"), viewModel.imageIds())
            assertTrue(viewModel.state.value.hasError)

            viewModel.retry()
            advanceUntilIdle()

            assertEquals(listOf("J", "A", "X", "B"), viewModel.imageIds())
            assertFalse(viewModel.state.value.hasError)
            assertEquals(mapOf("J" to 1, "A" to 1, "X" to 2, "B" to 1), detailCalls.toMap())
            assertEquals(1, searchCalls.size)
        }

    @Test
    fun `a slow earlier search that finishes after a newer one is discarded`() =
        runViewModelTest {
            val slowPeace = CompletableDeferred<Pair<List<String>, String?>>()
            val viewModel =
                viewModel(searchPage = { query, _ ->
                    if (query == "peace") slowPeace.await() else listOf("H") to null
                })
            advanceUntilIdle()
            viewModel.search("peace")
            advanceUntilIdle()
            viewModel.search("hope")
            advanceUntilIdle()
            slowPeace.complete(listOf("P") to null)
            advanceUntilIdle()

            assertEquals("hope", viewModel.state.value.query)
            assertEquals(listOf("H"), viewModel.imageIds())
            assertFalse("P" in detailCalls)
        }

    @Test
    fun `an empty search page with a next token still offers more pages`() =
        runViewModelTest {
            val viewModel =
                viewModel(searchPage = { _, token ->
                    if (token ==
                        null
                    ) {
                        emptyList<String>() to "e2"
                    } else {
                        listOf("B") to null
                    }
                })
            advanceUntilIdle()
            viewModel.search("empty")
            advanceUntilIdle()

            assertTrue(viewModel.state.value.hasMorePages)
            assertEquals(emptyList<String>(), viewModel.imageIds())

            viewModel.loadMore()
            advanceUntilIdle()

            assertEquals(listOf("B"), viewModel.imageIds())
            assertEquals("e2", searchCalls.last().second)
        }

    @Test
    fun `the search limit counts characters as readers see them`() =
        runViewModelTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            val hundredAccented = "e\u0301".repeat(100)
            viewModel.search(hundredAccented)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isQueryTooLong)
            assertEquals(hundredAccented, searchCalls.last().first)

            viewModel.search("\uD83D\uDE00".repeat(101))
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isQueryTooLong)
            assertEquals(1, searchCalls.size)
        }

    @Test
    fun `a rejected search leaves the running page loading`() =
        runViewModelTest {
            val firstPage = CompletableDeferred<Pair<List<VerseImage>, String?>>()
            val viewModel = viewModel(browsePage = { firstPage.await() })
            advanceUntilIdle()

            viewModel.search("a".repeat(101))
            firstPage.complete(listOf(image("A")) to null)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.isQueryTooLong)
            assertFalse(viewModel.state.value.isLoading)
            assertEquals(listOf("A"), viewModel.imageIds())
        }

    @Test
    fun `the submitted search is saved and restored`() =
        runViewModelTest {
            val savedStateHandle = SavedStateHandle()
            viewModel(savedStateHandle = savedStateHandle).search("  love ")
            advanceUntilIdle()

            val restored =
                viewModel(
                    savedStateHandle =
                        SavedStateHandle(
                            mapOf("query" to savedStateHandle.get<String>("query")),
                        ),
                )
            advanceUntilIdle()

            assertEquals("love", restored.state.value.query)
            assertEquals(listOf<Pair<String, String?>>("love" to null, "love" to null), searchCalls)
        }

    @Test
    fun `an image that is not loaded is fetched once by id`() =
        runViewModelTest {
            val viewModel = viewModel(browsePage = { listOf(image("A")) to null })
            advanceUntilIdle()

            assertEquals("A", viewModel.image("A").id)
            assertEquals("Z", viewModel.image("Z").id)
            assertEquals("Z", viewModel.image("Z").id)
            assertEquals(mapOf("Z" to 1), detailCalls.toMap())
        }

    @Test
    fun `a failed Bible version is shown and loaded again on retry`() =
        runViewModelTest {
            var versionCalls = 0
            val viewModel =
                viewModel(bibleVersion = {
                    versionCalls++
                    if (versionCalls == 1) error("version failed")
                    BibleVersion(id = 3034, abbreviation = "BSB", languageTag = "en")
                })
            advanceUntilIdle()

            assertTrue(viewModel.state.value.hasVersionError)

            viewModel.retry()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.hasVersionError)
            assertEquals(
                3034,
                viewModel.state.value.bibleVersion
                    ?.id,
            )
            assertEquals(2, versionCalls)
        }
}
