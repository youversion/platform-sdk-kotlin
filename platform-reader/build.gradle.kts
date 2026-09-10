import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

android {
    namespace = "com.youversion.platform.reader"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()

        consumerProguardFiles("consumer-rules.pro")
        vectorDrawables.useSupportLibrary = true
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // api(): these types appear in platform-reader's public signatures, so consumers need
    // them on their compile classpath. Everything else stays implementation.
    api(projects.platformCore) // BibleReader(bibleReference: BibleReference?)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui) // FontDefinition(fontFamily: FontFamily), BibleReader's @Composable bottomBar

    // platform-ui backs the reader's internals but never surfaces in its four public
    // declarations, so it stays off the consumer's compile classpath.
    implementation(projects.platformUi)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.serialization)

    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons.core)
    implementation(libs.androidx.compose.icons.extended)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.touchlab.kermit)

    implementation(libs.koin.core)
    implementation(libs.koin.androidx.compose)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testReleaseImplementation(libs.androidx.compose.ui.test.manifest)
}

mavenPublishing {
    coordinates(
        groupId = "com.youversion.platform",
        artifactId = "platform-reader",
        version = libs.versions.youversionPlatform.get(),
    )

    pom {
        name = "YouVersion Platform SDK - Reader"
        description =
            """
            High-level reader functionality combining core SDK features and UI components for complete Bible reading experiences.
            """.trimIndent()
    }
}
