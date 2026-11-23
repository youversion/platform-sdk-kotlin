package com.youversion.platform.ui.content

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youversion.platform.core.BibleDefaults
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.verseimages.models.VerseImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.BreakIterator
import java.util.Locale

/** The content language of the image library. The sample has no language picker yet. */
internal const val CONTENT_LANGUAGE_TAG = "en"

/**
 * The title of [passageId] in [bibleVersion], such as "John 3:16", or [passageId] itself until the version loads.
 */
internal fun passageTitle(
    bibleVersion: BibleVersion?,
    passageId: String,
): String =
    bibleVersion
        ?.reference(passageId)
        ?.let { bibleVersion.displayTitle(it, includesVersionAbbreviation = false) }
        ?: passageId

private const val PAGE_SIZE = 30
private const val MAXIMUM_QUERY_LENGTH = 100
private const val QUERY_KEY = "query"

private val String.characterCount: Int
    get() {
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
        iterator.setText(this)
        var count = 0
        while (iterator.next() != BreakIterator.DONE) count++
        return count
    }

/**
 * What the image library shows.
 *
 * @property query The submitted search, or empty when browsing.
 * @property images The loaded images, in server order without repeats.
 * @property bibleVersion The version used to title passages, or `null` until it loads.
 * @property isLoading Whether a page is loading.
 * @property hasError Whether the last page failed. Images already loaded stay.
 * @property hasMorePages Whether another page can be loaded.
 * @property isQueryTooLong Whether the last search was rejected for its length.
 * @property hasVersionError Whether the Bible version failed to load, so passages show their ids.
 */
internal data class ContentImagesState(
    val query: String = "",
    val images: List<VerseImage> = emptyList(),
    val bibleVersion: BibleVersion? = null,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val hasMorePages: Boolean = false,
    val isQueryTooLong: Boolean = false,
    val hasVersionError: Boolean = false,
)

/**
 * Pages through verse images, or through the images matching a search, for the Content tab.
 *
 * A search hit names only an image id, so each hit is fetched in full. Hits keep the order the server returned
 * them in, and a hit that fails to load is fetched again on [retry] without repeating the search. The submitted
 * search is kept in [savedStateHandle], so it survives process death.
 */
internal class ContentImagesViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val browsePage: suspend (pageToken: String?) -> Pair<List<VerseImage>, String?> = { pageToken ->
        YouVersionApi.verseImages
            .verseImages(languageRanges = listOf(CONTENT_LANGUAGE_TAG), pageSize = PAGE_SIZE, pageToken = pageToken)
            .let { it.data to it.nextPageToken }
    },
    private val searchPage: suspend (query: String, pageToken: String?) -> Pair<List<String>, String?> =
        { query, pageToken ->
            YouVersionApi.search
                .images(query = query, bibleId = BibleDefaults.VERSION_ID, pageSize = PAGE_SIZE, pageToken = pageToken)
                .let { results -> results.images.map { it.id } to results.nextPageToken }
        },
    private val imageDetail: suspend (imageId: String) -> VerseImage = { YouVersionApi.verseImages.verseImage(it) },
    private val bibleVersion: suspend () -> BibleVersion = { YouVersionApi.bible.version(BibleDefaults.VERSION_ID) },
) : ViewModel() {
    private val mutableState = MutableStateFlow(ContentImagesState(query = savedStateHandle[QUERY_KEY] ?: ""))
    val state: StateFlow<ContentImagesState> = mutableState.asStateFlow()

    private var loadJob: Job? = null
    private var versionJob: Job? = null
    private var nextPageToken: String? = null
    private var pendingHitIds: List<String>? = null
    private var pendingNextPageToken: String? = null
    private val hitIds = mutableListOf<String>()
    private val imagesById = mutableMapOf<String, VerseImage>()
    private val openedImagesById = mutableMapOf<String, VerseImage>()

    init {
        load()
    }

    /**
     * Replaces the images with those matching [query], or with every image when it is blank.
     */
    fun search(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.characterCount > MAXIMUM_QUERY_LENGTH) {
            loadJob?.cancel()
            mutableState.update { it.copy(isQueryTooLong = true, isLoading = false) }
            return
        }
        loadJob?.cancel()
        savedStateHandle[QUERY_KEY] = trimmedQuery
        nextPageToken = null
        pendingHitIds = null
        hitIds.clear()
        imagesById.clear()
        mutableState.update { ContentImagesState(query = trimmedQuery, bibleVersion = it.bibleVersion) }
        load()
    }

    /** Loads the next page, unless one is loading, the last one failed, or there are no more. */
    fun loadMore() {
        val current = state.value
        if (current.isLoading || current.hasError || !current.hasMorePages) return
        load()
    }

    /** Repeats the page that failed, and the Bible version if it failed. */
    fun retry() {
        if (state.value.hasError) load() else loadBibleVersion()
    }

    /**
     * The image with [imageId]: a loaded one, or else fetched, for a detail screen restored after process death.
     */
    suspend fun image(imageId: String): VerseImage =
        state.value.images.firstOrNull { it.id == imageId }
            ?: openedImagesById[imageId]
            ?: imageDetail(imageId).also { openedImagesById[imageId] = it }

    private fun load() {
        loadBibleVersion()
        loadJob?.cancel()
        mutableState.update { it.copy(isLoading = true, hasError = false, isQueryTooLong = false) }
        val query = state.value.query
        loadJob =
            viewModelScope.launch {
                try {
                    if (query.isEmpty()) loadBrowsePage() else loadSearchPage(query)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    ensureActive()
                    mutableState.update { it.copy(isLoading = false, hasError = true) }
                }
            }
    }

    private fun loadBibleVersion() {
        if (state.value.bibleVersion != null || versionJob?.isActive == true) return
        mutableState.update { it.copy(hasVersionError = false) }
        versionJob =
            viewModelScope.launch {
                try {
                    val version = bibleVersion()
                    mutableState.update { it.copy(bibleVersion = version) }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    mutableState.update { it.copy(hasVersionError = true) }
                }
            }
    }

    private suspend fun loadBrowsePage() {
        val (pageImages, pageToken) = browsePage(nextPageToken)
        currentCoroutineContext().ensureActive()
        nextPageToken = pageToken
        mutableState.update { current ->
            val loadedIds = current.images.mapTo(mutableSetOf()) { it.id }
            current.copy(
                images = current.images + pageImages.filter { loadedIds.add(it.id) },
                isLoading = false,
                hasMorePages = pageToken != null,
            )
        }
    }

    private suspend fun loadSearchPage(query: String) {
        val pageHitIds =
            pendingHitIds ?: searchPage(query, nextPageToken).let { (ids, pageToken) ->
                currentCoroutineContext().ensureActive()
                pendingHitIds = ids
                pendingNextPageToken = pageToken
                ids.filterNotTo(hitIds) { it in hitIds }
                ids
            }
        val resolvedImages =
            coroutineScope {
                pageHitIds
                    .distinct()
                    .filter { it !in imagesById }
                    .map { id ->
                        async {
                            try {
                                id to imageDetail(id)
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (_: Exception) {
                                id to null
                            }
                        }
                    }.awaitAll()
            }
        currentCoroutineContext().ensureActive()
        resolvedImages.forEach { (id, image) -> if (image != null) imagesById[id] = image }
        val images = hitIds.mapNotNull { imagesById[it] }
        if (resolvedImages.any { it.second == null }) {
            mutableState.update { it.copy(images = images, isLoading = false, hasError = true) }
            return
        }
        pendingHitIds = null
        nextPageToken = pendingNextPageToken
        mutableState.update {
            it.copy(images = images, isLoading = false, hasMorePages = pendingNextPageToken != null)
        }
    }
}
