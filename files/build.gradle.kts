plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "com.kylecorry.andromeda.files"
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
            api(project(":storage"))
            api("io.github.vinceglb:filekit-core:0.16.0")
            implementation("io.ktor:ktor-http:3.6.1")
            api("no.synth:kmp-zip-okio:2.1.0")
        }
        jvmTest.dependencies { implementation(kotlin("test")) }
        androidMain.dependencies { implementation(libs.androidx.core.ktx); implementation(libs.kotlinx.coroutines.android); implementation(libs.androidx.documentfile) }
    }
}
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "files", "25.1.1") }
