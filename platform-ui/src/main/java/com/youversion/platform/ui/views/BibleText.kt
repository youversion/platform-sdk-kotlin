package com.youversion.platform.ui.views

import androidx.compose.animation.core.copy
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.utilities.exceptions.BibleVersionApiException
import com.youversion.platform.ui.R
import com.youversion.platform.ui.views.rendering.BibleReferenceAttribute
import com.youversion.platform.ui.views.rendering.BibleTextBlock
import com.youversion.platform.ui.views.rendering.BibleTextCategory
import com.youversion.platform.ui.views.rendering.BibleTextCategoryAttribute
import com.youversion.platform.ui.views.rendering.BibleVersionRendering
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.nio.file.WatchEvent

data class BibleTextOptions(
    val fontFamily: FontFamily = FontFamily.Serif,
    val fontSize: TextUnit = 16.sp,
    val lineSpacing: TextUnit? = null,
    val textColor: Color? = null,
    val wocColor: Color = Color(0xFFF04C59), // YouVersion red
    val renderHeadlines: Boolean = true,
    val renderVerseNumbers: Boolean = true,
    val footnoteMode: BibleTextFootnoteMode = BibleTextFootnoteMode.NONE,
    val footnoteMarker: AnnotatedString? = getDefaultFootnoteMarker(),
) {
    val inlineContentMap =
        mapOf(
            FOOTNOTE_IMAGE_ID to
                InlineTextContent(
                    Placeholder(
                        width = 24.sp,
                        height = 32.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                    ),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .alpha(0.8f),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_material_footnotes),
                            contentDescription = stringResource(R.string.footnote_content_desc),
                            modifier = Modifier.size(20.dp),
                            tint = LocalContentColor.current.copy(alpha = 0.6f),
                        )
                    }
                },
        )

    companion object {
        const val FOOTNOTE_IMAGE_ID = "footnote_image_id"

        fun getImageFootnoteMarker(): AnnotatedString =
            buildAnnotatedString { appendInlineContent(id = FOOTNOTE_IMAGE_ID) }

        fun getDefaultFootnoteMarker(): AnnotatedString =
            buildAnnotatedString {
                pushStyle(SpanStyle(baselineShift = BaselineShift.Superscript))
                append("\u00A0※ ")
                pop()
            }
    }
}

enum class BibleTextFootnoteMode {
    NONE,
    INLINE,
    MARKER,
    LETTERS,
    IMAGE,
}

enum class BibleTextLoadingPhase {
    INACTIVE,
    LOADING,
    FAILED,
    NOT_PERMITTED,
    SUCCESS,
}

@Composable
fun BibleText(
    reference: BibleReference,
    textOptions: BibleTextOptions = BibleTextOptions(),
    selectedVerses: Set<BibleReference> = emptySet(),
    onVerseSelectedChange: (Set<BibleReference>) -> Unit = {},
    onVerseTap: ((reference: BibleReference, position: Offset) -> Unit)? = null,
    onFootnoteTap: ((reference: BibleReference, footNotes: List<AnnotatedString>) -> Unit)? = null,
    placeholder: @Composable (BibleTextLoadingPhase) -> Unit = { StandardPlaceholder(it) },
    onStateChange: (BibleTextLoadingPhase) -> Unit = {},
) {
    var blocks by remember { mutableStateOf<List<BibleTextBlock>>(emptyList()) }
    var loadingPhase by remember { mutableStateOf(BibleTextLoadingPhase.INACTIVE) }
    var isVersionRightToLeft by remember { mutableStateOf(false) }
    val bibleVersionRepository = BibleVersionRepository(LocalContext.current)

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(loadingPhase) {
        onStateChange(loadingPhase)
    }

    LaunchedEffect(reference, textOptions) {
        loadingPhase = BibleTextLoadingPhase.LOADING
        try {
            isVersionRightToLeft = bibleVersionRepository.version(reference.versionId).isRightToLeft

            val loadedBlocks =
                BibleVersionRendering.textBlocks(
                    bibleVersionRepository = bibleVersionRepository,
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

    // Determine the alignment for the main column
    val systemLayoutDirection = LocalLayoutDirection.current
    val mainColumnAlignment =
        when {
            systemLayoutDirection == LayoutDirection.Ltr && isVersionRightToLeft -> Alignment.End
            systemLayoutDirection == LayoutDirection.Ltr && !isVersionRightToLeft -> Alignment.Start
            systemLayoutDirection == LayoutDirection.Rtl && isVersionRightToLeft -> Alignment.Start
            systemLayoutDirection == LayoutDirection.Rtl && !isVersionRightToLeft -> Alignment.End
            else -> Alignment.Start
        }

    if (loadingPhase != BibleTextLoadingPhase.SUCCESS) {
        placeholder(loadingPhase)
    } else {
        Column(horizontalAlignment = mainColumnAlignment) {
            val visibleBlocks = remember(blocks) { blocks.filter { it.text.isNotBlank() || it.rows.isNotEmpty() } }
            visibleBlocks.forEachIndexed { index, block ->
                if (block.rows.isEmpty()) {
                    BibleTextBlock(
                        block = block,
                        textOptions = textOptions,
                        isFirstBlock = index == 0,
                        onTap = { localPosition, textLayoutResult ->
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
                                            block.footnotes.filter { footnote ->
                                                val referenceAnnotation =
                                                    footnote
                                                        .getStringAnnotations(
                                                            tag = BibleReferenceAttribute.NAME,
                                                            start = 0,
                                                            end = footnote.text.length,
                                                        ).firstOrNull()
                                                referenceAnnotation?.let { annotation ->
                                                    tappedRef ==
                                                        BibleReference.fromAnnotation(annotation.item)
                                                } == true
                                            }

                                        onFootnoteTap?.invoke(
                                            tappedRef,
                                            footNotes,
                                        )
                                    } else {
                                        onVerseTap?.invoke(
                                            tappedRef,
                                            localPosition,
                                        )
                                    }

                                    val newSelection = selectedVerses.toMutableSet()
                                    if (newSelection.contains(tappedRef)) {
                                        newSelection.remove(tappedRef)
                                    } else {
                                        newSelection.add(tappedRef)
                                    }
                                    onVerseSelectedChange(newSelection)
                                }
                            }
                        },
                    )
                } else {
                    BibleTableBlock(block = block, textOptions = textOptions)
                }
            }
        }
    }
}

fun BibleReference.Companion.fromAnnotation(annotation: String): BibleReference {
    val split = annotation.split(":")
    return BibleReference(
        versionId = split[0].toInt(),
        bookUSFM = split[1],
        chapter = split[2].toInt(),
        verse = split[3].toInt(),
    )
}

@Composable
private fun BibleTextBlock(
    block: BibleTextBlock,
    textOptions: BibleTextOptions,
    isFirstBlock: Boolean,
    onTap: (position: Offset, layoutResult: TextLayoutResult) -> Unit,
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val marginTop = if (isFirstBlock) 0.dp else block.marginTop

    Text(
        text = block.text,
        textAlign = block.alignment,
        lineHeight = textOptions.lineSpacing ?: (textOptions.fontSize * 2),
        modifier =
            Modifier
                .padding(top = marginTop)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { position ->
                            textLayoutResult?.let { layoutResult ->
                                onTap(position, layoutResult)
                            }
                        },
                        // onLongPress could also be used for selection
                    )
                },
        onTextLayout = { result ->
            textLayoutResult = result
        },
        inlineContent = textOptions.inlineContentMap,
    )
}

@Composable
private fun BibleTableBlock(
    block: BibleTextBlock,
    textOptions: BibleTextOptions,
) {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val numCols = block.rows.maxOfOrNull { it.size } ?: 0
        block.rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                for (i in 0 until numCols) {
                    val cellText = row.getOrNull(i) ?: AnnotatedString("")
                    if (i == 0) {
                        Box(
                            modifier =
                                Modifier.weight(1f),
                        ) {
                            Text(
                                text = cellText,
                                lineHeight = textOptions.lineSpacing ?: TextUnit.Unspecified,
                                color = textOptions.textColor ?: Color.Unspecified,
                            )
                        }
                    } else {
                        Box {
                            Text(
                                text = cellText,
                                lineHeight = textOptions.lineSpacing ?: TextUnit.Unspecified,
                                color = textOptions.textColor ?: Color.Unspecified,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StandardPlaceholder(phase: BibleTextLoadingPhase) {
    Box(
        modifier =
            Modifier
                .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (phase) {
            BibleTextLoadingPhase.INACTIVE -> {}
            BibleTextLoadingPhase.LOADING -> CircularProgressIndicator()
            BibleTextLoadingPhase.NOT_PERMITTED ->
                PlaceholderMessage(
                    icon = ImageVector.vectorResource(R.drawable.ic_material_lock),
                    text = "Your previously selected Bible version is unavailable. Please switch to another one.",
                )

            BibleTextLoadingPhase.FAILED ->
                PlaceholderMessage(
                    icon = ImageVector.vectorResource(R.drawable.ic_material_warning),
                    text =
                        "We’re having difficulties with your connection. " +
                            "Please download a Bible version when you’re online.",
                )

            BibleTextLoadingPhase.SUCCESS -> {}
        }
    }
}

@Composable
private fun PlaceholderMessage(
    icon: ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
        Text(text)
    }
}
