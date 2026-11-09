plugins {
    id("andromeda.compose")
}

kotlin {
    applyDefaultHierarchyTemplate {
        common {
            group("desktopApple") {
                withJvm()
                group("ios")
            }
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":speech"))
            api(libs.compose.runtime)
        }
        androidMain.dependencies { implementation(libs.compose.ui) }
    }
}
