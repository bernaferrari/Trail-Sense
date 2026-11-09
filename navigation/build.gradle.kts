plugins {
    id("andromeda.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":shared"))
        api(libs.compose.runtime)
        api(libs.jetbrains.navigation.compose)
        api(libs.jetbrains.lifecycle.runtime.compose)
    }
}
