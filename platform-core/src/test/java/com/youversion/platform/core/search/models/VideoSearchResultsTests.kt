package com.youversion.platform.core.search.models

import com.youversion.platform.core.videos.models.Video
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class VideoSearchResultsTests {
    private val video = Video(id = "v", playbackSources = emptyList(), playbackPreviewSources = emptyList())

    private fun results(
        videos: List<Video> = listOf(video),
        userIntent: SearchUserIntent? = SearchUserIntent.text,
        didYouMean: List<String> = listOf("fishes"),
        searchInsteadFor: String? = "fsh",
        nextPageToken: String? = "next",
    ) = VideoSearchResults(videos, userIntent, didYouMean, searchInsteadFor, nextPageToken)

    @Test
    fun `test results with the same fields are equal and hash alike`() {
        assertEquals(results(), results())
        assertEquals(results().hashCode(), results().hashCode())
    }

    @Test
    fun `test a difference in any field makes results unequal`() {
        listOf(
            results(videos = emptyList()),
            results(userIntent = null),
            results(didYouMean = emptyList()),
            results(searchInsteadFor = null),
            results(nextPageToken = null),
        ).forEach {
            assertNotEquals(results(), it)
            assertNotEquals(results().hashCode(), it.hashCode())
        }
    }

    @Test
    fun `test toString names every field`() {
        assertEquals(
            "VideoSearchResults(videos=[$video], userIntent=SearchUserIntent(rawValue=text), " +
                "didYouMean=[fishes], searchInsteadFor=fsh, nextPageToken=next)",
            results().toString(),
        )
    }

    @Test
    fun `test search videos compare by id`() {
        assertEquals(SearchVideo("v"), SearchVideo("v"))
        assertEquals(SearchVideo("v").hashCode(), SearchVideo("v").hashCode())
        assertNotEquals(SearchVideo("v"), SearchVideo("w"))
        assertEquals("SearchVideo(id=v)", SearchVideo("v").toString())
    }
}
