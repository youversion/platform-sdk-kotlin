package com.youversion.platform.ui.signin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class SignInViewModel : ViewModel() {
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
            } catch (_: CancellationException) {
                println("Login cancelled")
            } catch (_: Exception) {
                println("Login exception")
            } finally {
            }
        }
    }
}
