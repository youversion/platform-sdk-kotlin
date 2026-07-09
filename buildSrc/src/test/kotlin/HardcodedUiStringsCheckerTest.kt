import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HardcodedUiStringsCheckerTest {
    private val tempRoots = mutableListOf<File>()

    @After
    fun tearDown() {
        tempRoots.forEach { it.deleteRecursively() }
        tempRoots.clear()
    }

    @Test
    fun mixedTitleAndStringResource_flagsHardcodedTitle() {
        val violations =
            violationsFor(
                """SomeComposable(title = "Hardcoded", subtitle = stringResource(R.string.x))""",
            )

        assertEquals(1, violations.size)
        assertEquals("Hardcoded", violations.first().stringLiteral)
    }

    @Test
    fun textWithMigrationComment_flagsHardcodedLiteral() {
        val violations =
            violationsFor(
                """Text("Hello") // migrate to stringResource""",
            )

        assertEquals(1, violations.size)
        assertEquals("Hello", violations.first().stringLiteral)
    }

    @Test
    fun textWithConditionalStringResource_flagsHardcodedBranch() {
        val violations =
            violationsFor(
                """Text(if (condition) stringResource(R.string.foo) else "Hardcoded label")""",
            )

        assertEquals(1, violations.size)
        assertEquals("Hardcoded label", violations.first().stringLiteral)
    }

    @Test
    fun textWithOnlyStringResource_doesNotFlag() {
        val violations =
            violationsFor(
                """Text(stringResource(R.string.foo))""",
            )

        assertTrue(violations.isEmpty())
    }

    @Test
    fun textPropertyWithStringResource_doesNotFlag() {
        val violations =
            violationsFor(
                """text = stringResource(R.string.foo)""",
            )

        assertTrue(violations.isEmpty())
    }

    @Test
    fun tripleQuotedText_flagsViolation() {
        val violations =
            violationsFor(
                "Text(\"\"\"Hello World\"\"\")",
            )

        assertEquals(1, violations.size)
        assertEquals("Hello World", violations.first().stringLiteral)
    }

    @Test
    fun tripleQuotedWithEmbeddedQuotes_flagsViolation() {
        val violations =
            violationsFor(
                "Text(\"\"\"Say \"hi\"\"\"\")",
            )

        assertEquals(1, violations.size)
        assertEquals("Say \"hi\"", violations.first().stringLiteral)
    }

    @Test
    fun enumEntryWithHardcodedString_flagsViolation() {
        val violations =
            violationsForContent(
                """
                enum class Status {
                    SOME_ENTRY("Hardcoded"),
                }
                """.trimIndent(),
            )

        assertEquals(1, violations.size)
        assertEquals("Hardcoded", violations.first().stringLiteral)
    }

    @Test
    fun commentWithClosingBraceInsideEnum_stillFlagsEnumEntry() {
        val violations =
            violationsForContent(
                """
                enum class Status {
                    // fix } bug
                    SOME_ENTRY("Hardcoded"),
                }
                """.trimIndent(),
            )

        assertEquals(1, violations.size)
    }

    @Test
    fun camelCaseCallInsideEnumBody_doesNotFlag() {
        val violations =
            violationsForContent(
                """
                enum class Status {
                    SOME_ENTRY(stringResource(R.string.localized)),
                    ;

                    fun display(): String = someHelper("Hardcoded")
                }
                """.trimIndent(),
            )

        assertTrue(violations.isEmpty())
    }

    private fun violationsFor(line: String): List<HardcodedStringViolation> {
        val root =
            createTempProject(
                "platform-ui/src/main/java/com/example/TestScreen.kt" to line,
            )
        return HardcodedUiStringsChecker(root, listOf("platform-ui")).violations()
    }

    private fun violationsForContent(content: String): List<HardcodedStringViolation> {
        val root =
            createTempProject(
                "platform-ui/src/main/java/com/example/TestEnum.kt" to content,
            )
        return HardcodedUiStringsChecker(root, listOf("platform-ui")).violations()
    }

    private fun createTempProject(vararg files: Pair<String, String>): File {
        val root = createTempDirectory("hardcoded-ui-strings-checker").toFile()
        tempRoots += root

        files.forEach { (relativePath, content) ->
            val file = root.resolve(relativePath)
            checkNotNull(file.parentFile) { "Missing parent directory for $relativePath" }.mkdirs()
            file.writeText(content)
        }

        return root
    }
}
