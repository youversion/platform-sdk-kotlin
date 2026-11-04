// Standalone build that compiles the SDK the way the oldest supported consumer would: Kotlin 2.2.0,
// AGP 8.x and compileSdk 36. It resolves the SDK from mavenLocal, so publish it first from the repo
// root with `./gradlew publishToMavenLocal -PsdkVersion=0.0.0-SNAPSHOT`.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal {
            content { includeGroup("com.youversion.platform") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "consumer-baseline"
include(":app")
