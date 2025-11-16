
![Platform Kotlin SDK](./assets/github-kotlin-sdk-banner.png)

![Platform](https://img.shields.io/badge/Platform-Android-green)
[![License](https://img.shields.io/badge/license-Apache-blue.svg)](LICENSE)

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
  - [Displaying Verse of the Day](#displaying-verse-of-the-day)
- [Sample App](#sample-app)
- [For Different Use Cases](#-for-different-use-cases)
- [Contributing](#contributing-starting-early-2026)
- [Documentation](#documentation)
- [Support](#support)
- [License](#license)

    

## Features
- 📖 **Scripture Display** - Easy-to-use Jetpack Compose components for displaying Bible verses, chapters, and passages with `BibleText`
- 🔐 **User Authentication** - Seamless "Log In with YouVersion" integration using `LoginWithYouVersionButton`
- 🌅 **Verse of the Day** - Built-in `VerseOfTheDay` component and API access to VOTD data
- 🚀 **Modern Kotlin** - Built with coroutines, Jetpack Compose, and Material Theming
- 💾 **Smart Caching** - Automatic local caching for improved performance

## Requirements

- Android 5.0+
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
youVersionPlatform = "0.5.0"

[libraries]
youversion-platform-core = { module = "com.youversion.platform:platform-sdk-core", version.ref = "youVersionPlatform" }
youversion-platform-ui = { module = "com.youversion.platform:platform-sdk-ui", version.ref = "youVersionPlatform" }
youversion-platform-reader = { module = "com.youversion.platform:platform-sdk-reader", version.ref = "youVersionPlatform" }
```

```kotlin
// app/build.gradle.kts
implementation(libs.youversion.platform.core)
implementation(libs.youversion.platform.ui)
implementation(libs.youversion.platform.reader)
```

### Without Version Catalog

```kotlin
val youVersionPlatform = "0.5.0"
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
        reference = BibleReference(versionId = 111, bookUSFM = "JHN", chapter = 3, verse = 16)
    )
}
```

Display a verse range:
```kotlin
@Composable
fun Demo() {
    BibleText(
        reference = BibleReference(versionId = 111, bookUSFM = "JHN", chapter = 3, verseStart = 16, verseEnd = 20)
    )
}
```

Or display a full chapter:
```kotlin
@Composable
fun Demo() {
    BibleText(
        reference = BibleReference(versionId = 111, bookUSFM = "JHN", chapter = 3)
    )
}
```

> **Note**: For longer passages, wrap `BibleText` in a `verticalScroll`. The SDK automatically fetches Scripture from YouVersion servers and maintains a local cache for improved performance.

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

## Sample App

Explore the [examples directory](./examples) for a complete sample app demonstrating:
- Scripture display with various reference types
- User authentication flows
- VOTD integration
- Best practices for token storage

To run the sample app:
1. Open the root `platform-sdk-kotlin` directory in Android Studio
2. Select the `examples.sample-android` module
3. Build and run on emulator or device

## 🎯 For Different Use Cases

### 📱 Kotlin SDK

Building an Android application? This Kotlin SDK provides native Jetpack Compose components including `BibleText`, `VerseOfTheDay`, and `LoginWithYouVersionButton` using modern language features.

### 🔧 API Integration

Need direct access to YouVersion Platform APIs? See [our comprehensive API documentation](https://developers.youversion.com/overview) for advanced integration patterns and REST endpoints.

### 🤖 LLM Integration

Building AI applications with Bible content? Access YouVersion's LLM-optimized endpoints and structured data designed for language models. See [our LLM documentation](https://developers.youversion.com/for-llms) for details.

## Contributing (Starting Early 2026)

See [CONTRIBUTING.md](./CONTRIBUTING.md) for details on how to get started.

## Documentation

- [API Documentation](https://developers.youversion.com/overview) - Complete API reference
- [LLM Integration Guide](https://developers.youversion.com/for-llms) - AI/ML integration docs
- [Release Process](./RELEASING.md) - Contribution and release guidelines
- [Sample Code](./examples) - Working examples and best practices

## Support

- **Issues**: [GitHub Issues](https://github.com/youversion/platform-sdk-kotlin/issues)
- **Questions**: Open a [discussion](https://github.com/youversion/platform-sdk-kotlin/discussions)
- **Platform Support**: [YouVersion Platform](https://platform.youversion.com/)

## License

This SDK is licensed under the Apache License 2.0. See [LICENSE](./LICENSE) for details.

---

Made with ❤️ by [YouVersion](https://www.youversion.com)


