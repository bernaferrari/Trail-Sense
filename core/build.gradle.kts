plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

group = "com.kylecorry.trail_sense"
version = "1.0.0"

// Platform-independent Trail Sense logic (domain models, calculations, formatting-free services).
// Platform-independent Room models and DAOs are shared; database creation and platform services stay in platform code.
kotlin {
    android {
        namespace = "com.kylecorry.trail_sense.core"
        compileSdk = 37
        minSdk = 24
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    iosArm64()
    iosSimulatorArm64()
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                enabled = false
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.room.runtime)
            api(libs.sol)
            api(libs.luna)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.atomicfu)
            implementation(libs.androidx.collection)
            // Resource and color annotations (@ColorInt, @RawRes...) are multiplatform
            api(libs.androidx.annotation)
            api(libs.andromeda.core)
            api(libs.andromeda.sense)
            api(libs.andromeda.sound)
            api(libs.andromeda.signal)
            api(libs.andromeda.torch)
            api(libs.andromeda.preferences)
            api(libs.andromeda.geojson)
            api(libs.andromeda.gpx)
            api(libs.andromeda.csv)
            // Persisted state (e.g. GPSKalmanState) goes through Andromeda's kotlinx.serialization JsonConvert
            implementation(libs.kotlinx.serialization.core)
        }
        iosMain.dependencies { implementation("androidx.sqlite:sqlite-bundled:2.7.1") }
        wasmJsMain.dependencies {
            implementation("androidx.sqlite:sqlite-web:2.7.1")
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.multiplatform.settings.test)
        }
    }
}

// Room generates the native database implementation for each iOS target.
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
    add("kspWasmJs", libs.room.compiler)
}
