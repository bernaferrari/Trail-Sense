plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies { api(project(":shared")) }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.ksensor.core)
        }
    }
}
