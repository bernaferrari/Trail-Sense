plugins {
    id("andromeda.compose")
}

kotlin {
    android {
        namespace = "com.kylecorry.andromeda.pickers.shared"
    }
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
            api(projects.shared)
            api(projects.forms)
            api(libs.filekit.dialogs.compose)
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
        }
    }
}
