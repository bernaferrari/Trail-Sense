plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies { api(projects.shared) }
        androidMain.dependencies {
            implementation(projects.core)
            implementation(projects.permissions)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.ksensor.core)
        }
    }
}
