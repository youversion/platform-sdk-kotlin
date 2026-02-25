package com.youversion.platform.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.youversion.platform.core.bibles.domain.BibleIntroRepository
import com.youversion.platform.core.bibles.domain.BibleVersionRepository
import com.youversion.platform.core.utilities.exceptions.BibleVersionApiException
import com.youversion.platform.foundation.PlatformKoinGraph
import com.youversion.platform.ui.views.rendering.BibleTextBlock
import com.youversion.platform.ui.views.rendering.BibleVersionRendering
import kotlinx.coroutines.CancellationException

/**
 * A read-only Composable that renders Bible intro chapter content.
 *
 * This view reuses the same rendering pipeline as [BibleText] for consistent
 * styling, but does not support verse selection or tapping.
 *
 * @param versionId The Bible version ID.
 * @param bookUSFM The book USFM code (e.g., "GEN"). The passage ID is derived as "{bookUSFM}.INTRO".
 * @param textOptions Text styling options (font family, font size, line spacing, etc.).
 * @param placeholder A composable to display during loading and error states.
 * @param onStateChange Callback invoked when the loading phase changes.
 */
@Composable
fun BibleIntroText(
    versionId: Int,
    bookUSFM: String,
    textOptions: BibleTextOptions = BibleTextOptions(),
    placeholder: @Composable (BibleTextLoadingPhase) -> Unit = { StandardPlaceholder(it) },
    onStateChange: (BibleTextLoadingPhase) -> Unit = {},
) {
    var blocks by remember { mutableStateOf<List<BibleTextBlock>>(emptyList()) }
    var loadingPhase by remember { mutableStateOf(BibleTextLoadingPhase.INACTIVE) }
    var isVersionRightToLeft by remember { mutableStateOf(false) }
    val versionRepository: BibleVersionRepository = PlatformKoinGraph.koinApplication.koin.get()
    val introRepository: BibleIntroRepository = PlatformKoinGraph.koinApplication.koin.get()

    LaunchedEffect(loadingPhase) {
        onStateChange(loadingPhase)
    }

    LaunchedEffect(versionId, bookUSFM, textOptions) {
        loadingPhase = BibleTextLoadingPhase.LOADING
        try {
            isVersionRightToLeft = versionRepository.version(versionId).isRightToLeft

            val passageId = "${bookUSFM.uppercase()}.INTRO"
            val htmlContent = introRepository.introContent(versionId, passageId)
            val loadedBlocks =
                BibleVersionRendering.introTextBlocks(
                    htmlContent = htmlContent,
                    versionId = versionId,
                    bookUSFM = bookUSFM,
                    renderHeadlines = textOptions.renderHeadlines,
                    footnoteMode = textOptions.footnoteMode,
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
            println("loadIntroBlocks unexpected error: $e")
            loadingPhase = BibleTextLoadingPhase.FAILED
        }
    }

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
                    val marginTop = if (index == 0) 0.dp else block.marginTop
                    Text(
                        text = block.text,
                        textAlign = block.alignment,
                        lineHeight = textOptions.lineSpacing ?: (textOptions.fontSize * 1.5),
                        modifier =
                            Modifier
                                .padding(top = marginTop)
                                .fillMaxWidth(),
                        inlineContent = textOptions.inlineContentMap,
                    )
                } else {
                    IntroTableBlock(block = block, textOptions = textOptions)
                }
            }
        }
    }
}

@Composable
private fun IntroTableBlock(
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
                        Box(modifier = Modifier.weight(1f)) {
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
