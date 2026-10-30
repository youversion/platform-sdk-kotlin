package com.youversion.platform.buildlogic

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class ConventionPluginsTest {
    private val projectDir = createTempDirectory("convention-plugins").toFile()

    @After
    fun tearDown() {
        projectDir.deleteRecursively()
    }

    @Test
    fun publishing_setsGroupAndFallsBackToCatalogVersion() {
        writePublishingFixture()

        val pom = generatedPom()

        assertTrue(pom, pom.contains("<groupId>com.youversion.platform</groupId>"))
        assertTrue(pom, pom.contains("<version>${catalogVersion("youversionPlatform")}</version>"))
    }

    @Test
    fun publishing_usesSdkVersionPropertyWhenSet() {
        writePublishingFixture()

        val pom = generatedPom("-PsdkVersion=9.9.9")

        assertTrue(pom, pom.contains("<version>9.9.9</version>"))
    }

    @Test
    fun sdkLibrary_checkDependsOnRootHardcodedUiStringsTask() {
        writeSdkLibraryFixture()

        val output = build(":sdk:check", "--dry-run").output

        assertTrue(output, output.contains(":verifyNoHardcodedUiStrings SKIPPED"))
    }

    @Test
    fun sdkLibrary_enforcesLineCoverageMinimumOf85() {
        writeSdkLibraryFixture()

        val output = runner(":sdk:koverVerify").buildAndFail().output

        assertTrue(output, output.contains("expected minimum is 85"))
    }

    @Test
    fun androidLibrary_compilesForKotlin22Consumers() {
        writeSdkLibraryFixture()
        File(projectDir, "sdk/build.gradle.kts").appendText(
            """

            tasks.register("printKotlinOptions") {
                val compile = tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileReleaseKotlin")
                val languageVersion = compile.flatMap { it.compilerOptions.languageVersion }
                val apiVersion = compile.flatMap { it.compilerOptions.apiVersion }
                val moduleName = compile.flatMap { it.compilerOptions.moduleName }
                doLast {
                    println("languageVersion=${'$'}{languageVersion.get().version}")
                    println("apiVersion=${'$'}{apiVersion.get().version}")
                    println("moduleName=${'$'}{moduleName.get()}")
                }
            }
            """.trimIndent(),
        )

        val output = build(":sdk:printKotlinOptions").output

        assertTrue(output, output.contains("languageVersion=2.2"))
        assertTrue(output, output.contains("apiVersion=2.2"))
        assertTrue(output, output.contains("moduleName=sdk_release"))
    }

    @Test
    fun androidLibrary_publishesStdlibFloorInPom() {
        writeSdkLibraryFixture()

        build(":sdk:generatePomFileForMavenPublication")
        val pom = File(projectDir, "sdk/build/publications/maven/pom-default.xml").readText()

        assertTrue(
            pom,
            Regex(
                "<artifactId>kotlin-stdlib</artifactId>\\s*<version>${catalogVersion("kotlinStdlibFloor")}</version>",
            ).containsMatchIn(pom),
        )
    }

    private fun writePublishingFixture() {
        writeSettings("lib")
        write("build.gradle.kts", "")
        write(
            "lib/build.gradle.kts",
            """
            plugins {
                `java-library`
                id("youversion.publishing")
            }
            """.trimIndent(),
        )
    }

    private fun writeSdkLibraryFixture() {
        writeSettings("sdk")
        System.getProperty("androidSdkDir")?.let { sdkDir ->
            write("local.properties", "sdk.dir=${File(sdkDir).invariantSeparatorsPath}")
        }
        write(
            "gradle.properties",
            """
            android.builtInKotlin=false
            android.newDsl=false
            """.trimIndent(),
        )
        write(
            "build.gradle.kts",
            """
            plugins {
                id("youversion.hardcoded-ui-strings")
            }
            """.trimIndent(),
        )
        write(
            "sdk/build.gradle.kts",
            """
            plugins {
                id("youversion.sdk.library")
            }

            android {
                namespace = "com.youversion.platform.fixture"
            }

            mavenPublishing {
                coordinates(artifactId = "sdk")
            }
            """.trimIndent(),
        )
        write(
            "sdk/src/main/kotlin/Uncovered.kt",
            """
            fun uncovered(): Int = 1
            """.trimIndent(),
        )
    }

    private fun writeSettings(module: String) {
        write(
            "settings.gradle.kts",
            """
            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
                versionCatalogs {
                    create("libs") {
                        from(files("${versionCatalog.invariantSeparatorsPath}"))
                    }
                }
            }

            include(":$module")
            """.trimIndent(),
        )
    }

    private fun write(
        path: String,
        content: String,
    ) {
        File(projectDir, path).apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }

    private fun generatedPom(vararg arguments: String): String {
        build(":lib:generatePomFileForMavenPublication", *arguments)
        return File(projectDir, "lib/build/publications/maven/pom-default.xml").readText()
    }

    private fun build(vararg arguments: String): BuildResult = runner(*arguments).build()

    private fun runner(vararg arguments: String): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withEnvironment(System.getenv() + ("CI" to "true"))
            .withArguments(*arguments)

    private fun catalogVersion(alias: String): String =
        versionCatalog
            .readLines()
            .first { it.startsWith("$alias ") || it.startsWith("$alias=") }
            .substringAfter("\"")
            .substringBefore("\"")

    private companion object {
        val versionCatalog = File(System.getProperty("versionCatalog"))
    }
}
