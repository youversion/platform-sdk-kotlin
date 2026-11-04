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
 * Applies the Android library and Kotlin plugins with the SDK's shared SDK levels, compiling Java and
 * Kotlin to JVM 11 bytecode on whatever JDK runs the build.
 *
 * - Kotlin language/API version follows `kotlinStdlibFloor`, so consumers on that Kotlin compiler can
 *   read the SDK's metadata.
 * - kotlin-stdlib at `kotlinStdlibFloor` as the stdlib dependency published in the POMs.
 * - The Kotlin module name keeps its pre-2.4 form; Kotlin 2.4 prefixes the project group, which renames
 *   every mangled internal symbol in the API dumps.
 * - The AAR metadata's `minCompileSdk` is the SDK's compileSdk, so a consumer below it fails with a clear
 *   error.
 * - Unit tests run against compileSdk, so Robolectric's SDK 36 sandbox needs JDK 21. The sandbox reads
 *   FileDescriptor internals, so unit tests export `java.base/jdk.internal.access`.
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
                    aarMetadata {
                        minCompileSdk = compileSdk
                    }
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
                    unitTests.all { it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED") }
                }
            }

            extensions.configure<KotlinAndroidProjectExtension> {
                val stdlibFloor = libs.version("kotlinStdlibFloor")
                val floorLanguageVersion = KotlinVersion.fromVersion(stdlibFloor.split('.').take(2).joinToString("."))
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                    languageVersion.set(floorLanguageVersion)
                    apiVersion.set(floorLanguageVersion)
                }
                coreLibrariesVersion = stdlibFloor
                this.target.compilations.configureEach {
                    val moduleName = "${project.name}_$compilationName"
                    compileTaskProvider.configure { compilerOptions.moduleName.set(moduleName) }
                }
            }
        }
    }
}
