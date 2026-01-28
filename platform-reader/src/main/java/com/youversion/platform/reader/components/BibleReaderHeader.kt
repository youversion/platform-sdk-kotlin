package com.youversion.platform.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.youversion.platform.reader.theme.BibleReaderMaterialTheme
import com.youversion.platform.reader.theme.ui.BibleReaderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderHeader(
    isSignInProcessing: Boolean,
    signedIn: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    versionAbbreviation: String,
    onVersionClick: () -> Unit,
    onOpenHeaderMenu: () -> Unit,
    onFontSettingsClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    TopAppBar(
        title = {},
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(end = 16.dp),
            ) {
                BibleReaderHeaderDropdownMenu(
                    isSignInProcessing = isSignInProcessing,
                    signedIn = signedIn,
                    onOpenMenu = onOpenHeaderMenu,
                    onFontSettingsClick = onFontSettingsClick,
                    onSignInClick = onSignInClick,
                    onSignOutClick = onSignOutClick,
                )

                Button(
                    onClick = onVersionClick,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = BibleReaderTheme.colorScheme.buttonSecondary,
                            contentColor = BibleReaderTheme.colorScheme.textPrimary,
                        ),
                    contentPadding =
                        PaddingValues(
                            start = 12.dp,
                            top = 4.dp,
                            end = 12.dp,
                            bottom = 4.dp,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = versionAbbreviation,
                        style = BibleReaderTheme.typography.buttonLabelS,
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.background,
            ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview_BibleReaderHeader() {
    BibleReaderMaterialTheme {
        BibleReaderHeader(
            isSignInProcessing = false,
            signedIn = true,
            scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState()),
            versionAbbreviation = "NIV",
            onVersionClick = {},
            onOpenHeaderMenu = {},
            onFontSettingsClick = {},
            onSignInClick = {},
            onSignOutClick = {},
        )
    }
}
