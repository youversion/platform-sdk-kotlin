import java.util.Properties

plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotless)
}

// The plugins below are applied by the convention plugins, so build-logic compiles against them.
// compileOnly keeps their versions owned by the root build's `plugins {}` block.
private fun Provider<PluginDependency>.marker() =
    get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}" }

// TestKit only injects build-logic's runtime classpath, so the plugins it applies are added for the tests.
val testPluginClasspath: Configuration by configurations.creating

dependencies {
    listOf(
        libs.plugins.android.library,
        libs.plugins.kotlin.android,
        libs.plugins.kover,
        libs.plugins.maven.publish,
        libs.plugins.spotless,
    ).forEach {
        compileOnly(it.marker())
        testPluginClasspath(it.marker())
    }

    testImplementation(libs.junit)
    testImplementation(gradleTestKit())
}

tasks.pluginUnderTestMetadata {
    pluginClasspath.from(testPluginClasspath)
}

tasks.test {
    useJUnit()
    systemProperty(
        "versionCatalog",
        layout.projectDirectory
            .file("../gradle/libs.versions.toml")
            .asFile.absolutePath,
    )
    providers
        .fileContents(layout.projectDirectory.file("../local.properties"))
        .asText
        .map { Properties().apply { load(it.reader()) }.getProperty("sdk.dir").orEmpty() }
        .orNull
        ?.takeIf { it.isNotEmpty() }
        ?.let { systemProperty("androidSdkDir", it) }
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
    }

    kotlinGradle {
        target("*.kts")
        ktlint(libs.versions.ktlint.get())
    }
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "youversion.android.library"
            implementationClass = "com.youversion.platform.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("sdkLibrary") {
            id = "youversion.sdk.library"
            implementationClass = "com.youversion.platform.buildlogic.SdkLibraryConventionPlugin"
        }
        register("publishing") {
            id = "youversion.publishing"
            implementationClass = "com.youversion.platform.buildlogic.PublishingConventionPlugin"
        }
        register("hardcodedUiStrings") {
            id = "youversion.hardcoded-ui-strings"
            implementationClass = "com.youversion.platform.buildlogic.HardcodedUiStringsConventionPlugin"
        }
        register("spotless") {
            id = "youversion.spotless"
            implementationClass = "com.youversion.platform.buildlogic.SpotlessConventionPlugin"
        }
    }
}
