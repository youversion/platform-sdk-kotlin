import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.maven.publish)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

android {
    namespace = "com.youversion.platform.core"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(libs.koin.core)

    implementation(libs.kotlin.coroutines)
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
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        groupId = "com.youversion.platform",
        artifactId = "yvp-core",
        version = libs.versions.yvpPlatform.get(),
    )

    pom {
        name = "YouVersion Platform SDK - Core"
        description =
            """
            Provides the fundamental building blocks for the YouVersion platform, such as data models, network calls, and caching.
            """.trimIndent()
        url = "https://platform.youversion.com/"
        inceptionYear = "2025"

        licenses {
            license {
                name = "Apache License, Version 2.0"
                url = "https://opensource.org/licenses/Apache-2.0"
                distribution = "https://opensource.org/licenses/Apache-2.0"
            }
        }

        scm {
            connection = "scm:git:https://github.com/youversion/platform-sdk-kotlin.git"
            developerConnection = "scm:git:ssh://git@github.com/youversion/platform-sdk-kotlin.git"
            url = "https://github.com/youversion/platform-sdk-kotlin"
        }

        developers {
            developer {
                name = "YouVersion"
                email = "engineering@youversion.com"
            }
        }

        organization {
            name = "YouVersion"
            url = "https://youversion.com/"
        }
    }
}
