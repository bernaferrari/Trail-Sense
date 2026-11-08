plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}

kotlin {
    android {
        namespace = "com.kylecorry.andromeda.storage"
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
        api("com.russhwolf:multiplatform-settings-no-arg:1.3.0")
        api("com.squareup.okio:okio:3.18.2")
    }
}

group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "storage", "25.1.1") }
