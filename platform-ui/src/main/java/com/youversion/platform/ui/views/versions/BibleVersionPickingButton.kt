package com.youversion.platform.ui.views.versions

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.di.PlatformKoinGraph
import com.youversion.platform.ui.di.PlatformUIKoinModule
import com.youversion.platform.ui.theme.BibleReaderMaterialTheme
import com.youversion.platform.ui.theme.ui.BibleReaderTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.module.rememberKoinModules
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, KoinExperimentalAPI::class)
@Composable
fun BibleVersionPickingButton(
    initialVersionId: Int,
    modifier: Modifier = Modifier,
    onVersionChange: ((BibleVersion) -> Unit)? = null,
) {
    KoinIsolatedContext(context = PlatformKoinGraph.koinApplication) {
        rememberKoinModules { listOf(PlatformUIKoinModule) }

        var isShowingSheet by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val latestOnVersionChange = rememberUpdatedState(onVersionChange)
        val viewModelKey = rememberSaveable { UUID.randomUUID().toString() }

        val viewModel: BibleVersionsViewModel =
            koinViewModel(key = viewModelKey) {
                parametersOf(initialVersionId)
            }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val currentVersion = state.currentVersion

        LaunchedEffect(currentVersion) {
            currentVersion?.let { latestOnVersionChange.value?.invoke(it) }
        }

        BibleReaderMaterialTheme {
            Button(
                onClick = { isShowingSheet = true },
                modifier = modifier,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = BibleReaderTheme.colorScheme.buttonSecondary,
                        contentColor = BibleReaderTheme.colorScheme.textPrimary,
                    ),
                contentPadding =
                    PaddingValues(
                        start = 12.dp,
                        top = 4.dp,
                        end = 12.dp,
                        bottom = 4.dp,
                    ),
            ) {
                Text(
                    text = currentVersion?.localizedAbbreviation ?: currentVersion?.abbreviation ?: " ",
                    style = BibleReaderTheme.typography.buttonLabelS,
                )
            }

            if (isShowingSheet) {
                ModalBottomSheet(onDismissRequest = { isShowingSheet = false }, sheetState = sheetState) {
                    BibleVersionsStack(
                        viewModel = viewModel,
                        onDismiss = { isShowingSheet = false },
                        onVersionSelect = { selectedVersion ->
                            viewModel.onAction(BibleVersionsViewModel.Action.VersionSelected(selectedVersion))
                            isShowingSheet = false
                        },
                    )
                }
            }
        }
    }
}
