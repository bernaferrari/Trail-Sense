plugins {
    id("andromeda.compose")
}

kotlin {
    android {
        namespace = "com.kylecorry.andromeda.forms.shared"
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
        }
    }
}
