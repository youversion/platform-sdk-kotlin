package com.youversion.platform.core.highlights.domain

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.highlights.api.HighlightsApi
import com.youversion.platform.core.highlights.models.Highlight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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

    @Test
    fun `updateHighlightColors syncs a recolor of an existing highlight with verse-level passage and bare hex`() =
        runTest(testDispatcher) {
            val api =
                FakeHighlightsApi(
                    highlightsToReturn =
                        listOf(Highlight(versionId = 1, passageId = "GEN.1.1", color = "ff0000")),
                )
            val repository = repository(api)
            val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
            repository.ensureHighlightsForChapterLoaded(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1))
            advanceUntilIdle()

            repository.updateHighlightColors(listOf(reference), newColor = "#FF00FF")
            advanceUntilIdle()

            assertEquals(1, api.updateCount)
            assertEquals(0, api.createCount)
            assertEquals("GEN.1.1", api.updatedPassages.first())
            assertEquals("ff00ff", api.updatedColors.first())
        }

    @Test
    fun `updateHighlightColors syncs a create when no highlight exists for the reference yet`() =
        runTest(testDispatcher) {
            val api = FakeHighlightsApi()
            val repository = repository(api)

            repository.updateHighlightColors(
                listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
                newColor = "#FF00FF",
            )
            advanceUntilIdle()

            assertEquals(1, api.createCount)
            assertEquals(0, api.updateCount)
            assertEquals("GEN.1.1", api.createdPassages.first())
            assertEquals("ff00ff", api.createdColors.first())
        }

    @Test
    fun `removeHighlights syncs a delete to the server with verse-level passage`() =
        runTest(testDispatcher) {
            val api = FakeHighlightsApi()
            val repository = repository(api)

            repository.removeHighlights(
                listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
            )
            advanceUntilIdle()

            assertEquals(1, api.deleteCount)
            assertEquals("GEN.1.1", api.deletedPassages.first())
        }

    @Test
    fun `queue retries a failing update until it succeeds`() =
        runTest(testDispatcher) {
            val api =
                FakeHighlightsApi(
                    highlightsToReturn =
                        listOf(Highlight(versionId = 1, passageId = "GEN.1.1", color = "ff0000")),
                    failuresBeforeSuccess = 2,
                )
            val repository = repository(api)
            val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
            repository.ensureHighlightsForChapterLoaded(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1))
            advanceUntilIdle()

            repository.updateHighlightColors(listOf(reference), newColor = "#ff00ff")
            advanceUntilIdle()

            assertEquals(3, api.updateCount)
            assertEquals(0, repository.pendingOperationCount.value)
        }

    @Test
    fun `queue retries a failing remove until it succeeds`() =
        runTest(testDispatcher) {
            val api = FakeHighlightsApi(failuresBeforeSuccess = 2)
            val repository = repository(api)

            repository.removeHighlights(
                listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
            )
            advanceUntilIdle()

            assertEquals(3, api.deleteCount)
            assertEquals(0, repository.pendingOperationCount.value)
        }

    @Test
    fun `operation counts track a write that fails and then drains`() =
        runTest(testDispatcher) {
            val api = FakeHighlightsApi(failuresBeforeSuccess = 1)
            val repository = repository(api)

            repository.addHighlights(
                listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
                color = "#ff00ff",
            )
            runCurrent()

            assertEquals(1, repository.pendingOperationCount.value)
            assertEquals(1, repository.failedOperationCount.value)

            advanceUntilIdle()

            assertEquals(0, repository.pendingOperationCount.value)
            assertEquals(0, repository.failedOperationCount.value)
            assertEquals(2, api.createCount)
        }

    @Test
    fun `a write retries indefinitely until it succeeds, past the old retry cap`() =
        runTest(testDispatcher) {
            val api = FakeHighlightsApi(failuresBeforeSuccess = 6)
            val repository = repository(api)

            repository.addHighlights(
                listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
                color = "#ff00ff",
            )
            advanceUntilIdle()

            assertEquals(7, api.createCount)
            assertEquals(0, repository.pendingOperationCount.value)
        }

    @Test
    fun `clearOperationResults resets the failed operation count`() =
        runTest(testDispatcher) {
            val api = FakeHighlightsApi(failuresBeforeSuccess = 1)
            val repository = repository(api)

            repository.addHighlights(
                listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
                color = "#ff00ff",
            )
            runCurrent()
            assertEquals(1, repository.failedOperationCount.value)

            repository.clearOperationResults()
            runCurrent()
            assertEquals(0, repository.failedOperationCount.value)

            advanceUntilIdle()
            assertEquals(2, api.createCount)
        }

    @Test
    fun `flushPendingWrites suspends until queued writes are sent`() =
        runTest(testDispatcher) {
            val api = FakeHighlightsApi(failuresBeforeSuccess = 1)
            val repository = repository(api)

            repository.addHighlights(
                listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
                color = "#ff00ff",
            )
            runCurrent()
            assertEquals(1, api.createCount)

            repository.flushPendingWrites()

            assertEquals(2, api.createCount)
            assertEquals(0, repository.pendingOperationCount.value)
        }

    @Test
    fun `a write is not sent when the account changes before it syncs`() =
        runTest(testDispatcher) {
            val api = FakeHighlightsApi()
            var accountId: String? = "account-a"
            val repository =
                BibleHighlightsRepository(
                    api = api,
                    scope = CoroutineScope(testDispatcher),
                    currentAccountId = { accountId },
                ).also { BibleHighlightCache.clear() }

            repository.addHighlights(
                listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
                color = "#ff00ff",
            )
            accountId = "account-b"
            advanceUntilIdle()

            assertEquals(0, api.createCount)
            assertEquals(0, repository.pendingOperationCount.value)
        }

    @Test
    fun `a write still syncs across a token refresh that keeps the same account`() =
        runTest(testDispatcher) {
            val api = FakeHighlightsApi()
            val repository =
                BibleHighlightsRepository(
                    api = api,
                    scope = CoroutineScope(testDispatcher),
                    currentAccountId = { "account-a" },
                ).also { BibleHighlightCache.clear() }

            repository.addHighlights(
                listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
                color = "#ff00ff",
            )
            advanceUntilIdle()

            assertEquals(1, api.createCount)
        }

    @Test
    fun `reset cancels an in-flight load so it cannot repopulate the cleared cache`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val api =
                FakeHighlightsApi(
                    highlightsToReturn =
                        listOf(Highlight(versionId = 1, passageId = "GEN.1.1", color = "ff0000")),
                    highlightsGate = gate,
                )
            val repository = repository(api)
            val chapter = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)

            repository.ensureHighlightsForChapterLoaded(chapter)
            runCurrent()
            assertEquals(1, api.highlightsCount)

            repository.reset()
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(0, repository.highlights(overlapping = chapter).size)
        }
}

private class FakeHighlightsApi(
    private val highlightsToReturn: List<Highlight> = emptyList(),
    failuresBeforeSuccess: Int = 0,
    private val highlightsGate: CompletableDeferred<Unit>? = null,
) : HighlightsApi {
    var createCount = 0
    var updateCount = 0
    var deleteCount = 0
    var highlightsCount = 0
    val createdPassages = mutableListOf<String>()
    val createdColors = mutableListOf<String>()
    val updatedPassages = mutableListOf<String>()
    val updatedColors = mutableListOf<String>()
    val deletedPassages = mutableListOf<String>()

    private var remainingFailures = failuresBeforeSuccess

    private fun nextResult(): Boolean {
        if (remainingFailures > 0) {
            remainingFailures--
            return false
        }
        return true
    }

    override suspend fun createHighlight(
        versionId: Int,
        passageId: String,
        color: String,
    ): Boolean {
        createCount++
        createdPassages.add(passageId)
        createdColors.add(color)
        return nextResult()
    }

    override suspend fun highlights(
        versionId: Int,
        passageId: String,
    ): List<Highlight> {
        highlightsCount++
        highlightsGate?.await()
        return highlightsToReturn
    }

    override suspend fun updateHighlight(
        versionId: Int,
        passageId: String,
        color: String,
    ): Boolean {
        updateCount++
        updatedPassages.add(passageId)
        updatedColors.add(color)
        return nextResult()
    }

    override suspend fun deleteHighlight(
        versionId: Int,
        passageId: String,
    ): Boolean {
        deleteCount++
        deletedPassages.add(passageId)
        return nextResult()
    }
}
