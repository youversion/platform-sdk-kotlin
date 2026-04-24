package com.youversion.platform.ui.views.versions

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youversion.platform.core.bibles.models.BibleVersion
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleVersionPickingButton(
    initialVersionId: Int,
    modifier: Modifier = Modifier,
    onVersionChange: ((BibleVersion) -> Unit)? = null,
) {
    val viewModel: BibleVersionsViewModel = koinViewModel { parametersOf(initialVersionId) }
    var version by remember { mutableStateOf<BibleVersion?>(null) }
    var isShowingSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(viewModel) {
        viewModel.onVersionChange = { newVersion ->
            version = newVersion
            onVersionChange?.invoke(newVersion)
        }
    }

    OutlinedButton(onClick = { isShowingSheet = true }, modifier = modifier.defaultMinSize(30.dp)) {
        Text(
            text = version?.localizedAbbreviation ?: version?.abbreviation ?: " ",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }

    if (isShowingSheet) {
        ModalBottomSheet(onDismissRequest = { isShowingSheet = false }, sheetState = sheetState) {
            VersionsScreen(
                viewModel = viewModel,
                onBackClick = { isShowingSheet = false },
                onLanguagesClick = {},
                onVersionSelect = { selectedVersion ->
                    viewModel.onVersionChange(selectedVersion)
                    isShowingSheet = false
                },
            )
        }
    }
}
