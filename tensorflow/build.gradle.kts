plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "com.kylecorry.andromeda.tensorflow.shared"
        compileSdk = 37
        minSdk = 24
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
    jvm { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.compilations.getByName("main").cinterops.create("tflite") {
            defFile(project.file("src/nativeInterop/cinterop/tflite.def"))
            includeDirs(project.file("src/nativeInterop/cinterop"))
        }
    }
    jvmToolchain(17)
    sourceSets {
        commonMain.dependencies { api(project(":shared")) }
        androidMain.dependencies {
            implementation(project(":core"))
            api(libs.tensorflow.lite.support)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core.ktx)
        }
    }
}
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "tensorflow", "25.1.1") }
