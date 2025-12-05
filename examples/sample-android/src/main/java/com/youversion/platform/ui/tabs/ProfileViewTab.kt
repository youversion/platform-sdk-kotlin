package com.youversion.platform.ui.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.signin.SignInViewModel
import com.youversion.platform.ui.views.SignInWithYouVersionButton

@Composable
fun ProfileViewTab(onDestinationClick: (SampleDestination) -> Unit) {
    val signInViewModel = viewModel<SignInViewModel>()
    val state by signInViewModel.state.collectAsStateWithLifecycle()

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
            if (state.isSignedIn) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("You are signed in as:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(state.userName ?: "user name")
                    Text(state.userEmail ?: "user email")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { signInViewModel.onAction(SignInViewModel.Action.SignOut) }) {
                        Text("Sign Out")
                    }
                }
            } else {
                SignInWithYouVersionButton(
                    permissions = {
                        setOf(
                            SignInWithYouVersionPermission.PROFILE,
                            SignInWithYouVersionPermission.EMAIL,
                        )
                    },
                    stroked = true,
                )
            }
        }
    }
}
