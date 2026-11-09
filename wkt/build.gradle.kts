plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        jvmTest.dependencies {
            implementation(libs.junit.jupiter.api)
            runtimeOnly(libs.junit.jupiter.engine)
            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}
