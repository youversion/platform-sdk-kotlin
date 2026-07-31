
![Platform Kotlin SDK](./assets/github-kotlin-sdk-banner.png)

![Platform](https://img.shields.io/badge/Platform-Android-green)
[![License](https://img.shields.io/badge/license-Apache-blue.svg)](LICENSE)
[![Coverage](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/youversion/platform-sdk-kotlin/badges/coverage.json)](./RELEASING.md)

# YouVersion Platform SDK for Kotlin

A Kotlin SDK for integrating with the YouVersion Platform, enabling developers to display Scripture 
content and implement user authentication in any Android environment. Multiplatform support is 
currently not available.

## Table of Contents
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
  - [Which Modules Do I Need?](#which-modules-do-i-need)
  - [With Version Catalog](#with-version-catalog)
  - [Without Version Catalog](#without-version-catalog)
- [Getting Started](#getting-started)
- [Usage](#usage)
  - [Displaying Scripture](#displaying-scripture)
  - [Bible Reader](#bible-reader)
  - [Displaying Verse of the Day](#displaying-verse-of-the-day)
  - [Authentication](#authentication)
  - [Highlights](#highlights)
- [Sample App](#sample-app)
- [For Different Use Cases](#-for-different-use-cases)
- [Development Setup](#development-setup)
- [Contributing](#contributing-starting-early-2026)
- [Documentation](#documentation)
- [Support](#support)
- [License](#license)

    

## Features
- 📖 **Scripture Display** - Easy-to-use Jetpack Compose components for displaying Bible verses, chapters, and passages with `BibleText`
- 📕 **Bible Reader** - A complete Bible reading experience inside your app with `BibleReader`
- 🖍️ **Highlights** - The signed-in user's YouVersion highlights are rendered in `BibleText` and can be created, recolored, and removed from the reader
- 🔐 **User Authentication** - Seamless "Sign In with YouVersion" integration using `SignInWithYouVersionButton`, with a top-level toggle to disable all sign-in UI
- 🌅 **Verse of the Day** - Built-in `VerseOfTheDay` component and API access to VOTD data
- 🚀 **Modern Kotlin** - Built with coroutines, Jetpack Compose, and Material Theming
- 💾 **Smart Caching** - Automatic local caching for improved performance

## Requirements

- Android 6.0+ (API 23)
- Android Studio Narwhal+
- Kotlin 2.2.0+
- A YouVersion Platform API key ([Register here](https://platform.youversion.com/))


## Installation

Be sure you have `mavenCentral()` in your `repositories` block.
```kotlin
// settings.gradle.kts
repositories {
    google()
    mavenCentral()
}
```

### Which Modules Do I Need?

The Platform SDK is broken into three main modules:
- `platform-core`: Provides the core functionality for accessing the YouVersion Platform API.
- `platform-ui`: Provides UI components for displaying Bible content.
- `platform-reader`: Provides a full Bible Reader experience.

---
**I want to only access the Bible API's and build my own integrations**

You will only need `platform-core`.

**I want to display Bible content or authenticate with YouVersion in my app but with my own styling**

You will need `platform-ui` and `platform-core`.

**I want a full, batteries included, drop-in Bible Reader experience**

You will need `platform-reader`, `platform-ui`, and `platform-core`.

---

Great! Now that you know which modules you need, you can proceed with installation.

### With Version Catalog

```toml
# gradle/libs.versions.toml
[versions]
youVersionPlatform = "1.8.0"

[libraries]
youversion-platform-core = { module = "com.youversion.platform:platform-core", version.ref = "youVersionPlatform" }
youversion-platform-ui = { module = "com.youversion.platform:platform-ui", version.ref = "youVersionPlatform" }
youversion-platform-reader = { module = "com.youversion.platform:platform-reader", version.ref = "youVersionPlatform" }
```

```kotlin
// app/build.gradle.kts
implementation(libs.youversion.platform.core)
implementation(libs.youversion.platform.ui)
implementation(libs.youversion.platform.reader)
```

### Without Version Catalog

```kotlin
val youVersionPlatform = "1.8.0"
implementation("com.youversion.platform:platform-core:$youVersionPlatform")
implementation("com.youversion.platform:platform-ui:$youVersionPlatform")
implementation("com.youversion.platform:platform-reader:$youVersionPlatform")
```

## Getting Started

1. **Get Your API Key**: Register your app with [YouVersion Platform](https://platform.youversion.com/) to acquire an app key
2. **Configure the SDK**: Add the following to your app's initialization:

```kotlin
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        YouVersionPlatformConfiguration.configure(
            context = this,
            appKey = TODO("YOUR_APP_KEY_HERE"),
        )
    }
}
```

## Usage

### Displaying Scripture

Display a single verse:
```kotlin
@Composable
fun Demo() {
    BibleText(
        reference = BibleReference(versionId = 3034, bookUSFM = "JHN", chapter = 3, verse = 16)
    )
}
```

Display a verse range:
```kotlin
@Composable
fun Demo() {
    BibleText(
        reference = BibleReference(versionId = 3034, bookUSFM = "JHN", chapter = 3, verseStart = 16, verseEnd = 20)
    )
}
```

Or display a full chapter:
```kotlin
@Composable
fun Demo() {
    BibleText(
        reference = BibleReference(versionId = 3034, bookUSFM = "JHN", chapter = 3)
    )
}
```

> **Note**: For longer passages, wrap `BibleText` in a `verticalScroll`. The SDK automatically fetches Scripture from YouVersion servers and maintains a local cache for improved performance.

When the user is signed in and has granted the `highlights` permission, `BibleText` also renders their YouVersion highlights behind the verse text. See [Highlights](#highlights).

### Bible Reader

Displays a full Bible reading experience, very similar to the YouVersion Bible app, ready to be added as a tab in your app.

```kotlin
@Composable
fun ReaderTab() {
    BibleReader(
        appName = "Your App Name",
        appSignInMessage = "Sign in to see your **YouVersion** highlights in **Your App Name**.",
    )
}
```

`appName` and `appSignInMessage` are shown in the sign-in prompt the reader presents to a signed-out user. `appSignInMessage` is your app's own reason for asking, and supports `**bold**` markdown.

To open to a specific passage:

```kotlin
BibleReader(
    appName = "Your App Name",
    appSignInMessage = "Sign in to see your **YouVersion** highlights in **Your App Name**.",
    bibleReference = BibleReference(versionId = 3034, bookUSFM = "PSA", chapter = 23),
)
```

To offer your own fonts in the reader's font settings sheet, or to render a bottom bar beneath the reader, pass `fontDefinitionProvider` and `bottomBar`.

#### Disabling Sign-In

By default, a signed-out user who taps a verse is prompted to sign in with YouVersion. To suppress all sign-in UI, including that prompt and the header menu's sign-in option, set `isSignInEnabled` to `false` during configuration:

```kotlin
YouVersionPlatformConfiguration.configure(
    context = this,
    appKey = "YOUR_APP_KEY_HERE",
    isSignInEnabled = false,
)
```

When sign-in is disabled, the reader hides the highlight colors from a signed-out user rather than offering a control that could never work. A user who is already signed in keeps their highlight colors, since they need nothing further.

#### Filtering Available Languages

By default, the version picker offers Bible versions in every available language. To restrict it to a specific set of languages, pass `permittedLanguageTags` during configuration. For example, to make only English versions available:

```kotlin
YouVersionPlatformConfiguration.configure(
    context = this,
    appKey = "YOUR_APP_KEY_HERE",
    permittedLanguageTags = setOf("en"),
)
```

Tags follow [BCP 47](https://www.rfc-editor.org/rfc/bcp/bcp47.txt) (e.g. `"en"` for English, `"es"` for Spanish). When the resulting list contains versions in only one language, the language button in the version picker is hidden automatically.

#### Filtering Available Versions

To restrict the version picker to a specific set of Bible versions, pass `permittedVersionIds` during configuration:

```kotlin
YouVersionPlatformConfiguration.configure(
    context = this,
    appKey = "YOUR_APP_KEY_HERE",
    permittedVersionIds = setOf(12, 111, 1588),
)
```

IDs are the YouVersion Bible version IDs (e.g. `111` for NIV, `1588` for AMP). Combines with `permittedLanguageTags` — a version must satisfy both filters to be shown.

### Displaying Verse of the Day

Use the built-in VOTD component:

```kotlin
@Composable
fun Demo() {
    CompactVerseOfTheDay()
    // Or
    VerseOfTheDay()
}
```

Or fetch VOTD data for custom UI:

```kotlin
suspend fun fetchVotd(): YouVersionVerseOfTheDay {
    val dayOfTheYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    return YouVersionApi.votd.verseOfTheDay(dayOfTheYear)
}
```

### Authentication

Integrating "Sign In with YouVersion" is straightforward. The SDK handles the entire authentication flow, including launching the sign-in screen, handling the redirect, and managing tokens.

#### 1. Configure the Manifest

To handle the redirect from the YouVersion authentication, you need to add an intent filter to your main activity in your `AndroidManifest.xml` file. The SDK will use this to receive the authentication result.

```xml
<!-- AndroidManifest.xml -->
<activity
    android:name=".MainActivity"
    android:exported="true">
    <!-- ... existing intent filters -->

    <!-- Handle OAuth callback -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />

        <data
            android:scheme="youversionauth"
            android:host="callback" />
    </intent-filter>
</activity>
```

#### 2. Update Your Main Activity

Your main activity must extend `SignInWithYouVersionActivity`. This allows the SDK to automatically handle the result from the sign-in process.

```kotlin
// MainActivity.kt
import com.youversion.platform.ui.signin.SignInWithYouVersionActivity

class MainActivity : SignInWithYouVersionActivity() {
    // ...
}
```

#### 3. Add the Sign-In Button to Your UI

Use the `SignInWithYouVersionButton` composable in your UI. You can use the `SignInViewModel` to check if the user is already signed in and conditionally display the button.

- `SignInWithYouVersionPermission.PROFILE`: To access the user's name and profile picture.
- `SignInWithYouVersionPermission.EMAIL`: To access the user's email address.
- `SignInWithYouVersionPermission.HIGHLIGHTS`: To read and write the user's Bible highlights. See [Highlights](#highlights).

```kotlin
// ProfileScreen.kt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.ui.signin.SignInViewModel
import com.youversion.platform.ui.views.SignInWithYouVersionButton

@Composable
fun ProfileScreen() {
    val signInViewModel = viewModel<SignInViewModel>()
    val state by signInViewModel.state.collectAsStateWithLifecycle()

    if (state.isSignedIn) {
        Column {
            Text("Welcome, ${state.userName ?: "User"}!")
            Text("Your email is ${state.userEmail ?: "not available"}.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { signInViewModel.onAction(SignInViewModel.Action.SignOut()) }) {
                Text("Sign Out")
            }
        }
    } else {
        SignInWithYouVersionButton(
            permissions = {
                setOf(
                    SignInWithYouVersionPermission.PROFILE,
                    SignInWithYouVersionPermission.EMAIL
                )
            }
        )
    }
}
```

The `SignInViewModel` automatically updates its state when authentication completes, and your UI will recompose to reflect the user's authentication status.

### Highlights

Highlights belong to the user's YouVersion account, so a highlight created in your app appears in the YouVersion Bible app and in any other app the user has granted access to.

`BibleReader` provides the full experience with no extra work: tapping a verse opens the verse action sheet with a color picker, choosing a color highlights the selected verses, and choosing the color a verse already has removes the highlight. On dark reader themes the colors are dimmed automatically so the verse text stays readable. `BibleText` renders the same highlights, so a custom reading UI built on `platform-ui` stays in sync with the reader.

#### Highlight Permissions

Reading and writing highlights requires the user to be signed in **and** to have granted `SignInWithYouVersionPermission.HIGHLIGHTS`. `BibleReader` asks for it at the moment it is needed, taking one of two routes:

- A **signed-out** user is offered sign-in, with the highlights permission included in the requested permissions. The grant rides along with the sign-in.
- A **signed-in** user who has not granted it yet is shown a confirmation dialog and then the YouVersion permission page. This is the data exchange flow, and it exists so the user does not have to sign in again just to grant one more permission.

Both routes return through the same `youversionauth://callback` deep link used by sign-in, so highlights only work end to end once your app has completed the [Authentication](#authentication) setup — the manifest intent filter *and* a main activity extending `SignInWithYouVersionActivity`. Without that setup the grant never reaches the SDK and highlights stay unavailable.

To check whether the permission has been granted:

```kotlin
val hasHighlightsPermission = YouVersionApi.hasPermission(SignInWithYouVersionPermission.HIGHLIGHTS)
```

#### Requesting the Permission Yourself

Apps that need to start the same permission flow themselves can do it from Compose with `rememberDataExchange`:

```kotlin
import com.youversion.platform.core.users.model.SignInWithYouVersionPermission
import com.youversion.platform.ui.dataexchange.rememberDataExchange

@Composable
fun AllowHighlightsButton() {
    val requestDataExchange = rememberDataExchange()
    val coroutineScope = rememberCoroutineScope()

    Button(
        onClick = {
            coroutineScope.launch {
                val result = requestDataExchange(setOf(SignInWithYouVersionPermission.HIGHLIGHTS))
                if (result?.grants(SignInWithYouVersionPermission.HIGHLIGHTS) == true) {
                    // The grant is already persisted; highlights load on the next read.
                }
            }
        }
    ) {
        Text("Allow highlights")
    }
}
```

Outside Compose, use `DataExchangeHandler(activityResultRegistry).requestDataExchange(...)` directly. Either way the granted permission is persisted for you before the call returns, so a later `YouVersionApi.hasPermission(...)` reflects it without any extra work.

> **Note**: Data exchange only works for a user who is **already signed in** — it mints its token from the existing access token. For a signed-out user nothing is presented at all (the result is `DataExchangeStatus.NotStarted`); request `SignInWithYouVersionPermission.HIGHLIGHTS` as part of sign-in instead. `rememberDataExchange` and `DataExchangeHandler` live in `platform-ui`.

#### Highlights API

Apps using only `platform-core` can read and write highlights directly through `YouVersionApi.highlights`. All four calls are suspend functions and require the signed-in user to have granted the highlights permission.

```kotlin
// Read a chapter's highlights
val highlights = YouVersionApi.highlights.highlights(versionId = 111, passageId = "JHN.3")

// Create, recolor, and remove a highlight on a single verse
YouVersionApi.highlights.createHighlight(versionId = 111, passageId = "JHN.3.16", color = "fffe00")
YouVersionApi.highlights.updateHighlight(versionId = 111, passageId = "JHN.3.16", color = "5dff79")
YouVersionApi.highlights.deleteHighlight(versionId = 111, passageId = "JHN.3.16")
```

Colors are hex strings without a leading `#`. The palette the reader offers is `fffe00` (yellow), `5dff79` (green), `00d6ff` (cyan), `ffc66f` (orange), and `ff95ef` (pink), matching the Swift SDK.

All four calls throw `YouVersionNetworkException` with reason `NOT_PERMITTED` when the user has not granted highlights access; that request will not succeed on retry. The read call also throws `MISSING_AUTHENTICATION` when the request was unauthenticated, which a sign-in or token refresh may resolve. The create, update, and delete calls report an unauthenticated request as a `false` return instead of throwing, so check their `Boolean` result too — `false` means the write did not happen.

## Sample App

Explore the [examples directory](./examples) for a complete sample app demonstrating:
- Scripture display with various reference types
- The full `BibleReader` experience, including highlights
- User authentication flows
- VOTD integration
- Best practices for token storage

To run the sample app:
1. Open the `platform-sdk-kotlin` directory in Android Studio
2. Wait for Gradle sync to complete (File → Sync Project with Gradle Files if needed)
3. Add your API key to `examples/sample-android/src/main/java/com/youversion/platform/MainApplication.kt`
4. Select `sample-android` from the run configuration dropdown
5. Create an emulator if needed (Tools → Device Manager → Create Device)
6. Click Run

## 🎯 For Different Use Cases

### 📱 Kotlin SDK

Building an Android application? This Kotlin SDK provides native Jetpack Compose components including `BibleText`, `BibleReader`, `VerseOfTheDay`, and `SignInWithYouVersionButton` using modern language features.

### 🔧 API Integration

Need direct access to YouVersion Platform APIs? See [our comprehensive API documentation](https://developers.youversion.com/overview) for advanced integration patterns and REST endpoints.

### 🤖 LLM Integration

Building AI applications with Bible content? Access YouVersion's LLM-optimized endpoints and structured data designed for language models. See [our LLM documentation](https://developers.youversion.com/for-llms) for details.

## Development Setup

After cloning the repo, install Node.js dependencies to enable git hooks (commit message linting):

```bash
npm install
```

This installs [husky](https://typicode.github.io/husky/) and [commitlint](https://commitlint.js.org/), which enforce [Conventional Commits](https://www.conventionalcommits.org/) on every commit. Without this step, commits with non-conforming messages will pass locally but fail in CI.

### Localization guardrails

User-facing strings in `platform-ui` and `platform-reader` must come from synced string resources (platform-localization), not hardcoded Kotlin literals.

```bash
# Fail on hardcoded UI strings in platform-ui and platform-reader
./gradlew verifyNoHardcodedUiStrings

# Root check also runs the guardrail task
./gradlew check
```

Greptile PR rules live in `.greptile/`. See [docs/localization-guardrails.md](./docs/localization-guardrails.md) for the full policy.

## Contributing (Starting Early 2026)

See [CONTRIBUTING.md](./CONTRIBUTING.md) for details on how to get started.

## Documentation

- [API Documentation](https://developers.youversion.com/overview) - Complete API reference
- [LLM Integration Guide](https://developers.youversion.com/for-llms) - AI/ML integration docs
- [Release Process](./RELEASING.md) - Contribution and release guidelines
- [Release Runbook](./docs/RELEASE-RUNBOOK.md) - Recovery procedures when a release fails partway
- [Sample Code](./examples) - Working examples and best practices

## Support

- **Issues**: [GitHub Issues](https://github.com/youversion/platform-sdk-kotlin/issues)
- **Questions**: Open a [discussion](https://github.com/youversion/platform-sdk-kotlin/discussions)
- **Platform Support**: [YouVersion Platform](https://platform.youversion.com/)

## License

This SDK is licensed under the Apache License 2.0. See [LICENSE](./LICENSE) for details.

---

Made with ❤️ by [YouVersion](https://www.youversion.com)


