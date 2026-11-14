plugins {
    id("andromeda.compose")
}

kotlin {
    sharedSourceSets(SharedSourceSet.DesktopApple)
    sourceSets {
        commonMain.dependencies {
            api(project(":speech"))
            api(libs.compose.runtime)
        }
        androidMain.dependencies { implementation(libs.compose.ui) }
    }
}
