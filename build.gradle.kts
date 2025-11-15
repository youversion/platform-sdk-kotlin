
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import kotlinx.kover.gradle.plugin.KoverGradlePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.spotless) apply false
}

subprojects {
    apply<KoverGradlePlugin>()
    apply<SpotlessPlugin>()
    configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            targetExclude("build/**/*.kt")
            ktlint(libs.versions.ktlint.get())
            suppressLintsFor {
                shortCode = "standard:property-naming"
            }
        }

        kotlinGradle {
            target("*.kts")
            ktlint(libs.versions.ktlint.get())
        }
    }

    afterEvaluate {
        if (System.getenv("CI") == null) {
            tasks.withType<KotlinCompile> {
                finalizedBy("spotlessApply")
            }
        }
    }
}
