package com.youversion.platform.reader.screens.bible

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
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
import com.youversion.platform.reader.BibleReaderSearchViewModel
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
import com.youversion.platform.reader.sheets.BibleReaderSearchSheet
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
import com.youversion.platform.ui.views.BibleTextFootnoteMode
import com.youversion.platform.ui.views.BibleTextLoadingPhase
import com.youversion.platform.ui.views.BibleTextOptions
import com.youversion.platform.ui.views.SignInWithYouVersionPromptSheet
import com.youversion.platform.ui.views.bibleTextBlocks
import com.youversion.platform.ui.views.rememberBibleTextBlocksState
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

// The chapter header is the one item the list emits ahead of the blocks, so a block's scroll target is its own
// index plus this.
private const val CHAPTER_HEADER_ITEM_COUNT = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BibleScreen(
    viewModel: BibleReaderViewModel,
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

    val searchViewModel = viewModel<BibleReaderSearchViewModel>()
    val searchState by searchViewModel.state.collectAsStateWithLifecycle()

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

    var introLoadingPhase by remember { mutableStateOf(BibleTextLoadingPhase.INACTIVE) }
    var hasIntroOwnTitle by remember { mutableStateOf(false) }
    var isBannerDismissed by rememberSaveable { mutableStateOf(false) }

    val bibleTextOptions =
        BibleTextOptions(
            fontFamily = state.fontFamily,
            fontSize = state.fontSize,
            lineSpacingFraction = state.lineSpacingFraction,
            footnoteMode = BibleTextFootnoteMode.IMAGE,
        )
    val introPassageId = state.introPassageId
    val isShowingIntro = state.isViewingIntro && introPassageId != null

    val chapterBlocks =
        if (isShowingIntro) {
            null
        } else {
            rememberBibleTextBlocksState(reference = state.bibleReference, textOptions = bibleTextOptions)
        }
    val loadingPhase = chapterBlocks?.loadingPhase ?: introLoadingPhase
    val chapterListState = rememberLazyListState()

    // The chapter is held out of sight between laying out and the target being placed, so the frame it spends at
    // the top is never seen. Swift hides its own text the same way while a verse scroll is pending.
    val isScrollPending =
        state.scrollTargetReference?.let { target ->
            chapterBlocks?.loadingPhase == BibleTextLoadingPhase.SUCCESS && state.bibleReference.contains(target)
        } == true

    LaunchedEffect(state.scrollTargetReference, chapterBlocks?.loadingPhase) {
        val target = state.scrollTargetReference ?: return@LaunchedEffect
        val blocks = chapterBlocks ?: return@LaunchedEffect
        // A target the load never reached is left staged rather than consumed. A chapter that failed renders no
        // verses, so a later successful reload of it is the search result finally landing, not a scroll back to
        // somewhere the reader had moved on from; moving off the chapter drops the target in the view model.
        if (blocks.loadingPhase != BibleTextLoadingPhase.SUCCESS || !state.bibleReference.contains(target)) {
            return@LaunchedEffect
        }
        blocks.indexOfBlockContaining(target)?.let { index ->
            // Placed in the next measure pass rather than scrolled to, so the reference is simply where the
            // chapter opens instead of somewhere the list travels to from the top.
            chapterListState.requestScrollToItem(CHAPTER_HEADER_ITEM_COUNT + index)
        }
        viewModel.onAction(BibleReaderViewModel.Action.ScrollTargetReached)
    }

    LaunchedEffect(chapterListState) {
        chapterListState.interactionSource.interactions
            .filterIsInstance<DragInteraction.Start>()
            .collect { viewModel.onAction(BibleReaderViewModel.Action.ClearFocusedReference) }
    }

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

    // The verse action sheet is a standard sheet, which draws no scrim of its own, so the reader behind it is dimmed
    // here with the same color the modal sheets use.
    val verseActionScrimColor = BottomSheetDefaults.ScrimColor
    val verseActionScrimProgress by animateFloatAsState(
        targetValue = if (state.showVerseActionSheet) 1f else 0f,
        label = "verseActionScrim",
    )

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
                            .background(MaterialTheme.colorScheme.background),
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
            // A tap that no verse took is how the reader puts a selection down, so it is read here rather than off
            // the scrim: the scrim covers the verses too, and catching taps there would leave a reader unable to add
            // a second verse to what they have already selected.
            Box(
                modifier =
                    Modifier.pointerInput(state.showVerseActionSheet) {
                        if (state.showVerseActionSheet) {
                            detectTapGestures {
                                viewModel.onAction(BibleReaderViewModel.Action.ClearVerseSelection)
                            }
                        }
                    },
            ) {
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
                            onSearchClick = {
                                searchViewModel.onAction(
                                    BibleReaderSearchViewModel.Action.OpenSearch(state.bibleVersion),
                                )
                                viewModel.onAction(BibleReaderViewModel.Action.OpenSearch)
                            },
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
                            LazyColumn(
                                state = chapterListState,
                                modifier =
                                    Modifier
                                        .padding(horizontal = 32.dp)
                                        .weight(1f)
                                        .alpha(if (isScrollPending) 0f else 1f),
                            ) {
                                item {
                                    Spacer(modifier = Modifier.height(32.dp))
                                    val isHeaderVisible =
                                        state.bookName.isNotEmpty() && !(isShowingIntro && hasIntroOwnTitle)
                                    if (isHeaderVisible) {
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
                                                if (isShowingIntro) {
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
                                }
                                if (chapterBlocks != null) {
                                    bibleTextBlocks(
                                        state = chapterBlocks,
                                        textOptions = bibleTextOptions,
                                        selectedVerses = state.selectedVerses,
                                        focusedReference = state.focusedReference,
                                        onVerseTap = { reference, _ ->
                                            viewModel.onAction(BibleReaderViewModel.Action.OnVerseTap(reference))
                                        },
                                        onFootnoteTap = { reference, footnotes ->
                                            viewModel.onAction(
                                                BibleReaderViewModel.Action.OpenFootnotes(
                                                    reference = reference,
                                                    footnotes = footnotes,
                                                ),
                                            )
                                        },
                                    )
                                } else if (introPassageId != null) {
                                    item {
                                        BibleIntroText(
                                            versionId = state.bibleReference.versionId,
                                            bookUSFM = state.introBookUSFM ?: state.bibleReference.bookUSFM,
                                            passageId = introPassageId,
                                            textOptions = bibleTextOptions,
                                            onFootnoteTap = { footnotes ->
                                                viewModel.onAction(
                                                    BibleReaderViewModel.Action.OpenIntroFootnotes(
                                                        footnotes = footnotes,
                                                    ),
                                                )
                                            },
                                            onStateChange = { introLoadingPhase = it },
                                            onHasOwnTitleChange = { hasIntroOwnTitle = it },
                                        )
                                    }
                                }
                                item {
                                    if (loadingPhase == BibleTextLoadingPhase.SUCCESS) {
                                        Copyright(version = state.bibleVersion)
                                    }
                                    Spacer(modifier = Modifier.height(48.dp))
                                }
                            }
                            BibleReaderPassageSelection(
                                bookAndChapter = state.bookAndChapter(stringResource(R.string.intro_chapter_label)),
                                onReferenceClick = onReferencesClick,
                                onPreviousChapter = {
                                    viewModel.onAction(
                                        BibleReaderViewModel.Action.GoToPreviousChapter,
                                    )
                                },
                                onNextChapter = { viewModel.onAction(BibleReaderViewModel.Action.GoToNextChapter) },
                                bottomBarScrollBehavior = bottomBar?.let { bottomScrollBehavior },
                                scrollBehavior = passageSelectionScrollBehavior,
                            )
                        }

                        // Any Sheets or Dialogs
                        if (state.showingFontList) {
                            BibleReaderFontSettingsSheet(
                                onDismissRequest = {
                                    viewModel.onAction(
                                        BibleReaderViewModel.Action.CloseFontSettings,
                                    )
                                },
                                onSmallerFontClick = {
                                    viewModel.onAction(
                                        BibleReaderViewModel.Action.DecreaseFontSize,
                                    )
                                },
                                onBiggerFontClick = {
                                    viewModel.onAction(
                                        BibleReaderViewModel.Action.IncreaseFontSize,
                                    )
                                },
                                onLineSpacingClick = {
                                    viewModel.onAction(
                                        BibleReaderViewModel.Action.CycleLineSpacing,
                                    )
                                },
                                onFontClick = {
                                    viewModel.onAction(BibleReaderViewModel.Action.CloseFontSettings)
                                    onFontsClick()
                                },
                                onThemeSelect = { newReaderTheme ->
                                    viewModel.onAction(BibleReaderViewModel.Action.SetReaderTheme(newReaderTheme))
                                },
                                fontDefinition = state.selectedFontDefinition,
                                lineSpacingFraction = state.lineSpacingFraction,
                            )
                        }

                        if (state.showingSearch) {
                            BibleReaderSearchSheet(
                                onDismissRequest = { viewModel.onAction(BibleReaderViewModel.Action.CloseSearch) },
                                onQueryChange = { newQuery ->
                                    searchViewModel.onAction(BibleReaderSearchViewModel.Action.SetQuery(newQuery))
                                },
                                onSubmit = { searchViewModel.onAction(BibleReaderSearchViewModel.Action.Submit) },
                                onRequestResultText = { reference ->
                                    searchViewModel.onAction(
                                        BibleReaderSearchViewModel.Action.LoadResultText(reference),
                                    )
                                },
                                onSelectResult = { reference ->
                                    viewModel.onAction(BibleReaderViewModel.Action.GoToSearchResult(reference))
                                },
                                onLoadNextPage = {
                                    searchViewModel.onAction(BibleReaderSearchViewModel.Action.LoadNextPage)
                                },
                                onSelectSuggestedQuery = { query ->
                                    searchViewModel.onAction(
                                        BibleReaderSearchViewModel.Action.SelectSuggestedQuery(query),
                                    )
                                },
                                state = searchState,
                            )
                        }

                        if (state.shouldStartSignIn) {
                            SignInWithYouVersionPromptSheet(
                                onSignIn = {
                                    launchSignIn { viewModel.onAction(BibleReaderViewModel.Action.SignInCompleted) }
                                },
                                onDismissRequest = { viewModel.onAction(BibleReaderViewModel.Action.CancelSignIn) },
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

                if (verseActionScrimProgress > 0f) {
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .background(
                                    verseActionScrimColor.copy(
                                        alpha = verseActionScrimColor.alpha * verseActionScrimProgress,
                                    ),
                                ),
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
