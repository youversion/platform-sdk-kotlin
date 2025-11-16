
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import com.vanniktech.maven.publish.MavenPublishBaseExtension
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
    // Provides running test coverage using `./gradlew kover[Html|Xml]Report`.
    apply<KoverGradlePlugin>()

    // Provides code formatting of kotlin code using `./gradlew spotless[Check|Apply]`
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

    // Automatically formats code during local compilation.
    afterEvaluate {
        if (System.getenv("CI") == null) {
            tasks.withType<KotlinCompile> {
                finalizedBy("spotlessApply")
            }
        }
    }

    // Configure common Maven publishing settings for all modules. Each module will declare it's own
    // publishing block in order to provide detailed information about the module, such as it's coordinates.
    pluginManager.withPlugin("com.vanniktech.maven.publish") {
        configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
            signAllPublications()

            pom {
                url = "https://platform.youversion.com/"
                inceptionYear = "2025"

                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://opensource.org/licenses/Apache-2.0"
                        distribution = "https://opensource.org/licenses/Apache-2.0"
                    }
                }

                scm {
                    connection = "scm:git:https://github.com/youversion/platform-sdk-kotlin.git"
                    developerConnection = "scm:git:ssh://git@github.com/youversion/platform-sdk-kotlin.git"
                    url = "https://github.com/youversion/platform-sdk-kotlin"
                }

                developers {
                    developer {
                        name = "YouVersion"
                        email = "engineering@youversion.com"
                    }
                }

                organization {
                    name = "YouVersion"
                    url = "https://youversion.com/"
                }
            }
        }
    }
}
