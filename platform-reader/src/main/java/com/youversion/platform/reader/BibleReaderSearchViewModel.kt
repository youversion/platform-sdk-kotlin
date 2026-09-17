package com.youversion.platform.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Owns everything about a search run from the reader, scoped to the reader composable that creates it. */
internal class BibleReaderSearchViewModel : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onAction(action: Action) {
        when (action) {
            is Action.OpenSearch -> {
                abandonSearch()
                _state.update {
                    it.copy(
                        query = "",
                        status = SearchStatus.IDLE,
                        results = emptyList(),
                        searchVersion = action.bibleVersion,
                    )
                }
            }

            is Action.SetQuery -> {
                abandonSearch()
                _state.update {
                    it.copy(query = action.query, status = SearchStatus.IDLE, results = emptyList())
                }
            }

            is Action.Submit -> search()
        }
    }

    /** Drops whatever search is running. The reader asked for this, so it is not a failure. */
    private fun abandonSearch() {
        searchJob?.cancel()
        searchJob = null
    }

    private fun search() {
        val query = _state.value.query.trim()
        val version = _state.value.searchVersion

        if (query.isEmpty() || version == null) {
            abandonSearch()
            _state.update { it.copy(status = SearchStatus.IDLE, results = emptyList()) }
            return
        }

        // A completed status means the results for this exact query and version are still on
        // screen, since changing either returns the status to idle. Asking again would only buy
        // the reader a wait for an answer they are already looking at.
        if (_state.value.status == SearchStatus.COMPLETED) return

        abandonSearch()
        _state.update { it.copy(status = SearchStatus.SEARCHING, results = emptyList()) }

        searchJob =
            viewModelScope.launch {
                try {
                    val found = YouVersionApi.search.verses(query = query, bibleId = version.id)
                    _state.update {
                        it.copy(status = SearchStatus.COMPLETED, results = found.references)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e("Error searching for \"$query\"", e)
                    _state.update { it.copy(status = SearchStatus.FAILED) }
                }
            }
    }

    enum class SearchStatus { IDLE, SEARCHING, COMPLETED, FAILED }

    // ----- State
    data class State(
        val query: String = "",
        val status: SearchStatus = SearchStatus.IDLE,
        val results: List<BibleReference> = emptyList(),
        val searchVersion: BibleVersion? = null,
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
    }
}
