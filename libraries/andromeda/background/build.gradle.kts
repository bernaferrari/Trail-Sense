plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies { api(project(":shared")); api(libs.kotlinx.datetime) }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core.ktx)
            api(libs.androidx.work.runtime.ktx)
            implementation(project(":core"))
            implementation(project(":storage"))
            implementation(project(":permissions"))
            api(libs.luna)
        }
    }
}
