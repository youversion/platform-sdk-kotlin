package com.youversion.platform.ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.bibles.domain.BibleChapterRepository
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.di.PlatformInternalApi
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.core.highlights.domain.BibleHighlightsRepository
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.core.utilities.exceptions.BibleVersionApiException
import com.youversion.platform.ui.theme.readerColorScheme
import com.youversion.platform.ui.views.rendering.BibleReferenceAttribute
import com.youversion.platform.ui.views.rendering.BibleTextBlock
import com.youversion.platform.ui.views.rendering.BibleTextCategory
import com.youversion.platform.ui.views.rendering.BibleTextCategoryAttribute
import com.youversion.platform.ui.views.rendering.BibleVersionRendering
import com.youversion.platform.ui.views.rendering.isVisible
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * A loaded chapter, held apart from the composable that draws it so a host can lay the blocks out itself.
 *
 * Build one with [rememberBibleTextBlocksState] and hand it to [bibleTextBlocks].
 */
@PlatformInternalApi
@Immutable
class BibleTextBlocksState internal constructor(
    /** How far the load named by the state's reference has got. */
    val loadingPhase: BibleTextLoadingPhase,
    internal val blocks: List<BibleTextBlock>,
    internal val highlights: Map<BibleReference, Color>,
    internal val horizontalAlignment: Alignment.Horizontal,
) {
    // Blocks that render as nothing are dropped up front so an index into this list is also an index into the
    // items the chapter emits, which is what makes a block addressable as a scroll target.
    internal val visibleBlocks: List<BibleTextBlock> = blocks.filter { it.isVisible }

    /**
     * The position among the emitted blocks of the first one covering [reference], or null when no block does —
     * including while the chapter is still loading.
     */
    fun indexOfBlockContaining(reference: BibleReference): Int? {
        if (loadingPhase != BibleTextLoadingPhase.SUCCESS) return null
        return visibleBlocks.indexOfFirst { it.covers(reference) }.takeIf { it >= 0 }
    }
}

/**
 * Loads the chapter, or the passage within it, named by [reference] and keeps it for as long as it is composed.
 *
 * The returned state is replaced whenever the load moves on, so read it during composition rather than holding on
 * to a copy of it.
 */
@PlatformInternalApi
@Composable
fun rememberBibleTextBlocksState(
    reference: BibleReference,
    textOptions: BibleTextOptions = BibleTextOptions(),
): BibleTextBlocksState {
    var blocks by remember { mutableStateOf<List<BibleTextBlock>>(emptyList()) }
    var loadingPhase by remember { mutableStateOf(BibleTextLoadingPhase.INACTIVE) }
    var isVersionRightToLeft by remember { mutableStateOf(false) }
    val versionRepository: BibleVersionRepository = PlatformKoinGraph.koinApplication.koin.get()
    val chapterRepository: BibleChapterRepository = PlatformKoinGraph.koinApplication.koin.get()
    val highlightsRepository: BibleHighlightsRepository = PlatformKoinGraph.koinApplication.koin.get()

    val cachedHighlights by highlightsRepository.highlights.collectAsStateWithLifecycle()
    val configState by YouVersionPlatformConfiguration.configState.collectAsStateWithLifecycle()
    val isSignedIn = configState?.isSignedIn == true

    // Reading highlights needs both an account and that account's consent. Without either there is nothing on the
    // server to fetch and the request would only come back unauthorized or refused, so it is not made at all.
    val hasHighlightsAccess =
        isSignedIn &&
            configState?.grantedPermissions?.contains(SignInWithYouVersionPermission.HIGHLIGHTS) == true

    LaunchedEffect(reference) {
        if (hasHighlightsAccess) {
            highlightsRepository.ensureHighlightsForChapterLoaded(reference)
        }
    }

    // Nothing else re-triggers a load when access is gained while the reader is already viewing a chapter, whether
    // that was by signing in or by granting the highlights permission. Force a refresh only on the false -> true
    // transition so their highlights appear immediately, without bypassing the per-chapter throttle on every re-entry.
    var hadHighlightsAccess by remember { mutableStateOf(hasHighlightsAccess) }
    LaunchedEffect(hasHighlightsAccess) {
        if (hasHighlightsAccess && !hadHighlightsAccess) {
            highlightsRepository.ensureHighlightsForChapterLoaded(reference, forceReload = true)
        }
        hadHighlightsAccess = hasHighlightsAccess
    }

    val highlightAlpha = MaterialTheme.readerColorScheme.highlightAlpha
    val highlights =
        remember(cachedHighlights, reference, highlightAlpha, hasHighlightsAccess) {
            if (hasHighlightsAccess) {
                highlightColorsForReference(cachedHighlights, reference, highlightAlpha)
            } else {
                emptyMap()
            }
        }

    LaunchedEffect(reference, textOptions) {
        loadingPhase = BibleTextLoadingPhase.LOADING
        try {
            isVersionRightToLeft = versionRepository.version(reference.versionId).isRightToLeft

            val loadedBlocks =
                BibleVersionRendering.textBlocks(
                    bibleChapterRepository = chapterRepository,
                    reference = reference,
                    renderVerseNumbers = textOptions.renderVerseNumbers,
                    footnoteMode = textOptions.footnoteMode,
                    renderHeadlines = textOptions.renderHeadlines,
                    footnoteMarker = textOptions.footnoteMarker,
                    textColor = textOptions.textColor ?: Color.Unspecified,
                    wocColor = textOptions.wocColor,
                    fonts = BibleTextFonts(fontFamily = textOptions.fontFamily, baseSize = textOptions.fontSize),
                )

            if (loadedBlocks != null) {
                blocks = loadedBlocks
                loadingPhase = BibleTextLoadingPhase.SUCCESS
            } else {
                loadingPhase = BibleTextLoadingPhase.FAILED
            }
        } catch (_: BibleVersionApiException) {
            loadingPhase = BibleTextLoadingPhase.NOT_PERMITTED
        } catch (e: CancellationException) {
            loadingPhase = BibleTextLoadingPhase.INACTIVE
            throw e
        } catch (e: Exception) {
            println("loadBlocks unexpected error: $e")
            loadingPhase = BibleTextLoadingPhase.FAILED
        }
    }

    val systemLayoutDirection = LocalLayoutDirection.current
    val horizontalAlignment =
        when {
            systemLayoutDirection == LayoutDirection.Ltr && isVersionRightToLeft -> Alignment.End
            systemLayoutDirection == LayoutDirection.Ltr && !isVersionRightToLeft -> Alignment.Start
            systemLayoutDirection == LayoutDirection.Rtl && isVersionRightToLeft -> Alignment.Start
            systemLayoutDirection == LayoutDirection.Rtl && !isVersionRightToLeft -> Alignment.End
            else -> Alignment.Start
        }

    return remember(loadingPhase, blocks, highlights, horizontalAlignment) {
        BibleTextBlocksState(
            loadingPhase = loadingPhase,
            blocks = blocks,
            highlights = highlights,
            horizontalAlignment = horizontalAlignment,
        )
    }
}

/**
 * Emits the chapter held by [state] as one item per block, so a host's own header and footer can share a list with
 * it and a block can be scrolled to by its position.
 *
 * Until the load succeeds this is a single placeholder item instead.
 */
@PlatformInternalApi
fun LazyListScope.bibleTextBlocks(
    state: BibleTextBlocksState,
    textOptions: BibleTextOptions,
    selectedVerses: Set<BibleReference> = emptySet(),
    onVerseTap: ((reference: BibleReference, position: Offset) -> Unit)? = null,
    onFootnoteTap: ((reference: BibleReference, footNotes: List<AnnotatedString>) -> Unit)? = null,
) {
    if (state.loadingPhase != BibleTextLoadingPhase.SUCCESS) {
        item { StandardPlaceholder(state.loadingPhase) }
        return
    }

    itemsIndexed(state.visibleBlocks) { index, _ ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = state.horizontalAlignment,
        ) {
            BibleBlockContent(
                visibleBlocks = state.visibleBlocks,
                index = index,
                textOptions = textOptions,
                selectedVerses = selectedVerses,
                highlights = state.highlights,
                onVerseTap = onVerseTap,
                onFootnoteTap = onFootnoteTap,
            )
        }
    }
}

/**
 * Draws the block at [index] of [visibleBlocks], picking the table or single-run renderer for it and turning a
 * tap on it into either a footnote or a verse.
 *
 * The whole list is needed rather than the one block because a footnote tap gathers the footnotes of the
 * chapter, and because a block is placed against the margin of the one before it.
 */
@Composable
internal fun BibleBlockContent(
    visibleBlocks: List<BibleTextBlock>,
    index: Int,
    textOptions: BibleTextOptions,
    selectedVerses: Set<BibleReference>,
    highlights: Map<BibleReference, Color>,
    onVerseTap: ((reference: BibleReference, position: Offset) -> Unit)?,
    onFootnoteTap: ((reference: BibleReference, footNotes: List<AnnotatedString>) -> Unit)?,
) {
    val block = visibleBlocks[index]
    val coroutineScope = rememberCoroutineScope()

    if (block.rows.isEmpty()) {
        BibleTextBlock(
            block = block,
            textOptions = textOptions,
            isFirstBlock = index == 0,
            previousMarginBottom = if (index == 0) 0.dp else visibleBlocks[index - 1].marginBottom,
            selectedVerses = selectedVerses,
            highlights = highlights,
            onClick = { localPosition, textLayoutResult ->
                coroutineScope.launch {
                    val characterIndex = textLayoutResult.getOffsetForPosition(localPosition)

                    val tappedRef =
                        block.text
                            .getStringAnnotations(
                                tag = BibleReferenceAttribute.NAME,
                                start = characterIndex,
                                end = characterIndex,
                            ).firstOrNull()
                            ?.item
                            ?.let {
                                BibleReference.fromAnnotation(it)
                            }

                    if (tappedRef != null) {
                        val tappedFootnote =
                            block.text
                                .getStringAnnotations(
                                    tag = BibleTextCategoryAttribute.NAME,
                                    start = characterIndex,
                                    end = characterIndex,
                                ).firstOrNull {
                                    it.item == BibleTextCategory.FOOTNOTE_MARKER.name ||
                                        it.item == BibleTextCategory.FOOTNOTE_IMAGE.name
                                }

                        if (tappedFootnote != null) {
                            val footNotes =
                                visibleBlocks.flatMap { it.footnotes }.filter { footnote ->
                                    val referenceAnnotation =
                                        footnote
                                            .getStringAnnotations(
                                                tag = BibleReferenceAttribute.NAME,
                                                start = 0,
                                                end = footnote.text.length,
                                            ).firstOrNull()
                                    referenceAnnotation?.let { annotation ->
                                        tappedRef == BibleReference.fromAnnotation(annotation.item)
                                    } == true
                                }

                            onFootnoteTap?.invoke(tappedRef, footNotes)
                        } else {
                            onVerseTap?.invoke(tappedRef, localPosition)
                        }
                    }
                }
            },
        )
    } else {
        BibleTableBlock(
            block = block,
            textOptions = textOptions,
            selectedVerses = selectedVerses,
            highlights = highlights,
            onVerseTap = onVerseTap,
        )
    }
}

private fun BibleTextBlock.covers(reference: BibleReference): Boolean {
    val texts = if (rows.isEmpty()) listOf(text) else rows.flatten()
    return texts.any { candidate ->
        candidate
            .getStringAnnotations(
                tag = BibleReferenceAttribute.NAME,
                start = 0,
                end = candidate.length,
            ).any { BibleReference.fromAnnotation(it.item).overlaps(reference) }
    }
}
