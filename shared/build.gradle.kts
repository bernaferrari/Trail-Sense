plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)
    sourceSets.commonMain.dependencies {
        api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    }
    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
    }
}
