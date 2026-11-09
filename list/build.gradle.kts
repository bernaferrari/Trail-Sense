plugins {
    id("andromeda.compose")
}

kotlin {
    android {
        namespace = "com.kylecorry.andromeda.list.shared"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.shared)
            api(libs.compose.runtime)
            api(libs.compose.foundation)
        }
    }
}
