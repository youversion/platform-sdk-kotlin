package com.youversion.platform.ui.views.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youversion.platform.ui.R

@Composable
internal fun BibleAppLogo() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.widget_bible_app),
                style =
                    TextStyle(
                        fontWeight = FontWeight.W500,
                    ),
            )
        }
        Image(
            imageVector = ImageVector.vectorResource(R.drawable.yv_bibleapp),
            contentDescription = "YouVersion Logo",
            modifier = Modifier.size(28.dp),
        )
    }
}
