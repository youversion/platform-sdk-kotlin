package com.youversion.platform.reader

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Owns everything about a search run from the reader, scoped to the reader composable that creates it. */
internal class BibleReaderSearchViewModel : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun onAction(action: Action) {
        when (action) {
            is Action.OpenSearch -> {
                _state.update { it.copy(query = "") }
            }

            is Action.SetQuery -> {
                _state.update { it.copy(query = action.query) }
            }
        }
    }

    // ----- State
    data class State(
        val query: String = "",
    )

    // ----- Actions
    sealed interface Action {
        /** Start a search over, so nothing from the last one is on screen when the sheet rises. */
        data object OpenSearch : Action

        /** Record what the reader has typed so far. */
        data class SetQuery(
            val query: String,
        ) : Action
    }
}
