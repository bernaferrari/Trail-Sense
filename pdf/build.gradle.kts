plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "com.kylecorry.andromeda.pdf"
        compileSdk = 37
        minSdk = 24
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure { instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
        withHostTestBuilder {}.configure {}
    }
    jvm { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)
    applyDefaultHierarchyTemplate()
    sourceSets {
        val jvmSharedMain by creating {
            dependsOn(commonMain.get())
            dependencies { api(libs.luna); implementation(project(":wkt")); implementation(project(":compression")) }
        }
        jvmMain { dependsOn(jvmSharedMain) }
        androidMain { dependsOn(jvmSharedMain) }
        commonMain.dependencies { api(project(":shared")) }
        androidMain.dependencies { implementation(project(":core")); implementation(libs.androidx.core.ktx) }
        getByName("androidDeviceTest") {
            kotlin.srcDir("src/androidTest/java")
            dependencies { implementation(libs.junit); implementation(libs.androidx.junit); implementation(libs.androidx.espresso.core) }
        }
        getByName("androidHostTest") {
            kotlin.srcDir("src/test/java")
            dependencies {
                implementation(libs.junit); implementation(libs.junit.jupiter.api); implementation(libs.junit.jupiter.params)
                runtimeOnly(libs.junit.jupiter.engine); runtimeOnly(libs.junit.platform.launcher)
            }
        }
    }
}
tasks.withType<Test>().configureEach { useJUnitPlatform() }
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "pdf", "25.1.1") }
