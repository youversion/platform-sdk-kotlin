package com.youversion.platform.ui.views.widget

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youversion.platform.core.bibles.domain.BibleReference
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.foundation.PlatformKoinGraph
import com.youversion.platform.ui.R
import com.youversion.platform.ui.di.PlatformUIKoinModule
import com.youversion.platform.ui.utilities.ObserveAsEvents
import com.youversion.platform.ui.views.BibleText
import com.youversion.platform.ui.views.BibleTextOptions
import com.youversion.platform.ui.views.components.BibleAppLogo
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.module.rememberKoinModules
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf

@Composable
fun BibleWidget(
    reference: BibleReference,
    modifier: Modifier = Modifier,
    version: BibleVersion? = null,
    fontSize: TextUnit = 23.sp,
) {
    BibleWidget(
        reference = reference,
        version = version,
        modifier = modifier,
        textOptions =
            BibleTextOptions(
                fontSize = fontSize,
                textColor = MaterialTheme.colorScheme.onBackground,
            ),
    )
}

@OptIn(ExperimentalMaterial3Api::class, KoinExperimentalAPI::class)
@Composable
fun BibleWidget(
    reference: BibleReference,
    textOptions: BibleTextOptions,
    modifier: Modifier = Modifier,
    version: BibleVersion? = null,
) {
    KoinIsolatedContext(
        context = PlatformKoinGraph.koinApplication,
    ) {
        rememberKoinModules { listOf(PlatformUIKoinModule) }

        val context = LocalContext.current

        val viewModel: BibleWidgetViewModel = koinViewModel { parametersOf(reference, version) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        ObserveAsEvents(viewModel.events) { event ->
            when (event) {
                is BibleWidgetViewModel.Event.OnErrorLoadingBibleVersion -> {
                    Toast.makeText(context, "Error loading Bible version", Toast.LENGTH_LONG).show()
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier,
        ) {
            HeaderReference(
                reference = reference,
                version = state.bibleVersion,
            )
            Box(
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
            ) {
                BibleText(
                    reference = reference,
                    textOptions = textOptions,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Copyright(
                    version = state.bibleVersion,
                    onClick = { viewModel.onAction(BibleWidgetViewModel.Action.OnViewCopyright) },
                )
                BibleAppLogo()
            }
        }

        if (state.showCopyright) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.onAction(BibleWidgetViewModel.Action.OnCloseCopyright) },
            ) {
                CopyrightSheetContent(
                    version = state.bibleVersion,
                )
            }
        }
    }
}

@Composable
private fun HeaderReference(
    reference: BibleReference,
    version: BibleVersion?,
) {
    version
        ?.displayTitle(reference)
        ?.let { refText ->
            Text(
                text = refText,
                style =
                    TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
        }
}

@Composable
private fun RowScope.Copyright(
    version: BibleVersion?,
    onClick: () -> Unit = {},
) {
    val copyright = version?.copyright ?: version?.promotionalContent ?: ""
    Text(
        text = copyright,
        textAlign = TextAlign.Start,
        maxLines = 4,
        style =
            TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            ),
        modifier =
            Modifier
                .weight(1f)
                .clickable(
                    interactionSource = null,
                    enabled = true,
                    indication = ripple(),
                    onClick = onClick,
                ),
    )
}

@Composable
private fun CopyrightSheetContent(version: BibleVersion?) {
    Column(
        modifier =
            Modifier
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = version?.localizedTitle ?: stringResource(R.string.widget_copyright),
            style =
                TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Text(
            text = version?.promotionalContent ?: version?.copyright ?: "",
        )
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Preview
@Composable
private fun Preview_BibleWidget() {
    MaterialTheme {
        Surface {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(20.dp),
            ) {
                BibleWidget(
                    reference =
                        BibleReference(
                            versionId = 111,
                            bookUSFM = "2CO",
                            chapter = 1,
                            verseStart = 3,
                            verseEnd = 5,
                        ),
                    version = BibleVersion.preview,
                    fontSize = 18.sp,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                )
            }
        }
    }
}
