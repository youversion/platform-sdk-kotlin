import java.io.File

data class HardcodedStringViolation(
    val relativePath: String,
    val lineNumber: Int,
    val lineContent: String,
    val stringLiteral: String,
)

class HardcodedUiStringsChecker(
    private val projectRoot: File,
    private val moduleNames: List<String> = listOf("platform-ui", "platform-reader"),
) {
    private val stringLiteralPattern = Regex(""""(?:[^"\\]|\\.)*"""")

    private val textLiteralPattern = Regex("""Text\s*\(\s*"""")
    private val textPropertyPattern = Regex("""\btext\s*=\s*"""")
    private val basicTextLiteralPattern = Regex("""BasicText\s*\(\s*"""")
    private val titleLiteralPattern = Regex("""\btitle\s*=\s*"""")
    private val contentDescriptionLiteralPattern = Regex("""\bcontentDescription\s*=\s*"""")
    private val toastLiteralPattern = Regex("""Toast\.makeText\s*\([^,]+,\s*"""")
    private val enumEntryLiteralPattern = Regex("""^\s*\w+\s*\(\s*"""")

    private val excludedFileSuffixes =
        setOf(
            "ReaderFontSettings.kt",
            "BibleAppLogo.kt",
            "SignInWithYouVersionButton.kt",
        )

    fun violations(): List<HardcodedStringViolation> {
        val results = mutableListOf<HardcodedStringViolation>()

        moduleNames.forEach { moduleName ->
            val sourceRoot = projectRoot.resolve("$moduleName/src/main")
            if (!sourceRoot.isDirectory) {
                return@forEach
            }

            sourceRoot
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    results += scanFile(file)
                }
        }

        return results.sortedWith(compareBy({ it.relativePath }, { it.lineNumber }))
    }

    private fun scanFile(file: File): List<HardcodedStringViolation> {
        if (excludedFileSuffixes.any { file.name == it }) {
            return emptyList()
        }

        val relativePath = file.relativeTo(projectRoot).invariantSeparatorsPath
        val lines = file.readLines()
        val previewTracker = PreviewBlockTracker()
        val enumTracker = EnumClassTracker()

        return lines.mapIndexedNotNull { index, line ->
            val lineNumber = index + 1
            previewTracker.onLine(line)
            enumTracker.onLine(line)

            if (previewTracker.isInsidePreviewBlock) {
                return@mapIndexedNotNull null
            }

            if (isCommentLine(line)) {
                return@mapIndexedNotNull null
            }

            if (!isUiContext(line, enumTracker.isInsideEnumClass)) {
                return@mapIndexedNotNull null
            }

            if (isExcludedLine(line, relativePath)) {
                return@mapIndexedNotNull null
            }

            val literal = extractPrimaryUiStringLiteral(line) ?: return@mapIndexedNotNull null

            if (literal.isEmpty()) {
                return@mapIndexedNotNull null
            }

            HardcodedStringViolation(
                relativePath = relativePath,
                lineNumber = lineNumber,
                lineContent = line.trim(),
                stringLiteral = literal,
            )
        }
    }

    private fun isCommentLine(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("//") ||
            trimmed.startsWith("*") ||
            trimmed.startsWith("/**") ||
            trimmed.startsWith("*/")
    }

    private fun isUiContext(
        line: String,
        insideEnumClass: Boolean,
    ): Boolean {
        if (line.contains("stringResource") || line.contains("pluralStringResource")) {
            return false
        }

        if (textLiteralPattern.containsMatchIn(line) ||
            textPropertyPattern.containsMatchIn(line) ||
            basicTextLiteralPattern.containsMatchIn(line) ||
            titleLiteralPattern.containsMatchIn(line) ||
            contentDescriptionLiteralPattern.containsMatchIn(line) ||
            toastLiteralPattern.containsMatchIn(line)
        ) {
            return stringLiteralPattern.containsMatchIn(line)
        }

        if (insideEnumClass && enumEntryLiteralPattern.containsMatchIn(line)) {
            return stringLiteralPattern.containsMatchIn(line)
        }

        return false
    }

    private fun isExcludedLine(
        line: String,
        relativePath: String,
    ): Boolean {
        val trimmed = line.trim()

        if (trimmed.contains("testTag(")) {
            return true
        }

        if (trimmed.contains("Log.") || trimmed.contains("Logger.") || trimmed.contains("logger.")) {
            return true
        }

        if (trimmed.contains("println(") || trimmed.contains("assertionFailed(")) {
            return true
        }

        if (isRouteString(trimmed)) {
            return true
        }

        if (isStorageKeyLine(trimmed)) {
            return true
        }

        if (isMarkupOrConstantLine(trimmed)) {
            return true
        }

        if (
            relativePath.endsWith("BibleVersionsViewModel.kt") &&
            trimmed.contains("activeLanguageName") &&
            trimmed.contains("\"English\"")
        ) {
            return true
        }

        if (trimmed.contains("FontDefinition(")) {
            return true
        }

        return false
    }

    private fun isMarkupOrConstantLine(line: String): Boolean =
        line.contains(".contains(") ||
            line.contains(".startsWith(") ||
            line.contains(".split(") ||
            line.contains(".append(") ||
            line.contains("joinToString(") ||
            line.contains("AnnotatedString(") ||
            line.contains("copyText(") ||
            line.contains("onLanguageSearchQueryChange(") ||
            line.contains("onVersionSearchQueryChange(")

    private fun isRouteString(line: String): Boolean =
        line.contains("BibleReaderDestination(") ||
            Regex("""\broute\s*=\s*"""").containsMatchIn(line) ||
            Regex("""data object \w+ : \w+\(""").containsMatchIn(line)

    private fun isStorageKeyLine(line: String): Boolean =
        line.startsWith("private const val KEY_") ||
            line.startsWith("const val KEY_") ||
            line.contains("putString(") ||
            line.contains("getString(") ||
            line.contains("getStringOrNull(") ||
            line.contains("SharedPreferences") ||
            line.contains("preferencesKey")

    private fun extractPrimaryUiStringLiteral(line: String): String? {
        val match = stringLiteralPattern.find(line) ?: return null
        return match.value.removeSurrounding("\"")
    }
}

private class PreviewBlockTracker {
    private var pendingPreviewAnnotation = false
    private var insidePreviewFunction = false
    private var braceDepth = 0
    private var startedPreviewFunction = false

    val isInsidePreviewBlock: Boolean
        get() = insidePreviewFunction

    fun onLine(line: String) {
        val trimmed = line.trim()

        if (trimmed.startsWith("@Preview") || trimmed.startsWith("@CombinedPreview")) {
            pendingPreviewAnnotation = true
        }

        if (pendingPreviewAnnotation && trimmed.contains("fun ")) {
            insidePreviewFunction = true
            pendingPreviewAnnotation = false
            startedPreviewFunction = false
            braceDepth = 0
        }

        if (!insidePreviewFunction) {
            return
        }

        val openCount = BraceCounter.countOutsideStrings(line, '{')
        val closeCount = BraceCounter.countOutsideStrings(line, '}')
        braceDepth += openCount

        if (!startedPreviewFunction && openCount > 0) {
            startedPreviewFunction = true
        }

        braceDepth -= closeCount

        if (startedPreviewFunction && braceDepth <= 0 && closeCount > 0) {
            insidePreviewFunction = false
            startedPreviewFunction = false
            braceDepth = 0
        }
    }
}

private class EnumClassTracker {
    private var insideEnumClass = false
    private var braceDepth = 0
    private var startedEnumBody = false

    val isInsideEnumClass: Boolean
        get() = insideEnumClass

    fun onLine(line: String) {
        val trimmed = line.trim()

        if (trimmed.contains("enum class")) {
            insideEnumClass = true
            startedEnumBody = false
            braceDepth = 0
        }

        if (!insideEnumClass) {
            return
        }

        val openCount = BraceCounter.countOutsideStrings(line, '{')
        val closeCount = BraceCounter.countOutsideStrings(line, '}')

        if (!startedEnumBody && openCount > 0) {
            startedEnumBody = true
        }

        if (startedEnumBody) {
            braceDepth += openCount
            braceDepth -= closeCount

            if (braceDepth <= 0 && closeCount > 0) {
                insideEnumClass = false
                startedEnumBody = false
                braceDepth = 0
            }
        }
    }
}

private object BraceCounter {
    fun countOutsideStrings(
        line: String,
        target: Char,
    ): Int {
        var count = 0
        var inString = false
        var escaped = false

        for (character in line) {
            when {
                escaped -> escaped = false
                character == '\\' && inString -> escaped = true
                character == '"' -> inString = !inString
                !inString && character == target -> count++
            }
        }

        return count
    }
}
