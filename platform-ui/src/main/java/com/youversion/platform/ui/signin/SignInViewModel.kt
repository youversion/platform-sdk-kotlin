package com.youversion.platform.ui.signin

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.InitializerViewModelFactoryBuilder
import androidx.lifecycle.viewmodel.initializer
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.users.model.SignInWithYouVersion
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
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
                        isSignedIn = config?.accessToken != null,
                        userName = SignInWithYouVersion.currentUserName,
                        userEmail = SignInWithYouVersion.currentUserEmail,
                    )
                }
            }.launchIn(viewModelScope)
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.SignIn -> {
                handleSignIn(action)
            }
            is Action.ProcessAuthCallback -> {
                handleProcessAuthCallback(action)
            }
            is Action.CancelAuthentication -> {
                handleCancelAuthentication()
            }
            is Action.SignOut -> {
                handleSignOut()
            }
        }
    }

    private fun handleSignIn(action: Action.SignIn) {
        viewModelScope.launch {
            try {
                YouVersionAuthentication.signIn(
                    context = action.context,
                    permissions = action.permissions,
                )
            } catch (_: Exception) {
                _events.send(Event.SignInError)
            }
        }
    }

    private fun handleProcessAuthCallback(action: Action.ProcessAuthCallback) {
        viewModelScope.launch {
            try {
                YouVersionAuthentication.handleAuthCallback(application, action.intent)
            } catch (_: Exception) {
                _events.send(Event.AuthenticationError)
            }
        }
    }

    private fun handleCancelAuthentication() {
        YouVersionAuthentication.cancelAuthentication(application)
    }

    private fun handleSignOut() {
        YouVersionApi.users.signOut()
    }

    // ----- State
    data class SignInViewState(
        val isSignedIn: Boolean = false,
        val userName: String? = null,
        val userEmail: String? = null,
    )

    // ----- Events
    interface Event {
        data object SignInError : Event

        data object AuthenticationError : Event
    }

    // ----- Actions
    sealed interface Action {
        data class SignIn(
            val context: Context,
            val permissions: Set<SignInWithYouVersionPermission>,
        ) : Action

        data class ProcessAuthCallback(
            val intent: Intent,
        ) : Action

        data object CancelAuthentication : Action

        data object SignOut : Action
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
