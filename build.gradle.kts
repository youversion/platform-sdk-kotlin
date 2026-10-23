// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.binary.compatibility.validator)
    // Puts build-logic on the root build's classpath, for VerifyNoHardcodedUiStringsTask below.
    id("youversion.android.library") apply false
}

// Records the public API of the published modules in `<module>/api/<module>.api`, so that a change
// to a signature a released consumer links against shows up as a diff. Run `./gradlew apiDump`
// to re-record after an intentional API change; `./gradlew apiCheck` verifies the dump is current.
apiValidation {
    // The sample app is not published, so it has no API to protect.
    ignoredProjects += listOf("examples", "sample-android")
    // @PlatformInternalApi is an ERROR-level opt-in: consumers are not allowed to link against
    // what it marks, so those declarations are left out of the dump and stay free to change.
    nonPublicMarkers += "com.youversion.platform.core.di.PlatformInternalApi"
}

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

// Merges the SDK modules' coverage into one report at `build/reports/kover/report.xml`. The sample
// app has no tests, so it is left out.
dependencies {
    kover(projects.platformCore)
    kover(projects.platformUi)
    kover(projects.platformReader)
}
