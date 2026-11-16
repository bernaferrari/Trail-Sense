plugins {
    id("andromeda.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.markdown)
            api(libs.markdownRenderer.m3)
            api(libs.markdownRenderer.coil3)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            api(libs.compose.foundation)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
    }
}
