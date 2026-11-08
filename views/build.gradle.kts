plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "com.kylecorry.andromeda.views.shared"
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
            api(project(":shared"))
            api(project(":list"))
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api("io.coil-kt.coil3:coil-compose:3.6.3")
            api("io.github.panpf.zoomimage:zoomimage-compose-coil3:1.7.0")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.6.3")
            implementation("io.ktor:ktor-client-cio:3.6.1")
        }
    }
}
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "views", "25.1.1") }
