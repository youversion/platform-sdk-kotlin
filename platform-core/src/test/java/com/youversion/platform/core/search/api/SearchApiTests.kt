package com.youversion.platform.core.search.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.search.models.SearchUserIntent
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import com.youversion.platform.helpers.testCannotDownload
import com.youversion.platform.helpers.testForbiddenNotPermitted
import com.youversion.platform.helpers.testInvalidResponse
import com.youversion.platform.helpers.testUnauthorizedNotPermitted
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class SearchApiTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    @Test
    fun `test suggested queries success returns data`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(
                    "/v1/search-queries?language_ranges%5B%5D=en-US&language_ranges%5B%5D=es&query=love",
                    request.url.encodedPathAndQuery,
                )
                respondJson(
                    """
                    {
                        "data": [
                            { "text": "love", "source": "trending" },
                            { "text": "love your enemies" }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val queries =
                YouVersionApi.search.suggestedQueries(
                    query = "love",
                    languageRanges = listOf("en-US", "es"),
                )

            assertEquals(2, queries.size)
            assertEquals("love", queries[0].text)
            assertEquals("trending", queries[0].source)
            assertEquals("love your enemies", queries[1].text)
            assertNull(queries[1].source)
        }

    @Test
    fun `test trending queries success returns data`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(
                    "/v1/search-queries?language_ranges%5B%5D=%2A&trending=true",
                    request.url.encodedPathAndQuery,
                )
                respondJson(
                    """
                    {
                        "data": [
                            { "text": "peace", "source": "popular" }
                        ]
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val queries = YouVersionApi.search.trendingQueries(languageRanges = listOf("*"))

            assertEquals(1, queries.size)
            assertEquals("peace", queries[0].text)
            assertEquals("popular", queries[0].source)
        }

    @Test
    fun `test suggested queries returns an empty list if there is no content`() =
        runTest {
            MockEngine { respond("", HttpStatusCode.NoContent) }
                .also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            assertTrue {
                YouVersionApi.search
                    .suggestedQueries(query = "love", languageRanges = listOf("en"))
                    .isEmpty()
            }
        }

    @Test
    fun `test trending queries returns an empty list if there is no content`() =
        runTest {
            MockEngine { respond("", HttpStatusCode.NoContent) }
                .also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            assertTrue { YouVersionApi.search.trendingQueries(languageRanges = listOf("en")).isEmpty() }
        }

    @Test
    fun `test suggested queries rejects an empty query`() =
        runTest {
            startNoRequestExpected()
            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.search.suggestedQueries(query = "", languageRanges = listOf("en"))
            }
        }

    @Test
    fun `test suggested queries accepts a query longer than one hundred characters`() =
        runTest {
            MockEngine { respond("", HttpStatusCode.NoContent) }
                .also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            assertTrue {
                YouVersionApi.search
                    .suggestedQueries(query = "a".repeat(101), languageRanges = listOf("en"))
                    .isEmpty()
            }
        }

    @Test
    fun `test suggested queries rejects an empty language range list`() =
        runTest {
            startNoRequestExpected()
            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.search.suggestedQueries(query = "love", languageRanges = emptyList())
            }
        }

    @Test
    fun `test trending queries rejects an empty language range list`() =
        runTest {
            startNoRequestExpected()
            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.search.trendingQueries(languageRanges = emptyList())
            }
        }

    @Test
    fun `test suggested queries rejects a malformed language range`() =
        runTest {
            startNoRequestExpected()
            listOf("", "en_US", "en-", "-US", "abcdefghi", "en-US!").forEach { languageRange ->
                assertFailsWith<IllegalArgumentException>("expected $languageRange to be rejected") {
                    YouVersionApi.search.suggestedQueries(
                        query = "love",
                        languageRanges = listOf("en", languageRange),
                    )
                }
            }
        }

    @Test
    fun `test trending queries rejects a malformed language range`() =
        runTest {
            startNoRequestExpected()
            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.search.trendingQueries(languageRanges = listOf("en_US"))
            }
        }

    @Test
    fun `test verse search success returns data`() =
        runTest {
            MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                assertEquals(
                    "/v1/search-verses?query=two+fish&bible_id=111&user_intent=text" +
                        "&page_size=25&page_token=current-token",
                    request.url.encodedPathAndQuery,
                )
                respondJson(
                    """
                    {
                        "verses": [
                            { "reference": "MAT.14.17" },
                            { "reference": "JHN.6.9" }
                        ],
                        "user_intent": "text",
                        "did_you_mean": ["two fishes"],
                        "search_instead_for": null,
                        "next_page_token": "next-token"
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val results =
                YouVersionApi.search.verses(
                    query = "two fish",
                    bibleId = 111,
                    userIntent = SearchUserIntent.text,
                    pageSize = 25,
                    pageToken = "current-token",
                )

            assertEquals(
                listOf(
                    BibleReference(versionId = 111, bookUSFM = "MAT", chapter = 14, verse = 17),
                    BibleReference(versionId = 111, bookUSFM = "JHN", chapter = 6, verse = 9),
                ),
                results.references,
            )
            assertEquals(SearchUserIntent.text, results.userIntent)
            assertEquals(listOf("two fishes"), results.didYouMean)
            assertNull(results.searchInsteadFor)
            assertEquals("next-token", results.nextPageToken)
        }

    @Test
    fun `test verse search drops a malformed reference and keeps its well formed siblings`() =
        runTest {
            MockEngine {
                respondJson(
                    """
                    {
                        "verses": [
                            { "reference": "" },
                            { "reference": ".3.16" },
                            { "reference": "JHN.0.1" },
                            { "reference": "JHN.3.0" },
                            { "reference": "JHN.3" },
                            { "reference": "JHN.a.1" },
                            { "reference": "JHN.3.16.1" },
                            { "reference": "MAT.14.17" },
                            { "reference": "JHN.6.9" }
                        ],
                        "did_you_mean": []
                    }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val results = YouVersionApi.search.verses(query = "two fish", bibleId = 111)

            assertEquals(listOf("MAT.14.17", "JHN.6.9"), results.references.map { it.asUSFM })
        }

    @Test
    fun `test verse search upper cases a book so a result matches a reference built elsewhere`() =
        runTest {
            MockEngine {
                respondJson(
                    """
                    { "verses": [{ "reference": "jhn.3.16" }], "did_you_mean": [] }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val results = YouVersionApi.search.verses(query = "love", bibleId = 111)

            assertEquals(
                listOf(BibleReference(versionId = 111, bookUSFM = "JHN", chapter = 3, verse = 16)),
                results.references,
            )
        }

    @Test
    fun `test verse search preserves a user intent this version does not name`() =
        runTest {
            MockEngine {
                respondJson(
                    """
                    { "verses": [], "user_intent": "future-intent", "did_you_mean": [] }
                    """.trimIndent(),
                )
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val results = YouVersionApi.search.verses(query = "love", bibleId = 111)

            assertEquals(SearchUserIntent("future-intent"), results.userIntent)
        }

    @Test
    fun `test verse search reports no user intent when the platform names none`() =
        runTest {
            MockEngine {
                respondJson("""{ "verses": [], "did_you_mean": [] }""")
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            assertNull(YouVersionApi.search.verses(query = "love", bibleId = 111).userIntent)
        }

    @Test
    fun `test verse search rejects an empty query`() =
        runTest {
            startNoRequestExpected()
            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.search.verses(query = "", bibleId = 111)
            }
        }

    @Test
    fun `test verse search rejects a query longer than one hundred graphemes`() =
        runTest {
            startNoRequestExpected()
            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.search.verses(query = "a".repeat(101), bibleId = 111)
            }
        }

    @Test
    fun `test verse search measures a query in graphemes rather than utf-16 code units`() =
        runTest {
            MockEngine {
                respondJson("""{ "verses": [], "did_you_mean": [] }""")
            }.also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val query = "e\u0301".repeat(100)

            assertEquals(200, query.length)
            assertTrue {
                YouVersionApi.search
                    .verses(query = query, bibleId = 111)
                    .references
                    .isEmpty()
            }
        }

    @Test
    fun `test verse search rejects a bible version identifier of zero or less`() =
        runTest {
            startNoRequestExpected()
            listOf(0, -1).forEach { bibleId ->
                assertFailsWith<IllegalArgumentException>("expected $bibleId to be rejected") {
                    YouVersionApi.search.verses(query = "love", bibleId = bibleId)
                }
            }
        }

    @Test
    fun `test verse search rejects a page size outside one to ninety nine`() =
        runTest {
            startNoRequestExpected()
            listOf(0, 100).forEach { pageSize ->
                assertFailsWith<IllegalArgumentException>("expected $pageSize to be rejected") {
                    YouVersionApi.search.verses(query = "love", bibleId = 111, pageSize = pageSize)
                }
            }
        }

    @Test
    fun `test verse search intentionally fails on no content rather than returning nothing found`() =
        runTest {
            MockEngine { respond("", HttpStatusCode.NoContent) }
                .also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            assertFailsWith<YouVersionNetworkException> {
                YouVersionApi.search.verses(query = "love", bibleId = 111)
            }.apply { assertEquals(YouVersionNetworkException.Reason.INVALID_RESPONSE, reason) }
        }

    @Test
    fun `test verse search intentionally fails when did you mean is absent`() =
        runTest {
            MockEngine { respondJson("""{ "verses": [] }""") }
                .also { engine -> startYouVersionPlatformTest(engine) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            assertFailsWith<YouVersionNetworkException> {
                YouVersionApi.search.verses(query = "love", bibleId = 111)
            }.apply { assertEquals(YouVersionNetworkException.Reason.INVALID_RESPONSE, reason) }
        }

    @Test
    fun `test verse search unauthorized is not permitted`() =
        testUnauthorizedNotPermitted { YouVersionApi.search.verses(query = "love", bibleId = 111) }

    @Test
    fun `test verse search forbidden is not permitted`() =
        testForbiddenNotPermitted { YouVersionApi.search.verses(query = "love", bibleId = 111) }

    @Test
    fun `test verse search server error cannot download`() =
        testCannotDownload { YouVersionApi.search.verses(query = "love", bibleId = 111) }

    @Test
    fun `test verse search malformed body is an invalid response`() =
        testInvalidResponse { YouVersionApi.search.verses(query = "love", bibleId = 111) }

    private fun startNoRequestExpected() {
        MockEngine { fail("no request should have been issued") }
            .also { engine -> startYouVersionPlatformTest(engine) }
        YouVersionPlatformConfiguration.configure(appKey = "app")
    }
}
