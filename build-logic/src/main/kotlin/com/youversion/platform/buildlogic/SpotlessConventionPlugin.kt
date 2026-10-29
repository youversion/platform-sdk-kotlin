package com.youversion.platform.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Formats Kotlin sources and build scripts with ktlint. Outside CI, compiling also runs `spotlessApply`.
 */
class SpotlessConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.diffplug.spotless")

            val ktlintVersion = libs.version("ktlint")
            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("src/**/*.kt")
                    targetExclude("build/**/*.kt")
                    ktlint(ktlintVersion)
                    suppressLintsFor {
                        shortCode = "standard:property-naming"
                    }
                }

                kotlinGradle {
                    target("*.kts")
                    ktlint(ktlintVersion)
                }
            }

            if (!providers.environmentVariable("CI").isPresent) {
                tasks.withType<KotlinCompile>().configureEach {
                    finalizedBy("spotlessApply")
                }
            }
        }
    }
}
