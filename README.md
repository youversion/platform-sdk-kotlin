
![Platform Swift SDK](./assets/github-kotlin-sdk-banner.png)

## Code Formatting

This project uses [Spotless](https://github.com/diffplug/spotless) with [ktlint](https://github.com/pinterest/ktlint) to enforce consistent code formatting across the codebase.

### Running the Formatter

To check if your code is formatted correctly:
```bash
./gradlew spotlessCheck
```

To automatically format your code:
```bash
./gradlew spotlessApply
```

The `spotlessApply` action will run automatically during compilation.

### IDE Integration

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
