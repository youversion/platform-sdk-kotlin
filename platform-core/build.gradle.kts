plugins {
    id("youversion.sdk.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.youversion.platform.core"
    defaultConfig {
        val sdkVersion = providers.gradleProperty("sdkVersion").getOrElse("Dev")
        buildConfigField("String", "SDK_VERSION", "\"$sdkVersion\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(platform(projects.platformBom))

    // api(): these types appear in platform-core's public signatures, so consumers need
    // them on their compile classpath. Everything else stays implementation.
    api(libs.koin.core) // PlatformKoinGraph.koinApplication, start(List<Module>)
    api(libs.kotlin.coroutines) // YouVersionPlatformConfiguration.configState, BibleHighlightsRepository.highlights

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.serialization)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.serialization)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.logging)

    implementation(libs.touchlab.kermit)

    testImplementation(libs.koin.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
}

mavenPublishing {
    coordinates(artifactId = "platform-core")

    pom {
        name = "YouVersion Platform SDK - Core"
        description =
            """
            Provides the fundamental building blocks for the YouVersion platform, such as data models, network calls, and caching.
            """.trimIndent()
    }
}
