package com.youversion.platform.ui.signin

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import com.youversion.platform.ui.R

@Composable
fun SignOutConfirmationAlert(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.sign_out_confirmation_title))
        },
        text = {
            Text(text = stringResource(R.string.sign_out_confirmation_message))
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
                    text = stringResource(R.string.sign_out_confirm_button_text),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}
