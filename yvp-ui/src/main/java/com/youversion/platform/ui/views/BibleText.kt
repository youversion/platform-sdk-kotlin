package com.youversion.platform.ui.views

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.utilities.exceptions.BibleVersionApiException
import com.youversion.platform.ui.views.rendering.BibleReferenceAttribute
import com.youversion.platform.ui.views.rendering.BibleTextBlock
import com.youversion.platform.ui.views.rendering.BibleTextCategory
import com.youversion.platform.ui.views.rendering.BibleTextCategoryAttribute
import com.youversion.platform.ui.views.rendering.BibleVersionRendering
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class BibleTextOptions(
    val fontFamily: FontFamily = FontFamily.Serif,
    val fontSize: TextUnit = 16.sp,
    val lineSpacing: TextUnit? = null,
    val textColor: Color? = null,
    val wocColor: Color = Color(0xFFF04C59), // YouVersion red
    val footnoteMode: BibleTextFootnoteMode = BibleTextFootnoteMode.NONE,
    val footnoteMarker: AnnotatedString? = null,
)

enum class BibleTextFootnoteMode {
    NONE,
    INLINE,
    MARKER,
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
    onFootnoteTap: (AnnotatedString) -> Unit = {},
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
                    renderFootnotes = textOptions.footnoteMode != BibleTextFootnoteMode.NONE,
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
                                // Find the reference at the tapped index
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
                                    // Handle single tap action
                                    onVerseTap?.invoke(
                                        tappedRef,
                                        localPosition,
                                    ) // Position is local to the Text composable

                                    // Handle selection change (like a long press would)
                                    val newSelection = selectedVerses.toMutableSet()
                                    if (newSelection.contains(tappedRef)) {
                                        newSelection.remove(tappedRef)
                                    } else {
                                        newSelection.add(tappedRef)
                                    }
                                    onVerseSelectedChange(newSelection)
                                } else {
                                    // Check for footnote tap
                                    block.text
                                        .getStringAnnotations(
                                            tag = BibleTextCategoryAttribute.NAME,
                                            start = characterIndex,
                                            end = characterIndex,
                                        ).firstOrNull {
                                            it.item.startsWith(BibleTextCategory.FOOTNOTE_MARKER.name)
                                        }?.item
                                        ?.let {
                                            block.footnotes[it.split(":")[1].toInt()]
                                        }?.let {
                                            onFootnoteTap(it)
                                        }
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
                    icon = Icons.Default.Lock,
                    text = "Your previously selected Bible version is unavailable. Please switch to another one.",
                )

            BibleTextLoadingPhase.FAILED ->
                PlaceholderMessage(
                    icon = Icons.Default.Warning,
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
