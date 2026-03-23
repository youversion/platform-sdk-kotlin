package com.youversion.platform.reader.screens.versions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.organizations.models.Organization
import com.youversion.platform.reader.domain.BibleReaderRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VersionsViewModel(
    private val bibleReaderRepository: BibleReaderRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> by lazy { _state.asStateFlow() }

    init {
        loadVersions()
    }

    /**
     * Loads permitted and active-language listings concurrently. Each [async] block catches its own
     * exceptions so the [kotlinx.coroutines.Deferred] always completes successfully with a [Result],
     * guaranteeing both requests run to completion regardless of individual failures. When both fail,
     * the active-language error is attached via [Throwable.addSuppressed] so logging retains both
     * causes. State is only updated when both succeed.
     */
    private fun loadVersions() {
        viewModelScope.launch {
            try {
                val deferredPermittedVersions =
                    async {
                        try {
                            Result.success(bibleReaderRepository.permittedVersionsListing())
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                val deferredActiveLanguageVersions =
                    async {
                        try {
                            val chosenLanguage = _state.value.activeLanguageTag
                            Result.success(bibleReaderRepository.fetchVersionsInLanguage(chosenLanguage))
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }

                val permittedResult = deferredPermittedVersions.await()
                val activeResult = deferredActiveLanguageVersions.await()

                if (permittedResult.isFailure || activeResult.isFailure) {
                    combineConcurrentLoadFailures(permittedResult, activeResult)
                }

                _state.update {
                    it.copy(
                        activeLanguageVersions = activeResult.getOrThrow(),
                        permittedMinimalVersions = permittedResult.getOrThrow(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("Error loading versions", e)
            } finally {
                _state.update { it.copy(initializing = false) }
            }
        }
    }

    fun loadVersionsForLanguage(languageTag: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(initializing = true) }
                val versions = bibleReaderRepository.fetchVersionsInLanguage(languageTag)
                val languageName = bibleReaderRepository.languageName(languageTag)
                _state.update {
                    it.copy(
                        activeLanguageTag = languageTag,
                        activeLanguageVersions = versions,
                        activeLanguageName = languageName,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("Error loading versions for language $languageTag", e)
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
                } catch (e: CancellationException) {
                    throw e
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
        val activeLanguageName: String = "English",
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

/**
 * Throws the permitted-list failure when present, otherwise the active-language failure. When both
 * [Result]s fail, the active-language exception is [Throwable.addSuppressed] on the primary so callers
 * (for example [Logger]) see both concurrent errors.
 */
internal fun combineConcurrentLoadFailures(
    permittedResult: Result<List<BibleVersion>>,
    activeResult: Result<List<BibleVersion>>,
): Nothing {
    val primary = permittedResult.exceptionOrNull() ?: activeResult.exceptionOrNull()!!
    if (permittedResult.isFailure && activeResult.isFailure) {
        activeResult.exceptionOrNull()?.let { secondary ->
            if (secondary !== primary) {
                primary.addSuppressed(secondary)
            }
        }
    }
    throw primary
}
