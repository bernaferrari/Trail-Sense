plugins {
    id("andromeda.library")
}

kotlin {
    android {
        namespace = "com.kylecorry.andromeda.tensorflow.shared"
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.compilations.getByName("main").cinterops.create("tflite") {
            defFile(project.file("src/nativeInterop/cinterop/tflite.def"))
            includeDirs(project.file("src/nativeInterop/cinterop"))
        }
    }
    sourceSets {
        commonMain.dependencies { api(project(":shared")); api(project(":bitmaps")) }
        jvmMain.dependencies {
            implementation(libs.tensorflowLite.jvm)
            listOf("linux-arm64", "linux-x86_64", "macosx-arm64", "macosx-x86_64", "windows-x86_64").forEach { platform ->
            runtimeOnly("org.bytedeco:tensorflow-lite:2.21.0-1.5.14:$platform")
            runtimeOnly("org.bytedeco:javacpp:1.5.14:$platform")
            }
        }
        androidMain.dependencies {
            implementation(libs.litert)
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}
