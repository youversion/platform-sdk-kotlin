package com.youversion.platform.reader.screens.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.reader.BibleReaderViewModel
import com.youversion.platform.reader.components.BibleReaderHeader
import com.youversion.platform.reader.components.BibleReaderPassageSelection
import com.youversion.platform.reader.components.PassageSelectionDefaults
import com.youversion.platform.reader.sheets.BibleReaderFontSettingsSheet
import com.youversion.platform.reader.sheets.BibleReaderFootnotesSheet
import com.youversion.platform.ui.signin.SignInErrorAlert
import com.youversion.platform.ui.signin.SignInParameters
import com.youversion.platform.ui.signin.SignInViewModel
import com.youversion.platform.ui.signin.SignOutConfirmationAlert
import com.youversion.platform.ui.signin.rememberSignInWithYouVersion
import com.youversion.platform.ui.signin.rememberYouVersionAuthLauncher
import com.youversion.platform.ui.views.BibleText
import com.youversion.platform.ui.views.BibleTextFootnoteMode
import com.youversion.platform.ui.views.BibleTextLoadingPhase
import com.youversion.platform.ui.views.BibleTextOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BibleScreen(
    viewModel: BibleReaderViewModel,
    appName: String,
    appSignInMessage: String,
    bottomBar: @Composable (() -> Unit)? = null,
    onReferencesClick: () -> Unit,
    onVersionsClick: () -> Unit,
    onFontsClick: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val signInViewModel = viewModel<SignInViewModel>()
    val signInState by signInViewModel.state.collectAsStateWithLifecycle()

    var showSignInError by rememberSaveable { mutableStateOf(false) }

    val authTabLauncher =
        rememberYouVersionAuthLauncher { intent ->
            signInViewModel.onAction(SignInViewModel.Action.ProcessAuthCallback(intent))
        }

    val signInLauncher =
        rememberSignInWithYouVersion(
            onSignInError = {
                showSignInError = true
            },
        )

    var loadingPhase by remember { mutableStateOf(BibleTextLoadingPhase.INACTIVE) }

    val topScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val bottomScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
    val passageSelectionScrollBehavior = PassageSelectionDefaults.fadeAlwaysScrollBehavior()

    Scaffold(
        modifier =
            Modifier
                .nestedScroll(passageSelectionScrollBehavior.nestedScrollConnection)
                .nestedScroll(bottomScrollBehavior.nestedScrollConnection)
                .nestedScroll(topScrollBehavior.nestedScrollConnection),
        bottomBar = {
            bottomBar?.let {
                BottomAppBar(
                    scrollBehavior = bottomScrollBehavior,
                    content = {
                        Row {
                            it()
                        }
                    },
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            BibleReaderHeader(
                isSignInProcessing = signInState.isProcessing,
                signedIn = signInState.isSignedIn,
                versionAbbreviation = state.versionAbbreviation,
                scrollBehavior = topScrollBehavior,
                onVersionClick = onVersionsClick,
                onOpenHeaderMenu = { signInViewModel.onAction(SignInViewModel.Action.UpdateSignInState) },
                onFontSettingsClick = { viewModel.onAction(BibleReaderViewModel.Action.OpenFontSettings) },
                onSignInClick = {
                    signInLauncher(
                        SignInParameters(
                            context = context,
                            launcher = authTabLauncher,
                            permissions =
                                setOf(
                                    SignInWithYouVersionPermission.PROFILE,
                                    SignInWithYouVersionPermission.EMAIL,
                                ),
                        ),
                    )
                },
                onSignOutClick = { signInViewModel.onAction(SignInViewModel.Action.SignOut(true)) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(Color.Red)
                    .padding(bottom = 16.dp),
        ) {
            Text("Foo")
        }
        Box(modifier = Modifier.padding(innerPadding)) {
            // Scrollable Reader content
            Column {
                Column(
                    modifier =
                        Modifier
                            .padding(horizontal = 32.dp)
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    BibleText(
                        textOptions =
                            BibleTextOptions(
                                fontFamily = state.fontFamily,
                                fontSize = state.fontSize,
                                lineSpacing = state.lineSpacing,
                                footnoteMode = BibleTextFootnoteMode.IMAGE,
                            ),
                        reference = state.bibleReference,
                        onStateChange = { loadingPhase = it },
                        onFootnoteTap = { reference, footnotes ->
                            viewModel.onAction(
                                BibleReaderViewModel.Action.OpenFootnotes(
                                    reference = reference,
                                    footnotes = footnotes,
                                ),
                            )
                        },
                    )
                    if (loadingPhase == BibleTextLoadingPhase.SUCCESS) {
                        Copyright(version = state.bibleVersion)
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
                BibleReaderPassageSelection(
                    bookAndChapter = state.bookAndChapter,
                    onReferenceClick = onReferencesClick,
                    onPreviousChapter = { viewModel.onAction(BibleReaderViewModel.Action.GoToPreviousChapter) },
                    onNextChapter = { viewModel.onAction(BibleReaderViewModel.Action.GoToNextChapter) },
                    bottomBarScrollBehavior = bottomBar?.let { bottomScrollBehavior },
                    scrollBehavior = passageSelectionScrollBehavior,
                )
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
                    onThemeSelect = { newReaderTheme ->
                        viewModel.onAction(BibleReaderViewModel.Action.SetReaderTheme(newReaderTheme))
                    },
                    lineSpacingSettingIndex = state.lineSpacingSettingsIndex,
                    fontDefinition = state.selectedFontDefinition,
                )
            }

            if (showSignInError) {
                SignInErrorAlert(
                    onDismissRequest = { showSignInError = false },
                    onConfirm = { showSignInError = false },
                )
            }

            if (signInState.showSignOutConfirmation) {
                SignOutConfirmationAlert(
                    onDismissRequest = { signInViewModel.onAction(SignInViewModel.Action.CancelSignOut) },
                    onConfirm =
                        {
                            signInViewModel.onAction(SignInViewModel.Action.SignOut(false))
                        },
                )
            }

            if (state.showingFootnotes) {
                BibleReaderFootnotesSheet(
                    textOptions =
                        BibleTextOptions(
                            fontFamily = state.fontFamily,
                            fontSize = state.fontSize,
                            lineSpacing = state.lineSpacing,
                        ),
                    onDismissRequest = { viewModel.onAction(BibleReaderViewModel.Action.CloseFootnotes) },
                    version = state.bibleVersion,
                    reference = state.footnotesReference,
                    footnotes = state.footnotes,
                )
            }
        }
    }
}

@Composable
private fun Copyright(version: BibleVersion?) {
    val copyright = version?.copyright ?: version?.promotionalContent ?: ""
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
