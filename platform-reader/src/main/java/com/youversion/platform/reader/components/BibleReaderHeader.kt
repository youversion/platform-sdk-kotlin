package com.youversion.platform.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youversion.platform.reader.theme.readerColorScheme

@Composable
fun BibleReaderHeader(
    isSignInProcessing: Boolean,
    signedIn: Boolean,
    bookAndChapter: String,
    versionAbbreviation: String,
    onChapterClick: () -> Unit,
    onVersionClick: () -> Unit,
    onOpenHeaderMenu: () -> Unit,
    onFontSettingsClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
        ) {
            BibleReaderHalfPillPicker(
                bookAndChapter = bookAndChapter,
                versionAbbreviation = versionAbbreviation,
                handleChapterTap = onChapterClick,
                handleVersionTap = onVersionClick,
                foregroundColor = MaterialTheme.readerColorScheme.readerTextPrimaryColor,
                buttonColor = MaterialTheme.readerColorScheme.buttonPrimaryColor,
                compactMode = false,
            )
            BibleReaderHeaderMenu(
                isSignInProcessing = isSignInProcessing,
                signedIn = signedIn,
                onOpenMenu = onOpenHeaderMenu,
                onFontSettingsClick = onFontSettingsClick,
                onSignInClick = onSignInClick,
                onSignOutClick = onSignOutClick,
            )
        }
        HorizontalDivider()
    }
}
