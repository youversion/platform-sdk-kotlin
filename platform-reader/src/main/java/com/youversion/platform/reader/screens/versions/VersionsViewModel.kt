package com.youversion.platform.reader.screens.versions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.InitializerViewModelFactoryBuilder
import androidx.lifecycle.viewmodel.initializer
import co.touchlab.kermit.Logger
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.organizations.models.Organization
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VersionsViewModel(
    bibleVersion: BibleVersion?,
    private val bibleVersionRepository: BibleVersionRepository,
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
                val allVersions = bibleVersionRepository.allVersions()
                val deduplicated =
                    allVersions
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

    fun onAction(action: Action) {
        when (action) {
            is Action.VersionTapped -> {
                loadOrganization(bibleVersion = action.bibleVersion)
                _state.update { it.copy(selectedBibleVersion = action.bibleVersion) }
            }
            is Action.VersionDismissed ->
                _state.update { it.copy(selectedBibleVersion = null, selectedOrganization = null) }
        }
    }

    private fun loadOrganization(bibleVersion: BibleVersion) {
        viewModelScope.launch {
            bibleVersion.organizationId?.let {
                try {
                    val org = YouVersionApi.organizations.organization(it)
                    _state.update { it.copy(selectedOrganization = org) }
                } catch (e: Exception) {
                    Logger.e("Failed to get org", e)
                }
            }
        }
    }

    // ----- State
    data class State(
        val initializing: Boolean = true,
        val permittedVersions: List<BibleVersion> = emptyList(),
        val chosenLanguageTag: String,
        val showBibleVersionSheet: Boolean = false,
        val showBibleVersionLoading: Boolean = false,
        val selectedBibleVersion: BibleVersion? = null,
        val selectedOrganization: Organization? = null,
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
    sealed interface Action {
        data class VersionTapped(
            val bibleVersion: BibleVersion,
        ) : Action

        data object VersionDismissed : Action
    }

    // ----- Injection
    companion object {
        fun factory(
            bibleVersion: BibleVersion?,
            context: Context,
        ): ViewModelProvider.Factory =
            InitializerViewModelFactoryBuilder()
                .apply {
                    initializer {
                        VersionsViewModel(
                            bibleVersion = bibleVersion,
                            bibleVersionRepository = BibleVersionRepository(context),
                        )
                    }
                }.build()
    }
}
