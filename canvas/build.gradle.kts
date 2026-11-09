plugins {
    id("andromeda.compose")
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
        }
        commonMain.dependencies { api(projects.shared); api(libs.compose.foundation) }
    }
}
