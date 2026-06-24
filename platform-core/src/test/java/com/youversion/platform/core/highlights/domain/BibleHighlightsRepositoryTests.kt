package com.youversion.platform.core.highlights.domain

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.highlights.api.HighlightsApi
import com.youversion.platform.core.highlights.models.Highlight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BibleHighlightsRepositoryTests {
    private val testDispatcher = StandardTestDispatcher()

    private fun repository(api: HighlightsApi) =
        BibleHighlightsRepository(api = api, scope = CoroutineScope(testDispatcher)).also { it.reset() }

    @AfterTest
    fun teardown() = BibleHighlightCache.clear()

    @Test
    fun `optimistic add is reflected in the highlights state immediately`() =
        runTest(testDispatcher) {
            val repository = repository(FakeHighlightsApi())
            val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)

            repository.addHighlights(listOf(reference), color = "#ff00ff")

            assertEquals(1, repository.highlights.value.size)
            assertEquals("#ff00ff", repository.highlights(overlapping = reference).first().hexColor)
        }

    @Test
    fun `ensureHighlightsForChapterLoaded fetches and converts server highlights`() =
        runTest(testDispatcher) {
            val api =
                FakeHighlightsApi(
                    highlightsToReturn =
                        listOf(Highlight(versionId = 1, passageId = "GEN.1.1", color = "ff0000")),
                )
            val repository = repository(api)

            repository.ensureHighlightsForChapterLoaded(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1))
            advanceUntilIdle()

            assertEquals(1, api.highlightsCount)
            val highlights =
                repository.highlights(
                    overlapping = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1),
                )
            assertEquals(1, highlights.size)
            assertEquals("#ff0000", highlights.first().hexColor)
            assertEquals(1, highlights.first().bibleReference.verseStart)
        }

    @Test
    fun `ensureHighlightsForChapterLoaded throttles repeated loads of the same chapter`() =
        runTest(testDispatcher) {
            val api = FakeHighlightsApi()
            val repository = repository(api)
            val chapter = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)

            repository.ensureHighlightsForChapterLoaded(chapter)
            advanceUntilIdle()
            repository.ensureHighlightsForChapterLoaded(chapter)
            advanceUntilIdle()

            assertEquals(1, api.highlightsCount)
        }

    @Test
    fun `addHighlights syncs a create to the server with verse-level passage and bare hex`() =
        runTest(testDispatcher) {
            val api = FakeHighlightsApi()
            val repository = repository(api)

            repository.addHighlights(
                listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
                color = "#FF00FF",
            )
            advanceUntilIdle()

            assertEquals(1, api.createCount)
            assertEquals("GEN.1.1", api.createdPassages.first())
            assertEquals("ff00ff", api.createdColors.first())
        }

    @Test
    fun `queue retries a failing operation until it succeeds`() =
        runTest(testDispatcher) {
            val api = FakeHighlightsApi(failuresBeforeSuccess = 2)
            val repository = repository(api)

            repository.addHighlights(
                listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
                color = "#ff00ff",
            )
            advanceUntilIdle()

            assertEquals(3, api.createCount)
        }
}

private class FakeHighlightsApi(
    private val highlightsToReturn: List<Highlight> = emptyList(),
    failuresBeforeSuccess: Int = 0,
) : HighlightsApi {
    var createCount = 0
    var updateCount = 0
    var deleteCount = 0
    var highlightsCount = 0
    val createdPassages = mutableListOf<String>()
    val createdColors = mutableListOf<String>()

    private var remainingFailures = failuresBeforeSuccess

    override suspend fun createHighlight(
        versionId: Int,
        passageId: String,
        color: String,
    ): Boolean {
        createCount++
        createdPassages.add(passageId)
        createdColors.add(color)
        if (remainingFailures > 0) {
            remainingFailures--
            return false
        }
        return true
    }

    override suspend fun highlights(
        versionId: Int,
        passageId: String,
    ): List<Highlight> {
        highlightsCount++
        return highlightsToReturn
    }

    override suspend fun updateHighlight(
        versionId: Int,
        passageId: String,
        color: String,
    ): Boolean {
        updateCount++
        return true
    }

    override suspend fun deleteHighlight(
        versionId: Int,
        passageId: String,
    ): Boolean {
        deleteCount++
        return true
    }
}
