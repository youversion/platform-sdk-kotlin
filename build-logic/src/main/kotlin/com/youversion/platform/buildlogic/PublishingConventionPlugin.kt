package com.youversion.platform.buildlogic

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class PublishingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.vanniktech.maven.publish")

            // Common Maven publishing settings. Each module declares its own `mavenPublishing` block for
            // its artifactId and POM name/description.
            extensions.configure<MavenPublishBaseExtension> {
                coordinates(
                    groupId = "com.youversion.platform",
                    // The release workflow passes -PsdkVersion so the published coordinate and the
                    // version stamped into the release commit cannot drift. Local builds fall back to
                    // the catalog. See RELEASING.md.
                    version =
                        providers
                            .gradleProperty("sdkVersion")
                            .getOrElse(libs.version("youversionPlatform")),
                )

                publishToMavenCentral(
                    // After Central Portal validates the build, automatically release it to Maven Central
                    // This prevents the need to manually push the publish button in Central Portal.
                    automaticRelease = true,
                )
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
