package com.youversion.platform.core.notes.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.notes.models.Note
import com.youversion.platform.core.notes.models.NoteCreateRequest
import com.youversion.platform.core.notes.models.NoteStatus
import com.youversion.platform.core.notes.models.NoteUpdate
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotesApiTests : YouVersionPlatformTest {
    private val requests = mutableListOf<HttpRequestData>()

    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    private fun start(
        accessToken: String? = "token",
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) {
        startYouVersionPlatformTest(
            MockEngine { request ->
                requests += request
                handler(request)
            },
        )
        YouVersionPlatformConfiguration.configure(appKey = "app", accessToken = accessToken)
    }

    private suspend fun HttpRequestData.jsonBody(): JsonObject =
        Json.parseToJsonElement(body.toByteArray().decodeToString()).jsonObject

    @Test
    fun `test notes for references merges ranges, keeps gaps and decodes the page`() =
        runTest {
            start { request ->
                assertEquals(
                    "/v1/notes?bible_id=111&passage_id=JHN.3.1-3%2BJHN.3.7&page_size=10&page_token=a%2Bb%2F",
                    request.url.encodedPathAndQuery,
                )
                assertEquals("Bearer token", request.headers[HttpHeaders.Authorization])
                respondJson(
                    """
                    {
                      "data": [
                        {
                          "id": "n1", "title": "Born again", "content": "text", "color": "ffcc00",
                          "passage": { "bible_id": 111, "passage_id": "JHN.3.7+JHN.3.1-3" },
                          "language_tag": "en-US", "tags": ["faith"], "status": "private",
                          "created_dt": "2026-11-24T10:00:00Z", "unknown": 1
                        },
                        { "id": "n2", "content": "", "passage": { "bible_id": 111, "passage_id": "JHN.3" },
                          "status": "friends" }
                      ],
                      "next_page_token": "next+token"
                    }
                    """.trimIndent(),
                )
            }

            val page =
                YouVersionApi.notes.notes(
                    references =
                        listOf(
                            BibleReference(111, "JHN", 3, 7),
                            BibleReference(111, "JHN", 3, 1),
                            BibleReference(111, "JHN", 3, 2, 3),
                        ),
                    pageSize = 10,
                    pageToken = "a+b/",
                )

            assertEquals(
                PaginatedResponse(
                    listOf(
                        Note(
                            id = "n1",
                            title = "Born again",
                            content = "text",
                            color = "ffcc00",
                            references = listOf(BibleReference(111, "JHN", 3, 1, 3), BibleReference(111, "JHN", 3, 7)),
                            languageTag = "en-US",
                            tags = listOf("faith"),
                            status = NoteStatus.PRIVATE,
                            createdTime = "2026-11-24T10:00:00Z",
                            updatedTime = null,
                        ),
                        Note(
                            id = "n2",
                            title = null,
                            content = "",
                            color = null,
                            references = null,
                            languageTag = null,
                            tags = null,
                            status = NoteStatus("friends"),
                            createdTime = null,
                            updatedTime = null,
                        ),
                    ),
                    nextPageToken = "next+token",
                ),
                page,
            )
        }

    @Test
    fun `test notes for a chapter in any version sends no bible_id`() =
        runTest {
            start { request ->
                assertEquals("/v1/notes?passage_id=JHN.3", request.url.encodedPathAndQuery)
                respondJson("""{ "data": [], "next_page_token": "more" }""")
            }

            val page =
                YouVersionApi.notes.notes(
                    references = listOf(BibleReference(111, "jhn", 3, 16), BibleReference(111, "JHN", 3)),
                    isAnyVersion = true,
                )

            assertEquals(PaginatedResponse<Note>(emptyList(), nextPageToken = "more"), page)
        }

    @Test
    fun `test notes returns an empty last page on no content`() =
        runTest {
            start { request ->
                assertEquals("/v1/notes?bible_id=12", request.url.encodedPathAndQuery)
                respond("", HttpStatusCode.NoContent)
            }

            assertEquals(PaginatedResponse(emptyList()), YouVersionApi.notes.notes(versionId = 12))
        }

    @Test
    fun `test notes rejects invalid arguments before sending`() =
        runTest {
            start { error("no request expected") }

            assertFailsWith<IllegalArgumentException> { YouVersionApi.notes.notes(pageSize = 0) }
            assertFailsWith<IllegalArgumentException> { YouVersionApi.notes.notes(pageSize = 100) }
            assertFailsWith<IllegalArgumentException> { YouVersionApi.notes.notes(references = emptyList()) }
            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.notes.notes(
                    references = listOf(BibleReference(1, "JHN", 3, 1), BibleReference(1, "JHN", 4, 1)),
                )
            }
            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.notes.notes(
                    references = listOf(BibleReference(1, "JHN", 3, 1), BibleReference(2, "JHN", 3, 2)),
                )
            }
            assertTrue(requests.isEmpty())
        }

    @Test
    fun `test a signed-out user fails without a request`() =
        runTest {
            start(accessToken = null) { error("no request expected") }

            val exception = assertFailsWith<YouVersionNetworkException> { YouVersionApi.notes.note("n1") }

            assertEquals(YouVersionNetworkException.Reason.MISSING_AUTHENTICATION, exception.reason)
            assertTrue(requests.isEmpty())
        }

    @Test
    fun `test 401 is missing authentication and 403 is not permitted`() =
        runTest {
            start { request ->
                when (request.method) {
                    HttpMethod.Get -> respondJson("""{"message":"expired"}""", HttpStatusCode.Unauthorized)
                    else -> respondJson("""{"message":"no write_notes"}""", HttpStatusCode.Forbidden)
                }
            }

            val unauthorized = assertFailsWith<YouVersionNetworkException> { YouVersionApi.notes.notes() }
            assertEquals(YouVersionNetworkException.Reason.MISSING_AUTHENTICATION, unauthorized.reason)
            assertEquals(401, unauthorized.statusCode)

            val forbidden = assertFailsWith<YouVersionNetworkException> { YouVersionApi.notes.deleteNote("n1") }
            assertEquals(YouVersionNetworkException.Reason.NOT_PERMITTED, forbidden.reason)
            assertEquals("no write_notes", forbidden.serverMessage)
        }

    @Test
    fun `test deleteNote encodes the id and throws on a missing note`() =
        runTest {
            start { request ->
                assertEquals(HttpMethod.Delete, request.method)
                when (request.url.encodedPath) {
                    "/v1/notes/a%2Fb" -> respond("", HttpStatusCode.NoContent)
                    else -> respondJson("""{"message":"not found"}""", HttpStatusCode.NotFound)
                }
            }

            YouVersionApi.notes.deleteNote("a/b")
            val exception = assertFailsWith<YouVersionNetworkException> { YouVersionApi.notes.deleteNote("gone") }

            assertEquals(YouVersionNetworkException.Reason.REQUEST_REJECTED, exception.reason)
            assertEquals(404, exception.statusCode)
        }

    @Test
    fun `test createNote retried with the same request keeps its request id`() =
        runTest {
            start { request ->
                assertEquals("/v1/notes", request.url.encodedPathAndQuery)
                when (requests.size) {
                    1 -> respondJson("""{"message":"busy"}""", HttpStatusCode.ServiceUnavailable)
                    else -> respondJson("""{ "id": "n1", "content": "", "status": "public" }""", HttpStatusCode.Created)
                }
            }
            val request =
                NoteCreateRequest(
                    content = "Born again",
                    status = NoteStatus.PUBLIC,
                    color = "ffcc00",
                    references = listOf(BibleReference(111, "JHN", 3, 7), BibleReference(111, "JHN", 3, 1, 2)),
                )

            val failure = assertFailsWith<YouVersionNetworkException> { YouVersionApi.notes.createNote(request) }
            val note = YouVersionApi.notes.createNote(request)

            assertEquals(YouVersionNetworkException.Reason.SERVER_ERROR, failure.reason)
            assertEquals("n1", note.id)
            val bodies = requests.map { it.jsonBody() }
            assertEquals(bodies[0], bodies[1])
            assertEquals(
                Json.parseToJsonElement(
                    """
                    {
                      "request_id": "${request.requestId}", "content": "Born again", "status": "public", "color": "ffcc00",
                      "passage": { "bible_id": 111, "passage_id": "JHN.3.1-2+JHN.3.7" }
                    }
                    """.trimIndent(),
                ),
                bodies[0],
            )
        }

    @Test
    fun `test createNote sends empty content to save a passage without text`() =
        runTest {
            start { respondJson("""{ "id": "n1", "content": "", "status": "private" }""", HttpStatusCode.Created) }
            val request =
                NoteCreateRequest("", NoteStatus.PRIVATE, references = listOf(BibleReference(111, "JHN", 3, 16)))

            YouVersionApi.notes.createNote(request)

            assertEquals(
                Json.parseToJsonElement(
                    """
                    {
                      "request_id": "${request.requestId}", "content": "", "status": "private",
                      "passage": { "bible_id": 111, "passage_id": "JHN.3.16" }
                    }
                    """.trimIndent(),
                ),
                requests.single().jsonBody(),
            )
        }

    @Test
    fun `test createNote rejects a chapter attachment and a note with neither text nor passage`() =
        runTest {
            start { error("no request expected") }

            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.notes.createNote(NoteCreateRequest("", NoteStatus.PRIVATE))
            }

            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.notes.createNote(
                    NoteCreateRequest("text", NoteStatus.PRIVATE, references = listOf(BibleReference(111, "JHN", 3))),
                )
            }
        }

    @Test
    fun `test updateNote sends cleared fields as null and leaves out omitted ones`() =
        runTest {
            start { request ->
                assertEquals(HttpMethod.Patch, request.method)
                assertEquals("/v1/notes/n1", request.url.encodedPath)
                respondJson("""{ "id": "n1", "content": "new", "status": "private", "updated_dt": "now" }""")
            }

            val note =
                YouVersionApi.notes.updateNote(
                    "n1",
                    NoteUpdate(
                        title = NoteUpdate.Field.Clear,
                        content = "new",
                        references = NoteUpdate.Field.Clear,
                        tags = NoteUpdate.Field.Value(listOf("faith", "hope")),
                        color = NoteUpdate.Field.Value(""),
                    ),
                )

            assertEquals("now", note.updatedTime)
            assertEquals(
                Json.parseToJsonElement(
                    """{ "title": null, "content": "new", "color": "", "passage": null, "tags": ["faith", "hope"] }""",
                ),
                requests.single().jsonBody(),
            )
        }

    @Test
    fun `test updateNote rejects updates that change nothing or clear required values`() =
        runTest {
            start { error("no request expected") }

            assertFailsWith<IllegalArgumentException> { YouVersionApi.notes.updateNote("n1", NoteUpdate()) }
            assertFailsWith<IllegalArgumentException> { YouVersionApi.notes.updateNote("n1", NoteUpdate(content = "")) }
            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.notes.updateNote("n1", NoteUpdate(references = NoteUpdate.Field.Value(emptyList())))
            }
            assertFailsWith<IllegalArgumentException> {
                YouVersionApi.notes.updateNote("", NoteUpdate(status = NoteStatus.PUBLIC))
            }
        }

    @Test
    fun `test a malformed response is an invalid response`() =
        runTest {
            start { respondJson("""{ "data": [{ "id": "n1" }] }""") }

            val exception = assertFailsWith<YouVersionNetworkException> { YouVersionApi.notes.notes() }

            assertEquals(YouVersionNetworkException.Reason.INVALID_RESPONSE, exception.reason)
        }

    @Test
    fun `test logging notes hides the user's writing`() {
        val logged =
            listOf(
                NoteCreateRequest(
                    "secret text",
                    NoteStatus.PRIVATE,
                    title = "secret title",
                    tags = listOf("secret tag"),
                ),
                NoteUpdate(title = NoteUpdate.Field.Value("secret title"), content = "secret text"),
                Note(
                    "n1",
                    "secret title",
                    "secret text",
                    null,
                    null,
                    null,
                    listOf("secret tag"),
                    NoteStatus.PUBLIC,
                    null,
                    null,
                ),
            ).joinToString()

        assertFalse(logged.contains("secret"), logged)
    }

    @Test
    fun `test malformed passages decode to no references`() {
        listOf(
            """{ "bible_id": 1, "passage_id": "JHN.3.5-2" }""",
            """{ "bible_id": 1, "passage_id": "JHN.3.1+JHN.4.1" }""",
            """{ "bible_id": 1, "passage_id": "jhn.3.1" }""",
            """{ "bible_id": 1, "passage_id": "JHN.3.99999999999" }""",
            "null",
        ).forEach { assertNull(notePassageReferences(Json.parseToJsonElement(it)), it) }
    }
}
