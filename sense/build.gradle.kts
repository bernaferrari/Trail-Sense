plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "com.kylecorry.andromeda.sense"
        compileSdk = 37
        minSdk = 24
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
    jvm { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)
    sourceSets {
        commonMain.dependencies {
            api("com.kylecorry:sol:19.4.0")
            api(project(":device"))
            api(project(":storage"))
            implementation("org.jetbrains.kotlinx:atomicfu:0.31.0")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        androidMain.dependencies { implementation(project(":core")); implementation(project(":permissions")); implementation(project(":preferences")); implementation(libs.androidx.core.ktx) }
    }
}
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "sense", "25.1.1") }
