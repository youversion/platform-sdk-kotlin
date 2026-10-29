package com.youversion.platform.buildlogic

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Applies the common Maven Central publishing settings. Each module declares its own `mavenPublishing`
 * block for its artifactId and POM name/description.
 *
 * The version comes from `-PsdkVersion`, which the release workflow passes so the published coordinate
 * and the version stamped into the release commit cannot drift. Local builds fall back to the catalog's
 * `youversionPlatform`. See RELEASING.md.
 *
 * Releases are published with `automaticRelease`, so a build that passes Central Portal validation is
 * released without pressing the publish button in Central Portal.
 */
class PublishingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.vanniktech.maven.publish")

            extensions.configure<MavenPublishBaseExtension> {
                coordinates(
                    groupId = "com.youversion.platform",
                    version =
                        providers
                            .gradleProperty("sdkVersion")
                            .getOrElse(libs.version("youversionPlatform")),
                )

                publishToMavenCentral(automaticRelease = true)
                signAllPublications()

                pom {
                    url.set("https://platform.youversion.com/")
                    inceptionYear.set("2025")

                    licenses {
                        license {
                            name.set("Apache License, Version 2.0")
                            url.set("https://opensource.org/licenses/Apache-2.0")
                            distribution.set("https://opensource.org/licenses/Apache-2.0")
                        }
                    }

                    scm {
                        connection.set("scm:git:https://github.com/youversion/platform-sdk-kotlin.git")
                        developerConnection.set("scm:git:ssh://git@github.com/youversion/platform-sdk-kotlin.git")
                        url.set("https://github.com/youversion/platform-sdk-kotlin")
                    }

                    developers {
                        developer {
                            name.set("YouVersion")
                            email.set("engineering@youversion.com")
                        }
                    }

                    organization {
                        name.set("YouVersion")
                        url.set("https://youversion.com/")
                    }
                }
            }
        }
    }
}
