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
            api(libs.sol)
            api(projects.shared)
            api(libs.compose.foundation)
        }
        named("desktopAppleMain") {
            dependencies {
            implementation(libs.camerak)
            implementation(libs.okio)
            implementation(libs.coil.compose)
            }
        }
        androidMain.dependencies {
            implementation(projects.core)
            implementation(projects.permissions)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            api(libs.androidx.camera.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.androidx.core.ktx)
        }
    }
}
