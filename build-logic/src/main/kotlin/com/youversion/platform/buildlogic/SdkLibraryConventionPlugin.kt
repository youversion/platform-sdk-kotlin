package com.youversion.platform.buildlogic

import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/**
 * Configures a published SDK module: Android library, publishing, formatting, and an 85% line-coverage floor.
 *
 * SDK modules opt in to each other's `@PlatformInternalApi` declarations. Sample apps deliberately do not,
 * so that they compile against the same surface a consumer sees.
 */
class SdkLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("youversion.android.library")
            pluginManager.apply("youversion.publishing")
            pluginManager.apply("youversion.spotless")
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            extensions.configure<KotlinAndroidProjectExtension> {
                compilerOptions.optIn.add("com.youversion.platform.core.di.PlatformInternalApi")
            }

            extensions.configure<KoverProjectExtension> {
                reports.verify.rule { minBound(85, CoverageUnit.LINE) }
            }

            tasks.named("check") {
                dependsOn(":verifyNoHardcodedUiStrings")
            }
        }
    }
}
