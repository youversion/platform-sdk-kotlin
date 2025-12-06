package com.youversion.platform.ui.signin

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.youversion.platform.ui.R

/**
 * A reusable alert dialog for displaying a generic sign-in error.
 *
 * @param onDismissRequest Lambda invoked when the user tries to dismiss the dialog,
 *                         either by clicking outside or pressing the back button.
 * @param onConfirm Lambda invoked when the user clicks the confirmation ("OK") button.
 */
@Composable
fun SignInErrorAlert(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.sign_in_error_title))
        },
        text = {
            Text(text = stringResource(R.string.sign_in_error_message))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}
