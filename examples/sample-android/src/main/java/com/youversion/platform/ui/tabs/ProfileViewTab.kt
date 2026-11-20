package com.youversion.platform.ui.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.dataexchange.DataExchangeStatus
import com.youversion.platform.ui.dataexchange.rememberDataExchange
import com.youversion.platform.ui.signin.SignInViewModel
import com.youversion.platform.ui.theme.LinkColor
import com.youversion.platform.ui.views.SignInWithYouVersionButton
import kotlinx.coroutines.launch

@Composable
fun ProfileViewTab(onDestinationClick: (SampleDestination) -> Unit) {
    val signInViewModel = viewModel<SignInViewModel>()
    val state by signInViewModel.state.collectAsStateWithLifecycle()
    val config by YouVersionPlatformConfiguration.configState.collectAsStateWithLifecycle()
    val hasHighlightsPermission =
        config?.grantedPermissions?.contains(SignInWithYouVersionPermission.HIGHLIGHTS) == true
    val scope = rememberCoroutineScope()
    var dataExchangeStatusText by remember { mutableStateOf<String?>(null) }
    val requestDataExchange = rememberDataExchange()
    val linkButtonColors = ButtonDefaults.textButtonColors(contentColor = LinkColor)

    LaunchedEffect(Unit) {
        signInViewModel.onAction(SignInViewModel.Action.UpdateSignInState)
    }

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
                    Text(state.userName ?: "(no name)")
                    Text(state.userEmail ?: "(no email)")
                    Spacer(modifier = Modifier.height(16.dp))
                    if (hasHighlightsPermission) {
                        Text("Highlights permission: granted")
                    } else {
                        TextButton(
                            colors = linkButtonColors,
                            onClick = {
                                dataExchangeStatusText = null
                                scope.launch {
                                    val result = requestDataExchange(setOf(SignInWithYouVersionPermission.HIGHLIGHTS))
                                    dataExchangeStatusText =
                                        when (val status = result?.status) {
                                            null, DataExchangeStatus.NotStarted -> {
                                                "Highlights permission failed: the request couldn't start."
                                            }

                                            DataExchangeStatus.Granted -> {
                                                if (result.grants(SignInWithYouVersionPermission.HIGHLIGHTS)) {
                                                    null
                                                } else {
                                                    "Highlights permission was not granted."
                                                }
                                            }

                                            DataExchangeStatus.Cancelled -> {
                                                null
                                            }

                                            DataExchangeStatus.Missing -> {
                                                "Highlights permission failed: no status was returned."
                                            }

                                            is DataExchangeStatus.Unknown -> {
                                                "Highlights permission failed: unknown status \"${status.rawValue}\"."
                                            }
                                        }
                                }
                            },
                        ) {
                            Text("Request highlights permission")
                        }
                    }
                    dataExchangeStatusText?.let { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        colors = linkButtonColors,
                        onClick = {
                            dataExchangeStatusText = null
                            signInViewModel.onAction(SignInViewModel.Action.SignOut())
                        },
                    ) {
                        Text("Sign out")
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
