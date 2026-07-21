package com.youversion.platform.ui.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.youversion.platform.ui.R
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.views.components.BibleAppLogo

/**
 * A sheet that asks the reader to sign in with YouVersion, explaining which app is asking and why before any
 * browser opens.
 *
 * Shown at the moment an action needs an account rather than on entry, so a reader who only wanted to read is
 * never interrupted.
 *
 * @param appName The name of the host app, shown in the explanation.
 * @param onSignIn Called when the reader agrees to sign in.
 * @param onDismissRequest Called when the reader declines, or dismisses the sheet.
 * @param appSignInMessage The host app's own reason for asking. Omitted when null or blank.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInWithYouVersionPromptSheet(
    appName: String,
    onSignIn: () -> Unit,
    onDismissRequest: () -> Unit,
    appSignInMessage: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.sign_in_prompt_introducing),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            BibleAppLogo()

            if (!appSignInMessage.isNullOrBlank()) {
                Text(
                    text = appSignInMessage,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = stringResource(R.string.sign_in_prompt_paragraph, appName),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            SignInPromptButton(
                text = stringResource(R.string.sign_in_prompt_yes_button),
                isPrimary = true,
                onClick = onSignIn,
            )

            SignInPromptButton(
                text = stringResource(R.string.sign_in_prompt_no_button),
                isPrimary = false,
                onClick = onDismissRequest,
            )
        }
    }
}

@Composable
private fun SignInPromptButton(
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface

    Button(
        onClick = onClick,
        shape = SignInWithYouVersionButtonDefaults.capsuleShape,
        border =
            BorderStroke(
                width = 1.dp,
                color = if (isPrimary) contentColor else MaterialTheme.colorScheme.outline,
            ),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = contentColor,
            ),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SignInWithYouVersionPromptSheetPreview() {
    BibleReaderMaterialTheme {
        SignInWithYouVersionPromptSheet(
            appName = "Sample App",
            onSignIn = {},
            onDismissRequest = {},
            appSignInMessage = "Keep your highlights across devices",
        )
    }
}
