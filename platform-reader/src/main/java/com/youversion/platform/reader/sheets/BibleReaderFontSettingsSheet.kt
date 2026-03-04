package com.youversion.platform.reader.sheets

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.youversion.platform.reader.FontDefinition
import com.youversion.platform.reader.R
import com.youversion.platform.reader.ReaderFontSettings
import com.youversion.platform.reader.theme.Charcoal
import com.youversion.platform.reader.theme.Cream
import com.youversion.platform.reader.theme.MidnightBlue
import com.youversion.platform.reader.theme.PaperGray
import com.youversion.platform.reader.theme.PureWhite
import com.youversion.platform.reader.theme.ReaderColorScheme
import com.youversion.platform.reader.theme.ReaderTheme
import com.youversion.platform.reader.theme.Sepia
import com.youversion.platform.reader.theme.TrueBlack
import com.youversion.platform.reader.theme.readerColorScheme
import com.youversion.platform.reader.theme.ui.BibleReaderTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderFontSettingsSheet(
    onDismissRequest: () -> Unit,
    onSmallerFontClick: () -> Unit,
    onBiggerFontClick: () -> Unit,
    onFontClick: () -> Unit,
    onThemeSelect: (ReaderTheme) -> Unit,
    fontDefinition: FontDefinition,
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
                )
                FontDisplayButton(
                    fontDefinition = fontDefinition,
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
) {
    val minHeight = 48.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = minHeight)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .background(MaterialTheme.readerColorScheme.buttonSecondaryColor)
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

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = minHeight)
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .background(MaterialTheme.readerColorScheme.buttonSecondaryColor)
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
}

@Composable
private fun FontDisplayButton(
    fontDefinition: FontDefinition,
    onFontClick: () -> Unit,
) {
    OutlinedButton(
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
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
                    style = TextStyle(fontSize = 12.sp),
                    color = BibleReaderTheme.colorScheme.textMuted,
                )
                Text(
                    text = fontDefinition.fontName,
                    style = TextStyle(fontSize = 20.sp, fontFamily = fontDefinition.fontFamily),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ThemePicker(onThemeSelect: (ReaderTheme) -> Unit) {
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
        items(ReaderTheme.allThemes) { readerTheme ->
            ThemePickerItem(
                colorScheme = readerTheme.colorScheme,
                selected = MaterialTheme.readerColorScheme == readerTheme.colorScheme,
                onClick = { onThemeSelect(readerTheme) },
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
                .width(64.dp)
                .height(94.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = null,
                    enabled = true,
                    indication = ripple(color = { colorScheme.foreground }),
                    onClick = onClick,
                ).background(colorScheme.background)
                .border(1.dp, colorScheme.borderSecondaryColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp)
                .padding(top = 20.dp, bottom = 10.dp),
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
                        colorScheme.foreground
                    },
                modifier = Modifier.size(24.dp),
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
            onFontClick = {},
            onThemeSelect = {},
            fontDefinition = ReaderFontSettings.DEFAULT_FONT_DEFINITION,
        )
    }
}
