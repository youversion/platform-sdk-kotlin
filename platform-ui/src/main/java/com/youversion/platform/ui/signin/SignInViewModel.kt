package com.youversion.platform.ui.signin

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.users.model.SignInWithYouVersion
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel : ViewModel() {
    private val _state = MutableStateFlow(SignInViewState())
    val state: StateFlow<SignInViewState> by lazy { _state.asStateFlow() }

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

    fun signIn(
        context: Context,
        vararg permissions: SignInWithYouVersionPermission,
    ) {
        viewModelScope.launch {
            try {
                YouVersionAuthentication.signIn(
                    context = context,
                    permissions = permissions.toSet(),
                )
            } catch (_: Exception) {
                // TODO inform UI of error
            }
        }
    }

    fun handleAuthCallback(
        context: Context,
        intent: Intent,
    ) {
        viewModelScope.launch {
            try {
                YouVersionAuthentication.handleAuthCallback(context, intent)
            } catch (_: Exception) {
                // TODO inform UI of error
            }
        }
    }

    fun cancelAuthentication(context: Context) {
        YouVersionAuthentication.cancelAuthentication(context)
    }

    fun isAuthenticationInProgress(context: Context): Boolean =
        YouVersionAuthentication.isAuthenticationInProgress(context)

    fun signOut() {
        YouVersionApi.users.signOut()
    }
}

data class SignInViewState(
    val isSignedIn: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null,
)
