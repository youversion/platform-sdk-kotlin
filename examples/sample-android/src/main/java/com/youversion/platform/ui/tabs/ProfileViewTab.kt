package com.youversion.platform.ui.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.youversion.platform.core.YouVersionPlatformConfiguration
import com.youversion.platform.ui.components.SampleBottomBar
import com.youversion.platform.ui.components.SampleDestination
import com.youversion.platform.ui.views.SignInWithYouVersionButton
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService

@Composable
fun ProfileViewTab(onDestinationClick: (SampleDestination) -> Unit) {
    val context = LocalContext.current

    val authService = AuthorizationService(context)
    val authLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.let { data ->
                val response = AuthorizationResponse.fromIntent(data)
                val exception = AuthorizationException.fromIntent(data)

                when {
                    response != null -> {
                        // Exchange authorization code for access token
                        authService.performTokenRequest(
                            response.createTokenExchangeRequest(),
                        ) { tokenResponse, tokenException ->
                            if (tokenResponse != null) {
                                val accessToken = tokenResponse.accessToken
                                // Save token
                                YouVersionPlatformConfiguration.setAccessToken(accessToken)

//                            val result = SignInResult(
//                                accessToken = accessToken,
//                                grantedPermissions = extractPermissions(tokenResponse)
//                            )
//                            pendingAuthContinuation?.resume(result)
                            } else {
//                            pendingAuthContinuation?.resumeWithException(
//                                tokenException ?: Exception("Token exchange failed")
//                            )
                            }
                        }
                    }
                    exception != null -> {
//                    pendingAuthContinuation?.resumeWithException(exception)
                    }
                }
            }
        }

//    private val authService = AuthorizationService(activity)
//
//      // Register activity result launcher
//      private val authLauncher = activity.registerForActivityResult(
//          ActivityResultContracts.StartActivityForResult()
//      ) { result ->
//          val data = result.data
//          if (data != null) {
//              handleAuthResponse(data)
//          }
//      }

//    val result = remember { mutableStateOf<Bitmap?>(null) }
//    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult) {
//        result.value = it
//    }
//
//    Button(onClick = { launcher.launch() }) {
//        Text(text = "Take a picture")
//    }
//
//    result.value?.let { image ->
//        Image(image.asImageBitmap(), null, modifier = Modifier.fillMaxWidth())
//    }

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
                    // TODO: YV Auth
//                    val uri =
//                        authUri(
//                            "clientId",
//                            setOf(SignInWithYouVersionPermission.BIBLES),
//                            setOf(SignInWithYouVersionPermission.HIGHLIGHTS),
//                        )
//
//                    // Create AppAuth configuration
//                    val serviceConfig =
//                        AuthorizationServiceConfiguration(
//                            uri,
//                            Uri.parse("https://lat-446696173378.us-central1.run.app/auth/yv-token"), // Token endpoint
//                        )
//
//                    // Build authorization request
//                    val authRequest =
//                        AuthorizationRequest
//                            .Builder(
//                                serviceConfig,
//                                "clientId", // client ID
//                                ResponseTypeValues.CODE,
//                                Uri.parse("youversionauth://callback"), // Redirect URI
//                            ).apply {
// //                        setScopes(buildScopeString(requiredPermissions, optionalPermissions))
//                            }.build()
//
//                    // Launch Chrome Custom Tab for authentication
//                    val authIntent = authService.getAuthorizationRequestIntent(authRequest)
//                    authLauncher.launch(authIntent)
                },
                stroked = true,
            )
        }
    }
}
