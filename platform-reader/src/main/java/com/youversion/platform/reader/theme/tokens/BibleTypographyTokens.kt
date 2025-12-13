package com.youversion.platform.reader.theme.tokens

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.youversion.platform.reader.theme.AktivGrotesk
import com.youversion.platform.reader.theme.UntitledSerif

internal object BibleTypographyTokens {
    // ----- Header
    val HeaderXXL =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            lineHeight = 40.sp,
            letterSpacing = (-1.2).sp,
        )
    val HeaderXL =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.96).sp,
        )
    val HeaderL =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp,
            lineHeight = 25.sp,
            letterSpacing = (-0.75).sp,
        )
    val HeaderM =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 20.sp,
        )
    val HeaderS =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 16.sp,
        )

    // ----- Eyebrow
    val EyebrowM =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = 13.sp,
            letterSpacing = 2.08.sp,
        )
    val EyebrowS =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 11.sp,
            letterSpacing = 1.76.sp,
        )

    // ----- Label
    val LabelXL =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 20.sp,
        )
    val LabelL =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 16.sp,
        )
    val LabelM =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 13.sp,
        )
    val LabelS =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 11.sp,
        )

    // ----- Paragraph
    val ParagraphXL =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 30.sp,
        )
    val ParagraphL =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        )
    val ParagraphM =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 19.5.sp,
        )
    val ParagraphS =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.5.sp,
        )

    // ----- Caption
    val CaptionL =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 16.sp,
        )
    val CaptionS =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    val CaptionXS =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 16.sp,
        )

    // ----- Scripture
    val ScriptureXXLStrong =
        TextStyle(
            fontFamily = UntitledSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 40.sp,
            lineHeight = 46.4.sp,
            letterSpacing = (-1.6).sp,
        )
    val ScriptureXXL =
        ScriptureXXLStrong.copy(
            fontWeight = FontWeight.Normal,
        )
    val ScriptureXLStrong =
        TextStyle(
            fontFamily = UntitledSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 30.sp,
            lineHeight = 39.9.sp,
            letterSpacing = (-1.2).sp,
        )
    val ScriptureXL =
        ScriptureXLStrong.copy(
            fontWeight = FontWeight.Normal,
        )
    val ScriptureLStrong =
        TextStyle(
            fontFamily = UntitledSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 25.sp,
            lineHeight = 33.25.sp,
            letterSpacing = (-0.96).sp,
        )
    val ScriptureL =
        ScriptureLStrong.copy(
            fontWeight = FontWeight.Normal,
        )

    val ScriptureMStrong =
        TextStyle(
            fontFamily = UntitledSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 21.sp,
            lineHeight = 27.93.sp,
            letterSpacing = (-0.84).sp,
        )
    val ScriptureM =
        ScriptureMStrong.copy(
            fontWeight = FontWeight.Normal,
        )
    val ScriptureSStrong =
        TextStyle(
            fontFamily = UntitledSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 23.94.sp,
        )
    val ScriptureS =
        ScriptureSStrong.copy(
            fontWeight = FontWeight.Normal,
        )

    // ----- Button Label
    val ButtonLabelL =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 16.sp,
        )
    val ButtonLabelM =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = 16.sp,
        )
    val ButtonLabelS =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    val ButtonLabelXS =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    val ButtonLabelXXS =
        TextStyle(
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 16.sp,
        )
}
