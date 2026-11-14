plugins {
    id("andromeda.compose")
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
        }
        commonMain.dependencies { api(project(":shared")); api(libs.compose.foundation) }
    }
}
