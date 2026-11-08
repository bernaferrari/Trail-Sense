plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}

kotlin {
    android {
        namespace = "com.kylecorry.andromeda.device"
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
        commonTest.dependencies { implementation(kotlin("test")) }
        jvmMain.dependencies { implementation("com.github.oshi:oshi-core:6.11.1") }
        commonMain.dependencies {
            api(project(":shared"))
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
        }
        val mobileMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                api("io.github.shadadman:ksensor-sensors-motion:9.40.0")
                api("io.github.shadadman:ksensor-sensors-environment:9.40.0")
                api("io.github.shadadman:ksensor-sensors-positioning:9.40.0")
                api("io.github.shadadman:ksensor-states-system:9.40.0")
            }
        }
        androidMain {
            dependsOn(mobileMain)
            dependencies { implementation(libs.androidx.core.ktx) }
        }
        iosMain { dependsOn(mobileMain) }
    }
}

group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "device", "25.1.1") }
