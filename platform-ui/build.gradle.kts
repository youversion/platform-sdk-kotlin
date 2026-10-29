plugins {
    id("youversion.sdk.library")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.youversion.platform.ui"
    defaultConfig {
        vectorDrawables {
            // evenOdd support on API < 24
            useSupportLibrary = true
        }
    }
}

dependencies {
    api(platform(projects.platformBom))

    // api(): these types appear in platform-ui's public signatures, so consumers need
    // them on their compile classpath. Everything else stays implementation.
    api(projects.platformCore) // BibleText(BibleReference), BibleCard(BibleVersion), rememberSignIn()
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui) // BibleTextOptions/BibleTextFonts (TextStyle, AnnotatedString, TextUnit)
    api(libs.androidx.compose.ui.graphics) // ReaderColorScheme(Color), SignInWithYouVersionButtonDefaults (Shape)
    api(libs.androidx.compose.material3) // MaterialTheme.readerColorScheme extension receiver
    api(libs.androidx.activity.compose) // SignInWithYouVersionActivity : ComponentActivity
    api(libs.androidx.lifecycle.viewmodel.compose) // SignInViewModel : AndroidViewModel
    api(libs.koin.core) // PlatformUIKoinModule : Module
    api(libs.kotlin.coroutines) // SignInViewModel.state, BibleVersionsViewModel.state : StateFlow

    implementation(libs.androidx.browser)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.icons.core)
    implementation(libs.androidx.compose.icons.extended)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.touchlab.kermit)

    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.koin.androidx.compose)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.koin.test)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testReleaseImplementation(libs.androidx.compose.ui.test.manifest)
}

mavenPublishing {
    coordinates(artifactId = "platform-ui")

    pom {
        name = "YouVersion Platform SDK - UI"
        description =
            """
            Provides reusable Jetpack Compose UI components for building Bible reading experiences with the YouVersion platform.
            """.trimIndent()
    }
}
