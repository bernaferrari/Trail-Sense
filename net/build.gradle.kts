plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies { implementation(libs.ktor.client.cio) }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter.api)
            runtimeOnly(libs.junit.jupiter.engine)
            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}
