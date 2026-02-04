package com.youversion.platform.ui.views.votd

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.foundation.PlatformKoinGraph
import com.youversion.platform.ui.R
import com.youversion.platform.ui.di.PlatformUIKoinModule
import com.youversion.platform.ui.views.BibleText
import com.youversion.platform.ui.views.BibleTextOptions
import com.youversion.platform.ui.views.PreviewBackground
import com.youversion.platform.ui.views.components.BibleAppLogo
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.module.rememberKoinModules
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf

@Composable
fun CompactVerseOfTheDay(
    bibleVersionId: Int = 111,
    showIcon: Boolean = true,
    dark: Boolean = isSystemInDarkTheme(),
) {
    InternalVerseOfTheDay(
        bibleVersionId = bibleVersionId,
        showIcon = showIcon,
        dark = dark,
        compact = true,
    )
}

@Composable
fun VerseOfTheDay(
    bibleVersionId: Int = 111,
    showIcon: Boolean = true,
    dark: Boolean = isSystemInDarkTheme(),
    onShareClick: () -> Unit = {},
    onFullChapterClick: () -> Unit = {},
) {
    InternalVerseOfTheDay(
        bibleVersionId = bibleVersionId,
        showIcon = showIcon,
        dark = dark,
        compact = false,
        onShareClick = onShareClick,
        onFullChapterClick = onFullChapterClick,
    )
}

@OptIn(KoinExperimentalAPI::class)
@Composable
private fun InternalVerseOfTheDay(
    bibleVersionId: Int,
    showIcon: Boolean = true,
    dark: Boolean = isSystemInDarkTheme(),
    compact: Boolean = false,
    onShareClick: () -> Unit = {},
    onFullChapterClick: () -> Unit = {},
) {
    KoinIsolatedContext(
        context = PlatformKoinGraph.koinApplication,
    ) {
        rememberKoinModules { listOf(PlatformUIKoinModule) }
        val viewModel: VerseOfTheDayViewModel = koinViewModel { parametersOf(bibleVersionId) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        Box {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                if (state.bibleReference != null && state.bibleVersion != null) {
                    if (compact) {
                        CompactVerseOfTheDayContent(
                            bibleReference = state.bibleReference!!,
                            bibleVersion = state.bibleVersion!!,
                            dark = dark,
                            showIcon = showIcon,
                        )
                    } else {
                        VerseOfTheDayContent(
                            bibleReference = state.bibleReference!!,
                            bibleVersion = state.bibleVersion!!,
                            dark = dark,
                            showIcon = showIcon,
                            onShareClick = onShareClick,
                            onFullChapterClick = onFullChapterClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerseOfTheDayContent(
    bibleReference: BibleReference,
    bibleVersion: BibleVersion,
    dark: Boolean,
    showIcon: Boolean,
    onShareClick: () -> Unit = {},
    onFullChapterClick: () -> Unit = {},
) {
    val foregroundColor = if (dark) Color(0xFFBFBDBD) else Color(0xFF636161)
    val buttonColor = if (dark) Color(0xFF353333) else Color(0xFFEDEBEB)

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (showIcon) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_votd),
                    modifier = Modifier.size(44.dp),
                    contentDescription = null,
                )
            }
            Text(
                text = stringResource(R.string.tab_votd).uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = foregroundColor,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_material_share),
                    modifier = Modifier.size(24.dp),
                    contentDescription = null,
                )
            }
        }
        Box {
            BibleText(
                reference = bibleReference,
                textOptions =
                    BibleTextOptions(
                        fontSize = 16.sp,
                    ),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = bibleVersion.displayTitle(bibleReference, includesVersionAbbreviation = true),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = onFullChapterClick,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = foregroundColor,
                    ),
            ) {
                Text(
                    text = "Full Chapter",
                )
            }
            BibleAppLogo()
        }
    }
}

@Composable
private fun CompactVerseOfTheDayContent(
    bibleReference: BibleReference,
    bibleVersion: BibleVersion,
    dark: Boolean,
    showIcon: Boolean,
) {
    val foregroundColor = if (dark) Color(0xFFBFBDBD) else Color(0xFF636161)

    Column {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (showIcon) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_votd),
                    modifier = Modifier.size(44.dp),
                    contentDescription = null,
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.tab_votd).uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = foregroundColor,
                )

                Text(
                    text = bibleVersion.displayTitle(bibleReference, includesVersionAbbreviation = true),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
        BibleText(
            reference = bibleReference,
            textOptions =
                BibleTextOptions(
                    fontSize = 16.sp,
                ),
        )
    }
}

@Preview
@Composable
private fun Preview_VerseOfTheDay() {
    PreviewBackground(dark = true) {
        InternalVerseOfTheDay(
            bibleVersionId = 1,
        )
    }
}
