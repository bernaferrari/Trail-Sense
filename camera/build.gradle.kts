plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "com.kylecorry.andromeda.camera"
        compileSdk = 37
        minSdk = 24
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
    jvm { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            api(compose.foundation)
        }
        val desktopAppleMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation("io.github.kashif-mehmood-km:camerak:1.2")
                implementation("com.squareup.okio:okio:3.18.2")
                implementation("io.coil-kt.coil3:coil-compose:3.6.3")
            }
        }
        jvmMain { dependsOn(desktopAppleMain) }
        iosMain { dependsOn(desktopAppleMain) }
        androidMain.dependencies { implementation(project(":core")); implementation(project(":bitmaps")); implementation(project(":permissions")); implementation(libs.androidx.camera.camera2); implementation(libs.androidx.camera.lifecycle); api("androidx.camera:camera-compose:1.6.2"); implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.11.0"); implementation(libs.androidx.core.ktx) }
    }
}
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "camera", "25.1.1") }
