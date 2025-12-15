package com.youversion.platform.ui.signin

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A data class to hold the parameters required to launch the sign-in flow.
 *
 * @param context The Android context needed to launch the Custom Tab.
 * @param launcher The ActivityResultLauncher that will handle the AuthTabIntent result.
 * @param permissions The set of permissions to request from the user.
 */
data class SignInParameters(
    val context: Context,
    val launcher: ActivityResultLauncher<Intent>,
    val permissions: Set<SignInWithYouVersionPermission>,
)

/**
 * A reusable Composable effect that provides a stable launcher function to initiate
 * the YouVersion sign-in flow.
 *
 * This encapsulates the coroutine launching and error handling for the sign-in
 * action, simplifying the call site.
 *
 * @param onSignInError A lambda to be invoked if an exception occurs while trying
 *                      to launch the sign-in flow.
 * @return A stable lambda function `(SignInParameters) -> Unit` that can be called
 *         to start the authentication process.
 */
@Composable
fun rememberSignInWithYouVersion(onSignInError: () -> Unit): (SignInParameters) -> Unit {
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    return remember {
        { params: SignInParameters ->
            coroutineScope.launch {
                try {
                    YouVersionAuthentication.signIn(
                        context = params.context,
                        launcher = params.launcher,
                        permissions = params.permissions,
                    )
                } catch (_: Exception) {
                    onSignInError()
                }
            }
        }
    }
}

/**
 * A reusable Composable effect that creates and remembers an ActivityResultLauncher
 * specifically for the YouVersion authentication flow.
 *
 * @param onResult A lambda that will be invoked with the callback Intent when the
 *                 authentication flow successfully returns a result.
 * @return An `ActivityResultLauncher<Intent>` that can be passed to the sign-in function.
 */
@Composable
fun rememberYouVersionAuthLauncher(onResult: (intent: Intent) -> Unit): ActivityResultLauncher<Intent> =
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        result.data?.let(onResult)
    }
