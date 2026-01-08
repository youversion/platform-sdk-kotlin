package com.youversion.platform.reader.screens.versions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.organizations.models.Organization
import com.youversion.platform.reader.domain.BibleReaderGlobalState
import com.youversion.platform.reader.domain.BibleReaderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VersionsViewModel(
    private val bibleVersionRepository: BibleVersionRepository,
    private val bibleReaderRepository: BibleReaderRepository,
    private val globalState: BibleReaderGlobalState,
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            State(
                permittedVersions = globalState.permittedVersions,
            ),
        )
    val state: StateFlow<State> by lazy { _state.asStateFlow() }

    init {
        viewModelScope.launch {
            globalState.state.collect { gState ->
                _state.update { lState ->
                    lState.copy(
                        activeLanguageTag = gState.activeLanguageTag,
                        permittedVersions = gState.permittedVersions,
                    )
                }
            }
        }

        loadVersionsList()
    }

    private fun loadVersionsList() {
        if (globalState.permittedVersions.isNotEmpty()) return

        viewModelScope.launch {
            try {
                val permittedVersions = bibleReaderRepository.loadVersionsList()
                _state.update { it.copy(permittedVersions = permittedVersions) }
            } catch (e: Exception) {
                Logger.e("Error loading versions", e)
            } finally {
                _state.update { it.copy(initializing = false) }
            }
        }
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.VersionInfoTapped -> {
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
        val activeLanguageTag: String = "en",
        val showBibleVersionLoading: Boolean = false,
        val selectedBibleVersion: BibleVersion? = null,
        val selectedOrganization: Organization? = null,
        val searchQuery: String = "",
    ) {
        val versionsCount: Int
            get() = permittedVersions.count()
        val languagesCount: Int
            get() =
                permittedVersions
                    .distinctBy { it.languageTag }
                    .count()

        val showEmptyState: Boolean
            get() = !initializing && permittedVersions.isEmpty()

        val filteredVersions: List<BibleVersion>
            get() {
                val language = activeLanguageTag
                // TODO: handle search text

                return permittedVersions
                    .filter { it.languageTag == language }
                // TODO: handle search text
            }
        val activeLanguageVersionsCount: Int
            get() =
                permittedVersions
                    .count { it.languageTag == activeLanguageTag }
    }

    // ----- Events
    sealed interface Event

    // ----- Actions
    sealed interface Action {
        data class VersionInfoTapped(
            val bibleVersion: BibleVersion,
        ) : Action

        data object VersionDismissed : Action
    }
}
