package com.youversion.platform.ui.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.ui.R
import com.youversion.platform.ui.signin.SignInErrorAlert
import com.youversion.platform.ui.signin.rememberSignIn
import kotlinx.coroutines.launch

enum class SignInWithYouVersionButtonMode {
    FULL,
    COMPACT,
    ICON_ONLY,
}

object SignInWithYouVersionButtonDefaults {
    val capsuleShape: Shape
        @Composable get() = RoundedCornerShape(percent = 100)

    val rectangleShape: Shape
        @Composable get() = RoundedCornerShape(4.dp)

    val mode: SignInWithYouVersionButtonMode
        get() = SignInWithYouVersionButtonMode.FULL

    val padding: PaddingValues
        get() = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
}

/**
 * A YouVersion-branded sign-in button that handles the full authentication flow internally.
 *
 * The button uses [rememberSignIn] to launch the sign-in flow when tapped, manages its own
 * processing state, and displays an error alert on failure.
 *
 * @param permissions The set of permissions to request from the user.
 * @param paddingValues Content padding for the button.
 * @param shape The button shape.
 * @param mode The display mode controlling which text is shown.
 * @param stroked Whether to show a border stroke.
 * @param dark Whether to use dark-mode colors.
 * @param onTap Optional tap callback. When non-null the built-in auth flow is bypassed.
 */
@Composable
fun SignInWithYouVersionButton(
    permissions: () -> Set<SignInWithYouVersionPermission>,
    paddingValues: PaddingValues = SignInWithYouVersionButtonDefaults.padding,
    shape: Shape = SignInWithYouVersionButtonDefaults.capsuleShape,
    mode: SignInWithYouVersionButtonMode = SignInWithYouVersionButtonDefaults.mode,
    stroked: Boolean = false,
    dark: Boolean = isSystemInDarkTheme(),
    onTap: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val signIn = rememberSignIn()
    var isProcessing by remember { mutableStateOf(false) }
    var showSignInError by rememberSaveable { mutableStateOf(false) }

    if (showSignInError) {
        SignInErrorAlert(
            onDismissRequest = { showSignInError = false },
            onConfirm = { showSignInError = false },
        )
    }

    val colorGray15 = Color(0xFFDDDBDB)
    val colorGray35 = Color(0xFF474545)
    val strokeColor = if (dark) colorGray35 else colorGray15
    val strokeWidth = if (dark) 2.dp else 1.dp

    Button(
        enabled = !isProcessing,
        onClick = {
            if (onTap != null) {
                onTap()
            } else {
                if (isProcessing) return@Button
                isProcessing = true
                scope.launch {
                    try {
                        signIn(permissions())
                    } catch (_: Exception) {
                        showSignInError = true
                    } finally {
                        isProcessing = false
                    }
                }
            }
        },
        contentPadding = paddingValues,
        shape = shape,
        border = if (stroked) BorderStroke(strokeWidth, strokeColor) else null,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (dark) Color.Black else Color.White,
                contentColor = if (dark) Color.Green else Color.Black,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.size(26.dp)) {
                Image(
                    ImageVector.vectorResource(R.drawable.yv_bibleapp),
                    contentDescription = "Bible Logo",
                    modifier =
                        Modifier
                            .size(26.dp)
                            .alpha(
                                if (isProcessing) {
                                    0.5f
                                } else {
                                    1.0f
                                },
                            ),
                )
                if (isProcessing) {
                    CircularProgressIndicator()
                }
            }
            LocalizedLoginText(dark, mode)
        }
    }
}

@Composable
private fun LocalizedLoginText(
    dark: Boolean,
    mode: SignInWithYouVersionButtonMode,
) {
    when (mode) {
        SignInWithYouVersionButtonMode.FULL -> {
            val formatString = stringResource(R.string.sign_in_button_full)
            val brandName = "YouVersion"
            val annotatedString =
                buildAnnotatedString {
                    append(formatString)
                    append(" ")
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                        ),
                    ) {
                        append(brandName)
                    }
                }
            Text(
                text = annotatedString,
                color = if (dark) Color.White else Color.Black,
            )
        }

        SignInWithYouVersionButtonMode.COMPACT -> {
            Text(
                stringResource(R.string.sign_in_button_compact),
                color = if (dark) Color.White else Color.Black,
            )
        }

        SignInWithYouVersionButtonMode.ICON_ONLY -> {}
    }
}

// ----- Previews

@Preview
@Composable
private fun Preview_ButtonFull_DarkLight(
    @PreviewParameter(BooleanPreviewParameterProvider::class) rounded: Boolean,
) = ButtonPreview(SignInWithYouVersionButtonMode.FULL, rounded)

@Preview
@Composable
private fun Preview_ButtonCompact_DarkLight(
    @PreviewParameter(BooleanPreviewParameterProvider::class) rounded: Boolean,
) = ButtonPreview(SignInWithYouVersionButtonMode.COMPACT, rounded)

@Preview
@Composable
private fun Preview_ButtonIconOnly_DarkLight(
    @PreviewParameter(BooleanPreviewParameterProvider::class) rounded: Boolean,
) = ButtonPreview(SignInWithYouVersionButtonMode.ICON_ONLY, rounded)

@Composable
private fun ButtonPreview(
    mode: SignInWithYouVersionButtonMode,
    rounded: Boolean,
) {
    val shape =
        if (rounded) {
            SignInWithYouVersionButtonDefaults.capsuleShape
        } else {
            SignInWithYouVersionButtonDefaults.rectangleShape
        }
    Column {
        PreviewBackground(true) {
            SignInWithYouVersionButton(
                permissions = { emptySet() },
                mode = mode,
                shape = shape,
                stroked = false,
                dark = true,
            )
        }
        PreviewBackground(true) {
            SignInWithYouVersionButton(
                permissions = { emptySet() },
                mode = mode,
                shape = shape,
                stroked = true,
                dark = true,
            )
        }
        PreviewBackground(false) {
            SignInWithYouVersionButton(
                permissions = { emptySet() },
                mode = mode,
                shape = shape,
                stroked = false,
                dark = false,
            )
        }
        PreviewBackground(false) {
            SignInWithYouVersionButton(
                permissions = { emptySet() },
                mode = mode,
                shape = shape,
                stroked = true,
                dark = false,
            )
        }
    }
}

@Composable
internal fun PreviewBackground(
    dark: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier
                .background(if (dark) Color(0xFF333333) else Color(0xFFEEEEEE))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        content = content,
    )
}

private class BooleanPreviewParameterProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(true, false)
}
