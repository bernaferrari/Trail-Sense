plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}
kotlin {
    android {
        namespace = "com.kylecorry.andromeda.exceptions.shared"
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
        val jvmSharedMain by creating { dependsOn(commonMain.get()) }
        jvmMain { dependsOn(jvmSharedMain) }
        androidMain { dependsOn(jvmSharedMain) }
        commonMain.dependencies {
            api(project(":shared"))
            api(project(":core"))
            api("com.squareup.okio:okio:3.18.2")
            implementation("org.jetbrains.kotlinx:atomicfu:0.31.0")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing { coordinates("com.kylecorry.andromeda", "exceptions", "25.1.1") }
