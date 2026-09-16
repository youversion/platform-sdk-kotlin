package com.youversion.platform.core.bibles.domain

import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BibleIntroRepositoryTests : YouVersionPlatformTest {
    private val repository = BibleIntroRepository()

    @AfterTest
    fun teardown() {
        stopYouVersionPlatformTest()
    }

    @Test
    fun `test introContent fetches from network when not cached`() =
        runTest {
            MockEngine {
                respondJson(
                    """
                    {
                        "id": "GEN.INTRO",
                        "content": "<html>intro</html>",
                        "reference": "Genesis Intro"
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            val result = repository.introContent(206, "GEN.INTRO")
            assertEquals("<html>intro</html>", result)
        }

    @OptIn(ExperimentalAtomicApi::class)
    @Test
    fun `test introContent returns cached content on second call`() =
        runTest {
            val count = AtomicInt(0)

            MockEngine {
                count.incrementAndFetch()
                respondJson(
                    """
                    {
                        "id": "GEN.INTRO",
                        "content": "<html>intro</html>",
                        "reference": "Genesis Intro"
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            repository.introContent(206, "GEN.INTRO")
            repository.introContent(206, "GEN.INTRO")

            assertEquals(1, count.load())
        }

    @OptIn(ExperimentalAtomicApi::class)
    @Test
    fun `test introContent refetches after cached content expires`() =
        runTest {
            val count = AtomicInt(0)

            MockEngine {
                count.incrementAndFetch()
                respondJson(
                    """
                    {
                        "id": "GEN.INTRO",
                        "content": "<html>intro</html>",
                        "reference": "Genesis Intro"
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            var currentTime = System.currentTimeMillis()
            val repository = BibleIntroRepository(now = { currentTime })

            repository.introContent(206, "GEN.INTRO")
            repository.introContent(206, "GEN.INTRO")
            assertEquals(1, count.load())

            currentTime += 8L * 24 * 60 * 60 * 1000
            repository.introContent(206, "GEN.INTRO")
            assertEquals(2, count.load())
        }

    @OptIn(ExperimentalAtomicApi::class, ExperimentalCoroutinesApi::class)
    @Test
    fun `test concurrent calls deduplicate into a single network request`() =
        runTest {
            val count = AtomicInt(0)

            MockEngine {
                count.incrementAndFetch()
                respondJson(
                    """
                    {
                        "id": "GEN.INTRO",
                        "content": "<html>intro</html>",
                        "reference": "Genesis Intro"
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            joinAll(
                launch { repository.introContent(206, "GEN.INTRO") },
                launch { repository.introContent(206, "GEN.INTRO") },
                launch { repository.introContent(206, "GEN.INTRO") },
                launch { repository.introContent(206, "GEN.INTRO") },
                launch { repository.introContent(206, "GEN.INTRO") },
                launch { repository.introContent(206, "GEN.INTRO") },
            )

            assertEquals(1, count.load())
        }

    /**
     * The second caller coalesces onto the first caller's in-flight fetch, then the first caller is cancelled.
     * The first request never responds, so the only way the second caller can reach a result is by re-driving
     * the fetch itself once it sees a cancellation that is not its own.
     */
    @OptIn(ExperimentalAtomicApi::class, ExperimentalCoroutinesApi::class)
    @Test
    fun `test cancelling one caller does not cancel another coalesced onto the same fetch`() =
        runTest {
            val count = AtomicInt(0)
            val firstRequestStarted = CompletableDeferred<Unit>()

            MockEngine {
                if (count.incrementAndFetch() == 1) {
                    firstRequestStarted.complete(Unit)
                    awaitCancellation()
                }
                respondJson(
                    """
                    {
                        "id": "GEN.INTRO",
                        "content": "<html>intro</html>",
                        "reference": "Genesis Intro"
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            val first = launch { repository.introContent(206, "GEN.INTRO") }
            firstRequestStarted.await()

            val outcome = CompletableDeferred<Result<String>>()
            launch { outcome.complete(runCatching { repository.introContent(206, "GEN.INTRO") }) }
            runCurrent()

            first.cancel()

            assertEquals("<html>intro</html>", outcome.await().getOrThrow())
            assertEquals(2, count.load())
        }

    /**
     * Both callers share one fetch and that fetch fails. Unlike cancellation, a genuine failure belongs to
     * every caller awaiting it, so both must see it. The single request count is what proves the second
     * caller coalesced rather than quietly fetching for itself.
     */
    @OptIn(ExperimentalAtomicApi::class, ExperimentalCoroutinesApi::class)
    @Test
    fun `test a failed fetch is reported to every coalesced caller`() =
        runTest {
            val requestCount = AtomicInt(0)
            val firstRequestStarted = CompletableDeferred<Unit>()
            val failFirstRequest = CompletableDeferred<Unit>()

            MockEngine {
                requestCount.incrementAndFetch()
                firstRequestStarted.complete(Unit)
                failFirstRequest.await()
                throw RuntimeException("Network error")
            }.also { engine -> startYouVersionPlatformTest(engine) }

            val ownerOutcome = CompletableDeferred<Result<String>>()
            launch { ownerOutcome.complete(runCatching { repository.introContent(206, "GEN.INTRO") }) }
            firstRequestStarted.await()

            val waiterOutcome = CompletableDeferred<Result<String>>()
            launch { waiterOutcome.complete(runCatching { repository.introContent(206, "GEN.INTRO") }) }
            runCurrent()

            failFirstRequest.complete(Unit)

            assertFailsWith<RuntimeException> { ownerOutcome.await().getOrThrow() }
            assertFailsWith<RuntimeException> { waiterOutcome.await().getOrThrow() }
            assertEquals(1, requestCount.load())
        }

    @OptIn(ExperimentalAtomicApi::class)
    @Test
    fun `test error propagates and cleans up in-flight task`() =
        runTest {
            val count = AtomicInt(0)

            MockEngine {
                count.incrementAndFetch()
                if (count.load() == 1) {
                    throw RuntimeException("Network error")
                }
                respondJson(
                    """
                    {
                        "id": "GEN.INTRO",
                        "content": "<html>intro</html>",
                        "reference": "Genesis Intro"
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            assertFailsWith<RuntimeException> {
                repository.introContent(206, "GEN.INTRO")
            }

            val result = repository.introContent(206, "GEN.INTRO")
            assertEquals("<html>intro</html>", result)
            assertEquals(2, count.load())
        }

    @OptIn(ExperimentalAtomicApi::class)
    @Test
    fun `test different cache keys trigger separate network requests`() =
        runTest {
            val count = AtomicInt(0)

            MockEngine {
                count.incrementAndFetch()
                respondJson(
                    """
                    {
                        "id": "INTRO",
                        "content": "content",
                        "reference": "ref"
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            repository.introContent(206, "GEN.INTRO")
            repository.introContent(206, "EXO.INTRO")
            repository.introContent(1, "GEN.INTRO")

            assertEquals(3, count.load())
        }

    @OptIn(ExperimentalAtomicApi::class, ExperimentalCoroutinesApi::class)
    @Test
    fun `test concurrent calls for different keys do not interfere`() =
        runTest {
            val count = AtomicInt(0)

            MockEngine {
                count.incrementAndFetch()
                respondJson(
                    """
                    {
                        "id": "INTRO",
                        "content": "content",
                        "reference": "ref"
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            joinAll(
                launch { repository.introContent(206, "GEN.INTRO") },
                launch { repository.introContent(206, "GEN.INTRO") },
                launch { repository.introContent(206, "EXO.INTRO") },
                launch { repository.introContent(206, "EXO.INTRO") },
            )

            assertEquals(2, count.load())
        }
}
