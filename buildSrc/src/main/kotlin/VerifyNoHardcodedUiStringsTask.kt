import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class VerifyNoHardcodedUiStringsTask : DefaultTask() {
    @get:Input
    abstract val moduleNames: ListProperty<String>

    init {
        group = "verification"
        description = "Fails when hardcoded user-facing strings are found in SDK UI modules."
    }

    @TaskAction
    fun verify() {
        val checker =
            HardcodedUiStringsChecker(
                projectRoot = project.rootDir,
                moduleNames = moduleNames.get(),
            )

        val violations = checker.violations()
        if (violations.isEmpty()) {
            logger.lifecycle(
                "No hardcoded UI strings found in ${moduleNames.get().joinToString(", ")}.",
            )
            return
        }

        val message =
            buildString {
                appendLine("Hardcoded user-facing strings detected:")
                violations.forEach { violation ->
                    appendLine("  ${violation.relativePath}:${violation.lineNumber} \"${violation.stringLiteral}\"")
                    appendLine("    ${violation.lineContent}")
                }
                appendLine()
                appendLine("Use stringResource() / pluralStringResource() with platform-localization keys.")
            }

        throw GradleException(message)
    }
}
