package com.youversion.platform.ui.signin

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.InitializerViewModelFactoryBuilder
import androidx.lifecycle.viewmodel.initializer
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
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
            _state.update { it.copy(showSignOutConfirmation = true) }
        } else {
            _state.update { it.copy(showSignOutConfirmation = false) }
            YouVersionApi.users.signOut()
        }
    }

    private fun cancelSignOut() {
        _state.update { it.copy(showSignOutConfirmation = false) }
    }

    private fun updateSignInState() {
        viewModelScope.launch {
            val hasValidToken = YouVersionApi.hasValidToken()
            if (!hasValidToken) {
                YouVersionPlatformConfiguration.clearAuthData()
            }
        }
    }

    // ----- State
    data class SignInViewState(
        val isProcessing: Boolean = false,
        val isSignedIn: Boolean = false,
        val userName: String? = null,
        val userEmail: String? = null,
        val showSignOutConfirmation: Boolean = false,
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
