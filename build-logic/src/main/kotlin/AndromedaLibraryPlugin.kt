import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * An Andromeda library module: Kotlin Multiplatform for Android, the JVM and iOS, with an explicit and validated
 * public API, published as `com.kylecorry.andromeda:<module name>`.
 *
 * The Android namespace defaults to `com.kylecorry.andromeda.<module name>`; a module can set its own. Android host
 * and device tests are enabled for modules with `src/androidHostTest` or `src/androidDeviceTest`.
 */
class AndromedaLibraryPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("com.vanniktech.maven.publish")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
        val libraryVersion = libs.version("andromeda")

        extensions.configure<KotlinMultiplatformExtension> {
            explicitApiWarning()
            jvmToolchain(libs.version("jvmToolchain").toInt())

            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryExtension>("android") {
                namespace = "com.kylecorry.andromeda.${name.replace('-', '.')}"
                compileSdk = libs.version("android-compileSdk").toInt()
                minSdk = libs.version("android-minSdk").toInt()
                // Android host and device tests only exist where a module has them. Common tests run on the JVM and
                // iOS instead: on the Android host the framework is only stubs.
                if (file("src/androidHostTest").isDirectory) {
                    withHostTestBuilder { sourceSetTreeName = "androidHostTest" }.configure {}
                }
                if (file("src/androidDeviceTest").isDirectory) {
                    withDeviceTest { instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
                }
            }
            jvm()
            iosArm64()
            iosSimulatorArm64()

            sourceSets.commonTest.dependencies { implementation(kotlin("test")) }

            compilerOptions {
                // Expect/actual classes are used across the modules for platform implementations
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
            @OptIn(ExperimentalAbiValidation::class)
            abiValidation()
        }

        // Bytecode stays loadable by apps that still target Java 11
        tasks.withType<KotlinJvmCompile>().configureEach { compilerOptions.jvmTarget.set(JvmTarget.JVM_11) }
        tasks.withType<Test>().configureEach { useJUnitPlatform() }

        group = GROUP
        version = libraryVersion
        extensions.configure<MavenPublishBaseExtension> {
            coordinates(GROUP, name, libraryVersion)
        }
    }

    private fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

    private companion object {
        const val GROUP = "com.kylecorry.andromeda"
    }
}
