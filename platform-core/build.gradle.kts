plugins {
    id("youversion.android.library")
    alias(libs.plugins.kotlin.serialization)
    id("youversion.publishing")
    id("youversion.spotless")
}

android {
    namespace = "com.youversion.platform.core"
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")

        val sdkVersion = (project.findProperty("sdkVersion") as? String) ?: "Dev"
        buildConfigField("String", "SDK_VERSION", "\"$sdkVersion\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
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
    coordinates(
        groupId = "com.youversion.platform",
        artifactId = "platform-core",
        // The release workflow passes -PsdkVersion so the published coordinate and the
        // version stamped into the release commit cannot drift. Local builds fall back to
        // the catalog. See RELEASING.md.
        version =
            (project.findProperty("sdkVersion") as? String)
                ?: libs.versions.youversionPlatform.get(),
    )

    pom {
        name = "YouVersion Platform SDK - Core"
        description =
            """
            Provides the fundamental building blocks for the YouVersion platform, such as data models, network calls, and caching.
            """.trimIndent()
    }
}
