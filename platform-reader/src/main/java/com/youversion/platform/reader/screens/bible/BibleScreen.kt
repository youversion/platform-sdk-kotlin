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
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youversion.platform.core.YouVersionPlatformConfiguration
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
import com.youversion.platform.reader.sheets.DataExchangeConfirmationDialog
import com.youversion.platform.reader.sheets.HighlightColor
import com.youversion.platform.ui.dataexchange.DataExchangeStatus
import com.youversion.platform.ui.dataexchange.rememberDataExchange
import com.youversion.platform.ui.signin.SignInErrorAlert
import com.youversion.platform.ui.signin.SignInViewModel
import com.youversion.platform.ui.signin.SignOutConfirmationAlert
import com.youversion.platform.ui.signin.rememberSignIn
import com.youversion.platform.ui.theme.ui.BibleReaderTheme
import com.youversion.platform.ui.views.BibleIntroText
import com.youversion.platform.ui.views.BibleText
import com.youversion.platform.ui.views.BibleTextFootnoteMode
import com.youversion.platform.ui.views.BibleTextLoadingPhase
import com.youversion.platform.ui.views.BibleTextOptions
import com.youversion.platform.ui.views.SignInWithYouVersionPromptSheet
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
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()

    val colorsToRemove =
        remember(state.selectedVerses, highlights) {
            HighlightColor.entries.filter { viewModel.isColorPresentOnAnySelectedVerses(it.hexColor) }
        }
    val colorsToAdd =
        remember(state.selectedVerses, highlights) {
            HighlightColor.entries.filter { !viewModel.isColorPresentOnAllSelectedVerses(it.hexColor) }
        }

    val signInViewModel = viewModel<SignInViewModel>()
    val signInState by signInViewModel.state.collectAsStateWithLifecycle()

    // A signed-in reader keeps the colors even where sign-in is disabled: they already have the account the
    // highlight needs, so there is nothing left to prompt for.
    val showsHighlightColors = signInState.isSignedIn || signInState.isSignInEnabled

    var showSignInError by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val signIn = rememberSignIn()
    val permissions =
        setOf(
            SignInWithYouVersionPermission.PROFILE,
            SignInWithYouVersionPermission.HIGHLIGHTS,
        )

    fun launchSignIn(onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                signIn(permissions)
            } catch (_: Exception) {
                showSignInError = true
            }
            onComplete()
        }
    }

    // The flow can end on two routes, so it is completed by whichever reports first. The launcher returning a grant is
    // the direct signal, and is acted on alone; a launcher cancellation is not, because the browser tab reports one
    // when it is dismissed after a deep link the reader has yet to process. Everything else settles on the resume,
    // which reads the permission both routes persist before the reader can resume: a grant applies the pending
    // highlight, a dismissal or cancellation reads no grant and clears it.
    val isDataExchangeInProgress = rememberSaveable { mutableStateOf(false) }
    val requestDataExchange = rememberDataExchange(onBrowserOpened = { isDataExchangeInProgress.value = true })
    LaunchedEffect(state.shouldStartDataExchangeFlow) {
        if (!state.shouldStartDataExchangeFlow) return@LaunchedEffect
        val result = requestDataExchange(setOf(SignInWithYouVersionPermission.HIGHLIGHTS))
        val isHighlightsGranted = result?.grants(SignInWithYouVersionPermission.HIGHLIGHTS) == true
        // A null result means there was no launcher to open the browser with, and NotStarted means the flow could not
        // be started; either way nothing was asked and no resume will follow, so end the flow here rather than leaving
        // the reader waiting on a prompt it never saw.
        val wasBrowserNeverOpened = result == null || result.status == DataExchangeStatus.NotStarted
        if ((isHighlightsGranted || wasBrowserNeverOpened) && viewModel.state.value.shouldStartDataExchangeFlow) {
            isDataExchangeInProgress.value = false
            viewModel.onAction(
                BibleReaderViewModel.Action.DataExchangeCompleted(isHighlightsGranted = isHighlightsGranted),
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME &&
                    viewModel.state.value.shouldStartDataExchangeFlow &&
                    isDataExchangeInProgress.value
                ) {
                    isDataExchangeInProgress.value = false
                    val isHighlightsGranted =
                        YouVersionPlatformConfiguration.configState.value
                            ?.grantedPermissions
                            ?.contains(SignInWithYouVersionPermission.HIGHLIGHTS) == true
                    viewModel.onAction(
                        BibleReaderViewModel.Action.DataExchangeCompleted(isHighlightsGranted = isHighlightsGranted),
                    )
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // A highlight requested before a grant is held by the view model and reapplied once highlights access lands. The
    // grant arrives synchronously through data exchange but asynchronously through sign-in, so observe the persisted
    // permission and apply when it becomes available — this also covers a request that outlived the reader being
    // recreated during either browser flow.
    val config by YouVersionPlatformConfiguration.configState.collectAsStateWithLifecycle()
    val isHighlightsGranted =
        config?.grantedPermissions?.contains(SignInWithYouVersionPermission.HIGHLIGHTS) == true
    LaunchedEffect(isHighlightsGranted, state.hasHighlightRequest) {
        if (isHighlightsGranted && state.hasHighlightRequest) {
            viewModel.applyHighlightRequestIfPermitted()
        }
    }

    // A highlight change needs an account, so the view model holds it and raises shouldStartSignIn for a signed-out
    // reader; the change is dispatched regardless of sign-in state so the view model can capture it. Where the host
    // app has disabled sign-in the colors are hidden, so the guard here only stops a tap that should not be possible.
    val requestHighlight: (BibleReaderViewModel.Action) -> Unit = { action ->
        if (signInState.isSignedIn || signInState.isSignInEnabled) {
            viewModel.onAction(action)
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
                        colorsToRemove = colorsToRemove,
                        colorsToAdd = colorsToAdd,
                        showsHighlightColors = showsHighlightColors,
                        onAddHighlight = {
                            requestHighlight(BibleReaderViewModel.Action.AddHighlight(it))
                        },
                        onRemoveHighlight = {
                            requestHighlight(BibleReaderViewModel.Action.RemoveHighlight(it))
                        },
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
                        onSignInClick = { launchSignIn() },
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
                            val effectiveLineHeight = state.fontSize * state.lineSpacing
                            if (state.isViewingIntro && introPassageId != null) {
                                BibleIntroText(
                                    versionId = state.bibleReference.versionId,
                                    bookUSFM = state.introBookUSFM ?: state.bibleReference.bookUSFM,
                                    passageId = introPassageId,
                                    textOptions =
                                        BibleTextOptions(
                                            fontFamily = state.fontFamily,
                                            fontSize = state.fontSize,
                                            lineSpacing = effectiveLineHeight,
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
                                            lineSpacing = effectiveLineHeight,
                                            footnoteMode = BibleTextFootnoteMode.IMAGE,
                                        ),
                                    reference = state.bibleReference,
                                    selectedVerses = state.selectedVerses,
                                    onVerseTap = { reference, _ ->
                                        viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(reference))
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
                            onLineSpacingClick = { viewModel.onAction(BibleReaderViewModel.Action.CycleLineSpacing) },
                            onFontClick = {
                                viewModel.onAction(BibleReaderViewModel.Action.CloseFontSettings)
                                onFontsClick()
                            },
                            onThemeSelect = { newReaderTheme ->
                                viewModel.onAction(BibleReaderViewModel.Action.SetReaderTheme(newReaderTheme))
                            },
                            fontDefinition = state.selectedFontDefinition,
                            lineSpacing = state.lineSpacing,
                        )
                    }

                    if (state.shouldStartSignIn) {
                        SignInWithYouVersionPromptSheet(
                            appName = appName,
                            onSignIn = {
                                launchSignIn { viewModel.onAction(BibleReaderViewModel.Action.SignInCompleted) }
                            },
                            onDismissRequest = { viewModel.onAction(BibleReaderViewModel.Action.CancelSignIn) },
                            appSignInMessage = appSignInMessage,
                        )
                    }

                    if (state.showDataExchangeConfirmation) {
                        DataExchangeConfirmationDialog(
                            onConfirm = { viewModel.onAction(BibleReaderViewModel.Action.ConfirmDataExchange) },
                            onDismiss = { viewModel.onAction(BibleReaderViewModel.Action.CancelDataExchange) },
                        )
                    }

                    if (showSignInError) {
                        SignInErrorAlert(
                            onDismissRequest = { showSignInError = false },
                            onConfirm = { showSignInError = false },
                        )
                    }

                    signInState.signOutConfirmation?.let { signOutConfirmation ->
                        SignOutConfirmationAlert(
                            onDismissRequest = { signInViewModel.onAction(SignInViewModel.Action.CancelSignOut) },
                            onConfirm =
                                {
                                    signInViewModel.onAction(SignInViewModel.Action.SignOut(false))
                                },
                            confirmation = signOutConfirmation,
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
