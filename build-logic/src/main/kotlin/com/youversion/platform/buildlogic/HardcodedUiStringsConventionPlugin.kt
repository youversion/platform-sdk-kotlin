package com.youversion.platform.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * Registers `verifyNoHardcodedUiStrings` on the root project, scanning `platform-ui` and `platform-reader`.
 */
class HardcodedUiStringsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            tasks.register<VerifyNoHardcodedUiStringsTask>("verifyNoHardcodedUiStrings") {
                val modules = listOf("platform-ui", "platform-reader")
                moduleNames.set(modules)
                projectRoot.set(layout.projectDirectory)
                kotlinSources.setFrom(
                    modules.map { module ->
                        fileTree(layout.projectDirectory.dir("$module/src/main")) {
                            include("**/*.kt")
                        }
                    },
                )
                resultFile.set(layout.buildDirectory.file("verifyNoHardcodedUiStrings/result.txt"))
            }
        }
    }
}
