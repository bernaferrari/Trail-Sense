plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "com.kylecorry.andromeda.json"
        compileSdk = 37
        minSdk = 24
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    jvm { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain.dependencies { api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0") }
        getByName("androidDeviceTest") {
            kotlin.srcDir("src/androidTest/java")
            dependencies {
                implementation(libs.junit)
                implementation(libs.androidx.junit)
                implementation(libs.androidx.espresso.core)
            }
        }
    }
}
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "json", "25.1.1") }
