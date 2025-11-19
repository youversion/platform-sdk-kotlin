package com.youversion.platform.reader.sheets

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.reader.R
import com.youversion.platform.reader.theme.BibleReaderTheme
import com.youversion.platform.reader.theme.Charcoal
import com.youversion.platform.reader.theme.Cream
import com.youversion.platform.reader.theme.MidnightBlue
import com.youversion.platform.reader.theme.PaperGray
import com.youversion.platform.reader.theme.PureWhite
import com.youversion.platform.reader.theme.ReaderColorScheme
import com.youversion.platform.reader.theme.Sepia
import com.youversion.platform.reader.theme.TrueBlack
import com.youversion.platform.reader.theme.readerColorScheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderFontSettingsSheet(
    onDismissRequest: () -> Unit,
    onSmallerFontClick: () -> Unit,
    onBiggerFontClick: () -> Unit,
    onLineSpacingClick: () -> Unit,
    onFontClick: () -> Unit,
    onThemeSelect: (ReaderColorScheme) -> Unit,
    lineSpacingSettingIndex: Int,
) {
    val sheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(vertical = 16.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier =
                    Modifier
                        .padding(horizontal = 24.dp),
            ) {
                FontSizeButtons(
                    onSmallerFontClick = onSmallerFontClick,
                    onBiggerFontClick = onBiggerFontClick,
                    onLineSpacingClick = onLineSpacingClick,
                    lineSpacingSettingIndex = lineSpacingSettingIndex,
                )
                FontDisplayButton(
                    onFontClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                        onFontClick()
                    },
                )
            }

            ThemePicker(
                onThemeSelect = onThemeSelect,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FontSizeButtons(
    onSmallerFontClick: () -> Unit,
    onBiggerFontClick: () -> Unit,
    onLineSpacingClick: () -> Unit,
    lineSpacingSettingIndex: Int,
) {
    val minWidth = 126.dp
    val minHeight = 48.dp

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Left button (Smaller Font)
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .heightIn(min = minHeight)
                        .widthIn(min = minWidth)
                        .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                        .background(MaterialTheme.readerColorScheme.buttonPrimaryColor)
                        .clickable(
                            interactionSource = null,
                            enabled = true,
                            indication = ripple(),
                            onClick = onSmallerFontClick,
                        ),
            ) {
                Text(
                    text = "A",
                    style =
                        TextStyle(
                            fontSize = 14.sp,
                        ),
                )
            }

            // Right button (Larger Font)
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .heightIn(min = minHeight)
                        .widthIn(min = minWidth)
                        .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                        .background(MaterialTheme.readerColorScheme.buttonPrimaryColor)
                        .clickable(
                            interactionSource = null,
                            enabled = true,
                            indication = ripple(),
                            onClick = onBiggerFontClick,
                        ),
            ) {
                Text(
                    text = "A",
                    style =
                        TextStyle(
                            fontSize = 28.sp,
                        ),
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .heightIn(min = minHeight)
                    .widthIn(min = 57.dp)
                    .clip(RoundedCornerShape(size = 8.dp))
                    .background(MaterialTheme.readerColorScheme.buttonPrimaryColor)
                    .clickable(
                        interactionSource = null,
                        enabled = true,
                        indication = ripple(),
                        onClick = onLineSpacingClick,
                    ),
        ) {
            val targetSpacing = (3 * (lineSpacingSettingIndex + 1)).dp
            val animatedSpacing by animateDpAsState(targetValue = targetSpacing, label = "lineSpacingAnimation")

            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy((animatedSpacing)),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(32.dp)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.onBackground),
                )
                Box(
                    modifier =
                        Modifier
                            .width(32.dp)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.onBackground),
                )
                Box(
                    modifier =
                        Modifier
                            .width(32.dp)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.onBackground),
                )
            }
        }
    }
}

@Composable
private fun FontDisplayButton(onFontClick: () -> Unit) {
    OutlinedButton(
        shape = RoundedCornerShape(8.dp),
        onClick = onFontClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.font_settings_label),
                )
                Text(
                    text = "Baskerfille",
                )
            }
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_forward),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun ThemePicker(onThemeSelect: (ReaderColorScheme) -> Unit) {
    val colorSchemes =
        listOf(
            PureWhite,
            Sepia,
            PaperGray,
            Cream,
            Charcoal,
            MidnightBlue,
            TrueBlack,
        )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(colorSchemes) { colorScheme ->
            ThemePickerItem(
                colorScheme = colorScheme,
                selected = MaterialTheme.readerColorScheme == colorScheme,
                onClick = { BibleReaderTheme.selectedColorScheme.value = colorScheme },
            )
        }
    }
}

@Composable
private fun ThemePickerItem(
    colorScheme: ReaderColorScheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            Modifier
                .width(56.dp)
                .height(94.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = null,
                    enabled = true,
                    indication = ripple(color = { colorScheme.foreground }),
                    onClick = onClick,
                ).background(colorScheme.background)
                .border(1.dp, colorScheme.borderSecondaryColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(top = 4.dp),
    ) {
        TextLinePlaceholder(colorScheme = colorScheme)
        TextLinePlaceholder(widthFraction = 0.8f, colorScheme = colorScheme)
        TextLinePlaceholder(colorScheme = colorScheme)
        TextLinePlaceholder(widthFraction = 0.5f, colorScheme = colorScheme)

        Spacer(modifier = Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector =
                    if (selected) {
                        Icons.Default.CheckCircle
                    } else {
                        ImageVector.vectorResource(R.drawable.ic_circle)
                    },
                contentDescription = null,
                tint =
                    if (selected) {
                        colorScheme.readerTextPrimaryColor
                    } else {
                        colorScheme.readerTextMutedColor
                    },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TextLinePlaceholder(
    widthFraction: Float = 1.0f,
    colorScheme: ReaderColorScheme = MaterialTheme.readerColorScheme,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth(fraction = widthFraction)
                .height(2.dp)
                .background(colorScheme.foreground),
    )
}

@Preview
@Composable
private fun Preview_BibleReaderFontSettingsSheet() {
    MaterialTheme {
        BibleReaderFontSettingsSheet(
            onDismissRequest = {},
            onSmallerFontClick = {},
            onBiggerFontClick = {},
            onLineSpacingClick = {},
            onFontClick = {},
            onThemeSelect = {},
            lineSpacingSettingIndex = 1,
        )
    }
}
