plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies { api(project(":shared")) }
        androidMain.dependencies {
            implementation(project(":core"))
            implementation(project(":permissions"))
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.ksensor.core)
        }
    }
}
