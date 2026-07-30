package com.youversion.platform.ui.signin

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import com.youversion.platform.ui.R

/**
 * Asks the user to confirm signing out. Pass [confirmation] to choose between the standard prompt and the one warning
 * that unsynced highlight changes will be lost.
 */
@Composable
fun SignOutConfirmationAlert(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    confirmation: SignInViewModel.SignOutConfirmation = SignInViewModel.SignOutConfirmation.STANDARD,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(confirmation.titleResourceId))
        },
        text = {
            Text(text = stringResource(confirmation.messageResourceId))
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.sign_out_cancel_button_text))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text(
                    text = stringResource(confirmation.confirmButtonResourceId),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}
