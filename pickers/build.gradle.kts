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
            api(project(":shared"))
            api(project(":forms"))
            api(libs.filekit.dialogs.compose)
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
        }
    }
}
