plugins {
    id("andromeda.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":shared"))
        api(libs.compose.runtime)
        // Navigation Compose brings an older Compose UI; keep it at the Compose Multiplatform version
        api(libs.compose.ui)
        api(libs.jetbrains.navigation.compose)
        api(libs.jetbrains.lifecycle.runtime.compose)
    }
}
