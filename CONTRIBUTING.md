# Contributing to YouVersion Platform Kotlin SDK

First, thank you for your interest in contributing to the YouVersion Platform Kotlin SDK! YouVersion
strives to make the Bible accessible to all people and that includes allow you, a passionate developer,
to include the Bible in your own app!

For more information, please visit [the developer documentation](https://developers.youversion.com/overview).


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
./gradlew build
```

You can also launch the sample app under `examples/sample-android`.

### Project Structure

The project is structured into several modules:

- **yvp-core**
   - Core SDK logic which contains the API clients, configuration, caching, and data models.
- **yvp-ui**
   - UI components library (Jetpack Compose) which provide the building blocks for rendering Bible content.
- **yvp-reader**
   - High-level reader functionality which uses `yvp-core` and `yvp-ui` to provide a complete Bible reading experience.
- **examples/sample-android**
   - Sample Android app which demos using the all of the components together.

### Pull Requests

Contributions are made using GitHub [pull requests](https://help.github.com/en/articles/about-pull-requests):

1. Fork the platform-sdk-kotlin repository and work on your fork.
2. [Create](https://github.com/youversion/platform-sdk-kotlin/compare) a new PR with a request to merge to `main`
3. Ensure that the description is clear and refers to an existing issue/bug if applicable
4. When contributing a new feature, provide motivation and use-cases describing why
   the feature provides value to the SDK.
5. If the contribution requires updates to documentation (be it updating existing contents or creating new one), please include the changes in your PR.
6. Make sure any code contributed is covered by tests, no existing tests are broken and code is formatted.

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

The `spotlessApply` action will run automatically during compilation.

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


