plugins {
    id("andromeda.compose")
}

kotlin {
    sharedSourceSets(SharedSourceSet.DesktopApple)
    sourceSets {
        commonMain.dependencies {
            api(project(":print"))
            api(libs.compose.runtime)
        }
        androidMain.dependencies { implementation(libs.compose.ui) }
    }
}
