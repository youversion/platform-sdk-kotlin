package com.youversion.platform.ui.signin

import android.app.Application
import android.content.Intent
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.InitializerViewModelFactoryBuilder
import androidx.lifecycle.viewmodel.initializer
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.core.highlights.domain.BibleHighlightsRepository
import com.youversion.platform.ui.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel(
    application: Application,
) : AndroidViewModel(application) {
    /**
     * Whether any highlight change has yet to reach the server, which decides which sign-out confirmation is shown.
     *
     * Evaluated on each sign-out request rather than held as a value: the answer changes as the user highlights, and the
     * Koin graph it reads from is not guaranteed to be configured when this view model is constructed.
     */
    @VisibleForTesting
    internal var hasUnsyncedHighlights: () -> Boolean = {
        PlatformKoinGraph.koinApplication.koin
            .get<BibleHighlightsRepository>()
            .hasPendingOperations()
    }
    private val _state = MutableStateFlow(SignInViewState())
    val state: StateFlow<SignInViewState> by lazy { _state.asStateFlow() }

    private val _events = Channel<Event>()
    val events = _events.receiveAsFlow()

    init {
        YouVersionPlatformConfiguration.configState
            .onEach { config ->
                _state.update {
                    it.copy(
                        isSignedIn = config?.isSignedIn == true,
                        isSignInEnabled = config?.isSignInEnabled != false,
                        userName = YouVersionApi.users.currentUserName,
                        userEmail = YouVersionApi.users.currentUserEmail,
                    )
                }
            }.launchIn(viewModelScope)
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.ProcessAuthCallback -> {
                handleProcessAuthCallback(action)
            }
            is Action.SignOut -> {
                handleSignOut(action)
            }
            is Action.CancelSignOut -> cancelSignOut()
            is Action.UpdateSignInState -> updateSignInState()
        }
    }

    private fun handleProcessAuthCallback(action: Action.ProcessAuthCallback) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isProcessing = true) }
                YouVersionAuthentication.handleAuthCallback(application, action.intent)
            } catch (_: Exception) {
                _events.send(Event.AuthenticationError)
            } finally {
                _state.update { it.copy(isProcessing = false) }
            }
        }
    }

    private fun handleSignOut(action: Action.SignOut) {
        if (action.requireConfirmation) {
            val confirmation =
                if (hasUnsyncedHighlights()) {
                    SignOutConfirmation.UNSYNCED_HIGHLIGHTS
                } else {
                    SignOutConfirmation.STANDARD
                }
            _state.update { it.copy(signOutConfirmation = confirmation) }
        } else {
            _state.update { it.copy(signOutConfirmation = null) }
            YouVersionApi.users.signOut()
        }
    }

    private fun cancelSignOut() {
        _state.update { it.copy(signOutConfirmation = null) }
    }

    private fun updateSignInState() {
        viewModelScope.launch {
            val hasValidToken = YouVersionApi.hasValidToken()
            if (!hasValidToken) {
                YouVersionPlatformConfiguration.clearAuthData()
            }
        }
    }

    /**
     * Which sign-out confirmation to present, and the copy that presents it. [UNSYNCED_HIGHLIGHTS] warns that highlight
     * changes have not reached the server yet and will be lost, so it is offered whenever such changes are outstanding.
     */
    enum class SignOutConfirmation(
        @StringRes internal val titleResourceId: Int,
        @StringRes internal val messageResourceId: Int,
        @StringRes internal val confirmButtonResourceId: Int,
    ) {
        STANDARD(
            titleResourceId = R.string.sign_out_confirmation_title,
            messageResourceId = R.string.sign_out_confirmation_message,
            confirmButtonResourceId = R.string.sign_out_confirm_button_text,
        ),
        UNSYNCED_HIGHLIGHTS(
            titleResourceId = R.string.sign_out_pending_highlights_title,
            messageResourceId = R.string.sign_out_pending_highlights_message,
            confirmButtonResourceId = R.string.sign_out_pending_highlights_confirm_button_text,
        ),
    }

    // ----- State
    data class SignInViewState(
        val isProcessing: Boolean = false,
        val isSignedIn: Boolean = false,
        val isSignInEnabled: Boolean = true,
        val userName: String? = null,
        val userEmail: String? = null,
        val signOutConfirmation: SignOutConfirmation? = null,
    )

    // ----- Events
    interface Event {
        data object AuthenticationError : Event
    }

    // ----- Actions
    sealed interface Action {
        data class ProcessAuthCallback(
            val intent: Intent,
        ) : Action

        data class SignOut(
            val requireConfirmation: Boolean = false,
        ) : Action

        data object CancelSignOut : Action

        data object UpdateSignInState : Action
    }

    // ----- Injection
    companion object {
        fun factory(): ViewModelProvider.Factory =
            InitializerViewModelFactoryBuilder()
                .apply {
                    initializer {
                        val application = checkNotNull(get(ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY))
                        SignInViewModel(application)
                    }
                }.build()
    }
}
