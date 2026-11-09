plugins {
    id("andromeda.compose")
}

kotlin {
    applyDefaultHierarchyTemplate {
        common {
            group("mobile") {
                withAndroidLibrary()
                group("ios")
            }
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":permissions"))
            api(libs.compose.runtime)
        }
        named("mobileMain") {
            dependencies { implementation(libs.moko.permissions.compose) }
        }
    }
}
