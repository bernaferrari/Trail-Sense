plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}

kotlin {
    android {
        namespace = "com.kylecorry.andromeda.device"
        compileSdk = 37
        minSdk = 24
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
    jvm { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)
    sourceSets.commonMain.dependencies {
        api(project(":shared"))
        api("io.github.shadadman:ksensor-sensors-motion:9.40.0")
        api("io.github.shadadman:ksensor-sensors-environment:9.40.0")
        api("io.github.shadadman:ksensor-sensors-positioning:9.40.0")
        api("io.github.shadadman:ksensor-states-system:9.40.0")
    }
}

group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "device", "25.1.1") }
