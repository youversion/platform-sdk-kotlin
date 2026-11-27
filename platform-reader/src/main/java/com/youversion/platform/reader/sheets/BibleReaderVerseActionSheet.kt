package com.youversion.platform.reader.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.reader.BibleReaderVerseAction
import com.youversion.platform.reader.R
import com.youversion.platform.reader.icons.BookmarkIcon
import com.youversion.platform.reader.icons.CopyIcon
import com.youversion.platform.ui.theme.ReaderColorScheme
import com.youversion.platform.ui.theme.readerColorScheme
import com.youversion.platform.ui.R as UiR

/**
 * The actions offered for the current verse selection, in the order of [actions].
 *
 * Copy and share need no account. Highlighting does, so [showsHighlightColors] hides the color picker entirely when
 * the reader has no account and no way to get one — a control that could never work is not shown. When the reader is
 * signed out but sign-in is available, the colors are shown and tapping one asks them to sign in.
 */
@Composable
internal fun BibleReaderVerseActionSheet(
    actions: List<BibleReaderVerseAction>,
    colorsToRemove: List<HighlightColor>,
    colorsToAdd: List<HighlightColor>,
    showsHighlightColors: Boolean,
    onAddHighlight: (String) -> Unit,
    onRemoveHighlight: (String) -> Unit,
    onSave: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onCustomAction: (BibleReaderVerseAction) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { action ->
                when (action.kind) {
                    BibleReaderVerseAction.Kind.HIGHLIGHT -> {
                        if (showsHighlightColors && (colorsToRemove.isNotEmpty() || colorsToAdd.isNotEmpty())) {
                            HighlightColorPicker(
                                colorsToRemove = colorsToRemove,
                                colorsToAdd = colorsToAdd,
                                onAddHighlight = onAddHighlight,
                                onRemoveHighlight = onRemoveHighlight,
                            )
                        }
                    }

                    BibleReaderVerseAction.Kind.SAVE -> {
                        VerseActionButton(
                            icon = BookmarkIcon,
                            label = stringResource(UiR.string.notes_save),
                            onClick = onSave,
                        )
                    }

                    BibleReaderVerseAction.Kind.COPY -> {
                        VerseActionButton(
                            icon = CopyIcon,
                            label = stringResource(UiR.string.copy),
                            onClick = onCopy,
                        )
                    }

                    BibleReaderVerseAction.Kind.SHARE -> {
                        VerseActionButton(
                            icon = Icons.Outlined.Share,
                            label = stringResource(UiR.string.share),
                            onClick = onShare,
                        )
                    }

                    BibleReaderVerseAction.Kind.CUSTOM -> {
                        VerseActionButton(
                            icon = requireNotNull(action.icon),
                            label = requireNotNull(action.label),
                            onClick = { onCustomAction(action) },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun HighlightColorPicker(
    colorsToRemove: List<HighlightColor>,
    colorsToAdd: List<HighlightColor>,
    onAddHighlight: (String) -> Unit,
    onRemoveHighlight: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            Modifier
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                .padding(horizontal = 12.dp),
    ) {
        colorsToRemove.forEach { highlightColor ->
            HighlightColorButton(
                highlightColor = highlightColor,
                isSelected = true,
                onClick = { onRemoveHighlight(highlightColor.hexColor) },
            )
        }
        colorsToAdd.forEach { highlightColor ->
            HighlightColorButton(
                highlightColor = highlightColor,
                isSelected = false,
                onClick = { onAddHighlight(highlightColor.hexColor) },
            )
        }
    }
}

/**
 * A single swatch in the highlight color picker.
 *
 * The swatch is mixed into the reader background so it shows the color the highlight will actually
 * be on the page; the palette color on its own would read differently against the sheet behind it.
 */
@Composable
internal fun HighlightColorButton(
    highlightColor: HighlightColor,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colorName = stringResource(highlightColor.nameResId)
    val contentDescription =
        stringResource(
            if (isSelected) {
                R.string.verse_action_remove_highlight
            } else {
                R.string.verse_action_add_highlight
            },
            colorName,
        )
    val readerColorScheme = MaterialTheme.readerColorScheme
    val swatchColor = highlightColor.swatchColor(readerColorScheme)
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .border(
                    width = 1.dp,
                    color = readerColorScheme.foreground.copy(alpha = 0.2f),
                    shape = CircleShape,
                ).clickable(
                    interactionSource = null,
                    indication = ripple(),
                    onClick = onClick,
                ).semantics { this.contentDescription = contentDescription },
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = readerColorScheme.foreground,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Resolves the palette color of this highlight into the color its picker swatch is filled with,
 * mixing it into the reader background the same way the verse fill is so the swatch shows the color
 * the highlight will actually be on the page.
 */
internal fun HighlightColor.swatchColor(readerColorScheme: ReaderColorScheme): Color =
    readerColorScheme.mixedHighlightColor(color)

@Composable
private fun VerseActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            Modifier
                .sizeIn(minWidth = 56.dp, minHeight = 56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = null,
                    indication = ripple(),
                    onClick = onClick,
                ).padding(horizontal = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
