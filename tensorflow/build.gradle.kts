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
        jvmMain.dependencies {
            implementation("org.bytedeco:tensorflow-lite:2.21.0-1.5.14")
            listOf("linux-arm64", "linux-x86_64", "macosx-arm64", "macosx-x86_64", "windows-x86_64").forEach { platform ->
                runtimeOnly("org.bytedeco:tensorflow-lite:2.21.0-1.5.14:$platform")
                runtimeOnly("org.bytedeco:javacpp:1.5.14:$platform")
            }
        }
        jvmTest.dependencies { implementation(kotlin("test")) }
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
