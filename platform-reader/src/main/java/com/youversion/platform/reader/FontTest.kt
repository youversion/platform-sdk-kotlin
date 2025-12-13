package com.youversion.platform.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.reader.theme.AktivGrotesk
import com.youversion.platform.reader.theme.tokens.BibleTypographyTokens

@Composable
fun FontTest() {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = "Aktiv Grotesk Bold",
            style = BibleTypographyTokens.HeaderXXXL,
        )
        Text(
            "Aktiv Grotesk Regular",
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Normal,
            fontSize = 44.sp,
        )
        Text(
            "Aktiv Grotesk Medium",
            fontFamily = AktivGrotesk,
            fontWeight = FontWeight.Medium,
            fontSize = 44.sp,
        )
    }
}
