package com.youversion.platform.core.bibles.domain

import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
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
