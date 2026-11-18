package com.youversion.platform.core.videos.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class VideoModelsTests {
    private fun video(
        id: String = "v",
        title: String? = "Jesus",
        description: String? = "A film",
        references: List<String>? = listOf("JHN.3.16"),
        passageId: String? = "JHN.3",
        languageTag: String? = "en",
        runtimeSeconds: Int? = 120,
        orientation: String? = "landscape",
        thumbnail: VideoThumbnail? = VideoThumbnail("https://img.example/t.jpg", 1280, 720),
        playbackSources: List<VideoPlaybackSource> = listOf(VideoPlaybackSource("https://v.example/a.m3u8", "hls")),
        playbackPreviewSources: List<VideoPlaybackSource> = emptyList(),
        shareUrl: String? = "https://share.example/v",
    ) = Video(
        id = id,
        title = title,
        description = description,
        references = references,
        passageId = passageId,
        languageTag = languageTag,
        runtimeSeconds = runtimeSeconds,
        orientation = orientation,
        thumbnail = thumbnail,
        playbackSources = playbackSources,
        playbackPreviewSources = playbackPreviewSources,
        shareUrl = shareUrl,
    )

    private fun playlist(
        id: Int? = 7,
        title: String? = "Easter",
        description: String? = "Holy week",
        type: String? = "series",
        total: Int? = 2,
        videoIds: List<String>? = listOf("v1", "v2"),
    ) = VideoPlaylist(id, title, description, type, total, videoIds)

    @Test
    fun `test videos with the same fields are equal and hash alike`() {
        assertEquals(video(), video())
        assertEquals(video().hashCode(), video().hashCode())
    }

    @Test
    fun `test a difference in any video field makes videos unequal`() {
        listOf(
            video(id = "other"),
            video(title = null),
            video(description = null),
            video(references = null),
            video(passageId = null),
            video(languageTag = "es"),
            video(runtimeSeconds = 121),
            video(orientation = "portrait"),
            video(thumbnail = null),
            video(playbackSources = emptyList()),
            video(playbackPreviewSources = listOf(VideoPlaybackSource("https://v.example/p.mp4", "mp4"))),
            video(shareUrl = null),
        ).forEach {
            assertNotEquals(video(), it)
            assertNotEquals(video().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test video toString names every field`() {
        assertEquals(
            "Video(id=v, title=Jesus, description=A film, references=[JHN.3.16], passageId=JHN.3, languageTag=en, " +
                "runtimeSeconds=120, orientation=landscape, " +
                "thumbnail=VideoThumbnail(url=https://img.example/t.jpg, width=1280, height=720), " +
                "playbackSources=[VideoPlaybackSource(url=https://v.example/a.m3u8, format=hls)], " +
                "playbackPreviewSources=[], shareUrl=https://share.example/v)",
            video().toString(),
        )
    }

    @Test
    fun `test thumbnails compare every field`() {
        val thumbnail = VideoThumbnail("u", 4, 3)
        assertEquals(thumbnail, VideoThumbnail("u", 4, 3))
        assertEquals(thumbnail.hashCode(), VideoThumbnail("u", 4, 3).hashCode())
        listOf(VideoThumbnail("x", 4, 3), VideoThumbnail("u", 5, 3), VideoThumbnail("u", 4, 2)).forEach {
            assertNotEquals(thumbnail, it)
        }
    }

    @Test
    fun `test thumbnail aspect ratio needs a positive width and height`() {
        assertEquals(16.0 / 9, VideoThumbnail(width = 16, height = 9).aspectRatio)
        assertNull(VideoThumbnail(width = 16, height = 0).aspectRatio)
        assertNull(VideoThumbnail(width = -16, height = 9).aspectRatio)
        assertNull(VideoThumbnail(width = null, height = 9).aspectRatio)
    }

    @Test
    fun `test playback sources compare url and format`() {
        val source = VideoPlaybackSource("u", "hls")
        assertEquals(source, VideoPlaybackSource("u", "hls"))
        assertEquals(source.hashCode(), VideoPlaybackSource("u", "hls").hashCode())
        assertNotEquals(source, VideoPlaybackSource("u", "mp4"))
        assertNotEquals(source, VideoPlaybackSource("x", "hls"))
        assertEquals("VideoPlaybackSource(url=u, format=hls)", source.toString())
    }

    @Test
    fun `test playlists with the same fields are equal and hash alike`() {
        assertEquals(playlist(), playlist())
        assertEquals(playlist().hashCode(), playlist().hashCode())
    }

    @Test
    fun `test a difference in any playlist field makes playlists unequal`() {
        listOf(
            playlist(id = 8),
            playlist(title = null),
            playlist(description = null),
            playlist(type = "collection"),
            playlist(total = 3),
            playlist(videoIds = listOf("v2", "v1")),
        ).forEach {
            assertNotEquals(playlist(), it)
            assertNotEquals(playlist().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test playlist toString names every field`() {
        assertEquals(
            "VideoPlaylist(id=7, title=Easter, description=Holy week, type=series, total=2, videoIds=[v1, v2])",
            playlist().toString(),
        )
    }

    @Test
    fun `test thumbnail sizes carry the platform spelling`() {
        assertEquals(
            listOf("thumb", "card", "full"),
            listOf(VideoThumbnailSize.thumb, VideoThumbnailSize.card, VideoThumbnailSize.full).map { it.rawValue },
        )
        assertNotEquals(VideoThumbnailSize.thumb, VideoThumbnailSize.card)
        assertEquals("VideoThumbnailSize(rawValue=card)", VideoThumbnailSize.card.toString())
    }
}
