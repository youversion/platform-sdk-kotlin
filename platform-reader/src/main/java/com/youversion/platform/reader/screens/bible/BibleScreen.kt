package com.youversion.platform.reader.screens.bible

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.reader.BibleReaderViewModel
import com.youversion.platform.reader.components.BibleReaderHeader
import com.youversion.platform.reader.sheets.BibleReaderFontSettingsSheet
import com.youversion.platform.ui.views.BibleText
import com.youversion.platform.ui.views.BibleTextLoadingPhase
import com.youversion.platform.ui.views.BibleTextOptions

@Composable
internal fun BibleScreen(
    viewModel: BibleReaderViewModel,
    appName: String,
    appSignInMessage: String,
    bottomBar: @Composable () -> Unit = {},
    onReferencesClick: () -> Unit,
    onVersionsClick: () -> Unit,
    onFontsClick: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var loadingPhase by remember { mutableStateOf(BibleTextLoadingPhase.INACTIVE) }

    Scaffold(
        bottomBar = bottomBar,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Column {
                // Reader top bar
                BibleReaderHeader(
                    signedIn = false,
                    bookAndChapter = state.bookAndChapter,
                    versionAbbreviation = state.versionAbbreviation,
                    onChapterClick = onReferencesClick,
                    onVersionClick = onVersionsClick,
                    onFontSettingsClick = { viewModel.onAction(BibleReaderViewModel.Action.OpenFontSettings) },
                    onSignInClick = {},
                    onSignOutClick = {},
                )

                // Scrollable Reader content
                Box(
                    modifier =
                        Modifier
                            .padding(horizontal = 32.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        BibleText(
                            textOptions =
                                BibleTextOptions(
                                    fontSize = state.fontSize,
                                    lineSpacing = state.lineSpacing,
                                ),
                            reference = state.bibleReference,
                            onStateChange = { loadingPhase = it },
                        )
                        if (loadingPhase == BibleTextLoadingPhase.SUCCESS) {
                            Copyright(version = state.bibleVersion)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Any Sheets or Dialogs
            if (state.showingFontList) {
                BibleReaderFontSettingsSheet(
                    onDismissRequest = { viewModel.onAction(BibleReaderViewModel.Action.CloseFontSettings) },
                    onSmallerFontClick = { viewModel.onAction(BibleReaderViewModel.Action.DecreaseFontSize) },
                    onBiggerFontClick = { viewModel.onAction(BibleReaderViewModel.Action.IncreaseFontSize) },
                    onLineSpacingClick = {
                        viewModel.onAction(BibleReaderViewModel.Action.NextLineSpacingMultiplierOption)
                    },
                    onFontClick = {
                        viewModel.onAction(BibleReaderViewModel.Action.CloseFontSettings)
                        onFontsClick()
                    },
                    onThemeSelect = { },
                    lineSpacingSettingIndex = state.lineSpacingSettingsIndex,
                )
            }
        }
    }
}

@Composable
private fun Copyright(version: BibleVersion?) {
    val copyright = version?.copyrightShort ?: version?.copyrightLong ?: ""
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = copyright,
            textAlign = TextAlign.Center,
            style =
                TextStyle(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                ),
            modifier =
                Modifier
                    .widthIn(max = 280.dp)
                    .padding(top = 16.dp),
        )
    }
}
