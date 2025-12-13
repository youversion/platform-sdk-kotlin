package com.youversion.platform.reader.theme.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import com.youversion.platform.reader.theme.tokens.BibleTypographyTokens

class BibleReaderTypography internal constructor(
    val headerXXL: TextStyle = BibleTypographyTokens.HeaderXXL,
    val headerXL: TextStyle = BibleTypographyTokens.HeaderXL,
    val headerL: TextStyle = BibleTypographyTokens.HeaderL,
    val headerM: TextStyle = BibleTypographyTokens.HeaderM,
    val headerS: TextStyle = BibleTypographyTokens.HeaderS,
    val eyebrowM: TextStyle = BibleTypographyTokens.EyebrowM,
    val eyebrowS: TextStyle = BibleTypographyTokens.EyebrowS,
    val labelXL: TextStyle = BibleTypographyTokens.LabelXL,
    val labelL: TextStyle = BibleTypographyTokens.LabelL,
    val labelM: TextStyle = BibleTypographyTokens.LabelM,
    val labelS: TextStyle = BibleTypographyTokens.LabelS,
    val paragraphXL: TextStyle = BibleTypographyTokens.ParagraphXL,
    val paragraphL: TextStyle = BibleTypographyTokens.ParagraphL,
    val paragraphM: TextStyle = BibleTypographyTokens.ParagraphM,
    val paragraphS: TextStyle = BibleTypographyTokens.ParagraphS,
    val captionL: TextStyle = BibleTypographyTokens.CaptionL,
    val captionS: TextStyle = BibleTypographyTokens.CaptionS,
    val captionXS: TextStyle = BibleTypographyTokens.CaptionXS,
    val scriptureXXLStrong: TextStyle = BibleTypographyTokens.ScriptureXXLStrong,
    val scriptureXXL: TextStyle = BibleTypographyTokens.ScriptureXXL,
    val scriptureXLStrong: TextStyle = BibleTypographyTokens.ScriptureXLStrong,
    val scriptureXL: TextStyle = BibleTypographyTokens.ScriptureXL,
    val scriptureLStrong: TextStyle = BibleTypographyTokens.ScriptureLStrong,
    val scriptureL: TextStyle = BibleTypographyTokens.ScriptureL,
    val scriptureMStrong: TextStyle = BibleTypographyTokens.ScriptureMStrong,
    val scriptureM: TextStyle = BibleTypographyTokens.ScriptureM,
    val scriptureSStrong: TextStyle = BibleTypographyTokens.ScriptureSStrong,
    val scriptureS: TextStyle = BibleTypographyTokens.ScriptureS,
    val buttonLabelL: TextStyle = BibleTypographyTokens.ButtonLabelL,
    val buttonLabelM: TextStyle = BibleTypographyTokens.ButtonLabelM,
    val buttonLabelS: TextStyle = BibleTypographyTokens.ButtonLabelS,
    val buttonLabelXS: TextStyle = BibleTypographyTokens.ButtonLabelXS,
    val buttonLabelXXS: TextStyle = BibleTypographyTokens.ButtonLabelXXS,
)

internal val LocalBibleReaderTypography = staticCompositionLocalOf { BibleReaderTypography() }
