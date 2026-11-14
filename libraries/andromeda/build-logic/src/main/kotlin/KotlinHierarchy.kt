import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/** Source sets that some, but not all, of the targets share, named `<name>Main` and `<name>Test`. */
enum class SharedSourceSet {
    /** Android and the desktop JVM, for code using JVM libraries. */
    JvmShared,

    /** Android and iOS. */
    Mobile,

    /** The desktop JVM and iOS. */
    DesktopApple,
}

/** Adds [sets] to the default hierarchy of source sets. */
@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun KotlinMultiplatformExtension.sharedSourceSets(vararg sets: SharedSourceSet) {
    applyDefaultHierarchyTemplate {
        common {
            for (set in sets) {
                when (set) {
                    SharedSourceSet.JvmShared -> group("jvmShared") {
                        withJvm()
                        withAndroidLibrary()
                    }
                    SharedSourceSet.Mobile -> group("mobile") {
                        withAndroidLibrary()
                        group("ios")
                    }
                    SharedSourceSet.DesktopApple -> group("desktopApple") {
                        withJvm()
                        group("ios")
                    }
                }
            }
        }
    }
}

/**
 * The Android target of the `com.android.kotlin.multiplatform.library` plugin, which
 * [KotlinHierarchyBuilder.withAndroidTarget] does not match because it only covers the older `androidTarget()`.
 */
@OptIn(ExperimentalKotlinGradlePluginApi::class)
private fun KotlinHierarchyBuilder.withAndroidLibrary() {
    withCompilations { it.target.platformType == KotlinPlatformType.androidJvm }
}
