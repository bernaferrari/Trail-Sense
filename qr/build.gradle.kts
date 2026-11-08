plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "com.kylecorry.andromeda.qr"
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
        val jvmSharedMain by creating {
            dependsOn(commonMain.get())
            dependencies { implementation(libs.zxing) }
        }
        jvmMain { dependsOn(jvmSharedMain) }
        androidMain { dependsOn(jvmSharedMain) }
        iosMain.dependencies { implementation("io.github.yuroyami:kiteqr:0.1.0") }
        commonMain.dependencies {
            api(project(":shared"))
            implementation("io.github.g0dkar:qrcode-kotlin:4.5.0")
        }
        androidMain.dependencies { implementation(libs.androidx.core.ktx) }
    }
}
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "qr", "25.1.1") }
