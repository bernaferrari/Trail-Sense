plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies { api(projects.shared); api(libs.kotlinx.datetime) }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core.ktx)
            api(libs.androidx.work.runtime.ktx)
            implementation(projects.core)
            implementation(projects.storage)
            implementation(projects.permissions)
            api(libs.luna)
        }
    }
}
