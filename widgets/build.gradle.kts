plugins {
    id("andromeda.compose")
}

kotlin {
    android {
        namespace = "com.kylecorry.andromeda.widgets.shared"
        androidResources { enable = true }
    }
    sourceSets {
        commonMain.dependencies { api(projects.shared); implementation(libs.compose.runtime) }
        androidMain.dependencies {
            api(libs.androidx.glance.appwidget)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }
    }
}
