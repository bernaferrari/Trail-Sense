import org.gradle.api.Plugin
import org.gradle.api.Project

/** An [AndromedaLibraryPlugin] module with Compose Multiplatform UI. */
class AndromedaComposePlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(AndromedaLibraryPlugin::class.java)
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    }
}
