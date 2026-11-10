plugins {
    id("andromeda.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies { api(project(":sms")); api(libs.compose.runtime) }
        androidMain.dependencies { implementation(libs.compose.ui) }
    }
}
