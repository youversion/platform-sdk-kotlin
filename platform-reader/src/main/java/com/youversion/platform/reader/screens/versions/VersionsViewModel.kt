package com.youversion.platform.reader.screens.versions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.InitializerViewModelFactoryBuilder
import androidx.lifecycle.viewmodel.initializer
import co.touchlab.kermit.Logger
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VersionsViewModel(
    bibleVersion: BibleVersion?,
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            State(
                chosenLanguageTag = bibleVersion?.languageTag ?: "en",
            ),
        )
    val state: StateFlow<State> by lazy { _state.asStateFlow() }

    init {
        loadVersionsList()
    }

    private fun loadVersionsList() {
        viewModelScope.launch {
            try {
                // TODO: proper pagination
                val versions = YouVersionApi.bible.versions(pageSize = 99)
                val deduplicated =
                    versions
                        .sortedBy { it.id }
                        .fold(mutableListOf<BibleVersion>()) { acc, version ->
                            if (acc.none { it.id == version.id }) {
                                acc.add(version)
                            }

                            acc
                        }.toList()

                val sorted = deduplicated.sortedBy { it.title?.lowercase() }
                _state.update { it.copy(permittedVersions = sorted) }
            } catch (e: Exception) {
                Logger.e("Error loading versions", e)
            } finally {
                _state.update { it.copy(initializing = false) }
            }
        }
    }

    // ----- State
    data class State(
        val initializing: Boolean = true,
        val permittedVersions: List<BibleVersion> = emptyList(),
        val chosenLanguageTag: String,
        val searchQuery: String = "",
    ) {
        val versionsCount: Int
            get() = permittedVersions.count()

        val languagesCount: Int
            get() = permittedVersions.distinctBy { it.languageTag }.count()

        val showEmptyState: Boolean
            get() = !initializing && permittedVersions.isEmpty()

        val filteredVersions: List<BibleVersion>
            get() {
                return permittedVersions.filter { it.languageTag == chosenLanguageTag }
            }
    }

    // ----- Events
    sealed interface Event

    // ----- Actions
    sealed interface Action

    // ----- Injection
    companion object {
        fun factory(bibleVersion: BibleVersion?): ViewModelProvider.Factory =
            InitializerViewModelFactoryBuilder()
                .apply {
                    initializer {
                        VersionsViewModel(
                            bibleVersion = bibleVersion,
                        )
                    }
                }.build()
    }
}
