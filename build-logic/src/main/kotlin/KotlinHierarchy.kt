import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/**
 * Includes the Android target of the `com.android.kotlin.multiplatform.library` plugin in a hierarchy group;
 * [KotlinHierarchyBuilder.withAndroidTarget] only matches the older `androidTarget()`.
 */
fun KotlinHierarchyBuilder.withAndroidLibrary() {
    withCompilations { it.target.platformType == KotlinPlatformType.androidJvm }
}
