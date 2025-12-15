package com.youversion.platform.reader.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.youversion.platform.reader.R

@Composable
fun BibleReaderHeaderMenu(
    isSignInProcessing: Boolean,
    signedIn: Boolean,
    onOpenMenu: () -> Unit,
    onFontSettingsClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .padding(16.dp),
    ) {
        IconButton(onClick = {
            onOpenMenu()
            expanded = !expanded
        }) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_pending),
                contentDescription = "Fonts & Settings",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reader_menu_font_settings)) },
                onClick = {
                    expanded = false
                    onFontSettingsClick()
                },
            )
            if (signedIn) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.reader_menu_sign_out),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSignOutClick()
                    },
                    enabled = !isSignInProcessing,
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_menu_sign_in)) },
                    onClick = {
                        expanded = false
                        onSignInClick()
                    },
                    enabled = !isSignInProcessing,
                )
            }
        }
    }
}
