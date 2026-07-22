package com.youversion.platform.reader.sheets

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.youversion.platform.reader.R

/**
 * Asks a signed-in reader to allow this app to save highlights before the browser grant flow opens.
 *
 * Extracted as its own composable to match the reader's other confirmation dialogs rather than being inlined, and
 * because its copy lives in the reader string resources.
 *
 * @param onConfirm Called when the reader agrees to continue to the grant flow.
 * @param onDismiss Called when the reader declines or dismisses the dialog.
 */
@Composable
internal fun DataExchangeConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.data_exchange_highlights_question)) },
        text = { Text(stringResource(R.string.data_exchange_highlights_explanation)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.data_exchange_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
