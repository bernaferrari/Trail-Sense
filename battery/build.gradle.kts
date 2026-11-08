plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "com.kylecorry.andromeda.battery"
        compileSdk = 37
        minSdk = 24
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure { instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    }
    jvm { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)
    sourceSets {
        commonMain.dependencies {
            api("com.kylecorry:sol:19.4.0")
            api(project(":shared"))
            api(project(":device"))
        }
        androidMain.dependencies { implementation(project(":core")); implementation(libs.androidx.core.ktx) }
        getByName("androidDeviceTest") {
            kotlin.srcDir("src/androidTest/java")
            dependencies { implementation(libs.junit); implementation(libs.androidx.junit); implementation(libs.androidx.espresso.core) }
        }
    }
}
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "battery", "25.1.1") }
