package com.youversion.platform.core.highlights.domain

import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.highlights.api.HighlightsApi
import com.youversion.platform.core.highlights.models.Highlight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun `a synced add is not duplicated when the chapter is later fetched from the server`() =
        runTest(testDispatcher) {
            val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
            val api =
                FakeHighlightsApi(
                    highlightsToReturn =
                        listOf(Highlight(versionId = 1, passageId = "GEN.1.1", color = "ff00ff")),
                )
            val repository = repository(api)

            repository.addHighlights(listOf(reference), color = "#ff00ff")
            advanceUntilIdle()

            repository.ensureHighlightsForChapterLoaded(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1))
            advanceUntilIdle()

            assertEquals(1, repository.highlights(overlapping = reference).size)
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
                color = "#ff00ff",
            )
            advanceUntilIdle()

            assertEquals(1, api.createCount)
            assertEquals("GEN.1.1", api.createdPassages.first())
            assertEquals("ff00ff", api.createdColors.first())
        }

    @Test
    fun `an add with no verse start syncs once and a later recolor updates rather than firing a second create`() =
        runTest(testDispatcher) {
            // A caller-supplied reference with a null verseStart is normalized to verse 1 for the cache; the queued
            // operation must be normalized the same way, or the sync-completion lookup misses and the entry stays a
            // pending create, so the recolor fires a duplicate POST instead of a PUT.
            val api = FakeHighlightsApi()
            val repository = repository(api)
            val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1)

            repository.addHighlights(listOf(reference), color = "#ff0000")
            advanceUntilIdle()
            assertEquals(1, api.createCount)

            repository.updateHighlightColors(listOf(reference), newColor = "#00ff00")
            advanceUntilIdle()

            assertEquals(1, api.createCount)
            assertEquals(1, api.updateCount)
            assertEquals(1, repository.highlights.value.size)
            assertEquals(0, repository.pendingOperationCount.value)
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

            repository.updateHighlightColors(listOf(reference), newColor = "#ff00ff")
            advanceUntilIdle()

            assertEquals(1, api.updateCount)
            assertEquals(0, api.createCount)
            assertEquals("GEN.1.1", api.updatedPassages.first())
            assertEquals("ff00ff", api.updatedColors.first())
        }

    @Test
    fun `an add that fails while a recolor creates the highlight retries as an update`() =
        runTest(testDispatcher) {
            // The first server call (the add's create) fails; the recolor queued in the same batch then creates the
            // highlight, so the add's retry must sync as an update rather than firing a second create.
            val api = FakeHighlightsApi(failuresBeforeSuccess = 1)
            val repository = repository(api)
            val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)

            repository.addHighlights(listOf(reference), color = "#ff0000")
            repository.updateHighlightColors(listOf(reference), newColor = "#00ff00")
            advanceUntilIdle()

            assertEquals(2, api.createCount)
            assertEquals(1, api.updateCount)
            assertEquals(0, repository.pendingOperationCount.value)
        }

    @Test
    fun `updateHighlightColors syncs a create when no highlight exists for the reference yet`() =
        runTest(testDispatcher) {
            val api = FakeHighlightsApi()
            val repository = repository(api)

            repository.updateHighlightColors(
                listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
                newColor = "#ff00ff",
            )
            advanceUntilIdle()

            assertEquals(1, api.createCount)
            assertEquals(0, api.updateCount)
            assertEquals("GEN.1.1", api.createdPassages.first())
            assertEquals("ff00ff", api.createdColors.first())
        }

    @Test
    fun `recolor during an in-flight chapter load syncs as an update once the server highlight arrives`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val api =
                FakeHighlightsApi(
                    highlightsToReturn =
                        listOf(Highlight(versionId = 1, passageId = "GEN.1.1", color = "ff0000")),
                    highlightsGate = gate,
                )
            val repository = repository(api)
            val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)

            repository.ensureHighlightsForChapterLoaded(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1))
            runCurrent()

            repository.updateHighlightColors(listOf(reference), newColor = "#00ff00")
            runCurrent()
            assertEquals(0, api.createCount)
            assertEquals(0, api.updateCount)
            // The write is in flight, suspended awaiting the chapter load rather than being sent or retried. It must
            // not be reported as failed: waiting on a load is not a failure and must not accrue retry backoff.
            assertEquals(0, repository.failedOperationCount.value)

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(1, api.updateCount)
            assertEquals(0, api.createCount)
            assertEquals(0, repository.pendingOperationCount.value)
            assertEquals(1, repository.highlights(overlapping = reference).size)
            assertEquals(1, repository.highlights.value.size)
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
    fun `a concurrent chapter load does not re-add a highlight whose delete is in flight`() =
        runTest(testDispatcher) {
            val deleteGate = CompletableDeferred<Unit>()
            val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
            val api =
                FakeHighlightsApi(
                    highlightsToReturn =
                        listOf(Highlight(versionId = 1, passageId = "GEN.1.1", color = "ff0000")),
                    deleteGate = deleteGate,
                )
            val repository = repository(api)

            // Park the delete in flight on its gate, then load the chapter whose server copy still holds the highlight.
            repository.removeHighlights(listOf(reference))
            runCurrent()

            repository.ensureHighlightsForChapterLoaded(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1))
            advanceUntilIdle()
            // The tombstone blocks the merge, so the highlight is never resurrected while the delete is outstanding.
            assertEquals(0, repository.highlights.value.size)

            deleteGate.complete(Unit)
            advanceUntilIdle()

            assertEquals(1, api.deleteCount)
            assertEquals(0, repository.highlights.value.size)
        }

    @Test
    fun `a queued delete is not re-added by a concurrent chapter load`() =
        runTest(testDispatcher) {
            val blockingGate = CompletableDeferred<Unit>()
            val target = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
            val api =
                FakeHighlightsApi(
                    highlightsToReturn =
                        listOf(Highlight(versionId = 1, passageId = "GEN.1.1", color = "ff0000")),
                    deleteGate = blockingGate,
                )
            val repository = repository(api)

            // Park the processor on a gated delete so the target's delete stays queued rather than in flight.
            repository.removeHighlights(listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2, verse = 1)))
            runCurrent()
            repository.removeHighlights(listOf(target))
            runCurrent()

            // A load arrives while the target's delete is still queued; the server copy must not be re-added.
            repository.ensureHighlightsForChapterLoaded(
                BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1),
                forceReload = true,
            )
            advanceUntilIdle()
            assertEquals(0, repository.highlights(overlapping = target).size)

            blockingGate.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun `a delete tombstones synchronously so a load merging in the same turn cannot resurrect it`() =
        runTest(testDispatcher) {
            // Regression: removeHighlights writes the delete's tombstone into the cache synchronously, and a chapter
            // load's merge consults that tombstone to skip the server copy. A load that merges in the same turn as the
            // delete must see the tombstone; this pins that it is written synchronously rather than after a dispatch.
            val loadGate = CompletableDeferred<Unit>()
            val deleteGate = CompletableDeferred<Unit>()
            val target = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
            val api =
                FakeHighlightsApi(
                    highlightsToReturn =
                        listOf(Highlight(versionId = 1, passageId = "GEN.1.1", color = "ff0000")),
                    highlightsGate = loadGate,
                    deleteGate = deleteGate,
                )
            val repository = repository(api)

            // Park a chapter load after it has fetched the server copy but before it merges.
            repository.ensureHighlightsForChapterLoaded(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1))
            runCurrent()

            // Release the load first, then delete: the load's merge runs before the delete is confirmed by the server,
            // so only the synchronous cache tombstone can keep the load from resurrecting target.
            loadGate.complete(Unit)
            repository.removeHighlights(listOf(target))
            runCurrent()

            assertEquals(0, repository.highlights(overlapping = target).size)

            deleteGate.complete(Unit)
            advanceUntilIdle()

            assertEquals(1, api.deleteCount)
            assertEquals(0, repository.highlights(overlapping = target).size)
        }

    @Test
    fun `a delete is not resurrected by a stale load that lands after the delete synced`() =
        runTest(testDispatcher) {
            // Regression: a chapter GET issued before the delete reached the server can still return the highlight, and
            // its response can land after the delete has fully synced and left the queue. Nothing queue-based remains to
            // filter it; only the delete tombstone, kept until a load that started after the delete synced confirms the
            // removal, stops the stale response from resurrecting the highlight.
            val loadGate = CompletableDeferred<Unit>()
            val target = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)
            val api =
                FakeHighlightsApi(
                    highlightsToReturn =
                        listOf(Highlight(versionId = 1, passageId = "GEN.1.1", color = "ff0000")),
                    highlightsGate = loadGate,
                )
            val repository = repository(api)

            // Start a chapter load and park it after it has fetched the stale server copy but before it merges.
            repository.ensureHighlightsForChapterLoaded(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1))
            runCurrent()

            // Delete the highlight and let the delete fully sync and drain from the queue while the load is parked.
            repository.removeHighlights(listOf(target))
            advanceUntilIdle()
            assertEquals(1, api.deleteCount)
            assertEquals(0, repository.pendingOperationCount.value)

            // The stale response now lands. The queue no longer holds the delete, so the tombstone is the only guard.
            loadGate.complete(Unit)
            advanceUntilIdle()

            assertEquals(0, repository.highlights(overlapping = target).size)
            assertEquals(0, repository.highlights.value.size)
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
    fun `flushPendingWrites returns instead of hanging when the scope has been cancelled`() =
        runTest(testDispatcher) {
            val deleteGate = CompletableDeferred<Unit>()
            val api = FakeHighlightsApi(deleteGate = deleteGate)
            val repoScope = CoroutineScope(testDispatcher)
            val repository = BibleHighlightsRepository(api = api, scope = repoScope).also { it.reset() }

            // Park the processor on the first write (gated delete), then queue a second so an operation stays pending.
            repository.removeHighlights(listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)))
            runCurrent()
            repository.removeHighlights(listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 2, verse = 1)))
            runCurrent()
            assertEquals(1, repository.pendingOperationCount.value)

            repoScope.cancel()
            advanceUntilIdle()

            // A cancelled scope can never run the processor, so without the inactive-scope guard this call would spin
            // forever joining a dead job. Reaching the assertion at all proves it returned.
            repository.flushPendingWrites()

            assertEquals(1, repository.pendingOperationCount.value)
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
    fun `a write is dropped when the account changes while its chapter load is in flight`() =
        runTest(testDispatcher) {
            val loadGate = CompletableDeferred<Unit>()
            var accountId: String? = "account-a"
            val api =
                FakeHighlightsApi(
                    highlightsToReturn =
                        listOf(Highlight(versionId = 1, passageId = "GEN.1.1", color = "ff0000")),
                    highlightsGate = loadGate,
                )
            val repository =
                BibleHighlightsRepository(
                    api = api,
                    scope = CoroutineScope(testDispatcher),
                    currentAccountId = { accountId },
                ).also { BibleHighlightCache.clear() }
            val reference = BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)

            // Start a chapter load and park it, then queue a recolor that must wait for the load before it classifies.
            repository.ensureHighlightsForChapterLoaded(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1))
            runCurrent()
            repository.updateHighlightColors(listOf(reference), newColor = "#00ff00")
            runCurrent()

            // The account switches while the write is parked on the in-flight load; it must not be sent under account-b.
            accountId = "account-b"
            loadGate.complete(Unit)
            advanceUntilIdle()

            assertEquals(0, api.createCount)
            assertEquals(0, api.updateCount)
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
    fun `a change in the signed-in account clears the cached highlights`() =
        runTest(testDispatcher) {
            val accountIdChanges = MutableStateFlow<String?>("account-a")
            val repository =
                BibleHighlightsRepository(
                    api = FakeHighlightsApi(),
                    scope = CoroutineScope(testDispatcher),
                    currentAccountId = { accountIdChanges.value },
                    accountIdChanges = accountIdChanges,
                ).also { BibleHighlightCache.clear() }

            repository.addHighlights(
                listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
                color = "#ff00ff",
            )
            runCurrent()
            assertEquals(1, repository.highlights.value.size)

            accountIdChanges.value = "account-b"
            advanceUntilIdle()

            assertEquals(0, repository.highlights.value.size)
        }

    @Test
    fun `a repeated signal for the same account leaves the cached highlights intact`() =
        runTest(testDispatcher) {
            val accountIdChanges = MutableSharedFlow<String?>(replay = 1)
            accountIdChanges.tryEmit("account-a")
            val repository =
                BibleHighlightsRepository(
                    api = FakeHighlightsApi(),
                    scope = CoroutineScope(testDispatcher),
                    currentAccountId = { "account-a" },
                    accountIdChanges = accountIdChanges,
                ).also { BibleHighlightCache.clear() }

            repository.addHighlights(
                listOf(BibleReference(versionId = 1, bookUSFM = "GEN", chapter = 1, verse = 1)),
                color = "#ff00ff",
            )
            runCurrent()
            assertEquals(1, repository.highlights.value.size)

            // A token refresh re-emits the same account; the cache must survive rather than being wiped each refresh.
            accountIdChanges.tryEmit("account-a")
            advanceUntilIdle()

            assertEquals(1, repository.highlights.value.size)
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
    private val deleteGate: CompletableDeferred<Unit>? = null,
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
        deleteGate?.await()
        return nextResult()
    }
}
