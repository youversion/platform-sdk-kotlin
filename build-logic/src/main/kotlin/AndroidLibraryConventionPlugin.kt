import com.android.build.api.dsl.LibraryExtension
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            extensions.configure<LibraryExtension> {
                compileSdk = libs.version("compileSdk").toInt()
                defaultConfig.minSdk = libs.version("minSdk").toInt()
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }
            }

            extensions.configure<KotlinAndroidProjectExtension> {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                    // The SDK's own modules opt in to each other's @PlatformInternalApi declarations. Sample apps
                    // deliberately do not, so that they compile against the same surface a consumer sees.
                    optIn.add("com.youversion.platform.core.di.PlatformInternalApi")
                }
            }

            extensions.configure<KoverProjectExtension> {
                reports.verify.rule { minBound(85, CoverageUnit.LINE) }
            }

            tasks.named("check") {
                dependsOn(":verifyNoHardcodedUiStrings")
            }
        }
    }
}
