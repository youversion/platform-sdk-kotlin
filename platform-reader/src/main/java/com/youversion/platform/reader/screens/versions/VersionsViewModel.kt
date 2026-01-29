package com.youversion.platform.reader.screens.versions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.organizations.models.Organization
import com.youversion.platform.reader.domain.BibleReaderRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VersionsViewModel(
    private val bibleVersionRepository: BibleVersionRepository,
    private val bibleReaderRepository: BibleReaderRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> by lazy { _state.asStateFlow() }

    init {
        loadVersions()
    }

    private fun loadVersions() {
        viewModelScope.launch {
            try {
                val deferredPermittedVersions = async { bibleReaderRepository.permittedVersionsListing() }
                val deferredActiveLanguageVersions =
                    async {
                        val chosenLanguage = _state.value.activeLanguageTag
                        bibleReaderRepository.fetchVersionsInLanguage(chosenLanguage)
                    }

                val permittedVersions = deferredPermittedVersions.await()
                val activeLanguageVersions = deferredActiveLanguageVersions.await()

                _state.update {
                    it.copy(
                        activeLanguageVersions = activeLanguageVersions,
                        permittedMinimalVersions = permittedVersions,
                    )
                }
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

            is Action.VersionDismissed -> {
                _state.update { it.copy(selectedBibleVersion = null, selectedOrganization = null) }
            }
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
        val permittedMinimalVersions: List<BibleVersion> = emptyList(),
        val activeLanguageVersions: List<BibleVersion> = emptyList(),
        val activeLanguageTag: String = "en",
        val showBibleVersionLoading: Boolean = false,
        val selectedBibleVersion: BibleVersion? = null,
        val selectedOrganization: Organization? = null,
        val searchQuery: String = "",
    ) {
        val versionsCount: Int
            get() = permittedMinimalVersions.count()
        val languagesCount: Int
            get() =
                permittedMinimalVersions
                    .distinctBy { it.languageTag }
                    .count()

        val showEmptyState: Boolean
            get() = !initializing && permittedMinimalVersions.isEmpty()

        val activeLanguageVersionsCount: Int
            get() =
                permittedMinimalVersions
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
