package com.youversion.platform.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BibleReaderHalfPillPicker(
    bookAndChapter: String,
    versionAbbreviation: String,
    handleChapterTap: () -> Unit,
    handleVersionTap: () -> Unit,
    foregroundColor: Color,
    buttonColor: Color,
    compactMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val height = if (compactMode) 29.dp else 40.dp
    val fontSize = if (compactMode) 10.sp else 14.sp
    val chapterMinWidth = if (compactMode) 53.dp else 60.dp
    val chapterLeadingPadding = if (compactMode) 14.dp else 16.dp
    val chapterTrailingPadding = if (compactMode) 12.dp else 14.dp
    val versionMinWidth = if (compactMode) 30.dp else 36.dp
    val versionLeadingPadding = if (compactMode) 12.dp else 14.dp
    val versionTrailingPadding = if (compactMode) 14.dp else 16.dp

    Row(
        modifier =
            modifier.height(height),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Left button (book and chapter)
        Box(
            modifier =
                Modifier
                    .height(height)
                    .clip(RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50))
                    .background(buttonColor)
                    .clickable { handleChapterTap() }
                    .padding(start = chapterLeadingPadding, end = chapterTrailingPadding)
                    .widthIn(min = chapterMinWidth),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = bookAndChapter,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                color = foregroundColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        // Right button (version abbreviation)
        Box(
            modifier =
                Modifier
                    .height(height)
                    .clip(RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50))
                    .background(buttonColor)
                    .clickable { handleVersionTap() }
                    .padding(start = versionLeadingPadding, end = versionTrailingPadding)
                    .widthIn(min = versionMinWidth),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = versionAbbreviation,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                color = foregroundColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BibleReaderHalfPillPickersViewPreview() {
    Box(
        modifier =
            Modifier
                .background(Color.Gray)
                .padding(16.dp),
    ) {
        BibleReaderHalfPillPicker(
            bookAndChapter = "Genesis 1",
            versionAbbreviation = "KJV",
            handleChapterTap = {},
            handleVersionTap = {},
            foregroundColor = Color.Black,
            buttonColor = Color.White,
            compactMode = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BibleReaderHalfPillPickersViewCompactPreview() {
    Box(
        modifier =
            Modifier
                .background(Color.Gray)
                .padding(16.dp),
    ) {
        BibleReaderHalfPillPicker(
            bookAndChapter = "Genesis 1",
            versionAbbreviation = "KJV",
            handleChapterTap = {},
            handleVersionTap = {},
            foregroundColor = Color.Black,
            buttonColor = Color.White,
            compactMode = true,
        )
    }
}
