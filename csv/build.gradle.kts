plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}

kotlin {
    android {
        namespace = "com.kylecorry.andromeda.csv"
        compileSdk = 37
        minSdk = 24
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
    jvm { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain.dependencies {  }
        jvmTest {
            kotlin.srcDir("src/test/java")
            dependencies {
                implementation(libs.junit.jupiter.api)
                implementation(libs.junit.jupiter.params)
                runtimeOnly(libs.junit.jupiter.engine)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "csv", "25.1.1") }
