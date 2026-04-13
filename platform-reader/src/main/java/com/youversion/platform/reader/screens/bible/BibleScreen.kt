package com.youversion.platform.reader.screens.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.reader.BibleReaderViewModel
import com.youversion.platform.reader.R
import com.youversion.platform.reader.components.BibleReaderBanner
import com.youversion.platform.reader.components.BibleReaderBannerType
import com.youversion.platform.reader.components.BibleReaderHeader
import com.youversion.platform.reader.components.BibleReaderPassageSelection
import com.youversion.platform.reader.components.PassageSelectionDefaults
import com.youversion.platform.reader.sheets.BibleReaderFontSettingsSheet
import com.youversion.platform.reader.sheets.BibleReaderFootnotesSheet
import com.youversion.platform.reader.sheets.BibleReaderIntroFootnotesSheet
import com.youversion.platform.reader.sheets.BibleReaderVerseActionSheet
import com.youversion.platform.reader.theme.ui.BibleReaderTheme
import com.youversion.platform.ui.signin.SignInErrorAlert
import com.youversion.platform.ui.signin.SignInViewModel
import com.youversion.platform.ui.signin.SignOutConfirmationAlert
import com.youversion.platform.ui.signin.rememberSignIn
import com.youversion.platform.ui.views.BibleIntroText
import com.youversion.platform.ui.views.BibleText
import com.youversion.platform.ui.views.BibleTextFootnoteMode
import com.youversion.platform.ui.views.BibleTextLoadingPhase
import com.youversion.platform.ui.views.BibleTextOptions
import kotlinx.coroutines.launch

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
    val state by viewModel.state.collectAsStateWithLifecycle()

    val signInViewModel = viewModel<SignInViewModel>()
    val signInState by signInViewModel.state.collectAsStateWithLifecycle()

    var showSignInError by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val signIn = rememberSignIn()
    val permissions =
        setOf(
            SignInWithYouVersionPermission.PROFILE,
            SignInWithYouVersionPermission.EMAIL,
        )
    val launchSignIn: () -> Unit = {
        scope.launch {
            try {
                signIn(permissions)
            } catch (_: Exception) {
                showSignInError = true
            }
        }
    }

    var loadingPhase by remember { mutableStateOf(BibleTextLoadingPhase.INACTIVE) }
    var isBannerDismissed by rememberSaveable { mutableStateOf(false) }

    val bannerType =
        when (loadingPhase) {
            BibleTextLoadingPhase.FAILED -> BibleReaderBannerType.OFFLINE
            BibleTextLoadingPhase.NOT_PERMITTED -> BibleReaderBannerType.VERSION_UNAVAILABLE
            else -> null
        }

    LaunchedEffect(bannerType) {
        if (bannerType == null) {
            isBannerDismissed = false
        }
    }

    val topScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val bottomScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
    val passageSelectionScrollBehavior = PassageSelectionDefaults.fadeAlwaysScrollBehavior()

    val bottomSheetState =
        rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false,
        )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    LaunchedEffect(Unit) {
        snapshotFlow { state.showVerseActionSheet }
            .collect { shouldShow ->
                if (shouldShow) {
                    bottomSheetState.expand()
                } else {
                    bottomSheetState.hide()
                }
            }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { bottomSheetState.currentValue }
            .collect { sheetValue ->
                if (sheetValue != SheetValue.Expanded && state.showVerseActionSheet) {
                    viewModel.onAction(BibleReaderViewModel.Action.ClearVerseSelection)
                }
            }
    }

    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    Box {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetContent = {
                Column(
                    modifier =
                        Modifier
                            .testTag("verse_action_sheet")
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                            .dropShadow(sheetShape) {
                                radius = 16f
                                offset = Offset(0f, -8f)
                                color = Color.Black.copy(alpha = 0.15f)
                            }.clip(sheetShape)
                            .background(MaterialTheme.colorScheme.surface),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 16.dp, bottom = 8.dp)
                                .size(width = 32.dp, height = 4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    shape = RoundedCornerShape(2.dp),
                                ),
                    )
                    BibleReaderVerseActionSheet(
                        selectedVerses = state.selectedVerses,
                        onCopy = { viewModel.onAction(BibleReaderViewModel.Action.CopySelectedVerses) },
                        onShare = { viewModel.onAction(BibleReaderViewModel.Action.ShareSelectedVerses) },
                    )
                }
            },
            sheetPeekHeight = 0.dp,
            sheetDragHandle = null,
            sheetShape = RectangleShape,
            sheetShadowElevation = 0.dp,
            sheetContainerColor = Color.Transparent,
            containerColor = MaterialTheme.colorScheme.background,
        ) { sheetPadding ->
            Scaffold(
                modifier =
                    Modifier
                        .padding(sheetPadding)
                        .nestedScroll(passageSelectionScrollBehavior.nestedScrollConnection)
                        .nestedScroll(bottomScrollBehavior.nestedScrollConnection)
                        .nestedScroll(topScrollBehavior.nestedScrollConnection),
                topBar = {
                    BibleReaderHeader(
                        isSignInProcessing = signInState.isProcessing,
                        signedIn = signInState.isSignedIn,
                        versionAbbreviation = state.versionAbbreviation,
                        scrollBehavior = topScrollBehavior,
                        onVersionClick = onVersionsClick,
                        onOpenHeaderMenu = { signInViewModel.onAction(SignInViewModel.Action.UpdateSignInState) },
                        onFontSettingsClick = { viewModel.onAction(BibleReaderViewModel.Action.OpenFontSettings) },
                        onSignInClick = launchSignIn,
                        onSignOutClick = { signInViewModel.onAction(SignInViewModel.Action.SignOut(true)) },
                    )
                },
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
                containerColor = MaterialTheme.colorScheme.background,
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    Column {
                        Column(
                            modifier =
                                Modifier
                                    .padding(horizontal = 32.dp)
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                        ) {
                            Spacer(modifier = Modifier.height(32.dp))
                            if (state.bookName.isNotEmpty()) {
                                Text(
                                    text = state.bookName,
                                    style =
                                        TextStyle(
                                            fontFamily = state.fontFamily,
                                            fontSize = state.fontSize * 1.3,
                                            color = BibleReaderTheme.colorScheme.textMuted,
                                        ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    text =
                                        if (state.isViewingIntro) {
                                            stringResource(R.string.intro_chapter_label)
                                        } else {
                                            state.chapterNumber.toString()
                                        },
                                    style =
                                        TextStyle(
                                            fontFamily = state.fontFamily,
                                            fontSize = state.fontSize * 2.2,
                                            color = BibleReaderTheme.colorScheme.textMuted,
                                        ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                            val introPassageId = state.introPassageId
                            if (state.isViewingIntro && introPassageId != null) {
                                BibleIntroText(
                                    versionId = state.bibleReference.versionId,
                                    bookUSFM = state.introBookUSFM ?: state.bibleReference.bookUSFM,
                                    passageId = introPassageId,
                                    textOptions =
                                        BibleTextOptions(
                                            fontFamily = state.fontFamily,
                                            fontSize = state.fontSize,
                                            footnoteMode = BibleTextFootnoteMode.IMAGE,
                                        ),
                                    onFootnoteTap = { footnotes ->
                                        viewModel.onAction(
                                            BibleReaderViewModel.Action.OpenIntroFootnotes(
                                                footnotes = footnotes,
                                            ),
                                        )
                                    },
                                    onStateChange = { loadingPhase = it },
                                )
                            } else {
                                BibleText(
                                    textOptions =
                                        BibleTextOptions(
                                            fontFamily = state.fontFamily,
                                            fontSize = state.fontSize,
                                            footnoteMode = BibleTextFootnoteMode.IMAGE,
                                        ),
                                    reference = state.bibleReference,
                                    selectedVerses = state.selectedVerses,
                                    onVerseTap = { reference, _ ->
                                        if (signInState.isSignedIn) {
                                            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(reference))
                                        } else {
                                            launchSignIn()
                                        }
                                    },
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
                            }
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
                            onFontClick = {
                                viewModel.onAction(BibleReaderViewModel.Action.CloseFontSettings)
                                onFontsClick()
                            },
                            onThemeSelect = { newReaderTheme ->
                                viewModel.onAction(BibleReaderViewModel.Action.SetReaderTheme(newReaderTheme))
                            },
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
                                ),
                            onDismissRequest = { viewModel.onAction(BibleReaderViewModel.Action.CloseFootnotes) },
                            version = state.bibleVersion,
                            reference = state.footnotesReference,
                            footnotes = state.footnotes,
                        )
                    }
                }

                if (state.showingIntroFootnotes) {
                    BibleReaderIntroFootnotesSheet(
                        onDismissRequest = { viewModel.onAction(BibleReaderViewModel.Action.CloseIntroFootnotes) },
                        footnotes = state.introFootnotes,
                    )
                }
            }
        }

        // Banner overlay above all content including header
        bannerType?.let {
            BibleReaderBanner(
                bannerType = bannerType,
                isVisible = !isBannerDismissed,
                onDismiss = { isBannerDismissed = true },
                modifier =
                    Modifier
                        .statusBarsPadding()
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
            )
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
                    color = BibleReaderTheme.colorScheme.textMuted,
                ),
            modifier =
                Modifier
                    .widthIn(max = 280.dp)
                    .padding(top = 16.dp),
        )
    }
}
