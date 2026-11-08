plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "com.kylecorry.andromeda.print"
        compileSdk = 37
        minSdk = 24
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
    jvm { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)
    sourceSets {
        commonMain.dependencies { api(project(":shared")); api(project(":bitmaps")) }
        commonTest.dependencies { implementation(kotlin("test")) }
        jvmMain.dependencies { implementation("org.apache.pdfbox:pdfbox:3.0.6") }
        androidMain.dependencies { implementation(project(":core")); implementation(libs.androidx.core.ktx); implementation(libs.kotlinx.coroutines.android); implementation(libs.androidx.print) }
    }
}
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "print", "25.1.1") }
