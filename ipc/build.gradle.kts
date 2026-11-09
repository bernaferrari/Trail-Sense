plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "com.kylecorry.andromeda.ipc"
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
            implementation("io.ktor:ktor-network:3.6.1")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        androidMain.dependencies { implementation(libs.androidx.core.ktx); implementation(libs.kotlinx.coroutines.android) }
    }
}
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "ipc", "25.1.1") }

kotlin {
    sourceSets.androidMain.dependencies {
        implementation(project(":core"))
        implementation(libs.luna)
    }
}
