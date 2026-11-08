plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "com.kylecorry.andromeda.permissions"
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
        val mobileMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation("dev.icerock.moko:permissions:0.20.1")
                implementation("dev.icerock.moko:permissions-camera:0.20.1")
                implementation("dev.icerock.moko:permissions-location:0.20.1")
                implementation("dev.icerock.moko:permissions-microphone:0.20.1")
                implementation("dev.icerock.moko:permissions-bluetooth:0.20.1")
                implementation("dev.icerock.moko:permissions-notifications:0.20.1")
                implementation("dev.icerock.moko:permissions-motion:0.20.1")
                implementation("dev.icerock.moko:permissions-gallery:0.20.1")
                implementation("dev.icerock.moko:permissions-contacts:0.20.1")
                implementation("dev.icerock.moko:permissions-storage:0.20.1")
            }
        }
        androidMain { dependsOn(mobileMain) }
        iosMain { dependsOn(mobileMain) }
        commonMain.dependencies { api(project(":shared")) }
        androidMain.dependencies { implementation(project(":core")); implementation(libs.androidx.core.ktx); api("androidx.activity:activity:1.8.1") }
    }
}
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "permissions", "25.1.1") }
