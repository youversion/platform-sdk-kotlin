package com.youversion.platform.ui.views.versions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.languages.domain.LanguageRepository
import com.youversion.platform.core.organizations.models.Organization
import com.youversion.platform.ui.views.components.LanguageRowItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @param onVersionChange called when the user has chosen a new version (or their first). The caller
 *   should ensure their current reference exists in this new version and choose a new one if not.
 */
class BibleVersionsViewModel(
    initialVersionId: Int? = null,
    var onVersionChange: (BibleVersion) -> Unit,
    private val languageRepository: LanguageRepository,
    private val bibleVersionRepository: BibleVersionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> by lazy { _state.asStateFlow() }

    init {
        viewModelScope.launch {
            loadVersion(versionId = initialVersionId)
        }
        loadVersions()
    }

    private suspend fun loadVersion(versionId: Int?) {
        var loadedVersion: BibleVersion? = null
        if (versionId != null) {
            try {
                loadedVersion = bibleVersionRepository.version(id = versionId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("Error loading default version", e)
            }
        }
        if (loadedVersion != null) {
            setCurrentVersion(loadedVersion)
        } else {
            selectFallbackVersion()
        }
    }

    private fun setCurrentVersion(version: BibleVersion) {
        _state.update { it.copy(currentVersion = version) }
        onVersionChange(version)
    }

    private suspend fun selectFallbackVersion() {
        val fallbackId = acceptableFallbackVersionId()
        val version =
            fallbackId?.let {
                try {
                    bibleVersionRepository.version(id = it)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
            }
        if (version == null) {
            // TODO: navigate to the VersionsScreen so user can select a version.
            return
        }
        setCurrentVersion(version)
    }

    private suspend fun acceptableFallbackVersionId(): Int? {
        val downloads = bibleVersionRepository.downloadedVersions
        if (downloads.isNotEmpty()) {
            return downloads.first()
        }

        val versions =
            try {
                bibleVersionRepository.permittedVersionsListing()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("Could not fetch the permitted versions.", e)
                return null
            }

        versions.firstOrNull { it.languageTag == "en" }?.let { return it.id }
        return versions.firstOrNull()?.id
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
                            Result.success(bibleVersionRepository.permittedVersionsListing())
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
                            Result.success(bibleVersionRepository.fullVersions(chosenLanguage))
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
                _state.update {
                    it.copy(
                        initializing = true,
                        activeLanguageTag = languageTag,
                    )
                }
                val versions = bibleVersionRepository.fullVersions(languageTag)
                val languageName = languageRepository.languageName(languageTag)
                _state.update {
                    it.copy(
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

            is Action.VersionSelected -> {
                setCurrentVersion(action.bibleVersion)
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

    internal fun loadLanguages() {
        if (_state.value.allLanguages.isNotEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(languagesInitializing = true) }
            try {
                languageRepository.loadLanguageNames(null)

                val allPermittedLanguageTags = languageRepository.allPermittedLanguageTags()
                val allLanguages =
                    allPermittedLanguageTags
                        .map { tag ->
                            LanguageRowItem(
                                languageTag = tag,
                                displayName = languageRepository.languageName(tag),
                                localeDisplayName = null,
                            )
                        }

                val suggestedLanguageTags = languageRepository.suggestedLanguageTags()
                val suggestedLanguages =
                    suggestedLanguageTags
                        .map { tag ->
                            LanguageRowItem(
                                languageTag = tag,
                                displayName = languageRepository.languageName(tag),
                                localeDisplayName = null,
                            )
                        }

                _state.update {
                    it.copy(
                        suggestedLanguages = suggestedLanguages,
                        allLanguages = allLanguages,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("Failed to get languages", e)
            } finally {
                _state.update { it.copy(languagesInitializing = false) }
            }
        }
    }

    // ----- State
    data class State(
        val initializing: Boolean = true,
        val currentVersion: BibleVersion? = null,
        val permittedMinimalVersions: List<BibleVersion> = emptyList(),
        val activeLanguageVersions: List<BibleVersion> = emptyList(),
        val activeLanguageTag: String = "en",
        val activeLanguageName: String = "English",
        val showBibleVersionLoading: Boolean = false,
        val selectedBibleVersion: BibleVersion? = null,
        val selectedOrganization: Organization? = null,
        val searchQuery: String = "",
        val suggestedLanguages: List<LanguageRowItem> = emptyList(),
        val allLanguages: List<LanguageRowItem> = emptyList(),
        val languagesInitializing: Boolean = false,
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

        data class VersionSelected(
            val bibleVersion: BibleVersion,
        ) : Action
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
