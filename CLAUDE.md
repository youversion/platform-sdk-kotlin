# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

YouVersion Platform SDK (platform-sdk-kotlin) is an Android SDK for third-party apps to integrate Bible text services from YouVersion. It's designed to be distributed as a library dependency.

## Module Architecture

The project uses a multi-module architecture with clear separation of concerns:

- **platform-core**: Core SDK logic containing:
  - API clients (Bible, VOTD, Highlights, Languages)
  - Configuration management (`YouVersionPlatformConfiguration`)
  - Koin-based dependency injection
  - Data models and utilities
  - No UI dependencies

- **platform-ui**: UI components library (Jetpack Compose):
  - Reusable Compose UI components
  - Depends on `platform-core`

- **platform-reader**: High-level reader functionality:
  - Combines `platform-core` + `platform-ui`
  - Exposes both modules via `api()` dependencies
  - Intended as the main entry point for consumer apps

- **examples/sample-android**: Sample Android app demonstrating SDK usage

**Dependency Flow**: `platform-core` ← `platform-ui` ← `platform-reader` ← `sample-android`

## SDK Initialization Pattern

The SDK uses a singleton configuration pattern with Koin DI:

1. Apps must call `YouVersionPlatformConfiguration.configure()` with an `appKey` in `Application.onCreate()`
2. This initializes Koin context (`startYouVersionPlatform()`)
3. Koin provides: `HttpClient` (Ktor), `Store` (SharedPreferences), `Logger`
4. Access SDK via `YouVersionApi` object (contains `bible`, `votd`, `highlights`, `language` APIs)
5. All API calls are suspend functions using Kotlin coroutines

**Key Files**:
- Configuration: `platform-core/src/main/java/com/youversion/platform/core/YouVersionPlatformConfiguration.kt`
- DI Setup: `platform-core/src/main/java/com/youversion/platform/core/utilities/koin/`
- API Entry: `platform-core/src/main/java/com/youversion/platform/core/api/YouVersionApi.kt`

## Build Commands

```bash
# Full build (all modules + tests + lint + coverage)
./gradlew build

# Clean build
./gradlew clean build

# Build specific module
./gradlew :platform-core:build
./gradlew :platform-ui:build
./gradlew :platform-reader:build

# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :platform-core:test
./gradlew :platform-core:testDebugUnitTest

# Run single test class
./gradlew :platform-core:test --tests "com.youversion.platform.core.api.bible.BibleVersionsApiTests"

# Run single test method
./gradlew :platform-core:test --tests "com.youversion.platform.core.api.bible.BibleVersionsApiTests.testVersionsAPI"

# Code coverage report
./gradlew koverXmlReport
# Reports in: build/reports/kover/
```

## Code Formatting

This project enforces formatting via Spotless + ktlint:

```bash
# Check formatting
./gradlew spotlessCheck

# Auto-format all code
./gradlew spotlessApply
```

**Note**: `spotlessApply` runs automatically during compilation.

## Sample App

```bash
# Install on connected emulator/device
./gradlew :examples:sample-android:installDebug

# Install release build
./gradlew :examples:sample-android:installRelease

# Uninstall
./gradlew :examples:sample-android:uninstallDebug
```

**Important**: The sample app requires a valid YouVersion API key in `examples/sample-android/src/main/java/com/youversion/platform/MainApplication.kt`. The placeholder uses `TODO()` to force replacement before running.

## Dependency Management

- Uses Gradle version catalogs: `gradle/libs.versions.toml`
- Type-safe project accessors enabled for inter-module dependencies

## Local Setup

Create `local.properties` in project root (gitignored):
```properties
sdk.dir=/path/to/your/Android/sdk
```

Or set `ANDROID_HOME` environment variable.

## Code style and conventions

- Use GitHub to create pull requests (PRs).
- PR titles should always be the same as the first line of the commit message.
- When creating PRs, try to use the git config user email as the assignee.
- Prefer idiomatic, industry standard Kotlin style. Follow https://developer.android.com/kotlin/style-guide.
- Don't make whitespace-only changes.
- Prefer suspend functions over callback-based APIs.
- Asynchronous functions with return values should have names that are noun phrases describing the return value rather than verb phrases and should never begin with "get", "load", or "request".
- Don't add inline comments inside functions, but don't delete existing inline comments.
- Add documentation comments to new, non-private functions.
- Make access controls on properties and functions as strict as they can be (private, internal, protected, etc).
- Prefer to make properties immutable (val over var).
- Avoid abbreviations; prefer clarity over brevity.
- For Booleans, ensure that they start with a helping verb like “is”, “has”, or “should”. “shows” and “showing” are also acceptable prefixes.
- Non-boolean entities should end with a word that indicates their data type (ex. “shadowColor” rather than “colorShadow” for a Color).
- Do not prepend “this.” when it is unnecessary.
- Properties should be listed before all functions.
- Classes should not be marked open unless they are intended to be subclassed.
- Prefer data class or value class over open classes when possible.
- Don’t leave unused code.
- Do not leave commented out code in place.
- Avoid abbreviations.
- Class, object, interface, and enum entity names should always be in PascalCase.
- Property and function names should always be in camelCase.