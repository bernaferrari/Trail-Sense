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
            api(project(":print"))
            api(libs.compose.runtime)
        }
        androidMain.dependencies { implementation(libs.compose.ui) }
    }
}
