plugins {
    id("andromeda.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies { api(projects.sms); api(libs.compose.runtime) }
        androidMain.dependencies { implementation(libs.compose.ui) }
    }
}
