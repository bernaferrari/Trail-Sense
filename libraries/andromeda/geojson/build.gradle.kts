plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies { api(libs.sol); implementation(libs.kotlinx.serialization.json) }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter.api)
            implementation(libs.junit.jupiter.params)
            runtimeOnly(libs.junit.jupiter.engine)
            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}
