plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "com.kylecorry.andromeda.preferences"
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        withHostTestBuilder {}.configure {}
        compileSdk = 37
        minSdk = 24
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
    jvm { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)
    sourceSets {
        getByName("androidDeviceTest") {
            kotlin.srcDir("src/androidTest/java")
            dependencies {
                implementation(libs.junit)
                implementation(libs.androidx.junit)
                implementation(libs.androidx.espresso.core)
            }
        }
        getByName("androidHostTest") {
            kotlin.srcDir("src/test/java")
            dependencies {
                implementation(libs.junit)
                implementation(libs.mockito.kotlin)
            }
        }
        commonMain.dependencies { api(project(":storage")) }
        androidMain.dependencies { implementation(project(":core")); implementation(libs.androidx.core.ktx); implementation(libs.androidx.preference.ktx); implementation(libs.androidx.datastore.preferences) }
    }
}
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "preferences", "25.1.1") }
