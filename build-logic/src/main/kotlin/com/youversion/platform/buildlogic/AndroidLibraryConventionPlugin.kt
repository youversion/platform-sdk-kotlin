package com.youversion.platform.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/**
 * Applies the Android library and Kotlin plugins with the SDK's shared SDK levels and JVM 11 toolchain.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")

            extensions.configure<LibraryExtension> {
                compileSdk = libs.version("compileSdk").toInt()
                defaultConfig {
                    minSdk = libs.version("minSdk").toInt()
                    consumerProguardFiles("consumer-rules.pro")
                }
                buildTypes.named("release").configure {
                    isMinifyEnabled = false
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }
                testOptions {
                    unitTests.isIncludeAndroidResources = true
                    // AGP 9 defaults this to compileSdk, and Robolectric's SDK 36 sandbox needs Java 21.
                    targetSdk = libs.version("minSdk").toInt()
                }
            }

            extensions.configure<KotlinAndroidProjectExtension> {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                    // Consumers may build with a Kotlin 2.2 compiler, which cannot read metadata or
                    // stdlib APIs from a newer language version.
                    languageVersion.set(KotlinVersion.KOTLIN_2_2)
                    apiVersion.set(KotlinVersion.KOTLIN_2_2)
                }
                // Keeps the published POMs from requiring a kotlin-stdlib newer than consumers' compilers.
                coreLibrariesVersion = "2.2.0"
                // Kotlin 2.4 prefixes the module name with the project group, which renames every
                // mangled internal symbol recorded in the API dumps.
                this.target.compilations.configureEach {
                    val moduleName = "${project.name}_$compilationName"
                    compileTaskProvider.configure { compilerOptions.moduleName.set(moduleName) }
                }
            }
        }
    }
}
