package com.youversion.platform.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class HardcodedUiStringsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            tasks.register<VerifyNoHardcodedUiStringsTask>("verifyNoHardcodedUiStrings") {
                val modules = listOf("platform-ui", "platform-reader")
                moduleNames.set(modules)
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
