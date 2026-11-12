plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.kylecorry.trail_sense"
version = "1.0.0"

kotlin {
    android {
        namespace = "com.kylecorry.trail_sense.shared"
        compileSdk = 37
        androidResources { enable = true }
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // Domain models and calculations the shared UI and navigation build on
            api(project(":core"))
            api(libs.sol)
            // Navigation 3 keys and the app's back stack
            api(libs.androidx.navigation3.runtime)
            api(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)

            // Compose Multiplatform artifacts
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.cmp.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            // Google Sans Flex (OFL), the family's M3 Expressive typeface
            implementation(compose.components.resources)

            // Portable expressive theming
            implementation("com.materialkolor:material-kolor-material3:6.0.0")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
        }
    }
}

dependencies {
    add("androidMainApi", platform(libs.compose.bom))
}
