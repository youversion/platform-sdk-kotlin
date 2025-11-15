package com.youversion.platform.ui.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.signin.SignInViewModel
import com.youversion.platform.ui.views.SignInWithYouVersionButton

@Composable
fun ProfileViewTab(onDestinationClick: (SampleDestination) -> Unit) {
    val context = LocalContext.current
    val signInViewModel = viewModel<SignInViewModel>()

    Scaffold(
        bottomBar = {
            SampleBottomBar(
                currentDestination = SampleDestination.Profile,
                onDestinationClick = onDestinationClick,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            SignInWithYouVersionButton(
                onClick = {
                    signInViewModel.signIn(
                        context = context,
                        SignInWithYouVersionPermission.BIBLES,
                        SignInWithYouVersionPermission.HIGHLIGHTS,
                    )
                },
                stroked = true,
            )
        }
    }
}
