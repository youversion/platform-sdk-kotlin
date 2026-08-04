package com.youversion.platform.ui.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.ui.R
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.theme.fonts.AktivGrotesk
import com.youversion.platform.ui.theme.readerColorScheme

/**
 * A sheet that asks the reader to sign in with YouVersion, explaining which app is asking and why before any
 * browser opens.
 *
 * Shown at the moment an action needs an account rather than on entry, so a reader who only wanted to read is
 * never interrupted.
 *
 * The app the sheet names and the host app's own reason for asking come from
 * [YouVersionPlatformConfiguration.appName] and [YouVersionPlatformConfiguration.signInPromptMessage]. A host that
 * configures no app name is named by its launcher label instead, so the reader is never asked to grant account
 * access to an app the sheet leaves unnamed.
 *
 * @param onSignIn Called when the reader agrees to sign in.
 * @param onDismissRequest Called when the reader declines, or dismisses the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInWithYouVersionPromptSheet(
    onSignIn: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val config by YouVersionPlatformConfiguration.configState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hostAppLabel =
        remember(context) { context.applicationInfo.loadLabel(context.packageManager).toString() }
    val appName = config?.appName?.takeIf { it.isNotBlank() } ?: hostAppLabel
    val signInPromptMessage = config?.signInPromptMessage

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 72.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.sign_in_prompt_introducing),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = AktivGrotesk,
                fontSize = 10.sp,
                letterSpacing = 0.16.em,
            )

            Image(
                imageVector =
                    ImageVector.vectorResource(
                        if (MaterialTheme.readerColorScheme.isDark) {
                            R.drawable.yv_platform_dm
                        } else {
                            R.drawable.yv_platform_lm
                        },
                    ),
                contentDescription = null,
                modifier = Modifier.height(20.dp),
            )

            if (!signInPromptMessage.isNullOrBlank()) {
                Text(
                    text = boldMarkdownAnnotatedString(signInPromptMessage),
                    fontFamily = AktivGrotesk,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = boldMarkdownAnnotatedString(stringResource(R.string.sign_in_prompt_paragraph, appName)),
                fontFamily = AktivGrotesk,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
}

@Composable
private fun SignInPromptButton(
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val buttonColor =
        if (MaterialTheme.readerColorScheme.isDark) {
            MaterialTheme.readerColorScheme.readerWhiteColor
        } else {
            MaterialTheme.readerColorScheme.buttonSecondaryColor
        }

    Button(
        onClick = onClick,
        shape = SignInWithYouVersionButtonDefaults.capsuleShape,
        border =
            if (!isPrimary) {
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                null
            },
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (!isPrimary) Color.Transparent else buttonColor,
                contentColor = contentColor,
            ),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color =
                if (!isPrimary) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.readerColorScheme.readerBlackColor
                },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SignInWithYouVersionPromptSheetPreview() {
    BibleReaderMaterialTheme {
        SignInWithYouVersionPromptSheet(
            onSignIn = {},
            onDismissRequest = {},
        )
    }
}
