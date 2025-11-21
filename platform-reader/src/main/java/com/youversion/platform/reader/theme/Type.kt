package com.youversion.platform.reader.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.youversion.platform.reader.R

val UntitledSerif =
    FontFamily(
        Font(R.font.untitled_serif_app_regular, FontWeight.Normal),
        Font(R.font.untitled_serif_app_regular_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.untitled_serif_app_medium, FontWeight.Medium),
        Font(R.font.untitled_serif_app_medium_italic, FontWeight.Medium, FontStyle.Italic),
        Font(R.font.untitled_serif_app_bold, FontWeight.Bold),
        Font(R.font.untitled_serif_app_bold_italic, FontWeight.Bold, FontStyle.Italic),
    )
