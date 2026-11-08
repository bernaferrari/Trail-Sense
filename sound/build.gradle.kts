plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "com.kylecorry.andromeda.sound"
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
            api(project(":shared"))
            implementation("org.jetbrains.kotlinx:atomicfu:0.31.0")
            api("eu.iamkonstantin.kotlin:gadulka:1.14.0")
        }
        iosMain.dependencies { implementation("io.github.piotrprus:audio-capture:0.1.0") }
        androidMain.dependencies { implementation(project(":core")); implementation(project(":permissions")); implementation(libs.androidx.core.ktx); implementation(libs.kotlinx.coroutines.android) }
    }
}
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "sound", "25.1.1") }
