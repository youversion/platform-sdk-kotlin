package com.youversion.platform.core.videos.api

import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.api.YouVersionNetworkException
import com.youversion.platform.core.videos.models.Video
import com.youversion.platform.core.videos.models.VideoPlaybackSource
import com.youversion.platform.core.videos.models.VideoPlaylist
import com.youversion.platform.core.videos.models.VideoThumbnail
import com.youversion.platform.core.videos.models.VideoThumbnailSize
import com.youversion.platform.helpers.YouVersionPlatformTest
import com.youversion.platform.helpers.respondJson
import com.youversion.platform.helpers.startYouVersionPlatformTest
import com.youversion.platform.helpers.stopYouVersionPlatformTest
import com.youversion.platform.helpers.testForbiddenNotPermitted
import com.youversion.platform.helpers.testInvalidResponse
import com.youversion.platform.helpers.testServerError
import com.youversion.platform.helpers.testUnauthorizedNotPermitted
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class VideosApiTests : YouVersionPlatformTest {
    @AfterTest
    fun teardown() = stopYouVersionPlatformTest()

    @Test
    fun `test playlists encodes opaque values and decodes the page`() =
        runTest {
            MockEngine { request ->
                assertEquals(
                    "/v1-beta/organizations/org%2F1/playlists?page_size=50&page_token=next%2B%2F%3D",
                    request.url.encodedPathAndQuery,
                )
                assertEquals("en", request.headers[HttpHeaders.AcceptLanguage])
                respondJson(
                    """
                    {
                        "data": [
                            { "id": 7, "title": "Easter", "type": "series", "total": 2, "videos": ["v1", "v2"] }
                        ],
                        "next_page_token": "after"
                    }
                    """.trimIndent(),
                )
            }.also { startYouVersionPlatformTest(it) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val page =
                YouVersionApi.videos.playlists(
                    organizationId = "org/1",
                    pageSize = 50,
                    pageToken = "next+/=",
                )

            assertEquals(
                PaginatedResponse(
                    data =
                        listOf(
                            VideoPlaylist(
                                id = 7,
                                title = "Easter",
                                description = null,
                                type = "series",
                                total = 2,
                                videoIds = listOf("v1", "v2"),
                            ),
                        ),
                    nextPageToken = "after",
                ),
                page,
            )
        }

    @Test
    fun `test playlists with fields pages by the requested size`() =
        runTest {
            MockEngine { request ->
                assertEquals(
                    "/v1-beta/organizations/org/playlists?fields%5B%5D=id&fields%5B%5D=videos&page_size=5&page_token=",
                    request.url.encodedPathAndQuery,
                )
                respondJson("""{ "data": [{ "id": 1 }] }""")
            }.also { startYouVersionPlatformTest(it) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val page =
                YouVersionApi.videos.playlists(
                    organizationId = "org",
                    fields = listOf("id", "videos"),
                    pageSize = 5,
                    pageToken = "",
                )

            assertEquals(PaginatedResponse(listOf(VideoPlaylist(id = 1))), page)
        }

    @Test
    fun `test playlists without fields sends no fields`() =
        runTest {
            MockEngine { request ->
                assertEquals("/v1-beta/organizations/org/playlists", request.url.encodedPathAndQuery)
                respond("", HttpStatusCode.NoContent)
            }.also { startYouVersionPlatformTest(it) }

            YouVersionPlatformConfiguration.configure(appKey = "app")

            assertEquals(PaginatedResponse(emptyList()), YouVersionApi.videos.playlists(organizationId = "org"))
        }

    @Test
    fun `test playlists rejects invalid parameters before sending`() =
        runTest {
            MockEngine { error("no request expected") }.also { startYouVersionPlatformTest(it) }
            YouVersionPlatformConfiguration.configure(appKey = "app")
            listOf<suspend () -> Unit>(
                { YouVersionApi.videos.playlists(organizationId = "") },
                { YouVersionApi.videos.playlists(organizationId = "org", fields = emptyList()) },
                { YouVersionApi.videos.playlists(organizationId = "org", fields = listOf("bogus")) },
                { YouVersionApi.videos.playlists(organizationId = "org", pageSize = 0) },
                { YouVersionApi.videos.playlists(organizationId = "org", pageSize = 100) },
            ).forEach { assertFailsWith<IllegalArgumentException> { it() } }
        }

    @Test
    fun `test playlist fetches one playlist`() =
        runTest {
            MockEngine { request ->
                assertEquals("/v1-beta/organizations/org%2F1/playlists/7", request.url.encodedPathAndQuery)
                respondJson("""{ "id": 7, "title": "Easter" }""")
            }.also { startYouVersionPlatformTest(it) }

            YouVersionPlatformConfiguration.configure(appKey = "app")

            assertEquals(
                VideoPlaylist(id = 7, title = "Easter"),
                YouVersionApi.videos.playlist(organizationId = "org/1", id = 7),
            )
        }

    @Test
    fun `test playlists and playlist send the requested languages`() =
        runTest {
            MockEngine { request ->
                assertEquals("es, en;q=0.9", request.headers[HttpHeaders.AcceptLanguage])
                if (request.url.encodedPath.endsWith("/7")) {
                    respondJson("""{ "id": 7 }""")
                } else {
                    respondJson("""{ "data": [] }""")
                }
            }.also { startYouVersionPlatformTest(it) }

            YouVersionPlatformConfiguration.configure(appKey = "app")

            YouVersionApi.videos.playlists(organizationId = "org", languageRanges = listOf("es", "en"))
            YouVersionApi.videos.playlist(organizationId = "org", id = 7, languageRanges = listOf("es", "en"))
        }

    @Test
    fun `test playlist sends english by default and no language when given none`() =
        runTest {
            var expectedLanguage: String? = "en"
            MockEngine { request ->
                assertEquals(expectedLanguage, request.headers[HttpHeaders.AcceptLanguage])
                respondJson("""{ "id": 7 }""")
            }.also { startYouVersionPlatformTest(it) }

            YouVersionPlatformConfiguration.configure(appKey = "app")

            YouVersionApi.videos.playlist(organizationId = "org", id = 7)
            expectedLanguage = null
            YouVersionApi.videos.playlist(organizationId = "org", id = 7, languageRanges = emptyList())
        }

    @Test
    fun `test playlist rejects an empty organization`() =
        runTest {
            assertFailsWith<IllegalArgumentException> { YouVersionApi.videos.playlist(organizationId = "", id = 1) }
        }

    @Test
    fun `test video decodes metadata and resizes the thumbnail`() =
        runTest {
            MockEngine { request ->
                assertEquals("/v1-beta/videos/vid+1%2F=", request.url.encodedPathAndQuery)
                assertNull(request.headers[HttpHeaders.AcceptLanguage])
                respondJson(
                    """
                    {
                        "id": "vid+1/=",
                        "title": "Jesus",
                        "description": "A film",
                        "references": ["JHN.3.16", "JHN.3.16-17"],
                        "passage_id": "JHN.3",
                        "language_tag": "en",
                        "runtime": 120,
                        "orientation": "landscape",
                        "thumbnail": {
                            "url": "https://img.example.com/t.jpg?size=full&sig=a%2Bb&size=card#frag",
                            "width": 1280,
                            "height": 720
                        },
                        "playback_sources": [{ "url": "https://v.example.com/a.m3u8", "format": "hls" }],
                        "playback_preview_sources": [{ "url": "https://v.example.com/p.mp4", "format": "mp4" }],
                        "share_url": "https://share.example.com/v"
                    }
                    """.trimIndent(),
                )
            }.also { startYouVersionPlatformTest(it) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val video = YouVersionApi.videos.video(id = "vid+1/=", thumbnailSize = VideoThumbnailSize.thumb)

            assertEquals(
                Video(
                    id = "vid+1/=",
                    title = "Jesus",
                    description = "A film",
                    references = listOf("JHN.3.16", "JHN.3.16-17"),
                    passageId = "JHN.3",
                    languageTag = "en",
                    runtimeSeconds = 120,
                    orientation = "landscape",
                    thumbnail =
                        VideoThumbnail(
                            url = "https://img.example.com/t.jpg?sig=a%2Bb&size=thumb#frag",
                            width = 1280,
                            height = 720,
                        ),
                    playbackSources = listOf(VideoPlaybackSource("https://v.example.com/a.m3u8", "hls")),
                    playbackPreviewSources = listOf(VideoPlaybackSource("https://v.example.com/p.mp4", "mp4")),
                    shareUrl = "https://share.example.com/v",
                ),
                video,
            )
            assertEquals(1280.0 / 720, video.thumbnail?.aspectRatio)
        }

    @Test
    fun `test video without a size drops the thumbnail size`() =
        runTest {
            MockEngine {
                respondJson(
                    """
                    {
                        "id": "v",
                        "thumbnail": { "url": "https://img.example.com/t.jpg?size=card", "width": 0, "height": 720 },
                        "playback_sources": [],
                        "playback_preview_sources": []
                    }
                    """.trimIndent(),
                )
            }.also { startYouVersionPlatformTest(it) }

            YouVersionPlatformConfiguration.configure(appKey = "app")
            val thumbnail = YouVersionApi.videos.video(id = "v").thumbnail

            assertEquals(VideoThumbnail("https://img.example.com/t.jpg", 0, 720), thumbnail)
            assertNull(thumbnail?.aspectRatio)
        }

    @Test
    fun `test video leaves absent optional fields null`() =
        runTest {
            MockEngine {
                respondJson("""{ "id": "v", "playback_sources": [], "playback_preview_sources": [] }""")
            }.also { startYouVersionPlatformTest(it) }

            YouVersionPlatformConfiguration.configure(appKey = "app")

            assertEquals(
                Video(id = "v", playbackSources = emptyList(), playbackPreviewSources = emptyList()),
                YouVersionApi.videos.video(id = "v"),
            )
        }

    @Test
    fun `test video leaves explicit null optional fields null`() =
        runTest {
            MockEngine {
                respondJson(
                    """
                    {
                        "id": "v", "title": null, "description": null, "references": null, "passage_id": null,
                        "language_tag": null, "runtime": null, "orientation": null, "thumbnail": null,
                        "share_url": null, "playback_sources": [], "playback_preview_sources": []
                    }
                    """.trimIndent(),
                )
            }.also { startYouVersionPlatformTest(it) }

            YouVersionPlatformConfiguration.configure(appKey = "app")

            assertEquals(
                Video(id = "v", playbackSources = emptyList(), playbackPreviewSources = emptyList()),
                YouVersionApi.videos.video(id = "v"),
            )
        }

    @Test
    fun `test playlist leaves absent and null fields null`() =
        runTest {
            MockEngine {
                respondJson("""{ "id": 7, "title": null, "videos": null }""")
            }.also { startYouVersionPlatformTest(it) }

            YouVersionPlatformConfiguration.configure(appKey = "app")

            assertEquals(VideoPlaylist(id = 7), YouVersionApi.videos.playlist(organizationId = "org", id = 7))
        }

    @Test
    fun `test video rejects invalid parameters`() =
        runTest {
            assertFailsWith<IllegalArgumentException> { YouVersionApi.videos.video(id = "") }
        }

    @Test
    fun `test playlists throws not permitted when unauthorized`() =
        testUnauthorizedNotPermitted { YouVersionApi.videos.playlists(organizationId = "org") }

    @Test
    fun `test playlist throws not permitted when forbidden`() =
        testForbiddenNotPermitted { YouVersionApi.videos.playlist(organizationId = "org", id = 1) }

    @Test
    fun `test playlists throws not permitted when forbidden`() =
        testForbiddenNotPermitted { YouVersionApi.videos.playlists(organizationId = "org") }

    @Test
    fun `test playlist throws not permitted when unauthorized`() =
        testUnauthorizedNotPermitted { YouVersionApi.videos.playlist(organizationId = "org", id = 1) }

    @Test
    fun `test video throws not permitted when unauthorized`() =
        testUnauthorizedNotPermitted { YouVersionApi.videos.video(id = "v") }

    @Test
    fun `test video throws not permitted when forbidden`() =
        testForbiddenNotPermitted { YouVersionApi.videos.video(id = "v") }

    @Test
    fun `test video throws server error`() = testServerError { YouVersionApi.videos.video(id = "v") }

    @Test
    fun `test video throws invalid response on malformed body`() =
        testInvalidResponse { YouVersionApi.videos.video(id = "v") }

    @Test
    fun `test video with a malformed thumbnail url is an invalid response`() =
        runTest {
            MockEngine {
                respondJson(
                    """
                    {
                        "id": "v",
                        "thumbnail": { "url": "https://img.example.com:notaport/t.jpg?size=card" },
                        "playback_sources": [],
                        "playback_preview_sources": []
                    }
                    """.trimIndent(),
                )
            }.also { startYouVersionPlatformTest(it) }

            YouVersionPlatformConfiguration.configure(appKey = "app")

            assertFailsWith<YouVersionNetworkException> {
                YouVersionApi.videos.video(id = "v", thumbnailSize = VideoThumbnailSize.card)
            }.apply { assertEquals(YouVersionNetworkException.Reason.INVALID_RESPONSE, reason) }
        }

    @Test
    fun `test playlists throws server error`() =
        testServerError { YouVersionApi.videos.playlists(organizationId = "org") }

    @Test
    fun `test playlists throws invalid response on malformed body`() =
        testInvalidResponse { YouVersionApi.videos.playlists(organizationId = "org") }

    @Test
    fun `test playlist throws server error`() =
        testServerError { YouVersionApi.videos.playlist(organizationId = "org", id = 1) }

    @Test
    fun `test playlist throws invalid response on malformed body`() =
        testInvalidResponse { YouVersionApi.videos.playlist(organizationId = "org", id = 1) }

    @Test
    fun `test playlists sends only the first ten language ranges`() =
        runTest {
            val ranges = listOf("en", "es", "fr", "de", "it", "pt", "nl", "sv", "da", "fi", "nb")
            MockEngine { request ->
                assertEquals(
                    "en, es;q=0.9, fr;q=0.8, de;q=0.7, it;q=0.6, pt;q=0.5, nl;q=0.4, sv;q=0.3, da;q=0.2, fi;q=0.1",
                    request.headers[HttpHeaders.AcceptLanguage],
                )
                respondJson("""{ "data": [] }""")
            }.also { startYouVersionPlatformTest(it) }

            YouVersionPlatformConfiguration.configure(appKey = "app")

            YouVersionApi.videos.playlists(organizationId = "org", languageRanges = ranges)
        }

    @Test
    fun `test playlists and playlist send no language for only blank ranges`() =
        runTest {
            MockEngine { request ->
                assertNull(request.headers[HttpHeaders.AcceptLanguage])
                if (request.url.encodedPath.endsWith("/7")) {
                    respondJson("""{ "id": 7 }""")
                } else {
                    respondJson("""{ "data": [] }""")
                }
            }.also { startYouVersionPlatformTest(it) }

            YouVersionPlatformConfiguration.configure(appKey = "app")

            YouVersionApi.videos.playlists(organizationId = "org", languageRanges = listOf(" "))
            YouVersionApi.videos.playlist(organizationId = "org", id = 7, languageRanges = listOf(" "))
        }

    @Test
    fun `test playlists with a few fields and no page size asks for every remaining playlist`() =
        runTest {
            MockEngine { request ->
                assertEquals(
                    "/v1-beta/organizations/org/playlists?fields%5B%5D=id&page_size=%2A",
                    request.url.encodedPathAndQuery,
                )
                respondJson("""{ "data": [] }""")
            }.also { startYouVersionPlatformTest(it) }

            YouVersionPlatformConfiguration.configure(appKey = "app")

            YouVersionApi.videos.playlists(organizationId = "org", fields = listOf("id"))
        }
}
