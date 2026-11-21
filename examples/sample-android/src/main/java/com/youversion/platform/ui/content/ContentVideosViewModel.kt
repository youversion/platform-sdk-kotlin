package com.youversion.platform.ui.content

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.videos.models.Video
import com.youversion.platform.core.videos.models.VideoPlaylist
import com.youversion.platform.core.videos.models.VideoThumbnailSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.text.Collator
import java.util.Locale

/** BibleProject's organization, whose playlists the video library shows. */
internal const val BIBLE_PROJECT_ORGANIZATION_ID = "6a4552c4-96eb-4df9-aa89-b2cae8db310d"

private const val PLAYLIST_PAGE_SIZE = 99
private const val MAXIMUM_CONCURRENT_VIDEO_REQUESTS = 4

/**
 * What the collection list shows.
 *
 * @property collections Every playlist once, sorted by title in the content language.
 * @property isLoading Whether the playlists are loading.
 * @property hasError Whether the playlists failed to load.
 */
@Immutable
internal data class ContentCollectionsState(
    val collections: List<VideoPlaylist> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
)

/**
 * The videos loaded so far, keyed by the id they were requested with, because a response's [Video.id] differs from
 * the requested one.
 *
 * @property videosByRequestedId The videos that loaded.
 * @property loadingIds The requested ids still loading or waiting to load.
 * @property failedIds The requested ids whose last load failed.
 */
@Immutable
internal data class ContentVideosState(
    val videosByRequestedId: Map<String, Video> = emptyMap(),
    val loadingIds: Set<String> = emptySet(),
    val failedIds: Set<String> = emptySet(),
)

/**
 * Loads BibleProject's playlists and the videos in them for the Content tab's video library, in one content language.
 *
 * Each video loads on its own, at most [MAXIMUM_CONCURRENT_VIDEO_REQUESTS] at a time, so one failure leaves the others
 * visible, and a video that loaded is never fetched again. Results for a previous language are dropped.
 */
internal class ContentVideosViewModel(
    initialLanguageTag: String = DEFAULT_CONTENT_LANGUAGE_TAG,
    private val playlistsPage: suspend (languageTag: String, pageToken: String?) -> Pair<List<VideoPlaylist>, String?> =
        { languageTag, pageToken ->
            YouVersionApi.videos
                .playlists(
                    organizationId = BIBLE_PROJECT_ORGANIZATION_ID,
                    pageSize = PLAYLIST_PAGE_SIZE,
                    pageToken = pageToken,
                    languageRanges = listOf(languageTag),
                ).let { it.data to it.nextPageToken }
        },
    private val playlistDetail: suspend (languageTag: String, id: Int) -> VideoPlaylist = { languageTag, id ->
        YouVersionApi.videos.playlist(BIBLE_PROJECT_ORGANIZATION_ID, id, listOf(languageTag))
    },
    private val videoDetail: suspend (id: String) -> Video = {
        YouVersionApi.videos.video(it, VideoThumbnailSize.card)
    },
) : ViewModel() {
    private val mutableCollectionsState = MutableStateFlow(ContentCollectionsState())
    val collectionsState: StateFlow<ContentCollectionsState> = mutableCollectionsState.asStateFlow()

    private val mutableVideosState = MutableStateFlow(ContentVideosState())
    val videosState: StateFlow<ContentVideosState> = mutableVideosState.asStateFlow()

    /** The language of the collections. */
    var languageTag: String = initialLanguageTag
        private set

    private val videoRequests = Semaphore(MAXIMUM_CONCURRENT_VIDEO_REQUESTS)
    private var collectionsJob: Job? = null
    private var videosJob = SupervisorJob(viewModelScope.coroutineContext[Job])
    private val openedCollectionsById = mutableMapOf<Int, VideoPlaylist>()

    init {
        loadCollections()
    }

    /** Loads the playlists again after they failed. */
    fun retryCollections() {
        if (collectionsState.value.hasError) loadCollections()
    }

    /** Replaces the collections, and the videos loaded from them, with those in [languageTag]. */
    fun setLanguage(languageTag: String) {
        if (languageTag == this.languageTag) return
        this.languageTag = languageTag
        videosJob.cancel()
        videosJob = SupervisorJob(viewModelScope.coroutineContext[Job])
        mutableVideosState.value = ContentVideosState()
        openedCollectionsById.clear()
        loadCollections()
    }

    /**
     * The collection with [id]: a listed one, or else fetched, for a collection screen restored after process death.
     */
    suspend fun collection(id: Int): VideoPlaylist =
        collectionsState.value.collections.firstOrNull { it.id == id }
            ?: openedCollectionsById[id]
            ?: playlistDetail(languageTag, id).also { openedCollectionsById[id] = it }

    /** Loads each of [ids] that has neither loaded nor is loading. */
    fun loadVideos(ids: List<String>) {
        val current = videosState.value
        val pendingIds = ids.distinct().filter { it !in current.videosByRequestedId && it !in current.loadingIds }
        if (pendingIds.isEmpty()) return
        mutableVideosState.update {
            it.copy(loadingIds = it.loadingIds + pendingIds, failedIds = it.failedIds - pendingIds.toSet())
        }
        pendingIds.forEach { id ->
            viewModelScope.launch(videosJob) {
                val video =
                    try {
                        videoRequests.withPermit { videoDetail(id) }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        null
                    }
                ensureActive()
                mutableVideosState.update {
                    if (video == null) {
                        it.copy(loadingIds = it.loadingIds - id, failedIds = it.failedIds + id)
                    } else {
                        it.copy(
                            videosByRequestedId = it.videosByRequestedId + (id to video),
                            loadingIds = it.loadingIds - id,
                        )
                    }
                }
            }
        }
    }

    private fun loadCollections() {
        collectionsJob?.cancel()
        val languageTag = languageTag
        mutableCollectionsState.value = ContentCollectionsState()
        collectionsJob =
            viewModelScope.launch {
                try {
                    val collections = mutableListOf<VideoPlaylist>()
                    val visitedTokens = mutableSetOf<String>()
                    var pageToken: String? = null
                    do {
                        val (page, nextPageToken) = playlistsPage(languageTag, pageToken)
                        collections += page
                        pageToken =
                            nextPageToken?.also { check(visitedTokens.add(it)) { "The playlists repeated a page." } }
                    } while (pageToken != null)
                    ensureActive()
                    val collator = Collator.getInstance(Locale.forLanguageTag(languageTag))
                    mutableCollectionsState.value =
                        ContentCollectionsState(
                            collections =
                                collections
                                    .distinctBy { it.id ?: Any() }
                                    .sortedWith(compareBy(collator) { it.title.orEmpty() }),
                            isLoading = false,
                        )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    ensureActive()
                    mutableCollectionsState.update { it.copy(isLoading = false, hasError = true) }
                }
            }
    }
}
