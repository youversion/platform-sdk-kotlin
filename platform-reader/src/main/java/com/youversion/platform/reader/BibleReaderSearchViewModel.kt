package com.youversion.platform.reader

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.ui.views.rendering.BibleVersionRendering
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** Owns everything about a search run from the reader, scoped to the reader composable that creates it. */
internal class BibleReaderSearchViewModel : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var searchJob: Job? = null

    private var nextPageJob: Job? = null

    /**
     * Resolved on each text fetch rather than held as a value: the Koin graph it reads from is not guaranteed to be
     * configured when this view model is constructed.
     */
    @VisibleForTesting
    internal var bibleChapterRepository: () -> BibleChapterRepository = {
        PlatformKoinGraph.koinApplication.koin.get()
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.OpenSearch -> {
                abandonSearch()
                _state.update {
                    it
                        .copy(
                            query = "",
                            status = SearchStatus.IDLE,
                            searchVersion = action.bibleVersion,
                        ).withoutResults()
                }
            }

            is Action.SetQuery -> {
                abandonSearch()
                _state.update {
                    it.copy(query = action.query, status = SearchStatus.IDLE).withoutResults()
                }
            }

            is Action.Submit -> search()

            is Action.LoadResultText -> loadResultText(action.reference)

            is Action.LoadNextPage -> loadNextPage()
        }
    }

    /**
     * Drops whatever search is running, along with any page being read on top of it. The reader asked for this, so
     * it is not a failure.
     */
    private fun abandonSearch() {
        searchJob?.cancel()
        searchJob = null
        nextPageJob?.cancel()
        nextPageJob = null
    }

    private fun search() {
        val query = _state.value.query.trim()
        val version = _state.value.searchVersion

        if (query.isEmpty() || version == null) {
            abandonSearch()
            _state.update { it.copy(status = SearchStatus.IDLE).withoutResults() }
            return
        }

        // A completed status means the results for this exact query and version are still on
        // screen, since changing either returns the status to idle. Asking again would only buy
        // the reader a wait for an answer they are already looking at.
        if (_state.value.status == SearchStatus.COMPLETED) return

        abandonSearch()
        _state.update { it.copy(status = SearchStatus.SEARCHING).withoutResults() }

        searchJob =
            viewModelScope.launch {
                try {
                    val found = YouVersionApi.search.verses(query = query, bibleId = version.id)
                    _state.update {
                        it.copy(
                            status = SearchStatus.COMPLETED,
                            results = found.references,
                            resultSetId = UUID.randomUUID(),
                            nextPageToken = found.nextPageToken,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e("Error searching for \"$query\"", e)
                    _state.update { it.copy(status = SearchStatus.FAILED) }
                }
            }
    }

    /**
     * Reads the page that follows what is already listed, which the list asks for as the reader nears the end of it.
     *
     * An ask that arrives while a page is already out is dropped rather than queued: the request already running
     * will deliver that same page. Results are de-duplicated by passage id, since the platform can return one
     * result on either side of a page boundary and the list is keyed by that id. A page that will not load raises
     * a retry the reader can tap, the query no longer being the obvious way to ask again.
     */
    private fun loadNextPage() {
        val state = _state.value
        val query = state.query.trim()
        val version = state.searchVersion
        val pageToken = state.nextPageToken

        if (state.status != SearchStatus.COMPLETED || state.isLoadingNextPage) return
        if (version == null || pageToken.isNullOrEmpty()) return

        _state.update { it.copy(isLoadingNextPage = true, hasNextPageLoadError = false) }

        nextPageJob =
            viewModelScope.launch {
                try {
                    val found =
                        YouVersionApi.search.verses(
                            query = query,
                            bibleId = version.id,
                            pageToken = pageToken,
                        )
                    _state.update { current ->
                        val listedPassageIds = current.results.mapTo(mutableSetOf()) { it.asUSFM }
                        current.copy(
                            results = current.results + found.references.filter { listedPassageIds.add(it.asUSFM) },
                            nextPageToken = found.nextPageToken,
                            isLoadingNextPage = false,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e("Error loading the next page of results for \"$query\"", e)
                    _state.update { it.copy(isLoadingNextPage = false, hasNextPageLoadError = true) }
                }
            }
    }

    /**
     * Fills in one result's own verse text, fetched chapter-granularly so a screenful of results drawn from one
     * chapter costs the reader a single fetch.
     *
     * The passage is entered with no text of its own before the fetch starts, which is what a row that has already
     * asked is recognised by: a row scrolled out and back neither asks a second time nor asks again for a chapter
     * that has already failed. A result whose text cannot be read is left at that entry and so keeps its title and
     * says nothing, there being nothing here for the reader to act on.
     *
     * Text belonging to a result set the reader has already moved on from is dropped rather than written under
     * results it was never fetched for.
     */
    private fun loadResultText(reference: BibleReference) {
        val passageId = reference.asUSFM
        val resultSetId = _state.value.resultSetId ?: return
        if (_state.value.resultTextByPassageId.containsKey(passageId)) return

        _state.update { it.copy(resultTextByPassageId = it.resultTextByPassageId + (passageId to null)) }

        viewModelScope.launch {
            val text =
                try {
                    BibleVersionRendering.plainTextOf(bibleChapterRepository(), reference)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e("Error reading the text of $passageId", e)
                    null
                } ?: return@launch

            _state.update {
                if (it.resultSetId != resultSetId) {
                    it
                } else {
                    it.copy(resultTextByPassageId = it.resultTextByPassageId + (passageId to text.trim()))
                }
            }
        }
    }

    enum class SearchStatus { IDLE, SEARCHING, COMPLETED, FAILED }

    // ----- State
    data class State(
        val query: String = "",
        val status: SearchStatus = SearchStatus.IDLE,
        val results: List<BibleReference> = emptyList(),
        val resultTextByPassageId: Map<String, String?> = emptyMap(),
        val searchVersion: BibleVersion? = null,
        val resultSetId: UUID? = null,
        val nextPageToken: String? = null,
        val isLoadingNextPage: Boolean = false,
        val hasNextPageLoadError: Boolean = false,
    )

    /**
     * The state with the last run's results, their text, the identity they were fetched under and everywhere paging
     * through them had reached all let go.
     */
    private fun State.withoutResults(): State =
        copy(
            results = emptyList(),
            resultTextByPassageId = emptyMap(),
            resultSetId = null,
            nextPageToken = null,
            isLoadingNextPage = false,
            hasNextPageLoadError = false,
        )

    // ----- Actions
    sealed interface Action {
        /** Start a search over against [bibleVersion], so nothing from the last one is on screen. */
        data class OpenSearch(
            val bibleVersion: BibleVersion?,
        ) : Action

        /** Record what the reader has typed so far. */
        data class SetQuery(
            val query: String,
        ) : Action

        /** Search for what is in the field. */
        data object Submit : Action

        /** Ask for [reference]'s own verse text, which a result row does as it comes into view. */
        data class LoadResultText(
            val reference: BibleReference,
        ) : Action

        /** Ask for the page that follows the results already listed, which the list does as its end nears. */
        data object LoadNextPage : Action
    }
}
