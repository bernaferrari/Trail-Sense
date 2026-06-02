plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.kylecorry.trail_sense"
version = "1.0.0"

kotlin {
    // JVM target — allows the existing classic Android app (and future Desktop) to consume the common code
    jvm()

    // Real iOS targets — this module is now genuinely multiplatform
    iosArm64()
    iosSimulatorArm64()
    // Add more targets later (wasmJs, etc.) when ready

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform artifacts
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)

            // Portable expressive theming
            implementation("com.materialkolor:material-kolor:4.1.1")
        }
    }
}
