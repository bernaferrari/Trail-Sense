plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.kylecorry.trail_sense"
version = "1.0.0"

// Platform-independent Trail Sense logic (domain models, calculations, formatting-free services).
// Anything that needs Android, Room, Compose, or files lives in :app (or this module's androidMain).
kotlin {
    android {
        namespace = "com.kylecorry.trail_sense.core"
        compileSdk = 37
        minSdk = 24
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.sol)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.atomicfu)
            api(libs.andromeda.core)
            api(libs.andromeda.sense)
            api(libs.andromeda.sound)
            api(libs.andromeda.signal)
            api(libs.andromeda.torch)
            api(libs.andromeda.preferences)
            api(libs.andromeda.geojson)
            // Persisted state (e.g. GPSKalmanState) goes through Andromeda's kotlinx.serialization JsonConvert
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
