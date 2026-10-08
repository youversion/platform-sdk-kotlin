# Contributing to YouVersion Platform Kotlin SDK

First, thank you for your interest in contributing to the YouVersion Platform Kotlin SDK! YouVersion
strives to make the Bible accessible to all people and that includes allowing you, a passionate developer,
to include the Bible in your own app!

For more information, please visit [the developer documentation](https://developers.youversion.com/overview).


## Quick Start (Run Sample App)

1. **Clone** the repository:
   ```bash
   git clone https://github.com/youversion/platform-sdk-kotlin.git
   ```

2. **Get an API key** from [platform.youversion.com](https://platform.youversion.com/)

3. **Add your API key** to `examples/sample-android/src/main/java/com/youversion/platform/MainApplication.kt`:
   ```kotlin
   appKey = "YOUR_API_KEY_HERE"
   ```

4. **Open in Android Studio**:
   - File → Open → select the `platform-sdk-kotlin` folder
   - File → Sync Project with Gradle Files (wait for sync to complete)

5. **Set up an emulator** (if needed):
   - Tools → Device Manager → Create Device
   - Select a phone (e.g., Pixel 8) and a system image (API 34+)

6. **Run the sample app**:
   - Select `sample-android` from the run configuration dropdown
   - Click Run


## How to Contribute

There are many ways you can help!
1. Open an issue to report a bug or suggest a feature.
2. Submit a pull request with a fix or new feature.
3. Update the documentation to improve the developer experience.


## Development Guidelines

All of our work is done inside of Android Studio. Please make sure you're using the [latest version](https://developer.android.com/studio).

### Building the Project

After cloning the repository to your machine, open it in Android Studio and sync the project. Once
complete, verify that the project builds.

```bash
# Tests and lint for every module. Coverage reports are under Writing Tests.
./gradlew build
```

Point Gradle at your Android SDK with a `sdk.dir` entry in `local.properties` at the project root (the file is gitignored), or set the `ANDROID_HOME` environment variable.

### Gradle and module dependencies

Inter-module dependencies use Gradle's type-safe project accessors (enabled in `settings.gradle.kts` via `TYPESAFE_PROJECT_ACCESSORS`). Shared library and plugin versions live in the version catalog at `gradle/libs.versions.toml`.

### Gradle commands

Command-line tasks complement the IDE workflow above. Public API checks and Spotless are documented in later sections. Use `testDebugUnitTest` when filtering with `--tests`.

```bash
# Clean build
./gradlew clean build

# Build a specific module
./gradlew :platform-core:build
./gradlew :platform-ui:build
./gradlew :platform-reader:build

# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :platform-core:test
./gradlew :platform-core:testDebugUnitTest

# Run a single test class or method
./gradlew :platform-core:testDebugUnitTest --tests "com.youversion.platform.core.bibles.api.BiblesApiVersionsTests"
./gradlew :platform-core:testDebugUnitTest --tests "com.youversion.platform.core.bibles.api.BiblesApiVersionsTests.test versions success returns decoded version metadata"

# Sample app on a connected emulator or device
./gradlew :examples:sample-android:installDebug
./gradlew :examples:sample-android:installRelease
./gradlew :examples:sample-android:uninstallDebug
```

### Sample App

The library includes a sample app under `examples/sample-android` which is used to demo and test functionality in the
SDK. It requires a valid YouVersion API key to be added to `examples/sample-android/src/main/java/com/youversion/platform/MainApplication.kt`. 
If a key is not provided, the sample app will crash on launch.

#### Device builds on BrowserStack

When an approved collaborator on `platform-sdk-kotlin_automation` opens a PR
from a branch in this repository, **BrowserStack App Live PR Build** dispatches
the existing automation build for the PR's exact head commit. New commits do
not rebuild automatically. To upload the current PR head again, an approved
collaborator comments `/app-live <sha>` — naming the full 40-character sha of
the head commit being approved, and nothing else — on the open PR. On this
explicit rebuild path the
commenter authorizes that one same-repository revision; the PR author does not
also need access to the automation repository. The automation repository builds
the `sample-android` `.apk`, uploads it to BrowserStack App Live, and returns
the `bs://...` app id in the SDK workflow summary.
After a successful upload, `github-actions[bot]` creates or updates one PR
comment with the latest build details and the `/app-live` instruction.

Naming the commit is what binds the approval to a revision. Because a comment
event carries no head commit, the workflow has to read the head when it runs,
which is not when the comment was posted — so without the sha, a push landing
in between would inherit the approval and send an unreviewed revision into a
build that holds the automation repository's credentials. With the sha, a moved
head refuses the build and the workflow log names the sha to re-issue. Two
conveniences on top of that rule:

- A bare `/app-live` is accepted when the PR author is themselves an approved
  collaborator on `platform-sdk-kotlin_automation`, since winning that race
  would grant them nothing they cannot already do directly.
- Surrounding whitespace is ignored, and a comment that starts with
  `/app-live` but is not one of these two forms is refused with a notice in the
  workflow log rather than silently ignored.

The sha has to be all 40 characters. An abbreviation could only be compared as
a prefix, and a 7-character prefix is 28 bits — grinding a second commit that
shares it is ordinary vanity-hash work, and the author can pre-compute it
against their own commit's prefix before the approval is even posted. So
abbreviations are refused rather than resolved.

Builds are numbered per PR: the key is the branch's ticket key plus the PR
number, incrementing for each upload, for example `kotlin-YPE-3011-pr9-1`
and `kotlin-YPE-3011-pr9-2`. A sanitized 10-character branch label plus the
PR number is used when the branch has no ticket key, for example
`feature/rework-reader` becomes `kotlin-rework-rea-pr9-1`. The exact source
SHA is recorded separately in the workflow summary.

This initial bridge produces an App Live build only. It does not run the Hinqa
corpus or upload to App Automate.

### Project Structure

The project is structured into several modules:

- **platform-core**
   - Core SDK logic which contains the API clients, configuration, caching, and data models.
- **platform-ui**
   - UI components library (Jetpack Compose) which provide the building blocks for rendering Bible content.
- **platform-reader**
   - High-level reader functionality which uses `platform-core` and `platform-ui` to provide a complete `BibleReader` experience.
- **platform-bom**
   - Bill of materials that keeps every published module on the same version.
- **examples/sample-android**
   - Sample Android app which demos using all of the components together.

### Branches

Maintainers working in this repository: every change — including documentation, tooling, and small fixes — belongs on a branch named after its Jira ticket. Merge into `main` only through a pull request; do not push directly to `main`.

**Branch naming:** `<JIRA-TICKET>-<kebab-description>`

- Examples: `YPE-1187-implement-bible-highlights-repository`, `BA-1204-plans-update`, `BA-5678-bibles-cache-cleanup`
- Put the ticket prefix first. Do not use an initials prefix or a `feature/` prefix.
- If there is no ticket yet, create one in Jira before you start work.

**Standard workflow:**

1. Create a branch from `main`.
2. Make your changes on that branch.
3. Open a pull request into `main`. The PR title should match the first line of the commit message.

**Feature branches** (work that spans multiple sub-tickets, or risky changes that need isolation before merging to `main`):

1. Create an epic branch from `main`: `<EPIC-TICKET>-<kebab-description>` (for example, `YPE-1900-offline-search`).
2. Create task branches from the epic branch: `<TASK-TICKET>-<kebab-description>`.
3. Open pull requests from task branches into the epic branch.
4. When the epic is complete, open one pull request from the epic branch into `main`.

**Bringing `main` into an epic or task branch:**

- Merge `main` into the epic branch first.
- Then merge the updated epic branch into the task branch.
- Do not merge `main` directly into a task branch.

### Pull Requests

Contributions are made using GitHub [pull requests](https://help.github.com/en/articles/about-pull-requests):

1. Fork the platform-sdk-kotlin repository and work on your fork.
2. [Create](https://github.com/youversion/platform-sdk-kotlin/compare) a new PR with a request to merge to `main`
3. Ensure that the description is clear and refers to an existing issue/bug if applicable
4. When contributing a new feature, provide motivation and use-cases describing why
   the feature provides value to the SDK.
5. If the contribution requires updates to documentation (be it updating existing contents or creating new one), please include the changes in your PR.
6. Make sure any code contributed is covered by tests, no existing tests are broken and code is formatted.
7. All PRs are checked for failing tests and code formatting.
8. When you open a PR, assign yourself if you can.

### Writing Tests
Tests should be in the same module and package as the code they are testing. Follow existing patterns
that are used in the project when it comes to naming tests and writing assertions. All tests can be run
and coverage reported with:

```bash
./gradlew koverHtmlReport
./gradlew koverXmlReport
./gradlew koverVerify
```

`koverHtmlReport` is the local HTML review. `koverXmlReport` and `koverVerify` are the CI-style report and the per-module threshold check. Reports are under `build/reports/kover/`.

### Public API

The published modules (`platform-core`, `platform-ui`, `platform-reader`) have their public API recorded in
`<module>/api/<module>.api`. Because every test here is recompiled against the current code, a signature change
is invisible to the test suite — but it breaks apps already built against a released version. CI fails any PR
whose code no longer matches its dump.

```bash
./gradlew apiCheck
```

If your change is meant to alter the public API, re-record the dump and commit the updated `.api` files with it:

```bash
./gradlew apiDump
```

Read the diff `apiCheck` prints before regenerating. A removal or signature change you did not intend is a break
for existing consumers, and `apiDump` would only paper over it.

Not every dump change is a consumer-facing one. Adding or removing a `@Composable` lambda, including a
`@Preview`, moves a generated `ComposableSingletons$...getLambda$...` entry; that is compiler bookkeeping, so
run `apiDump` and carry on. Anything marked `@PlatformInternalApi` never reaches the dump at all.

### Commit messages
* Commit messages should be written in English
* They should follow the [Conventional Commits](https://www.conventionalcommits.org/) specification

### Code Formatting

This project uses [Spotless](https://github.com/diffplug/spotless) with [ktlint](https://github.com/pinterest/ktlint) to enforce consistent code formatting across the codebase. As you contribute
to the project, please make sure your code is formatted correctly before submitting a pull request. Your PR will not be reviewed 
unless all checks pass.

#### Running the Formatter

To check if your code is formatted correctly:
```bash
./gradlew spotlessCheck
```

To automatically format your code:
```bash
./gradlew spotlessApply
```

The `spotlessApply` action will automatically run locally during compilation.

#### IDE Integration

For the best development experience, we recommend installing the [KtLint plugin](https://plugins.jetbrains.com/plugin/15057-ktlint) for IntelliJ IDEA or Android Studio. This plugin will:
- Highlight formatting issues in real-time as you write code
- Provide quick fixes for common formatting problems
- Integrate seamlessly with your IDE's code formatting settings

**Installation:**
1. Open IntelliJ IDEA / Android Studio
2. Go to Settings → Plugins
3. Search for "ktlint"
4. Install the official KtLint plugin
5. Restart your IDE

With the plugin installed, you can format files using the standard IDE format shortcut (⌘+⌥+L on Mac, Ctrl+Alt+L on Windows/Linux), and it will automatically apply ktlint rules.


