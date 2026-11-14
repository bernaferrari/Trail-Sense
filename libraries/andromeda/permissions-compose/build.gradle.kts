plugins {
    id("andromeda.compose")
}

kotlin {
    sharedSourceSets(SharedSourceSet.Mobile)
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
