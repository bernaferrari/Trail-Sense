plugins {
    id("andromeda.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.shared)
        api(libs.compose.runtime)
        api(libs.jetbrains.navigation.compose)
        api(libs.jetbrains.lifecycle.runtime.compose)
    }
}
