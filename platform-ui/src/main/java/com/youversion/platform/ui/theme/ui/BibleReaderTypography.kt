package com.youversion.platform.ui.theme.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import com.youversion.platform.core.di.PlatformInternalApi
import com.youversion.platform.ui.theme.tokens.BibleTypographyTokens

@PlatformInternalApi
public class BibleReaderTypography internal constructor(
    public val headerXXL: TextStyle = BibleTypographyTokens.HeaderXXL,
    public val headerXL: TextStyle = BibleTypographyTokens.HeaderXL,
    public val headerL: TextStyle = BibleTypographyTokens.HeaderL,
    public val headerM: TextStyle = BibleTypographyTokens.HeaderM,
    public val headerS: TextStyle = BibleTypographyTokens.HeaderS,
    public val eyebrowM: TextStyle = BibleTypographyTokens.EyebrowM,
    public val eyebrowS: TextStyle = BibleTypographyTokens.EyebrowS,
    public val labelXL: TextStyle = BibleTypographyTokens.LabelXL,
    public val labelL: TextStyle = BibleTypographyTokens.LabelL,
    public val labelM: TextStyle = BibleTypographyTokens.LabelM,
    public val labelS: TextStyle = BibleTypographyTokens.LabelS,
    public val paragraphXL: TextStyle = BibleTypographyTokens.ParagraphXL,
    public val paragraphL: TextStyle = BibleTypographyTokens.ParagraphL,
    public val paragraphM: TextStyle = BibleTypographyTokens.ParagraphM,
    public val paragraphS: TextStyle = BibleTypographyTokens.ParagraphS,
    public val captionL: TextStyle = BibleTypographyTokens.CaptionL,
    public val captionS: TextStyle = BibleTypographyTokens.CaptionS,
    public val captionXS: TextStyle = BibleTypographyTokens.CaptionXS,
    public val scriptureXXLStrong: TextStyle = BibleTypographyTokens.ScriptureXXLStrong,
    public val scriptureXXL: TextStyle = BibleTypographyTokens.ScriptureXXL,
    public val scriptureXLStrong: TextStyle = BibleTypographyTokens.ScriptureXLStrong,
    public val scriptureXL: TextStyle = BibleTypographyTokens.ScriptureXL,
    public val scriptureLStrong: TextStyle = BibleTypographyTokens.ScriptureLStrong,
    public val scriptureL: TextStyle = BibleTypographyTokens.ScriptureL,
    public val scriptureMStrong: TextStyle = BibleTypographyTokens.ScriptureMStrong,
    public val scriptureM: TextStyle = BibleTypographyTokens.ScriptureM,
    public val scriptureSStrong: TextStyle = BibleTypographyTokens.ScriptureSStrong,
    public val scriptureS: TextStyle = BibleTypographyTokens.ScriptureS,
    public val buttonLabelL: TextStyle = BibleTypographyTokens.ButtonLabelL,
    public val buttonLabelM: TextStyle = BibleTypographyTokens.ButtonLabelM,
    public val buttonLabelS: TextStyle = BibleTypographyTokens.ButtonLabelS,
    public val buttonLabelXS: TextStyle = BibleTypographyTokens.ButtonLabelXS,
    public val buttonLabelXXS: TextStyle = BibleTypographyTokens.ButtonLabelXXS,
)

internal val LocalBibleReaderTypography = staticCompositionLocalOf { BibleReaderTypography() }
