plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        jvmTest.dependencies {
            implementation(libs.junit.jupiter.api)
            implementation(libs.junit.jupiter.params)
            runtimeOnly(libs.junit.jupiter.engine)
            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}
