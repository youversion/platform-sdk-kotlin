package com.youversion.platform.core.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.search.api.SearchQueryResponse
import com.youversion.platform.core.utilities.koin.PlatformCoreKoinComponent
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.preparePost
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class YouVersionApiExtensionsTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    /**
     * The body channel carries only the start of a JSON document, so the read is still waiting for the rest
     * of it when the caller's job is cancelled. Real dispatchers and real time are needed throughout: the
     * read has to reach the point of waiting on the channel before the cancellation lands, which a
     * virtual-time scheduler cannot arrange. Once `readStarted` fires the read can no longer complete — the
     * channel is never closed — so any cancellation from that point on lands in the body read. The exception
     * is captured inside the `execute` block because
     * ktor's own scope replaces whatever the block threw with the job's cancellation, which would hide the
     * conversion under test.
     */
    @Test
    fun `test cancelling mid body read is reported as cancellation not invalid response`() =
        runTest {
            val body = ByteChannel(autoFlush = true)
            val requestHandled = CompletableDeferred<Unit>()
            MockEngine {
                body.writeStringUtf8("[")
                requestHandled.complete(Unit)
                respond(
                    content = body,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")

            withContext(Dispatchers.Default) {
                val observed = CompletableDeferred<Throwable>()
                val readStarted = CompletableDeferred<Unit>()
                val job =
                    launch {
                        PlatformCoreKoinComponent
                            .httpClient
                            .preparePost("https://api.youversion.com/v1/search-queries")
                            .execute { response ->
                                readStarted.complete(Unit)
                                try {
                                    parseApiBody<List<SearchQueryResponse>>(response)
                                } catch (e: Throwable) {
                                    observed.complete(e)
                                    throw e
                                }
                            }
                    }

                requestHandled.await()
                readStarted.await()
                delay(200)
                job.cancel()

                assertIs<CancellationException>(observed.await())
                job.join()
                assertTrue(job.isCancelled)
            }
        }
}
