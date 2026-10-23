plugins {
    `kotlin-dsl`
}

// The plugins below are applied by the convention plugins, so build-logic compiles against them.
// compileOnly keeps their versions owned by the root build's `plugins {}` block.
fun Provider<PluginDependency>.marker() = get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version.requiredVersion}" }

dependencies {
    compileOnly(libs.plugins.android.library.marker())
    compileOnly(libs.plugins.kotlin.android.marker())
    compileOnly(libs.plugins.kover.marker())
    compileOnly(libs.plugins.maven.publish.marker())
    compileOnly(libs.plugins.spotless.marker())

    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "youversion.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("publishing") {
            id = "youversion.publishing"
            implementationClass = "PublishingConventionPlugin"
        }
        register("spotless") {
            id = "youversion.spotless"
            implementationClass = "SpotlessConventionPlugin"
        }
    }
}
