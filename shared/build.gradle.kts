plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}

kotlin {
    android {
        namespace = "com.kylecorry.andromeda.shared"
        compileSdk = 37
        minSdk = 24
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
    jvm {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)
    applyDefaultHierarchyTemplate()
    sourceSets {
        val jvmSharedMain by creating { dependsOn(commonMain.get()) }
        jvmSharedMain.kotlin.srcDir("src/jvmSharedMain/kotlin")
        jvmMain { dependsOn(jvmSharedMain) }
        androidMain { dependsOn(jvmSharedMain) }
    }
    sourceSets.commonMain.dependencies {
        api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
        api("com.kylecorry:sol:19.4.0")
        implementation("io.github.pdvrieze.xmlutil:core:1.0.2")
        implementation("io.ktor:ktor-client-cio:3.6.1")
        implementation("org.jetbrains.kotlinx:atomicfu:0.31.0")
    }
    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
    }
}

group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing {
    coordinates("com.kylecorry.andromeda", "shared", "25.1.1")
}
