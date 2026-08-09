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
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    iosArm64()
    iosSimulatorArm64()

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
